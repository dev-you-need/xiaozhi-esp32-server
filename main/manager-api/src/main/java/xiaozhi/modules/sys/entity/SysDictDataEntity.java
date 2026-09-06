package xiaozhi.modules.sys.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xiaozhi.common.entity.BaseEntity;

/**
 * Словарь данных
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_dict_data")
public class SysDictDataEntity extends BaseEntity {
    /**
     * ID типа словаря
     */
    private Long dictTypeId;
    /**
     * Метка словаря
     */
    private String dictLabel;
    /**
     * Значение словаря
     */
    private String dictValue;
    /**
     * Примечание
     */
    private String remark;
    /**
     * Сортировка
     */
    private Integer sort;
    /**
     * Обновляющий
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updater;
    /**
     * Время обновления
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateDate;
}