package xiaozhi.common.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Обработка дат
 * Copyright (c) 人人开源 All rights reserved.
 * Website: https://www.renren.io
 */
public class DateUtils {
    /**
     * Формат времени(yyyy-MM-dd)
     */
    public final static String DATE_PATTERN = "yyyy-MM-dd";
    /**
     * Формат времени(yyyy-MM-dd HH:mm:ss)
     */
    public final static String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public final static String DATE_TIME_MILLIS_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";


    /**
     * Форматирование даты, формат: yyyy-MM-dd
     *
     * @param date дата
     * @return дата в формате yyyy-MM-dd
     */
    public static String format(Date date) {
        return format(date, DATE_PATTERN);
    }

    /**
     * Форматирование даты, формат: yyyy-MM-dd
     *
     * @param date    дата
     * @param pattern формат, например: DateUtils.DATE_TIME_PATTERN
     * @return дата в формате yyyy-MM-dd
     */
    public static String format(Date date, String pattern) {
        if (date != null) {
            SimpleDateFormat df = new SimpleDateFormat(pattern);
            return df.format(date);
        }
        return null;
    }

    /**
     * Разбор даты
     *
     * @param date    дата
     * @param pattern формат, например: DateUtils.DATE_TIME_PATTERN
     * @return Date
     */
    public static Date parse(String date, String pattern) {
        try {
            return new SimpleDateFormat(pattern).parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String getDateTimeNow() {
        return getDateTimeNow(DATE_TIME_PATTERN);
    }

    public static String getDateTimeNow(String pattern) {
        return format(new Date(), pattern);
    }

    public static String millsToSecond(long mills) {
        return String.format("%.3f", mills / 1000.0);
    }

    /**
     * Получить краткую строку времени: до 10 секунд — «только что», далее — секунды, минуты, часы, дни; более недели — полная дата и время
     * @param date
     * @return
     */
    public static String getShortTime(Date date) {
        if (date == null) {
            return null;
        }
        // Преобразование Date в Instant
        LocalDateTime localDateTime = date.toInstant()
                // Получение системного часового пояса
                .atZone(ZoneId.systemDefault())
                // Преобразование в LocalDateTime
                .toLocalDateTime();
        // Текущее время
        LocalDateTime now = LocalDateTime.now();
        // Разница во времени в секундах
        long secondsBetween = ChronoUnit.SECONDS.between(localDateTime, now);

        if (secondsBetween <= 10) {
            return "刚刚";
        } else if (secondsBetween < 60) {
            return secondsBetween + "秒前";
        } else if (secondsBetween < 60 * 60) {
            return secondsBetween / 60 + "分钟前";
        } else if (secondsBetween < 86400) {
            return secondsBetween / 3600 + "小时前";
        } else if (secondsBetween < 604800) {
            return secondsBetween / 86400 + "天前";
        } else {
            // Более недели — показать полные дату и время
            return format(date,DATE_TIME_PATTERN);
        }
    }
}
