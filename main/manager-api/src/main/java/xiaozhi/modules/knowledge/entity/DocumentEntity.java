package xiaozhi.modules.knowledge.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Таблица документов (теневая БД для документов RAGFlow)
 * Соответствующее имя таблицы: ai_knowledge_document
 */
@Data
@TableName(value = "ai_rag_knowledge_document", autoResultMap = true)
@Schema(description = "Таблица документов базы знаний")
public class DocumentEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "Локальный уникальный ID")
    private String id;

    @Schema(description = "ID базы знаний (связь с ai_rag_dataset.dataset_id)")
    private String datasetId;

    @Schema(description = "ID документа RAGFlow (удаленный ID)")
    private String documentId;

    @Schema(description = "Название документа")
    private String name;

    @Schema(description = "Размер файла (Байты)")
    private String size;

    @Schema(description = "Тип файла (pdf/doc/txt и т.д.)")
    private String type;

    @Schema(description = "Метод разбиения на блоки")
    private String chunkMethod;

    @Schema(description = "Конфигурация парсера (JSON строка)")
    private String parserConfig;

    @Schema(description = "Статус доступности (1: включен/нормальный, 0: отключен/недействителен)")
    private String status;

    @Schema(description = "Статус выполнения (UNSTART/RUNNING/CANCEL/DONE/FAIL)")
    private String run;

    @Schema(description = "Прогресс парсинга (0.0 ~ 1.0)")
    private Double progress;

    @Schema(description = "Эскиз (Base64 или URL)")
    private String thumbnail;

    @Schema(description = "Время парсинга (в секундах)")
    private Double processDuration;

    @Schema(description = "Пользовательские метаданные (формат JSON)")
    private String metaFields;

    @Schema(description = "Тип источника (local, s3, url и т.д.)")
    private String sourceType;

    @Schema(description = "Информация об ошибке парсинга")
    private String error;

    @Schema(description = "Количество блоков")
    private Integer chunkCount;

    @Schema(description = "Количество токенов")
    private Long tokenCount;

    @Schema(description = "Включен ли (0: отключен 1: включен)")
    private Integer enabled;

    @Schema(description = "Создатель")
    @TableField(fill = FieldFill.INSERT)
    private Long creator;

    @Schema(description = "Время создания")
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @Schema(description = "Время обновления")
    @TableField(fill = FieldFill.UPDATE)
    private Date updatedAt;

    @Schema(description = "Время последней синхронизации")
    private Date lastSyncAt;
}
