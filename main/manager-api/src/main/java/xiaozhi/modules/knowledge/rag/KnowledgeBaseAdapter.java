package xiaozhi.modules.knowledge.rag;

import java.util.List;
import java.util.Map;

import xiaozhi.modules.knowledge.dto.dataset.DatasetDTO;

import xiaozhi.common.page.PageData;
import xiaozhi.modules.knowledge.dto.KnowledgeFilesDTO;
import xiaozhi.modules.knowledge.dto.document.DocumentDTO;
import xiaozhi.modules.knowledge.dto.document.ChunkDTO;
import xiaozhi.modules.knowledge.dto.document.RetrievalDTO;
import java.util.function.Consumer;

/**
 * Абстрактный базовый класс API-адаптера базы знаний
 * Определение общего интерфейса работы базы знаний для поддержки нескольких внутренних реализаций API
 */
public abstract class KnowledgeBaseAdapter {

        /**
         * Получить идентификацию типа адаптера
         * 
         * @ тип адаптера возврата (например, Ragflow, Milvus, PINECONE и т. д.)
         */
        public abstract String getAdapterType();

        /**
         * Начальная конфигурация адаптера
         * 
         * @ param конфигурационные параметры конфигурации
         */
        public abstract void initialize(Map<String, Object> config);

        /**
         * Убедитесь, что конфигурация действительна
         * 
         * @ param конфигурационные параметры конфигурации
         * @ возврат результатов проверки
         */
        public abstract boolean validateConfig(Map<String, Object> config);

        /**
         * Список документов по постраничным запросам
         * 
         * @ param datasetId Идентификатор базы знаний
         * @ param queryParams параметры запроса
         * @ param page Страница
         * @ param limit per page
         * @ return постраничные данные
         */
        public abstract PageData<KnowledgeFilesDTO> getDocumentList(String datasetId,
                        DocumentDTO.ListReq req);

        /**
         * Получить сведения о документе на основе идентификатора документа
         * 
         * @ param datasetId Идентификатор базы знаний
         * @ param documentId ID документа
         * @ return Сведения о документе (Strong Type InfoVO)
         */
        public abstract DocumentDTO.InfoVO getDocumentById(String datasetId, String documentId);

        /**
         * Загрузка документов в базу знаний
         * 
         * @ param req параметры запроса на загрузку
         * @ вернуть загруженную информацию о документе
         */
        public abstract KnowledgeFilesDTO uploadDocument(DocumentDTO.UploadReq req);

        /**
         * Запрос списка документов по пагинации статуса
         * 
         * @ param datasetId Идентификатор базы знаний
         * @ param status статус синтаксического анализа документа
         * @ param page Страница
         * @ param limit per page
         * @ return постраничные данные
         */
        public abstract PageData<KnowledgeFilesDTO> getDocumentListByStatus(String datasetId,
                        Integer status,
                        Integer page,
                        Integer limit);

        /**
         * Удаление документов (поддерживается массовое удаление)
         * 
         * @ param datasetId Идентификатор базы знаний
         * @ param req Объект запроса, содержащий список идентификаторов документов
         */
        public abstract void deleteDocument(String datasetId, DocumentDTO.BatchIdReq req);

        /**
         * Разобрать документ (нарезанный)
         * 
         * @ param datasetId Идентификатор базы знаний
         * @ param documentIds Список идентификаторов документов
         * @ return parse results
         */
        public abstract boolean parseDocuments(String datasetId, List<String> documentIds);

        /**
         * Перечислите фрагменты указанного документа
         * 
         * @ param datasetId Идентификатор базы знаний
         * @ param documentId ID документа
         * @ param req list request parameters (пагинация, ключевые слова и т.д.)
         * @ return slice list VO
         */
        public abstract ChunkDTO.ListVO listChunks(String datasetId,
                        String documentId,
                        ChunkDTO.ListReq req);

        /**
         * Отзыв теста - Извлечение соответствующих срезов из базы знаний
         * 
         * @ param req получение параметров тестового запроса
         * @ return отзыв результатов теста
         */
        public abstract RetrievalDTO.ResultVO retrievalTest(
                        RetrievalDTO.TestReq req);

        /**
         * Тестовое подключение
         * 
         * @ результаты теста обратного соединения
         */
        public abstract boolean testConnection();

        /**
         * Получить информацию о состоянии адаптера
         * 
         * @ информация О статусе возврата
         */
        public abstract Map<String, Object> getStatus();

        /**
         * Получение поддерживаемых параметров конфигурации
         * 
         * @ return описание параметра конфигурации
         */
        public abstract Map<String, Object> getSupportedConfig();

        /**
         * Получить конфигурацию по умолчанию
         * 
         * @ return конфигурация по умолчанию
         */
        public abstract Map<String, Object> getDefaultConfig();

        /**
         * Создать набор данных
         * 
         * @ param req создание параметров
         * @ return dataset details
         */
        public abstract DatasetDTO.InfoVO createDataset(DatasetDTO.CreateReq req);

        /**
         * Обновить набор данных
         * 
         * @ param datasetId Идентификатор набора данных
         * @ param req update parameters
         * @ return dataset details
         */
        public abstract DatasetDTO.InfoVO updateDataset(String datasetId, DatasetDTO.UpdateReq req);

        /**
         * Удалить набор данных
         * 
         * @ param req Удаление параметров запроса (включая список идентификаторов)
         * @ результаты операции возврата сыпучих материалов
         */
        public abstract DatasetDTO.BatchOperationVO deleteDataset(DatasetDTO.BatchIdReq req);

        /**
         * Количество документов, получающих набор данных
         *
         * @ param datasetId Идентификатор набора данных
         * @ return количество документов
         */
        public abstract Integer getDocumentCount(String datasetId);

        /**
         * Получить полную информацию о датасете (название, введение, количество документов и т.д.)
         * Используется для определения удаления конца RAGFlow, изменения имени синхронизации/профиля
         *
         * @ param datasetId Идентификатор набора данных
         * @ return dataset details, возвращает null, если сторона RAGFlow не существует
         */
        public abstract DatasetDTO.InfoVO getDatasetInfo(String datasetId);

        /**
         * Отправить потоковый запрос (SSE)
         * 
         * @ param endpoint API endpoint
         * @ param body request body
         * @ param onData data callback
         */
        public abstract void postStream(String endpoint, Object body, Consumer<String> onData);

        /**
         * Вопросы SearchBot
         *
         * @ param config Rag configuration
         * @ param body request body
         * @ param onData data callback
         * @ return response object
         */
        public abstract Object postSearchBotAsk(Map<String, Object> config, Object body,
                        Consumer<String> onData);

        /**
         * Беседа с
         ботом агента *
         * @ param config Rag configuration
         * @ param agentId Идентификатор агента
         * @ param body request body
         * @ param onData data callback
         */
        public abstract void postAgentBotCompletion(Map<String, Object> config, String agentId, Object body,
                        Consumer<String> onData);
}