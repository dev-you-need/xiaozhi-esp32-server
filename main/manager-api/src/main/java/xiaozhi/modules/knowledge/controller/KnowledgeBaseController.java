package xiaozhi.modules.knowledge.controller;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.hutool.core.collection.CollUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.page.PageData;
import xiaozhi.common.utils.Result;
import xiaozhi.modules.knowledge.dto.KnowledgeBaseDTO;
import xiaozhi.modules.knowledge.service.KnowledgeBaseService;
import xiaozhi.modules.knowledge.service.KnowledgeManagerService;
import xiaozhi.modules.model.entity.ModelConfigEntity;
import xiaozhi.modules.security.user.SecurityUser;

@AllArgsConstructor
@RestController
@RequestMapping("/datasets")
@Tag(name = "知识库管理")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeManagerService knowledgeManagerService;

    @GetMapping
    @Operation(summary = "分页查询知识库列表")
    @RequiresPermissions("sys:role:normal")
    public Result<PageData<KnowledgeBaseDTO>> getPageList(
            @RequestParam(required = false) String name,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer page_size) {
        //Получить текущий идентификатор пользователя, вошедшего в систему
        Long currentUserId = SecurityUser.getUserId();

        KnowledgeBaseDTO knowledgeBaseDTO = new KnowledgeBaseDTO();
        knowledgeBaseDTO.setName(name);
        knowledgeBaseDTO.setCreator(currentUserId); //Установите идентификатор креатора для фильтрации разрешений

        PageData<KnowledgeBaseDTO> pageData = knowledgeBaseService.getPageList(knowledgeBaseDTO, page, page_size);
        return new Result<PageData<KnowledgeBaseDTO>>().ok(pageData);
    }

    @GetMapping("/{dataset_id}")
    @Operation(summary = "根据知识库ID获取知识库详情")
    @RequiresPermissions("sys:role:normal")
    public Result<KnowledgeBaseDTO> getByDatasetId(@PathVariable("dataset_id") String datasetId) {
        //Получить текущий идентификатор пользователя, вошедшего в систему
        Long currentUserId = SecurityUser.getUserId();

        KnowledgeBaseDTO knowledgeBaseDTO = knowledgeBaseService.getByDatasetId(datasetId);

        //Проверка разрешений: пользователи могут просматривать только базы знаний, которые они создали
        if (knowledgeBaseDTO.getCreator() == null || !knowledgeBaseDTO.getCreator().equals(currentUserId)) {
            throw new RenException(ErrorCode.NO_PERMISSION);
        }

        return new Result<KnowledgeBaseDTO>().ok(knowledgeBaseDTO);
    }

    @PostMapping
    @Operation(summary = "创建知识库")
    @RequiresPermissions("sys:role:normal")
    public Result<KnowledgeBaseDTO> save(@RequestBody @Validated KnowledgeBaseDTO knowledgeBaseDTO) {
        KnowledgeBaseDTO resp = knowledgeBaseService.save(knowledgeBaseDTO);
        return new Result<KnowledgeBaseDTO>().ok(resp);
    }

    @PutMapping("/{dataset_id}")
    @Operation(summary = "更新知识库")
    @RequiresPermissions("sys:role:normal")
    public Result<KnowledgeBaseDTO> update(@PathVariable("dataset_id") String datasetId,
            @RequestBody @Validated KnowledgeBaseDTO knowledgeBaseDTO) {
        //Получить текущий идентификатор пользователя, вошедшего в систему
        Long currentUserId = SecurityUser.getUserId();

        //Сначала получите информацию о существующей базе знаний, чтобы проверить разрешения
        KnowledgeBaseDTO existingKnowledgeBase = knowledgeBaseService.getByDatasetId(datasetId);

        //Проверка разрешений: пользователи могут обновлять только базы знаний, которые они создали сами
        if (existingKnowledgeBase.getCreator() == null || !existingKnowledgeBase.getCreator().equals(currentUserId)) {
            throw new RenException(ErrorCode.NO_PERMISSION);
        }

        //[FIX] Введите идентификатор, чтобы сервисный уровень не находил записи
        knowledgeBaseDTO.setId(existingKnowledgeBase.getId());
        knowledgeBaseDTO.setDatasetId(datasetId);
        KnowledgeBaseDTO resp = knowledgeBaseService.update(knowledgeBaseDTO);
        return new Result<KnowledgeBaseDTO>().ok(resp);
    }

    @DeleteMapping("/{dataset_id}")
    @Operation(summary = "删除单个知识库")
    @Parameter(name = "dataset_id", description = "知识库ID", required = true)
    @RequiresPermissions("sys:role:normal")
    public Result<Void> delete(@PathVariable("dataset_id") String datasetId) {
        //Получить текущий идентификатор пользователя, вошедшего в систему
        Long currentUserId = SecurityUser.getUserId();

        //Сначала получите информацию о существующей базе знаний, чтобы проверить разрешения
        KnowledgeBaseDTO existingKnowledgeBase = knowledgeBaseService.getByDatasetId(datasetId);

        //Проверка разрешений: пользователи могут удалять только базы знаний, которые они создали
        if (existingKnowledgeBase.getCreator() == null || !existingKnowledgeBase.getCreator().equals(currentUserId)) {
            throw new RenException(ErrorCode.NO_PERMISSION);
        }

        //[Исправление архитектуры] Предотвращение потерянных данных и устранение циклических зависимостей путем оркестрации каскадных удалений
        knowledgeManagerService.deleteDatasetWithFiles(datasetId);
        return new Result<>();
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除知识库")
    @Parameter(name = "ids", description = "知识库ID列表，用逗号分隔", required = true)
    @RequiresPermissions("sys:role:normal")
    public Result<Void> deleteBatch(@RequestParam("ids") String ids) {
        if (StringUtils.isBlank(ids)) {
            throw new RenException(ErrorCode.PARAMS_GET_ERROR);
        }

        //Получить текущий идентификатор пользователя, вошедшего в систему
        Long currentUserId = SecurityUser.getUserId();
        List<String> idList = Arrays.asList(ids.split(","));
        List<KnowledgeBaseDTO> knowledgeBaseDTOs = Optional.ofNullable(knowledgeBaseService.getByDatasetIdList(idList))
                .orElseGet(ArrayList::new);
        if (CollUtil.isNotEmpty(knowledgeBaseDTOs)) {
            knowledgeBaseDTOs.forEach(item -> {
                //Проверка разрешений: пользователи могут удалять только базы знаний, которые они создали
                if (item.getCreator() == null || !item.getCreator().equals(currentUserId)) {
                    throw new RenException(ErrorCode.NO_PERMISSION);
                }
                //[Исправление архитектуры] Удалить по каскаду уровней оркестровки
                knowledgeManagerService.deleteDatasetWithFiles(item.getDatasetId());
            });
        }
        return new Result<>();
    }

    @GetMapping("/rag-models")
    @Operation(summary = "获取RAG模型列表")
    @RequiresPermissions("sys:role:normal")
    public Result<List<ModelConfigEntity>> getRAGModels() {
        List<ModelConfigEntity> result = knowledgeBaseService.getRAGModels();
        return new Result<List<ModelConfigEntity>>().ok(result);
    }
}
