package xiaozhi.modules.agent.vo;

import lombok.Data;

import java.util.Date;


/**
 * VO отображения списка голосовых отпечатков агента
 */

@Data
public class AgentVoicePrintVO {

    
/**
     * Первичный ключid
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
    
/**
     * Время создания
     */

    private Date createDate;
}
