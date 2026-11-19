package com.tiketin.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtil {
    private static final String[] BULAN_ID = {
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    };
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2})[:.](\\d{2})");

    public static LocalDate parseDateSafe(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        String trimmed = dateStr.trim();
        if (trimmed.length() > 10) {
            trimmed = trimmed.substring(0, 10);
        }
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Parses event date and time string (e.g. "2026-12-01" and "08:00 WIB")
     * into LocalDateTime. Defaults time to 23:59 when time is missing/unparseable.
     */
    public static LocalDateTime parseEventDateTime(String dateStr, String timeStr) {
        LocalDate date = parseDateSafe(dateStr);
        if (date == null) {
            return null;
        }
        LocalTime time = LocalTime.of(23, 59);
        if (timeStr != null && !timeStr.isBlank()) {
            Matcher m = TIME_PATTERN.matcher(timeStr);
            if (m.find()) {
                try {
                    int h = Integer.parseInt(m.group(1));
                    int min = Integer.parseInt(m.group(2));
                    if (h >= 0 && h <= 23 && min >= 0 && min <= 59) {
                        time = LocalTime.of(h, min);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return LocalDateTime.of(date, time);
    }

    public static boolean isEventPassed(String dateStr, String timeStr) {
        return isEventPassed(dateStr, timeStr, LocalDateTime.now());
    }

    public static boolean isEventPassed(String dateStr, String timeStr, LocalDateTime referenceNow) {
        LocalDateTime eventDt = parseEventDateTime(dateStr, timeStr);
        LocalDateTime now = referenceNow != null ? referenceNow : LocalDateTime.now();
        return eventDt != null && now.isAfter(eventDt);
    }

    public static String formatTanggalId(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return "-";
        }
        try {
            LocalDate date = parseDateSafe(dateStr);
            if (date == null) {
                return dateStr;
            }
            int day = date.getDayOfMonth();
            int month = date.getMonthValue();
            int year = date.getYear();
            return String.format("%d %s %d", day, BULAN_ID[month - 1], year);
        } catch (ArrayIndexOutOfBoundsException e) {
            return dateStr;
        }
    }

    public static boolean isUpcoming(String dateStr) {
        LocalDate date = parseDateSafe(dateStr);
        return date != null && !date.isBefore(LocalDate.now());
    }
}
