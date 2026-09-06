package xiaozhi.common.xss;

import org.apache.commons.lang3.StringUtils;

import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.exception.RenException;

/**
 * Фильтрация SQL
 * Copyright (c) 人人开源 All rights reserved.
 * Website: https://www.renren.io
 */
public class SqlFilter {

    /**
     * Фильтрация SQL-инъекций
     *
     * @param str проверяемая строка
     */
    public static String sqlInject(String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }
        // Удаление символов ', ", ;, \
        str = str.replace("'", "");
        str = str.replace("\"", "");
        str = str.replace(";", "");
        str = str.replace("\\", "");

        // Преобразование в нижний регистр
        str = str.toLowerCase();

        // Недопустимые символы
        String[] keywords = { "master", "truncate", "insert", "select", "delete", "update", "declare", "alter",
                "drop" };

        // Проверка на наличие недопустимых символов
        for (String keyword : keywords) {
            if (str.contains(keyword)) {
                throw new RenException(ErrorCode.INVALID_SYMBOL);
            }
        }

        return str;
    }
}
