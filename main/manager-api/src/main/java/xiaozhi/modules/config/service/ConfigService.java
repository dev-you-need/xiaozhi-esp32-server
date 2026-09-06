package xiaozhi.modules.config.service;

import java.util.List;
import java.util.Map;

public interface ConfigService {
    /**
     * Получение конфигурации сервера
     *
     * @param isCache использовать кэш
     * @return информация конфигурации
     */
    Map<String, Object> getConfig(Boolean isCache);

    /**
     * Получение конфигурации моделей агента
     *
     * @param macAddress     MAC-адрес
     * @param selectedModule инстанцированные клиентом модели
     * @return информация конфигурации моделей
     */
    Map<String, Object> getAgentModels(String macAddress, Map<String, String> selectedModule);

    /**
     * Получение заменяющих слов агента
     *
     * @param macAddress MAC-адрес устройства
     * @return список заменяющих слов, формат: ["шаблон1|шаблон01", "шаблон2|шаблон02"]
     */
    List<String> getCorrectWords(String macAddress);
}
