<?php
require_once __DIR__ . '/config/database.php';
require_once __DIR__ . '/includes/auth_check.php';
require_once __DIR__ . '/includes/functions.php';

$currentUser = require_role('organizer', 'login.php');

$pageTitle = 'Dashboard Organizer — Tiketin';
$activeNav = '';
$bodyDataDashboard = 'organizer';

$myEvents = [];
$myOrders = [];
$totalSlots = 0;

if (isset($pdo)) {
    try {
        // Ambil event milik organizer
        $stmtEvents = $pdo->prepare("SELECT * FROM events WHERE owner_id = ? ORDER BY date ASC");
        $stmtEvents->execute([$currentUser['id']]);
        $myEvents = $stmtEvents->fetchAll();

        foreach ($myEvents as $evt) {
            $totalSlots += (int)$evt['slots'];
        }

        // Ambil order untuk event milik organizer
        $stmtOrders = $pdo->prepare("
            SELECT o.*, u.full_name AS buyer_name, u.username AS buyer_username, e.name AS event_name 
            FROM orders o 
            JOIN events e ON o.event_id = e.id 
            JOIN users u ON o.user_id = u.id 
            WHERE e.owner_id = ? 
            ORDER BY o.purchased_at DESC
        ");
        $stmtOrders->execute([$currentUser['id']]);
        $myOrders = $stmtOrders->fetchAll();

        // Ambil rekening pembayaran milik organizer (tabel payment)
        $stmtPayments = $pdo->prepare("
            SELECT * FROM payment 
            WHERE user_id = ? 
            ORDER BY id ASC
        ");
        $stmtPayments->execute([$currentUser['id']]);
        $myPayments = $stmtPayments->fetchAll();
    } catch (Exception $e) {
        $myEvents = [];
        $myOrders = [];
        $myPayments = [];
    }
}

require_once __DIR__ . '/includes/header.php';
?>

<main class="page-main container">
    <div class="page-heading">
        <p class="eyebrow">Ruang organizer</p>
        <h1>Event milikmu.</h1>
        <p>Tambah dan perbarui acara yang kamu kelola. Event baru langsung tampil di halaman utama.</p>
    </div>

    <!-- Ringkasan Statistik -->
    <div class="dashboard-summary" id="dashboardSummary">
        <div class="summary-item">
            <strong><?= count($myEvents) ?></strong>
            <span>Event milikmu</span>
        </div>
        <div class="summary-item">
            <strong><?= number_format($totalSlots, 0, ',', '.') ?></strong>
            <span>Sisa tiket</span>
        </div>
        <div class="summary-item">
            <strong><?= count($myOrders) ?></strong>
            <span>Order masuk</span>
        </div>
        <div class="summary-item">
            <strong><?= count($myPayments) ?></strong>
            <span>Rekening aktif</span>
        </div>
    </div>

    <div class="dashboard-layout">
        <!-- Form Tambah / Edit Event -->
        <section class="panel dashboard-section" aria-labelledby="formTitle">
            <h2 id="eventFormTitle">Tambah event</h2>
            <p id="eventError" class="form-error"></p>
            
            <form id="eventForm" action="proses/event_process.php" method="POST">
                <input type="hidden" name="action" id="formAction" value="create" />
                <input type="hidden" name="event_id" id="formEventId" value="" />

                <div class="form-field">
                    <label for="eventName">Nama event</label>
                    <input id="eventName" name="name" required placeholder="Contoh: Senja Suara Festival" />
                </div>

                <div class="form-grid">
                    <div class="form-field">
                        <label for="eventDate">Tanggal</label>
                        <input id="eventDate" name="date" type="date" required />
                    </div>
                    <div class="form-field">
                        <label for="eventTime">Jam mulai acara</label>
                        <input id="eventTime" name="time" type="text" placeholder="Contoh: 19:00 WIB" required />
                    </div>
                </div>

                <div class="form-grid">
                    <div class="form-field">
                        <label for="eventLocation">Lokasi acara</label>
                        <input id="eventLocation" name="location" type="text" placeholder="Contoh: Balai Sarbini, Jakarta" required />
                    </div>
                    <div class="form-field">
                        <label for="eventCategory">Kategori</label>
                        <select id="eventCategory" name="category">
                            <option value="Konser">Konser</option>
                            <option value="Wisata">Wisata</option>
                            <option value="Seminar">Seminar</option>
                            <option value="Lainnya">Lainnya</option>
                        </select>
                    </div>
                </div>

                <div class="form-grid">
                    <div class="form-field">
                        <label for="eventPrice">Harga tiket (Rp)</label>
                        <input id="eventPrice" name="price" type="number" min="0" step="1000" placeholder="150000" required />
                    </div>
                    <div class="form-field">
                        <label for="eventSlots">Kuota tiket</label>
                        <input id="eventSlots" name="slots" type="number" min="0" placeholder="100" required />
                    </div>
                </div>

                <div class="form-field">
                    <label for="eventDescription">Deskripsi singkat</label>
                    <textarea id="eventDescription" name="description" rows="3" placeholder="Ceritakan keseruan dan detail acaramu..."></textarea>
                </div>

                <div class="form-actions">
                    <button class="button button-submit" id="eventSubmit" type="submit">Simpan event</button>
                    <button class="button button-quiet" id="cancelEdit" type="button" style="display:none;" onclick="resetEventForm()">Batal</button>
                </div>
            </form>
        </section>

        <!-- Daftar Event Terdaftar -->
        <section class="panel dashboard-section" aria-labelledby="eventsTitle">
            <div class="panel-heading">
                <h2 id="eventsTitle">Event terdaftar</h2>
                <span id="eventCount"><?= count($myEvents) ?> event</span>
            </div>
            
            <div class="management-list" id="eventList">
                <?php if (empty($myEvents)): ?>
                    <p class="empty-state">Belum ada event. Buat event pertamamu melalui form di sebelah kiri.</p>
                <?php else: ?>
                    <?php foreach ($myEvents as $event): ?>
                        <article class="management-row">
                            <div class="management-copy">
                                <h3><?= htmlspecialchars($event['name']) ?></h3>
                                <p>
                                    📅 <?= format_tanggal_id($event['date']) ?><?= !empty($event['time']) ? ' · 🕒 ' . htmlspecialchars($event['time']) : '' ?> · 
                                    <?= format_rupiah($event['price']) ?> · 
                                    <strong><?= (int)$event['slots'] ?></strong> slot tersisa
                                </p>
                                <?php if (!empty($event['location'])): ?>
                                    <p style="font-size:0.83rem; color:var(--muted); margin-top:2px;">📍 <?= htmlspecialchars($event['location']) ?></p>
                                <?php endif; ?>
                            </div>
                            <div class="row-actions">
                                <button class="button button-quiet" type="button" onclick="editEvent(<?= htmlspecialchars(json_encode($event)) ?>)">Edit</button>
                                <a class="button button-danger" 
                                   href="proses/event_process.php?action=delete&id=<?= (int)$event['id'] ?>" 
                                   onclick="return confirm('Yakin ingin menghapus event &quot;<?= htmlspecialchars(addslashes($event['name'])) ?>&quot;? Tindakan ini tidak bisa dibatalkan.')">
                                    Hapus
                                </a>
                            </div>
                        </article>
                    <?php endforeach; ?>
                <?php endif; ?>
            </div>
        </section>
    </div>

    <!-- Section Rekening Pembayaran Organizer -->
    <section class="panel dashboard-section" id="rekeningSection" style="margin-top: 32px;" aria-labelledby="paymentTitle">
        <div class="panel-heading">
            <div>
                <h2 id="paymentTitle">Rekening pembayaran</h2>
                <p style="margin: 4px 0 0; font-size: 0.88rem; color: var(--muted);">
                    Rekening ini otomatis ditampilkan kepada pembeli saat mereka memilih metode pembayaran transfer bank pada event milikmu.
                </p>
            </div>
            <span id="paymentCount"><?= count($myPayments) ?> rekening aktif</span>
        </div>

        <div class="dashboard-layout" style="margin-top: 20px;">
            <!-- Form Tambah Rekening -->
            <div style="background: var(--paper); padding: 22px; border-radius: 14px; border: 1px solid var(--line);">
                <h3 style="margin-top:0; font-size: 1.15rem; margin-bottom: 14px;">Tambah rekening baru</h3>
                <form action="proses/payment_process.php" method="POST">
                    <input type="hidden" name="action" value="add" />
                    
                    <div class="form-field" style="margin-bottom: 12px;">
                        <label for="bankSelect">Pilih Bank</label>
                        <select id="bankSelect" name="bank_name" required onchange="toggleCustomBank(this.value)">
                            <option value="Bank BCA">Bank BCA</option>
                            <option value="Bank BRI">Bank BRI</option>
                            <option value="Bank Mandiri">Bank Mandiri</option>
                            <option value="Bank BNI">Bank BNI</option>
                            <option value="Bank BSI">Bank BSI (Syariah)</option>
                            <option value="Bank CIMB Niaga">Bank CIMB Niaga</option>
                            <option value="Bank Permata">Bank Permata</option>
                            <option value="Lainnya">Bank Lainnya...</option>
                        </select>
                    </div>

                    <div class="form-field" id="customBankWrapper" style="margin-bottom: 12px; display: none;">
                        <label for="customBankInput">Nama Bank Kustom</label>
                        <input id="customBankInput" name="custom_bank" placeholder="Contoh: Bank Jago / SeaBank" />
                    </div>

                    <div class="form-field" style="margin-bottom: 12px;">
                        <label for="accountNumber">Nomor Rekening</label>
                        <input id="accountNumber" name="account_number" type="text" placeholder="Contoh: 8820192831" required />
                    </div>

                    <div class="form-field" style="margin-bottom: 18px;">
                        <label for="accountName">Atas Nama (Nama Pemilik)</label>
                        <input id="accountName" name="account_name" type="text" placeholder="Contoh: Senja Suara Creative" required />
                    </div>

                    <button class="button button-submit" type="submit" style="margin-top:0;">Simpan rekening</button>
                </form>
            </div>

            <!-- Daftar Rekening Terdaftar -->
            <div>
                <h3 style="margin-top:0; font-size: 1.15rem; margin-bottom: 14px;">Daftar rekening aktif</h3>
                <?php if (empty($myPayments)): ?>
                    <p class="empty-state">Belum ada rekening pembayaran. Daftarkan rekening bank di sebelah kiri agar pembeli dapat melakukan transfer.</p>
                <?php else: ?>
                    <div class="management-list">
                        <?php foreach ($myPayments as $pay): ?>
                            <article class="management-row" style="align-items: center;">
                                <div class="management-copy">
                                    <div style="display:flex; align-items:center; gap:8px;">
                                        <span class="badge badge-berhasil" style="font-weight:700;"><?= htmlspecialchars($pay['bank_name']) ?></span>
                                        <strong style="font-size: 1.05rem; letter-spacing: 0.03em; font-family: monospace;"><?= htmlspecialchars($pay['account_number']) ?></strong>
                                    </div>
                                    <p style="margin-top: 4px; font-size: 0.85rem; color: var(--muted);">
                                        a.n <strong><?= htmlspecialchars($pay['account_name']) ?></strong>
                                    </p>
                                </div>
                                <div class="row-actions">
                                    <a class="button button-danger button-small" 
                                       href="proses/payment_process.php?action=delete&id=<?= (int)$pay['id'] ?>"
                                       onclick="return confirm('Hapus rekening <?= htmlspecialchars(addslashes($pay['bank_name'])) ?> (<?= htmlspecialchars(addslashes($pay['account_number'])) ?>)?')">
                                        Hapus
                                    </a>
                                </div>
                            </article>
                        <?php endforeach; ?>
                    </div>
                <?php endif; ?>
            </div>
        </div>
    </section>

    <!-- Section Order Masuk -->
    <section class="panel dashboard-section" style="margin-top: 32px;" aria-labelledby="ordersTitle">
        <div class="panel-heading">
            <h2 id="ordersTitle">Order masuk</h2>
            <span id="orderCount"><?= count($myOrders) ?> order</span>
        </div>

        <div class="order-filter">
            <label for="orderEventFilter">Filter Event:</label>
            <select id="orderEventFilter" onchange="filterOrders()">
                <option value="">Semua event</option>
                <?php foreach ($myEvents as $evt): 
                    $cnt = count(array_filter($myOrders, fn($o) => (int)$o['event_id'] === (int)$evt['id']));
                    if ($cnt === 0) continue;
                ?>
                    <option value="<?= (int)$evt['id'] ?>"><?= htmlspecialchars($evt['name']) ?> (<?= $cnt ?>)</option>
                <?php endforeach; ?>
            </select>
        </div>

        <div class="order-list" id="orderList">
            <?php if (empty($myOrders)): ?>
                <p class="empty-state">Belum ada order masuk untuk event kamu.</p>
            <?php else: ?>
                <?php foreach ($myOrders as $order): 
                    $buyer = $order['buyer_name'] ?: $order['buyer_username'];
                    $statusText = $order['status'] ?: 'menunggu';
                    $statusClass = 'badge-' . $statusText;
                    $methodLabel = get_payment_method_label($order['payment_method']);
                ?>
                    <article class="order-card" data-event-id="<?= (int)$order['event_id'] ?>">
                        <div class="order-header">
                            <span class="order-buyer"><?= htmlspecialchars($buyer) ?> <small style="color:var(--muted);">(#<?= htmlspecialchars($order['order_code']) ?>)</small></span>
                            <span class="badge <?= $statusClass ?>"><?= htmlspecialchars($statusText) ?></span>
                        </div>
                        <div class="order-details">
                            <div class="order-detail">
                                <span class="order-detail-label">Event</span>
                                <span class="order-detail-value"><?= htmlspecialchars($order['event_name']) ?></span>
                            </div>
                            <div class="order-detail">
                                <span class="order-detail-label">Jumlah</span>
                                <span class="order-detail-value"><?= (int)$order['quantity'] ?> tiket</span>
                            </div>
                            <div class="order-detail">
                                <span class="order-detail-label">Total</span>
                                <span class="order-detail-value"><?= format_rupiah($order['total_price']) ?></span>
                            </div>
                            <div class="order-detail">
                                <span class="order-detail-label">Metode</span>
                                <span class="order-detail-value"><?= htmlspecialchars($methodLabel) ?></span>
                            </div>
                            <div class="order-detail">
                                <span class="order-detail-label">Dipesan</span>
                                <span class="order-detail-value"><?= date('d M Y, H:i', strtotime($order['purchased_at'])) ?></span>
                            </div>
                        </div>

                        <div class="order-actions">
                            <?php if (!empty($order['payment_proof'])): ?>
                                <button class="button button-quiet button-small" type="button" onclick="viewProof('uploads/proofs/<?= htmlspecialchars($order['payment_proof']) ?>')">
                                    Lihat bukti
                                </button>
                            <?php endif; ?>

                            <?php if ($statusText === 'menunggu'): ?>
                                <form action="proses/event_process.php" method="POST" style="display:inline-block;">
                                    <input type="hidden" name="action" value="update_order_status" />
                                    <input type="hidden" name="order_id" value="<?= (int)$order['id'] ?>" />
                                    <input type="hidden" name="status" value="berhasil" />
                                    <button class="button button-primary button-small" type="submit">Terima</button>
                                </form>

                                <form action="proses/event_process.php" method="POST" style="display:inline-block;" onsubmit="return confirm('Tolak pesanan ini dan kembalikan slot tiket?')">
                                    <input type="hidden" name="action" value="update_order_status" />
                                    <input type="hidden" name="order_id" value="<?= (int)$order['id'] ?>" />
                                    <input type="hidden" name="status" value="dibatalkan" />
                                    <button class="button button-danger button-small" type="submit">Tolak</button>
                                </form>
                            <?php endif; ?>
                        </div>
                    </article>
                <?php endforeach; ?>
            <?php endif; ?>
        </div>
    </section>
</main>

<!-- Modal Lihat Bukti Transfer -->
<div class="modal-backdrop" id="proofModal" role="dialog" aria-modal="true" aria-hidden="true" onclick="closeProofModal()">
    <div class="modal" style="max-width:500px;" onclick="event.stopPropagation()">
        <button class="modal-close" type="button" aria-label="Tutup pratinjau bukti transfer" onclick="closeProofModal()">×</button>
        <p class="eyebrow">Verifikasi Transaksi</p>
        <h2>Bukti Pembayaran.</h2>
        <div style="text-align:center; padding:16px 0;">
            <img id="modalProofImg" class="proof-modal-image" src="" alt="Bukti Transfer" />
        </div>
        <button class="button button-quiet" type="button" onclick="closeProofModal()" style="width:100%; margin-top:8px;">Tutup</button>
    </div>
</div>

<script>
function editEvent(event) {
    document.getElementById('formAction').value = 'update';
    document.getElementById('formEventId').value = event.id;
    document.getElementById('eventName').value = event.name;
    document.getElementById('eventDate').value = event.date;
    document.getElementById('eventTime').value = event.time || '';
    document.getElementById('eventLocation').value = event.location || '';
    document.getElementById('eventCategory').value = event.category;
    document.getElementById('eventPrice').value = event.price;
    document.getElementById('eventSlots').value = event.slots;
    document.getElementById('eventDescription').value = event.description || '';

    document.getElementById('eventFormTitle').textContent = `Edit ${event.name}`;
    document.getElementById('eventSubmit').textContent = 'Simpan perubahan';
    document.getElementById('cancelEdit').style.display = 'inline-block';

    const form = document.getElementById('eventForm');
    form.scrollIntoView({ behavior: 'smooth', block: 'center' });
    document.getElementById('eventName').focus();
}

function resetEventForm() {
    const form = document.getElementById('eventForm');
    form.reset();
    document.getElementById('formAction').value = 'create';
    document.getElementById('formEventId').value = '';
    document.getElementById('eventFormTitle').textContent = 'Tambah event';
    document.getElementById('eventSubmit').textContent = 'Simpan event';
    document.getElementById('cancelEdit').style.display = 'none';
}

function filterOrders() {
    const filterVal = document.getElementById('orderEventFilter').value;
    const cards = document.querySelectorAll('.order-card');
    let visibleCount = 0;

    cards.forEach(card => {
        const evtId = card.dataset.eventId;
        if (!filterVal || evtId === filterVal) {
            card.style.display = '';
            visibleCount++;
        } else {
            card.style.display = 'none';
        }
    });

    document.getElementById('orderCount').textContent = visibleCount + ' order';
}

function toggleCustomBank(val) {
    const wrap = document.getElementById('customBankWrapper');
    if (wrap) {
        wrap.style.display = val === 'Lainnya' ? 'block' : 'none';
        const inp = document.getElementById('customBankInput');
        if (inp && val === 'Lainnya') inp.focus();
    }
}

function viewProof(src) {
    const modal = document.getElementById('proofModal');
    const img = document.getElementById('modalProofImg');
    if (modal && img) {
        img.src = src;
        modal.classList.add('is-open');
        modal.setAttribute('aria-hidden', 'false');
    }
}

function closeProofModal() {
    const modal = document.getElementById('proofModal');
    if (modal) {
        modal.classList.remove('is-open');
        modal.setAttribute('aria-hidden', 'true');
    }
}
</script>

<?php require_once __DIR__ . '/includes/footer.php'; ?>
