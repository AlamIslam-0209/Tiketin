<?php
/**
 * Autentikasi dan Manajemen Sesi Pengguna
 */

if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

/**
 * Mendapatkan data pengguna yang sedang login
 *
 * @return array|null
 */
function current_user(): ?array
{
    return $_SESSION['user'] ?? null;
}

/**
 * Cek apakah ada pengguna yang sedang login
 *
 * @return bool
 */
function is_logged_in(): bool
{
    return isset($_SESSION['user']) && !empty($_SESSION['user']['id']);
}

/**
 * Mengarahkan ke halaman home default berdasarkan role pengguna
 *
 * @param string|null $role
 * @return string
 */
function home_for(?string $role): string
{
    if ($role === 'admin') {
        return 'admin.php';
    } elseif ($role === 'organizer') {
        return 'organizer.php';
    }
    return 'index.php#home';
}

/**
 * Memastikan pengguna sudah login; alihkan ke login.php jika belum
 *
 * @param string $redirectUrl
 * @return array
 */
function require_login(string $redirectUrl = 'login.php'): array
{
    if (!is_logged_in()) {
        set_flash('error', 'Silakan login terlebih dahulu untuk melanjutkan.');
        header("Location: {$redirectUrl}");
        exit;
    }
    return $_SESSION['user'];
}

/**
 * Memastikan pengguna memiliki salah satu role yang diizinkan
 *
 * @param string|array $allowedRoles
 * @param string|null $redirectUrl
 * @return array
 */
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

/**
 * Set pesan notifikasi flash (sukses, error, info)
 *
 * @param string $type ('success', 'error', 'info')
 * @param string $message
 * @return void
 */
function set_flash(string $type, string $message): void
{
    $_SESSION['flash'] = [
        'type'    => $type,
        'message' => $message,
    ];
}

/**
 * Ambil dan bersihkan pesan flash
 *
 * @return array|null
 */
function get_flash(): ?array
{
    if (isset($_SESSION['flash'])) {
        $flash = $_SESSION['flash'];
        unset($_SESSION['flash']);
        return $flash;
    }
    return null;
}

/**
 * Perbarui data sesi pengguna aktif dari database
 *
 * @param PDO $pdo
 * @param int $userId
 * @return void
 */
function refresh_user_session(PDO $pdo, int $userId): void
{
    $stmt = $pdo->prepare("SELECT id, username, role, full_name, organization FROM users WHERE id = ?");
    $stmt->execute([$userId]);
    $user = $stmt->fetch();
    if ($user) {
        $_SESSION['user'] = $user;
    }
}
