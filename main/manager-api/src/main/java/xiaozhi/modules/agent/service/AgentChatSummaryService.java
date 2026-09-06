package xiaozhi.modules.agent.service;


/**
 * Интерфейс сервиса итогов истории чата агента
 */

public interface AgentChatSummaryService {

    
/**
     * Во исполнениеID сессииСформировать сводку чата и сохранить вАгентПамять
     * 
     * @param sessionId ID сессии
     * @return Результат сохранения
     */

    boolean generateAndSaveChatSummary(String sessionId);

    
/**
     * Во исполнениеID сессииСгенерировать заголовок чата и сохранить
     *
     * @param sessionId ID сессии
     * @return Успешность выполнения
     */

    boolean generateAndSaveChatTitle(String sessionId);
}