package xiaozhi.modules.agent.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;


/**
 * Таблица голосовых отпечатков агента
 *
 * @author zjy
 */

@TableName(value = "ai_agent_voice_print")
@Data
public class AgentVoicePrintEntity {
    
/**
     * Первичный ключid
     */

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
/**
     * ID связанного агента
     */

    private String agentId;
    
/**
     * ID связанного аудио
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
     * Создатель
     */

    @TableField(fill = FieldFill.INSERT)
    private Long creator;
    
/**
     * Время создания
     */

    @TableField(fill = FieldFill.INSERT)
    private Date createDate;

    
/**
     * Обновивший
     */

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updater;
    
/**
     * Время обновления
     */

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateDate;
}
