<?php
require_once __DIR__ . '/includes/auth_check.php';

// Bersihin session dan cookie lama biar status login benar-benar hilang
$_SESSION = [];
if (ini_get("session.use_cookies")) {
    $params = session_get_cookie_params();
    setcookie(session_name(), '', time() - 42000,
        $params["path"], $params["domain"],
        $params["secure"], $params["httponly"]
    );
}
session_destroy();

// Buka session baru khusus untuk nampung pesan flash logout
session_start();
set_flash('info', 'Anda telah berhasil keluar dari akun.');
header('Location: login.php');
exit;
