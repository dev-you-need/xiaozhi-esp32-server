package xiaozhi.modules.agent.dto;

import lombok.Data;


/**
 * DTO изменения голосового отпечатка агента
 *
 * @author zjy
 */

@Data
public class AgentVoicePrintUpdateDTO {
    
/**
     * ID голосового отпечатка агента
     */

    private String id;
    
/**
     * ID аудиофайла
     */

    private String audioId;
    
/**
     * Имя владельца голосового отпечатка
     */

    private String sourceName;
    
/**
     * Описание владельца голосового отпечатка
     */

    private String introduce;
}
