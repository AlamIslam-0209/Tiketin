package com.tiketin.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared input, schedule, payment, and media validation helpers used by both
 * JavaFX UI views and DAO persistence layers.
 */
public final class ValidationUtil {

    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_EXTS = Set.of("png", "jpg", "jpeg", "webp", "gif");

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,50}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{8,15}$");
    private static final Pattern CLOCK_PATTERN = Pattern.compile("(\\d{1,2})[:.](\\d{2})");
    private static final Pattern BANK_ACC_PATTERN = Pattern.compile("^[0-9\\-]{5,32}$");
    private static final Pattern QRIS_ID_PATTERN = Pattern.compile("^[A-Za-z0-9\\-]{5,40}$");

    private ValidationUtil() {}

    public static String validateRequired(String value, String fieldLabel) {
        if (value == null || value.trim().isEmpty()) {
            return fieldLabel + " wajib diisi.";
        }
        return null;
    }

    public static String validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "Username wajib diisi.";
        }
        String trimmed = username.trim();
        if (trimmed.length() < 3) {
            return "Username minimal 3 karakter.";
        }
        if (trimmed.length() > 50) {
            return "Username maksimal 50 karakter.";
        }
        if (trimmed.chars().anyMatch(Character::isWhitespace)) {
            return "Username tidak boleh mengandung spasi.";
        }
        if (!USERNAME_PATTERN.matcher(trimmed).matches()) {
            return "Username hanya boleh berisi huruf, angka, titik (.), garis bawah (_), atau strip (-).";
        }
        return null;
    }

    public static String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Password wajib diisi.";
        }
        if (password.length() < 6) {
            return "Password minimal 6 karakter.";
        }
        if (password.length() > 100) {
            return "Password maksimal 100 karakter.";
        }
        return null;
    }

    public static String validateContact(String contact) {
        if (contact == null || contact.trim().isEmpty()) {
            return "Kontak aktif wajib diisi.";
        }
        String trimmed = contact.trim();
        if (EMAIL_PATTERN.matcher(trimmed).matches()) {
            return null;
        }
        String cleanedPhone = trimmed.replaceAll("[\\s\\-().]", "");
        if (PHONE_PATTERN.matcher(cleanedPhone).matches()) {
            return null;
        }
        return "Kontak harus berupa nomor WhatsApp (8–15 digit) atau email yang valid.";
    }

    public static String validateEventTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return "Waktu acara wajib diisi (contoh: 19:00 WIB).";
        }
        Matcher m = CLOCK_PATTERN.matcher(timeStr.trim());
        if (!m.find()) {
            return "Waktu acara harus mencantumkan jam yang valid (contoh: 19:00 WIB).";
        }
        try {
            int hour = Integer.parseInt(m.group(1));
            int minute = Integer.parseInt(m.group(2));
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return "Jam acara tidak valid (gunakan rentang 00:00 – 23:59).";
            }
        } catch (NumberFormatException ex) {
            return "Format waktu acara tidak valid.";
        }
        return null;
    }

    public static String validateFutureSchedule(LocalDate date, String timeStr) {
        if (date == null) {
            return "Tanggal acara wajib dipilih.";
        }
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            return "Tanggal acara tidak boleh di masa lalu.";
        }
        String timeErr = validateEventTime(timeStr);
        if (timeErr != null) {
            return timeErr;
        }
        LocalDateTime eventDt = DateUtil.parseEventDateTime(date.toString(), timeStr);
        if (eventDt != null && eventDt.toLocalDate().equals(today) && LocalDateTime.now().isAfter(eventDt)) {
            return "Waktu acara untuk hari ini sudah terlewat.";
        }
        return null;
    }

    public static String validateFutureSchedule(String dateStr, String timeStr) {
        LocalDate date = DateUtil.parseDateSafe(dateStr);
        if (date == null) {
            return "Format tanggal acara tidak valid (gunakan YYYY-MM-DD).";
        }
        return validateFutureSchedule(date, timeStr);
    }

    public static String validateEventFields(String name, String location, LocalDate date, String timeStr,
                                             int price, int slots, boolean requirePositiveSlots) {
        String err = validateRequired(name, "Nama acara");
        if (err != null) return err;

        err = validateRequired(location, "Lokasi / venue acara");
        if (err != null) return err;

        err = validateFutureSchedule(date, timeStr);
        if (err != null) return err;

        if (price < 0) {
            return "Harga tiket tidak boleh kurang dari Rp 0.";
        }
        if (requirePositiveSlots && slots < 1) {
            return "Kuota tiket minimal 1.";
        }
        if (!requirePositiveSlots && slots < 0) {
            return "Kuota tiket tidak boleh negatif.";
        }
        return null;
    }

    public static String validateOrderQuantity(int quantity) {
        if (quantity < 1 || quantity > 10) {
            return "Jumlah tiket harus antara 1 hingga 10 per transaksi.";
        }
        return null;
    }

    public static String normalizeAccountNumber(String bankName, String rawAccountNumber) {
        if (rawAccountNumber == null) return "";
        String trimmed = rawAccountNumber.replaceAll("\\s+", "");
        if (bankName != null && bankName.toLowerCase(Locale.ROOT).contains("qris")) {
            return trimmed;
        }
        return trimmed.replaceAll("[^0-9\\-]", "");
    }

    public static String validatePaymentAccount(String bankName, String rawAccountNumber, String accountName) {
        String err = validateRequired(bankName, "Nama bank / e-wallet");
        if (err != null) return err;

        err = validateRequired(accountName, "Nama pemilik rekening");
        if (err != null) return err;

        if (rawAccountNumber == null || rawAccountNumber.trim().isEmpty()) {
            return "Nomor rekening / ID pembayaran wajib diisi.";
        }
        String normalized = normalizeAccountNumber(bankName, rawAccountNumber);
        boolean isQris = bankName.toLowerCase(Locale.ROOT).contains("qris");
        if (isQris) {
            if (!QRIS_ID_PATTERN.matcher(normalized).matches()) {
                return "ID / NMID QRIS tidak valid (minimal 5 karakter huruf/angka/strip).";
            }
            return null;
        }
        if (!BANK_ACC_PATTERN.matcher(normalized).matches() || normalized.replace("-", "").length() < 5) {
            return "Nomor rekening / VA harus berupa angka minimal 5 digit (boleh pakai tanda '-').";
        }
        return null;
    }

    public static String validateEventImagePath(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return "Foto acara wajib diunggah.";
        }
        return null;
    }

    public static String validateImageFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return "File gambar tidak ditemukan.";
        }
        if (!file.canRead()) {
            return "File gambar tidak dapat dibaca.";
        }
        long size = file.length();
        if (size <= 0) {
            return "File gambar kosong atau rusak.";
        }
        if (size > MAX_IMAGE_BYTES) {
            return "Ukuran file gambar maksimal 5 MB.";
        }
        String ext = extractExtension(file.getName());
        if (!ALLOWED_IMAGE_EXTS.contains(ext)) {
            return "Format file harus PNG, JPG, JPEG, WEBP, atau GIF.";
        }
        if (!hasValidImageMagicBytes(file)) {
            return "File bukan gambar yang valid atau isinya rusak.";
        }
        return null;
    }

    public static String extractExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot <= 0 || dot >= filename.length() - 1) return "";
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean hasValidImageMagicBytes(File file) {
        byte[] header = new byte[12];
        int read;
        try (FileInputStream fis = new FileInputStream(file)) {
            read = fis.read(header);
        } catch (IOException ex) {
            return false;
        }
        if (read < 4) return false;

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (read >= 8
                && (header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                && (header[4] & 0xFF) == 0x0D && (header[5] & 0xFF) == 0x0A
                && (header[6] & 0xFF) == 0x1A && (header[7] & 0xFF) == 0x0A) {
            return true;
        }
        // JPEG: FF D8 FF
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return true;
        }
        // GIF: GIF87a / GIF89a
        if (header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8') {
            return true;
        }
        // WEBP: RIFF....WEBP
        if (read >= 12
                && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return true;
        }
        return false;
    }
}
