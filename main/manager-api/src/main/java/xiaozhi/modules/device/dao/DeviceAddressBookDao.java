package xiaozhi.modules.device.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import xiaozhi.modules.device.entity.DeviceAddressBookEntity;

@Mapper
public interface DeviceAddressBookDao extends BaseMapper<DeviceAddressBookEntity> {

    /**
     * Добавить запись контактов устройства
     */
    int insertAddressBook(DeviceAddressBookEntity entity);

    /**
     * Получить список адресной книги устройства
     */
    List<DeviceAddressBookEntity> getAddressBookList(@Param("macAddress") String macAddress);

    /**
     * Обновить псевдоним
     */
    void updateAlias(@Param("macAddress") String macAddress, @Param("targetMac") String targetMac, @Param("alias") String alias);

    /**
     * Обновить разрешения
     */
    void updatePermission(@Param("macAddress") String macAddress, @Param("targetMac") String targetMac, @Param("hasPermission") Boolean hasPermission);

    /**
     * Массовое удаление записей контактов, связанных с устройством
     */
    void deleteByMacAddresses(@Param("macAddresses") List<String> macAddresses);
}
