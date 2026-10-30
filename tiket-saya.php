<?php
require_once __DIR__ . '/config/database.php';
require_once __DIR__ . '/includes/auth_check.php';
require_once __DIR__ . '/includes/functions.php';

$currentUser = require_login('login.php');
if ($currentUser['role'] === 'admin') {
    header('Location: admin.php');
    exit;
}

$pageTitle = 'Tiket Saya — Tiketin';
$activeNav = 'my-tickets';
$extraCss = ['tiket-saya.css'];

$orders = [];
if (isset($pdo)) {
    try {
        $stmt = $pdo->prepare("
            SELECT o.*, e.name AS event_name, e.date AS event_date, e.time AS event_time, e.location AS event_location, e.category AS event_category,
                   GROUP_CONCAT(t.ticket_code ORDER BY t.id ASC SEPARATOR ', ') AS ticket_codes
            FROM orders o 
            JOIN events e ON o.event_id = e.id 
            LEFT JOIN tickets t ON t.order_id = o.id
            WHERE o.user_id = ? 
            GROUP BY o.id
            ORDER BY o.purchased_at DESC
        ");
        $stmt->execute([$currentUser['id']]);
        $orders = $stmt->fetchAll();
    } catch (Exception $e) {
        $orders = [];
    }
}

$today = date('Y-m-d');
$upcomingOrders = [];
$historyOrders = [];

foreach ($orders as $order) {
    if ($order['event_date'] >= $today) {
        $upcomingOrders[] = $order;
    } else {
        $historyOrders[] = $order;
    }
}

require_once __DIR__ . '/includes/header.php';
?>

<main class="page-main container">
    <div class="page-heading">
        <p class="eyebrow">Pesananmu</p>
        <h1>Tiket Saya.</h1>
        <p>Semua tiket yang kamu pesan tersimpan di sini.</p>
    </div>

    <!-- Tiket Belum Dipakai (Mendatang) -->
    <section class="my-tickets-section" aria-labelledby="upcomingTitle">
        <div class="my-tickets-heading">
            <h2 id="upcomingTitle">Belum dipakai</h2>
            <span id="upcomingCount"><?= count($upcomingOrders) ?> pesanan</span>
        </div>
        <div class="my-tickets-list" id="upcomingList">
            <?php if (empty($upcomingOrders)): ?>
                <div class="my-ticket-empty">
                    <p>Belum ada tiket untuk acara mendatang.</p>
                    <?php if (empty($orders)): ?>
                        <a class="button button-primary" href="index.php#tickets">Jelajahi tiket →</a>
                    <?php endif; ?>
                </div>
            <?php else: ?>
                <?php foreach ($upcomingOrders as $order): 
                    $cat = $order['event_category'] ?: 'Event';
                    $status = $order['status'] ?: 'menunggu';
                    $statusLabel = $status === 'berhasil' ? 'Berhasil' : ($status === 'dibatalkan' ? 'Dibatalkan' : 'Menunggu');
                    $badgeClass = 'badge-' . $status;
                ?>
                    <article class="my-ticket-card">
                        <div class="my-ticket-top">
                            <span class="my-ticket-category"><?= htmlspecialchars($cat) ?></span>
                            <span class="badge <?= $badgeClass ?>"><?= htmlspecialchars($statusLabel) ?></span>
                        </div>
                        <h3><?= htmlspecialchars($order['event_name']) ?></h3>
                        <dl class="my-ticket-details">
                            <div class="my-ticket-detail">
                                <dt>Jadwal acara</dt>
                                <dd><?= format_tanggal_id($order['event_date']) ?><?= !empty($order['event_time']) ? ' · ' . htmlspecialchars($order['event_time']) : '' ?></dd>
                            </div>
                            <div class="my-ticket-detail">
                                <dt>Lokasi acara</dt>
                                <dd><?= htmlspecialchars($order['event_location'] ?: 'Belum ditentukan') ?></dd>
                            </div>
                            <div class="my-ticket-detail">
                                <dt>Jumlah tiket</dt>
                                <dd><?= (int)$order['quantity'] ?> tiket</dd>
                            </div>
                            <div class="my-ticket-detail">
                                <dt>Total harga</dt>
                                <dd><?= format_rupiah($order['total_price']) ?></dd>
                            </div>
                            <div class="my-ticket-detail">
                                <dt>Metode bayar</dt>
                                <dd><?= htmlspecialchars(get_payment_method_label($order['payment_method'])) ?></dd>
                            </div>
                            <div class="my-ticket-detail">
                                <dt>Dipesan</dt>
                                <dd><?= date('d M Y, H:i', strtotime($order['purchased_at'])) ?></dd>
                            </div>
                            <?php if (!empty($order['ticket_codes'])): ?>
                                <div class="my-ticket-detail" style="grid-column: 1 / -1;">
                                    <dt>Kode E-Tiket</dt>
                                    <dd><code><?= htmlspecialchars($order['ticket_codes']) ?></code></dd>
                                </div>
                            <?php endif; ?>
                        </dl>
                    </article>
                <?php endforeach; ?>
            <?php endif; ?>
        </div>
    </section>

    <!-- Riwayat Tiket (Lewat Tanggal) -->
    <section class="my-tickets-section" aria-labelledby="historyTitle">
        <div class="my-tickets-heading">
            <h2 id="historyTitle">Riwayat</h2>
            <span id="historyCount"><?= count($historyOrders) ?> pesanan</span>
        </div>
        <div class="my-tickets-list" id="historyList">
            <?php if (empty($historyOrders)): ?>
                <div class="my-ticket-empty">
                    <p>Belum ada riwayat tiket sebelumnya.</p>
                </div>
            <?php else: ?>
                <?php foreach ($historyOrders as $order): 
                    $cat = $order['event_category'] ?: 'Event';
                    $status = $order['status'] ?: 'menunggu';
                    $statusLabel = $status === 'berhasil' ? 'Selesai' : ($status === 'dibatalkan' ? 'Dibatalkan' : 'Menunggu');
                    $badgeClass = 'badge-' . $status;
                ?>
                    <article class="my-ticket-card">
                        <div class="my-ticket-top">
                            <span class="my-ticket-category"><?= htmlspecialchars($cat) ?></span>
                            <span class="badge <?= $badgeClass ?>"><?= htmlspecialchars($statusLabel) ?></span>
                        </div>
                        <h3><?= htmlspecialchars($order['event_name']) ?></h3>
                        <dl class="my-ticket-details">
                            <div class="my-ticket-detail">
                                <dt>Jadwal acara</dt>
                                <dd><?= format_tanggal_id($order['event_date']) ?><?= !empty($order['event_time']) ? ' · ' . htmlspecialchars($order['event_time']) : '' ?></dd>
                            </div>
                            <div class="my-ticket-detail">
                                <dt>Lokasi acara</dt>
                                <dd><?= htmlspecialchars($order['event_location'] ?: 'Belum ditentukan') ?></dd>
                            </div>
                            <div class="my-ticket-detail">
                                <dt>Jumlah tiket</dt>
                                <dd><?= (int)$order['quantity'] ?> tiket</dd>
                            </div>
                            <div class="my-ticket-detail">
                                <dt>Total harga</dt>
                                <dd><?= format_rupiah($order['total_price']) ?></dd>
                            </div>
                            <div class="my-ticket-detail">
                                <dt>Metode bayar</dt>
                                <dd><?= htmlspecialchars(get_payment_method_label($order['payment_method'])) ?></dd>
                            </div>
                            <div class="my-ticket-detail">
                                <dt>Dipesan</dt>
                                <dd><?= date('d M Y, H:i', strtotime($order['purchased_at'])) ?></dd>
                            </div>
                            <?php if (!empty($order['ticket_codes'])): ?>
                                <div class="my-ticket-detail" style="grid-column: 1 / -1;">
                                    <dt>Kode E-Tiket</dt>
                                    <dd><code><?= htmlspecialchars($order['ticket_codes']) ?></code></dd>
                                </div>
                            <?php endif; ?>
                        </dl>
                    </article>
                <?php endforeach; ?>
            <?php endif; ?>
        </div>
    </section>
</main>

<?php require_once __DIR__ . '/includes/footer.php'; ?>
