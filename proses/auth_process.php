<?php
/**
 * Handler Proses Autentikasi (Login & Register)
 */
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../includes/auth_check.php';
require_once __DIR__ . '/../includes/functions.php';

$pdo = get_db();
$action = $_GET['action'] ?? ($_POST['action'] ?? '');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    header('Location: ../login.php');
    exit;
}

if ($action === 'login') {
    $username = trim($_POST['username'] ?? '');
    $password = $_POST['password'] ?? '';

    if (empty($username) || empty($password)) {
        set_flash('error', 'Silakan isi username dan password.');
        header('Location: ../login.php');
        exit;
    }

    try {
        $stmt = $pdo->prepare("SELECT * FROM users WHERE LOWER(username) = LOWER(?) LIMIT 1");
        $stmt->execute([$username]);
        $user = $stmt->fetch();

        // Verifikasi password (Teks polos sesuai kesepakatan pengujian PRD)
        if ($user && $user['password'] === $password) {
            $_SESSION['user'] = [
                'id'           => $user['id'],
                'username'     => $user['username'],
                'role'         => $user['role'],
                'full_name'    => $user['full_name'],
                'organization' => $user['organization']
            ];

            set_flash('success', 'Selamat datang kembali, ' . $user['full_name'] . '!');

            // Cek apakah ada redirect tujuan
            $target = $_POST['redirect'] ?? '';
            if (!empty($target) && strpos($target, 'http') === false) {
                header("Location: ../{$target}");
            } else {
                header("Location: ../" . home_for($user['role']));
            }
            exit;
        } else {
            set_flash('error', 'Username atau password salah.');
            header('Location: ../login.php');
            exit;
        }
    } catch (Exception $e) {
        set_flash('error', 'Terjadi kesalahan sistem: ' . $e->getMessage());
        header('Location: ../login.php');
        exit;
    }

} elseif ($action === 'register') {
    $fullName = trim($_POST['fullName'] ?? '');
    $username = trim($_POST['username'] ?? '');
    $password = $_POST['password'] ?? '';

    if (empty($fullName) || strlen($username) < 3 || strlen($password) < 6) {
        set_flash('error', 'Isi nama lengkap, username minimal 3 karakter, dan password minimal 6 karakter.');
        header('Location: ../register.php');
        exit;
    }

    try {
        // Cek duplikasi username
        $checkStmt = $pdo->prepare("SELECT id FROM users WHERE LOWER(username) = LOWER(?) LIMIT 1");
        $checkStmt->execute([$username]);
        if ($checkStmt->fetch()) {
            set_flash('error', 'Username sudah dipakai oleh pengguna lain.');
            header('Location: ../register.php');
            exit;
        }

        // Simpan user baru (role: user, plain text password)
        $insertStmt = $pdo->prepare("INSERT INTO users (username, password, role, full_name) VALUES (?, ?, 'user', ?)");
        $insertStmt->execute([$username, $password, $fullName]);
        $newUserId = (int)$pdo->lastInsertId();

        // Otomatis login
        $_SESSION['user'] = [
            'id'           => $newUserId,
            'username'     => $username,
            'role'         => 'user',
            'full_name'    => $fullName,
            'organization' => null
        ];

        set_flash('success', 'Pendaftaran berhasil! Selamat datang di Tiketin.');
        header('Location: ../index.php#home');
        exit;
    } catch (Exception $e) {
        set_flash('error', 'Gagal mendaftar: ' . $e->getMessage());
        header('Location: ../register.php');
        exit;
    }

} else {
    header('Location: ../login.php');
    exit;
}
