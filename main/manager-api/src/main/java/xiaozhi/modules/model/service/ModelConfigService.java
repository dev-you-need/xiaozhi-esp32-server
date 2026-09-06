package xiaozhi.modules.model.service;

import java.util.List;
import java.util.Map;

import xiaozhi.common.page.PageData;
import xiaozhi.common.service.BaseService;
import xiaozhi.modules.model.dto.LlmModelBasicInfoDTO;
import xiaozhi.modules.model.dto.ModelBasicInfoDTO;
import xiaozhi.modules.model.dto.ModelConfigBodyDTO;
import xiaozhi.modules.model.dto.ModelConfigDTO;
import xiaozhi.modules.model.entity.ModelConfigEntity;

public interface ModelConfigService extends BaseService<ModelConfigEntity> {

    List<ModelBasicInfoDTO> getModelCodeList(String modelType, String modelName);

    List<LlmModelBasicInfoDTO> getLlmModelCodeList(String modelName);

    PageData<ModelConfigDTO> getPageList(String modelType, String modelName, String page, String limit);

    ModelConfigDTO add(String modelType, String provideCode, ModelConfigBodyDTO modelConfigBodyDTO);

    ModelConfigDTO edit(String modelType, String provideCode, String id, ModelConfigBodyDTO modelConfigBodyDTO);

    void delete(String id);

    /**
     * Получение имени модели по ID
     * 
     * @param id ID модели
     * @return Имя модели
     */
    String getModelNameById(String id);

    /**
     * Получение конфигурации модели по ID
     * 
     * @param id ID модели
     * @return Сущность конфигурации модели
     */
    ModelConfigEntity getModelByIdFromCache(String id);

    /**
     * Установка модели по умолчанию
     *
     * @param modelType Тип модели
     * @param isDefault Является ли модель по умолчанию (1: да, 0: нет)
     */
    void setDefaultModel(String modelType, int isDefault);

    /**
     * Получение списка платформ TTS, соответствующих условиям
     *
     * @return Список платформ TTS (id и modelName)
     */
    List<Map<String, Object>> getTtsPlatformList();

    /**
     * Получение всех активных конфигураций моделей по типу модели
     *
     * @param modelType Тип модели (например: LLM, TTS, ASR и т.д.)
     * @return Список активных конфигураций моделей
     */
    List<ModelConfigEntity> getEnabledModelsByType(String modelType);
}
