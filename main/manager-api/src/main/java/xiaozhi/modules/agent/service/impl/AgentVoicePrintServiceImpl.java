package xiaozhi.modules.agent.service.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;

import lombok.extern.slf4j.Slf4j;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.utils.ConvertUtils;
import xiaozhi.common.utils.JsonUtils;
import xiaozhi.modules.agent.dao.AgentVoicePrintDao;
import xiaozhi.modules.agent.dto.AgentVoicePrintSaveDTO;
import xiaozhi.modules.agent.dto.AgentVoicePrintUpdateDTO;
import xiaozhi.modules.agent.dto.IdentifyVoicePrintResponse;
import xiaozhi.modules.agent.entity.AgentVoicePrintEntity;
import xiaozhi.modules.agent.service.AgentChatAudioService;
import xiaozhi.modules.agent.service.AgentChatHistoryService;
import xiaozhi.modules.agent.service.AgentVoicePrintService;
import xiaozhi.modules.agent.vo.AgentVoicePrintVO;
import xiaozhi.modules.sys.service.SysParamsService;


/**
 * @author zjy
 */

@Service
@Slf4j
public class AgentVoicePrintServiceImpl extends CrudRepository<AgentVoicePrintDao, AgentVoicePrintEntity>
        implements AgentVoicePrintService {
    private final AgentChatAudioService agentChatAudioService;
    private final RestTemplate restTemplate;
    private final SysParamsService sysParamsService;
    private final AgentChatHistoryService agentChatHistoryService;
    // Программный класс транзакций Spring Boot
    private final TransactionTemplate transactionTemplate;
    // Порог распознавания
    private final Double RECOGNITION = 0.5;
    private final Executor taskExecutor;

    public AgentVoicePrintServiceImpl(AgentChatAudioService agentChatAudioService, RestTemplate restTemplate,
                                      SysParamsService sysParamsService, AgentChatHistoryService agentChatHistoryService,
                                      TransactionTemplate transactionTemplate, @Qualifier("taskExecutor") Executor taskExecutor) {
        this.agentChatAudioService = agentChatAudioService;
        this.restTemplate = restTemplate;
        this.sysParamsService = sysParamsService;
        this.agentChatHistoryService = agentChatHistoryService;
        this.transactionTemplate = transactionTemplate;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public boolean insert(AgentVoicePrintSaveDTO dto) {
        // Получить аудиоданные
        ByteArrayResource resource = getVoicePrintAudioWAV(dto.getAgentId(), dto.getAudioId());
        // Проверить, был ли этот голос зарегистрирован
        IdentifyVoicePrintResponse response = identifyVoicePrint(dto.getAgentId(), resource);
        if (response != null && response.getScore() > RECOGNITION) {
            // Запросить информацию пользователя по распознанному ID голосового отпечатка
            AgentVoicePrintEntity existingVoicePrint = baseMapper.selectById(response.getSpeakerId());
            String existingUserName = existingVoicePrint != null ? existingVoicePrint.getSourceName() : "未知用户";
            throw new RenException(ErrorCode.VOICEPRINT_ALREADY_REGISTERED, existingUserName);
        }
        AgentVoicePrintEntity entity = ConvertUtils.sourceToTarget(dto, AgentVoicePrintEntity.class);
        // Начать транзакцию
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                // Сохранить информацию голосового отпечатка
                int row = baseMapper.insert(entity);
                // Если после вставки затронуто не ровно 1 строку — ошибка сохранения, откат
                if (row != 1) {
                    status.setRollbackOnly(); // Пометить откат транзакции
                    return false;
                }
                // Отправить запрос на регистрацию голосового отпечатка
                registerVoicePrint(entity.getId(), resource);
                return true;
            } catch (RenException e) {
                status.setRollbackOnly(); // Пометить откат транзакции
                throw e;
            } catch (Exception e) {
                status.setRollbackOnly(); // Пометить откат транзакции
                log.error("保存声纹错误原因：{}", e.getMessage());
                throw new RenException(ErrorCode.VOICE_PRINT_SAVE_ERROR);
            }
        }));
    }

    @Override
    public boolean delete(Long userId, String voicePrintId) {
        // Начать транзакцию
        boolean b = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                // Удалить голосовой отпечаток для текущего пользователя и агента
                int row = baseMapper.delete(new LambdaQueryWrapper<AgentVoicePrintEntity>()
                        .eq(AgentVoicePrintEntity::getId, voicePrintId)
                        .eq(AgentVoicePrintEntity::getCreator, userId));
                if (row != 1) {
                    status.setRollbackOnly(); // Пометить откат транзакции
                    return false;
                }

                return true;
            } catch (Exception e) {
                status.setRollbackOnly(); // Пометить откат транзакции
                log.error("删除声纹存在错误原因：{}", e.getMessage());
                throw new RenException(ErrorCode.VOICEPRINT_DELETE_ERROR);
            }
        }));
        // Удаление из сервиса выполняется только после успешного удаления из БД
        if(b){
            taskExecutor.execute(()-> {
                try {
                    cancelVoicePrint(voicePrintId);
                }catch (RuntimeException e) {
                    log.error("删除声纹存在运行时错误原因：{}，id：{}", e.getMessage(),voicePrintId);
                }
            });
        }
        return b;
    }

    @Override
    public List<AgentVoicePrintVO> list(Long userId, String agentId) {
        // Поиск данных для текущего пользователя и агента
        List<AgentVoicePrintEntity> list = baseMapper.selectList(new LambdaQueryWrapper<AgentVoicePrintEntity>()
                .eq(AgentVoicePrintEntity::getAgentId, agentId)
                .eq(AgentVoicePrintEntity::getCreator, userId));
        return list.stream().map(entity -> {
            // Преобразовать в тип AgentVoicePrintVO
            return ConvertUtils.sourceToTarget(entity, AgentVoicePrintVO.class);
        }).toList();

    }

    @Override
    public boolean update(Long userId, AgentVoicePrintUpdateDTO dto) {
        AgentVoicePrintEntity agentVoicePrintEntity = baseMapper
                .selectOne(new LambdaQueryWrapper<AgentVoicePrintEntity>()
                        .eq(AgentVoicePrintEntity::getId, dto.getId())
                        .eq(AgentVoicePrintEntity::getCreator, userId));
        if (agentVoicePrintEntity == null) {
            return false;
        }
        // Получить ID аудио
        String audioId = dto.getAudioId();
        // Получить ID агента
        String agentId = agentVoicePrintEntity.getAgentId();
        ByteArrayResource resource;
        // audioId не пуст и отличается от ранее сохраненногоID аудио, необходимо зановоПолучить аудиоданныедля генерации голосового отпечатка
        if (!StringUtils.isEmpty(audioId) && !audioId.equals(agentVoicePrintEntity.getAudioId())) {
            resource = getVoicePrintAudioWAV(agentId, audioId);

            // Проверить, был ли этот голос зарегистрирован
            IdentifyVoicePrintResponse response = identifyVoicePrint(agentId, resource);
            // Если возвращаемая оценка выше RECOGNITION, голосовой отпечаток уже существует
            if (response != null && response.getScore() > RECOGNITION) {
                // Если возвращаемый id не совпадает с изменяемымID голосового отпечатка, это означает что данныйID голосового отпечатка - голос уже зарегистрирован и не является оригинальным, изменение запрещено
                if (!response.getSpeakerId().equals(dto.getId())) {
                    // Запросить информацию пользователя по распознанному ID голосового отпечатка
                    AgentVoicePrintEntity existingVoicePrint = baseMapper.selectById(response.getSpeakerId());
                    String existingUserName = existingVoicePrint != null ? existingVoicePrint.getSourceName() : "未知用户";
                    throw new RenException(ErrorCode.VOICEPRINT_UPDATE_NOT_ALLOWED, existingUserName);
                }
            }
        } else {
            resource = null;
        }
        // Начать транзакцию
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                AgentVoicePrintEntity entity = ConvertUtils.sourceToTarget(dto, AgentVoicePrintEntity.class);
                int row = baseMapper.updateById(entity);
                if (row != 1) {
                    status.setRollbackOnly(); // Пометить откат транзакции
                    return false;
                }
                if (resource != null) {
                    String id = entity.getId();
                    // Сначала отменить регистрацию вектора голосового отпечатка для данного ID
                    cancelVoicePrint(id);
                    // Отправить запрос на регистрацию голосового отпечатка
                    registerVoicePrint(id, resource);
                }
                return true;
            } catch (RenException e) {
                status.setRollbackOnly(); // Пометить откат транзакции
                throw e;
            } catch (Exception e) {
                status.setRollbackOnly(); // Пометить откат транзакции
                log.error("修改声纹错误原因：{}", e.getMessage());
                throw new RenException(ErrorCode.VOICEPRINT_UPDATE_ADMIN_ERROR);
            }
        }));
    }

    
/**
     * Создать URI-объект интерфейса голосовых отпечатков
     *
     * @return Объект URI
     */

    private URI getVoicePrintURI() {
        // Получить адрес интерфейса голосовых отпечатков
        String voicePrint = sysParamsService.getValue(Constant.SERVER_VOICE_PRINT, true);
        try {
            return new URI(voicePrint);
        } catch (URISyntaxException e) {
            log.error("路径格式不正确路径：{}，\n错误信息:{}", voicePrint, e.getMessage());
                throw new RenException(ErrorCode.VOICEPRINT_API_URI_ERROR);
        }
    }

    
/**
     * Получить базовый путь адреса голосовых отпечатков
     * 
     * @param uri URI адреса голосового отпечатка
     * @return БазовоеПуть
     */

    private String getBaseUrl(URI uri) {
        String protocol = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();
        if (port == -1) {
            return "%s://%s".formatted(protocol, host);
        } else {
            return "%s://%s:%s".formatted(protocol, host, port);
        }
    }

    
/**
     * Получить токен авторизации
     *
     * @param uri URI адреса голосового отпечатка
     * @return Значение Authorization
     */

    private String getAuthorization(URI uri) {
        // Получить параметры
        String query = uri.getQuery();
        // Получить ключ шифрования aes
        String str = "key=";
        return "Bearer " + query.substring(query.indexOf(str) + str.length());
    }

    
/**
     * Получить ресурсные данные аудио голосового отпечатка
     *
     * @param audioId ID аудио
     * @return Данные аудиоресурса голосового отпечатка
     */

    private ByteArrayResource getVoicePrintAudioWAV(String agentId, String audioId) {
        // Определить, принадлежит ли аудио текущему агенту
        boolean b = agentChatHistoryService.isAudioOwnedByAgent(audioId, agentId);
        if (!b) {
            throw new RenException(ErrorCode.VOICEPRINT_AUDIO_NOT_BELONG_AGENT);
        }
        // Получить аудиоданные
        byte[] audio = agentChatAudioService.getAudio(audioId);
        // Если аудиоданные пусты — выдать ошибку и прекратить выполнение
        if (audio == null || audio.length == 0) {
            throw new RenException(ErrorCode.VOICEPRINT_AUDIO_EMPTY);
        }
        // Обернуть массив байтов в ресурс и вернуть
        return new ByteArrayResource(audio) {
            @Override
            public String getFilename() {
                return "VoicePrint.WAV"; // Установить имя файла
            }
        };
    }

    
/**
     * Отправить HTTP-запрос на регистрацию голосового отпечатка
     * 
     * @param id       ID голосового отпечатка
     * @param resource Аудиоресурс голосового отпечатка
     */

    private void registerVoicePrint(String id, ByteArrayResource resource) {
        // Обработать адрес интерфейса, получить префикс
        URI uri = getVoicePrintURI();
        String baseUrl = getBaseUrl(uri);
        String requestUrl = baseUrl + "/voiceprint/register";
        // Создать тело запроса
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("speaker_id", id);
        body.add("file", resource);

        // Создать заголовки запроса
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getAuthorization(uri));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        // Создать тело запроса
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        // Отправить POST запрос
        ResponseEntity<String> response = restTemplate.postForEntity(requestUrl, requestEntity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("声纹注册失败,请求路径：{}", requestUrl);
            throw new RenException(ErrorCode.VOICEPRINT_REGISTER_REQUEST_ERROR);
        }
        // Проверить содержимое ответа
        String responseBody = response.getBody();
        if (responseBody == null || !responseBody.contains("true")) {
            log.error("声纹注册失败,请求处理失败内容：{}", responseBody == null ? "空内容" : responseBody);
            throw new RenException(ErrorCode.VOICEPRINT_REGISTER_PROCESS_ERROR);
        }
    }

    
/**
     * Отправить запрос на отмену регистрации голосового отпечатка
     * 
     * @param voicePrintId ID голосового отпечатка
     */

    private void cancelVoicePrint(String voicePrintId) {
        URI uri = getVoicePrintURI();
        String baseUrl = getBaseUrl(uri);
        String requestUrl = baseUrl + "/voiceprint/" + voicePrintId;
        // Создать заголовки запроса
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getAuthorization(uri));
        // Создать тело запроса
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);

        // Отправить POST запрос
        ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.DELETE, requestEntity,
                String.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("声纹注销失败,请求路径：{}", requestUrl);
            throw new RenException(ErrorCode.VOICEPRINT_UNREGISTER_REQUEST_ERROR);
        }
        // Проверить содержимое ответа
        String responseBody = response.getBody();
        if (responseBody == null || !responseBody.contains("true")) {
            log.error("声纹注销失败,请求处理失败内容：{}", responseBody == null ? "空内容" : responseBody);
            throw new RenException(ErrorCode.VOICEPRINT_UNREGISTER_PROCESS_ERROR);
        }
    }

    
/**
     * Отправить HTTP-запрос на распознавание голосового отпечатка
     * 
     * @param agentId  ID агента
     * @param resource Аудиоресурс голосового отпечатка
     * @return Возвращает данные распознавания
     */

    private IdentifyVoicePrintResponse identifyVoicePrint(String agentId, ByteArrayResource resource) {

        // Получить все зарегистрированные голосовые отпечатки агента
        List<AgentVoicePrintEntity> agentVoicePrintList = baseMapper
                .selectList(new LambdaQueryWrapper<AgentVoicePrintEntity>()
                        .select(AgentVoicePrintEntity::getId)
                        .eq(AgentVoicePrintEntity::getAgentId, agentId));

        // Если количество голосовых отпечатков равно 0 — запрос распознавания не нужен
        if (agentVoicePrintList.isEmpty()) {
            return null;
        }
        // Обработать адрес интерфейса, получить префикс
        URI uri = getVoicePrintURI();
        String baseUrl = getBaseUrl(uri);
        String requestUrl = baseUrl + "/voiceprint/identify";
        // Создать тело запроса
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // Создать параметр speaker_id
        String speakerIds = agentVoicePrintList.stream()
                .map(AgentVoicePrintEntity::getId)
                .collect(Collectors.joining(","));
        body.add("speaker_ids", speakerIds);
        body.add("file", resource);

        // Создать заголовки запроса
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getAuthorization(uri));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        // Создать тело запроса
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        // Отправить POST запрос
        ResponseEntity<String> response = restTemplate.postForEntity(requestUrl, requestEntity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("声纹识别请求失败,请求路径：{}", requestUrl);
            throw new RenException(ErrorCode.VOICEPRINT_IDENTIFY_REQUEST_ERROR);
        }
        // Проверить содержимое ответа
        String responseBody = response.getBody();
        if (responseBody != null) {
            return JsonUtils.parseObject(responseBody, IdentifyVoicePrintResponse.class);
        }
        return null;
    }
}
