package xiaozhi.modules.agent.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;

import xiaozhi.modules.agent.entity.AgentTemplateEntity;


/**
 * @author chenerlei
 * @description Сервис работы с БД для таблицы ai_agent_template (шаблоны конфигурации агента)
 * @createDate 2025-03-22 11:48:18
 */

public interface AgentTemplateService extends IRepository<AgentTemplateEntity> {

    
/**
     * Получить шаблон по умолчанию
     * 
     * @return Сущность шаблона по умолчанию
     */

    AgentTemplateEntity getDefaultTemplate();

    
/**
     * Обновить ID модели в шаблоне по умолчанию
     * 
     * @param modelType Тип модели
     * @param modelId   ID модели
     */

    void updateDefaultTemplateModelId(String modelType, String modelId);

    
/**
     * Пересортировать оставшиеся шаблоны после удаления
     * 
     * Значение Сортировка удаленного шаблона @ param deletedSort     */

    void reorderTemplatesAfterDelete(Integer deletedSort);

    
/**
     * Получить следующий доступный порядковый номер (найти наименьший неиспользуемый)
     * 
     * @return Следующий доступный порядковый номер
     */

    Integer getNextAvailableSort();
}
