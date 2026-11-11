package com.tiketin.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class TicketCodeGenerator {
    private static final Random RANDOM = new Random();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static String generateOrderCode() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        int randVal = RANDOM.nextInt(0x1000000);
        String hexPart = String.format("%06X", randVal);
        return "TKT-" + datePart + "-" + hexPart;
    }

    public static String formatSingleTicketCode(String orderCode, int index) {
        return String.format("%s-%02d", orderCode, index);
    }
}
