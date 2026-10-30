<?php
/**
 * Helper Utility Functions untuk Tiketin
 */

/**
 * Format angka ke format mata uang Rupiah (contoh: Rp 175.000)
 *
 * @param int|float $amount
 * @return string
 */
function format_rupiah($amount): string
{
    return 'Rp ' . number_format((float)$amount, 0, ',', '.');
}

/**
 * Format tanggal Y-m-d ke format bahasa Indonesia (contoh: 12 Okt 2026)
 *
 * @param string $dateStr (Format: YYYY-MM-DD)
 * @param bool $shortMonth
 * @return string
 */
function format_tanggal_id(?string $dateStr, bool $shortMonth = true): string
{
    if (!$dateStr) return '-';
    
    $timestamp = strtotime($dateStr);
    if (!$timestamp) return $dateStr;

    $day = date('j', $timestamp);
    $monthIndex = (int)date('n', $timestamp);
    $year = date('Y', $timestamp);

    $monthsShort = [
        1 => 'Jan', 2 => 'Feb', 3 => 'Mar', 4 => 'Apr', 5 => 'Mei', 6 => 'Jun',
        7 => 'Jul', 8 => 'Agu', 9 => 'Sep', 10 => 'Okt', 11 => 'Nov', 12 => 'Des'
    ];

    $monthsLong = [
        1 => 'Januari', 2 => 'Februari', 3 => 'Maret', 4 => 'April', 5 => 'Mei', 6 => 'Juni',
        7 => 'Juli', 8 => 'Agustus', 9 => 'September', 10 => 'Oktober', 11 => 'November', 12 => 'Desember'
    ];

    $monthName = $shortMonth ? ($monthsShort[$monthIndex] ?? '') : ($monthsLong[$monthIndex] ?? '');

    return "{$day} {$monthName} {$year}";
}

/**
 * Sanitasi string input dari potensi XSS
 *
 * @param mixed $data
 * @return string
 */
function sanitize($data): string
{
    return htmlspecialchars(trim((string)$data), ENT_QUOTES, 'UTF-8');
}

/**
 * Buat kode pesanan acak yang unik (contoh: TKT-20261022-8F9B)
 *
 * @return string
 */
function generate_order_code(): string
{
    return 'TKT-' . date('Ymd') . '-' . strtoupper(substr(bin2hex(random_bytes(4)), 0, 6));
}

/**
 * Label metode pembayaran yang ramah pengguna
 *
 * @param string $methodKey
 * @return string
 */
function get_payment_method_label(?string $methodKey): string
{
    $labels = [
        'qris'    => 'QRIS',
        'bca'     => 'Bank BCA',
        'bri'     => 'Bank BRI',
        'bsi'     => 'Bank BSI',
        'mandiri' => 'Bank Mandiri',
    ];

    return $labels[strtolower((string)$methodKey)] ?? ($methodKey ?: '-');
}
