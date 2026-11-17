<?php
// Proses pengajuan calon organizer dan persetujuan admin
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

    $imgResult = ['uploaded_new' => false, 'filename' => null];
    try {
        $checkStmt = $pdo->prepare("SELECT status, image_path FROM applications WHERE user_id = ? ORDER BY id DESC LIMIT 1");
        $checkStmt->execute([$currentUser['id']]);
        $prev = $checkStmt->fetch();

        if ($prev && $prev['status'] === 'pending') {
            set_flash('error', 'Anda sudah memiliki pengajuan yang sedang ditinjau.');
            header('Location: ../apply.php');
            exit;
        }

        $prevImg = ($prev && !empty($prev['image_path']) && resolve_event_image_url($prev['image_path']) !== null)
            ? basename(trim((string)$prev['image_path']))
            : null;
        $imgResult = handle_event_image_upload('event_image', 'app', $prevImg === null, $prevImg);
        if (!$imgResult['ok']) {
            set_flash('error', $imgResult['error']);
            header('Location: ../apply.php');
            exit;
        }

        $insertStmt = $pdo->prepare("
            INSERT INTO applications (user_id, full_name, contact, organization, event_name, event_date, event_time, event_location, event_price, event_slots, event_category, event_description, image_path, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending')
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
            $eventDesc,
            $imgResult['filename']
        ]);

        set_flash('success', 'Pengajuan organizer berhasil dikirim! Menunggu persetujuan admin.');
        header('Location: ../apply.php');
        exit;
    } catch (Exception $e) {
        if (!empty($imgResult['uploaded_new']) && !empty($imgResult['filename'])) {
            @unlink(__DIR__ . '/../uploads/events/' . basename($imgResult['filename']));
        }
        set_flash('error', 'Gagal mengirimkan pengajuan: ' . $e->getMessage());
        header('Location: ../apply.php');
        exit;
    }

} elseif ($action === 'decide') {
    require_role('admin', '../login.php');

    $appId = filter_var($_POST['application_id'] ?? '', FILTER_VALIDATE_INT) ?: 0;
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
            // Sekalian ubah status pengajuan, naikkan role user, dan terbitkan event perdananya
            $upApp = $pdo->prepare("UPDATE applications SET status = 'approved', reason = NULL WHERE id = ?");
            $upApp->execute([$appId]);

            $upUser = $pdo->prepare("UPDATE users SET role = 'organizer', full_name = ?, organization = ? WHERE id = ?");
            $upUser->execute([$app['full_name'], $app['organization'], $app['user_id']]);

            $inEvt = $pdo->prepare("
                INSERT INTO events (owner_id, name, date, time, location, price, slots, category, description, image_path)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                $app['event_description'],
                $app['image_path'] ?? null
            ]);

            $pdo->commit();
            set_flash('success', 'Pengajuan organisasi "' . $app['organization'] . '" disetujui! Role pengguna berhasil diubah dan event perdana telah tayang.');
        } else {
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
