package xiaozhi.modules.agent.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import xiaozhi.modules.agent.entity.AgentChatHistoryEntity;


/**
 * {@link AgentChatHistoryEntity} DAO-объект истории чата агента
 *
 * @author Goody
 * @version 1.0, 2025/4/30
 * @since 1.0.0
 */

@Mapper
public interface AiAgentChatHistoryDao extends BaseMapper<AgentChatHistoryEntity> {

    
/**
     * Удалить историю чата по ID агента
     *
     * @param agentId ID агента
     */

    void deleteHistoryByAgentId(String agentId);

    
/**
     * Удалить ID аудио по ID агента
     *
     * @param agentId ID агента
     */

    void deleteAudioIdByAgentId(String agentId);

    
/**
     * Получить список всех ID аудио по ID агента
     *
     * @param agentId ID агента
     * @return ID аудиоСписок
     */

    List<String> getAudioIdsByAgentId(String agentId);

    
/**
     * Массовое удаление аудио
     *
     * @param audioIds ID аудиоСписок
     */

    void deleteAudioByIds(@Param("audioIds") List<String> audioIds);
}
