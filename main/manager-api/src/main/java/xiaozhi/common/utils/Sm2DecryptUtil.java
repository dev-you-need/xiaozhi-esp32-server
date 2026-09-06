package xiaozhi.common.utils;

import org.apache.commons.lang3.StringUtils;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.exception.RenException;
import xiaozhi.modules.security.service.CaptchaService;
import xiaozhi.modules.sys.service.SysParamsService;

/**
 * Утилита дешифрования SM2 и проверки кода подтверждения
 * Инкапсулирует повторяющуюся логику дешифрования SM2, извлечения и проверки кода подтверждения
 */
public class Sm2DecryptUtil {

    /**
     * Длина кода подтверждения
     */
    private static final int CAPTCHA_LENGTH = 5;

    /**
     * Дешифрование данных SM2, извлечение и проверка кода подтверждения
     * 
     * @param encryptedPassword зашифрованный пароль SM2
     * @param captchaId         ID кода подтверждения
     * @param captchaService    сервис кодов подтверждения
     * @param sysParamsService  сервис системных параметров
     * @return расшифрованный пароль
     */
    public static String decryptAndValidateCaptcha(String encryptedPassword, String captchaId,
            CaptchaService captchaService, SysParamsService sysParamsService) {
        // Получение приватного ключа SM2
        String privateKeyStr = sysParamsService.getValue(Constant.SM2_PRIVATE_KEY, true);
        if (StringUtils.isBlank(privateKeyStr)) {
            throw new RenException(ErrorCode.SM2_KEY_NOT_CONFIGURED);
        }

        // Дешифрование пароля с помощью приватного ключа SM2
        String decryptedContent;
        try {
            decryptedContent = SM2Utils.decrypt(privateKeyStr, encryptedPassword);
        } catch (Exception e) {
            throw new RenException(ErrorCode.SM2_DECRYPT_ERROR);
        }

        // Разделение кода подтверждения и пароля: первые 5 символов — код подтверждения, остальное — пароль
        if (decryptedContent.length() > CAPTCHA_LENGTH) {
            String embeddedCaptcha = decryptedContent.substring(0, CAPTCHA_LENGTH);
            String actualPassword = decryptedContent.substring(CAPTCHA_LENGTH);

            boolean embeddedCaptchaValid = captchaService.validate(captchaId, embeddedCaptcha, true);
            if (!embeddedCaptchaValid) {
                throw new RenException(ErrorCode.SMS_CAPTCHA_ERROR);
            }

            return actualPassword;
        } else if (decryptedContent.length() > 0) {
            throw new RenException(ErrorCode.SMS_CAPTCHA_ERROR);
        } else {
            throw new RenException(ErrorCode.SM2_DECRYPT_ERROR);
        }
    }
}