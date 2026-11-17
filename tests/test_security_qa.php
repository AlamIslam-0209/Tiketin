<?php
// Rangkaian uji keamanan, kontrol akses, IDOR, dan edge-case untuk Tiketin
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../includes/functions.php';

$baseUrl = getenv('TIKETIN_TEST_URL') ?: 'http://127.0.0.1:8765';
$pdo = get_db();

$passed = 0;
$failed = 0;

function qa_assert(bool $cond, string $label, string $detail = ''): void
{
    global $passed, $failed;
    if ($cond) {
        echo " [PASS] {$label}\n";
        $passed++;
    } else {
        echo " [FAIL] {$label}" . ($detail !== '' ? " -> {$detail}" : '') . "\n";
        $failed++;
    }
}

function qa_curl(
    string $url,
    string $method = 'GET',
    $data = [],
    ?string $cookieJar = null,
    array $headers = [],
    bool $follow = false
): array {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, $follow);
    curl_setopt($ch, CURLOPT_HEADER, true);

    if ($cookieJar !== null) {
        curl_setopt($ch, CURLOPT_COOKIEJAR, $cookieJar);
        curl_setopt($ch, CURLOPT_COOKIEFILE, $cookieJar);
    }

    if (!empty($headers)) {
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    }

    if ($method === 'POST') {
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $data);
    }

    $raw = curl_exec($ch);
    if ($raw === false) {
        $err = curl_error($ch);
        curl_close($ch);
        throw new RuntimeException("cURL error for {$url}: {$err}");
    }

    $headerSize = curl_getinfo($ch, CURLINFO_HEADER_SIZE);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $headerStr = substr($raw, 0, $headerSize);
    $body = substr($raw, $headerSize);
    curl_close($ch);

    return [
        'code'    => $httpCode,
        'headers' => $headerStr,
        'body'    => $body,
    ];
}

function get_location_header(string $headers): string
{
    if (preg_match('/^Location:\s*(.+)$/im', $headers, $m)) {
        return trim($m[1]);
    }
    return '';
}

function create_tiny_png(string $path): void
{
    // Gambar PNG kecil yang sah buat simulasi upload
    $pngBase64 = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=';
    file_put_contents($path, base64_decode($pngBase64));
}

echo "=== MEMULAI COMPREHENSIVE SECURITY & QA TEST SUITE TIKETIN ===\n\n";

$tmpDir = sys_get_temp_dir();
$userCookie = $tmpDir . '/qa_user_' . uniqid() . '.cookie';
$orgCookie = $tmpDir . '/qa_org_' . uniqid() . '.cookie';
$org2Cookie = $tmpDir . '/qa_org2_' . uniqid() . '.cookie';
$adminCookie = $tmpDir . '/qa_admin_' . uniqid() . '.cookie';
$tempUserCookie = $tmpDir . '/qa_temp_' . uniqid() . '.cookie';
$sqliCookie = $tmpDir . '/qa_sqli_' . uniqid() . '.cookie';

$fakeTxtFile = $tmpDir . '/qa_fake_' . uniqid() . '.txt';
$fakeJpgFile = $tmpDir . '/qa_fake_img_' . uniqid() . '.jpg';
$validPngFile = $tmpDir . '/qa_valid_' . uniqid() . '.png';

file_put_contents($fakeTxtFile, "ini bukan gambar");
file_put_contents($fakeJpgFile, "<?php echo 'fake jpg'; ?>");
create_tiny_png($validPngFile);

$createdUserIds = [];
$createdEventIds = [];
$createdOrderIds = [];
$createdAppIds = [];
$createdPaymentIds = [];
$createdEventImageFiles = [];

try {
    // Siapin cookie login untuk tiap role yang mau diuji
    qa_curl("{$baseUrl}/proses/auth_process.php?action=login", 'POST', [
        'username' => 'pengunjung',
        'password' => 'user123'
    ], $userCookie);

    qa_curl("{$baseUrl}/proses/auth_process.php?action=login", 'POST', [
        'username' => 'senjasuara',
        'password' => 'organizer123'
    ], $orgCookie);

    qa_curl("{$baseUrl}/proses/auth_process.php?action=login", 'POST', [
        'username' => 'jelajah_nusantara',
        'password' => 'organizer123'
    ], $org2Cookie);

    qa_curl("{$baseUrl}/proses/auth_process.php?action=login", 'POST', [
        'username' => 'admin',
        'password' => 'admin123'
    ], $adminCookie);

    // Uji pengamanan cookie sesi, open redirect, dan proteksi CSRF same-origin
    echo "1. Menguji Session Hardening, Open Redirect & CSRF Origin Guard...\n";

    $loginRes = qa_curl("{$baseUrl}/proses/auth_process.php?action=login", 'POST', [
        'username' => 'pengunjung',
        'password' => 'user123',
        'redirect' => '//evil.com/phish'
    ], $sqliCookie);
    $loc1 = get_location_header($loginRes['headers']);
    qa_assert(
        str_contains($loginRes['headers'], 'HttpOnly') || str_contains($loginRes['headers'], 'httponly'),
        'Session cookie memakai flag HttpOnly'
    );
    qa_assert(
        stripos($loginRes['headers'], 'SameSite=Lax') !== false,
        'Session cookie memakai flag SameSite=Lax'
    );
    qa_assert(
        !str_contains($loc1, 'evil.com'),
        'Open Redirect (//evil.com) ditolak saat login',
        "Location: {$loc1}"
    );

    $loginRes2 = qa_curl("{$baseUrl}/proses/auth_process.php?action=login", 'POST', [
        'username' => 'pengunjung',
        'password' => 'user123',
        'redirect' => 'https://evil.com'
    ]);
    $loc2 = get_location_header($loginRes2['headers']);
    qa_assert(
        !str_contains($loc2, 'evil.com'),
        'Open Redirect (https://evil.com) ditolak saat login',
        "Location: {$loc2}"
    );

    $csrfRes = qa_curl("{$baseUrl}/proses/event_process.php", 'POST', [
        'action' => 'create',
        'name'   => 'CSRF Event'
    ], $orgCookie, ['Origin: https://evil.com']);
    qa_assert($csrfRes['code'] === 403, 'CSRF Cross-Origin POST dengan Origin eksternal ditolak (HTTP 403)', "HTTP {$csrfRes['code']}");

    $csrfRefRes = qa_curl("{$baseUrl}/proses/event_process.php", 'POST', [
        'action' => 'create',
        'name'   => 'CSRF Referer Event'
    ], $orgCookie, ['Referer: https://attacker.example/form.html']);
    qa_assert($csrfRefRes['code'] === 403, 'CSRF Cross-Origin POST dengan Referer eksternal ditolak (HTTP 403)', "HTTP {$csrfRefRes['code']}");

    // Uji ketahanan terhadap SQL Injection
    echo "\n2. Menguji Ketahanan SQL Injection (SQLi)...\n";

    $sqliPayloads = [
        "' OR '1'='1",
        "admin' -- ",
        "' OR 1=1#",
        "'; DROP TABLE users; -- ",
        "' UNION SELECT 1,2,3,4,5,6 -- "
    ];

    $sqliBypassed = false;
    foreach ($sqliPayloads as $payload) {
        $c = $tmpDir . '/qa_sqli_try_' . uniqid() . '.cookie';
        $r = qa_curl("{$baseUrl}/proses/auth_process.php?action=login", 'POST', [
            'username' => $payload,
            'password' => $payload
        ], $c);
        $checkAdmin = qa_curl("{$baseUrl}/admin.php", 'GET', [], $c);
        @unlink($c);
        if ($checkAdmin['code'] === 200 || str_contains($r['body'], 'SQLSTATE')) {
            $sqliBypassed = true;
            break;
        }
    }
    qa_assert(!$sqliBypassed, 'Login kebal terhadap payload SQL Injection klasik & UNION');

    // Payload SQLi di form daftar harus tersimpan apa adanya tanpa error SQL
    $sqliUser = "sqli_" . time() . "' OR '1'='1";
    $regRes = qa_curl("{$baseUrl}/proses/auth_process.php?action=register", 'POST', [
        'fullName' => "User ' OR 1=1 --",
        'username' => $sqliUser,
        'password' => 'password123'
    ], $tempUserCookie);
    $stmtFindSqli = $pdo->prepare("SELECT id, full_name FROM users WHERE username = ?");
    $stmtFindSqli->execute([$sqliUser]);
    $rowSqli = $stmtFindSqli->fetch();
    if ($rowSqli) {
        $createdUserIds[] = (int)$rowSqli['id'];
    }
    qa_assert(
        $rowSqli !== false && $rowSqli['full_name'] === "User ' OR 1=1 --",
        'Register menyimpan karakter SQLi secara literal (Prepared Statement aman)'
    );

    // Payload SQLi di parameter ID endpoint
    $idSqli = qa_curl("{$baseUrl}/proses/event_process.php?action=delete&id=1%20OR%201=1", 'GET', [], $orgCookie);
    $evt1Exists = (bool)$pdo->query("SELECT id FROM events WHERE id = 1")->fetch();
    qa_assert($evt1Exists && !str_contains($idSqli['body'], 'SQLSTATE'), 'Parameter ID event kebal terhadap SQLi');

    // Uji ketahanan XSS dan sanitasi output
    echo "\n3. Menguji Ketahanan XSS & Sanitasi Output...\n";

    $sanitized = sanitize('<script>alert("xss")</script>');
    qa_assert(
        !str_contains($sanitized, '<script>') && str_contains($sanitized, '&lt;script&gt;'),
        'Helper sanitize() meng-escape tag HTML/script dengan benar'
    );

    $xssEventName = 'QA XSS Event <script>alert("xss")</script> ' . time();
    qa_curl("{$baseUrl}/proses/event_process.php", 'POST', [
        'action'      => 'create',
        'name'        => $xssEventName,
        'date'        => date('Y-m-d', strtotime('+14 days')),
        'time'        => '19:00 WIB',
        'location'    => '<img src=x onerror=alert(1)>',
        'price'       => 50000,
        'slots'       => 20,
        'category'    => 'Konser',
        'description' => '<script>console.log("pwn")</script>',
        'event_image' => new CURLFile($validPngFile, 'image/png', 'event.png')
    ], $orgCookie);

    $stmtEvtXss = $pdo->prepare("SELECT id, image_path FROM events WHERE name = ?");
    $stmtEvtXss->execute([$xssEventName]);
    $xssEventRow = $stmtEvtXss->fetch();
    $xssEventId = $xssEventRow ? (int)$xssEventRow['id'] : 0;
    if ($xssEventId > 0) {
        $createdEventIds[] = $xssEventId;
        if (!empty($xssEventRow['image_path'])) {
            $createdEventImageFiles[] = basename($xssEventRow['image_path']);
        }
    }

    $idxHtml = qa_curl("{$baseUrl}/index.php")['body'];
    $orgHtml = qa_curl("{$baseUrl}/organizer.php", 'GET', [], $orgCookie)['body'];
    qa_assert(
        $xssEventId > 0
        && !str_contains($idxHtml, '<script>alert("xss")</script>')
        && !str_contains($idxHtml, '<img src=x onerror=alert(1)>')
        && !str_contains($orgHtml, '<script>alert("xss")</script>')
        && str_contains($idxHtml, 'escapeHtml('),
        'Stored & DOM XSS ter-escape di index.php dan organizer.php'
    );

    // Uji kontrol akses dan celah IDOR lintas role maupun lintas organizer
    echo "\n4. Menguji Access Control & IDOR (Cross-Role & Cross-Organizer)...\n";

    // User biasa mencoba membuat event
    $userCreateEvt = qa_curl("{$baseUrl}/proses/event_process.php", 'POST', [
        'action' => 'create',
        'name'   => 'Illegal User Event',
        'date'   => date('Y-m-d', strtotime('+10 days')),
        'price'  => 10000,
        'slots'  => 10
    ], $userCookie);
    $illegalEvtCheck = $pdo->query("SELECT id FROM events WHERE name = 'Illegal User Event'")->fetch();
    qa_assert(!$illegalEvtCheck && $userCreateEvt['code'] === 302, 'User biasa ditolak membuat event');

    // User biasa mencoba memutus pengajuan organizer
    $userDecide = qa_curl("{$baseUrl}/proses/application_process.php", 'POST', [
        'action'         => 'decide',
        'application_id' => 1,
        'status'         => 'approved'
    ], $userCookie);
    qa_assert($userDecide['code'] === 302, 'User biasa ditolak melakukan approval/reject pengajuan organizer');

    // User biasa mencoba menambah rekening pembayaran
    $userAddPay = qa_curl("{$baseUrl}/proses/payment_process.php", 'POST', [
        'action'         => 'add',
        'bank_name'      => 'Bank Illegal',
        'account_number' => '999999999',
        'account_name'   => 'Hacker'
    ], $userCookie);
    $illegalPayCheck = $pdo->query("SELECT id FROM payment WHERE account_number = '999999999'")->fetch();
    qa_assert(!$illegalPayCheck && $userAddPay['code'] === 302, 'User biasa ditolak menambah rekening pembayaran');

    // Organizer pertama mencoba mengubah dan menghapus event milik organizer lain
    $origEvt3 = $pdo->query("SELECT name FROM events WHERE id = 3")->fetchColumn();
    qa_curl("{$baseUrl}/proses/event_process.php", 'POST', [
        'action'   => 'update',
        'event_id' => 3,
        'name'     => 'Hijacked Event Name',
        'date'     => date('Y-m-d', strtotime('+20 days')),
        'price'    => 1000,
        'slots'    => 5
    ], $orgCookie);
    $afterEvt3 = $pdo->query("SELECT name FROM events WHERE id = 3")->fetchColumn();
    qa_assert($afterEvt3 === $origEvt3, 'IDOR: Organizer A tidak bisa mengubah event milik Organizer B');

    qa_curl("{$baseUrl}/proses/event_process.php?action=delete&id=3", 'GET', [], $orgCookie);
    $evt3StillThere = (bool)$pdo->query("SELECT id FROM events WHERE id = 3")->fetchColumn();
    qa_assert($evt3StillThere, 'IDOR: Organizer A tidak bisa menghapus event milik Organizer B');

    // Organizer pertama mencoba menghapus rekening milik organizer lain
    qa_curl("{$baseUrl}/proses/payment_process.php?action=delete&id=5", 'GET', [], $orgCookie);
    $pay5StillThere = (bool)$pdo->query("SELECT id FROM payment WHERE id = 5")->fetchColumn();
    qa_assert($pay5StillThere, 'IDOR: Organizer A tidak bisa menghapus rekening milik Organizer B');

    // Coba pesan tiket memakai rekening milik organizer lain saat checkout
    $beforeSlots1 = (int)$pdo->query("SELECT slots FROM events WHERE id = 1")->fetchColumn();
    $idorOrderRes = qa_curl("{$baseUrl}/proses/order_process.php", 'POST', [
        'event_id'       => 1,
        'quantity'       => 1,
        'payment_method' => 'bank_5',
        'payment_proof'  => new CURLFile($validPngFile, 'image/png', 'proof.png')
    ], $userCookie);
    $afterSlots1 = (int)$pdo->query("SELECT slots FROM events WHERE id = 1")->fetchColumn();
    qa_assert(
        $beforeSlots1 === $afterSlots1 && $idorOrderRes['code'] === 302,
        'IDOR Checkout: Pemakaian rekening milik organizer lain ditolak & slot tidak berkurang'
    );

    // Uji validasi logika bisnis, upload file ketat, dan alur fitur utama
    echo "\n5. Menguji Validasi Edge Cases, Upload Ketat, Hapus Event Admin & Auto-Refresh Sesi...\n";

    // Kuantitas tiket di luar batas wajar harus ditolak
    foreach ([0, -5, 11] as $badQty) {
        qa_curl("{$baseUrl}/proses/order_process.php", 'POST', [
            'event_id'       => 1,
            'quantity'       => $badQty,
            'payment_method' => 'qris',
            'payment_proof'  => new CURLFile($validPngFile, 'image/png', 'proof.png')
        ], $userCookie);
    }
    $slotsAfterBadQty = (int)$pdo->query("SELECT slots FROM events WHERE id = 1")->fetchColumn();
    qa_assert($slotsAfterBadQty === $beforeSlots1, 'Kuantitas tiket tidak valid (0, -5, 11) ditolak');

    // Upload file teks dan skrip PHP berkedok ekstensi gambar harus ditolak
    qa_curl("{$baseUrl}/proses/order_process.php", 'POST', [
        'event_id'       => 1,
        'quantity'       => 1,
        'payment_method' => 'qris',
        'payment_proof'  => new CURLFile($fakeTxtFile, 'text/plain', 'fake.txt')
    ], $userCookie);
    qa_curl("{$baseUrl}/proses/order_process.php", 'POST', [
        'event_id'       => 1,
        'quantity'       => 1,
        'payment_method' => 'qris',
        'payment_proof'  => new CURLFile($fakeJpgFile, 'image/jpeg', 'shell.jpg')
    ], $userCookie);
    $slotsAfterBadFile = (int)$pdo->query("SELECT slots FROM events WHERE id = 1")->fetchColumn();
    qa_assert($slotsAfterBadFile === $beforeSlots1, 'Upload file non-gambar / ekstensi .jpg palsu ditolak oleh MIME + getimagesize()');

    // Upload foto event palsu juga harus ditolak
    $fakeEventName = 'Fake Image Event ' . time();
    qa_curl("{$baseUrl}/proses/event_process.php", 'POST', [
        'action'      => 'create',
        'name'        => $fakeEventName,
        'date'        => date('Y-m-d', strtotime('+10 days')),
        'price'       => 50000,
        'slots'       => 10,
        'event_image' => new CURLFile($fakeJpgFile, 'image/jpeg', 'shell.jpg')
    ], $orgCookie);
    $fakeImgEvt = $pdo->prepare("SELECT id FROM events WHERE name = ?");
    $fakeImgEvt->execute([$fakeEventName]);
    qa_assert(!$fakeImgEvt->fetch(), 'Upload foto event palsu ditolak oleh validasi MIME + getimagesize()');

    // Harga atau kuota negatif saat buat event harus ditolak
    qa_curl("{$baseUrl}/proses/event_process.php", 'POST', [
        'action' => 'create',
        'name'   => 'Negative Price Event',
        'date'   => date('Y-m-d', strtotime('+10 days')),
        'price'  => -50000,
        'slots'  => 10
    ], $orgCookie);
    qa_curl("{$baseUrl}/proses/event_process.php", 'POST', [
        'action' => 'create',
        'name'   => 'Negative Slots Event',
        'date'   => date('Y-m-d', strtotime('+10 days')),
        'price'  => 50000,
        'slots'  => -5
    ], $orgCookie);
    $negEvt = $pdo->query("SELECT id FROM events WHERE name IN ('Negative Price Event', 'Negative Slots Event')")->fetch();
    qa_assert(!$negEvt, 'Pembuatan event dengan harga negatif atau kuota negatif ditolak');

    // Checkout yang sah dilanjutkan pembatalan order oleh organizer agar kuota kembali
    $pengunjungId = (int)$pdo->query("SELECT id FROM users WHERE username = 'pengunjung'")->fetchColumn();
    $validOrderRes = qa_curl("{$baseUrl}/proses/order_process.php", 'POST', [
        'event_id'       => $xssEventId,
        'quantity'       => 2,
        'payment_method' => 'bank_2',
        'payment_proof'  => new CURLFile($validPngFile, 'image/png', 'proof.png')
    ], $userCookie);
    $stmtOrder = $pdo->prepare("SELECT id, payment_proof, status FROM orders WHERE user_id = ? AND event_id = ? ORDER BY id DESC LIMIT 1");
    $stmtOrder->execute([$pengunjungId, $xssEventId]);
    $createdOrder = $stmtOrder->fetch();
    if ($createdOrder) {
        $createdOrderIds[] = (int)$createdOrder['id'];
    }
    $slotsBooked = (int)$pdo->query("SELECT slots FROM events WHERE id = {$xssEventId}")->fetchColumn();
    qa_assert(
        $createdOrder !== false && $slotsBooked === 18 && get_location_header($validOrderRes['headers']) === '../tiket-saya.php',
        'Checkout valid dengan PNG & rekening milik organizer mengurangi slot atomik (20 -> 18)'
    );

    // Organizer lain tidak boleh memverifikasi order bukan miliknya
    if ($createdOrder) {
        qa_curl("{$baseUrl}/proses/event_process.php", 'POST', [
            'action'   => 'update_order_status',
            'order_id' => $createdOrder['id'],
            'status'   => 'berhasil'
        ], $org2Cookie);
        $statusCheck = $pdo->query("SELECT status FROM orders WHERE id = " . (int)$createdOrder['id'])->fetchColumn();
        qa_assert($statusCheck === 'menunggu', 'IDOR Order: Organizer B tidak bisa memverifikasi order milik Organizer A');

        // Pembatalan order oleh organizer pemilik harus mengembalikan slot dan membatalkan tiket
        qa_curl("{$baseUrl}/proses/event_process.php", 'POST', [
            'action'   => 'update_order_status',
            'order_id' => $createdOrder['id'],
            'status'   => 'dibatalkan'
        ], $orgCookie);
        $slotsRestored = (int)$pdo->query("SELECT slots FROM events WHERE id = {$xssEventId}")->fetchColumn();
        $ticketStatus = $pdo->query("SELECT status FROM tickets WHERE order_id = " . (int)$createdOrder['id'])->fetchColumn();
        qa_assert(
            $slotsRestored === 20 && $ticketStatus === 'cancelled',
            'Pembatalan order oleh Organizer pemilik mengembalikan slot (18 -> 20) & membatalkan tiket'
        );

        // Hapus file upload bukti test supaya folder uploads tetap bersih
        if (!empty($createdOrder['payment_proof'])) {
            $proofPath = __DIR__ . '/../uploads/proofs/' . basename($createdOrder['payment_proof']);
            if (file_exists($proofPath)) {
                @unlink($proofPath);
            }
        }
    }

    // Pengajuan organizer ganda saat masih berstatus pending harus ditolak
    if ($rowSqli) {
        $tmpUserId = (int)$rowSqli['id'];
        qa_curl("{$baseUrl}/proses/application_process.php", 'POST', [
            'action'       => 'apply',
            'fullName'     => 'Temp Applicant',
            'contact'      => '08123456789',
            'organization' => 'QA Org',
            'name'         => 'QA First Event',
            'date'         => date('Y-m-d', strtotime('+30 days')),
            'time'         => '19:00 WIB',
            'location'     => 'Jakarta',
            'category'     => 'Seminar',
            'price'        => 25000,
            'slots'        => 30,
            'description'  => 'Test app first',
            'event_image'  => new CURLFile($validPngFile, 'image/png', 'app.png')
        ], $tempUserCookie);
        qa_curl("{$baseUrl}/proses/application_process.php", 'POST', [
            'action'       => 'apply',
            'fullName'     => 'Temp Applicant',
            'contact'      => '08123456789',
            'organization' => 'QA Org Two',
            'name'         => 'QA Second Event',
            'date'         => date('Y-m-d', strtotime('+30 days')),
            'time'         => '19:00 WIB',
            'location'     => 'Jakarta',
            'category'     => 'Seminar',
            'price'        => 25000,
            'slots'        => 30,
            'description'  => 'Test app second'
        ], $tempUserCookie);
        $cntApp = (int)$pdo->query("SELECT COUNT(*) FROM applications WHERE user_id = {$tmpUserId}")->fetchColumn();
        qa_assert($cntApp === 1, 'Pengajuan organizer ganda saat masih berstatus pending ditolak');

        // Auto-refresh sesi user harus langsung aktif setelah admin menyetujui pengajuan
        $appRow = $pdo->query("SELECT id, image_path FROM applications WHERE user_id = {$tmpUserId} ORDER BY id DESC LIMIT 1")->fetch();
        $appId = $appRow ? (int)$appRow['id'] : 0;
        if ($appId > 0) {
            $createdAppIds[] = $appId;
            if (!empty($appRow['image_path'])) {
                $createdEventImageFiles[] = basename($appRow['image_path']);
            }
            qa_curl("{$baseUrl}/proses/application_process.php", 'POST', [
                'action'         => 'decide',
                'application_id' => $appId,
                'status'         => 'approved'
            ], $adminCookie);

            // Coba akses dashboard organizer pakai cookie lama tanpa login ulang
            $orgDashRes = qa_curl("{$baseUrl}/organizer.php", 'GET', [], $tempUserCookie);
            qa_assert(
                $orgDashRes['code'] === 200 && str_contains($orgDashRes['body'], 'Dashboard Organizer'),
                'Auto-refresh sesi user langsung mengenali kenaikan role ke organizer tanpa logout'
            );

            // Uji penghapusan event perdana oleh admin
            $firstEvtId = (int)$pdo->query("SELECT id FROM events WHERE owner_id = {$tmpUserId} ORDER BY id DESC LIMIT 1")->fetchColumn();
            if ($firstEvtId > 0) {
                $adminDelRes = qa_curl("{$baseUrl}/proses/event_process.php?action=delete&id={$firstEvtId}", 'GET', [], $adminCookie);
                $stillExists = (bool)$pdo->query("SELECT id FROM events WHERE id = {$firstEvtId}")->fetchColumn();
                qa_assert(
                    !$stillExists && get_location_header($adminDelRes['headers']) === '../admin.php',
                    'Admin berhasil menghapus event dan diarahkan kembali ke admin.php'
                );
            }
        }
    }

    // Cek detail UI dan konsistensi aset frontend
    echo "\n6. Menguji Perbaikan UI/UX & Aset Frontend...\n";

    $headerCheck = qa_curl("{$baseUrl}/index.php")['body'];
    $userIndexHtml = qa_curl("{$baseUrl}/index.php", 'GET', [], $userCookie)['body'];
    $cssCheck = qa_curl("{$baseUrl}/assets/css/style.css")['body'];
    $adminPageHtml = qa_curl("{$baseUrl}/admin.php", 'GET', [], $adminCookie)['body'];

    qa_assert(str_contains($headerCheck, 'gsap/3.12.5/gsap.min.js'), 'URL CDN GSAP diperbarui ke 3.12.5');
    qa_assert(str_contains($userIndexHtml, 'maks. 5MB'), 'Label ukuran upload di index.php sinkron dengan backend (maks. 5MB)');
    qa_assert(
        str_contains($cssCheck, '.category-icon.konser') && str_contains($cssCheck, '.category-icon.wisata'),
        'CSS category-icon mendukung kelas .konser dan .wisata'
    );
    qa_assert(
        str_contains($adminPageHtml, 'proses/event_process.php?action=delete&id='),
        'Tombol Hapus event tersedia pada halaman Admin'
    );

} finally {
    // Bersihin seluruh data sisa test biar database dan folder uploads tetap bersih
    foreach ($createdOrderIds as $oid) {
        $pdo->prepare("DELETE FROM orders WHERE id = ?")->execute([$oid]);
    }
    foreach ($createdEventIds as $eid) {
        $pdo->prepare("DELETE FROM events WHERE id = ?")->execute([$eid]);
    }
    foreach ($createdAppIds as $aid) {
        $pdo->prepare("DELETE FROM applications WHERE id = ?")->execute([$aid]);
    }
    foreach ($createdPaymentIds as $pid) {
        $pdo->prepare("DELETE FROM payment WHERE id = ?")->execute([$pid]);
    }
    foreach ($createdUserIds as $uid) {
        $pdo->prepare("DELETE FROM users WHERE id = ?")->execute([$uid]);
    }
    foreach (array_unique($createdEventImageFiles) as $imgFile) {
        $imgPath = __DIR__ . '/../uploads/events/' . basename($imgFile);
        if (is_file($imgPath)) {
            @unlink($imgPath);
        }
    }

    @unlink($userCookie);
    @unlink($orgCookie);
    @unlink($org2Cookie);
    @unlink($adminCookie);
    @unlink($tempUserCookie);
    @unlink($sqliCookie);
    @unlink($fakeTxtFile);
    @unlink($fakeJpgFile);
    @unlink($validPngFile);
}

echo "\n============================================\n";
echo "RINGKASAN SECURITY & QA TEST: {$passed} PASS, {$failed} FAIL\n";
echo "============================================\n";

if ($failed > 0) {
    exit(1);
}
