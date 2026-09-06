package xiaozhi.modules.agent.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import org.apache.ibatis.annotations.Select;
import xiaozhi.common.dao.BaseDao;
import xiaozhi.modules.agent.entity.AgentEntity;
import xiaozhi.modules.agent.vo.AgentInfoVO;

@Mapper
public interface AgentDao extends BaseDao<AgentEntity> {
    
/**
     * Получить количество устройств агента
     * 
     * @param agentId ID агента
     * @return Количество устройств
     */

    Integer getDeviceCountByAgentId(@Param("agentId") String agentId);

    
/**
     * Запросить информацию агента по умолчанию для устройства по MAC-адресу
     *
     * @param macAddress ОборудованиеMAC-адрес
     * @return Информация агента по умолчанию
     */

    @Select(" SELECT a.* FROM ai_device d " +
            " LEFT JOIN ai_agent a ON d.agent_id = a.id " +
            " WHERE d.mac_address = #{macAddress} " +
            " ORDER BY d.id DESC LIMIT 1")
    AgentEntity getDefaultAgentByMacAddress(@Param("macAddress") String macAddress);

    
/**
     * Запросить информацию агента по ID, включая информацию о плагинах
     *
     * @param agentId ID агента
     */

    AgentInfoVO selectAgentInfoById(@Param("agentId") String agentId);

    
/**
     * Заблокировать основную запись агента для последовательной записи конфигурации
     *
     * @param agentId ID агента
     */

    AgentEntity selectByIdForUpdate(@Param("agentId") String agentId);

    
/**
     * Точная запись полей агента, покрываемых снимком, включая null из целевого снимка.
     * Не обновлять поля, не относящиеся к снимку: владельца, информацию о создании и т. д.
     *
     * @param agent С примененным целевым снимкомАгент
     * @return Количество затронутых строк
     */

    int updateSnapshotFields(@Param("agent") AgentEntity agent);
}
