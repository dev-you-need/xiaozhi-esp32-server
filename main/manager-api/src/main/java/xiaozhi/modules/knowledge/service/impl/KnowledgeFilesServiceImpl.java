package xiaozhi.modules.knowledge.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import xiaozhi.common.exception.ErrorCode;
import org.springframework.util.CollectionUtils;
import xiaozhi.common.exception.RenException;
import xiaozhi.modules.knowledge.dto.KnowledgeFilesDTO;
import xiaozhi.modules.knowledge.dto.document.ChunkDTO;
import xiaozhi.modules.knowledge.dto.document.RetrievalDTO;
import xiaozhi.modules.knowledge.dto.document.DocumentDTO;
import xiaozhi.common.page.PageData;
import xiaozhi.common.redis.RedisKeys;
import xiaozhi.common.redis.RedisUtils;
import xiaozhi.common.service.impl.BaseServiceImpl;
import xiaozhi.modules.knowledge.dao.DocumentDao;
import xiaozhi.modules.knowledge.entity.DocumentEntity;
import xiaozhi.modules.knowledge.rag.KnowledgeBaseAdapter;
import xiaozhi.modules.knowledge.rag.KnowledgeBaseAdapterFactory;
import xiaozhi.modules.knowledge.service.KnowledgeBaseService;
import xiaozhi.modules.knowledge.service.KnowledgeFilesService;

@Service
@Slf4j
public class KnowledgeFilesServiceImpl extends BaseServiceImpl<DocumentDao, DocumentEntity>
        implements KnowledgeFilesService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentDao documentDao;
    private final ObjectMapper objectMapper;
    private final RedisUtils redisUtils;

    public KnowledgeFilesServiceImpl(KnowledgeBaseService knowledgeBaseService,
            DocumentDao documentDao,
            ObjectMapper objectMapper,
            RedisUtils redisUtils) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentDao = documentDao;
        this.objectMapper = objectMapper;
        this.redisUtils = redisUtils;
    }

    @Lazy
    @Autowired
    private KnowledgeFilesService self;

    @Override
    public Map<String, Object> getRAGConfig(String ragModelId) {
        return knowledgeBaseService.getRAGConfig(ragModelId);
    }

    @Override
    public PageData<KnowledgeFilesDTO> getPageList(KnowledgeFilesDTO knowledgeFilesDTO, Integer page, Integer limit) {
        log.info("=== 开始获取知识库文档列表 (Local-First 优化版) ===");
        String datasetId = knowledgeFilesDTO.getDatasetId();
        if (StringUtils.isBlank(datasetId)) {
            throw new RenException(ErrorCode.RAG_DATASET_ID_AND_MODEL_ID_NOT_NULL);
        }

        //Полная синхронизация сверки: извлекайте удаленные документы из RAGFlow для синхронизации в реальном времени, чтобы обеспечить немедленную осведомленность об удаленных изменениях
        try {
            self.syncDocumentsFromRAG(datasetId);
        } catch (Exception e) {
            log.warn("从RAGFlow全量同步文档失败(不影响本地查询): datasetId={}, error={}", datasetId, e.getMessage());
        }

        //1. Получение данных локальной теневой таблицы (вкладка MyBatis-Plus)
        Page<DocumentEntity> pageParams = new Page<>(page, limit);
        QueryWrapper<DocumentEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dataset_id", datasetId);
        if (StringUtils.isNotBlank(knowledgeFilesDTO.getName())) {
            queryWrapper.like("name", knowledgeFilesDTO.getName());
        }
        if (StringUtils.isNotBlank(knowledgeFilesDTO.getRun())) {
            queryWrapper.eq("run", knowledgeFilesDTO.getRun());
        }
        if (StringUtils.isNotBlank(knowledgeFilesDTO.getStatus())) {
            queryWrapper.eq("status", knowledgeFilesDTO.getStatus());
        }
        queryWrapper.orderByDesc("created_at");

        //2. Выполнение локальных запросов
        Page<DocumentEntity> iPage = documentDao.selectPage(pageParams, queryWrapper);

        //3. Преобразование DTO вручную
        List<KnowledgeFilesDTO> dtoList = new ArrayList<>();
        for (DocumentEntity entity : iPage.getRecords()) {
            dtoList.add(convertEntityToDTO(entity));
        }
        PageData<KnowledgeFilesDTO> pageData = new PageData<>(dtoList, iPage.getTotal());

        //4. Синхронизация динамического состояния (с ограничением тока и защитой)
        //[Исправление ошибок] P1: Расширение белого списка синхронизации, отмена/сбой также позволяет низкочастотной синхронизации поддерживать самовосстановление
        if (pageData.getList() != null && !pageData.getList().isEmpty()) {
            KnowledgeBaseAdapter adapter = null;
            for (KnowledgeFilesDTO dto : pageData.getList()) {
                String runStatus = dto.getRun();
                //Синхронизация с высоким приоритетом: работает/UNSTART (перезарядка 5 с)
                boolean isActiveSync = "RUNNING".equals(runStatus) || "UNSTART".equals(runStatus);
                //Низкочастотная самовосстанавливающаяся синхронизация: отмена/сбой (60 секунд перезарядки) для предотвращения постоянной блокировки состояния ошибки
                boolean isRecoverySync = "CANCEL".equals(runStatus) || "FAIL".equals(runStatus);
                boolean needSync = isActiveSync || isRecoverySync;

                if (needSync) {
                    //Защита от ограничения тока: 5 секунд охлаждения в активном состоянии, 60 секунд охлаждения в самовосстанавливающемся состоянии
                    long cooldownMs = isActiveSync ? 5000 : 60000;
                    DocumentEntity localEntity = documentDao.selectOne(new QueryWrapper<DocumentEntity>()
                            .eq("document_id", dto.getDocumentId()));
                    if (localEntity != null && localEntity.getLastSyncAt() != null) {
                        long diff = System.currentTimeMillis() - localEntity.getLastSyncAt().getTime();
                        if (diff < cooldownMs) {
                            continue;
                        }
                    }

                    //Отложить адаптер инициализации, создавать только тогда, когда синхронизация действительно необходима
                    if (adapter == null) {
                        try {
                            Map<String, Object> ragConfig = knowledgeBaseService.getRAGConfigByDatasetId(datasetId);
                            adapter = KnowledgeBaseAdapterFactory.getAdapter(extractAdapterType(ragConfig), ragConfig);
                        } catch (Exception e) {
                            log.warn("同步中断：无法初始化适配器, {}", e.getMessage());
                            break;
                        }
                    }
                    //[критическое исправление] Запишите количество токенов перед синхронизацией, используемое для расчета приращений
                    Long oldTokenCount = dto.getTokenCount() != null ? dto.getTokenCount() : 0L;

                    syncDocumentStatusWithRAG(dto, adapter);

                    //Рассчитываем приращения и обновляем статистику базы знаний (в соответствии с задачами по времени)
                    Long newTokenCount = dto.getTokenCount() != null ? dto.getTokenCount() : 0L;
                    Long tokenDelta = newTokenCount - oldTokenCount;
                    if (tokenDelta != 0) {
                        knowledgeBaseService.updateStatistics(datasetId, 0, 0L, tokenDelta);
                        log.info("懒加载同步: 修正知识库统计, docId={}, tokenDelta={}", dto.getDocumentId(), tokenDelta);
                    }
                }
            }
        }

        log.info("获取文档列表成功，总数: {}", pageData.getTotal());
        return pageData;
    }

    /**
     * Преобразовать локальные объекты записи в DTO, вручную выровнять несогласованные поля (size - > fileSize, type - > fileType)
     */
    private KnowledgeFilesDTO convertEntityToDTO(DocumentEntity entity) {
        if (entity == null) {
            return null;
        }
        KnowledgeFilesDTO dto = new KnowledgeFilesDTO();
        //1. Копия базового поля
        BeanUtils.copyProperties(entity, dto);

        //Выпуск 2: Исправление семантики идентификатора. Внешний интерфейс привык использовать идентификатор в качестве первичного ключа для операции.
        //В этом модуле удаленный documentId всегда должен быть сопоставлен с идентификатором DTO, гарантируя, что идентификатор интерфейса согласован во время таких операций, как details/deletion.
        dto.setId(entity.getDocumentId());

        //2. Преобразовать локальный объект записи в DTO, вручную выровнять несогласованные поля (size - > fileSize, type - > fileType)
        dto.setFileSize(entity.getSize());
        dto.setFileType(entity.getType());
        dto.setRun(entity.getRun());
        dto.setChunkCount(entity.getChunkCount());
        dto.setTokenCount(entity.getTokenCount());
        dto.setError(entity.getError());

        //3. Десериализация пользовательских метаданных JSON (выпуск 3)
        if (StringUtils.isNotBlank(entity.getMetaFields())) {
            try {
                dto.setMetaFields(objectMapper.readValue(entity.getMetaFields(),
                        new TypeReference<Map<String, Object>>() {
                        }));
            } catch (Exception e) {
                log.warn("反序列化 MetaFields 失败, entityId: {}, error: {}", entity.getId(), e.getMessage());
            }
        }

        //4. Парсинг конфигурации JSON десериализация
        if (StringUtils.isNotBlank(entity.getParserConfig())) {
            try {
                dto.setParserConfig(objectMapper.readValue(entity.getParserConfig(),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                        }));
            } catch (Exception e) {
                log.warn("反序列化 ParserConfig 失败, entityId: {}, error: {}", entity.getId(), e.getMessage());
            }
        }
        return dto;

    }

    /**
     * Синхронизация статуса документа с фактическим статусом RAG
     * Оптимизируйте логику синхронизации состояний, чтобы гарантировать, что состояние в разрешении может отображаться нормально
     * Обновляется до статуса завершения только в том случае, если документ имеет срезы и время парсинга превышает 30 секунд
     */
    /**
     * Синхронизация состояния документа с фактическим состоянием RAG (улучшено: поддерживает внешний входящий адаптер)
     */
    private void syncDocumentStatusWithRAG(KnowledgeFilesDTO dto, KnowledgeBaseAdapter adapter) {
        if (dto == null || StringUtils.isBlank(dto.getDocumentId()) || adapter == null) {
            return;
        }

        String documentId = dto.getDocumentId();
        String datasetId = dto.getDatasetId();

        try {
            //Используйте строгий тип ListReq с фильтрацией идентификаторов для получения статуса
            DocumentDTO.ListReq listReq = DocumentDTO.ListReq.builder()
                    .id(documentId)
                    .page(1)
                    .pageSize(1)
                    .build();

            PageData<KnowledgeFilesDTO> remoteList = adapter.getDocumentList(datasetId, listReq);

            if (remoteList != null && remoteList.getList() != null && !remoteList.getList().isEmpty()) {
                KnowledgeFilesDTO remoteDto = remoteList.getList().get(0);
                String remoteStatus = remoteDto.getStatus();

                //Логика определения выравнивания состояния ядра
                boolean statusChanged = remoteStatus != null && !remoteStatus.equals(dto.getStatus());
                boolean runChanged = remoteDto.getRun() != null && !remoteDto.getRun().equals(dto.getRun());
                boolean isProcessing = "RUNNING".equals(remoteDto.getRun()) || "UNSTART".equals(remoteDto.getRun());

                //Синхронизация до тех пор, пока статус изменился, или статус работы изменился, или файл все еще анализируется (прогресс промывки в реальном времени)
                if (statusChanged || runChanged || isProcessing) {
                    log.info("影子同步：状态变化={}，解析中={}，文档={}，最新状态={}，进度={}",
                            statusChanged, isProcessing, documentId, remoteStatus, remoteDto.getProgress());

                    //1. Синхронизация памяти DTO
                    dto.setStatus(remoteStatus);
                    dto.setRun(remoteDto.getRun());
                    dto.setProgress(remoteDto.getProgress());
                    dto.setChunkCount(remoteDto.getChunkCount());
                    dto.setTokenCount(remoteDto.getTokenCount());
                    dto.setError(remoteDto.getError());
                    dto.setProcessDuration(remoteDto.getProcessDuration());
                    dto.setThumbnail(remoteDto.getThumbnail());

                    //2. Синхронизация локальных теневых таблиц
                    UpdateWrapper<DocumentEntity> updateWrapper = new UpdateWrapper<DocumentEntity>()
                            .set("status", remoteStatus)
                            .set("run", remoteDto.getRun())
                            .set("progress", remoteDto.getProgress())
                            .set("chunk_count", remoteDto.getChunkCount())
                            .set("token_count", remoteDto.getTokenCount())
                            .set("error", remoteDto.getError())
                            .set("process_duration", remoteDto.getProcessDuration())
                            .set("thumbnail", remoteDto.getThumbnail())
                            .eq("document_id", documentId)
                            .eq("dataset_id", datasetId);

                    //Сериализованная синхронизация метаданных
                    if (remoteDto.getMetaFields() != null) {
                        try {
                            updateWrapper.set("meta_fields",
                                    objectMapper.writeValueAsString(remoteDto.getMetaFields()));
                        } catch (Exception e) {
                            log.warn("同步元数据序列化失败: {}", e.getMessage());
                        }
                    }

                    //Установите приоритет времени обновления на стороне Rag, чтобы избежать перезаписи времени бизнес-модификации при локальной синхронизации
                    Date lastUpdate = remoteDto.getUpdatedAt() != null ? remoteDto.getUpdatedAt() : new Date();
                    updateWrapper.set("updated_at", lastUpdate);
                    updateWrapper.set("last_sync_at", new Date()); //Запись времени синхронизации теневой библиотеки

                    documentDao.update(null, updateWrapper);
                }
            } else {
                //Проблема 6: Удаленный список пуст, либо документ был удален, либо возникла проблема с вызовом адаптера
                //[Исправление ошибки] P2: Признак отмены только в том случае, если пульт возвращает законный пустой список
                //В то же время обновите last_sync_at, чтобы взаимодействовать с механизмом охлаждения P1, чтобы предотвратить высокочастотное неправильное суждение
                log.warn("远程同步感知：RAGFlow 返回空文档列表, docId={}, 当前本地状态={}",
                        documentId, dto.getRun());
                dto.setRun("CANCEL");
                dto.setError("文档在远程服务中已被删除");

                documentDao.update(null, new UpdateWrapper<DocumentEntity>()
                        .set("run", "CANCEL")
                        .set("error", "文档在远程服务中已被删除")
                        .set("updated_at", new Date())
                        .set("last_sync_at", new Date())
                        .eq("document_id", documentId));
            }
        } catch (Exception e) {
            //[Исправление ошибки] P2: Не отмечайте отмену, когда вызов адаптера является ненормальным, избегайте ложных срабатываний из-за проблем с сетью/десериализацией
            //Только журнал, дождитесь следующего цикла синхронизации, чтобы повторить попытку
            log.warn("同步文档状态时适配器调用失败(不标记CANCEL), documentId: {}, error: {}",
                    documentId, e.getMessage());
        }
    }

    @Override
    public DocumentDTO.InfoVO getByDocumentId(String documentId, String datasetId) {
        if (StringUtils.isBlank(documentId) || StringUtils.isBlank(datasetId)) {
            throw new RenException(ErrorCode.RAG_DATASET_ID_AND_MODEL_ID_NOT_NULL);
        }

        log.info("=== 开始根据documentId获取文档 ===");
        log.info("documentId: {}, datasetId: {}", documentId, datasetId);

        try {
            //Получить конфигурацию Rag
            Map<String, Object> ragConfig = knowledgeBaseService.getRAGConfigByDatasetId(datasetId);

            //Тип адаптера извлечения
            String adapterType = extractAdapterType(ragConfig);

            //Получение экземпляра адаптера с помощью фабрики адаптеров
            KnowledgeBaseAdapter adapter = KnowledgeBaseAdapterFactory.getAdapter(adapterType, ragConfig);

            //Получение сведений о документе с помощью адаптера
            DocumentDTO.InfoVO info = adapter.getDocumentById(datasetId, documentId);

            if (info != null) {
                log.info("获取文档详情成功，documentId: {}", documentId);
                return info;
            } else {
                throw new RenException(ErrorCode.Knowledge_Base_RECORD_NOT_EXISTS);
            }

        } catch (Exception e) {
            log.error("根据documentId获取文档失败: {}", e.getMessage(), e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "null";
            if (e instanceof RenException) {
                throw (RenException) e;
            }
            throw new RenException(ErrorCode.RAG_API_ERROR, errorMessage);
        } finally {
            log.info("=== 根据documentId获取文档操作结束 ===");
        }
    }

    @Override
    public KnowledgeFilesDTO uploadDocument(String datasetId, MultipartFile file, String name,
            Map<String, Object> metaFields, String chunkMethod,
            Map<String, Object> parserConfig) {
        if (StringUtils.isBlank(datasetId) || file == null || file.isEmpty()) {
            throw new RenException(ErrorCode.PARAMS_GET_ERROR);
        }

        log.info("=== 开始文档上传操作 (强一致性优化) ===");

        //1. Приготовление (нетранзакционное)
        String fileName = StringUtils.isNotBlank(name) ? name : file.getOriginalFilename();
        if (StringUtils.isBlank(fileName)) {
            throw new RenException(ErrorCode.RAG_FILE_NAME_NOT_NULL);
        }

        log.info("1. 发起远程上传: datasetId={}, fileName={}", datasetId, fileName);

        //Получить адаптер (нетранзакционный)
        Map<String, Object> ragConfig = knowledgeBaseService.getRAGConfigByDatasetId(datasetId);
        KnowledgeBaseAdapter adapter = KnowledgeBaseAdapterFactory.getAdapter(extractAdapterType(ragConfig), ragConfig);

        //Построение строго типизированного запроса DTO
        DocumentDTO.UploadReq uploadReq = DocumentDTO.UploadReq.builder()
                .datasetId(datasetId)
                .file(file)
                .name(fileName)
                .metaFields(metaFields)
                .build();

        //Convert chunking method (String - > Enum)
        if (StringUtils.isNotBlank(chunkMethod)) {
            try {
                uploadReq.setChunkMethod(DocumentDTO.InfoVO.ChunkMethod.valueOf(chunkMethod.toUpperCase()));
            } catch (Exception e) {
                log.warn("无效的分块方法: {}, 将使用后台默认配置", chunkMethod);
            }
        }

        //Конфигурация разрешения преобразования (Map - > DTO)
        if (parserConfig != null && !parserConfig.isEmpty()) {
            uploadReq.setParserConfig(objectMapper.convertValue(parserConfig, DocumentDTO.InfoVO.ParserConfig.class));
        }

        //Выполнить удаленную загрузку (трудоемкий ввод-вывод, вне транзакции)
        KnowledgeFilesDTO result = adapter.uploadDocument(uploadReq);

        if (result == null || StringUtils.isBlank(result.getDocumentId())) {
            throw new RenException(ErrorCode.RAG_API_ERROR, "远程上传成功但未返回有效 DocumentID");
        }

        //2. Локальное сохранение (через самовызов для активации @ Transactional proxy)
        log.info("2. 同步保存本地影子记录: documentId={}", result.getDocumentId());
        self.saveDocumentShadow(datasetId, result, fileName, chunkMethod, parserConfig);

        log.info("=== 文档上传与影子记录保存成功 ===");
        return result;
    }

    /**
     * Атомизированное сохранение теневых записей (Upsert семантика)
     * Обновить, если document_id уже существует, вставить, если нет, избежать уникальных конфликтов ограничений
     *
     * @ return true = новая вставка, false = обновление существующей записи
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDocumentShadow(String datasetId, KnowledgeFilesDTO result, String originalName, String chunkMethod,
            Map<String, Object> parserConfig) {
        DocumentEntity entity = new DocumentEntity();
        entity.setDatasetId(datasetId);
        entity.setDocumentId(result.getDocumentId());
        entity.setName(StringUtils.isNotBlank(result.getName()) ? result.getName() : originalName);
        entity.setSize(result.getFileSize());
        entity.setType(getFileType(entity.getName()));
        entity.setChunkMethod(chunkMethod);

        if (parserConfig != null) {
            try {
                entity.setParserConfig(objectMapper.writeValueAsString(parserConfig));
            } catch (Exception e) {
                log.warn("序列化解析配置失败: {}", e.getMessage());
            }
        }

        entity.setStatus(result.getStatus() != null ? result.getStatus() : "1");
        entity.setRun(result.getRun());
        entity.setProgress(result.getProgress());
        entity.setThumbnail(result.getThumbnail());
        entity.setProcessDuration(result.getProcessDuration());
        entity.setSourceType(result.getSourceType());
        entity.setError(result.getError());
        entity.setChunkCount(result.getChunkCount());
        entity.setTokenCount(result.getTokenCount());
        entity.setEnabled(1);

        //persist metadata
        if (result.getMetaFields() != null) {
            try {
                entity.setMetaFields(objectMapper.writeValueAsString(result.getMetaFields()));
            } catch (Exception e) {
                log.warn("持久化影子元数据失败: {}", e.getMessage());
            }
        }

        //Установите приоритет синхронизации меток времени на стороне тряпки или используйте местное время, если нет
        entity.setCreatedAt(result.getCreatedAt() != null ? result.getCreatedAt() : new Date());
        entity.setUpdatedAt(result.getUpdatedAt() != null ? result.getUpdatedAt() : new Date());

        //Upsert: Проверьте, существует ли document_id, обновите, если он существует, вставьте, если его нет
        DocumentEntity existing = documentDao.selectOne(
                new QueryWrapper<DocumentEntity>().eq("document_id", entity.getDocumentId()));

        if (existing != null) {
            entity.setId(existing.getId());
            entity.setCreatedAt(existing.getCreatedAt()); //Сохранить исходное время создания
            documentDao.updateById(entity);
            log.info("影子记录已更新: documentId={}", entity.getDocumentId());
            return false;
        } else {
            documentDao.insert(entity);
            //Статистика по общему количеству документов датасета увеличивается при добавлении новых записей
            knowledgeBaseService.updateStatistics(datasetId, 1, 0L, 0L);
            log.info("影子记录已插入: documentId={}, datasetId={}", entity.getDocumentId(), datasetId);
            return true;
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void deleteDocuments(String datasetId, DocumentDTO.BatchIdReq req) {
        if (StringUtils.isBlank(datasetId) || req == null || req.getIds() == null || req.getIds().isEmpty()) {
            throw new RenException(ErrorCode.RAG_DATASET_ID_AND_MODEL_ID_NOT_NULL);
        }

        List<String> documentIds = req.getIds();
        log.info("=== 开始批量删除文档: datasetId={}, count={} ===", datasetId, documentIds.size());

        //1. Пакетные разрешения и предварительное подтверждение статуса
        List<DocumentEntity> entities = documentDao.selectList(
                new QueryWrapper<DocumentEntity>()
                        .eq("dataset_id", datasetId)
                        .in("document_id", documentIds));

        if (entities.size() != documentIds.size()) {
            log.warn("部分文档不存在或归属权异常: 预期={}, 实际={}", documentIds.size(), entities.size());
            throw new RenException(ErrorCode.NO_PERMISSION);
        }

        long totalChunkDelta = 0;
        long totalTokenDelta = 0;

        for (DocumentEntity entity : entities) {
            //Блокировать запросы на удаление для анализируемых документов
            //[Исправление ошибки] Поле запуска (running) должно использоваться при синтаксическом анализе вместо поля состояния
            //status = __ STR67 __ означает __ STR68 __, а не __ STR69 __
            if ("RUNNING".equals(entity.getRun())) {
                log.warn("拦截解析中文件的删除请求: docId={}", entity.getDocumentId());
                throw new RenException(ErrorCode.RAG_DOCUMENT_PARSING_DELETE_ERROR);
            }
            totalChunkDelta += entity.getChunkCount() != null ? entity.getChunkCount() : 0L;
            totalTokenDelta += entity.getTokenCount() != null ? entity.getTokenCount() : 0L;
        }

        //2. Получить адаптер (нетранзакционный)
        Map<String, Object> ragConfig = knowledgeBaseService.getRAGConfigByDatasetId(datasetId);
        KnowledgeBaseAdapter adapter = KnowledgeBaseAdapterFactory.getAdapter(extractAdapterType(ragConfig), ragConfig);

        //3. Выполните удаленное удаление
        try {
            adapter.deleteDocument(datasetId, req);
            log.info("远程批量删除请求成功");
        } catch (Exception e) {
            log.error("远程删除请求失败，中止本地清理以避免数据不一致: {}", e.getMessage());
            throw new RenException(e.getMessage());
        }

        //4. Атомизация очищает локальные теневые записи и синхронизирует статистику
        self.deleteDocumentShadows(documentIds, datasetId, totalChunkDelta, totalTokenDelta);

        //5. Очистить кэш
        try {
            String cacheKey = RedisKeys.getKnowledgeBaseCacheKey(datasetId);
            redisUtils.delete(cacheKey);
            log.info("已驱逐数据集缓存: {}", cacheKey);
        } catch (Exception e) {
            log.warn("驱逐 Redis 缓存失败: {}", e.getMessage());
        }

        log.info("=== 批量文档清理完成 ===");
    }

    /**
     * Массовая атомизация удаляет теневые записи и синхронизирует статистику родительской таблицы
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocumentShadows(List<String> documentIds, String datasetId, Long chunkDelta, Long tokenDelta) {
        //1. Запись о физическом удалении
        int deleted = documentDao.delete(
                new QueryWrapper<DocumentEntity>()
                        .eq("dataset_id", datasetId)
                        .in("document_id", documentIds));

        if (deleted > 0) {
            //2. Синхронное обновление статистики датасета
            knowledgeBaseService.updateStatistics(datasetId, -documentIds.size(), -chunkDelta, -tokenDelta);
            log.info("已同步扣减数据集统计: datasetId={}, chunks={}, tokens={}", datasetId, chunkDelta, tokenDelta);
        }
    }

    /**
     * Get File Types - Поддерживает четыре типа форматов документов Rag
     */
    private String getFileType(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            log.warn("文件名为空，返回unknown类型");
            return "unknown";
        }

        try {
            int lastDotIndex = fileName.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
                String extension = fileName.substring(lastDotIndex + 1).toLowerCase();

                //тип формата документа
                String[] documentTypes = { "pdf", "doc", "docx", "txt", "md", "mdx" };
                String[] spreadsheetTypes = { "csv", "xls", "xlsx" };
                String[] presentationTypes = { "ppt", "pptx" };

                //Проверить тип документа
                for (String type : documentTypes) {
                    if (type.equals(extension)) {
                        return "document";
                    }
                }

                //Проверить тип формы
                for (String type : spreadsheetTypes) {
                    if (type.equals(extension)) {
                        return "spreadsheet";
                    }
                }
                //Проверить тип слайда
                for (String type : presentationTypes) {
                    if (type.equals(extension)) {
                        return "presentation";
                    }
                }
                //возвращаем оригинальное расширение как тип файла
                return extension;
            }
            return "unknown";
        } catch (Exception e) {
            log.error("获取文件类型失败: ", e);
            return "unknown";
        }
    }

    /**
     * Извлеките тип адаптера из конфигурации тряпки
     */
    private String extractAdapterType(Map<String, Object> config) {
        if (config == null) {
            throw new RenException(ErrorCode.RAG_CONFIG_NOT_FOUND);
        }

        //Извлечение поля type из конфигурации
        String adapterType = (String) config.get("type");
        if (StringUtils.isBlank(adapterType)) {
            throw new RenException(ErrorCode.RAG_ADAPTER_TYPE_NOT_FOUND);
        }

        //Убедитесь, что тип адаптера зарегистрирован
        if (!KnowledgeBaseAdapterFactory.isAdapterTypeRegistered(adapterType)) {
            throw new RenException(ErrorCode.RAG_ADAPTER_TYPE_NOT_SUPPORTED, "适配器类型未注册: " + adapterType);
        }

        return adapterType;
    }

    @Override
    public boolean parseDocuments(String datasetId, List<String> documentIds) {
        if (StringUtils.isBlank(datasetId) || documentIds == null || documentIds.isEmpty()) {
            throw new RenException(ErrorCode.RAG_DATASET_ID_AND_MODEL_ID_NOT_NULL);
        }

        log.info("=== 开始解析文档（切块） ===");
        log.info("datasetId: {}, documentIds: {}", datasetId, documentIds);

        try {
            //Получить конфигурацию Rag
            Map<String, Object> ragConfig = knowledgeBaseService.getRAGConfigByDatasetId(datasetId);

            //Тип адаптера извлечения
            String adapterType = extractAdapterType(ragConfig);

            //Получить адаптер базы знаний
            KnowledgeBaseAdapter adapter = KnowledgeBaseAdapterFactory.getAdapter(adapterType, ragConfig);

            log.debug("解析文档参数: documentIds: {}", documentIds);

            //Вызов адаптера для разбора документа
            boolean result = adapter.parseDocuments(datasetId, documentIds);

            if (result) {
                log.info("文档解析命令发送成功，准备同步本地影子库状态，datasetId: {}, documentIds: {}", datasetId, documentIds);
                //Обновите статус локальной тени до Running и Parsing (1), как только инструкция будет успешно выполнена, обеспечив немедленную отправку списка Local-First
                documentDao.update(null, new UpdateWrapper<DocumentEntity>()
                        .set("run", "RUNNING")
                        .set("status", "1")
                        .set("updated_at", new Date())
                        .eq("dataset_id", datasetId)
                        .in("document_id", documentIds));

                log.info("文档本地状态已更新为 RUNNING");
            } else {
                log.error("文档解析失败，datasetId: {}, documentIds: {}", datasetId, documentIds);
                throw new RenException(ErrorCode.RAG_API_ERROR, "文档解析失败");
            }

            return result;

        } catch (Exception e) {
            log.error("解析文档失败: {}", e.getMessage(), e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "null";
            if (e instanceof RenException) {
                throw (RenException) e;
            }
            throw new RenException(ErrorCode.RAG_API_ERROR, errorMessage);
        } finally {
            log.info("=== 解析文档操作结束 ===");
        }
    }

    @Override
    public ChunkDTO.ListVO listChunks(String datasetId, String documentId, ChunkDTO.ListReq req) {
        if (StringUtils.isBlank(datasetId) || StringUtils.isBlank(documentId)) {
            throw new RenException(ErrorCode.RAG_DATASET_ID_AND_MODEL_ID_NOT_NULL);
        }

        log.info("=== 开始列出切片: datasetId={}, documentId={}, req={} ===", datasetId, documentId, req);

        try {
            Map<String, Object> ragConfig = knowledgeBaseService.getRAGConfigByDatasetId(datasetId);
            KnowledgeBaseAdapter adapter = KnowledgeBaseAdapterFactory.getAdapter(extractAdapterType(ragConfig),
                    ragConfig);

            ChunkDTO.ListVO result = adapter.listChunks(datasetId, documentId, req);
            log.info("切片列表获取成功: datasetId={}, total={}", datasetId, result.getTotal());
            return result;
        } catch (Exception e) {
            log.error("列出切片失败: {}", e.getMessage(), e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "null";
            if (e instanceof RenException) {
                throw (RenException) e;
            }
            throw new RenException(ErrorCode.RAG_API_ERROR, errorMessage);
        } finally {
            log.info("=== 列出切片操作结束 ===");
        }
    }

    @Override
    public RetrievalDTO.ResultVO retrievalTest(RetrievalDTO.TestReq req) {
        if (CollectionUtils.isEmpty(req.getDatasetIds())) {
            throw new RenException("未指定召回测试的知识库");
        }

        log.info("=== 开始召回测试: req={} ===", req);

        try {
            Map<String, Object> ragConfig = knowledgeBaseService.getRAGConfigByDatasetId(req.getDatasetIds().get(0));
            KnowledgeBaseAdapter adapter = KnowledgeBaseAdapterFactory.getAdapter(extractAdapterType(ragConfig),
                    ragConfig);

            RetrievalDTO.ResultVO result = adapter.retrievalTest(req);
            log.info("召回测试成功: total={}", result != null ? result.getTotal() : 0);
            return result;
        } catch (Exception e) {
            log.error("召回测试失败: {}", e.getMessage(), e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "null";
            if (e instanceof RenException) {
                throw (RenException) e;
            }
            throw new RenException(ErrorCode.RAG_API_ERROR, errorMessage);
        } finally {
            log.info("=== 召回测试操作结束 ===");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocumentsByDatasetId(String datasetId) {
        log.info("级联清理数据集文档: datasetId={}", datasetId);
        List<DocumentEntity> list = documentDao
                .selectList(new QueryWrapper<DocumentEntity>().eq("dataset_id", datasetId));
        if (list == null || list.isEmpty())
            return;

        List<String> docIds = list.stream().map(DocumentEntity::getDocumentId).toList();

        //Пакет вызывает существующую логику удаления (включая физическое удаление Rag)
        DocumentDTO.BatchIdReq req = DocumentDTO.BatchIdReq.builder().ids(docIds).build();
        this.deleteDocuments(datasetId, req);
    }

    @Override
    public int syncDocumentsFromRAG(String datasetId) {
        log.info("=== 开始从RAGFlow全量同步文档到本地影子表: datasetId={} ===", datasetId);

        //1. Получите адаптер
        Map<String, Object> ragConfig = knowledgeBaseService.getRAGConfigByDatasetId(datasetId);
        KnowledgeBaseAdapter adapter = KnowledgeBaseAdapterFactory.getAdapter(extractAdapterType(ragConfig), ragConfig);

        //2. Пагинация вытягивает все документы удаленно
        List<KnowledgeFilesDTO> allRemoteDocs = new ArrayList<>();
        int pageNum = 1;
        int pageSize = 100;
        long totalRemote = Long.MAX_VALUE;

        while ((long) (pageNum - 1) * pageSize < totalRemote) {
            DocumentDTO.ListReq req = DocumentDTO.ListReq.builder()
                    .page(pageNum)
                    .pageSize(pageSize)
                    .build();
            PageData<KnowledgeFilesDTO> remotePage = adapter.getDocumentList(datasetId, req);
            if (remotePage == null || remotePage.getList() == null || remotePage.getList().isEmpty()) {
                break;
            }
            allRemoteDocs.addAll(remotePage.getList());
            totalRemote = remotePage.getTotal();
            pageNum++;
        }

        //3. Получить существующие документы локально
        List<DocumentEntity> localDocs = documentDao.selectList(
                new QueryWrapper<DocumentEntity>().eq("dataset_id", datasetId));
        Set<String> localDocIds = localDocs.stream()
                .map(DocumentEntity::getDocumentId)
                .collect(Collectors.toSet());

        //4. Удаленный сбор идентификаторов документов
        Set<String> remoteDocIds = allRemoteDocs.stream()
                .map(KnowledgeFilesDTO::getDocumentId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        //5. Добавить: Вставьте документ, который существует удаленно, но отсутствует локально
        List<KnowledgeFilesDTO> newDocs = allRemoteDocs.stream()
                .filter(doc -> doc.getDocumentId() != null && !localDocIds.contains(doc.getDocumentId()))
                .collect(Collectors.toList());

        int syncCount = 0;
        if (!newDocs.isEmpty()) {
            for (KnowledgeFilesDTO doc : newDocs) {
                try {
                    self.saveDocumentShadow(datasetId, doc, doc.getName(), doc.getChunkMethod(), doc.getParserConfig());
                    //Синхронизация статистики существующего удаленного токена/чанка
                    Long tokenCount = doc.getTokenCount() != null ? doc.getTokenCount() : 0L;
                    long chunkCount = doc.getChunkCount() != null ? doc.getChunkCount().longValue() : 0L;
                    if (tokenCount > 0 || chunkCount > 0) {
                        knowledgeBaseService.updateStatistics(datasetId, 0, chunkCount, tokenCount);
                    }
                    syncCount++;
                } catch (Exception e) {
                    log.warn("同步单个文档影子记录失败: docId={}, error={}", doc.getDocumentId(), e.getMessage());
                }
            }
            log.info("从RAGFlow新增同步 {} 个文档影子记录, datasetId={}", syncCount, datasetId);
        }

        //6. Очистка: удаление теневых записей, которые больше не существуют удаленно, но остаются локально
        List<DocumentEntity> deletedDocs = localDocs.stream()
                .filter(entity -> !remoteDocIds.contains(entity.getDocumentId()))
                .collect(Collectors.toList());

        if (!deletedDocs.isEmpty()) {
            List<String> deletedDocIds = new ArrayList<>();
            long totalChunkDelta = 0;
            long totalTokenDelta = 0;

            for (DocumentEntity entity : deletedDocs) {
                deletedDocIds.add(entity.getDocumentId());
                totalChunkDelta += entity.getChunkCount() != null ? entity.getChunkCount() : 0L;
                totalTokenDelta += entity.getTokenCount() != null ? entity.getTokenCount() : 0L;
            }
            try {
                self.deleteDocumentShadows(deletedDocIds, datasetId, totalChunkDelta, totalTokenDelta);
                log.info("清理远端已删除的影子记录: {} 个, datasetId={}", deletedDocs.size(), datasetId);
            } catch (Exception e) {
                log.warn("清理远端已删除的影子记录失败: datasetId={}, error={}", datasetId, e.getMessage());
            }
        }

        //7. Полное обновление: документы, которые существуют как удаленно, так и локально, синхронизируют все поля удаленно
        //Обработка сценариев, таких как повторная передача documentId повторного использования RAGFlow, изменения метаданных после удаленного редактирования и т. д.
        Map<String, KnowledgeFilesDTO> remoteDocMap = allRemoteDocs.stream()
                .filter(doc -> doc.getDocumentId() != null)
                .collect(Collectors.toMap(KnowledgeFilesDTO::getDocumentId, doc -> doc, (a, b) -> b));

        Map<String, DocumentEntity> localDocMap = localDocs.stream()
                .collect(Collectors.toMap(DocumentEntity::getDocumentId, e -> e, (a, b) -> b));

        int updateCount = 0;
        for (Map.Entry<String, KnowledgeFilesDTO> entry : remoteDocMap.entrySet()) {
            String docId = entry.getKey();
            DocumentEntity local = localDocMap.get(docId);
            if (local == null) {
                continue; //Не локально, обрабатывается этапом 5
            }
            KnowledgeFilesDTO remote = entry.getValue();

            //Полное обновление поля (в зависимости от того, что удалено) для обеспечения полной согласованности с RAGFlow локально
            UpdateWrapper<DocumentEntity> updateWrapper = new UpdateWrapper<DocumentEntity>()
                    .set("run", remote.getRun())
                    .set("status", remote.getStatus() != null ? remote.getStatus() : local.getStatus())
                    .set("progress", remote.getProgress())
                    .set("chunk_count", remote.getChunkCount())
                    .set("token_count", remote.getTokenCount())
                    .set("size", remote.getFileSize())
                    .set("error", remote.getError())
                    .set("process_duration", remote.getProcessDuration())
                    .set("updated_at", new Date())
                    .set("last_sync_at", new Date())
                    .eq("document_id", docId)
                    .eq("dataset_id", datasetId);

            if (remote.getName() != null) {
                updateWrapper.set("name", remote.getName());
            }
            if (remote.getThumbnail() != null) {
                updateWrapper.set("thumbnail", remote.getThumbnail());
            }
            if (remote.getMetaFields() != null) {
                try {
                    updateWrapper.set("meta_fields", objectMapper.writeValueAsString(remote.getMetaFields()));
                } catch (Exception e) {
                    log.warn("同步更新元数据序列化失败: docId={}, error={}", docId, e.getMessage());
                }
            }

            documentDao.update(null, updateWrapper);

            //Разница в статистике синхронизации (правильная родительская таблица при изменении количества чанков/токенов)
            Long remoteTokenCount = remote.getTokenCount() != null ? remote.getTokenCount() : 0L;
            Long localTokenCount = local.getTokenCount() != null ? local.getTokenCount() : 0L;
            long remoteChunkCount = remote.getChunkCount() != null ? remote.getChunkCount().longValue() : 0L;
            long localChunkCount = local.getChunkCount() != null ? local.getChunkCount().longValue() : 0L;
            long tokenDelta = remoteTokenCount - localTokenCount;
            long chunkDelta = remoteChunkCount - localChunkCount;
            if (tokenDelta != 0 || chunkDelta != 0) {
                knowledgeBaseService.updateStatistics(datasetId, 0, chunkDelta, tokenDelta);
                log.info("影子更新: 修正知识库统计, docId={}, chunkDelta={}, tokenDelta={}", docId, chunkDelta, tokenDelta);
            }

            updateCount++;
        }

        if (syncCount == 0 && deletedDocs.isEmpty() && updateCount == 0) {
            log.info("本地影子表已与RAGFlow完全同步, datasetId={}", datasetId);
        } else {
            log.info("同步完成: 新增={}, 清理={}, 更新={}, datasetId={}", syncCount, deletedDocs.size(), updateCount, datasetId);
        }

        return syncCount;
    }

    @Override
    public void syncRunningDocuments() {
        //1. Запрос всех текущих документов о состоянии
        List<DocumentEntity> runningDocs = documentDao.selectList(
                new QueryWrapper<DocumentEntity>()
                        .eq("run", "RUNNING")
                        .eq("status", "1") //Синхронизировать только документы
        );

        if (runningDocs == null || runningDocs.isEmpty()) {
            return;
        }

        log.info("定时任务: 发现 {} 个文档正在解析中，开始同步...", runningDocs.size());

        //2. Группировка по DatasetID, повторное использование адаптера
        Map<String, List<DocumentEntity>> groupedDocs = runningDocs.stream()
                .collect(Collectors.groupingBy(DocumentEntity::getDatasetId));

        groupedDocs.forEach((datasetId, docs) -> {
            KnowledgeBaseAdapter adapter = null;
            try {
                //Инициализация адаптера (каждый набор данных инициализируется только один раз)
                Map<String, Object> ragConfig = knowledgeBaseService.getRAGConfigByDatasetId(datasetId);
                adapter = KnowledgeBaseAdapterFactory.getAdapter(extractAdapterType(ragConfig), ragConfig);
            } catch (Exception e) {
                log.warn("无法为数据集 {} 初始化适配器，跳过同步: {}", datasetId, e.getMessage());
                return;
            }

            for (DocumentEntity doc : docs) {
                try {
                    //Построить временный DTO к методу синхронизации
                    KnowledgeFilesDTO dto = convertEntityToDTO(doc);
                    //Записываем количество токенов перед синхронизацией
                    Long oldTokenCount = dto.getTokenCount() != null ? dto.getTokenCount() : 0L;

                    syncDocumentStatusWithRAG(dto, adapter);

                    //3. [критические исправления] Рассчитать приращения и обновить статистику базы знаний
                    Long newTokenCount = dto.getTokenCount() != null ? dto.getTokenCount() : 0L;
                    Long tokenDelta = newTokenCount - oldTokenCount;

                    //обновляем статистику только если статус меняется НА Success И меняется количество токенов
                    if (tokenDelta != 0) {
                        knowledgeBaseService.updateStatistics(datasetId, 0, 0L, tokenDelta);
                        log.info("定时任务: 同步修正知识库统计, docId={}, tokenDelta={}", dto.getDocumentId(), tokenDelta);
                    }
                } catch (Exception e) {
                    log.error("同步文档 {} 失败: {}", doc.getDocumentId(), e.getMessage());
                }
            }
        });
    }
}