package xiaozhi.modules.sys.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xiaozhi.common.entity.BaseEntity;

/**
 * Управление параметрами */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_params")
public class SysParamsEntity extends BaseEntity {
    /**
     * Кодирование параметров     */
    private String paramCode;
    /**
     * Значение параметра     */
    private String paramValue;
    /**
     * Тип значения: string-string, number-numeric, boolean-boolean, array-array     */
    private String valueType;
    /**
     * Тип 0: Системный параметр 1: Несистемный параметр     */
    private Integer paramType;
    /**
     * Примечание     */
    private String remark;
    /**
     * Обновлено     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updater;
    /**
     * Время обновления     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateDate;

}