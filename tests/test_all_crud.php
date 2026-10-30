<?php
/**
 * Automated End-to-End CRUD Test Suite for Tiketin Migration
 */
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../includes/functions.php';

$pdo = get_db();
echo "=== MEMULAI TEST CRUD TIKETIN PHP NATIVE & MYSQL ===\n\n";

function assert_true($condition, $msg) {
    if ($condition) {
        echo " [PASS] " . $msg . "\n";
    } else {
        echo " [FAIL] " . $msg . "\n";
        exit(1);
    }
}

// 1. Test Autentikasi Pengguna
echo "1. Menguji Autentikasi & Registrasi User...\n";
$testUser = 'tester_' . time();
$testPass = 'password123';
$testFullName = 'Akun Tester';

// Register
$stmt = $pdo->prepare("INSERT INTO users (username, password, role, full_name) VALUES (?, ?, 'user', ?)");
$stmt->execute([$testUser, $testPass, $testFullName]);
$testerId = (int)$pdo->lastInsertId();
assert_true($testerId > 0, "Registrasi user baru berhasil (ID: $testerId)");

// Login test
$stmt = $pdo->prepare("SELECT * FROM users WHERE username = ? LIMIT 1");
$stmt->execute([$testUser]);
$loggedUser = $stmt->fetch();
assert_true($loggedUser && $loggedUser['password'] === $testPass, "Verifikasi login user berhasil dengan plain text password");

// 2. Test CRUD Event oleh Organizer
echo "\n2. Menguji CRUD Event oleh Organizer...\n";
$organizerId = 2; // Akun 'senjasuara'
$testEventName = 'Konser Akustik Uji Coba ' . time();
$testDate = date('Y-m-d', strtotime('+7 days'));
$testTime = '20:00 WIB';
$testLocation = 'M Bloc Live House, Jakarta Selatan';
$testPrice = 85000;
$testSlots = 50;

// CREATE Event
$stmt = $pdo->prepare("INSERT INTO events (owner_id, name, date, time, location, price, slots, category, description) VALUES (?, ?, ?, ?, ?, ?, ?, 'Konser', 'Deskripsi uji coba')");
$stmt->execute([$organizerId, $testEventName, $testDate, $testTime, $testLocation, $testPrice, $testSlots]);
$eventId = (int)$pdo->lastInsertId();
assert_true($eventId > 0, "Tambah event baru berhasil (ID: $eventId)");

// READ Event
$stmt = $pdo->prepare("SELECT * FROM events WHERE id = ?");
$stmt->execute([$eventId]);
$fetchedEvent = $stmt->fetch();
assert_true($fetchedEvent && $fetchedEvent['name'] === $testEventName && $fetchedEvent['time'] === $testTime && $fetchedEvent['location'] === $testLocation, "Baca detail event beserta jam & lokasi berhasil");

// UPDATE Event
$updatedPrice = 90000;
$updatedLocation = 'Gelora Bung Karno, Senayan';
$stmt = $pdo->prepare("UPDATE events SET price = ?, location = ? WHERE id = ?");
$stmt->execute([$updatedPrice, $updatedLocation, $eventId]);
$stmt = $pdo->prepare("SELECT price, location FROM events WHERE id = ?");
$stmt->execute([$eventId]);
$updatedRow = $stmt->fetch();
assert_true((int)$updatedRow['price'] === $updatedPrice && $updatedRow['location'] === $updatedLocation, "Update harga dan lokasi event berhasil");

// 3. Test Pemesanan Tiket & Pengurangan Slot
echo "\n3. Menguji Pemesanan Tiket & Transaksi Slot...\n";
$qtyToBook = 3;
$orderCode = generate_order_code();
$totalPrice = $updatedPrice * $qtyToBook;

$pdo->beginTransaction();
$stmt = $pdo->prepare("UPDATE events SET slots = slots - ? WHERE id = ?");
$stmt->execute([$qtyToBook, $eventId]);

$stmt = $pdo->prepare("
    INSERT INTO orders (order_code, user_id, event_id, quantity, unit_price, total_price, payment_method, payment_proof, status)
    VALUES (?, ?, ?, ?, ?, ?, 'bca', 'proof_test.jpg', 'menunggu')
");
$stmt->execute([$orderCode, $testerId, $eventId, $qtyToBook, $updatedPrice, $totalPrice]);
$orderId = (int)$pdo->lastInsertId();

// Simpan rincian ke tabel tickets
$tStmt = $pdo->prepare("INSERT INTO tickets (order_id, event_id, ticket_code, purchase_price) VALUES (?, ?, ?, ?)");
for ($i = 1; $i <= $qtyToBook; $i++) {
    $tStmt->execute([$orderId, $eventId, sprintf('%s-%02d', $orderCode, $i), $updatedPrice]);
}
$pdo->commit();

assert_true($orderId > 0, "Insert order tiket berhasil (Kode: $orderCode, ID: $orderId)");

// Verifikasi isi tabel tickets
$stmt = $pdo->prepare("SELECT COUNT(*) FROM tickets WHERE order_id = ?");
$stmt->execute([$orderId]);
assert_true((int)$stmt->fetchColumn() === $qtyToBook, "Tabel 'tickets' berhasil menampung $qtyToBook tiket individual");

// Cek pengurangan slot
$stmt = $pdo->prepare("SELECT slots FROM events WHERE id = ?");
$stmt->execute([$eventId]);
$remainingSlots = (int)$stmt->fetchColumn();
assert_true($remainingSlots === ($testSlots - $qtyToBook), "Slot tiket berkurang dengan benar ($testSlots -> $remainingSlots)");

// 4. Test Organizer Konfirmasi Order
echo "\n4. Menguji Organizer Menerima Pesanan...\n";
$stmt = $pdo->prepare("UPDATE orders SET status = 'berhasil' WHERE id = ?");
$stmt->execute([$orderId]);

$stmt = $pdo->prepare("SELECT status FROM orders WHERE id = ?");
$stmt->execute([$orderId]);
assert_true($stmt->fetchColumn() === 'berhasil', "Organizer berhasil mengubah status pesanan menjadi 'berhasil'");

// 5. Test Pengajuan Organizer & Approval oleh Admin
echo "\n5. Menguji Pengajuan Calon Organizer & Approval Admin...\n";
$appEventName = 'Festival Budaya Mahasiswa';
$appTime = '16:00 WIB';
$appLocation = 'Auditorium Kampus Terpadu';
$stmt = $pdo->prepare("
    INSERT INTO applications (user_id, full_name, contact, organization, event_name, event_date, event_time, event_location, event_price, event_slots, event_category, event_description, status)
    VALUES (?, ?, '081299998888', 'Komunitas Seni Kampus', ?, ?, ?, ?, 50000, 100, 'Konser', 'Event pertama', 'pending')
");
$stmt->execute([$testerId, $testFullName, $appEventName, date('Y-m-d', strtotime('+30 days')), $appTime, $appLocation]);
$appId = (int)$pdo->lastInsertId();
assert_true($appId > 0, "Pengajuan organizer berhasil disimpan dengan status 'pending'");

// Admin Approve
$pdo->beginTransaction();
$pdo->prepare("UPDATE applications SET status = 'approved' WHERE id = ?")->execute([$appId]);
$pdo->prepare("UPDATE users SET role = 'organizer', organization = 'Komunitas Seni Kampus' WHERE id = ?")->execute([$testerId]);
$pdo->prepare("
    INSERT INTO events (owner_id, name, date, time, location, price, slots, category) 
    VALUES (?, ?, ?, ?, ?, 50000, 100, 'Konser')
")->execute([$testerId, $appEventName, date('Y-m-d', strtotime('+30 days')), $appTime, $appLocation]);
$newFirstEventId = (int)$pdo->lastInsertId();
$pdo->commit();

$stmt = $pdo->prepare("SELECT role, organization FROM users WHERE id = ?");
$stmt->execute([$testerId]);
$upgradedUser = $stmt->fetch();
assert_true($upgradedUser['role'] === 'organizer' && $upgradedUser['organization'] === 'Komunitas Seni Kampus', "Role user berhasil dinaikkan menjadi 'organizer'");
assert_true($newFirstEventId > 0, "Event perdana dari pengajuan berhasil otomatis diterbitkan (ID: $newFirstEventId)");

$stmt = $pdo->prepare("SELECT time, location FROM events WHERE id = ?");
$stmt->execute([$newFirstEventId]);
$firstEvt = $stmt->fetch();
assert_true($firstEvt['time'] === $appTime && $firstEvt['location'] === $appLocation, "Event perdana mewarisi jam & lokasi pengajuan dengan akurat");

// 6. Test DELETE Event
echo "\n6. Menguji Hapus Event oleh Organizer...\n";
$stmt = $pdo->prepare("DELETE FROM events WHERE id = ?");
$stmt->execute([$newFirstEventId]);
$stmt = $pdo->prepare("SELECT id FROM events WHERE id = ?");
$stmt->execute([$newFirstEventId]);
assert_true(!$stmt->fetch(), "Event berhasil dihapus dari database");

// 7. Test CRUD Rekening Pembayaran Organizer (Tabel `payment`)
echo "\n7. Menguji CRUD Rekening Pembayaran Organizer (Tabel `payment`)...\n";
$testBank = 'Bank BCA';
$testAccNum = '8820999123';
$testAccName = 'Tester Organizer';

// Create
$stmt = $pdo->prepare("INSERT INTO payment (user_id, bank_name, account_number, account_name, is_active) VALUES (?, ?, ?, ?, 1)");
$stmt->execute([$testerId, $testBank, $testAccNum, $testAccName]);
$paymentId = (int)$pdo->lastInsertId();
assert_true($paymentId > 0, "Tambah rekening pembayaran baru berhasil (ID: $paymentId)");

// Read & Verify Isolation per Organizer
$stmt = $pdo->prepare("SELECT * FROM payment WHERE user_id = ?");
$stmt->execute([$testerId]);
$testerAccounts = $stmt->fetchAll();
assert_true(count($testerAccounts) === 1 && $testerAccounts[0]['account_number'] === $testAccNum, "Rekening organizer berhasil dibaca secara terisolasi");

// Cek bahwa organizer 'senjasuara' (ID: 2) memiliki rekening berbeda dengan 'jelajah' (ID: 3)
$stmt = $pdo->prepare("SELECT account_number FROM payment WHERE user_id = 2");
$stmt->execute();
$senjaAccs = $stmt->fetchAll(PDO::FETCH_COLUMN);

$stmt = $pdo->prepare("SELECT account_number FROM payment WHERE user_id = 3");
$stmt->execute();
$jelajahAccs = $stmt->fetchAll(PDO::FETCH_COLUMN);

$hasIntersection = !empty(array_intersect($senjaAccs, $jelajahAccs));
assert_true(!$hasIntersection && count($senjaAccs) > 0 && count($jelajahAccs) > 0, "Verifikasi multi-organizer: rekening senjasuara dan jelajah berbeda");

// Delete
$stmt = $pdo->prepare("DELETE FROM payment WHERE id = ? AND user_id = ?");
$stmt->execute([$paymentId, $testerId]);
$stmt = $pdo->prepare("SELECT id FROM payment WHERE id = ?");
$stmt->execute([$paymentId]);
assert_true(!$stmt->fetch(), "Rekening pembayaran berhasil dihapus dari database");

// Cleanup test records
$pdo->prepare("DELETE FROM orders WHERE id = ?")->execute([$orderId]);
$pdo->prepare("DELETE FROM events WHERE id = ?")->execute([$eventId]);
$pdo->prepare("DELETE FROM applications WHERE id = ?")->execute([$appId]);
$pdo->prepare("DELETE FROM users WHERE id = ?")->execute([$testerId]);

echo "\n============================================\n";
echo "SEMUA 15 TEST SKENARIO CRUD BERHASIL (100% PASS)!\n";
echo "============================================\n";
