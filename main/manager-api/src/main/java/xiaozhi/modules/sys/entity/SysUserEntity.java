package xiaozhi.modules.sys.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xiaozhi.common.entity.BaseEntity;

/**
 * Пользователь системы */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_user")
public class SysUserEntity extends BaseEntity {
    /**
     * имя пользователя     */
    private String username;
    /**
     * Пароль     */
    private String password;
    /**
     * Суперадминистратор 0: Нет 1: Да     */
    private Integer superAdmin;
    /**
     * Состояние 0: Деактивировано 1: Нормально     */
    private Integer status;
    /**
     * Обновлено     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updater;
    /**
     * Время обновления     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateDate;

}