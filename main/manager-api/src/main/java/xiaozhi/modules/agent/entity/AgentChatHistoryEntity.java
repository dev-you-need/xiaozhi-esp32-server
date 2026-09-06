package xiaozhi.modules.agent.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Таблица истории чата агента
 *
 * @author Goody
 * @version 1.0, 2025/4/30
 * @since 1.0.0
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "ai_agent_chat_history")
public class AgentChatHistoryEntity {
    
/**
     * ID первичного ключа
     */

    @TableId(type = IdType.AUTO)
    private Long id;

    
/**
     * MAC-адрес
     */

    @TableField(value = "mac_address")
    private String macAddress;

    
/**
     * ID агента
     */

    @TableField(value = "agent_id")
    private String agentId;

    
/**
     * ID сессии
     */

    @TableField(value = "session_id")
    private String sessionId;

    
/**
     * Тип сообщения: 1 — пользователь, 2 — агент
     */

    @TableField(value = "chat_type")
    private Byte chatType;

    
/**
     * Содержимое чата
     */

    @TableField(value = "content")
    private String content;

    
/**
     * Аудиоданные в формате base64
     */

    @TableField(value = "audio_id")
    private String audioId;

    
/**
     * Время создания
     */

    @TableField(value = "created_at")
    private Date createdAt;

    
/**
     * Время обновления
     */

    @TableField(value = "updated_at")
    private Date updatedAt;
}
