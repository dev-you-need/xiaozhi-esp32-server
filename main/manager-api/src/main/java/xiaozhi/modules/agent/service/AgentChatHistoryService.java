package xiaozhi.modules.agent.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.repository.IRepository;

import xiaozhi.common.page.PageData;
import xiaozhi.modules.agent.dto.AgentChatHistoryDTO;
import xiaozhi.modules.agent.dto.AgentChatSessionDTO;
import xiaozhi.modules.agent.entity.AgentChatHistoryEntity;
import xiaozhi.modules.agent.vo.AgentChatHistoryUserVO;


/**
 * Сервис обработки таблицы истории чата агента
 *
 * @author Goody
 * @version 1.0, 2025/4/30
 * @since 1.0.0
 */

public interface AgentChatHistoryService extends IRepository<AgentChatHistoryEntity> {

    
/**
     * Получить список сессий по ID агента
     *
     * @param params Параметры запроса: agentId, page, limit
     * @return Постраничный список сессий
     */

    PageData<AgentChatSessionDTO> getSessionListByAgentId(Map<String, Object> params);

    
/**
     * Получить список записей чата по ID сессии
     *
     * @param agentId   ID агента
     * @param sessionId ID сессии
     * @return Список записей чата
     */

    List<AgentChatHistoryDTO> getChatHistoryBySessionId(String agentId, String sessionId);

    
/**
     * Получить ID агента по ID сессии
     *
     * @param sessionId ID сессии
     * @return ID агента
     */

    String getAgentIdBySessionId(String sessionId);

    
/**
     * Удалить записи чата по ID агента
     *
     * @param agentId     ID агента
     * @param deleteAudio Удалять ли аудио
     * @param deleteText  Удалять ли текст
     */

    void deleteByAgentId(String agentId, Boolean deleteAudio, Boolean deleteText);

    
/**
     * Получить последние 50 пользовательских записей чата агента (с аудиоданными)
     *
     * @param agentId ID агента
     * @return Список записей чата (только пользователи)
     */

    List<AgentChatHistoryUserVO> getRecentlyFiftyByAgentId(String agentId);

    
/**
     * Получить содержимое чата по ID аудиоданных
     *
     * @param audioId ID аудио
     * @return Содержимое чата
     */

    String getContentByAudioId(String audioId);

    
/**
     * Получить ID агента по ID аудио
     *
     * @param audioId ID аудио
     * @return ID агента
     */

    String getAgentIdByAudioId(String audioId);


    
/**
     * Проверить, принадлежит ли ID аудио данному агенту
     *
     * @param audioId ID аудио
     * @param agentId ID аудио
     * @return T — принадлежит, F — не принадлежит
     */

    boolean isAudioOwnedByAgent(String audioId,String agentId);
}
