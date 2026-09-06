package xiaozhi.common.redis;

/**
 * Класс констант ключей Redis
 * Copyright (c) 人人开源 All rights reserved.
 * Website: https://www.renren.io
 */
public class RedisKeys {
    /**
     * Ключ системных параметров
     */
    public static String getSysParamsKey() {
        return "sys:params";
    }

    /**
     * Ключ кода подтверждения
     */
    public static String getCaptchaKey(String uuid) {
        return "sys:captcha:" + uuid;
    }

    /**
     * Ключ кода подтверждения незарегистрированного устройства
     */
    public static String getDeviceCaptchaKey(String captcha) {
        return "sys:device:captcha:" + captcha;
    }

    /**
     * Ключ ID пользователя
     */
    public static String getUserIdKey(Long userid) {
        return "sys:username:id:" + userid;
    }

    /**
     * Ключ имени модели
     */
    public static String getModelNameById(String id) {
        return "model:name:" + id;
    }

    /**
     * Ключ конфигурации модели
     */
    public static String getModelConfigById(String id) {
        return "model:data:" + id;
    }

    /**
     * Ключ кэша имени голоса
     */
    public static String getTimbreNameById(String id) {
        return "timbre:name:" + id;
    }

    /**
     * Ключ кэша количества устройств
     */
    public static String getAgentDeviceCountById(String id) {
        return "agent:device:count:" + id;
    }

    /**
     * Ключ кэша времени последнего подключения устройства агента
     */
    public static String getAgentDeviceLastConnectedAtById(String id) {
        return "agent:device:lastConnected:" + id;
    }

    /**
     * Ключ кэша конфигурации сервера
     */
    public static String getServerConfigKey() {
        return "server:config";
    }

    /**
     * Ключ кэша деталей голоса
     */
    public static String getTimbreDetailsKey(String id) {
        return "timbre:details:" + id;
    }

    /**
     * Ключ версии
     */
    public static String getVersionKey() {
        return "sys:version";
    }

    /**
     * Ключ ID прошивки OTA
     */
    public static String getOtaIdKey(String uuid) {
        return "ota:id:" + uuid;
    }

    /**
     * Ключ количества загрузок прошивки OTA
     */
    public static String getOtaDownloadCountKey(String uuid) {
        return "ota:download:count:" + uuid;
    }

    /**
     * Ключ кэша данных словаря
     */
    public static String getDictDataByTypeKey(String dictType) {
        return "sys:dict:data:" + dictType;
    }

    /**
     * Ключ кэша ID аудио агента
     */
    public static String getAgentAudioIdKey(String uuid) {
        return "agent:audio:id:" + uuid;
    }

    /**
     * Ключ кэша кода подтверждения SMS
     */
    public static String getSMSValidateCodeKey(String phone) {
        return "sms:Validate:Code:" + phone;
    }

    /**
     * Ключ кэша времени последней отправки кода подтверждения SMS
     */
    public static String getSMSLastSendTimeKey(String phone) {
        return "sms:Validate:Code:" + phone + ":last_send_time";
    }

    /**
     * Ключ кэша количества отправок кода подтверждения SMS за сегодня
     */
    public static String getSMSTodayCountKey(String phone) {
        return "sms:Validate:Code:" + phone + ":today_count";
    }

    /**
     * Ключ маппинга UUID истории чата
     */
    public static String getChatHistoryKey(String uuid) {
        return "agent:chat:history:" + uuid;
    }

    /**
     * Ключ кэша ID аудио клонирования голоса
     */
    public static String getVoiceCloneAudioIdKey(String uuid) {
        return "voiceClone:audio:id:" + uuid;
    }

    /**
     * Ключ кэша базы знаний
     */
    public static String getKnowledgeBaseCacheKey(String datasetId) {
        return "knowledge:base:" + datasetId;
    }

    /**
     * Ключ метки временной регистрации устройства
     */
    public static String getTmpRegisterMacKey(String deviceId) {
        return "tmp_register_mac:" + deviceId;
    }

    /**
     * Привязка устройства OTA
     */
    public static String getOtaActivationCode(String activationCode) {
        return "ota:activation:code:" + activationCode;
    }

    /**
     * Получение информации об устройстве OTA
     */
    public static String getOtaDeviceActivationInfo(String deviceId) {
        return "ota:activation:data:" + deviceId;
    }

    /**
     * Количество загрузок OTA
     */
    public static String getOtaUploadCountKey(Long username) {
        return "ota:upload:count:" + username;
    }

    /**
     * Ключ кэша адресной книги устройств
     */
    public static String getAddressBookKey() {
        return "device:address_book:all";
    }

}
