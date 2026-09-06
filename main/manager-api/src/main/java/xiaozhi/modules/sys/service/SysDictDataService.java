package xiaozhi.modules.sys.service;

import java.util.List;
import java.util.Map;

import xiaozhi.common.page.PageData;
import xiaozhi.common.service.BaseService;
import xiaozhi.modules.sys.dto.SysDictDataDTO;
import xiaozhi.modules.sys.entity.SysDictDataEntity;
import xiaozhi.modules.sys.vo.SysDictDataItem;
import xiaozhi.modules.sys.vo.SysDictDataVO;

/**
 * Словарь данных
 */
public interface SysDictDataService extends BaseService<SysDictDataEntity> {

    /**
     * Постраничный запрос информации словаря данных
     *
     * @param params Параметры запроса, включая информацию о постраничности и условия запроса
     * @return Возвращает результат постраничного запроса словаря данных
     */
    PageData<SysDictDataVO> page(Map<String, Object> params);

    /**
     * Получить сущность словаря данных по ID
     *
     * @param id Уникальный идентификатор сущности словаря данных
     * @return Возвращает подробную информацию о сущности словаря данных
     */
    SysDictDataVO get(Long id);

    /**
     * Сохранить новый элемент словаря данных
     *
     * @param dto Объект передачи данных для сохранения элемента словаря данных
     */
    void save(SysDictDataDTO dto);

    /**
     * Обновить элемент словаря данных
     *
     * @param dto Объект передачи данных для обновления элемента словаря данных
     */
    void update(SysDictDataDTO dto);

    /**
     * Удалить элемент словаря данных
     *
     * @param ids Массив ID элементов словаря данных для удаления
     */
    void delete(Long[] ids);

    /**
     * Удалить данные словаря по ID типа словаря
     *
     * @param dictTypeId ID типа словаря
     */
    void deleteByTypeId(Long dictTypeId);

    /**
     * Получить список данных словаря по типу словаря
     *
     * @param dictType Тип словаря
     * @return Возвращает список данных словаря
     */
    List<SysDictDataItem> getDictDataByType(String dictType);

}