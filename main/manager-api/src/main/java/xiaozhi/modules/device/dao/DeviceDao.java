package xiaozhi.modules.device.dao;

import java.util.Date;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import xiaozhi.modules.device.entity.DeviceEntity;

@Mapper
public interface DeviceDao extends BaseMapper<DeviceEntity> {
    /**
     * Получить последнее время подключения для всех устройств этого агента
     * 
     * @param agentId Агентid
     * @return
     */
    Date getAllLastConnectedAtByAgentId(String agentId);

}