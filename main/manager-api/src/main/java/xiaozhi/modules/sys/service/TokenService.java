package xiaozhi.modules.sys.service;

public interface TokenService {
    /**
     * Генерация токена
     *
     * @param userId
     * @return
     */
    String createToken(long userId);
}
