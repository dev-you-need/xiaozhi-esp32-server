package xiaozhi.modules.agent.service;

import xiaozhi.common.service.BaseService;
import xiaozhi.modules.agent.entity.AgentContextProviderEntity;

public interface AgentContextProviderService extends BaseService<AgentContextProviderEntity> {
    
/**
     * Получить конфигурацию источника контекста по ID агента
     * @param agentId ID агента
     * @return Сущность конфигурации источника контекста
     */

    AgentContextProviderEntity getByAgentId(String agentId);

    
/**
     * Сохранить или обновить конфигурацию источника контекста
     * @param entity Сущность
     */

    void saveOrUpdateByAgentId(AgentContextProviderEntity entity);

    
/**
     * Удалить конфигурацию источника контекста по ID агента
     * @param agentId ID агента
     */

    void deleteByAgentId(String agentId);
}
