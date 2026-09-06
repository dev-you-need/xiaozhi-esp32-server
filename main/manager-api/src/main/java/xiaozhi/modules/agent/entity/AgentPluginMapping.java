package xiaozhi.modules.agent.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * Уникальная таблица отображений агент-плагин
 * 
 * @TableName ai_agent_plugin_mapping
 */

@Data
@TableName(value = "ai_agent_plugin_mapping")
@Schema(description = "Agent与插件的唯一映射表")
public class AgentPluginMapping implements Serializable {
    
/**
     * Первичный ключ
     */

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "映射信息主键ID")
    private Long id;

    
/**
     * ID агента
     */

    @Schema(description = "智能体ID")
    private String agentId;

    
/**
     * ID плагина
     */

    @Schema(description = "插件ID")
    private String pluginId;

    
/**
     * Параметры плагина (в формате JSON)
     */

    @Schema(description = "插件参数(Json)格式")
    private String paramInfo;

    // Избыточное поле для удобства при запросе плагинов по id для получения Provider_code плагина, см. dao/xml файл
    @TableField(exist = false)
    @Schema(description = "插件provider_code, 对应表ai_model_provider")
    private String providerCode;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}