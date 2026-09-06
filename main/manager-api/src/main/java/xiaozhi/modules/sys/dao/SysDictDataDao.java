package xiaozhi.modules.sys.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import org.apache.ibatis.annotations.Param;
import xiaozhi.common.dao.BaseDao;
import xiaozhi.modules.sys.entity.SysDictDataEntity;
import xiaozhi.modules.sys.vo.SysDictDataItem;

/**
 * Данные словаря
 */
@Mapper
public interface SysDictDataDao extends BaseDao<SysDictDataEntity> {

    List<SysDictDataItem> getDictDataByType(String dictType);

    /**
     * Получить код типа словаря по ID типа словаря
     * 
     * @param dictTypeId ID типа словаря
     * @return Код типа словаря
     */
    String getTypeByTypeId(Long dictTypeId);

    /**
     * Получить набор кодов типов словаря по набору ID данных словаря
     */
    List<String> getDictTypesByIdList(@Param("dictDataIdList") List<Long> dictDataIdList);
}
