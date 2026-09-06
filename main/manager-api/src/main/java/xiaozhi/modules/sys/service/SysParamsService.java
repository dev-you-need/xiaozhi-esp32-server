package xiaozhi.modules.sys.service;

import java.util.List;
import java.util.Map;

import xiaozhi.common.page.PageData;
import xiaozhi.common.service.BaseService;
import xiaozhi.modules.sys.dto.SysParamsDTO;
import xiaozhi.modules.sys.entity.SysParamsEntity;

/**
 * Управление параметрами
 */
public interface SysParamsService extends BaseService<SysParamsEntity> {

    PageData<SysParamsDTO> page(Map<String, Object> params);

    List<SysParamsDTO> list(Map<String, Object> params);

    SysParamsDTO get(Long id);

    void save(SysParamsDTO dto);

    void update(SysParamsDTO dto);

    void delete(String[] ids);

    /**
     * Получить значение параметра по коду параметра
     *
     * @param paramCode Код параметра
     * @param fromCache Получать ли из кэша
     */
    String getValue(String paramCode, Boolean fromCache);

    /**
     * Получить объект Object значения по коду параметра
     *
     * @param paramCode Код параметра
     * @param clazz     Объект Object
     */
    <T> T getValueObject(String paramCode, Class<T> clazz);

    /**
     * Обновить значение по коду параметра
     *
     * @param paramCode  Код параметра
     * @param paramValue Значение параметра
     */
    int updateValueByCode(String paramCode, String paramValue);

    /**
     * Инициализировать секретный ключ сервера
     */
    void initServerSecret();

    /**
     * Получить конфигурацию меню функций системы
     *
     * @param fromCache Получать ли из кэша
     * @return JSON-строка конфигурации меню функций системы
     */
    String getSystemWebMenu(boolean fromCache);

    /**
     * Обновить конфигурацию меню функций системы (автоматическая обработка связанных с функциями плагинов)
     *
     * @param configJson Новая JSON-строка конфигурации меню функций системы
     */
    void updateSystemWebMenu(String configJson);
}
