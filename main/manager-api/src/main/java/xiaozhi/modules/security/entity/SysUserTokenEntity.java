package xiaozhi.modules.security.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * Маркер пользователя системы */
@Data
@TableName("sys_user_token")
public class SysUserTokenEntity implements Serializable {

    /**
     * id
     */
    @TableId
    private Long id;
    /**
     * ID пользователя     */
    private Long userId;
    /**
     * Пользовательский токен     */
    private String token;
    /**
     * Время истечения срока действия     */
    private Date expireDate;
    /**
     * Время обновления     */
    private Date updateDate;
    /**
     * Время создания     */
    @TableField(fill = FieldFill.INSERT)
    private Date createDate;

}