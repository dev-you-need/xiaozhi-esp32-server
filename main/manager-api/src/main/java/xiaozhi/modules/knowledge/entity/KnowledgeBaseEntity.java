package xiaozhi.modules.knowledge.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName(value = "ai_rag_dataset", autoResultMap = true)
@Schema(description = "Таблица базы знаний")
public class KnowledgeBaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "Уникальный идентификатор")
    private String id;

    @Schema(description = "ID базы знаний")
    private String datasetId;

//    @Deprecated
    @Schema(description = "ID конфигурации модели RAG (указатель на учетные данные RAGFlow)")
    private String ragModelId;

    @Schema(description = "ID арендатора")
    private String tenantId;

    @Schema(description = "Название базы знаний")
    private String name;

    @Schema(description = "Аватар базы знаний (Base64)")
    private String avatar;

    @Schema(description = "Описание базы знаний")
    private String description;

    @Schema(description = "Название модели встраивания")
    private String embeddingModel;

    @Schema(description = "Настройки разрешений: me/team")
    private String permission;

    @Schema(description = "Метод разбиения на блоки")
    private String chunkMethod;

    @Schema(description = "Конфигурация парсера (JSON строка)")
    private String parserConfig;

    @Schema(description = "Общее количество блоков")
    private Long chunkCount;

    @Schema(description = "Общее количество документов")
    private Long documentCount;

    @Schema(description = "Общее количество токенов")
    private Long tokenNum;

    @Schema(description = "Статус (0: отключен 1: включен)")
    private Integer status;

    @Schema(description = "Создатель")
    @TableField(fill = FieldFill.INSERT)
    private Long creator;

    @Schema(description = "Время создания")
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @Schema(description = "Обновляющий")
    @TableField(fill = FieldFill.UPDATE)
    private Long updater;

    @Schema(description = "Время обновления")
    @TableField(fill = FieldFill.UPDATE)
    private Date updatedAt;
}