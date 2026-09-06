package xiaozhi.modules.agent.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;


/**
 * Таблица шаблонов конфигурации агента
 * 
 * @TableName ai_agent_template
 */

@TableName(value = "ai_agent_template")
@Data
public class AgentTemplateEntity implements Serializable {
    
/**
     * Уникальный идентификатор агента
     */

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    
/**
     * Код агента
     */

    private String agentCode;

    
/**
     * Название агента
     */

    private String agentName;

    
/**
     * Идентификатор модели распознавания речи
     */

    private String asrModelId;

    
/**
     * Идентификатор модели обнаружения голосовой активности
     */

    private String vadModelId;

    
/**
     * Идентификатор большой языковой модели
     */

    private String llmModelId;

    
/**
     * Идентификатор модели VLLM
     */

    private String vllmModelId;

    
/**
     * Идентификатор модели синтеза речи
     */

    private String ttsModelId;

    
/**
     * Идентификатор голоса
     */

    private String ttsVoiceId;

    
/**
     * Язык голоса
     */

    private String ttsLanguage;

    
/**
     * Громкость TTS
     */

    private Integer ttsVolume;

    
/**
     * Скорость речи TTS
     */

    private Integer ttsRate;

    
/**
     * Тон TTS
     */

    private Integer ttsPitch;

    
/**
     * Идентификатор модели памяти
     */

    private String memModelId;

    
/**
     * Идентификатор модели намерений
     */

    private String intentModelId;

    
/**
     * Конфигурация истории чата (0 — не записывать, 1 — только текст, 2 — текст и аудио)
     */

    private Integer chatHistoryConf;

    
/**
     * Параметры настройки роли
     */

    private String systemPrompt;

    
/**
     * Память итогов
     */

    private String summaryMemory;
    
/**
     * Код языка
     */

    private String langCode;

    
/**
     * Язык взаимодействия
     */

    private String language;

    
/**
     * Вес сортировки
     */

    private Integer sort;

    
/**
     * Создатель ID
     */

    private Long creator;

    
/**
     * Время создания
     */

    private Date createdAt;

    
/**
     * Обновивший ID
     */

    private Long updater;

    
/**
     * Время обновления
     */

    private Date updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}