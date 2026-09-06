package xiaozhi.modules.agent.dao;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import xiaozhi.modules.agent.entity.AgentTemplateEntity;


/**
 * @author chenerlei
 * @description Маппер работы с БД для таблицы ai_agent_template (шаблоны конфигурации агента)
 * @createDate 2025-03-22 11:48:18
 */

@Mapper
public interface AgentTemplateDao extends BaseMapper<AgentTemplateEntity> {

}
