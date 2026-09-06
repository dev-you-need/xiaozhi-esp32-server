package xiaozhi.modules.security.user;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import xiaozhi.common.user.UserDetail;

/**
 * Класс инструмента Shiro
 * Авторское право (c) Open Source for All. Все права защищены.
 * Веб-сайт: https://www.renren.io
 */
public class SecurityUser {

    public static Subject getSubject() {
        try {
            return SecurityUtils.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Получить информацию о пользователе
     */
    public static UserDetail getUser() {
        Subject subject = getSubject();
        if (subject == null) {
            return new UserDetail();
        }

        UserDetail user = (UserDetail) subject.getPrincipal();
        if (user == null) {
            return new UserDetail();
        }

        return user;
    }

    public static String getToken() {
        return getUser().getToken();
    }

    /**
     * Получить идентификатор пользователя
     */
    public static Long getUserId() {
        return getUser().getId();
    }
}