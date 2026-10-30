<?php
/**
 * Automated HTTP Simulation Test for Tiketin
 */

$baseUrl = 'http://127.0.0.1:8765';

function test_http($name, $callable) {
    echo "Running: $name... ";
    try {
        $res = $callable();
        if ($res === true) {
            echo "[PASS]\n";
        } else {
            echo "[FAIL] $res\n";
            exit(1);
        }
    } catch (Exception $e) {
        echo "[FAIL EXCEPTION] " . $e->getMessage() . "\n";
        exit(1);
    }
}

function curl_req($url, $method = 'GET', $data = [], $cookieJar = null, $follow = false) {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, $follow);
    curl_setopt($ch, CURLOPT_HEADER, true);

    if ($cookieJar) {
        curl_setopt($ch, CURLOPT_COOKIEJAR, $cookieJar);
        curl_setopt($ch, CURLOPT_COOKIEFILE, $cookieJar);
    }

    if ($method === 'POST') {
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $data);
    }

    $raw = curl_exec($ch);
    $headerSize = curl_getinfo($ch, CURLINFO_HEADER_SIZE);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $headerStr = substr($raw, 0, $headerSize);
    $body = substr($raw, $headerSize);
    curl_close($ch);

    return [
        'code' => $httpCode,
        'headers' => $headerStr,
        'body' => $body
    ];
}

$userCookie = sys_get_temp_dir() . '/tiketin_test_user_' . uniqid() . '.cookie';
$orgCookie = sys_get_temp_dir() . '/tiketin_test_org_' . uniqid() . '.cookie';
$adminCookie = sys_get_temp_dir() . '/tiketin_test_admin_' . uniqid() . '.cookie';

// 1. Public Pages
test_http("1. Public GET index.php", function() use ($baseUrl) {
    $r = curl_req($baseUrl . '/index.php');
    if ($r['code'] !== 200) return "Expected 200, got {$r['code']}";
    if (!str_contains($r['body'], 'Tiketin')) return "Tiketin brand not found in body";
    if (!str_contains($r['body'], 'assets/css/style.css')) return "style.css link missing";
    return true;
});

test_http("2. Public GET login.php & register.php", function() use ($baseUrl) {
    $r1 = curl_req($baseUrl . '/login.php');
    if ($r1['code'] !== 200 || !str_contains($r1['body'], 'action="proses/auth_process.php?action=login"')) {
        return "login.php check failed";
    }
    $r2 = curl_req($baseUrl . '/register.php');
    if ($r2['code'] !== 200 || !str_contains($r2['body'], 'action="proses/auth_process.php?action=register"')) {
        return "register.php check failed";
    }
    return true;
});

// 2. Auth Guard Protection (unauthenticated)
test_http("3. Unauthenticated redirects to login.php", function() use ($baseUrl) {
    $pages = ['/tiket-saya.php', '/apply.php', '/organizer.php', '/admin.php'];
    foreach ($pages as $p) {
        $r = curl_req($baseUrl . $p);
        if ($r['code'] !== 302 || !str_contains($r['headers'], 'Location: login.php')) {
            return "Page $p did not redirect to login.php properly (code: {$r['code']})";
        }
    }
    return true;
});

// 3. User Login & Access Control
test_http("4. User Login & Session Guard", function() use ($baseUrl, $userCookie) {
    $r = curl_req($baseUrl . '/proses/auth_process.php?action=login', 'POST', [
        'username' => 'alam',
        'password' => 'user123'
    ], $userCookie);

    if ($r['code'] !== 302 || !str_contains($r['headers'], 'index.php#home')) {
        return "User login failed to redirect to index.php#home";
    }

    // Access tiket-saya as user
    $r2 = curl_req($baseUrl . '/tiket-saya.php', 'GET', [], $userCookie);
    if ($r2['code'] !== 200 || !str_contains($r2['body'], 'Tiket Saya')) {
        return "Failed to access tiket-saya.php as user (code: {$r2['code']})";
    }

    // Access apply.php as user
    $r3 = curl_req($baseUrl . '/apply.php', 'GET', [], $userCookie);
    if ($r3['code'] !== 200 || !str_contains($r3['body'], 'Ajukan jadi organizer')) {
        return "Failed to access apply.php as user (code: {$r3['code']})";
    }

    // Attempt to access organizer.php as user -> should redirect
    $r4 = curl_req($baseUrl . '/organizer.php', 'GET', [], $userCookie);
    if ($r4['code'] !== 302) {
        return "User should not access organizer.php (code: {$r4['code']})";
    }

    // Attempt to access admin.php as user -> should redirect
    $r5 = curl_req($baseUrl . '/admin.php', 'GET', [], $userCookie);
    if ($r5['code'] !== 302) {
        return "User should not access admin.php (code: {$r5['code']})";
    }

    return true;
});

// 4. Organizer Login & Dashboard
test_http("5. Organizer Login & Dashboard Access", function() use ($baseUrl, $orgCookie) {
    $r = curl_req($baseUrl . '/proses/auth_process.php?action=login', 'POST', [
        'username' => 'senjasuara',
        'password' => 'organizer123'
    ], $orgCookie);

    if ($r['code'] !== 302 || !str_contains($r['headers'], 'organizer.php')) {
        return "Organizer login failed to redirect to organizer.php";
    }

    $r2 = curl_req($baseUrl . '/organizer.php', 'GET', [], $orgCookie);
    if ($r2['code'] !== 200 || !str_contains($r2['body'], 'Dashboard Organizer')) {
        return "Failed to access organizer.php as organizer (code: {$r2['code']})";
    }

    return true;
});

// 5. Admin Login & Dashboard
test_http("6. Admin Login & Dashboard Access", function() use ($baseUrl, $adminCookie) {
    $r = curl_req($baseUrl . '/proses/auth_process.php?action=login', 'POST', [
        'username' => 'admin',
        'password' => 'admin123'
    ], $adminCookie);

    if ($r['code'] !== 302 || !str_contains($r['headers'], 'admin.php')) {
        return "Admin login failed to redirect to admin.php";
    }

    $r2 = curl_req($baseUrl . '/admin.php', 'GET', [], $adminCookie);
    if ($r2['code'] !== 200 || !str_contains($r2['body'], 'Dashboard Admin')) {
        return "Failed to access admin.php as admin (code: {$r2['code']})";
    }

    return true;
});

// 6. Logout test
test_http("7. Logout clears session", function() use ($baseUrl, $userCookie) {
    $r = curl_req($baseUrl . '/logout.php', 'GET', [], $userCookie);
    if ($r['code'] !== 302) return "Logout should redirect";

    // Now try to access tiket-saya with the same cookie
    $r2 = curl_req($baseUrl . '/tiket-saya.php', 'GET', [], $userCookie);
    if ($r2['code'] !== 302 || !str_contains($r2['headers'], 'login.php')) {
        return "User session was not cleared properly after logout";
    }
    return true;
});

// Clean up cookies
@unlink($userCookie);
@unlink($orgCookie);
@unlink($adminCookie);

echo "\nAll HTTP simulation tests passed successfully!\n";
