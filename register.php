<?php
require_once __DIR__ . '/config/database.php';
require_once __DIR__ . '/includes/auth_check.php';

// Jika sudah login, langsung alihkan ke home
if (is_logged_in()) {
    header('Location: ' . home_for(current_user()['role']));
    exit;
}

$pageTitle = 'Daftar — Tiketin';
$activeNav = 'login';
require_once __DIR__ . '/includes/header.php';
?>

<main class="login-main container">
    <section class="login-card" aria-labelledby="registerTitle">
        <p class="eyebrow">Akun baru</p>
        <h1 id="registerTitle">Daftar ke Tiketin.</h1>
        <p class="login-description">Mulai sebagai user biasa. Kamu bisa mengajukan diri jadi organizer nanti.</p>
        
        <form id="registerForm" action="proses/auth_process.php?action=register" method="POST">
            <div class="login-field">
                <label for="fullName">Nama lengkap</label>
                <input id="fullName" name="fullName" autocomplete="name" required autofocus />
            </div>
            <div class="login-field">
                <label for="username">Username</label>
                <input id="username" name="username" autocomplete="username" minlength="3" required />
            </div>
            <div class="login-field">
                <label for="password">Password</label>
                <input id="password" name="password" type="password" autocomplete="new-password" minlength="6" required />
            </div>
            
            <button class="button button-submit" type="submit">Buat akun <span aria-hidden="true">→</span></button>
        </form>

        <p class="auth-switch">Sudah punya akun? <a href="login.php">Masuk di sini</a></p>
    </section>
</main>

<?php require_once __DIR__ . '/includes/footer.php'; ?>
