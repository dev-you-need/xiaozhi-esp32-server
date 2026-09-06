package xiaozhi.common.xss;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Параметры конфигурации XSS
 * Copyright (c) 人人开源 All rights reserved.
 * Website: https://www.renren.io
 */
@Data
@ConfigurationProperties(prefix = "renren.xss")
public class XssProperties {
    /**
     * Включена ли XSS-фильтрация
     */
    private boolean enabled;
    /**
     * Список исключённых URL
     */
    private List<String> excludeUrls = Collections.emptyList();
}
