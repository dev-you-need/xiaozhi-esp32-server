package xiaozhi.modules.agent.dto;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


/**
 * DTO создания агента
 * Для создания агента; не содержит id, agentCode и sort — эти поля генерируются системой
 */

@Data
@Schema(description = "智能体创建对象")
public class AgentCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "智能体名称", example = "客服助手")
    @NotBlank(message = "智能体名称不能为空")
    private String agentName;
}