<?php
/**
 * Handler Pemesanan Tiket & Upload Bukti Pembayaran
 */
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

$eventId = (int)($_POST['event_id'] ?? 0);
$quantity = (int)($_POST['quantity'] ?? 1);
$paymentMethod = trim($_POST['payment_method'] ?? '');

if ($eventId <= 0 || $quantity < 1 || $quantity > 10) {
    set_flash('error', 'Data pemesanan tidak valid (kuantitas maksimal 10 tiket).');
    header('Location: ../index.php#tickets');
    exit;
}

$paymentMethodName = 'Transfer Bank';
if (strtolower($paymentMethod) === 'qris') {
    $paymentMethodName = 'QRIS';
} elseif (str_starts_with($paymentMethod, 'bank_')) {
    $payId = (int)substr($paymentMethod, 5);
    $stmtP = $pdo->prepare("SELECT bank_name FROM payment WHERE id = ?");
    $stmtP->execute([$payId]);
    $payRow = $stmtP->fetch();
    $paymentMethodName = $payRow ? $payRow['bank_name'] : 'Transfer Bank';
} elseif (in_array(strtolower($paymentMethod), ['bca', 'bri', 'bsi', 'mandiri'], true)) {
    $paymentMethodName = get_payment_method_label($paymentMethod);
} else {
    set_flash('error', 'Metode pembayaran tidak valid.');
    header('Location: ../index.php#tickets');
    exit;
}

// Validasi Upload Bukti Transfer
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

// Validasi MIME jika modul finfo tersedia
if (function_exists('finfo_open')) {
    $finfo = @finfo_open(FILEINFO_MIME_TYPE);
    if ($finfo) {
        $mimeType = @finfo_file($finfo, $file['tmp_name']);
        @finfo_close($finfo);
        $allowedMimes = ['image/jpeg', 'image/pjpeg', 'image/png', 'image/x-png', 'image/webp', 'image/gif', 'application/octet-stream'];
        if ($mimeType && !in_array($mimeType, $allowedMimes, true)) {
            set_flash('error', 'Tipe file tidak valid. Harap unggah file foto/screenshot bukti transfer.');
            header('Location: ../index.php#tickets');
            exit;
        }
    }
}

if ($file['size'] > 5 * 1024 * 1024) {
    set_flash('error', 'Ukuran file bukti pembayaran maksimal 5 MB.');
    header('Location: ../index.php#tickets');
    exit;
}

$proofFilename = 'proof_' . date('Ymd_His') . '_' . bin2hex(random_bytes(4)) . '.' . $ext;
$uploadDir = __DIR__ . '/../uploads/proofs/';
if (!is_dir($uploadDir)) {
    @mkdir($uploadDir, 0777, true);
}
@chmod($uploadDir, 0777);
$targetPath = $uploadDir . $proofFilename;

if (!move_uploaded_file($file['tmp_name'], $targetPath)) {
    set_flash('error', 'Gagal menyimpan file bukti pembayaran di server. Periksa hak akses folder uploads.');
    header('Location: ../index.php#tickets');
    exit;
}
@chmod($targetPath, 0666);

try {
    $pdo->beginTransaction();

    // Kunci row event untuk verifikasi kuota slot (SELECT FOR UPDATE)
    $stmtEvent = $pdo->prepare("SELECT id, name, price, slots FROM events WHERE id = ? FOR UPDATE");
    $stmtEvent->execute([$eventId]);
    $event = $stmtEvent->fetch();

    if (!$event) {
        throw new Exception('Event tidak ditemukan.');
    }

    if ($event['slots'] < $quantity) {
        throw new Exception('Slot tiket tidak mencukupi (sisa slot: ' . $event['slots'] . ').');
    }

    // Kurangi kuota slot tiket
    $updateStmt = $pdo->prepare("UPDATE events SET slots = slots - ? WHERE id = ?");
    $updateStmt->execute([$quantity, $eventId]);

    // Simpan data order
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

    // Simpan rincian tiket individual ke tabel 'tickets' (sesuai ERD)
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
    // Hapus file yang sudah diunggah jika transaksi database gagal
    if ($proofFilename && file_exists($targetPath)) {
        @unlink($targetPath);
    }

    set_flash('error', 'Gagal memproses pesanan: ' . $e->getMessage());
    header('Location: ../index.php#tickets');
    exit;
}
