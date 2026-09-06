package xiaozhi.modules.security.service;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Код подтверждения
 * Copyright (c) Renren Open Source All rights reserved.
 * Website: https://www.renren.io
 */
public interface CaptchaService {

    /**
     * Изображение кода подтверждения
     */
    void create(HttpServletResponse response, String uuid) throws IOException;

    /**
     * Проверка кода подтверждения
     * 
     * @param uuid   uuid
     * @param code   код подтверждения
     * @param delete удалять ли код подтверждения
     * @return true: успех false: неудача
     */
    boolean validate(String uuid, String code, Boolean delete);

    /**
     * Отправить SMS-код подтверждения
     * 
     * @param phone номер телефона
     */
    void sendSMSValidateCode(String phone);

    /**
     * Проверка SMS-кода подтверждения
     * 
     * @param phone  номер телефона
     * @param code   код подтверждения
     * @param delete удалять ли код подтверждения
     * @return true: успех false: неудача
     */
    boolean validateSMSValidateCode(String phone, String code, Boolean delete);
}
