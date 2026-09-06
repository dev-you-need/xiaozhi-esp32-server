package xiaozhi.modules.sys.service;

import xiaozhi.common.page.PageData;
import xiaozhi.common.service.BaseService;
import xiaozhi.modules.sys.dto.AdminPageUserDTO;
import xiaozhi.modules.sys.dto.PasswordDTO;
import xiaozhi.modules.sys.dto.SysUserDTO;
import xiaozhi.modules.sys.entity.SysUserEntity;
import xiaozhi.modules.sys.vo.AdminPageUserVO;

/**
 * Системный пользователь
 */
public interface SysUserService extends BaseService<SysUserEntity> {

    SysUserDTO getByUsername(String username);

    SysUserDTO getByUserId(Long userId);

    void save(SysUserDTO dto);

    /**
     * Удалить указанного пользователя и связанные данные об устройствах и агентах
     * 
     * @param ids
     */
    void deleteById(Long ids);

    /**
     * Проверить, разрешено ли изменение пароля
     * 
     * @param userId      ID пользователя
     * @param passwordDTO Параметры проверки пароля
     */
    void changePassword(Long userId, PasswordDTO passwordDTO);

    /**
     * Изменить пароль напрямую, без проверки
     * 
     * @param userId   ID пользователя
     * @param password Пароль
     */
    void changePasswordDirectly(Long userId, String password);

    /**
     * Сбросить пароль
     * 
     * @param userId ID пользователя
     * @return Случайно сгенерированный пароль, соответствующий стандартам
     */
    String resetPassword(Long userId);

    /**
     * Постраничная информация о пользователях для администратора
     * 
     * @param dto Параметры постраничного поиска
     * @return Данные постраничного списка пользователей
     */
    PageData<AdminPageUserVO> page(AdminPageUserDTO dto);

    /**
     * Пакетное изменение статуса пользователя
     * 
     * @param status  Статус пользователя
     * @param userIds Массив ID пользователей
     */
    void changeStatus(Integer status, String[] userIds);

    /**
     * Получить, разрешена ли регистрация пользователей
     * 
     * @return Разрешена ли регистрация пользователей
     */
    boolean getAllowUserRegister();
}
