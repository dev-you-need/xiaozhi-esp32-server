package xiaozhi.modules.agent.dto;

import lombok.Data;


/**
 * DTO сохранения голосового отпечатка агента
 *
 * @author zjy
 */

@Data
public class AgentVoicePrintSaveDTO {
    
/**
     * ID связанного агента
     */

    private String agentId;
    
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
