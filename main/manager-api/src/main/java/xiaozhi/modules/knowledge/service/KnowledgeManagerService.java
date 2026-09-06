package xiaozhi.modules.knowledge.service;

import java.util.List;

/**
 * Сервис оркестрации домена модуля базы знаний
 * Используется для обработки сложных бизнес-процессов между KnowledgeBase и KnowledgeFiles, полностью решая проблему циклических зависимостей между сервисами.
 */
public interface KnowledgeManagerService {

    /**
     * Каскадное удаление базы знаний и всех ее документов (включая локальную БД и удаленные данные RAGFlow)
     * 
     * @param datasetId ID базы знаний
     */
    void deleteDatasetWithFiles(String datasetId);

    /**
     * Пакетное каскадное удаление базы знаний
     * 
     * @param datasetIds список ID базы знаний
     */
    void batchDeleteDatasetsWithFiles(List<String> datasetIds);
}
