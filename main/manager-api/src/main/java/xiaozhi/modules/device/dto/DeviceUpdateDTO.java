package xiaozhi.modules.device.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * DTO обновления устройства
 */
@Data
public class DeviceUpdateDTO implements Serializable {
    /**
    * Статус автоматического обновления
    */
    @Max(1)
    @Min(0)
    private Integer autoUpdate;

    /**
    * Псевдоним устройства
    */
    @Size(max = 64)
    private String alias;

    private static final long serialVersionUID = 1L;
}
