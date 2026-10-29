<?php
/**
 * Handler CRUD Event & Pembaruan Status Order (Organizer & Admin)
 */
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../includes/auth_check.php';
require_once __DIR__ . '/../includes/functions.php';

$pdo = get_db();
$currentUser = require_login('../login.php');

$action = $_GET['action'] ?? ($_POST['action'] ?? '');
$redirectTarget = $currentUser['role'] === 'admin' ? '../admin.php' : '../organizer.php';

if ($action === 'create') {
    require_role('organizer', '../login.php');

    $name = trim($_POST['name'] ?? '');
    $date = trim($_POST['date'] ?? '');
    $time = trim($_POST['time'] ?? '19:00 WIB');
    $location = trim($_POST['location'] ?? 'Jakarta');
    $price = (int)($_POST['price'] ?? 0);
    $slots = (int)($_POST['slots'] ?? 0);
    $category = trim($_POST['category'] ?? 'Konser');
    $description = trim($_POST['description'] ?? '');

    if (empty($name) || empty($date) || $price < 0 || $slots < 0) {
        set_flash('error', 'Lengkapi seluruh data event dengan harga dan slot yang valid.');
        header('Location: ../organizer.php');
        exit;
    }

    try {
        $stmt = $pdo->prepare("
            INSERT INTO events (owner_id, name, date, time, location, price, slots, category, description)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ");
        $stmt->execute([
            $currentUser['id'],
            $name,
            $date,
            $time ?: '19:00 WIB',
            $location ?: 'Jakarta',
            $price,
            $slots,
            $category ?: 'Konser',
            $description
        ]);

        set_flash('success', 'Event "' . $name . '" berhasil diterbitkan!');
        header('Location: ../organizer.php');
        exit;
    } catch (Exception $e) {
        set_flash('error', 'Gagal menambahkan event: ' . $e->getMessage());
        header('Location: ../organizer.php');
        exit;
    }

} elseif ($action === 'update') {
    $eventId = (int)($_POST['event_id'] ?? 0);
    $name = trim($_POST['name'] ?? '');
    $date = trim($_POST['date'] ?? '');
    $time = trim($_POST['time'] ?? '19:00 WIB');
    $location = trim($_POST['location'] ?? 'Jakarta');
    $price = (int)($_POST['price'] ?? 0);
    $slots = (int)($_POST['slots'] ?? 0);
    $category = trim($_POST['category'] ?? 'Konser');
    $description = trim($_POST['description'] ?? '');

    if ($eventId <= 0 || empty($name) || empty($date) || $price < 0 || $slots < 0) {
        set_flash('error', 'Data perubahan event tidak lengkap atau tidak valid.');
        header("Location: {$redirectTarget}");
        exit;
    }

    try {
        // Cek hak akses event
        $stmt = $pdo->prepare("SELECT * FROM events WHERE id = ?");
        $stmt->execute([$eventId]);
        $event = $stmt->fetch();

        if (!$event) {
            set_flash('error', 'Event tidak ditemukan.');
            header("Location: {$redirectTarget}");
            exit;
        }

        if ($currentUser['role'] !== 'admin' && (int)$event['owner_id'] !== (int)$currentUser['id']) {
            set_flash('error', 'Anda tidak memiliki wewenang untuk mengubah event ini.');
            header("Location: {$redirectTarget}");
            exit;
        }

        $update = $pdo->prepare("
            UPDATE events 
            SET name = ?, date = ?, time = ?, location = ?, price = ?, slots = ?, category = ?, description = ?
            WHERE id = ?
        ");
        $update->execute([
            $name, 
            $date, 
            $time ?: '19:00 WIB', 
            $location ?: 'Jakarta', 
            $price, 
            $slots, 
            $category, 
            $description, 
            $eventId
        ]);

        set_flash('success', 'Perubahan event "' . $name . '" berhasil disimpan!');
        header("Location: {$redirectTarget}");
        exit;
    } catch (Exception $e) {
        set_flash('error', 'Gagal menyimpan perubahan event: ' . $e->getMessage());
        header("Location: {$redirectTarget}");
        exit;
    }

} elseif ($action === 'delete') {
    require_role('organizer', '../login.php');

    $eventId = (int)($_GET['id'] ?? ($_POST['event_id'] ?? 0));

    if ($eventId <= 0) {
        set_flash('error', 'ID event tidak valid.');
        header('Location: ../organizer.php');
        exit;
    }

    try {
        $stmt = $pdo->prepare("SELECT * FROM events WHERE id = ? AND owner_id = ?");
        $stmt->execute([$eventId, $currentUser['id']]);
        $event = $stmt->fetch();

        if (!$event) {
            set_flash('error', 'Hanya organizer pemilik yang dapat menghapus event ini.');
            header('Location: ../organizer.php');
            exit;
        }

        $del = $pdo->prepare("DELETE FROM events WHERE id = ?");
        $del->execute([$eventId]);

        set_flash('success', 'Event "' . $event['name'] . '" telah berhasil dihapus.');
        header('Location: ../organizer.php');
        exit;
    } catch (Exception $e) {
        set_flash('error', 'Gagal menghapus event: ' . $e->getMessage());
        header('Location: ../organizer.php');
        exit;
    }

} elseif ($action === 'update_order_status') {
    require_role('organizer', '../login.php');

    $orderId = (int)($_POST['order_id'] ?? 0);
    $newStatus = trim($_POST['status'] ?? '');

    if ($orderId <= 0 || !in_array($newStatus, ['berhasil', 'dibatalkan'], true)) {
        set_flash('error', 'Status atau ID pesanan tidak valid.');
        header('Location: ../organizer.php');
        exit;
    }

    try {
        $pdo->beginTransaction();

        // Verifikasi kepemilikan event terkait order
        $stmt = $pdo->prepare("
            SELECT o.*, e.owner_id, e.id AS evt_id 
            FROM orders o 
            JOIN events e ON o.event_id = e.id 
            WHERE o.id = ? 
            FOR UPDATE
        ");
        $stmt->execute([$orderId]);
        $order = $stmt->fetch();

        if (!$order || (int)$order['owner_id'] !== (int)$currentUser['id']) {
            throw new Exception('Anda tidak memiliki wewenang mengelola order ini.');
        }

        // Jika dibatalkan dan status sebelumnya bukan dibatalkan, kembalikan kuota tiket
        if ($newStatus === 'dibatalkan' && $order['status'] !== 'dibatalkan') {
            $restore = $pdo->prepare("UPDATE events SET slots = slots + ? WHERE id = ?");
            $restore->execute([$order['quantity'], $order['evt_id']]);
        }

        $update = $pdo->prepare("UPDATE orders SET status = ? WHERE id = ?");
        $update->execute([$newStatus, $orderId]);

        // Perbarui status seluruh tiket terkait di tabel 'tickets'
        $ticketStatus = ($newStatus === 'dibatalkan') ? 'cancelled' : 'valid';
        $updateTickets = $pdo->prepare("UPDATE tickets SET status = ? WHERE order_id = ?");
        $updateTickets->execute([$ticketStatus, $orderId]);

        $pdo->commit();

        $statusLabel = $newStatus === 'berhasil' ? 'diterima' : 'ditolak/dibatalkan';
        set_flash('success', 'Pesanan #' . $order['order_code'] . ' telah ' . $statusLabel . '.');
        header('Location: ../organizer.php');
        exit;
    } catch (Exception $e) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        set_flash('error', 'Gagal memperbarui status order: ' . $e->getMessage());
        header('Location: ../organizer.php');
        exit;
    }

} else {
    header("Location: {$redirectTarget}");
    exit;
}
