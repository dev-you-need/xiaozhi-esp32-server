package xiaozhi.modules.security.service;

import xiaozhi.common.page.TokenDTO;
import xiaozhi.common.service.BaseService;
import xiaozhi.common.utils.Result;
import xiaozhi.modules.security.entity.SysUserTokenEntity;
import xiaozhi.modules.sys.dto.PasswordDTO;
import xiaozhi.modules.sys.dto.SysUserDTO;

/**
 * Токен пользователя
 * Copyright (c) Renren Open Source All rights reserved.
 * Website: https://www.renren.io
 */
public interface SysUserTokenService extends BaseService<SysUserTokenEntity> {

    /**
     * Сгенерировать токен
     *
     * @param userId ID пользователя
     */
    Result<TokenDTO> createToken(Long userId);

    SysUserDTO getUserByToken(String token);

    /**
     * Выход из системы
     *
     * @param userId ID пользователя
     */
    void logout(Long userId);

    /**
     * Изменить пароль
     *
     * @param userId
     * @param passwordDTO
     */
    void changePassword(Long userId, PasswordDTO passwordDTO);

}