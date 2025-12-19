package com.matjazt.tools;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SimpleTools {

    private static final DateTimeFormatter DEFAULT_LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String formatDefault(LocalDateTime timestamp) {
        return timestamp.format(DEFAULT_LOCAL_DATE_TIME_FORMATTER);
    }
}