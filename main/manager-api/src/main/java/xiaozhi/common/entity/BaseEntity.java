package xiaozhi.common.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;

import lombok.Data;

/**
 * Базовый класс сущности, который должны наследовать все сущности
 * Copyright (c) 人人开源 All rights reserved.
 * Website: https://www.renren.io
 */
@Data
public abstract class BaseEntity implements Serializable {
    /**
     * id
     */
    @TableId
    private Long id;
    /**
     * Создатель
     */
    @TableField(fill = FieldFill.INSERT)
    private Long creator;
    /**
     * Время создания
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createDate;
}