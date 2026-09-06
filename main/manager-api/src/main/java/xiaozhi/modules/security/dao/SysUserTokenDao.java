package xiaozhi.modules.security.dao;

import java.util.Date;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import xiaozhi.common.dao.BaseDao;
import xiaozhi.modules.security.entity.SysUserTokenEntity;

/**
 * Маркер пользователя системы * (c) Open Source for All rights reserved. * Website: https://www.renren.io
 */
@Mapper
public interface SysUserTokenDao extends BaseDao<SysUserTokenEntity> {

    SysUserTokenEntity getByToken(String token);

    SysUserTokenEntity getByUserId(Long userId);

    void logout(@Param("userId") Long userId, @Param("expireDate") Date expireDate);
}
