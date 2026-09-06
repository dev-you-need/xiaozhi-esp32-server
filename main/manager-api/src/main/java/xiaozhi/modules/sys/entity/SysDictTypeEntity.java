package xiaozhi.modules.sys.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xiaozhi.common.entity.BaseEntity;

/**
 * Тип словаря */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_dict_type")
public class SysDictTypeEntity extends BaseEntity {
    /**
     * Кодировка типа словаря     */
    private String dictType;
    /**
     * Название словаря     */
    private String dictName;
    /**
     * Примечание     */
    private String remark;
    /**
     * Сортировка     */
    private Integer sort;
    /**
     * Обновлено     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updater;
    /**
     * Время обновления     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateDate;
}