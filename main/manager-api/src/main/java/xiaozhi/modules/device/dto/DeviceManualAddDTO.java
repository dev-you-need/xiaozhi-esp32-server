package xiaozhi.modules.device.dto;

import lombok.Data;

@Data
public class DeviceManualAddDTO {
    private String agentId;
    private String board;        // Модель устройства
    private String appVersion;   // Версия прошивки
    private String macAddress;   // MacАдрес
} 