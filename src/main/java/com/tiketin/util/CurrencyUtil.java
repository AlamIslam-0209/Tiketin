package com.tiketin.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class CurrencyUtil {
    private static final DecimalFormat FORMATTER;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        FORMATTER = new DecimalFormat("#,##0", symbols);
    }

    public static String formatRupiah(long amount) {
        return "Rp " + FORMATTER.format(amount);
    }
}
