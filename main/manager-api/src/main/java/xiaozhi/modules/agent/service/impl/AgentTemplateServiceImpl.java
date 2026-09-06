package xiaozhi.modules.agent.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;

import xiaozhi.modules.agent.dao.AgentTemplateDao;
import xiaozhi.modules.agent.entity.AgentTemplateEntity;
import xiaozhi.modules.agent.service.AgentTemplateService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;


/**
 * @author chenerlei
 * @description Сервис работы с БД для таблицы ai_agent_template (шаблоны конфигурации агента)реализация
 * @createDate 2025-03-22 11:48:18
 */

@Service
public class AgentTemplateServiceImpl extends CrudRepository<AgentTemplateDao, AgentTemplateEntity>
        implements AgentTemplateService {

    
/**
     * Получить шаблон по умолчанию
     * 
     * @return Сущность шаблона по умолчанию
     */

    public AgentTemplateEntity getDefaultTemplate() {
        LambdaQueryWrapper<AgentTemplateEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(AgentTemplateEntity::getSort)
                .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    
/**
     * Обновить ID модели в шаблоне по умолчанию
     * 
     * @param modelType Тип модели
     * @param modelId   ID модели
     */

    @Override
    public void updateDefaultTemplateModelId(String modelType, String modelId) {
        modelType = modelType.toUpperCase();
        // Если модель RAG — обновление не требуется
        if (modelType.equals("RAG")) {
            return;
        }

        UpdateWrapper<AgentTemplateEntity> wrapper = new UpdateWrapper<>();
        switch (modelType) {
            case "ASR":
                wrapper.set("asr_model_id", modelId);
                break;
            case "VAD":
                wrapper.set("vad_model_id", modelId);
                break;
            case "LLM":
                wrapper.set("llm_model_id", modelId);
                break;
            case "TTS":
                wrapper.set("tts_model_id", modelId);
                wrapper.set("tts_voice_id", null);
                break;
            case "VLLM":
                wrapper.set("vllm_model_id", modelId);
                break;
            case "MEMORY":
                wrapper.set("mem_model_id", modelId);
                break;
            case "INTENT":
                wrapper.set("intent_model_id", modelId);
                break;
        }
        wrapper.ge("sort", 0);
        update(wrapper);
    }

    @Override
    public void reorderTemplatesAfterDelete(Integer deletedSort) {
        if (deletedSort == null) {
            return;
        }
        
        // Запросить записи со значением сортировки больше, чем у удалённого шаблона
        UpdateWrapper<AgentTemplateEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.gt("sort", deletedSort)
                    .setSql("sort = sort - 1");
        
        // Массовое обновление — уменьшить значение сортировки на 1
        this.update(updateWrapper);
    }

    @Override
    public Integer getNextAvailableSort() {
        // Запросить все существующие значения сортировки по возрастанию
        List<Integer> sortValues = baseMapper.selectList(new QueryWrapper<AgentTemplateEntity>())
                .stream()
                .map(AgentTemplateEntity::getSort)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
        
        // Если значений сортировки нет — вернуть 1
        if (sortValues.isEmpty()) {
            return 1;
        }
        
        // Найти наименьший неиспользуемый номер
        int expectedSort = 1;
        for (Integer sort : sortValues) {
            if (sort > expectedSort) {
                // Найден пропущенный номер
                return expectedSort;
            }
            expectedSort = sort + 1;
        }
        
        // Если пропусков нет — вернуть максимальный номер + 1
        return expectedSort;
    }
}
