<?php
// Proses pemesanan tiket dan upload bukti transfer
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../includes/auth_check.php';
require_once __DIR__ . '/../includes/functions.php';

$pdo = get_db();
$user = require_login('../login.php');

if ($user['role'] === 'admin') {
    set_flash('error', 'Admin tidak dapat melakukan pemesanan tiket.');
    header('Location: ../index.php#tickets');
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    header('Location: ../index.php#tickets');
    exit;
}

$eventId = filter_var($_POST['event_id'] ?? '', FILTER_VALIDATE_INT) ?: 0;
$rawQty = filter_var($_POST['quantity'] ?? 1, FILTER_VALIDATE_INT);
$quantity = $rawQty !== false ? $rawQty : 0;
$paymentMethod = trim($_POST['payment_method'] ?? '');

if ($eventId <= 0 || $quantity < 1 || $quantity > 10) {
    set_flash('error', 'Data pemesanan tidak valid (kuantitas maksimal 10 tiket).');
    header('Location: ../index.php#tickets');
    exit;
}

$stmtEvtOwner = $pdo->prepare("SELECT owner_id FROM events WHERE id = ?");
$stmtEvtOwner->execute([$eventId]);
$evtOwnerRow = $stmtEvtOwner->fetch();
if (!$evtOwnerRow) {
    set_flash('error', 'Event tidak ditemukan.');
    header('Location: ../index.php#tickets');
    exit;
}

$paymentMethodName = 'Transfer Bank';
if (strtolower($paymentMethod) === 'qris') {
    $paymentMethodName = 'QRIS';
} elseif (str_starts_with($paymentMethod, 'bank_')) {
    $payId = filter_var(substr($paymentMethod, 5), FILTER_VALIDATE_INT) ?: 0;
    $stmtP = $pdo->prepare("SELECT bank_name FROM payment WHERE id = ? AND user_id = ? AND is_active = 1");
    $stmtP->execute([$payId, $evtOwnerRow['owner_id']]);
    $payRow = $stmtP->fetch();
    if (!$payRow) {
        set_flash('error', 'Rekening pembayaran tidak valid untuk event ini.');
        header('Location: ../index.php#tickets');
        exit;
    }
    $paymentMethodName = $payRow['bank_name'];
} elseif (in_array(strtolower($paymentMethod), ['bca', 'bri', 'bsi', 'mandiri'], true)) {
    $paymentMethodName = get_payment_method_label($paymentMethod);
} else {
    set_flash('error', 'Metode pembayaran tidak valid.');
    header('Location: ../index.php#tickets');
    exit;
}

$proofFilename = null;
if (!isset($_FILES['payment_proof']) || $_FILES['payment_proof']['error'] !== UPLOAD_ERR_OK) {
    $errCode = $_FILES['payment_proof']['error'] ?? 'missing';
    $errMsg = 'Harap unggah bukti pembayaran transfer yang valid.';
    if ($errCode === UPLOAD_ERR_INI_SIZE || $errCode === UPLOAD_ERR_FORM_SIZE) {
        $errMsg = 'Ukuran file gambar bukti transfer melebihi batas maksimal server.';
    } elseif ($errCode === UPLOAD_ERR_NO_FILE) {
        $errMsg = 'File bukti transfer belum dipilih. Silakan pilih foto/screenshot bukti transfer.';
    }
    set_flash('error', $errMsg);
    header('Location: ../index.php#tickets');
    exit;
}

$file = $_FILES['payment_proof'];
$ext = strtolower(pathinfo($file['name'], PATHINFO_EXTENSION));
$allowedExts = ['jpg', 'jpeg', 'png', 'webp', 'gif'];

if (!in_array($ext, $allowedExts, true)) {
    set_flash('error', 'Format file bukti pembayaran harus gambar (JPG, PNG, atau WEBP).');
    header('Location: ../index.php#tickets');
    exit;
}

if (function_exists('finfo_open')) {
    $finfo = @finfo_open(FILEINFO_MIME_TYPE);
    if ($finfo) {
        $mimeType = @finfo_file($finfo, $file['tmp_name']);
        @finfo_close($finfo);
        $allowedMimes = ['image/jpeg', 'image/pjpeg', 'image/png', 'image/x-png', 'image/webp', 'image/gif'];
        if ($mimeType && !in_array($mimeType, $allowedMimes, true)) {
            set_flash('error', 'Tipe file tidak valid. Harap unggah file foto/screenshot bukti transfer.');
            header('Location: ../index.php#tickets');
            exit;
        }
    }
}

if (@getimagesize($file['tmp_name']) === false) {
    set_flash('error', 'File bukti pembayaran bukan gambar yang valid.');
    header('Location: ../index.php#tickets');
    exit;
}

if ($file['size'] > 5 * 1024 * 1024) {
    set_flash('error', 'Ukuran file bukti pembayaran maksimal 5 MB.');
    header('Location: ../index.php#tickets');
    exit;
}

$proofFilename = 'proof_' . date('Ymd_His') . '_' . bin2hex(random_bytes(4)) . '.' . $ext;
$uploadDir = __DIR__ . '/../uploads/proofs/';
if (!is_dir($uploadDir)) {
    @mkdir($uploadDir, 0755, true);
}
@chmod($uploadDir, 0755);
$targetPath = $uploadDir . $proofFilename;

if (!move_uploaded_file($file['tmp_name'], $targetPath)) {
    set_flash('error', 'Gagal menyimpan file bukti pembayaran di server. Periksa hak akses folder uploads.');
    header('Location: ../index.php#tickets');
    exit;
}
@chmod($targetPath, 0644);

try {
    $pdo->beginTransaction();

    // Kunci baris event dulu biar stok tiket aman dari double booking
    $stmtEvent = $pdo->prepare("SELECT id, name, date, time, price, slots FROM events WHERE id = ? FOR UPDATE");
    $stmtEvent->execute([$eventId]);
    $event = $stmtEvent->fetch();

    if (!$event) {
        throw new Exception('Event tidak ditemukan.');
    }

    if (is_event_passed($event['date'] ?? null, $event['time'] ?? null)) {
        throw new Exception('Acara sudah berakhir dan tiket tidak lagi tersedia.');
    }

    if ($event['slots'] < $quantity) {
        throw new Exception('Slot tiket tidak mencukupi (sisa slot: ' . $event['slots'] . ').');
    }

    $updateStmt = $pdo->prepare("UPDATE events SET slots = slots - ? WHERE id = ?");
    $updateStmt->execute([$quantity, $eventId]);

    $orderCode = generate_order_code();
    $totalPrice = (int)$event['price'] * $quantity;

    $orderStmt = $pdo->prepare("
        INSERT INTO orders (order_code, user_id, event_id, quantity, unit_price, total_price, payment_method, payment_proof, status)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'menunggu')
    ");
    $orderStmt->execute([
        $orderCode,
        $user['id'],
        $eventId,
        $quantity,
        $event['price'],
        $totalPrice,
        $paymentMethodName,
        $proofFilename
    ]);
    $newOrderId = (int)$pdo->lastInsertId();

    $ticketStmt = $pdo->prepare("
        INSERT INTO tickets (order_id, event_id, ticket_code, purchase_price, status)
        VALUES (?, ?, ?, ?, 'valid')
    ");
    for ($i = 1; $i <= $quantity; $i++) {
        $singleTicketCode = sprintf('%s-%02d', $orderCode, $i);
        $ticketStmt->execute([
            $newOrderId,
            $eventId,
            $singleTicketCode,
            $event['price']
        ]);
    }

    $pdo->commit();

    set_flash('success', 'Pemesanan tiket berhasil dikirim! Kode pesanan: ' . $orderCode);
    header('Location: ../tiket-saya.php');
    exit;

} catch (Exception $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    // Kalau transaksi gagal, hapus lagi file bukti yang terlanjur ke-upload
    if ($proofFilename && file_exists($targetPath)) {
        @unlink($targetPath);
    }

    set_flash('error', 'Gagal memproses pesanan: ' . $e->getMessage());
    header('Location: ../index.php#tickets');
    exit;
}
