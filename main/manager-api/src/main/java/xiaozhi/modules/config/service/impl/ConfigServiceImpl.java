package xiaozhi.modules.config.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.AllArgsConstructor;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.redis.RedisKeys;
import xiaozhi.common.redis.RedisUtils;
import xiaozhi.common.utils.ConvertUtils;
import xiaozhi.common.utils.JsonUtils;
import xiaozhi.modules.agent.dao.AgentVoicePrintDao;
import xiaozhi.modules.agent.entity.AgentContextProviderEntity;
import xiaozhi.modules.agent.entity.AgentEntity;
import xiaozhi.modules.agent.entity.AgentPluginMapping;
import xiaozhi.modules.agent.entity.AgentTemplateEntity;
import xiaozhi.modules.agent.entity.AgentVoicePrintEntity;
import xiaozhi.modules.agent.service.AgentContextProviderService;
import xiaozhi.modules.agent.service.AgentMcpAccessPointService;
import xiaozhi.modules.agent.service.AgentPluginMappingService;
import xiaozhi.modules.agent.service.AgentService;
import xiaozhi.modules.agent.service.AgentTemplateService;
import xiaozhi.modules.correctword.service.CorrectWordFileService;
import xiaozhi.modules.agent.vo.AgentVoicePrintVO;
import xiaozhi.modules.correctword.vo.CorrectWordSimpleVO;
import xiaozhi.modules.config.service.ConfigService;
import xiaozhi.modules.device.entity.DeviceEntity;
import xiaozhi.modules.device.service.DeviceService;
import xiaozhi.modules.model.entity.ModelConfigEntity;
import xiaozhi.modules.model.service.ModelConfigService;
import xiaozhi.modules.sys.dto.SysParamsDTO;
import xiaozhi.modules.sys.service.SysParamsService;
import xiaozhi.modules.timbre.service.TimbreService;
import xiaozhi.modules.timbre.vo.TimbreDetailsVO;
import xiaozhi.modules.voiceclone.entity.VoiceCloneEntity;
import xiaozhi.modules.voiceclone.service.VoiceCloneService;

@Service
@AllArgsConstructor
public class ConfigServiceImpl implements ConfigService {
    private final SysParamsService sysParamsService;
    private final DeviceService deviceService;
    private final ModelConfigService modelConfigService;
    private final AgentService agentService;
    private final AgentTemplateService agentTemplateService;
    private final RedisUtils redisUtils;
    private final TimbreService timbreService;
    private final AgentPluginMappingService agentPluginMappingService;
    private final AgentMcpAccessPointService agentMcpAccessPointService;
    private final AgentContextProviderService agentContextProviderService;
    private final VoiceCloneService cloneVoiceService;
    private final AgentVoicePrintDao agentVoicePrintDao;
    private final CorrectWordFileService correctWordFileService;

    @Override
    public Map<String, Object> getConfig(Boolean isCache) {
        if (isCache) {
            // Сначала получить конфигурацию из Redis
            Object cachedConfig = redisUtils.get(RedisKeys.getServerConfigKey());
            if (cachedConfig != null) {
                return JsonUtils.toStringObjectMap(cachedConfig);
            }
        }

        // Построение конфигурации
        Map<String, Object> result = new HashMap<>();
        buildConfig(result);

        // Запрос агента по умолчанию
        AgentTemplateEntity agent = agentTemplateService.getDefaultTemplate();
        if (agent == null) {
            throw new RenException(ErrorCode.AGENT_TEMPLATE_NOT_FOUND);
        }

        // Построение конфигурации модулей
        buildModuleConfig(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                agent.getVadModelId(),
                agent.getAsrModelId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                result,
                isCache);

        // Сохранение конфигурации в Redis
        redisUtils.set(RedisKeys.getServerConfigKey(), result);

        return result;
    }

    @Override
    public Map<String, Object> getAgentModels(String macAddress, Map<String, String> selectedModule) {
        // Проверка, является ли запрос запросом консоли управления
        String redisKey = RedisKeys.getTmpRegisterMacKey(macAddress);
        Object isAdminRequest = redisUtils.get(redisKey);

        if (isAdminRequest != null && "true".equals(isAdminRequest)) {
            // Запрос консоли управления — возврат результата getConfig
            redisUtils.delete(redisKey); // Очистка после использования
            return getConfig(true);
        }
        // Поиск устройства по MAC-адресу
        DeviceEntity device = deviceService.getDeviceByMacAddress(macAddress);
        if (device == null) {
            // Если устройство не найдено — проверить в Redis наличие устройства для подключения
            String cachedCode = deviceService.geCodeByDeviceId(macAddress);
            if (StringUtils.isNotBlank(cachedCode)) {
                throw new RenException(ErrorCode.OTA_DEVICE_NEED_BIND, cachedCode);
            }
            throw new RenException(ErrorCode.OTA_DEVICE_NOT_FOUND);
        }

        // Получение информации об агенте
        AgentEntity agent = agentService.getAgentById(device.getAgentId());
        if (agent == null) {
            throw new RenException(ErrorCode.AGENT_NOT_FOUND);
        }
        // Получение информации о голосе
        String voice = null;
        String referenceAudio = null;
        String referenceText = null;
        String language = null;
        TimbreDetailsVO timbre = timbreService.get(agent.getTtsVoiceId());
        if (timbre != null) {
            voice = timbre.getTtsVoice();
            referenceAudio = timbre.getReferenceAudio();
            referenceText = timbre.getReferenceText();
            // Приоритет — язык, выбранный пользователем; если не задан — первый язык, поддерживаемый голосом
            if (StringUtils.isNotBlank(agent.getTtsLanguage())) {
                language = agent.getTtsLanguage();
            } else if (StringUtils.isNotBlank(timbre.getLanguages())) {
                language = timbre.getLanguages().split("、")[0].trim();
            }
        } else {
            VoiceCloneEntity voice_print = cloneVoiceService.selectById(agent.getTtsVoiceId());
            if (voice_print != null) {
                voice = voice_print.getVoiceId();
                // Приоритет — язык, выбранный пользователем; если не задан — значение по умолчанию
                language = StringUtils.isNotBlank(agent.getTtsLanguage()) ? agent.getTtsLanguage() : "普通话";
            }
        }
        // Построение возвращаемых данных
        Map<String, Object> result = new HashMap<>();
        // Получение максимального количества символов в день на одно устройство
        String deviceMaxOutputSize = sysParamsService.getValue("device_max_output_size", true);
        result.put("device_max_output_size", deviceMaxOutputSize);

        // Получение конфигурации истории чата
        Integer chatHistoryConf = agent.getChatHistoryConf();
        if (agent.getMemModelId() != null && agent.getMemModelId().equals(Constant.MEMORY_NO_MEM)) {
            chatHistoryConf = Constant.ChatHistoryConfEnum.IGNORE.getCode();
        } else if (agent.getMemModelId() != null
                && !agent.getMemModelId().equals(Constant.MEMORY_NO_MEM)
                && agent.getChatHistoryConf() == null) {
            chatHistoryConf = Constant.ChatHistoryConfEnum.RECORD_TEXT_AUDIO.getCode();
        }
        result.put("chat_history_conf", chatHistoryConf);
        // Если клиент уже инстанцировал модель — не возвращать
        String alreadySelectedVadModelId = selectedModule.get("VAD");
        if (alreadySelectedVadModelId != null && alreadySelectedVadModelId.equals(agent.getVadModelId())) {
            agent.setVadModelId(null);
        }
        String alreadySelectedAsrModelId = selectedModule.get("ASR");
        if (alreadySelectedAsrModelId != null && alreadySelectedAsrModelId.equals(agent.getAsrModelId())) {
            agent.setAsrModelId(null);
        }

        // Добавление информации о параметрах вызова функций
        if (!Objects.equals(agent.getIntentModelId(), "Intent_nointent")) {
            String agentId = agent.getId();
            List<AgentPluginMapping> pluginMappings = agentPluginMappingService.agentPluginParamsByAgentId(agentId);
            if (pluginMappings != null && !pluginMappings.isEmpty()) {
                Map<String, Object> pluginParams = new HashMap<>();
                for (AgentPluginMapping pluginMapping : pluginMappings) {
                    pluginParams.put(pluginMapping.getProviderCode(), pluginMapping.getParamInfo());
                }
                result.put("plugins", pluginParams);
            }
        }
        // Получение адреса MCP-точки доступа
        String mcpEndpoint = agentMcpAccessPointService.getAgentMcpAccessAddress(agent.getId());
        if (StringUtils.isNotBlank(mcpEndpoint) && mcpEndpoint.startsWith("ws")) {
            mcpEndpoint = mcpEndpoint.replace("/mcp/", "/call/");
            result.put("mcp_endpoint", mcpEndpoint);
        }

        // Получение конфигурации источников контекста
        AgentContextProviderEntity contextProviderEntity = agentContextProviderService.getByAgentId(agent.getId());
        if (contextProviderEntity != null && contextProviderEntity.getContextProviders() != null
                && !contextProviderEntity.getContextProviders().isEmpty()) {
            result.put("context_providers", contextProviderEntity.getContextProviders());
        }

        // Получение информации о голосовом отпечатке
        buildVoiceprintConfig(agent.getId(), result);

        // Построение конфигурации модулей
        buildModuleConfig(
                agent.getAgentName(),
                agent.getSystemPrompt(),
                agent.getSummaryMemory(),
                voice,
                referenceAudio,
                referenceText,
                language,
                agent.getTtsVolume(),
                agent.getTtsRate(),
                agent.getTtsPitch(),
                agent.getVadModelId(),
                agent.getAsrModelId(),
                agent.getLlmModelId(),
                agent.getVllmModelId(),
                agent.getSlmModelId(),
                agent.getTtsModelId(),
                agent.getMemModelId(),
                agent.getIntentModelId(),
                null,
                result,
                true);

        return result;
    }

    @Override
    public List<String> getCorrectWords(String macAddress) {
        DeviceEntity device = deviceService.getDeviceByMacAddress(macAddress);
        if (device == null) {
            return Collections.emptyList();
        }
        List<CorrectWordSimpleVO> items = correctWordFileService.getAllItemsByAgentId(device.getAgentId());
        return items.stream()
                .map(item -> item.getSourceWord() + "|" + item.getTargetWord())
                .collect(Collectors.toList());
    }

    /**
     * Построение конфигурации
     * 
     * @param config список системных параметров
     * @return информация конфигурации
     */
    private Object buildConfig(Map<String, Object> config) {

        // Запрос всех системных параметров
        List<SysParamsDTO> paramsList = sysParamsService.list(new HashMap<>());

        for (SysParamsDTO param : paramsList) {
            String[] keys = param.getParamCode().split("\\.");
            Map<String, Object> current = config;

            // Обход всех ключей, кроме последнего
            for (int i = 0; i < keys.length - 1; i++) {
                String key = keys[i];
                Object nestedConfig = current.computeIfAbsent(key, ignored -> new HashMap<String, Object>());
                Map<String, Object> nestedMap = JsonUtils.toStringObjectMap(nestedConfig);
                current.put(key, nestedMap);
                current = nestedMap;
            }

            // Обработка последнего ключа
            String lastKey = keys[keys.length - 1];
            String value = param.getParamValue();

            // Преобразование значения в зависимости от valueType
            switch (param.getValueType().toLowerCase()) {
                case "number":
                    try {
                        double doubleValue = Double.parseDouble(value);
                        // Если числовое значение является целым — преобразовать в Integer
                        if (doubleValue == (int) doubleValue) {
                            current.put(lastKey, (int) doubleValue);
                        } else {
                            current.put(lastKey, doubleValue);
                        }
                    } catch (NumberFormatException e) {
                        current.put(lastKey, value);
                    }
                    break;
                case "boolean":
                    current.put(lastKey, Boolean.parseBoolean(value));
                    break;
                case "array":
                    // Преобразование строки, разделённой точкой с запятой, в массив
                    List<String> list = new ArrayList<>();
                    for (String num : value.split(";")) {
                        if (StringUtils.isNotBlank(num)) {
                            list.add(num.trim());
                        }
                    }
                    current.put(lastKey, list);
                    break;
                case "json":
                    try {
                        current.put(lastKey, JsonUtils.parseObject(value, Object.class));
                    } catch (Exception e) {
                        current.put(lastKey, value);
                    }
                    break;
                default:
                    current.put(lastKey, value);
            }
        }

        return config;
    }

    /**
     * Построение конфигурации голосового отпечатка
     * 
     * @param agentId ID агента
     * @param result  результирующий Map
     */
    private void buildVoiceprintConfig(String agentId, Map<String, Object> result) {
        try {
            // Получение адреса интерфейса голосового отпечатка
            String voiceprintUrl = sysParamsService.getValue(Constant.SERVER_VOICE_PRINT, true);
            if (StringUtils.isBlank(voiceprintUrl) || "null".equals(voiceprintUrl)) {
                return;
            }

            // Получение информации о голосовых отпечатках агента (без проверки прав пользователя)
            List<AgentVoicePrintVO> voiceprints = getVoiceprintsByAgentId(agentId);
            if (voiceprints == null || voiceprints.isEmpty()) {
                return;
            }

            // Построение списка speakers
            List<String> speakers = new ArrayList<>();
            for (AgentVoicePrintVO voiceprint : voiceprints) {
                String speakerStr = String.format("%s,%s,%s",
                        voiceprint.getId(),
                        voiceprint.getSourceName(),
                        voiceprint.getIntroduce() != null ? voiceprint.getIntroduce() : "");
                speakers.add(speakerStr);
            }

            // Построение конфигурации голосового отпечатка
            Map<String, Object> voiceprintConfig = new HashMap<>();
            voiceprintConfig.put("url", voiceprintUrl);
            voiceprintConfig.put("speakers", speakers);

            // Получение порога схожести распознавания голосового отпечатка, по умолчанию 0.4
            String thresholdStr = sysParamsService.getValue("server.voiceprint_similarity_threshold", true);
            if (StringUtils.isNotBlank(thresholdStr) && !"null".equals(thresholdStr)) {
                try {
                    double threshold = Double.parseDouble(thresholdStr);
                    voiceprintConfig.put("similarity_threshold", threshold);
                } catch (NumberFormatException e) {
                    // При ошибке разбора использовать значение по умолчанию 0.4
                    voiceprintConfig.put("similarity_threshold", 0.4);
                }
            } else {
                voiceprintConfig.put("similarity_threshold", 0.4);
            }

            result.put("voiceprint", voiceprintConfig);
        } catch (Exception e) {
            // Ошибка получения конфигурации голосового отпечатка не влияет на другие функции
            System.err.println("Ошибка получения конфигурации голосового отпечатка: " + e.getMessage());
        }
    }

    /**
     * Получение информации о голосовых отпечатках агента
     * 
     * @param agentId ID агента
     * @return список информации о голосовых отпечатках
     */
    private List<AgentVoicePrintVO> getVoiceprintsByAgentId(String agentId) {
        LambdaQueryWrapper<AgentVoicePrintEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AgentVoicePrintEntity::getAgentId, agentId);
        queryWrapper.orderByAsc(AgentVoicePrintEntity::getCreateDate);
        List<AgentVoicePrintEntity> entities = agentVoicePrintDao.selectList(queryWrapper);
        return ConvertUtils.sourceToTarget(entities, AgentVoicePrintVO.class);
    }

    /**
     * Построение конфигурации модулей
     * 
     * @param prompt         подсказка
     * @param voice          голос
     * @param referenceAudio путь к эталонному аудио
     * @param referenceText  эталонный текст
     * @param vadModelId     ID модели VAD
     * @param asrModelId     ID модели ASR
     * @param llmModelId     ID модели LLM
     * @param ttsModelId     ID модели TTS
     * @param memModelId     ID модели памяти
     * @param intentModelId  ID модели намерений
     * @param result         результирующий Map
     */
    private void buildModuleConfig(
            String assistantName,
            String prompt,
            String summaryMemory,
            String voice,
            String referenceAudio,
            String referenceText,
            String language,
            Integer ttsVolume,
            Integer ttsRate,
            Integer ttsPitch,
            String vadModelId,
            String asrModelId,
            String llmModelId,
            String vllmModelId,
            String slmModelId,
            String ttsModelId,
            String memModelId,
            String intentModelId,
            String ragModelId,
            Map<String, Object> result,
            boolean isCache) {
        Map<String, String> selectedModule = new HashMap<>();

        String[] modelTypes = { "VAD", "ASR", "TTS", "Memory", "Intent", "LLM", "VLLM", "SLM", "RAG" };
        String[] modelIds = { vadModelId, asrModelId, ttsModelId, memModelId, intentModelId, llmModelId, vllmModelId,
                slmModelId, ragModelId };
        String intentLLMModelId = null;
        String memLocalShortLLMModelId = null;

        for (int i = 0; i < modelIds.length; i++) {
            if (modelIds[i] == null) {
                continue;
            }
            // Ключевой момент: третий параметр — false, чтобы получить исходный ключ
            ModelConfigEntity model = modelConfigService.getModelByIdFromCache(modelIds[i]);
            if (model == null) {
                continue;
            }
            Map<String, Object> typeConfig = new HashMap<>();
            if (model.getConfigJson() != null) {
                typeConfig.put(model.getId(), model.getConfigJson());
                // Если тип TTS — добавить свойство private_voice
                if ("TTS".equals(modelTypes[i])) {
                    if (voice != null)
                        ((Map<String, Object>) model.getConfigJson()).put("private_voice", voice);
                    if (referenceAudio != null)
                        ((Map<String, Object>) model.getConfigJson()).put("ref_audio", referenceAudio);
                    if (referenceText != null)
                        ((Map<String, Object>) model.getConfigJson()).put("ref_text", referenceText);
                    if (language != null)
                        ((Map<String, Object>) model.getConfigJson()).put("language", language);
                    if (ttsVolume != null)
                        ((Map<String, Object>) model.getConfigJson()).put("ttsVolume", ttsVolume);
                    if (ttsRate != null)
                        ((Map<String, Object>) model.getConfigJson()).put("ttsRate", ttsRate);
                    if (ttsPitch != null)
                        ((Map<String, Object>) model.getConfigJson()).put("ttsPitch", ttsPitch);

                    // Для клонирования голоса Volcengine необходимо заменить resource_id
                    Map<String, Object> map = (Map<String, Object>) model.getConfigJson();
                    if (Constant.VOICE_CLONE_HUOSHAN_DOUBLE_STREAM.equals(map.get("type"))) {
                        // Если voice начинается с "S_" — использовать seed-icl-1.0
                        if (voice != null && voice.startsWith("S_")) {
                            map.put("resource_id", "seed-icl-1.0");
                        }
                    }
                }
                // Если тип Intent и type=intent_llm — добавить дополнительную модель
                if ("Intent".equals(modelTypes[i])) {
                    Map<String, Object> map = (Map<String, Object>) model.getConfigJson();
                    if ("intent_llm".equals(map.get("type"))) {
                        intentLLMModelId = (String) map.get("llm");
                        if (StringUtils.isNotBlank(intentLLMModelId) && intentLLMModelId.equals(llmModelId)) {
                            intentLLMModelId = null;
                        }
                    }
                    if (map.get("functions") != null) {
                        String functionStr = (String) map.get("functions");
                        if (StringUtils.isNotBlank(functionStr)) {
                            String[] functions = functionStr.split(";");
                            map.put("functions", functions);
                        }
                    }
                    System.out.println("map: " + map);
                }
                if ("Memory".equals(modelTypes[i])) {
                    Map<String, Object> map = (Map<String, Object>) model.getConfigJson();
                    if ("mem_local_short".equals(map.get("type"))) {
                        memLocalShortLLMModelId = (String) map.get("llm");
                        if (StringUtils.isNotBlank(memLocalShortLLMModelId)
                                && memLocalShortLLMModelId.equals(llmModelId)) {
                            memLocalShortLLMModelId = null;
                        }
                    }
                }
                // Если тип LLM и intentLLMModelId не пуст — добавить дополнительную модель
                if ("LLM".equals(modelTypes[i])) {
                    if (StringUtils.isNotBlank(intentLLMModelId)) {
                        if (!typeConfig.containsKey(intentLLMModelId)) {
                            // Изменить здесь: добавить параметр isMaskSensitive=false
                            ModelConfigEntity intentLLM = modelConfigService.getModelByIdFromCache(intentLLMModelId);
                            typeConfig.put(intentLLM.getId(), intentLLM.getConfigJson());
                        }
                    }
                    if (StringUtils.isNotBlank(memLocalShortLLMModelId)) {
                        if (!typeConfig.containsKey(memLocalShortLLMModelId)) {
                            // Изменить здесь: добавить параметр isMaskSensitive=false
                            ModelConfigEntity memLocalShortLLM = modelConfigService
                                    .getModelByIdFromCache(memLocalShortLLMModelId);
                            typeConfig.put(memLocalShortLLM.getId(), memLocalShortLLM.getConfigJson());
                        }
                    }
                    // LLM также возвращает выбранный SLM; если ID совпадают — не дублировать
                    if (StringUtils.isNotBlank(slmModelId) && !slmModelId.equals(llmModelId)) {
                        if (!typeConfig.containsKey(slmModelId)) {
                            ModelConfigEntity slmModel = modelConfigService.getModelByIdFromCache(slmModelId);
                            if (slmModel != null && slmModel.getConfigJson() != null) {
                                typeConfig.put(slmModel.getId(), slmModel.getConfigJson());
                            }
                        }
                    }
                }
            }
            result.put(modelTypes[i], typeConfig);

            selectedModule.put(modelTypes[i], model.getId());
        }

        result.put("selected_module", selectedModule);
        if (StringUtils.isNotBlank(prompt)) {
            prompt = prompt.replace("{{assistant_name}}", StringUtils.isBlank(assistantName) ? "小智" : assistantName);
        }
        result.put("prompt", prompt);
        result.put("summaryMemory", summaryMemory);
    }
}
