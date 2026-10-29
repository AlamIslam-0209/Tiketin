<?php
/**
 * Header Template Tiketin
 */
if (!defined('APP_ENV')) {
    require_once __DIR__ . '/../config/database.php';
}
require_once __DIR__ . '/auth_check.php';
require_once __DIR__ . '/functions.php';

$currentUser = current_user();
$pageTitle = $pageTitle ?? 'Tiketin — Temukan Event Favoritmu';
$activeNav = $activeNav ?? '';
$extraCss = $extraCss ?? [];

// Status pengajuan jika login sebagai user biasa
$userAppStatus = null;
if ($currentUser && $currentUser['role'] === 'user' && isset($pdo)) {
    try {
        $stmtApp = $pdo->prepare("SELECT status, reason FROM applications WHERE user_id = ? ORDER BY id DESC LIMIT 1");
        $stmtApp->execute([$currentUser['id']]);
        $userAppStatus = $stmtApp->fetch();
    } catch (Exception $e) {
        $userAppStatus = null;
    }
}

$flash = get_flash();
?>
<!doctype html>
<html lang="id">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <meta name="description" content="Tiketin — temukan dan pesan tiket event favoritmu secara mudah dan aman." />
    <title><?= htmlspecialchars($pageTitle) ?></title>
    <link rel="stylesheet" href="assets/css/style.css" />
    <?php foreach ($extraCss as $cssFile): ?>
        <link rel="stylesheet" href="assets/css/<?= htmlspecialchars(basename($cssFile)) ?>" />
    <?php endforeach; ?>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/gsap/3.15.0/gsap.min.js" defer></script>
    <script src="assets/js/animation.js" defer></script>
</head>
<body<?= isset($bodyDataDashboard) ? ' data-dashboard="' . htmlspecialchars($bodyDataDashboard) . '"' : '' ?>>
    <header class="site-header">
        <nav class="navbar container" aria-label="Navigasi utama">
            <a class="wordmark" href="index.php#home" aria-label="Tiketin, kembali ke Home">Tiketin</a>
            <div class="nav-links">
                <a class="nav-link <?= $activeNav === 'home' ? 'active' : '' ?>" href="index.php#home">Home</a>
                <a class="nav-link <?= $activeNav === 'tickets' ? 'active' : '' ?>" href="index.php#tickets">Ticket</a>
                <?php if ($currentUser && $currentUser['role'] !== 'admin'): ?>
                    <a class="nav-link <?= $activeNav === 'my-tickets' ? 'active' : '' ?>" href="tiket-saya.php">Tiket Saya</a>
                <?php endif; ?>

                <div class="account-slot" id="accountSlot">
                    <?php if (!$currentUser): ?>
                        <a class="nav-link <?= $activeNav === 'login' ? 'active' : '' ?>" href="login.php">Login</a>
                    <?php else: ?>
                        <button class="nav-link account-trigger" id="accountTrigger" type="button" aria-expanded="false" aria-controls="accountMenu">
                            Akun <span aria-hidden="true">⌄</span>
                        </button>
                        <div class="account-menu" id="accountMenu" hidden>
                            <strong id="accountName"><?= htmlspecialchars($currentUser['username']) ?></strong>
                            <span class="account-role" id="accountRole"><?= ucfirst($currentUser['role']) ?></span>
                            
                            <div id="accountStatus">
                                <?php if ($currentUser['role'] === 'user' && $userAppStatus): ?>
                                    <?php if ($userAppStatus['status'] === 'pending'): ?>
                                        <p style="font-size:0.85rem;color:var(--muted);margin:6px 0;">Pengajuan organizer menunggu persetujuan.</p>
                                    <?php elseif ($userAppStatus['status'] === 'rejected'): ?>
                                        <p style="font-size:0.85rem;color:#b91c1c;margin:6px 0;">Pengajuan ditolak: <?= htmlspecialchars($userAppStatus['reason']) ?></p>
                                    <?php endif; ?>
                                <?php endif; ?>
                            </div>

                            <div id="accountActions">
                                <?php if ($currentUser['role'] === 'user'): ?>
                                    <a class="account-action" href="apply.php">Ajukan jadi organizer</a>
                                <?php else: ?>
                                    <a class="account-action" href="<?= home_for($currentUser['role']) ?>">Dashboard <?= htmlspecialchars($currentUser['role']) ?></a>
                                <?php endif; ?>
                            </div>

                            <a class="account-action logout-action" id="logoutButton" href="logout.php">Logout</a>
                        </div>
                    <?php endif; ?>
                </div>
            </div>
        </nav>
    </header>

    <?php if ($flash): 
        $toastClass = 'toast-' . ($flash['type'] === 'error' ? 'error' : ($flash['type'] === 'success' ? 'success' : 'info'));
        $toastIcon = $flash['type'] === 'error' ? '⚠' : ($flash['type'] === 'success' ? '✓' : '✦');
    ?>
        <div class="toast is-visible <?= $toastClass ?>" id="toastFlash" role="alert">
            <span style="font-weight:700; font-size:1.05rem;"><?= $toastIcon ?></span>
            <span><?= htmlspecialchars($flash['message']) ?></span>
            <button type="button" onclick="document.getElementById('toastFlash').classList.remove('is-visible')" style="border:0; background:transparent; cursor:pointer; color:inherit; font-size:1.1rem; padding:0 0 0 6px; margin-left:auto; line-height:1;">×</button>
        </div>
        <script>
            setTimeout(() => {
                const toast = document.getElementById('toastFlash');
                if (toast) toast.classList.remove('is-visible');
            }, 4500);
        </script>
    <?php endif; ?>
