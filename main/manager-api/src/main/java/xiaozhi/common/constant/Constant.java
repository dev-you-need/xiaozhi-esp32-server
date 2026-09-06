package xiaozhi.common.constant;

import lombok.Getter;

/**
 * Константы
 * Copyright (c) 人人开源 All rights reserved.
 * Website: https://www.renren.io
 */
public interface Constant {
    /**
     * Успех
     */
    int SUCCESS = 1;
    /**
     * Ошибка
     */
    int FAIL = 0;
    /**
     * OK
     */
    String OK = "OK";
    /**
     * Идентификатор пользователя
     */
    String USER_KEY = "userId";
    /**
     * Идентификатор корневого узла меню
     */
    Long MENU_ROOT = 0L;
    /**
     * Идентификатор корневого узла отдела
     */
    Long DEPT_ROOT = 0L;
    /**
     * Идентификатор корневого узла словаря данных
     */
    Long DICT_ROOT = 0L;
    /**
     * По возрастанию
     */
    String ASC = "asc";
    /**
     * По убыванию
     */
    String DESC = "desc";
    /**
     * Имя поля даты создания
     */
    String CREATE_DATE = "create_date";

    /**
     * Имя поля даты создания
     */
    String ID = "id";

    /**
     * Фильтрация прав доступа к данным
     */
    String SQL_FILTER = "sqlFilter";

    /**
     * Текущий номер страницы
     */
    String PAGE = "page";
    /**
     * Количество записей на странице
     */
    String LIMIT = "limit";
    /**
     * Поле сортировки
     */
    String ORDER_FIELD = "orderField";
    /**
     * Способ сортировки
     */
    String ORDER = "order";

    /**
     * Идентификатор авторизации в заголовке запроса
     */
    String AUTHORIZATION = "Authorization";

    /**
     * Серверный ключ
     */
    String SERVER_SECRET = "server.secret";

    /**
     * Публичный ключ SM2
     */
    String SM2_PUBLIC_KEY = "server.public_key";

    /**
     * Приватный ключ SM2
     */
    String SM2_PRIVATE_KEY = "server.private_key";

    /**
     * Адрес WebSocket
     */
    String SERVER_WEBSOCKET = "server.websocket";

    /**
     * Конфигурация MQTT-шлюза
     */
    String SERVER_MQTT_GATEWAY = "server.mqtt_gateway";

    /**
     * Адрес OTA
     */
    String SERVER_OTA = "server.ota";

    /**
     * Разрешена ли регистрация пользователей
     */
    String SERVER_ALLOW_USER_REGISTER = "server.allow_user_register";

    /**
     * Адрес панели управления для отображения шестизначного кода подтверждения
     */
    String SERVER_FRONTED_URL = "server.fronted_url";

    /**
     * Разделитель пути
     */
    String FILE_EXTENSION_SEG = ".";

    /**
     * Путь MCP-точки доступа
     */
    String SERVER_MCP_ENDPOINT = "server.mcp_endpoint";

    /**
     * Путь MCP-точки доступа
     */
    String SERVER_VOICE_PRINT = "server.voice_print";

    /**
     * Ключ MQTT
     */
    String SERVER_MQTT_SECRET = "server.mqtt_signature_key";

    /**
     * Переключатель аутентификации WebSocket
     */
    String SERVER_AUTH_ENABLED = "server.auth.enabled";

    /**
     * Конфигурация системного меню
     */
    String SYSTEM_WEB_MENU = "system-web.menu";

    /**
     * Без памяти
     */
    String MEMORY_NO_MEM = "Memory_nomem";

    /**
     * Только отправка истории чата (без суммаризации памяти)
     */
    String MEMORY_MEM_REPORT_ONLY = "Memory_mem_report_only";

    /**
     * Память Mem0AI
     */
    String MEMORY_MEM0AI = "Memory_mem0ai";

    /**
     * Память PowerMem
     */
    String MEMORY_POWERMEM = "Memory_powermem";

    /**
     * Двухканальное клонирование голоса Volcengine
     */
    String VOICE_CLONE_HUOSHAN_DOUBLE_STREAM = "huoshan_double_stream";

    /**
     * Тип конфигурации RAG
     */
    String RAG_CONFIG_TYPE = "RAG";

    enum SysBaseParam {
        /**
         * Номер ICP
         */
        BEIAN_ICP_NUM("server.beian_icp_num"),
        /**
         * Номер GA
         */
        BEIAN_GA_NUM("server.beian_ga_num"),
        /**
         * Имя системы
         */
        SERVER_NAME("server.name");

        private String value;

        SysBaseParam(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Статус обучения
     */
    enum TrainStatus {
        /**
         * Не обучено
         */
        NOT_TRAINED(0),
        /**
         * Обучение
         */
        TRAINING(1),
        /**
         * Обучено
         */
        TRAINED(2),
        /**
         * Ошибка обучения
         */
        TRAIN_FAILED(3);

        private final int code;

        TrainStatus(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * Системные SMS
     */
    enum SysMSMParam {
        /**
         * ID ключа авторизации Aliyun
         */
        ALIYUN_SMS_ACCESS_KEY_ID("aliyun.sms.access_key_id"),
        /**
         * Ключ авторизации Aliyun
         */
        ALIYUN_SMS_ACCESS_KEY_SECRET("aliyun.sms.access_key_secret"),
        /**
         * Подпись SMS Aliyun
         */
        ALIYUN_SMS_SIGN_NAME("aliyun.sms.sign_name"),
        /**
         * Шаблон SMS Aliyun
         */
        ALIYUN_SMS_SMS_CODE_TEMPLATE_CODE("aliyun.sms.sms_code_template_code"),
        /**
         * Максимальное количество SMS на номер
         */
        SERVER_SMS_MAX_SEND_COUNT("server.sms_max_send_count"),
        /**
         * Включена ли регистрация по телефону
         */
        SERVER_ENABLE_MOBILE_REGISTER("server.enable_mobile_register");

        private String value;

        SysMSMParam(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Состояние данных
     */
    enum DataOperation {
        /**
         * Вставка
         */
        INSERT("I"),
        /**
         * Изменено
         */
        UPDATE("U"),
        /**
         * Удалено
         */
        DELETE("D");

        private String value;

        DataOperation(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @Getter
    enum ChatHistoryConfEnum {
        IGNORE(0, "不记录"),
        RECORD_TEXT(1, "记录文本"),
        RECORD_TEXT_AUDIO(2, "文本音频都记录");

        private final int code;
        private final String name;

        ChatHistoryConfEnum(int code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    /**
     * Версия
     */
    public static final String VERSION = "0.9.6";

    /**
     * Недействительный URL прошивки
     */
    String INVALID_FIRMWARE_URL = "http://xiaozhi.server.com:8002/xiaozhi/otaMag/download/NOT_ACTIVATED_FIRMWARE_THIS_IS_A_INVALID_URL";

    /**
     * Тип словаря
     */
    enum DictType {
        /**
         * Код региона телефона
         */
        MOBILE_AREA("MOBILE_AREA");

        private String value;

        DictType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}