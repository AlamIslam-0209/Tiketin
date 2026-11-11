package com.tiketin.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class DateUtil {
    private static final String[] BULAN_ID = {
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    };

    public static String formatTanggalId(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return "-";
        }
        try {
            LocalDate date = LocalDate.parse(dateStr.trim());
            int day = date.getDayOfMonth();
            int month = date.getMonthValue();
            int year = date.getYear();
            return String.format("%d %s %d", day, BULAN_ID[month - 1], year);
        } catch (DateTimeParseException | ArrayIndexOutOfBoundsException e) {
            return dateStr;
        }
    }

    public static boolean isUpcoming(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return false;
        }
        try {
            LocalDate date = LocalDate.parse(dateStr.trim());
            return !date.isBefore(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
