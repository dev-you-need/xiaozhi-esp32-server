package xiaozhi.modules.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.BeanUtils;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.page.PageData;
import xiaozhi.common.redis.RedisKeys;
import xiaozhi.common.redis.RedisUtils;
import xiaozhi.common.service.impl.BaseServiceImpl;
import xiaozhi.common.utils.ConvertUtils;
import xiaozhi.common.utils.JsonUtils;
import xiaozhi.modules.knowledge.dao.KnowledgeBaseDao;
import xiaozhi.modules.knowledge.dao.DocumentDao;
import xiaozhi.modules.knowledge.entity.DocumentEntity;
import xiaozhi.modules.knowledge.dto.KnowledgeBaseDTO;
import xiaozhi.modules.knowledge.dto.dataset.DatasetDTO;
import xiaozhi.modules.knowledge.entity.KnowledgeBaseEntity;
import xiaozhi.modules.knowledge.rag.KnowledgeBaseAdapter;
import xiaozhi.modules.knowledge.rag.KnowledgeBaseAdapterFactory;
import xiaozhi.modules.knowledge.service.KnowledgeBaseService;
import xiaozhi.modules.model.dao.ModelConfigDao;
import xiaozhi.modules.model.entity.ModelConfigEntity;
import xiaozhi.modules.model.service.ModelConfigService;
import xiaozhi.modules.security.user.SecurityUser;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Класс реализации услуг базы знаний (рефакторинг)
 * Встроенный адаптер RAGFlow с режимом Shadow DB
 */
@Service
@AllArgsConstructor
@Slf4j
public class KnowledgeBaseServiceImpl extends BaseServiceImpl<KnowledgeBaseDao, KnowledgeBaseEntity>
        implements KnowledgeBaseService {

    private final KnowledgeBaseDao knowledgeBaseDao;
    private final DocumentDao documentDao;
    private final ModelConfigService modelConfigService;
    private final ModelConfigDao modelConfigDao;
    private final RedisUtils redisUtils;

    @Override
    public PageData<KnowledgeBaseDTO> getPageList(KnowledgeBaseDTO knowledgeBaseDTO, Integer page, Integer limit) {
        Page<KnowledgeBaseEntity> pageInfo = new Page<>(page, limit);
        QueryWrapper<KnowledgeBaseEntity> queryWrapper = new QueryWrapper<>();

        if (knowledgeBaseDTO != null) {
            queryWrapper.like(StringUtils.isNotBlank(knowledgeBaseDTO.getName()), "name", knowledgeBaseDTO.getName());
            queryWrapper.eq(knowledgeBaseDTO.getStatus() != null, "status", knowledgeBaseDTO.getStatus());
            queryWrapper.eq("creator", knowledgeBaseDTO.getCreator());
        }
        queryWrapper.orderByDesc("created_at");

        IPage<KnowledgeBaseEntity> iPage = knowledgeBaseDao.selectPage(pageInfo, queryWrapper);
        PageData<KnowledgeBaseDTO> pageData = getPageData(iPage, KnowledgeBaseDTO.class);

        // Enrich with Document Count from RAG (Optional / Lazy)
        if (pageData != null && pageData.getList() != null) {
            pageData.getList().removeIf(dto -> {
                enrichDocumentCount(dto);
                //syncDatasetFromRAG очищает локальные записи, когда обнаруживает, что сторона RAGFlow была удалена
                //DatasetId пуст в качестве маркера и должен быть удален из списка
                return dto.getDatasetId() == null;
            });
        }
        return pageData;
    }

    private void enrichDocumentCount(KnowledgeBaseDTO dto) {
        syncDatasetFromRAG(dto);
    }

    /**
     * Синхронизация информации о наборе данных из RAGFlow: обнаружение удаления, имя/профиль синхронизации, получение количества документов
     * Запрос RAGFlow в режиме реального времени каждый раз при обновлении списка, чтобы обеспечить немедленную осведомленность об удаленных изменениях
     */
    private void syncDatasetFromRAG(KnowledgeBaseDTO dto) {
        try {
            if (StringUtils.isBlank(dto.getDatasetId()) || StringUtils.isBlank(dto.getRagModelId())) {
                return;
            }

            KnowledgeBaseAdapter adapter = getAdapterByModelId(dto.getRagModelId());
            if (adapter == null) {
                return;
            }

            DatasetDTO.InfoVO datasetInfo = adapter.getDatasetInfo(dto.getDatasetId());
            //getDatasetInfo обычно возвращает значение null, указывающее на то, что пульт не существует; исключение RenException, вызванное исключением, перехватывается внешней ловушкой
            if (datasetInfo == null) {
                //Сторона RAGFlow подтвердила удаление → локальной каскадной очистки
                log.info("数据集 {} 在 RAGFlow 端不存在，执行本地清理", dto.getDatasetId());
                cleanupLocalDataset(dto.getDatasetId(), dto.getId());
                //Отметить как удаленный, пусть родитель будет удален из списка
                dto.setDatasetId(null);
                return;
            }

            //Имя синхронизации (без префикса username_prefix)
            String ragflowName = datasetInfo.getName();
            if (StringUtils.isNotBlank(ragflowName)) {
                String localName = ragflowName.contains("_") ? ragflowName.substring(ragflowName.indexOf('_') + 1) : ragflowName;
                if (!localName.equals(dto.getName())) {
                    log.info("同步知识库名称: {} -> {}", dto.getName(), localName);
                    KnowledgeBaseEntity entity = knowledgeBaseDao.selectById(dto.getId());
                    if (entity != null) {
                        entity.setName(localName);
                        knowledgeBaseDao.updateById(entity);
                        dto.setName(localName);
                    }
                }
            }

            //Введение в синхронизацию
            String ragflowDesc = datasetInfo.getDescription();
            String localDesc = dto.getDescription();
            boolean descChanged = (ragflowDesc == null && localDesc != null) || (ragflowDesc != null && !ragflowDesc.equals(localDesc));
            if (descChanged) {
                log.info("同步知识库简介: datasetId={}", dto.getDatasetId());
                KnowledgeBaseEntity entity = knowledgeBaseDao.selectById(dto.getId());
                if (entity != null) {
                    entity.setDescription(ragflowDesc);
                    knowledgeBaseDao.updateById(entity);
                    dto.setDescription(ragflowDesc);
                }
            }

            //Устанавливаем количество документов (сохраняем исходную функцию)
            if (datasetInfo.getDocumentCount() != null) {
                dto.setDocumentCount(datasetInfo.getDocumentCount().intValue());
            }

        } catch (Exception e) {
            log.error("同步数据集信息失败 {}: {}", dto.getName(), e.getMessage());
            dto.setDocumentCount(0);
            dto.setErrorMessage(e.getMessage());
        }
    }

    /**
     * Локальная каскадная очистка: очистка всех локальных связанных данных после удаления стороны RAGFlow
     * Без вызова API удаления RAGFlow
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanupLocalDataset(String datasetId, String entityId) {
        try {
            //1. Удаление теневой записи документа
            documentDao.delete(new QueryWrapper<DocumentEntity>().eq("dataset_id", datasetId));
            //2. Удаление сопоставлений плагинов
            knowledgeBaseDao.deletePluginMappingByKnowledgeBaseId(entityId);
            //3. Удалить запись базы знаний
            knowledgeBaseDao.deleteById(entityId);
            //4. Очистить кэш
            redisUtils.delete(RedisKeys.getKnowledgeBaseCacheKey(entityId));
            log.info("本地级联清理完成: datasetId={}, entityId={}", datasetId, entityId);
        } catch (Exception e) {
            log.error("本地级联清理失败: datasetId={}, entityId={}", datasetId, entityId, e);
        }
    }

    @Override
    public KnowledgeBaseDTO getById(String id) {
        KnowledgeBaseEntity entity = knowledgeBaseDao.selectById(id);
        if (entity == null) {
            throw new RenException(ErrorCode.Knowledge_Base_RECORD_NOT_EXISTS);
        }
        return ConvertUtils.sourceToTarget(entity, KnowledgeBaseDTO.class);
    }

    @Override
    public KnowledgeBaseDTO getByDatasetId(String datasetId) {
        if (StringUtils.isBlank(datasetId)) {
            throw new RenException(ErrorCode.PARAMS_GET_ERROR);
        }
        //[Production Fix] Поиск совместимости: приоритетный поиск по dataset_id, не удается найти поиск по идентификатору первичного ключа, убедитесь, что любой UUID, переданный во внешний интерфейс, может попасть
        KnowledgeBaseEntity entity = knowledgeBaseDao
                .selectOne(new QueryWrapper<KnowledgeBaseEntity>()
                        .eq("dataset_id", datasetId)
                        .or()
                        .eq("id", datasetId));
        if (entity == null) {
            throw new RenException(ErrorCode.Knowledge_Base_RECORD_NOT_EXISTS);
        }
        return ConvertUtils.sourceToTarget(entity, KnowledgeBaseDTO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseDTO save(KnowledgeBaseDTO dto) {
        // 1. Validation
        checkDuplicateName(dto.getName(), null);
        KnowledgeBaseAdapter adapter = null;

        // 2. RAG Creation
        String datasetId = null;
        try {
            //Если не указана модель Rag, автоматически используется системный дефолт
            if (StringUtils.isBlank(dto.getRagModelId())) {
                List<ModelConfigEntity> models = getRAGModels();
                if (models != null && !models.isEmpty()) {
                    dto.setRagModelId(models.get(0).getId());
                } else {
                    throw new RenException(ErrorCode.RAG_CONFIG_NOT_FOUND, "未指定且无可用默认 RAG 模型");
                }
            }

            Map<String, Object> ragConfig = getValidatedRAGConfig(dto.getRagModelId());
            adapter = KnowledgeBaseAdapterFactory.getAdapter((String) ragConfig.get("type"),
                    ragConfig);

            DatasetDTO.CreateReq createReq = ConvertUtils.sourceToTarget(dto, DatasetDTO.CreateReq.class);
            createReq.setName(SecurityUser.getUser().getUsername() + "_" + dto.getName());

            DatasetDTO.InfoVO ragResponse = adapter.createDataset(createReq);
            if (ragResponse == null || StringUtils.isBlank(ragResponse.getId())) {
                throw new RenException(ErrorCode.RAG_API_ERROR, "RAG创建返回无效: 缺失ID");
            }
            datasetId = ragResponse.getId();

            // 3. Local Save (Shadow)
            KnowledgeBaseEntity entity = ConvertUtils.sourceToTarget(dto, KnowledgeBaseEntity.class);

            //[Production Fix] Унифицируйте локальный идентификатор и идентификатор RAGFlow, чтобы предотвратить путаницу идентификатора при вызовах/удалении или/обновлении интерфейса (локальный
            //UUID против Rag UUID), что привело к ошибке 10163
            entity.setId(datasetId);
            entity.setDatasetId(datasetId);
            entity.setStatus(1); // Default Enabled

            //✅полное сохранение: строго полная обратная запись (требование пользователя)
            //Используйте сильно типизированные свойства DTO для получения, больше не анализируйте вручную ключ из карты
            entity.setTenantId(ragResponse.getTenantId());
            entity.setChunkMethod(ragResponse.getChunkMethod());
            entity.setEmbeddingModel(ragResponse.getEmbeddingModel());
            entity.setPermission(ragResponse.getPermission());

            if (StringUtils.isBlank(entity.getAvatar())) {
                entity.setAvatar(ragResponse.getAvatar());
            }

            // Parse Config (JSON)
            if (ragResponse.getParserConfig() != null) {
                entity.setParserConfig(JsonUtils.toJsonString(ragResponse.getParserConfig()));
            }

            // Numeric fields
            entity.setChunkCount(ragResponse.getChunkCount() != null ? ragResponse.getChunkCount() : 0L);
            entity.setDocumentCount(ragResponse.getDocumentCount() != null ? ragResponse.getDocumentCount() : 0L);
            entity.setTokenNum(ragResponse.getTokenNum() != null ? ragResponse.getTokenNum() : 0L);

            //Пустой Creator/Updater и пусть FieldMetaObjectHandler автозаполнение от SecurityUser
            //ConvertUtils скопирует creator = 0 в DTO, в результате чего strictInsertFill пропустит заполнение
            entity.setCreator(null);
            entity.setUpdater(null);

            knowledgeBaseDao.insert(entity);
            return ConvertUtils.sourceToTarget(entity, KnowledgeBaseDTO.class);
        } catch (Exception e) {
            log.error("RAG创建或本地保存失败", e);
            //Если datasetId был сгенерирован, но не удалось сохранить локально, попробуйте откатить Rag (Best Effort)
            if (StringUtils.isNotBlank(datasetId)) {
                try {
                    if (adapter != null)
                        adapter.deleteDataset(
                                DatasetDTO.BatchIdReq.builder().ids(Collections.singletonList(datasetId)).build());
                } catch (Exception rollbackEx) {
                    log.error("RAG回滚失败: {}", datasetId, rollbackEx);
                }
            }
            if (e instanceof RenException) {
                throw (RenException) e;
            }
            throw new RenException(ErrorCode.RAG_API_ERROR, "创建知识库失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("deprecation")
    public KnowledgeBaseDTO update(KnowledgeBaseDTO dto) {
        log.info("Update Service Called: ID={}, DatasetID={}", dto.getId(), dto.getDatasetId());
        KnowledgeBaseEntity entity = knowledgeBaseDao.selectById(dto.getId());
        if (entity == null) {
            log.error("Update failed: Entity not found for ID={}", dto.getId());
            throw new RenException(ErrorCode.Knowledge_Base_RECORD_NOT_EXISTS);
        }

        checkDuplicateName(dto.getName(), dto.getId());

        //Проверка конфликта идентификатора набора данных с другими записями
        if (StringUtils.isNotBlank(dto.getDatasetId())) {
            KnowledgeBaseEntity conflictEntity = knowledgeBaseDao.selectOne(
                    new QueryWrapper<KnowledgeBaseEntity>()
                            .eq("dataset_id", dto.getDatasetId())
                            .ne("id", dto.getId()));
            if (conflictEntity != null) {
                throw new RenException(ErrorCode.DB_RECORD_EXISTS);
            }
        }

        // RAG Update if needed
        if (StringUtils.isNotBlank(entity.getDatasetId()) && StringUtils.isNotBlank(dto.getRagModelId())) {
            try {
                //🤖AUTO-FILL: Если DTO не передает идентификатор ragModelId (редко), попробуйте повторно использовать
                if (StringUtils.isBlank(dto.getRagModelId())) {
                    dto.setRagModelId(entity.getRagModelId());
                }

                //[FIX] Smart Completion: Если поле ключа в DTO пусто, используется старое значение в Entity
                //Убедитесь, что запрос, отправленный в RAGFlow, содержит все необходимые поля (поддержка частичного обновления)
                if (StringUtils.isBlank(dto.getPermission())) {
                    dto.setPermission(entity.getPermission());
                }
                if (StringUtils.isBlank(dto.getChunkMethod())) {
                    dto.setChunkMethod(entity.getChunkMethod());
                }

                KnowledgeBaseAdapter adapter = getAdapterByModelId(dto.getRagModelId());
                if (adapter != null) {
                    DatasetDTO.UpdateReq updateReq = ConvertUtils.sourceToTarget(dto, DatasetDTO.UpdateReq.class);

                    //1. Обработка обязательного/основного префикса поля
                    if (StringUtils.isNotBlank(dto.getName())) {
                        updateReq.setName(SecurityUser.getUser().getUsername() + "_" + dto.getName());
                    }

                    //2. Поддержка конфигурации парсера (если в DTO есть конфигурация в виде строки, попробуйте конвертировать, но предпочтительнее DTOization)
                    if (StringUtils.isNotBlank(dto.getParserConfig())) {
                        try {
                            DatasetDTO.ParserConfig parserConfig = JsonUtils.parseObject(dto.getParserConfig(),
                                    DatasetDTO.ParserConfig.class);
                            updateReq.setParserConfig(parserConfig);
                        } catch (Exception e) {
                            log.warn("解析 parser_config 失败，跳过同步", e);
                        }
                    }

                    adapter.updateDataset(entity.getDatasetId(), updateReq);
                    log.info("RAG更新成功: {}", entity.getDatasetId());
                }
            } catch (Exception e) {
                log.error("RAG更新失败", e);
                //Восстановление согласованности транзакций: общий откат при сбое Rag
                if (e instanceof RenException) {
                    throw (RenException) e;
                }
                throw new RenException(ErrorCode.RAG_API_ERROR, "RAG更新失败: " + e.getMessage());
            }
        }

        BeanUtils.copyProperties(dto, entity);
        knowledgeBaseDao.updateById(entity);

        // Clean cache
        redisUtils.delete(RedisKeys.getKnowledgeBaseCacheKey(entity.getId()));

        return ConvertUtils.sourceToTarget(entity, KnowledgeBaseDTO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByDatasetId(String datasetId) {
        if (StringUtils.isBlank(datasetId)) {
            throw new RenException(ErrorCode.PARAMS_GET_ERROR);
        }

        KnowledgeBaseEntity entity = knowledgeBaseDao
                .selectOne(new QueryWrapper<KnowledgeBaseEntity>().eq("dataset_id", datasetId));

        //1. Возобновить проверку 404: исключение броска записи не найдено
        if (entity == null) {
            log.warn("记录不存在，datasetId: {}", datasetId);
            throw new RenException(ErrorCode.Knowledge_Base_RECORD_NOT_EXISTS);
        }
        log.info("找到记录: ID={}, datasetId={}, ragModelId={}",
                entity.getId(), entity.getDatasetId(), entity.getRagModelId());

        // 2. RAG Delete (Strict Mode)
        //Восстановление строгой согласованности: ошибка удаления Rag вызывает исключение, запускает откат транзакции, не позволяет удалять локальные, но удаленные грязные данные
        boolean apiDeleteSuccess = false;
        if (StringUtils.isNotBlank(entity.getRagModelId()) && StringUtils.isNotBlank(entity.getDatasetId())) {
            try {
                KnowledgeBaseAdapter adapter = getAdapterByModelId(entity.getRagModelId());
                if (adapter != null) {
                    adapter.deleteDataset(
                            DatasetDTO.BatchIdReq.builder().ids(Collections.singletonList(datasetId)).build());
                }
                apiDeleteSuccess = true;
            } catch (Exception e) {
                log.error("RAG删除失败，触发回滚", e);
                if (e instanceof RenException) {
                    throw (RenException) e;
                }
                throw new RenException(ErrorCode.RAG_API_ERROR, "RAG删除失败: " + e.getMessage());
            }
        } else {
            log.warn("datasetId或ragModelId为空，跳过RAG删除");
            apiDeleteSuccess = true; //Нет набора данных Rag, считается успешным
        }

        // 3. Local Delete (Safe Order)
        //Восстановите правильный порядок: сначала удалите подтаблицу (Plugin Mapping), затем удалите основную таблицу (Entity)
        if (apiDeleteSuccess) {
            log.info("开始删除ai_agent_plugin_mapping表中与知识库ID '{}' 相关的映射记录", entity.getId());
            log.info("开始删除关联数据, entityId: {}", entity.getId());
            knowledgeBaseDao.deletePluginMappingByKnowledgeBaseId(entity.getId());
            log.info("插件映射记录删除完成");
            int deleteCount = knowledgeBaseDao.deleteById(entity.getId());
            log.info("本地数据库删除结果: {}", deleteCount > 0 ? "成功" : "失败");
            redisUtils.delete(RedisKeys.getKnowledgeBaseCacheKey(entity.getId()));
        }
    }

    @Override
    public List<KnowledgeBaseDTO> getByDatasetIdList(List<String> datasetIdList) {
        if (datasetIdList == null || datasetIdList.isEmpty()) {
            return Collections.emptyList();
        }
        //[Production Fix] Поиск массовой совместимости
        QueryWrapper<KnowledgeBaseEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("dataset_id", datasetIdList).or().in("id", datasetIdList);
        List<KnowledgeBaseEntity> list = knowledgeBaseDao.selectList(queryWrapper);
        return ConvertUtils.sourceToTarget(list, KnowledgeBaseDTO.class);
    }

    @Override
    public Map<String, Object> getRAGConfig(String ragModelId) {
        return getValidatedRAGConfig(ragModelId);
    }

    @Override
    public Map<String, Object> getRAGConfigByDatasetId(String datasetId) {
        KnowledgeBaseEntity entity = knowledgeBaseDao
                .selectOne(new QueryWrapper<KnowledgeBaseEntity>().eq("dataset_id", datasetId));
        if (entity == null || StringUtils.isBlank(entity.getRagModelId())) {
            throw new RenException(ErrorCode.RAG_CONFIG_NOT_FOUND);
        }
        return getRAGConfig(entity.getRagModelId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatistics(String datasetId, Integer docDelta, Long chunkDelta, Long tokenDelta) {
        log.info("递增更新知识库统计: datasetId={}, docs={}, chunks={}, tokens={}", datasetId, docDelta, chunkDelta, tokenDelta);
        knowledgeBaseDao.updateStatsAfterChange(datasetId, docDelta, chunkDelta, tokenDelta);
    }

    @Override
    public List<ModelConfigEntity> getRAGModels() {
        return modelConfigDao.selectList(new QueryWrapper<ModelConfigEntity>()
                .select("id", "model_name", "config_json") // Explicitly select needed fields
                .eq("model_type", Constant.RAG_CONFIG_TYPE)
                .eq("is_enabled", 1)
                .orderByDesc("is_default")
                .orderByDesc("create_date"));
    }

    // --- Helpers ---

    private void checkDuplicateName(String name, String excludeId) {
        if (StringUtils.isBlank(name))
            return;
        QueryWrapper<KnowledgeBaseEntity> qw = new QueryWrapper<>();
        qw.eq("name", name).eq("creator", SecurityUser.getUserId());
        if (excludeId != null)
            qw.ne("id", excludeId);
        if (knowledgeBaseDao.selectCount(qw) > 0) {
            throw new RenException(ErrorCode.KNOWLEDGE_BASE_NAME_EXISTS);
        }
    }

    private KnowledgeBaseAdapter getAdapterByModelId(String modelId) {
        Map<String, Object> config = getValidatedRAGConfig(modelId);
        return KnowledgeBaseAdapterFactory.getAdapter((String) config.get("type"), config);
    }

    private Map<String, Object> getValidatedRAGConfig(String modelId) {
        ModelConfigEntity configEntity = modelConfigService.getModelByIdFromCache(modelId);
        if (configEntity == null || configEntity.getConfigJson() == null) {
            throw new RenException(ErrorCode.RAG_CONFIG_NOT_FOUND);
        }
        Map<String, Object> config = new HashMap<>(configEntity.getConfigJson());
        if (!config.containsKey("type")) {
            config.put("type", "ragflow");
        }
        return config;
    }
}