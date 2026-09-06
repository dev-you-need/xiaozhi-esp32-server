package xiaozhi.modules.agent.dto;

import java.time.LocalDateTime;

import lombok.Data;


/**
 * DTO списка сессий агента
 */

@Data
public class AgentChatSessionDTO {
    
/**
     * ID сессии
     */

    private String sessionId;

    
/**
     * Время сессии
     */

    private LocalDateTime createdAt;

    
/**
     * Количество сообщений
     */

    private Integer chatCount;

    
/**
     * Заголовок сессии
     */

    private String title;
}