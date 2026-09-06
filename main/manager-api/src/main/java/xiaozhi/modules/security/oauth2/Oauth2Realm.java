package xiaozhi.modules.security.oauth2;

import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.DisabledAccountException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.user.UserDetail;
import xiaozhi.common.utils.ConvertUtils;
import xiaozhi.common.utils.MessageUtils;
import xiaozhi.modules.security.entity.SysUserTokenEntity;
import xiaozhi.modules.security.service.ShiroService;
import xiaozhi.modules.sys.entity.SysUserEntity;
import xiaozhi.modules.sys.enums.SuperAdminEnum;

/**
 * Сертификация
 * Авторское право (c) Open Source for All. Все права защищены.
 * Веб-сайт: https://www.renren.io
 */
@Component
public class Oauth2Realm extends AuthorizingRealm {
    @Lazy
    @Resource
    private ShiroService shiroService;

    private static final Logger logger = LoggerFactory.getLogger(Oauth2Realm.class);

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof Oauth2Token;
    }

    /**
     * Авторизация (вызывается при проверке разрешений)
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        UserDetail user = (UserDetail) principals.getPrimaryPrincipal();

        //Список разрешений пользователя
        Set<String> permsSet = new HashSet<>();

        if (user.getSuperAdmin() == SuperAdminEnum.YES.value()) {
            permsSet.add("sys:role:superAdmin");
            permsSet.add("sys:role:normal");
        } else {
            permsSet.add("sys:role:normal");
        }

        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        info.setStringPermissions(permsSet);
        return info;
    }

    /**
     * Аутентификация (вызывается при входе в систему)
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String accessToken = (String) token.getPrincipal();

        //Запрос информации о пользователе в соответствии с accessToken
        SysUserTokenEntity tokenEntity = shiroService.getByToken(accessToken);
        //сбой токена
        if (tokenEntity == null || tokenEntity.getExpireDate().getTime() < System.currentTimeMillis()) {
            throw new IncorrectCredentialsException(MessageUtils.getMessage(ErrorCode.TOKEN_INVALID));
        }

        //Запрос информации о пользователе
        SysUserEntity userEntity = shiroService.getUser(tokenEntity.getUserId());

        //Преобразование в объект UserDetail
        UserDetail userDetail = ConvertUtils.sourceToTarget(userEntity, UserDetail.class);

        userDetail.setToken(accessToken);

        //Блокировка аккаунта
        if (userDetail.getStatus() == null) {
            logger.error("账号状态异常，status 不能为空");
            throw new DisabledAccountException(MessageUtils.getMessage(ErrorCode.ACCOUNT_DISABLE));
        }

        if (userDetail.getStatus() == 0) {
            throw new LockedAccountException(MessageUtils.getMessage(ErrorCode.ACCOUNT_LOCK));
        }

        SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(userDetail, accessToken, getName());
        return info;
    }

}