<?php
/**
 * Handler CRUD Rekening & Metode Pembayaran Organizer (Tabel `payment` sesuai ERD)
 */
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../includes/auth_check.php';
require_once __DIR__ . '/../includes/functions.php';

$pdo = get_db();
$currentUser = require_login('../login.php');
require_role('organizer', '../login.php');

$action = $_GET['action'] ?? ($_POST['action'] ?? '');

if ($action === 'add') {
    $bankName = trim($_POST['bank_name'] ?? '');
    $customBank = trim($_POST['custom_bank'] ?? '');
    if ($bankName === 'Lainnya' && !empty($customBank)) {
        $bankName = $customBank;
    }

    $accountNumber = preg_replace('/[^0-9\-]/', '', trim($_POST['account_number'] ?? ''));
    $accountName = trim($_POST['account_name'] ?? '');

    if (empty($bankName) || empty($accountNumber) || empty($accountName)) {
        set_flash('error', 'Lengkapi nama bank, nomor rekening, dan nama pemilik rekening.');
        header('Location: ../organizer.php#rekeningSection');
        exit;
    }

    try {
        $stmt = $pdo->prepare("
            INSERT INTO payment (user_id, bank_name, account_number, account_name, is_active)
            VALUES (?, ?, ?, ?, 1)
        ");
        $stmt->execute([
            $currentUser['id'],
            $bankName,
            $accountNumber,
            $accountName
        ]);

        set_flash('success', "Rekening {$bankName} ({$accountNumber}) berhasil ditambahkan!");
        header('Location: ../organizer.php#rekeningSection');
        exit;
    } catch (Exception $e) {
        set_flash('error', 'Gagal menyimpan rekening: ' . $e->getMessage());
        header('Location: ../organizer.php#rekeningSection');
        exit;
    }

} elseif ($action === 'delete') {
    $paymentId = (int)($_GET['id'] ?? ($_POST['id'] ?? 0));

    if ($paymentId <= 0) {
        set_flash('error', 'ID rekening tidak valid.');
        header('Location: ../organizer.php#rekeningSection');
        exit;
    }

    try {
        $stmt = $pdo->prepare("DELETE FROM payment WHERE id = ? AND user_id = ?");
        $stmt->execute([$paymentId, $currentUser['id']]);

        if ($stmt->rowCount() > 0) {
            set_flash('info', 'Rekening pembayaran berhasil dihapus.');
        } else {
            set_flash('error', 'Rekening tidak ditemukan atau Anda tidak memiliki akses.');
        }

        header('Location: ../organizer.php#rekeningSection');
        exit;
    } catch (Exception $e) {
        set_flash('error', 'Gagal menghapus rekening: ' . $e->getMessage());
        header('Location: ../organizer.php#rekeningSection');
        exit;
    }

} else {
    header('Location: ../organizer.php');
    exit;
}
