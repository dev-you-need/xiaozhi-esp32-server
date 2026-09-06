package xiaozhi.modules.sys.service;


import java.util.function.Consumer;

/**
 * Определение вспомогательного класса системного пользователя для избежания циклической зависимости с модулем пользователей
 * Например, пользователи и устройства зависят друг от друга: пользователям нужно получить все устройства, а устройствам нужно получить имя пользователя каждого устройства
 * @author zjy
 * @since 2025-4-2
 */
public interface SysUserUtilService {
    /**
     * Присвоить имя пользователя
     * @param userId ID пользователя
     * @param setter Метод присваивания
     */
    void assignUsername( Long userId, Consumer<String> setter);
}
