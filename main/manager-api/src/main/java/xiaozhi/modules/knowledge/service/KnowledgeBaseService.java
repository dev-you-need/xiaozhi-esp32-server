package xiaozhi.modules.knowledge.service;

import java.util.List;
import java.util.Map;

import xiaozhi.common.page.PageData;
import xiaozhi.common.service.BaseService;
import xiaozhi.modules.knowledge.dto.KnowledgeBaseDTO;
import xiaozhi.modules.knowledge.entity.KnowledgeBaseEntity;
import xiaozhi.modules.model.entity.ModelConfigEntity;

/**
 * Интерфейс сервиса базы знаний
 */
public interface KnowledgeBaseService extends BaseService<KnowledgeBaseEntity> {

    /**
     * Постраничный запрос списка базы знаний
     * 
     * @param knowledgeBaseDTO условия поиска
     * @param page             номер страницы
     * @param limit            количество на странице
     * @return постраничные данные
     */
    PageData<KnowledgeBaseDTO> getPageList(KnowledgeBaseDTO knowledgeBaseDTO, Integer page, Integer limit);

    /**
     * Получить информацию о базе знаний по ID
     * 
     * @param id ID базы знаний
     * @return информация о базе знаний
     */
    KnowledgeBaseDTO getById(String id);

    /**
     * Добавить базу знаний
     * 
     * @param knowledgeBaseDTO информация о базе знаний
     * @return добавленная база знаний
     */
    KnowledgeBaseDTO save(KnowledgeBaseDTO knowledgeBaseDTO);

    /**
     * Обновить базу знаний
     * 
     * @param knowledgeBaseDTO информация о базе знаний
     * @return обновленная база знаний
     */
    KnowledgeBaseDTO update(KnowledgeBaseDTO knowledgeBaseDTO);

    /**
     * Запросить базу знаний по ID набора данных
     * 
     * @param datasetId ID набора данных
     * @return информация о базе знаний
     */
    KnowledgeBaseDTO getByDatasetId(String datasetId);

    /**
     * Запросить базу знаний по списку ID наборов данных
     *
     * @param datasetIdList список ID наборов данных
     * @return информация о базе знаний
     */
    List<KnowledgeBaseDTO> getByDatasetIdList(List<String> datasetIdList);

    /**
     * Удалить базу знаний по ID набора данных
     * 
     * @param datasetId ID набора данных
     */
    void deleteByDatasetId(String datasetId);

    /**
     * Получить информацию о конфигурации RAG
     * 
     * @param ragModelId ID конфигурации модели RAG
     * @return информация о конфигурации RAG
     */
    Map<String, Object> getRAGConfig(String ragModelId);

    /**
     * Получить соответствующую конфигурацию RAG по ID набора данных
     * 
     * @param datasetId ID набора данных
     * @return конфигурация RAG
     */
    Map<String, Object> getRAGConfigByDatasetId(String datasetId);

    /**
     * Получить список моделей RAG
     * 
     * @return список моделей RAG
     */
    List<ModelConfigEntity> getRAGModels();

    /**
     * Обновить статистику базы знаний (для обратного вызова сервиса файлов)
     * 
     * @param datasetId  ID набора данных
     * @param docDelta   приращение количества документов
     * @param chunkDelta приращение количества блоков
     * @param tokenDelta приращение количества токенов
     */
    void updateStatistics(String datasetId, Integer docDelta, Long chunkDelta, Long tokenDelta);
}