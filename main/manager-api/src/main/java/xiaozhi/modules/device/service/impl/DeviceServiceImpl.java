package xiaozhi.modules.device.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.page.PageData;
import xiaozhi.common.redis.RedisKeys;
import xiaozhi.common.redis.RedisUtils;
import xiaozhi.common.service.impl.BaseServiceImpl;
import xiaozhi.common.user.UserDetail;
import xiaozhi.common.utils.ConvertUtils;
import xiaozhi.common.utils.DateUtils;
import xiaozhi.common.utils.JsonUtils;
import xiaozhi.modules.device.dao.DeviceDao;
import xiaozhi.modules.device.dto.DeviceManualAddDTO;
import xiaozhi.modules.device.dto.DevicePageUserDTO;
import xiaozhi.modules.device.dto.DeviceReportReqDTO;
import xiaozhi.modules.device.dto.DeviceReportRespDTO;
import xiaozhi.modules.device.entity.DeviceEntity;
import xiaozhi.modules.device.entity.OtaEntity;
import xiaozhi.modules.device.service.DeviceAddressBookService;
import xiaozhi.modules.device.service.DeviceService;
import xiaozhi.modules.device.service.OtaService;
import xiaozhi.modules.device.vo.UserShowDeviceListVO;
import xiaozhi.modules.security.user.SecurityUser;
import xiaozhi.modules.sys.service.SysParamsService;
import xiaozhi.modules.sys.service.SysUserUtilService;

@Slf4j
@Service
@AllArgsConstructor
public class DeviceServiceImpl extends BaseServiceImpl<DeviceDao, DeviceEntity> implements DeviceService {

    private final DeviceDao deviceDao;
    private final SysUserUtilService sysUserUtilService;
    private final SysParamsService sysParamsService;
    private final RedisUtils redisUtils;
    private final OtaService otaService;
    private final DeviceAddressBookService deviceAddressBookService;

    @Async
    public void updateDeviceConnectionInfo(String agentId, String deviceId, String appVersion) {
        try {
            DeviceEntity device = new DeviceEntity();
            device.setId(deviceId);
            device.setLastConnectedAt(new Date());
            if (StringUtils.isNotBlank(appVersion)) {
                device.setAppVersion(appVersion);
            }
            deviceDao.updateById(device);
            if (StringUtils.isNotBlank(agentId)) {
                redisUtils.set(RedisKeys.getAgentDeviceLastConnectedAtById(agentId), new Date());
            }
        } catch (Exception e) {
            log.error("异步更新设备连接信息失败", e);
        }
    }

    @Override
    public Boolean deviceActivation(String agentId, String activationCode) {
        if (StringUtils.isBlank(activationCode)) {
            throw new RenException(ErrorCode.ACTIVATION_CODE_EMPTY);
        }
        String deviceKey = RedisKeys.getOtaActivationCode(activationCode);
        String cacheDeviceId = (String) redisUtils.get(deviceKey);
        if (StringUtils.isBlank(cacheDeviceId)) {
            throw new RenException(ErrorCode.ACTIVATION_CODE_ERROR);
        }
        String deviceId = cacheDeviceId;
        String safeDeviceId = deviceId.replace(":", "_").toLowerCase();
        String cacheDeviceKey = RedisKeys.getOtaDeviceActivationInfo(safeDeviceId);
        Map<String, Object> cacheMap = JsonUtils.toStringObjectMap(redisUtils.get(cacheDeviceKey));
        if (MapUtil.isEmpty(cacheMap)) {
            throw new RenException(ErrorCode.ACTIVATION_CODE_ERROR);
        }
        String cachedCode = (String) cacheMap.get("activation_code");
        if (!activationCode.equals(cachedCode)) {
            throw new RenException(ErrorCode.ACTIVATION_CODE_ERROR);
        }
        //Проверьте, активировано ли устройство
        if (selectById(deviceId) != null) {
            throw new RenException(ErrorCode.DEVICE_ALREADY_ACTIVATED);
        }

        String macAddress = (String) cacheMap.get("mac_address");
        String board = (String) cacheMap.get("board");
        String appVersion = (String) cacheMap.get("app_version");
        UserDetail user = SecurityUser.getUser();
        if (user.getId() == null) {
            throw new RenException(ErrorCode.USER_NOT_LOGIN);
        }

        Date currentTime = new Date();
        DeviceEntity deviceEntity = new DeviceEntity();
        deviceEntity.setId(deviceId);
        deviceEntity.setBoard(board);
        deviceEntity.setAgentId(agentId);
        deviceEntity.setAppVersion(appVersion);
        deviceEntity.setMacAddress(macAddress);
        deviceEntity.setUserId(user.getId());
        deviceEntity.setCreator(user.getId());
        deviceEntity.setAutoUpdate(1);
        deviceEntity.setCreateDate(currentTime);
        deviceEntity.setUpdater(user.getId());
        deviceEntity.setUpdateDate(currentTime);
        deviceEntity.setLastConnectedAt(currentTime);
        deviceDao.insert(deviceEntity);

        //Очистите кэш redis, очистите кэш устройств агента
        redisUtils.delete(List.of(cacheDeviceKey, deviceKey, RedisKeys.getAgentDeviceCountById(agentId)));
        return true;
    }

    /**
     * Захват данных устройства в режиме онлайн
     */
    @Override
    public String getDeviceOnlineData(String agentId) {
        //Получить адрес MQTT-шлюза из параметров системы
        String mqttGatewayUrl = sysParamsService.getValue("server.mqtt_manager_api", true);
        if (StringUtils.isBlank(mqttGatewayUrl) || "null".equals(mqttGatewayUrl)) {
            return "";
        }
        //Построение полного URL
        String url = StrUtil.format("http://{}/api/devices/status", mqttGatewayUrl);

        //Получить список устройств текущего пользователя
        UserDetail user = SecurityUser.getUser();
        List<DeviceEntity> devices = getUserDevices(user.getId(), agentId);

        //Построение массива deviceIds
        Set<String> deviceIds = devices.stream().map(o -> {
            String macAddress = Optional.ofNullable(o.getMacAddress()).orElse("unknown").replace(":", "_");
            String groupId = Optional.ofNullable(o.getBoard()).orElse("GID_default").replace(":", "_");
            return StrUtil.format("{}@@@{}@@@{}", groupId, macAddress, macAddress);
        }).collect(Collectors.toSet());

        //построение входных параметров запроса
        Map<String, Set<String>> params = MapUtil
                .builder(new HashMap<String, Set<String>>())
                .put("clientIds", deviceIds).build();

        if (CollUtil.isNotEmpty(deviceIds)) {
            return postToMqttGateway(url, params);
        }
        //Ответ на возврат
        return "";
    }

    @Override
    public DeviceReportRespDTO checkDeviceActive(String macAddress, String clientId, DeviceReportReqDTO deviceReport) {
        DeviceReportRespDTO response = new DeviceReportRespDTO();
        response.setServer_time(buildServerTime());

        DeviceEntity deviceById = getDeviceByMacAddress(macAddress);

        //Если устройство не привязано, верните текущую загруженную информацию о прошивке (не обновляйте), чтобы она была совместима со старой версией прошивки
        if (deviceById == null) {
            DeviceReportRespDTO.Firmware firmware = new DeviceReportRespDTO.Firmware();
            firmware.setVersion(deviceReport.getApplication().getVersion());
            firmware.setUrl(Constant.INVALID_FIRMWARE_URL);
            response.setFirmware(firmware);
        } else {
            //Возвращайте информацию об обновлении прошивки только в том случае, если устройство привязано и автоматическое обновление явно включено
            if (Integer.valueOf(1).equals(deviceById.getAutoUpdate())) {
                String type = deviceReport.getBoard() == null ? null : deviceReport.getBoard().getType();
                DeviceReportRespDTO.Firmware firmware = buildFirmwareInfo(type,
                        deviceReport.getApplication() == null ? null : deviceReport.getApplication().getVersion());
                response.setFirmware(firmware);
            }
        }

        //Добавление конфигурации WebSocket
        DeviceReportRespDTO.Websocket websocket = new DeviceReportRespDTO.Websocket();
        //Получите URL-адрес WebSocket из системного параметра, используйте значение по умолчанию, если оно не настроено
        String wsUrl = sysParamsService.getValue(Constant.SERVER_WEBSOCKET, true);

        //Проверьте, включена ли аутентификация, и сгенерируйте токены
        String authEnabled = sysParamsService.getValue(Constant.SERVER_AUTH_ENABLED, true);
        if ("true".equalsIgnoreCase(authEnabled)) {
            try {
                //Сгенерировать токен
                String token = generateWebSocketToken(clientId, macAddress);
                websocket.setToken(token);
            } catch (Exception e) {
                log.error("生成WebSocket token失败: {}", e.getMessage());
                websocket.setToken("");
            }
        } else {
            websocket.setToken("");
        }

        if (StringUtils.isBlank(wsUrl) || wsUrl.equals("null")) {
            log.error("WebSocket地址未配置，请登录智控台，在参数管理找到【server.websocket】配置");
            wsUrl = "ws://xiaozhi.server.com:8000/xiaozhi/v1/";
            websocket.setUrl(wsUrl);
        } else {
            String[] wsUrls = wsUrl.split("\\;");
            if (wsUrls.length > 0) {
                //Случайный выбор URL-адреса WebSocket
                websocket.setUrl(wsUrls[RandomUtil.randomInt(0, wsUrls.length)]);
            } else {
                log.error("WebSocket地址未配置，请登录智控台，在参数管理找到【server.websocket】配置");
                websocket.setUrl("ws://xiaozhi.server.com:8000/xiaozhi/v1/");
            }
        }

        response.setWebsocket(websocket);

        //Добавление конфигурации MQTT UDP
        //Получите адрес шлюза MQTT из параметров системы, используйте только в том случае, если конфигурация действительна
        String mqttUdpConfig = sysParamsService.getValue(Constant.SERVER_MQTT_GATEWAY, true);
        if (mqttUdpConfig != null && !mqttUdpConfig.equals("null") && !mqttUdpConfig.isEmpty()) {
            try {
                String groupId = deviceById != null && deviceById.getBoard() != null ? deviceById.getBoard()
                        : "GID_default";
                DeviceReportRespDTO.MQTT mqtt = buildMqttConfig(macAddress, groupId);
                if (mqtt != null) {
                    mqtt.setEndpoint(mqttUdpConfig);
                    response.setMqtt(mqtt);
                }
            } catch (Exception e) {
                log.error("生成MQTT配置失败: {}", e.getMessage());
            }
        }

        if (deviceById != null) {
            //Асинхронное обновление информации о времени последнего подключения и версии, если устройство присутствует
            String appVersion = deviceReport.getApplication() != null ? deviceReport.getApplication().getVersion()
                    : null;
            //Вызов асинхронных методов через Spring proxy
            ((DeviceServiceImpl) AopContext.currentProxy()).updateDeviceConnectionInfo(deviceById.getAgentId(),
                    deviceById.getId(), appVersion);
        } else {
            //Сгенерировать код активации, если устройство не существует
            DeviceReportRespDTO.Activation code = buildActivation(macAddress, deviceReport);
            response.setActivation(code);
        }

        return response;
    }

    @Override
    public List<DeviceEntity> getUserDevices(Long userId, String agentId) {
        QueryWrapper<DeviceEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("agent_id", agentId);
        return baseDao.selectList(wrapper);
    }

    @Override
    public List<UserShowDeviceListVO> getUserDeviceList(Long userId, String agentId) {
        List<DeviceEntity> devices = getUserDevices(userId, agentId);
        return devices.stream().map(this::toUserShowDeviceListVO).toList();
    }

    private UserShowDeviceListVO toUserShowDeviceListVO(DeviceEntity device) {
        UserShowDeviceListVO vo = ConvertUtils.sourceToTarget(device, UserShowDeviceListVO.class);
        vo.setDeviceType(device.getBoard());
        vo.setBoard(device.getBoard());
        vo.setAutoUpdate(device.getAutoUpdate());
        vo.setCreateDateTimestamp(toTimestamp(device.getCreateDate()));
        vo.setLastConnectedAtTimestamp(toTimestamp(device.getLastConnectedAt()));
        return vo;
    }

    private Long toTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @Override
    public void unbindDevice(Long userId, String deviceId) {
        //Сначала запросите информацию об устройстве, получите agentId и macAddress
        DeviceEntity device = baseDao.selectById(deviceId);
        if (device == null) {
            return;
        }
        String macAddress = device.getMacAddress();
        if (StringUtils.isNotBlank(device.getAgentId())) {
            //Очистка кэша устройств агента
            redisUtils.delete(RedisKeys.getAgentDeviceCountById(device.getAgentId()));
        }

        UpdateWrapper<DeviceEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("id", deviceId);
        baseDao.delete(wrapper);

        //Удаление записей разрешений телефонной книги, связанных с устройством
        deviceAddressBookService.deleteByMacAddresses(Collections.singletonList(macAddress));
    }

    @Override
    public void deleteByUserId(Long userId) {
        UpdateWrapper<DeviceEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId);
        baseDao.delete(wrapper);
    }

    @Override
    public Long selectCountByUserId(Long userId) {
        UpdateWrapper<DeviceEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId);
        return baseDao.selectCount(wrapper);
    }

    @Override
    public void deleteByAgentId(String agentId) {
        //Сначала запросить все устройства под агентом, получить MAC-адрес для удаления записи контакта
        QueryWrapper<DeviceEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("agent_id", agentId);
        List<DeviceEntity> devices = baseDao.selectList(queryWrapper);

        Удаление устройства
        UpdateWrapper<DeviceEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("agent_id", agentId);
        baseDao.delete(wrapper);

        //Массовое удаление всех записей разрешений телефонной книги, связанных с этими устройствами
        if (!devices.isEmpty()) {
            List<String> macAddresses = devices.stream()
                    .map(DeviceEntity::getMacAddress)
                    .collect(Collectors.toList());
            deviceAddressBookService.deleteByMacAddresses(macAddresses);
        }
    }

    @Override
    public PageData<UserShowDeviceListVO> page(DevicePageUserDTO dto) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(Constant.PAGE, dto.getPage());
        params.put(Constant.LIMIT, dto.getLimit());
        IPage<DeviceEntity> page = baseDao.selectPage(
                getPage(params, "mac_address", true),
                //Определить критерии запроса
                new QueryWrapper<DeviceEntity>()
                        //Требуется поиск по ключевому слову устройства
                        .like(StringUtils.isNotBlank(dto.getKeywords()), "alias", dto.getKeywords()));
        //Пройдитесь по странице, чтобы получить данные обратно и вернуть необходимые поля
        List<UserShowDeviceListVO> list = page.getRecords().stream().map(device -> {
            UserShowDeviceListVO vo = toUserShowDeviceListVO(device);
            //Измените время последней модификации на время краткого описания
            vo.setRecentChatTime(DateUtils.getShortTime(device.getUpdateDate()));
            sysUserUtilService.assignUsername(device.getUserId(),
                    vo::setBindUserName);
            return vo;
        }).toList();
        //Рассчитать количество страниц
        return new PageData<>(list, page.getTotal());
    }

    @Override
    public DeviceEntity getDeviceByMacAddress(String macAddress) {
        if (StringUtils.isBlank(macAddress)) {
            return null;
        }
        QueryWrapper<DeviceEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("mac_address", macAddress);
        return baseDao.selectOne(wrapper);
    }

    private DeviceReportRespDTO.ServerTime buildServerTime() {
        DeviceReportRespDTO.ServerTime serverTime = new DeviceReportRespDTO.ServerTime();
        TimeZone tz = TimeZone.getDefault();
        serverTime.setTimestamp(Instant.now().toEpochMilli());
        serverTime.setTimeZone(tz.getID());
        serverTime.setTimezone_offset(tz.getOffset(System.currentTimeMillis()) / (60 * 1000));
        return serverTime;
    }

    @Override
    public String geCodeByDeviceId(String deviceId) {
        String dataKey = getDeviceCacheKey(deviceId);

        Map<String, Object> cacheMap = JsonUtils.toStringObjectMap(redisUtils.get(dataKey));
        if (cacheMap != null && cacheMap.containsKey("activation_code")) {
            String cachedCode = (String) cacheMap.get("activation_code");
            return cachedCode;
        }
        return null;
    }

    @Override
    public Date getLatestLastConnectionTime(String agentId) {
        //Запрос, если есть время кэширования, возврат, если есть
        Date cachedDate = (Date) redisUtils.get(RedisKeys.getAgentDeviceLastConnectedAtById(agentId));
        if (cachedDate != null) {
            return cachedDate;
        }
        Date maxDate = deviceDao.getAllLastConnectedAtByAgentId(agentId);
        if (maxDate != null) {
            redisUtils.set(RedisKeys.getAgentDeviceLastConnectedAtById(agentId), maxDate);
        }
        return maxDate;
    }

    private String getDeviceCacheKey(String deviceId) {
        String safeDeviceId = deviceId.replace(":", "_").toLowerCase();
        return RedisKeys.getOtaDeviceActivationInfo(safeDeviceId);
    }

    public DeviceReportRespDTO.Activation buildActivation(String deviceId, DeviceReportReqDTO deviceReport) {
        DeviceReportRespDTO.Activation code = new DeviceReportRespDTO.Activation();

        String cachedCode = geCodeByDeviceId(deviceId);

        if (StringUtils.isNotBlank(cachedCode)) {
            code.setCode(cachedCode);
            String frontedUrl = sysParamsService.getValue(Constant.SERVER_FRONTED_URL, true);
            code.setMessage(frontedUrl + "\n" + cachedCode);
            code.setChallenge(deviceId);
        } else {
            String newCode = RandomUtil.randomNumbers(6);
            code.setCode(newCode);
            String frontedUrl = sysParamsService.getValue(Constant.SERVER_FRONTED_URL, true);
            code.setMessage(frontedUrl + "\n" + newCode);
            code.setChallenge(deviceId);

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("id", deviceId);
            dataMap.put("mac_address", deviceId);

            dataMap.put("board", (deviceReport.getBoard() != null && deviceReport.getBoard().getType() != null)
                    ? deviceReport.getBoard().getType()
                    : (deviceReport.getChipModelName() != null ? deviceReport.getChipModelName() : "unknown"));
            dataMap.put("app_version", (deviceReport.getApplication() != null)
                    ? deviceReport.getApplication().getVersion()
                    : null);

            dataMap.put("deviceId", deviceId);
            dataMap.put("activation_code", newCode);

            //запись главного ключа данных
            String dataKey = getDeviceCacheKey(deviceId);
            redisUtils.set(dataKey, dataMap);

            //запись ключа кода активации встречной проверки
            String codeKey = RedisKeys.getOtaActivationCode(newCode);
            redisUtils.set(codeKey, deviceId);
        }
        return code;
    }

    private DeviceReportRespDTO.Firmware buildFirmwareInfo(String type, String currentVersion) {
        if (StringUtils.isBlank(type)) {
            return null;
        }
        if (StringUtils.isBlank(currentVersion)) {
            currentVersion = "0.0.0";
        }

        OtaEntity ota = otaService.getLatestOta(type);
        DeviceReportRespDTO.Firmware firmware = new DeviceReportRespDTO.Firmware();
        String downloadUrl = null;

        if (ota != null) {
            //Если устройство не имеет информации о версии или версия OTA более новая, чем версия устройства, возвращается адрес загрузки
            if (compareVersions(ota.getVersion(), currentVersion) > 0) {
                String otaUrl = sysParamsService.getValue(Constant.SERVER_OTA, true);
                if (StringUtils.isBlank(otaUrl) || otaUrl.equals("null")) {
                    log.error("OTA地址未配置，请登录智控台，在参数管理找到【server.ota】配置");
                    //Постараемся получить из запроса
                    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                            .getRequestAttributes())
                            .getRequest();
                    otaUrl = request.getRequestURL().toString();
                }
                //Замените/ota/в URL на/otaMag/download/
                String uuid = UUID.randomUUID().toString();
                redisUtils.set(RedisKeys.getOtaIdKey(uuid), ota.getId());
                downloadUrl = otaUrl.replace("/ota/", "/otaMag/download/") + uuid;
            }
        }

        firmware.setVersion(ota == null ? currentVersion : ota.getVersion());
        firmware.setUrl(downloadUrl == null ? Constant.INVALID_FIRMWARE_URL : downloadUrl);
        return firmware;
    }

    /**
     * Сравните два номера версий
     * 
     * @ param version1 version 1
     * @ param version2 version 2
     * @ return Если version1 > version2 возвращает 1, version1 < version2 возвращает -1, равно возвращает 0
     */
    private static int compareVersions(String version1, String version2) {
        if (version1 == null || version2 == null) {
            return 0;
        }

        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");

        int length = Math.max(v1Parts.length, v2Parts.length);
        for (int i = 0; i < length; i++) {
            int v1 = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2 = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;

            if (v1 > v2) {
                return 1;
            } else if (v1 < v2) {
                return -1;
            }
        }
        return 0;
    }

    @Override
    public void manualAddDevice(Long userId, DeviceManualAddDTO dto) {
        //проверить, что mac уже существует
        QueryWrapper<DeviceEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("mac_address", dto.getMacAddress());
        DeviceEntity exist = baseDao.selectOne(wrapper);
        if (exist != null) {
            throw new RenException(ErrorCode.MAC_ADDRESS_ALREADY_EXISTS);
        }
        Date now = new Date();
        DeviceEntity entity = new DeviceEntity();
        entity.setId(dto.getMacAddress());
        entity.setUserId(userId);
        entity.setAgentId(dto.getAgentId());
        entity.setBoard(dto.getBoard());
        entity.setAppVersion(dto.getAppVersion());
        entity.setMacAddress(dto.getMacAddress());
        entity.setCreateDate(now);
        entity.setUpdateDate(now);
        entity.setLastConnectedAt(now);
        entity.setCreator(userId);
        entity.setUpdater(userId);
        entity.setAutoUpdate(1);
        baseDao.insert(entity);

        //Добавлено: Количество агентов Кэш устройства очищен
        redisUtils.delete(RedisKeys.getAgentDeviceCountById(dto.getAgentId()));
    }

    @Override
    public List<DeviceEntity> searchDevicesByMacAddress(String macAddress, Long userId) {
        QueryWrapper<DeviceEntity> wrapper = new QueryWrapper<>();
        wrapper.like("mac_address", macAddress);
        wrapper.eq("user_id", userId);
        return deviceDao.selectList(wrapper);
    }

    /**
     * Сгенерировать подпись пароля MQTT
     * 
     * @ param content signature content (clientId + '|' + username)
     * @ param secretKey
     * @ return Подпись в кодировке Base64 HMAC-SHA256
     */
    private String generatePasswordSignature(String content, String secretKey) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(keySpec);
        byte[] signature = hmac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature);
    }

    /**
     * Создание токена аутентификации WebSocket следует логике реализации AuthManager на стороне Python: token = signature.timestamp
     * 
     * @ param clientId ID клиента
     * @ param username username (обычно deviceId/macAddress)
     * @ return строка маркера аутентификации
     */
    public String generateWebSocketToken(String clientId, String username)
            throws NoSuchAlgorithmException, InvalidKeyException {
        //Получить ключ из параметров системы
        String secretKey = sysParamsService.getValue(Constant.SERVER_SECRET, false);
        if (StringUtils.isBlank(secretKey)) {
            throw new IllegalStateException("WebSocket认证密钥未配置(server.secret)");
        }

        //Получить текущую временную метку (сек)
        long timestamp = System.currentTimeMillis() / 1000;

        //содержимое подписи сборки: clientId | username | timestamp
        String content = String.format("%s|%s|%d", clientId, username, timestamp);

        //Создание подписи HMAC-SHA256
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(keySpec);
        byte[] signature = hmac.doFinal(content.getBytes(StandardCharsets.UTF_8));

        //Кодированная подпись, защищенная URL-адресом Base64 (удалить отступ =)
        String signatureBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);

        //формат возврата: signature.timestamp
        return String.format("%s.%d", signatureBase64, timestamp);
    }

    /**
     * Создайте информацию о конфигурации MQTT
     * 
     * @ param macAddress MAC-адрес
     * @ param groupId ID группы
     * @ return Объект конфигурации MQTT
     */
    private DeviceReportRespDTO.MQTT buildMqttConfig(String macAddress, String groupId)
            throws Exception {
        //Получение ключей подписи из переменных среды или системных параметров
        String signatureKey = sysParamsService.getValue("server.mqtt_signature_key", true);
        if (StringUtils.isBlank(signatureKey)) {
            log.warn("缺少MQTT_SIGNATURE_KEY，跳过MQTT配置生成");
            return null;
        }

        //Формат идентификатора клиента сборки: groupId @ @ @ macAddress @ @ @ uuid
        String groupIdSafeStr = groupId.replace(":", "_");
        String deviceIdSafeStr = macAddress.replace(":", "_");
        String mqttClientId = String.format("%s@@@%s@@@%s", groupIdSafeStr, deviceIdSafeStr, deviceIdSafeStr);

        //Сборка пользовательских данных (включая такую информацию, как IP)
        Map<String, String> userData = new HashMap<>();
        //Попробуйте получить клиентский IP-адрес
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String clientIp = request.getRemoteAddr();
                userData.put("ip", clientIp);
            }
        } catch (Exception e) {
            userData.put("ip", "unknown");
        }

        //Кодируем пользовательские данные как Base64 JSON
        String userDataJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(userData);
        String username = Base64.getEncoder().encodeToString(userDataJson.getBytes(StandardCharsets.UTF_8));

        //Сгенерировать подпись пароля
        String password = generatePasswordSignature(mqttClientId + "|" + username, signatureKey);

        //Построение конфигурации MQTT
        DeviceReportRespDTO.MQTT mqtt = new DeviceReportRespDTO.MQTT();
        mqtt.setClient_id(mqttClientId);
        mqtt.setUsername(username);
        mqtt.setPassword(password);
        mqtt.setPublish_topic("device-server");
        mqtt.setSubscribe_topic("devices/p2p/" + deviceIdSafeStr);

        return mqtt;
    }

    private String postToMqttGateway(String url, Object requestBody) {
        String signatureKey = sysParamsService.getValue(Constant.SERVER_MQTT_SECRET, false);
        return MqttGatewayAuthorization.postJson(
                url,
                JSONUtil.toJsonStr(requestBody),
                signatureKey,
                Instant.now());
    }

    @Override
    public Object getDeviceTools(String deviceId) {
        //Получить адрес MQTT-шлюза из параметров системы
        String mqttGatewayUrl = sysParamsService.getValue("server.mqtt_manager_api", true);
        if (StringUtils.isBlank(mqttGatewayUrl) || "null".equals(mqttGatewayUrl)) {
            return null;
        }

        //Получение информации об устройстве
        DeviceEntity device = baseDao.selectById(deviceId);
        if (device == null) {
            return null;
        }

        //Проверьте, принадлежит ли устройство текущему пользователю
        UserDetail user = SecurityUser.getUser();
        if (!device.getUserId().equals(user.getId())) {
            return null;
        }

        //Build clientId
        String macAddress = Optional.ofNullable(device.getMacAddress()).orElse("unknown").replace(":", "_");
        String groupId = Optional.ofNullable(device.getBoard()).orElse("GID_default").replace(":", "_");
        String clientId = StrUtil.format("{}@@@{}@@@{}", groupId, macAddress, macAddress);

        //Построение полного URL
        String url = StrUtil.format("http://{}/api/commands/{}", mqttGatewayUrl, clientId);

        //Сохраняем список всех инструментов
        List<Object> allTools = new ArrayList<>();
        String cursor = null;

        //Цикл для получения данных пагинации
        while (true) {
            //build params
            Map<String, Object> paramsMap = MapUtil.builder(new HashMap<String, Object>())
                    .put("withUserTools", true)
                    .build();
            //Если есть курсор, добавьте его в параметры запроса
            if (StringUtils.isNotBlank(cursor)) {
                paramsMap.put("cursor", cursor);
            }

            //инициатор запроса сборки
            Map<String, Object> payload = MapUtil
                    .builder(new HashMap<String, Object>())
                    .put("jsonrpc", "2.0")
                    .put("id", 2)
                    .put("method", "tools/list")
                    .put("params", paramsMap)
                    .build();

            Map<String, Object> requestBody = MapUtil
                    .builder(new HashMap<String, Object>())
                    .put("type", "mcp")
                    .put("payload", payload)
                    .build();

            String resultMessage = postToMqttGateway(url, requestBody);

            //parse response
            if (StringUtils.isBlank(resultMessage)) {
                break;
            }

            JSONObject jsonObject = JSONUtil.parseObj(resultMessage);
            if (!jsonObject.getBool("success", false)) {
                break;
            }

            JSONObject data = jsonObject.getJSONObject("data");
            if (data == null) {
                break;
            }

            //Получить список инструментов для текущей страницы
            JSONArray tools = data.getJSONArray("tools");
            if (tools != null && !tools.isEmpty()) {
                allTools.addAll(tools);
            }

            //получить курсор для следующей страницы
            String nextCursor = data.getStr("nextCursor");
            if (StringUtils.isBlank(nextCursor)) {
                //Нет следующей страницы
                break;
            }
            cursor = nextCursor;
        }

        //Сборка возвращенных результатов
        if (allTools.isEmpty()) {
            return null;
        }

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("tools", allTools);
        return resultData;
    }

    @Override
    public Object callDeviceTool(String deviceId, String toolName, Map<String, Object> arguments) {
        //Получить адрес MQTT-шлюза из параметров системы
        String mqttGatewayUrl = sysParamsService.getValue("server.mqtt_manager_api", true);
        if (StringUtils.isBlank(mqttGatewayUrl) || "null".equals(mqttGatewayUrl)) {
            return null;
        }

        //Получение информации об устройстве
        DeviceEntity device = baseDao.selectById(deviceId);
        if (device == null) {
            return null;
        }

        //Проверьте, принадлежит ли устройство текущему пользователю
        UserDetail user = SecurityUser.getUser();
        if (!device.getUserId().equals(user.getId())) {
            return null;
        }

        //Build clientId
        String macAddress = Optional.ofNullable(device.getMacAddress()).orElse("unknown").replace(":", "_");
        String groupId = Optional.ofNullable(device.getBoard()).orElse("GID_default").replace(":", "_");
        String clientId = StrUtil.format("{}@@@{}@@@{}", groupId, macAddress, macAddress);

        //Построение полного URL
        String url = StrUtil.format("http://{}/api/commands/{}", mqttGatewayUrl, clientId);

        //инициатор запроса сборки
        Map<String, Object> params = MapUtil
                .builder(new HashMap<String, Object>())
                .put("name", toolName)
                .put("arguments", arguments)
                .build();

        Map<String, Object> payload = MapUtil
                .builder(new HashMap<String, Object>())
                .put("jsonrpc", "2.0")
                .put("id", 2)
                .put("method", "tools/call")
                .put("params", params)
                .build();

        Map<String, Object> requestBody = MapUtil
                .builder(new HashMap<String, Object>())
                .put("type", "mcp")
                .put("payload", payload)
                .build();

        String resultMessage = postToMqttGateway(url, requestBody);

        //parse response
        if (StringUtils.isNotBlank(resultMessage)) {
            cn.hutool.json.JSONObject jsonObject = JSONUtil.parseObj(resultMessage);
            if (jsonObject.getBool("success", false)) {
                cn.hutool.json.JSONObject data = jsonObject.getJSONObject("data");
                if (data != null) {
                    cn.hutool.json.JSONArray content = data.getJSONArray("content");
                    if (content != null && content.size() > 0) {
                        cn.hutool.json.JSONObject firstContent = content.getJSONObject(0);
                        if (firstContent != null && "text".equals(firstContent.getStr("type"))) {
                            String text = firstContent.getStr("text");
                            if (StringUtils.isNotBlank(text)) {
                                String trimmedText = text.trim();
                                if (trimmedText.startsWith("{") || trimmedText.startsWith("[")) {
                                    try {
                                        return JSONUtil.parseObj(trimmedText);
                                    } catch (Exception e) {
                                        return trimmedText;
                                    }
                                } else if ("true".equals(trimmedText)) {
                                    return true;
                                } else if ("false".equals(trimmedText)) {
                                    return false;
                                } else {
                                    return trimmedText;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
