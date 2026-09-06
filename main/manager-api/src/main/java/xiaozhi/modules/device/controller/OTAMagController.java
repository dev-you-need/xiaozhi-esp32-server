package xiaozhi.modules.device.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cn.hutool.crypto.digest.DigestUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.page.PageData;
import xiaozhi.common.redis.RedisKeys;
import xiaozhi.common.redis.RedisUtils;
import xiaozhi.common.utils.Result;
import xiaozhi.common.validator.ValidatorUtils;
import xiaozhi.modules.device.entity.OtaEntity;
import xiaozhi.modules.device.service.OtaService;
import xiaozhi.modules.security.user.SecurityUser;
import xiaozhi.modules.sys.enums.SuperAdminEnum;
import xiaozhi.modules.sys.service.SysParamsService;

@Tag(name = "Управление обновлениями прошивки", description = "Интерфейсы OTA")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/otaMag")
public class OTAMagController {
    private static final Logger logger = LoggerFactory.getLogger(OTAController.class);
    private final OtaService otaService;
    private final RedisUtils redisUtils;
    private final SysParamsService sysParamsService;

    @GetMapping
    @Operation(summary = "Постраничный запрос информации о прошивке OTA")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "Номер текущей страницы, начинается с 1", required = true),
            @Parameter(name = Constant.LIMIT, description = "Количество записей на странице", required = true)
    })
    @RequiresPermissions("sys:role:superAdmin")
    public Result<PageData<OtaEntity>> page(@Parameter(hidden = true) @RequestParam Map<String, Object> params) {
        ValidatorUtils.validateEntity(params);
        PageData<OtaEntity> page = otaService.page(params);
        return new Result<PageData<OtaEntity>>().ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "Информация о прошивке OTA")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<OtaEntity> get(@PathVariable("id") String id) {
        OtaEntity data = otaService.selectById(id);
        return new Result<OtaEntity>().ok(data);
    }

    @PostMapping
    @Operation(summary = "Сохранить информацию о прошивке OTA")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<Void> save(@RequestBody OtaEntity entity) {
        if (entity == null) {
            return new Result<Void>().error("Информация о прошивке не может быть пустой");
        }
        if (StringUtils.isBlank(entity.getFirmwareName())) {
            return new Result<Void>().error("Название прошивки не может быть пустым");
        }
        if (StringUtils.isBlank(entity.getType())) {
            return new Result<Void>().error("Тип прошивки не может быть пустым");
        }
        if (StringUtils.isBlank(entity.getVersion())) {
            return new Result<Void>().error("Номер версии не может быть пустым");
        }
        try {
            otaService.save(entity);
            return new Result<Void>();
        } catch (RuntimeException e) {
            return new Result<Void>().error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удаление OTA")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<Void> delete(@PathVariable("id") String[] ids) {
        if (ids == null || ids.length == 0) {
            return new Result<Void>().error("ID удаляемой прошивки не может быть пустым");
        }
        otaService.delete(ids);
        return new Result<Void>();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Изменить информацию о прошивке OTA")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<?> update(@PathVariable("id") String id, @RequestBody OtaEntity entity) {
        if (entity == null) {
            return new Result<>().error("Информация о прошивке не может быть пустой");
        }
        entity.setId(id);
        try {
            otaService.update(entity);
            return new Result<>();
        } catch (RuntimeException e) {
            return new Result<>().error(e.getMessage());
        }
    }

    @GetMapping("/getDownloadUrl/{id}")
    @Operation(summary = "Получить ссылку на скачивание прошивки OTA")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<String> getDownloadUrl(@PathVariable("id") String id) {
        String uuid = UUID.randomUUID().toString();
        redisUtils.set(RedisKeys.getOtaIdKey(uuid), id);
        return new Result<String>().ok(uuid);
    }

    @GetMapping("/download/{uuid}")
    @Operation(summary = "Скачать файл прошивки")
    public ResponseEntity<byte[]> downloadFirmware(@PathVariable("uuid") String uuid) {
        String id = (String) redisUtils.get(RedisKeys.getOtaIdKey(uuid));
        if (StringUtils.isBlank(id)) {
            return ResponseEntity.notFound().build();
        }

        // Проверить количество загрузок
        String downloadCountKey = RedisKeys.getOtaDownloadCountKey(uuid);
        Integer downloadCount = (Integer) Optional.ofNullable(redisUtils.get(downloadCountKey)).orElse(0);

        // Если количество загрузок превышает 3, вернуть 404
        if (downloadCount >= 3) {
            redisUtils.delete(List.of(downloadCountKey, RedisKeys.getOtaIdKey(uuid)));
            logger.warn("Download limit exceeded for UUID: {}", uuid);
            return ResponseEntity.notFound().build();
        }

        redisUtils.set(downloadCountKey, downloadCount + 1);

        try {
            // Получить информацию о прошивке
            OtaEntity otaEntity = null;
            if (id.indexOf("file:") == 0) {
                id = id.substring(5);
                otaEntity = new OtaEntity();
                otaEntity.setFirmwarePath(id);
                otaEntity.setType("assets");
                otaEntity.setVersion("1.0.0");
            } else {
                otaEntity = otaService.selectById(id);
            }

            if (otaEntity == null || StringUtils.isBlank(otaEntity.getFirmwarePath())) {
                logger.warn("Firmware not found or path is empty for ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            // Получить путь к файлу - убедиться, что путь абсолютный или правильный относительный путь
            String firmwarePath = otaEntity.getFirmwarePath();
            String originalFilename = otaEntity.getType() + "_" + otaEntity.getVersion();
            Path path;

            // Проверить, является ли путь абсолютным
            if (Paths.get(firmwarePath).isAbsolute()) {
                path = Paths.get(firmwarePath);
            } else {
                // Если это относительный путь, разрешить его из текущего рабочего каталога
                path = Paths.get(System.getProperty("user.dir"), firmwarePath);
            }

            logger.info("Attempting to download firmware for ID: {}, DB path: {}, resolved path: {}",
                    id, firmwarePath, path.toAbsolutePath());

            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                // Попробовать найти имя файла напрямую в каталоге firmware
                String fileName = new File(firmwarePath).getName();
                Path altPath = Paths.get(System.getProperty("user.dir"), "firmware", fileName);

                logger.info("File not found at primary path, trying alternative path: {}", altPath.toAbsolutePath());

                if (Files.exists(altPath) && Files.isRegularFile(altPath)) {
                    path = altPath;
                } else {
                    logger.error("Firmware file not found at either path: {} or {}",
                            path.toAbsolutePath(), altPath.toAbsolutePath());
                    return ResponseEntity.notFound().build();
                }
            }

            // Прочитать содержимое файла
            byte[] fileContent = Files.readAllBytes(path);

            // Установить заголовок ответа

            if (firmwarePath.contains(".")) {
                String extension = firmwarePath.substring(firmwarePath.lastIndexOf("."));
                originalFilename += extension;
            }

            // Очистить имя файла, удалить небезопасные символы
            String safeFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");

            logger.info("Providing download for firmware ID: {}, filename: {}, size: {} bytes",
                    id, safeFilename, fileContent.length);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeFilename + "\"")
                    .body(fileContent);
        } catch (IOException e) {
            logger.error("Error reading firmware file for ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Unexpected error during firmware download for ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/upload")
    @Operation(summary = "Загрузить файл прошивки")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<String> uploadFirmware(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new Result<String>().error("Загружаемый файл не может быть пустым");
        }

        // Проверить расширение файла
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return new Result<String>().error("Имя файла не может быть пустым");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!extension.equals(".bin") && !extension.equals(".apk")) {
            return new Result<String>().error("Разрешено загружать только файлы форматов .bin и .apk");
        }

        try {
            // Вычислить MD5-хэш файла
            String md5 = calculateMD5(file);

            // Установить путь хранения
            String uploadDir = "uploadfile";
            Path uploadPath = Paths.get(uploadDir);

            // Если каталог не существует, создать его
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Использовать MD5 в качестве имени файла, использовать расширение .bin
            String uniqueFileName = md5 + extension;
            Path filePath = uploadPath.resolve(uniqueFileName);

            // Проверить, существует ли файл
            if (Files.exists(filePath)) {
                return new Result<String>().ok(filePath.toString());
            }

            // Сохранить файл
            Files.copy(file.getInputStream(), filePath);

            // Вернуть путь к файлу
            return new Result<String>().ok(filePath.toString());
        } catch (IOException e) {
            return new Result<String>().error("Ошибка загрузки файла: " + e.getMessage());
        }
    }

    @PostMapping("/uploadAssetsBin")
    @Operation(summary = "Загрузить файл ресурсов прошивки")
    @RequiresPermissions("sys:role:normal")
    public Result<String> uploadAssetsBin(@RequestParam("file") MultipartFile file) {
        String otaUrl = sysParamsService.getValue(Constant.SERVER_OTA, true);
        if (StringUtils.isBlank(otaUrl) || otaUrl.equals("null")) {
            return new Result<String>().error(ErrorCode.OTA_URL_EMPTY);
        }
        logger.info("username:{},uploadAssetsBin size: {}", SecurityUser.getUser().getUsername(), file.getSize());
        // Проверить размер файла (максимум 20 МБ для ресурсов прошивки)
        if (file.getSize() > 20 * 1024 * 1024) {
            return new Result<String>().error(ErrorCode.VOICE_CLONE_AUDIO_TOO_LARGE);
        }
        // Обычные пользователи могут загружать только 50 раз в день
        if (SecurityUser.getUser().getSuperAdmin() == SuperAdminEnum.NO.value()) {
            String uploadCountKey = RedisKeys.getOtaUploadCountKey(SecurityUser.getUser().getId());
            Integer uploadCount = (Integer) Optional.ofNullable(redisUtils.get(uploadCountKey)).orElse(0);
            if (uploadCount >= 50) {
                return new Result<String>().error(ErrorCode.OTA_UPLOAD_COUNT_EXCEED);
            }
            // Увеличить количество загрузок
            redisUtils.increment(RedisKeys.getOtaUploadCountKey(SecurityUser.getUser().getId()),
                    RedisUtils.DEFAULT_EXPIRE);
        }
        Result<String> result = uploadFirmware(file);

        // Сгенерировать путь к файлу ресурсов
        if (StringUtils.isNotBlank(result.getData())) {
            String uuid = UUID.randomUUID().toString();
            redisUtils.set(RedisKeys.getOtaIdKey(uuid), "file:" + result.getData());
            String downloadUrl = otaUrl.replace("/ota/", "/otaMag/download/") + uuid;
            result.setData(downloadUrl);
        }
        return result;
    }

    private String calculateMD5(MultipartFile file) throws IOException {
        return DigestUtil.md5Hex(file.getBytes());
    }
}
