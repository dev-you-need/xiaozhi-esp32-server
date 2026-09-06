package xiaozhi.common.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import cn.hutool.json.JSONObject;

/**
 * Утилита обработки конфиденциальных данных
 */
public class SensitiveDataUtils {

    // Список конфиденциальных полей
    private static final Set<String> SENSITIVE_FIELDS = new HashSet<>(Arrays.asList(
            "api_key", "personal_access_token", "access_token", "token",
            "secret", "access_key_secret", "secret_key"));

    /**
     * Проверка, является ли поле конфиденциальным
     */
    public static boolean isSensitiveField(String fieldName) {
        return StringUtils.isNotBlank(fieldName) && SENSITIVE_FIELDS.contains(fieldName.toLowerCase());
    }

    /**
     * Маскировка средней части строки
     */
    public static String maskMiddle(String value) {
        if (StringUtils.isBlank(value) || value.length() == 1) {
            return value;
        }

        int length = value.length();
        if (length <= 8) {
            // Для коротких строк — сохранить первые 2 и последние 2 символа
            return value.substring(0, 2) + "****" + value.substring(length - 2);
        } else {
            // Для длинных строк — сохранить первые 4 и последние 4 символа
            int maskLength = length - 8;
            StringBuilder maskBuilder = new StringBuilder();
            for (int i = 0; i < maskLength; i++) {
                maskBuilder.append('*');
            }
            return value.substring(0, 4) + maskBuilder.toString() + value.substring(length - 4);
        }
    }

    /**
     * Проверка, обработана ли строка маской
     */
    public static boolean isMaskedValue(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        // Значение маски содержит как минимум 4 подряд идущих символа *
        return value.contains("****");
    }

    /**
     * Обработка конфиденциальных полей в JSONObject
     */
    public static JSONObject maskSensitiveFields(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        JSONObject result = new JSONObject();

        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);

            if (SENSITIVE_FIELDS.contains(key.toLowerCase()) && value instanceof String) {
                result.set(key, maskMiddle((String) value));
            } else if (value instanceof JSONObject) {
                result.set(key, maskSensitiveFields((JSONObject) value));
            } else {
                result.set(key, value);
            }
        }

        return result;
    }

    /**
     * Сравнение конфиденциальных полей двух JSONObject
     * Особое сравнение конфиденциальных полей, таких как api_key
     */
    public static boolean isSensitiveDataEqual(JSONObject original, JSONObject updated) {
        if (original == null && updated == null) {
            return true;
        }
        if (original == null || updated == null) {
            return false;
        }

        // Извлечение и сравнение определённых конфиденциальных полей
        return compareSpecificSensitiveFields(original, updated, "api_key") &&
                compareSpecificSensitiveFields(original, updated, "personal_access_token") &&
                compareSpecificSensitiveFields(original, updated, "access_token") &&
                compareSpecificSensitiveFields(original, updated, "token") &&
                compareSpecificSensitiveFields(original, updated, "secret") &&
                compareSpecificSensitiveFields(original, updated, "access_key_secret") &&
                compareSpecificSensitiveFields(original, updated, "secret_key");
    }

    /**
     * Сравнение определённого конфиденциального поля в двух JSON-объектах
     * Обход всего дерева JSON-объектов для поиска и сравнения указанных конфиденциальных полей
     */
    private static boolean compareSpecificSensitiveFields(JSONObject original, JSONObject updated, String fieldName) {
        // Извлечение указанного конфиденциального поля из исходного объекта
        Map<String, String> originalFields = new HashMap<>();
        extractSpecificSensitiveField(original, originalFields, fieldName, "");

        // Извлечение указанного конфиденциального поля из обновлённого объекта
        Map<String, String> updatedFields = new HashMap<>();
        extractSpecificSensitiveField(updated, updatedFields, fieldName, "");

        // Если количество полей различается — были добавления или удаления
        if (originalFields.size() != updatedFields.size()) {
            return false;
        }

        // Сравнение значений каждого поля
        for (Map.Entry<String, String> entry : originalFields.entrySet()) {
            String key = entry.getKey();
            String originalValue = entry.getValue();
            String updatedValue = updatedFields.get(key);

            if (updatedValue == null || !updatedValue.equals(originalValue)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Рекурсивное извлечение конфиденциальных полей по имени из JSON-объекта
     */
    private static void extractSpecificSensitiveField(JSONObject jsonObject, Map<String, String> fieldsMap,
            String targetFieldName, String parentPath) {
        if (jsonObject == null) {
            return;
        }

        for (String key : jsonObject.keySet()) {
            String fullPath = parentPath.isEmpty() ? key : parentPath + "." + key;
            Object value = jsonObject.get(key);

            if (value instanceof JSONObject) {
                // Рекурсивная обработка вложенного JSON-объекта
                extractSpecificSensitiveField((JSONObject) value, fieldsMap, targetFieldName, fullPath);
            } else if (value instanceof String && key.equalsIgnoreCase(targetFieldName)) {
                // Найдено целевое конфиденциальное поле — сохранение его пути и значения
                fieldsMap.put(fullPath, (String) value);
            }
        }
    }
}
