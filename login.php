<?php
require_once __DIR__ . '/config/database.php';
require_once __DIR__ . '/includes/auth_check.php';

// Jika sudah login, langsung alihkan ke dashboard/home yang sesuai
if (is_logged_in()) {
    header('Location: ' . home_for(current_user()['role']));
    exit;
}

$pageTitle = 'Login — Tiketin';
$activeNav = 'login';
require_once __DIR__ . '/includes/header.php';
?>

<main class="login-main container">
    <section class="login-card" aria-labelledby="loginTitle">
        <p class="eyebrow">Selamat datang kembali</p>
        <h1 id="loginTitle">Masuk ke Tiketin.</h1>
        <p class="login-description">Masuk untuk melanjutkan aktivitasmu di Tiketin.</p>
        
        <form id="loginForm" action="proses/auth_process.php?action=login" method="POST">
            <div class="login-field">
                <label for="username">Username</label>
                <input id="username" name="username" type="text" autocomplete="username" required autofocus />
            </div>
            <div class="login-field">
                <label for="password">Password</label>
                <input id="password" name="password" type="password" autocomplete="current-password" required />
            </div>
            
            <button class="button button-submit" type="submit">Masuk <span aria-hidden="true">→</span></button>
        </form>

        <p class="auth-switch">Belum punya akun? <a href="register.php">Daftar sekarang</a></p>

        <!-- Informasi Akun Demo untuk Pengujian Cepat -->
        <details class="demo-accounts" open>
            <summary>Akun Demo Tersedia</summary>
            <p>
                • <strong>Admin</strong>: <code>admin</code> / <code>admin123</code><br>
                • <strong>Organizer</strong>: <code>senjasuara</code> / <code>organizer123</code><br>
                • <strong>User</strong>: <code>pengunjung</code> / <code>user123</code>
            </p>
        </details>
    </section>
</main>

<?php require_once __DIR__ . '/includes/footer.php'; ?>
