package xiaozhi.common.utils;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Интернационализация
 * Copyright (c) 人人开源 All rights reserved.
 * Website: https://www.renren.io
 */
public class MessageUtils {
    private static MessageSource messageSource;

    public static String getMessage(int code) {
        return getMessage(code, new String[0]);
    }

    public static String getMessage(int code, String... params) {
        if (messageSource == null) {
            // Отложенная инициализация, чтобы убедиться, что контекст Spring полностью инициализирован
            messageSource = (MessageSource) SpringContextUtils.getBean("messageSource");
        }
        return messageSource.getMessage(code + "", params, LocaleContextHolder.getLocale());
    }
}
