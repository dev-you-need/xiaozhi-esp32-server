package xiaozhi.modules.agent.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;

import xiaozhi.common.constant.Constant;
import xiaozhi.common.page.PageData;
import xiaozhi.common.utils.ConvertUtils;
import xiaozhi.common.utils.JsonUtils;
import xiaozhi.modules.agent.Enums.AgentChatHistoryType;
import xiaozhi.modules.agent.dao.AiAgentChatHistoryDao;
import xiaozhi.modules.agent.dto.AgentChatHistoryDTO;
import xiaozhi.modules.agent.dto.AgentChatSessionDTO;
import xiaozhi.modules.agent.entity.AgentChatHistoryEntity;
import xiaozhi.modules.agent.service.AgentChatHistoryService;
import xiaozhi.modules.agent.service.AgentChatTitleService;
import xiaozhi.modules.agent.vo.AgentChatHistoryUserVO;


/**
 * Сервис обработки таблицы истории чата агента {@link AgentChatHistoryService} impl
 *
 * @author Goody
 * @version 1.0, 2025/4/30
 * @since 1.0.0
 */

@Service
@RequiredArgsConstructor
public class AgentChatHistoryServiceImpl extends CrudRepository<AiAgentChatHistoryDao, AgentChatHistoryEntity>
        implements AgentChatHistoryService {

    private final AgentChatTitleService agentChatTitleService;

    @Override
    public PageData<AgentChatSessionDTO> getSessionListByAgentId(Map<String, Object> params) {
        String agentId = (String) params.get("agentId");
        int page = Integer.parseInt(params.get(Constant.PAGE).toString());
        int limit = Integer.parseInt(params.get(Constant.LIMIT).toString());

        // Сформировать условия запроса
        QueryWrapper<AgentChatHistoryEntity> wrapper = new QueryWrapper<>();
        wrapper.select("session_id", "MAX(created_at) as created_at", "COUNT(*) as chat_count")
                .eq("agent_id", agentId)
                .groupBy("session_id")
                .orderByDesc("created_at");

        // Выполнить постраничный запрос
        Page<Map<String, Object>> pageParam = new Page<>(page, limit);
        IPage<Map<String, Object>> result = this.baseMapper.selectMapsPage(pageParam, wrapper);

        List<AgentChatSessionDTO> records = result.getRecords().stream().map(map -> {
            AgentChatSessionDTO dto = new AgentChatSessionDTO();
            dto.setSessionId((String) map.get("session_id"));
            dto.setCreatedAt((LocalDateTime) map.get("created_at"));
            dto.setChatCount(((Number) map.get("chat_count")).intValue());
            dto.setTitle(agentChatTitleService.getTitleBySessionId(dto.getSessionId()));
            return dto;
        }).collect(Collectors.toList());

        return new PageData<>(records, result.getTotal());
    }

    @Override
    public List<AgentChatHistoryDTO> getChatHistoryBySessionId(String agentId, String sessionId) {
        // Сформировать условия запроса
        QueryWrapper<AgentChatHistoryEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("agent_id", agentId)
                .eq("session_id", sessionId)
                .orderByAsc("created_at");

        // Запросить записи чата
        List<AgentChatHistoryEntity> historyList = list(wrapper);

        // Преобразовать в DTO
        return ConvertUtils.sourceToTarget(historyList, AgentChatHistoryDTO.class);
    }

    @Override
    public String getAgentIdBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        AgentChatHistoryEntity entity = baseMapper.selectOne(
                new LambdaQueryWrapper<AgentChatHistoryEntity>()
                        .select(AgentChatHistoryEntity::getAgentId)
                        .eq(AgentChatHistoryEntity::getSessionId, sessionId)
                        .last("LIMIT 1"));
        return entity == null ? null : entity.getAgentId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByAgentId(String agentId, Boolean deleteAudio, Boolean deleteText) {
        if (deleteAudio) {
            // Удаление аудио пакетами для избежания тайм-аута
            List<String> audioIds = baseMapper.getAudioIdsByAgentId(agentId);
            if (CollUtil.isNotEmpty(audioIds)) {
                // Удалять по 1000 записей за пакет
                List<List<String>> batch = ListUtil.split(audioIds, 1000);
                batch.forEach(dataList -> {
                    baseMapper.deleteAudioByIds(dataList);
                });
            }
        }
        if (deleteAudio && !deleteText) {
            baseMapper.deleteAudioIdByAgentId(agentId);
        }
        if (deleteText) {
            baseMapper.deleteHistoryByAgentId(agentId);
        }

    }

    @Override
    public List<AgentChatHistoryUserVO> getRecentlyFiftyByAgentId(String agentId) {
        // Сформировать условия запроса (без сортировки по времени создания — данные уже упорядочены по первичному ключу
        // Без сортировки снижается нагрузка на полный проход при постраничной навигации)
        LambdaQueryWrapper<AgentChatHistoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(AgentChatHistoryEntity::getContent, AgentChatHistoryEntity::getAudioId)
                .eq(AgentChatHistoryEntity::getAgentId, agentId)
                .eq(AgentChatHistoryEntity::getChatType, AgentChatHistoryType.USER.getValue())
                .isNotNull(AgentChatHistoryEntity::getAudioId)
                // Добавлена эта строка для обеспечения убывающего порядка по времени создания
                // Используется ID, т. к. чем больше ID, тем позже запись создана — результат совпадает с убывающей сортировкой по времени создания
                // Преимущество сортировки по ID — высокая производительность за счёт индекса первичного ключа
                .orderByDesc(AgentChatHistoryEntity::getId);

        // Сформировать постраничный запрос для получения первых 50 записей
        Page<AgentChatHistoryEntity> pageParam = new Page<>(0, 50);
        IPage<AgentChatHistoryEntity> result = this.baseMapper.selectPage(pageParam, wrapper);
        return result.getRecords().stream().map(item -> {
            AgentChatHistoryUserVO vo = ConvertUtils.sourceToTarget(item, AgentChatHistoryUserVO.class);
            // Обработать поле content, чтобы возвращать только содержимое чата
            if (vo != null && vo.getContent() != null) {
                vo.setContent(extractContentFromString(vo.getContent()));
            }
            return vo;
        }).toList();
    }

    
/**
     * Извлечь содержимое чата из поля content
     * Если content в формате JSON (например {"speaker": "...", "content": "..."}), извлечь поле content
     * поле
     * Если content — обычная строка, вернуть напрямую
     * 
     * @param content Исходное содержимое
     * @return Извлечённое содержимое чата
     */

    private String extractContentFromString(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        // Попытаться распознать как JSON
        try {
            Map<String, Object> jsonMap = JsonUtils.parseMap(content);
            if (jsonMap != null && jsonMap.containsKey("content")) {
                Object contentObj = jsonMap.get("content");
                return contentObj != null ? contentObj.toString() : content;
            }
        } catch (Exception e) {
            // Если это не валидный JSON — вернуть исходное содержимое
        }

        // Если формат не JSON или поле content отсутствует — вернуть исходное содержимое
        return content;
    }

    @Override
    public String getContentByAudioId(String audioId) {
        AgentChatHistoryEntity agentChatHistoryEntity = baseMapper
                .selectOne(new LambdaQueryWrapper<AgentChatHistoryEntity>()
                        .select(AgentChatHistoryEntity::getContent)
                        .eq(AgentChatHistoryEntity::getAudioId, audioId));
        return agentChatHistoryEntity == null ? null : agentChatHistoryEntity.getContent();
    }

    @Override
    public String getAgentIdByAudioId(String audioId) {
        if (audioId == null || audioId.isBlank()) {
            return null;
        }
        AgentChatHistoryEntity entity = baseMapper.selectOne(
                new LambdaQueryWrapper<AgentChatHistoryEntity>()
                        .select(AgentChatHistoryEntity::getAgentId)
                        .eq(AgentChatHistoryEntity::getAudioId, audioId)
                        .last("LIMIT 1"));
        return entity == null ? null : entity.getAgentId();
    }

    @Override
    public boolean isAudioOwnedByAgent(String audioId, String agentId) {
        // Проверить наличие записи с указанными audioId и agentId — если ровно одна, данные принадлежат агенту
        Long row = baseMapper.selectCount(new LambdaQueryWrapper<AgentChatHistoryEntity>()
                .eq(AgentChatHistoryEntity::getAudioId, audioId)
                .eq(AgentChatHistoryEntity::getAgentId, agentId));
        return row == 1;
    }
}
