package xiaozhi.modules.sms.service;

/**
 * Интерфейс определения методов службы SMS
 *
 * @author zjy
 * @since 2025-05-12
 */
public interface SmsService {

    /**
     * Отправка SMS с кодом проверки
     * @param phone Номер телефона
     * @param VerificationCode Код проверки
     */
    void sendVerificationCodeSms(String phone, String VerificationCode) ;
}
