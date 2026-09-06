package xiaozhi.modules.agent.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.repository.IRepository;

import xiaozhi.modules.agent.entity.AgentPluginMapping;


/**
 * @description Сервис работы с БД для таблицы ai_agent_plugin_mapping (уникальные отображения агент-плагин)
 * @createDate 2025-05-25 22:33:17
 */

public interface AgentPluginMappingService extends IRepository<AgentPluginMapping> {

    
/**
     * Получить параметры плагина по ID агента
     *
     * @param agentId
     * @return
     */

    List<AgentPluginMapping> agentPluginParamsByAgentId(String agentId);

    
/**
     * Удалить параметры плагина по ID агента
     *
     * @param agentId
     */

    void deleteByAgentId(String agentId);

    
/**
     * Удалить отображения плагина для всех агентов по ID плагина
     *
     * @param pluginId ID плагина
     */

    void deleteByPluginId(String pluginId);
}
