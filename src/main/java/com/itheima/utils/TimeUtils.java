// com/itheima/utils/TimeUtils.java
package com.itheima.utils;

import lombok.extern.slf4j.Slf4j;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
public class TimeUtils {

    /**
     * 将各种格式的时间转换为时间戳
     */
    public static Long toTimestamp(Object timeObj) {
        if (timeObj == null) return System.currentTimeMillis();

        try {
            // 1. 如果是数字
            if (timeObj instanceof Number) {
                return ((Number) timeObj).longValue();
            }

            // 2. 如果是字符串
            String timeStr = timeObj.toString().trim();

            // 2.1 尝试解析为数字
            try {
                return Long.parseLong(timeStr);
            } catch (NumberFormatException e) {
                // 2.2 尝试解析为ISO时间
                return parseIsoTime(timeStr);
            }

        } catch (Exception e) {
            log.warn("时间转换失败: {}", timeObj, e);
            return System.currentTimeMillis();
        }
    }

    /**
     * 解析ISO时间字符串
     */
    private static Long parseIsoTime(String timeStr) {
        // 常见的ISO格式
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss.SSS",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy/MM/dd HH:mm:ss"
        };

        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                LocalDateTime dateTime = LocalDateTime.parse(timeStr, formatter);
                return dateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
            } catch (DateTimeParseException e) {
                // 继续尝试下一个格式
            }
        }

        // 如果都失败，尝试使用Instant直接解析
        try {
            return Instant.parse(timeStr).toEpochMilli();
        } catch (Exception e) {
            throw new IllegalArgumentException("无法解析时间格式: " + timeStr);
        }
    }

    /**
     * 将时间戳格式化为ISO字符串
     */
    public static String formatToIso(Long timestamp) {
        if (timestamp == null) return null;
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);
    }
}