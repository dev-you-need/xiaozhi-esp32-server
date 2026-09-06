package xiaozhi.modules.sys.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import lombok.AllArgsConstructor;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.page.PageData;
import xiaozhi.common.service.impl.BaseServiceImpl;
import xiaozhi.common.utils.ConvertUtils;
import xiaozhi.common.utils.JsonUtils;
import xiaozhi.common.utils.SM2Utils;
import xiaozhi.modules.agent.service.AgentPluginMappingService;
import xiaozhi.modules.sys.dao.SysParamsDao;
import xiaozhi.modules.sys.dto.SysParamsDTO;
import xiaozhi.modules.sys.entity.SysParamsEntity;
import xiaozhi.modules.sys.redis.SysParamsRedis;
import xiaozhi.modules.sys.service.SysParamsService;

/**
 * Управление параметрами */
@AllArgsConstructor
@Service
public class SysParamsServiceImpl extends BaseServiceImpl<SysParamsDao, SysParamsEntity> implements SysParamsService {
    private final SysParamsRedis sysParamsRedis;
    private final AgentPluginMappingService agentPluginMappingService;

    @Override
    public PageData<SysParamsDTO> page(Map<String, Object> params) {
        IPage<SysParamsEntity> page = baseDao.selectPage(
                getPage(params, null, false),
                getWrapper(params));

        return getPageData(page, SysParamsDTO.class);
    }

    @Override
    public List<SysParamsDTO> list(Map<String, Object> params) {
        List<SysParamsEntity> entityList = baseDao.selectList(getWrapper(params));

        return ConvertUtils.sourceToTarget(entityList, SysParamsDTO.class);
    }

    private QueryWrapper<SysParamsEntity> getWrapper(Map<String, Object> params) {
        String paramCode = (String) params.get("paramCode");

        QueryWrapper<SysParamsEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("param_type", 1);
        wrapper.nested(StringUtils.isNotBlank(paramCode), i -> i.like("param_code", paramCode)
                .or()
                .like("remark", paramCode));

        return wrapper;
    }

    @Override
    public SysParamsDTO get(Long id) {
        SysParamsEntity entity = baseDao.selectById(id);

        return ConvertUtils.sourceToTarget(entity, SysParamsDTO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(SysParamsDTO dto) {
        validateParamValue(dto);

        SysParamsEntity entity = ConvertUtils.sourceToTarget(dto, SysParamsEntity.class);
        insert(entity);

        sysParamsRedis.set(entity.getParamCode(), entity.getParamValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysParamsDTO dto) {
        validateParamValue(dto);
        detectingSMSParameters(dto.getParamCode(), dto.getParamValue());
        SysParamsEntity entity = ConvertUtils.sourceToTarget(dto, SysParamsEntity.class);
        updateById(entity);

        sysParamsRedis.set(entity.getParamCode(), entity.getParamValue());
    }

    /**
     * Проверить тип значения параметра     */
    private void validateParamValue(SysParamsDTO dto) {
        if (dto == null) {
            throw new RenException(ErrorCode.PARAM_VALUE_NULL);
        }

        if (StringUtils.isBlank(dto.getParamValue())) {
            throw new RenException(ErrorCode.PARAM_VALUE_NULL);
        }

        if (StringUtils.isBlank(dto.getValueType())) {
            throw new RenException(ErrorCode.PARAM_TYPE_NULL);
        }

        String valueType = dto.getValueType().toLowerCase();
        String paramValue = dto.getParamValue();

        switch (valueType) {
            case "string":
                break;
            case "array":
                break;
            case "number":
                try {
                    Double.parseDouble(paramValue);
                } catch (NumberFormatException e) {
                    throw new RenException(ErrorCode.PARAM_NUMBER_INVALID);
                }
                break;
            case "boolean":
                if (!"true".equalsIgnoreCase(paramValue) && !"false".equalsIgnoreCase(paramValue)) {
                    throw new RenException(ErrorCode.PARAM_BOOLEAN_INVALID);
                }
                break;
            case "json":
                try {
                    //Начните с проверки, начинается ли оно с {and ends with}
                    String trimmedValue = paramValue.trim();
                    if (!trimmedValue.startsWith("{") || !trimmedValue.endsWith("}")) {
                        throw new RenException(ErrorCode.PARAM_JSON_INVALID);
                    }
                    //Затем попробуйте разобрать JSON
                    JsonUtils.parseObject(paramValue, Object.class);
                } catch (Exception e) {
                    throw new RenException(ErrorCode.PARAM_JSON_INVALID);
                }
                break;
            default:
                throw new RenException(ErrorCode.PARAM_TYPE_INVALID);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String[] ids) {
        //Удалить данные Redis
        List<String> paramCodeList = baseDao.getParamCodeList(ids);
        String[] paramCodes = paramCodeList.toArray(new String[paramCodeList.size()]);
        if (paramCodes.length > 0) {
            sysParamsRedis.delete(paramCodes);
        }

        //shops|удалить
        deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public String getValue(String paramCode, Boolean fromCache) {
        String paramValue = null;
        if (fromCache) {
            paramValue = sysParamsRedis.get(paramCode);
            if (paramValue == null) {
                paramValue = baseDao.getValueByCode(paramCode);

                sysParamsRedis.set(paramCode, paramValue);
            }
        } else {
            paramValue = baseDao.getValueByCode(paramCode);
        }
        return paramValue;
    }

    @Override
    public <T> T getValueObject(String paramCode, Class<T> clazz) {
        String paramValue = getValue(paramCode, true);
        if (StringUtils.isNotBlank(paramValue)) {
            return JsonUtils.parseObject(paramValue, clazz);
        }

        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RenException(ErrorCode.PARAMS_GET_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateValueByCode(String paramCode, String paramValue) {
        int count = baseDao.updateValueByCode(paramCode, paramValue);
        sysParamsRedis.set(paramCode, paramValue);
        return count;
    }

    @Override
    public void initServerSecret() {
        //Получить ключ сервера
        String secretParam = getValue(Constant.SERVER_SECRET, false);
        if (StringUtils.isBlank(secretParam) || "null".equals(secretParam)) {
            String newSecret = UUID.randomUUID().toString();
            updateValueByCode(Constant.SERVER_SECRET, newSecret);
        }

        //Инициализация пары ключей SM2
        initSM2KeyPair();
    }

    /**
     * Инициализация пары ключей SM2     */
    private void initSM2KeyPair() {
        //Получение открытого ключа SM2
        String publicKey = getValue(Constant.SM2_PUBLIC_KEY, false);
        //Получить закрытый ключ SM2
        String privateKey = getValue(Constant.SM2_PRIVATE_KEY, false);

        //Создать новую пару ключей, если открытый или закрытый ключ пуст
        if (StringUtils.isBlank(publicKey) || StringUtils.isBlank(privateKey) || 
            "null".equals(publicKey) || "null".equals(privateKey)) {
            Map<String, String> keyPair = SM2Utils.createKey();
            String newPublicKey = keyPair.get(SM2Utils.KEY_PUBLIC_KEY);
            String newPrivateKey = keyPair.get(SM2Utils.KEY_PRIVATE_KEY);

            //Обновить пары ключей в базе данных
            updateValueByCode(Constant.SM2_PUBLIC_KEY, newPublicKey);
            updateValueByCode(Constant.SM2_PRIVATE_KEY, newPrivateKey);
        }
    }

    /**
     * Проверьте, соответствуют ли параметры SMS требованиям     * 
     * Кодировка параметра @ param paramCode     * Значение параметра @ param paramValue     * @ return pass or not     */
    private boolean detectingSMSParameters(String paramCode, String paramValue) {
        //Определите, является ли это кодировкой параметра, которая открывает регистрацию мобильного телефона. Если это не кодировка параметра, и нет необходимости обнаруживать другие параметры SMS, верните true напрямую
        if (!Constant.SysMSMParam.SERVER_ENABLE_MOBILE_REGISTER.getValue().equals(paramCode)) {
            return true;
        }
        //Определите, выключен ли он, выключена ли регистрация SMS и не нужно ли обнаруживать другие параметры SMS, верните true напрямую
        if ("false".equalsIgnoreCase(paramValue)) {
            return true;
        }
        //Определить, пуст ли параметр ассоциации SMS
        ArrayList<String> list = new ArrayList<String>();
        list.add(Constant.SysMSMParam.SERVER_SMS_MAX_SEND_COUNT.getValue());
        list.add(Constant.SysMSMParam.ALIYUN_SMS_ACCESS_KEY_ID.getValue());
        list.add(Constant.SysMSMParam.ALIYUN_SMS_ACCESS_KEY_SECRET.getValue());
        list.add(Constant.SysMSMParam.ALIYUN_SMS_SIGN_NAME.getValue());
        list.add(Constant.SysMSMParam.ALIYUN_SMS_SMS_CODE_TEMPLATE_CODE.getValue());
        StringBuilder str = new StringBuilder();
        list.forEach(item -> {
            if (!StringUtils.isNoneBlank(item)) {
                str.append(",").append(item);
            }
        });
        if (!str.isEmpty()) {
            String promptStr = "%s这些参数不可以为空";
            String substring = str.substring(1, str.length());
            throw new RenException(promptStr.formatted(substring));
        }
        return true;
    }
    @Override
    public String getSystemWebMenu(boolean fromCache) {
        return getValue(Constant.SYSTEM_WEB_MENU, fromCache);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSystemWebMenu(String configJson) {
        //Получить текущую конфигурацию
        String currentConfig = getSystemWebMenu(false);
        Map<String, Object> currentMap = null;
        Map<String, Object> newMap = null;

        try {
            if (StringUtils.isNotBlank(currentConfig)) {
                currentMap = JsonUtils.parseMap(currentConfig);
            }
            if (StringUtils.isNotBlank(configJson)) {
                newMap = JsonUtils.parseMap(configJson);
            }
        } catch (Exception e) {
            throw new RenException(ErrorCode.PARAM_JSON_INVALID);
        }

        //Проверьте, отключена ли функция addressBook
        if (currentMap != null && newMap != null) {
            Map<?, ?> currentFeatures = Map.class.cast(currentMap.get("features"));
            Map<?, ?> newFeatures = Map.class.cast(newMap.get("features"));

            if (currentFeatures != null && newFeatures != null) {
                Object currentAddressBookObj = currentFeatures.get("addressBook");
                Object newAddressBookObj = newFeatures.get("addressBook");

                Boolean currentEnabled = false;
                Boolean newEnabled = false;

                if (currentAddressBookObj instanceof Map<?, ?> currentAddressBook) {
                    Object enabled = currentAddressBook.get("enabled");
                    currentEnabled = enabled != null ? Boolean.class.cast(enabled) : false;
                }

                if (newAddressBookObj instanceof Map<?, ?> newAddressBook) {
                    Object enabled = newAddressBook.get("enabled");
                    newEnabled = enabled != null ? Boolean.class.cast(enabled) : false;
                }

                //Если ранее включено, теперь отключено, удалите все плагины call_device
                if (Boolean.TRUE.equals(currentEnabled) && !Boolean.TRUE.equals(newEnabled)) {
                    agentPluginMappingService.deleteByPluginId("SYSTEM_PLUGIN_CALL_DEVICE");
                }
            }
        }

        //Обновить конфигурацию
        updateValueByCode(Constant.SYSTEM_WEB_MENU, configJson);
    }
}
