package xiaozhi.modules.device.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("ai_device_address_book")
@Schema(description = "Адресная книга устройства")
public class DeviceAddressBookEntity {

    @TableId(type = IdType.INPUT)
    @Schema(description = "MAC-адрес этого устройства")
    private String macAddress;

    @Schema(description = "MAC-адрес другого устройства")
    private String targetMac;

    @Schema(description = "Как я называю другого")
    private String alias;

    @Schema(description = "Есть ли разрешение на вызов")
    private Boolean hasPermission;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "Создатель")
    private Long creator;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "Время создания")
    private Date createDate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "Обновляющий")
    private Long updater;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "Время обновления")
    private Date updateDate;
}