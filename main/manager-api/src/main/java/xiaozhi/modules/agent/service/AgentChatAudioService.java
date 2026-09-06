package xiaozhi.modules.agent.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;

import xiaozhi.modules.agent.entity.AgentChatAudioEntity;


/**
 * Сервис обработки таблицы аудиоданных чата агента
 *
 * @author Goody
 * @version 1.0, 2025/5/8
 * @since 1.0.0
 */

public interface AgentChatAudioService extends IRepository<AgentChatAudioEntity> {
    
/**
     * Сохранить аудиоданные
     *
     * @param audioData Аудиоданные
     * @return ID аудио
     */

    String saveAudio(byte[] audioData);

    
/**
     * Получить аудиоданные
     *
     * @param audioId ID аудио
     * @return Аудиоданные
     */

    byte[] getAudio(String audioId);
}
