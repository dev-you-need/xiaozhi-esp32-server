package xiaozhi.modules.knowledge.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import xiaozhi.common.page.PageData;
import xiaozhi.modules.knowledge.dto.KnowledgeFilesDTO;
import xiaozhi.modules.knowledge.dto.document.ChunkDTO;
import xiaozhi.modules.knowledge.dto.document.RetrievalDTO;
import xiaozhi.modules.knowledge.dto.document.DocumentDTO;

/**
 * Интерфейс сервиса документов базы знаний
 */
public interface KnowledgeFilesService {

        /**
         * Постраничный запрос списка документов
         * 
         * @param knowledgeFilesDTO условия поиска
         * @param page              номер страницы
         * @param limit             количество на странице
         * @return постраничные данные
         */
        PageData<KnowledgeFilesDTO> getPageList(KnowledgeFilesDTO knowledgeFilesDTO, Integer page, Integer limit);

        /**
         * Получить информацию о документе по ID документа и ID базы знаний
         * 
         * @param documentId ID документа
         * @param datasetId  ID базы знаний
         * @return информация о документе (строго типизированный InfoVO)
         */
        DocumentDTO.InfoVO getByDocumentId(String documentId, String datasetId);

        /**
         * Загрузить документ в базу знаний
         * 
         * @param datasetId    ID базы знаний
         * @param file         загружаемый файл
         * @param name         название документа
         * @param metaFields   поля метаданных
         * @param chunkMethod  метод разбиения на блоки
         * @param parserConfig конфигурация парсера
         * @return информация о загруженном документе
         */
        KnowledgeFilesDTO uploadDocument(String datasetId, MultipartFile file, String name,
                        Map<String, Object> metaFields, String chunkMethod,
                        Map<String, Object> parserConfig);

        /**
         * Пакетное удаление документов
         * 
         * @param datasetId ID базы знаний
         * @param req       параметры запроса на удаление (список ID документов)
         */
        void deleteDocuments(String datasetId, DocumentDTO.BatchIdReq req);

        /**
         * Получить информацию о конфигурации RAG
         * 
         * @param ragModelId ID конфигурации модели RAG
         * @return информация о конфигурации RAG
         */
        Map<String, Object> getRAGConfig(String ragModelId);

        /**
         * Парсить документы (разбиение на блоки)
         * 
         * @param datasetId   ID базы знаний
         * @param documentIds список ID документов
         * @return результат парсинга
         */
        boolean parseDocuments(String datasetId, List<String> documentIds);

        /**
         * Вывести список блоков указанного документа
         * 
         * @param datasetId  ID базы знаний
         * @param documentId ID документа
         * @param req        параметры запроса списка блоков
         * @return информация о списке блоков
         */
        ChunkDTO.ListVO listChunks(String datasetId, String documentId, ChunkDTO.ListReq req);

        /**
         * Тест восстановления
         * 
         * @param req параметры запроса теста восстановления
         * @return результат теста восстановления
         */
        RetrievalDTO.ResultVO retrievalTest(RetrievalDTO.TestReq req);

        /**
         * Сохранить теневую запись документа
         */
        boolean saveDocumentShadow(String datasetId, KnowledgeFilesDTO result, String originalName, String chunkMethod,
                        Map<String, Object> parserConfig);

        /**
         * Пакетное удаление теневых записей документов и синхронизация статистики
         * 
         * @param documentIds список ID документов
         * @param datasetId   ID набора данных
         * @param chunkDelta  общее количество блоков для вычитания
         * @param tokenDelta  общее количество токенов для вычитания
         */
        void deleteDocumentShadows(List<String> documentIds, String datasetId, Long chunkDelta, Long tokenDelta);

        /**
         * Очистить все связанные документы по ID набора данных (для каскадного удаления)
         * 
         * @param datasetId ID набора данных
         */
        void deleteDocumentsByDatasetId(String datasetId);

        /**
         * Синхронизировать все документы в статусе RUNNING (для вызова планировщиком задач)
         */
        void syncRunningDocuments();

        /**
         * Полная синхронизация документов из RAGFlow в локальную теневую таблицу
         * Извлечь все удаленные документы, сравнить с локальной теневой таблицей, вставить отсутствующие записи
         *
         * @param datasetId ID набора данных
         * @return количество вновь синхронизированных документов
         */
        int syncDocumentsFromRAG(String datasetId);
}