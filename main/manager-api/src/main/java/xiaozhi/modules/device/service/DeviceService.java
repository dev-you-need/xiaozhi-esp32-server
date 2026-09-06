package xiaozhi.modules.device.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import xiaozhi.common.page.PageData;
import xiaozhi.common.service.BaseService;
import xiaozhi.modules.device.dto.DeviceManualAddDTO;
import xiaozhi.modules.device.dto.DevicePageUserDTO;
import xiaozhi.modules.device.dto.DeviceReportReqDTO;
import xiaozhi.modules.device.dto.DeviceReportRespDTO;
import xiaozhi.modules.device.entity.DeviceEntity;
import xiaozhi.modules.device.vo.UserShowDeviceListVO;

public interface DeviceService extends BaseService<DeviceEntity> {
    /**
     * Получить данные об онлайн-статусе устройства
     */
    String getDeviceOnlineData(String agentId);

    /**
     * Проверить, активно ли устройство
     */
    DeviceReportRespDTO checkDeviceActive(String macAddress, String clientId,
            DeviceReportReqDTO deviceReport);

    /**
     * Получить список устройств пользователя для указанного агента
     */
    List<DeviceEntity> getUserDevices(Long userId, String agentId);

    /**
     * Получить список устройств пользователя для указанного агента (с обработкой часового пояса)
     */
    List<UserShowDeviceListVO> getUserDeviceList(Long userId, String agentId);

    /**
     * Отвязать устройство
     */
    void unbindDevice(Long userId, String deviceId);

    /**
     * Активация устройства
     */
    Boolean deviceActivation(String agentId, String activationCode);

    /**
     * Удалить все устройства пользователя
     * 
     * @param userId ID пользователя
     */
    void deleteByUserId(Long userId);

    /**
     * Удалить все устройства, связанные с указанным агентом
     * 
     * @param agentId ID агента
     */
    void deleteByAgentId(String agentId);

    /**
     * Получить количество устройств пользователя
     * 
     * @param userId ID пользователя
     * @return количество устройств
     */
    Long selectCountByUserId(Long userId);

    /**
     * Получить информацию обо всех устройствах с постраничной разбивкой
     *
     * @param dto параметры поиска с постраничной разбивкой
     * @return постраничные данные списка пользователей
     */
    PageData<UserShowDeviceListVO> page(DevicePageUserDTO dto);

    /**
     * Получить информацию об устройстве по MAC-адресу
     * 
     * @param macAddress MAC-адрес
     * @return информация об устройстве
     */
    DeviceEntity getDeviceByMacAddress(String macAddress);

    /**
     * Получить код активации по ID устройства
     * 
     * @param deviceId ID устройства
     * @return код активации
     */
    String geCodeByDeviceId(String deviceId);

    /**
     * Получить время последнего подключения устройства для этого агента
     * 
     * @param agentId ID агента
     * @return время последнего подключения устройства
     */
    Date getLatestLastConnectionTime(String agentId);

    /**
     * Ручное добавление устройства
     */
    void manualAddDevice(Long userId, DeviceManualAddDTO dto);

    /**
     * Обновить информацию о подключении устройства
     */
    void updateDeviceConnectionInfo(String agentId, String deviceId, String appVersion);

    /**
     * Генерировать токен аутентификации WebSocket
     *
     * @param clientId ID клиента
     * @param username имя пользователя (обычно deviceId)
     * @return строка токена аутентификации
     * @throws Exception исключение при генерации токена
     */
    String generateWebSocketToken(String clientId, String username) throws Exception;

    /**
     * Искать устройства по MAC-адресу
     *
     * @param macAddress ключевое слово MAC-адреса
     * @param userId     ID пользователя
     * @return список устройств
     */
    List<DeviceEntity> searchDevicesByMacAddress(String macAddress, Long userId);

    /**
     * Получить список инструментов устройства
     */
    Object getDeviceTools(String deviceId);

    /**
     * Вызвать инструмент устройства
     */
    Object callDeviceTool(String deviceId, String toolName, Map<String, Object> arguments);

    }