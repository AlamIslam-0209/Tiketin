<?php
// Helper autentikasi, hak akses role, dan pesan flash berbasis session

if (session_status() === PHP_SESSION_NONE) {
    $isHttps = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off')
        || (($_SERVER['SERVER_PORT'] ?? null) == 443);
    session_set_cookie_params([
        'lifetime' => 0,
        'path'     => '/',
        'domain'   => '',
        'secure'   => $isHttps,
        'httponly' => true,
        'samesite' => 'Lax',
    ]);
    session_start();
}

// Tolak request POST lintas origin biar aman dari serangan CSRF
function verify_same_origin(): void
{
    if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
        return;
    }

    $httpHost = $_SERVER['HTTP_HOST'] ?? '';
    if ($httpHost === '') {
        return;
    }

    $sourceHeader = $_SERVER['HTTP_ORIGIN'] ?? ($_SERVER['HTTP_REFERER'] ?? '');
    if ($sourceHeader === '') {
        return;
    }

    $parsedSource = parse_url($sourceHeader);
    $sourceHost = strtolower((string)($parsedSource['host'] ?? ''));
    if ($sourceHost === '') {
        http_response_code(403);
        die('Akses ditolak: header Origin/Referer tidak valid.');
    }

    $sourceAuthority = $sourceHost . (isset($parsedSource['port']) ? ':' . $parsedSource['port'] : '');
    $parsedExpected = parse_url('http://' . $httpHost);
    $expectedHost = strtolower((string)($parsedExpected['host'] ?? ''));
    $expectedAuthority = $expectedHost . (isset($parsedExpected['port']) ? ':' . $parsedExpected['port'] : '');

    if (!hash_equals($expectedAuthority, $sourceAuthority)) {
        http_response_code(403);
        die('Akses ditolak: permintaan lintas-origin tidak diizinkan (CSRF protection).');
    }
}

function current_user(): ?array
{
    return $_SESSION['user'] ?? null;
}

function is_logged_in(): bool
{
    return isset($_SESSION['user']) && !empty($_SESSION['user']['id']);
}

// Tentukan halaman tujuan utama berdasarkan role user
function home_for(?string $role): string
{
    if ($role === 'admin') {
        return 'admin.php';
    } elseif ($role === 'organizer') {
        return 'organizer.php';
    }
    return 'index.php#home';
}

// Tendang ke halaman login kalau user belum masuk
function require_login(string $redirectUrl = 'login.php'): array
{
    if (!is_logged_in()) {
        set_flash('error', 'Silakan login terlebih dahulu untuk melanjutkan.');
        header("Location: {$redirectUrl}");
        exit;
    }
    return $_SESSION['user'];
}

// Batasi akses halaman hanya untuk role tertentu
function require_role($allowedRoles, ?string $redirectUrl = null): array
{
    $user = require_login('login.php');
    $roles = is_array($allowedRoles) ? $allowedRoles : [$allowedRoles];

    if (!in_array($user['role'], $roles, true)) {
        set_flash('error', 'Anda tidak memiliki hak akses ke halaman tersebut.');
        $target = $redirectUrl ?: home_for($user['role']);
        header("Location: {$target}");
        exit;
    }

    return $user;
}

// Simpan notifikasi sementara yang hanya tampil sekali setelah redirect
function set_flash(string $type, string $message): void
{
    $_SESSION['flash'] = [
        'type'    => $type,
        'message' => $message,
    ];
}

function get_flash(): ?array
{
    if (isset($_SESSION['flash'])) {
        $flash = $_SESSION['flash'];
        unset($_SESSION['flash']);
        return $flash;
    }
    return null;
}

// Sinkronkan ulang data session user dari database setelah ada perubahan profil/role
function refresh_user_session(PDO $pdo, int $userId): void
{
    try {
        $stmt = $pdo->prepare("SELECT id, username, role, full_name, organization FROM users WHERE id = ?");
        $stmt->execute([$userId]);
        $user = $stmt->fetch();
        if ($user) {
            $_SESSION['user'] = $user;
        } else {
            unset($_SESSION['user']);
        }
    } catch (Exception $e) {
        // Biarkan state session lama kalau query gagal sementara
    }
}

verify_same_origin();

if (is_logged_in() && isset($GLOBALS['pdo']) && $GLOBALS['pdo'] instanceof PDO) {
    refresh_user_session($GLOBALS['pdo'], (int)$_SESSION['user']['id']);
}
