package xiaozhi.modules.agent.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xiaozhi.modules.agent.entity.AgentPluginMapping;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;


/**
* @description Маппер работы с БД для таблицы ai_agent_plugin_mapping (уникальные отображения агент-плагин)
* @createDate 2025-05-25 22:33:17
* @Entity xiaozhi.modules.agent.entity.AgentPluginMapping
*/

@Mapper
public interface AgentPluginMappingMapper extends BaseMapper<AgentPluginMapping> {
    List<AgentPluginMapping> selectPluginsByAgentId(@Param("agentId") String agentId);
}




