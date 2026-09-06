package xiaozhi.modules.agent.service;


import java.util.List;


/**
 * Сервис обработки точки доступа MCP агента
 *
 * @author zjy
 */

public interface AgentMcpAccessPointService {
    
/**
     * Получить адрес точки доступа MCP агента
     * @param id ID агента
     * @return Адрес точки доступа MCP
     */

   String getAgentMcpAccessAddress(String id);

    
/**
     * Получить список инструментов точки доступа MCP агента
     * @param id ID агента
     * @return Список инструментов
     */

   List<String> getAgentMcpToolsList(String id);
}
