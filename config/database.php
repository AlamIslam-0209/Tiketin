<?php
/**
 * Konfigurasi Koneksi Database Tiketin (Mendukung .env & Standar XAMPP)
 * 
 * Sistem akan otomatis membaca file `.env` di root proyek jika tersedia.
 * Jika file `.env` belum dibuat, sistem otomatis menggunakan default lokal XAMPP.
 */

// 1. Loader File .env Ringan & Otomatis (Tanpa dependensi Composer eksternal)
$envFile = dirname(__DIR__) . '/.env';
if (file_exists($envFile)) {
    $lines = file($envFile, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
    foreach ($lines as $line) {
        $line = trim($line);
        // Lewati baris kosong atau baris komentar (#)
        if ($line === '' || str_starts_with($line, '#')) {
            continue;
        }
        if (str_contains($line, '=')) {
            [$key, $value] = explode('=', $line, 2);
            $key = trim($key);
            $value = trim($value);
            if ((str_starts_with($value, '"') && str_ends_with($value, '"')) ||
                (str_starts_with($value, "'") && str_ends_with($value, "'"))) {
                $value = substr($value, 1, -1);
            }
            // Daftarkan ke environment
            putenv("{$key}={$value}");
            $_ENV[$key] = $value;
            $_SERVER[$key] = $value;
        }
    }
}

// 2. Definisi Konstanta Database dengan Fallback Aman
if (!defined('APP_ENV')) {
    define('APP_ENV', $_ENV['APP_ENV'] ?? (getenv('APP_ENV') ?: 'local'));
}

if (!defined('DB_HOST')) {
    define('DB_HOST', $_ENV['DB_HOST'] ?? (getenv('DB_HOST') ?: '127.0.0.1'));
}

if (!defined('DB_PORT')) {
    define('DB_PORT', $_ENV['DB_PORT'] ?? (getenv('DB_PORT') ?: '3306'));
}

if (!defined('DB_NAME')) {
    define('DB_NAME', $_ENV['DB_NAME'] ?? (getenv('DB_NAME') ?: 'tiketin_db2'));
}

if (!defined('DB_USER')) {
    define('DB_USER', $_ENV['DB_USER'] ?? (getenv('DB_USER') ?: 'root'));
}

if (!defined('DB_PASS')) {
    // Password kosong ('') adalah nilai valid di XAMPP, gunakan pengecekan khusus
    $pass = $_ENV['DB_PASS'] ?? getenv('DB_PASS');
    define('DB_PASS', $pass !== false && $pass !== null ? $pass : '');
}

/**
 * Mendapatkan koneksi PDO ke database
 *
 * @return PDO
 */
function get_db(): PDO
{
    static $pdo = null;

    if ($pdo === null) {
        $dsn = sprintf('mysql:host=%s;port=%s;dbname=%s;charset=utf8mb4', DB_HOST, DB_PORT, DB_NAME);
        $options = [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES   => false,
        ];

        try {
            $pdo = new PDO($dsn, DB_USER, DB_PASS, $options);
        } catch (PDOException $e) {
            $msg = "Gagal terhubung ke database MySQL (" . APP_ENV . "): " . $e->getMessage();
            $msg .= "\nPastikan MySQL di XAMPP Control Panel sudah dalam status START dan database '" . DB_NAME . "' sudah dibuat.";
            $msg .= "\nTips: Anda dapat menyesuaikan konfigurasi database di file .env.";

            if (php_sapi_name() === 'cli') {
                throw new Exception($msg);
            } else {
                die("<div style='font-family:sans-serif;padding:20px;background:#fee2e2;border:1px solid #ef4444;color:#991b1b;border-radius:8px;max-width:600px;margin:40px auto;line-height:1.5;'>
                    <h3 style='margin-top:0;'>Terjadi Kesalahan Koneksi Database</h3>
                    <p style='white-space:pre-line;'>" . htmlspecialchars($msg) . "</p>
                </div>");
            }
        }
    }

    return $pdo;
}

try {
    $pdo = get_db();
} catch (Exception $e) {
    $pdo = null;
}
