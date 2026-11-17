<?php
// Konfigurasi koneksi database Tiketin: pakai .env kalau tersedia, kalau nggak pakai default XAMPP

// Muat isi .env secara manual tanpa perlu library tambahan
$envFile = dirname(__DIR__) . '/.env';
if (file_exists($envFile)) {
    $lines = file($envFile, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
    foreach ($lines as $line) {
        $line = trim($line);
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
            putenv("{$key}={$value}");
            $_ENV[$key] = $value;
            $_SERVER[$key] = $value;
        }
    }
}

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
    // Password kosong di XAMPP dianggap valid, jadi ceknya dibedain dari false/null
    $pass = $_ENV['DB_PASS'] ?? getenv('DB_PASS');
    define('DB_PASS', $pass !== false && $pass !== null ? $pass : '');
}

// Pastikan kolom image_path sudah ada dan isi foto bawaan untuk data lama yang masih kosong
function ensure_event_image_schema(PDO $pdo): void
{
    static $ensured = false;
    if ($ensured) {
        return;
    }
    $ensured = true;

    try {
        foreach (['events', 'applications'] as $table) {
            $colStmt = $pdo->query("SHOW COLUMNS FROM `{$table}` LIKE 'image_path'");
            if (!$colStmt || !$colStmt->fetch()) {
                $pdo->exec("ALTER TABLE `{$table}` ADD COLUMN `image_path` VARCHAR(255) DEFAULT NULL");
            }
        }

        $uploadDir = dirname(__DIR__) . '/uploads/events/';
        $eventIdMap = [
            1  => 'event_01_senja_suara_indie.jpg',
            2  => 'event_02_ruang_bunyi_acoustic.jpg',
            3  => 'event_03_kepulauan_seribu.jpg',
            4  => 'event_04_kota_tua_batavia.jpg',
            5  => 'event_05_tech_creator_summit.jpg',
            6  => 'event_06_workshop_uiux.jpg',
            7  => 'event_07_jazz_tebing_breksi.jpg',
            8  => 'event_08_golden_sunrise_prau.jpg',
            9  => 'event_09_cyber_security.jpg',
            10 => 'event_10_simfoni_nusantara.jpg',
            15 => 'event_15_pernikahan_tradisional.jpg',
            21 => 'event_21_innoelectrica.jpg',
            22 => 'event_22_sumatra_esports.jpg',
        ];
        $keywordMap = [
            'senja suara'            => 'event_01_senja_suara_indie.jpg',
            'ruang bunyi'            => 'event_02_ruang_bunyi_acoustic.jpg',
            'kepulauan seribu'       => 'event_03_kepulauan_seribu.jpg',
            'kota tua'               => 'event_04_kota_tua_batavia.jpg',
            'tech creator'           => 'event_05_tech_creator_summit.jpg',
            'ui/ux'                  => 'event_06_workshop_uiux.jpg',
            'uiux'                   => 'event_06_workshop_uiux.jpg',
            'jazz'                   => 'event_07_jazz_tebing_breksi.jpg',
            'tebing breksi'          => 'event_07_jazz_tebing_breksi.jpg',
            'prau'                   => 'event_08_golden_sunrise_prau.jpg',
            'cyber security'         => 'event_09_cyber_security.jpg',
            'simfoni'                => 'event_10_simfoni_nusantara.jpg',
            'pernikahan tradisional' => 'event_15_pernikahan_tradisional.jpg',
            'innoelectrica'          => 'event_21_innoelectrica.jpg',
            'esports'                => 'event_22_sumatra_esports.jpg',
            'tari'                   => 'app_05_tari_riau.jpg',
            'selendang sutra'        => 'app_05_tari_riau.jpg',
            'audio mixing'           => 'app_16_audio_mixing.jpg',
        ];
        $appIdMap = [
            5  => 'app_05_tari_riau.jpg',
            16 => 'app_16_audio_mixing.jpg',
        ];

        $resolveMatch = static function (int $id, string $name, array $idMap) use ($keywordMap, $uploadDir): ?string {
            $lower = strtolower($name);
            foreach ($keywordMap as $kw => $file) {
                if (str_contains($lower, $kw) && is_file($uploadDir . $file)) {
                    return $file;
                }
            }
            if (isset($idMap[$id]) && is_file($uploadDir . $idMap[$id])) {
                return $idMap[$id];
            }
            return null;
        };

        $evtRows = $pdo->query("SELECT id, name FROM events WHERE image_path IS NULL OR TRIM(image_path) = ''")->fetchAll();
        if ($evtRows) {
            $upEvt = $pdo->prepare("UPDATE events SET image_path = ? WHERE id = ?");
            foreach ($evtRows as $row) {
                $match = $resolveMatch((int)$row['id'], (string)$row['name'], $eventIdMap);
                if ($match !== null) {
                    $upEvt->execute([$match, (int)$row['id']]);
                }
            }
        }

        $appRows = $pdo->query("SELECT id, event_name FROM applications WHERE image_path IS NULL OR TRIM(image_path) = ''")->fetchAll();
        if ($appRows) {
            $upApp = $pdo->prepare("UPDATE applications SET image_path = ? WHERE id = ?");
            foreach ($appRows as $row) {
                $match = $resolveMatch((int)$row['id'], (string)$row['event_name'], $appIdMap);
                if ($match !== null) {
                    $upApp->execute([$match, (int)$row['id']]);
                }
            }
        }
    } catch (Throwable $e) {
        // Biarin tetap lanjut kalau DB belum siap atau nggak ngizinin perubahan struktur tabel
    }
}

// Ambil koneksi PDO ke database (dibuat sekali saja per request)
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
            ensure_event_image_schema($pdo);
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
