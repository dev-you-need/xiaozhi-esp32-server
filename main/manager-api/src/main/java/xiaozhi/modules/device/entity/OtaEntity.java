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
@TableName("ai_ota")
@Schema(description = "Информация о прошивке")
public class OtaEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "ID")
    private String id;

    @Schema(description = "Название прошивки")
    private String firmwareName;

    @Schema(description = "Тип прошивки")
    private String type;

    @Schema(description = "Номер версии")
    private String version;

    @Schema(description = "Размер файла (Байты)")
    private Long size;

    @Schema(description = "Примечание/Описание")
    private String remark;

    @Schema(description = "Путь к прошивке")
    private String firmwarePath;

    @Schema(description = "Сортировка")
    private Integer sort;

    @Schema(description = "Обновляющий")
    @TableField(fill = FieldFill.UPDATE)
    private Long updater;

    @Schema(description = "Время обновления")
    @TableField(fill = FieldFill.UPDATE)
    private Date updateDate;

    @Schema(description = "Создатель")
    @TableField(fill = FieldFill.INSERT)
    private Long creator;

    @Schema(description = "Время создания")
    @TableField(fill = FieldFill.INSERT)
    private Date createDate;
}