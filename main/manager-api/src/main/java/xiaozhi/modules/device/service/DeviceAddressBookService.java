package xiaozhi.modules.device.service;

import java.util.List;
import java.util.Map;

import xiaozhi.modules.device.entity.DeviceAddressBookEntity;

public interface DeviceAddressBookService {

    /**
     * Получить список адресной книги устройства
     */
    List<DeviceAddressBookEntity> getAddressBookList(String macAddress);

    /**
     * Получить адресные книги всех устройств (для глобального кэша)
     */
    Map<String, Map<String, String>> getAllAddressBooks();

    /**
     * Обновить псевдоним
     */
    void updateAlias(String macAddress, String targetMac, String alias);

    /**
     * Обновить разрешение
     */
    void updatePermission(String macAddress, String targetMac, Boolean hasPermission);

    /**
     * Добавить или обновить запись адресной книги
     */
    void saveOrUpdate(String macAddress, String targetMac, String alias, Boolean hasPermission);

    /**
     * Обновить кэш адресной книги
     */
    void refreshCache();

    /**
     * Инициировать вызов по никнейму
     * @param callerMac MAC-адрес вызывающего
     * @param nickname никнейм вызываемого
     * @param isAnswer режим ответа (пропуск проверки разрешений)
     */
    Map<String, Object> callByNickname(String callerMac, String nickname, boolean isAnswer);

    /**
     * Пакетное удаление записей адресной книги, связанных с устройствами
     */
    void deleteByMacAddresses(List<String> macAddresses);
}