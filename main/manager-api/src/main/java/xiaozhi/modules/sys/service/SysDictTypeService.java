package xiaozhi.modules.sys.service;

import java.util.List;
import java.util.Map;

import xiaozhi.common.page.PageData;
import xiaozhi.common.service.BaseService;
import xiaozhi.modules.sys.dto.SysDictTypeDTO;
import xiaozhi.modules.sys.entity.SysDictTypeEntity;
import xiaozhi.modules.sys.vo.SysDictTypeVO;

/**
 * Словарь данных
 */
public interface SysDictTypeService extends BaseService<SysDictTypeEntity> {

    /**
     * Постраничный запрос информации о типах словаря
     *
     * @param params Параметры запроса, включая информацию о постраничности и условия запроса
     * @return Возвращает данные о типах словаря постранично
     */
    PageData<SysDictTypeVO> page(Map<String, Object> params);

    /**
     * Получить информацию о типе словаря по ID
     *
     * @param id ID типа словаря
     * @return Возвращает объект типа словаря
     */
    SysDictTypeVO get(Long id);

    /**
     * Сохранить информацию о типе словаря
     *
     * @param dto Объект передачи данных типа словаря
     */
    void save(SysDictTypeDTO dto);

    /**
     * Обновить информацию о типе словаря
     *
     * @param dto Объект передачи данных типа словаря
     */
    void update(SysDictTypeDTO dto);

    /**
     * Удалить информацию о типе словаря
     *
     * @param ids Массив ID типов словаря для удаления
     */
    void delete(Long[] ids);

    /**
     * Вывести список всей информации о типах словаря
     *
     * @return Возвращает список типов словаря
     */
    List<SysDictTypeVO> list(Map<String, Object> params);
}