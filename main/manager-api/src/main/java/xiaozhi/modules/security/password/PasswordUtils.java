package xiaozhi.modules.security.password;

/**
 * Криптографические инструменты * (c) Open Source for All rights reserved. * Website: https://www.renren.io
 */
public class PasswordUtils {
    private static PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Шифрование     *
     * @ param str string     * @ return возвращает зашифрованную строку     */
    public static String encode(String str) {
        return passwordEncoder.encode(str);
    }

    /**
     * Сравнить, равны ли пароли     *
     * Пароль с открытым текстом @ param str     * @ param пароль Зашифрованный пароль     * @ return true: success false: failure     */
    public static boolean matches(String str, String password) {
        return passwordEncoder.matches(str, password);
    }

    public static void main(String[] args) {
        String str = "admin";
        String password = encode(str);

        System.out.println(password);
        System.out.println(matches(str, password));
    }

}
