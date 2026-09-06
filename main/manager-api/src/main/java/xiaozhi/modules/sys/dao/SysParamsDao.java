package xiaozhi.modules.sys.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import xiaozhi.common.dao.BaseDao;
import xiaozhi.modules.sys.entity.SysParamsEntity;

/**
 * Управление параметрами
 */
@Mapper
public interface SysParamsDao extends BaseDao<SysParamsEntity> {
    /**
     * Запрос значения по коду параметра
     *
     * @param paramCode Код параметра
     * @return Значение параметра
     */
    String getValueByCode(String paramCode);

    /**
     * Получить список кодов параметров
     *
     * @param ids ids
     * @return Возвращает список кодов параметров
     */
    List<String> getParamCodeList(String[] ids);

    /**
     * Обновить значение по коду параметра
     *
     * @param paramCode  Код параметра
     * @param paramValue Значение параметра
     */
    int updateValueByCode(@Param("paramCode") String paramCode, @Param("paramValue") String paramValue);
}
