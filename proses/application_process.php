<?php
/**
 * Handler Pengajuan & Keputusan Calon Organizer
 */
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../includes/auth_check.php';
require_once __DIR__ . '/../includes/functions.php';

$pdo = get_db();
$currentUser = require_login('../login.php');

$action = $_GET['action'] ?? ($_POST['action'] ?? '');

if ($action === 'apply') {
    require_role('user', '../login.php');

    $fullName = trim($_POST['fullName'] ?? '');
    $contact = trim($_POST['contact'] ?? '');
    $organization = trim($_POST['organization'] ?? '');

    $eventName = trim($_POST['name'] ?? '');
    $eventDate = trim($_POST['date'] ?? '');
    $eventTime = trim($_POST['time'] ?? '19:00 WIB');
    $eventLocation = trim($_POST['location'] ?? 'Jakarta');
    $eventPrice = (int)($_POST['price'] ?? 0);
    $eventSlots = (int)($_POST['slots'] ?? 0);
    $eventCategory = trim($_POST['category'] ?? 'Konser');
    $eventDesc = trim($_POST['description'] ?? '');

    if (empty($fullName) || empty($contact) || empty($organization)) {
        set_flash('error', 'Lengkapi identitas diri, kontak, dan nama organisasi.');
        header('Location: ../apply.php');
        exit;
    }

    if (empty($eventName) || empty($eventDate) || $eventPrice < 0 || $eventSlots < 1) {
        set_flash('error', 'Lengkapi detail event perdana (kuota minimal 1 tiket).');
        header('Location: ../apply.php');
        exit;
    }

    try {
        // Cek pengajuan terakhir
        $checkStmt = $pdo->prepare("SELECT status FROM applications WHERE user_id = ? ORDER BY id DESC LIMIT 1");
        $checkStmt->execute([$currentUser['id']]);
        $prev = $checkStmt->fetch();

        if ($prev && $prev['status'] === 'pending') {
            set_flash('error', 'Anda sudah memiliki pengajuan yang sedang ditinjau.');
            header('Location: ../apply.php');
            exit;
        }

        $insertStmt = $pdo->prepare("
            INSERT INTO applications (user_id, full_name, contact, organization, event_name, event_date, event_time, event_location, event_price, event_slots, event_category, event_description, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending')
        ");
        $insertStmt->execute([
            $currentUser['id'],
            $fullName,
            $contact,
            $organization,
            $eventName,
            $eventDate,
            $eventTime ?: '19:00 WIB',
            $eventLocation ?: 'Jakarta',
            $eventPrice,
            $eventSlots,
            $eventCategory ?: 'Konser',
            $eventDesc
        ]);

        set_flash('success', 'Pengajuan organizer berhasil dikirim! Menunggu persetujuan admin.');
        header('Location: ../apply.php');
        exit;
    } catch (Exception $e) {
        set_flash('error', 'Gagal mengirimkan pengajuan: ' . $e->getMessage());
        header('Location: ../apply.php');
        exit;
    }

} elseif ($action === 'decide') {
    require_role('admin', '../login.php');

    $appId = (int)($_POST['application_id'] ?? 0);
    $decision = trim($_POST['status'] ?? '');
    $reason = trim($_POST['reason'] ?? '');

    if ($appId <= 0 || !in_array($decision, ['approved', 'rejected'], true)) {
        set_flash('error', 'Keputusan tidak valid.');
        header('Location: ../admin.php');
        exit;
    }

    if ($decision === 'rejected' && empty($reason)) {
        set_flash('error', 'Wajib menyertakan alasan penolakan.');
        header('Location: ../admin.php');
        exit;
    }

    try {
        $pdo->beginTransaction();

        $stmt = $pdo->prepare("SELECT * FROM applications WHERE id = ? FOR UPDATE");
        $stmt->execute([$appId]);
        $app = $stmt->fetch();

        if (!$app || $app['status'] !== 'pending') {
            throw new Exception('Pengajuan tidak ditemukan atau sudah diproses.');
        }

        if ($decision === 'approved') {
            // 1. Update status aplikasi
            $upApp = $pdo->prepare("UPDATE applications SET status = 'approved', reason = NULL WHERE id = ?");
            $upApp->execute([$appId]);

            // 2. Naikkan role pengguna ke organizer dan update nama/organisasi
            $upUser = $pdo->prepare("UPDATE users SET role = 'organizer', full_name = ?, organization = ? WHERE id = ?");
            $upUser->execute([$app['full_name'], $app['organization'], $app['user_id']]);

            // 3. Terbitkan event pertama
            $inEvt = $pdo->prepare("
                INSERT INTO events (owner_id, name, date, time, location, price, slots, category, description)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ");
            $inEvt->execute([
                $app['user_id'],
                $app['event_name'],
                $app['event_date'],
                $app['event_time'] ?: '19:00 WIB',
                $app['event_location'] ?: 'Jakarta',
                $app['event_price'],
                $app['event_slots'],
                $app['event_category'],
                $app['event_description']
            ]);

            $pdo->commit();
            set_flash('success', 'Pengajuan organisasi "' . $app['organization'] . '" disetujui! Role pengguna berhasil diubah dan event perdana telah tayang.');
        } else {
            // Rejected
            $upApp = $pdo->prepare("UPDATE applications SET status = 'rejected', reason = ? WHERE id = ?");
            $upApp->execute([$reason, $appId]);

            $pdo->commit();
            set_flash('info', 'Pengajuan organisasi "' . $app['organization'] . '" telah ditolak.');
        }

        header('Location: ../admin.php');
        exit;
    } catch (Exception $e) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        set_flash('error', 'Gagal memproses keputusan: ' . $e->getMessage());
        header('Location: ../admin.php');
        exit;
    }

} else {
    header('Location: ../admin.php');
    exit;
}
