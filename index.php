<?php
require_once __DIR__ . '/config/database.php';
require_once __DIR__ . '/includes/auth_check.php';
require_once __DIR__ . '/includes/functions.php';

$pageTitle = 'Tiketin — Temukan Event Favoritmu';
$activeNav = 'home';

$events = [];
$organizerPayments = [];
if (isset($pdo)) {
    try {
        $stmt = $pdo->query("SELECT e.*, u.organization, u.full_name AS organizer_name FROM events e LEFT JOIN users u ON e.owner_id = u.id ORDER BY e.date ASC");
        $events = $stmt->fetchAll();

        // Ambil rekening pembayaran aktif milik seluruh organizer (tabel payment)
        $stmtPay = $pdo->query("SELECT * FROM payment WHERE is_active = 1 ORDER BY id ASC");
        $allPay = $stmtPay->fetchAll();
        foreach ($allPay as $p) {
            $organizerPayments[(int)$p['user_id']][] = [
                'id' => (int)$p['id'],
                'bank_name' => $p['bank_name'],
                'account_number' => $p['account_number'],
                'account_name' => $p['account_name']
            ];
        }
    } catch (Exception $e) {
        $events = [];
        $organizerPayments = [];
    }
}

require_once __DIR__ . '/includes/header.php';
?>

<main>
    <!-- Hero Section -->
    <section class="hero container" id="home">
        <div class="hero-copy">
            <p class="eyebrow">Satu tempat untuk semua momen</p>
            <h1>Temukan event yang bikin kamu <span>nggak sabar.</span></h1>
            <p class="hero-description">
                Pesan tiket konser, wisata, seminar, dan event pilihan dengan cara yang lebih simpel.
            </p>
            <a class="button button-primary" href="#tickets">Jelajahi tiket <span aria-hidden="true">↗</span></a>
        </div>
        <div class="hero-note" aria-label="Informasi singkat Tiketin">
            <span class="hero-note-mark" aria-hidden="true">✦</span>
            <div>
                <strong>Pilihan terkurasi</strong>
                <p>Konser intim sampai festival besar, semuanya jelas dari harga sampai sisa tiket.</p>
            </div>
        </div>
    </section>

    <!-- Ticket Section -->
    <section class="tickets-section container" id="tickets" aria-labelledby="ticketsTitle">
        <div class="section-heading">
            <div>
                <p class="eyebrow">Koleksi tiket</p>
                <h2 id="ticketsTitle">Jelajahi event pilihan.</h2>
                <p class="section-description">
                    Cari berdasarkan nama atau pilih kategori yang kamu mau.
                </p>
            </div>

            <div class="filters" role="search" aria-label="Pencarian dan filter event">
                <input id="eventSearch" type="search" placeholder="Cari nama event..." aria-label="Cari event" />
                <select id="categoryFilter" aria-label="Filter berdasarkan kategori">
                    <option value="">Semua kategori</option>
                    <option value="Konser">Konser</option>
                    <option value="Wisata">Wisata</option>
                    <option value="Seminar">Seminar</option>
                    <option value="Lainnya">Lainnya</option>
                </select>
            </div>
        </div>

        <div class="ticket-grid" id="ticketGrid" aria-live="polite">
            <?php if (empty($events)): ?>
                <p class="empty-state">Belum ada event yang tersedia saat ini.</p>
            <?php else: ?>
                <?php foreach ($events as $event): 
                    $category = $event['category'] ?: 'Lainnya';
                    $catClass = in_array($category, ['Konser', 'Wisata', 'Seminar']) ? strtolower($category) : 'other';
                    $catIcon = $category === 'Konser' ? '♪' : ($category === 'Wisata' ? '◇' : ($category === 'Seminar' ? '▦' : '✦'));
                    $isAvailable = $event['slots'] > 0;
                    $eventTime = $event['time'] ?: '19:00 WIB';
                    $eventLocation = $event['location'] ?: 'Lokasi diumumkan segera';
                ?>
                    <article class="ticket-card" 
                             data-name="<?= htmlspecialchars(strtolower($event['name'])) ?>" 
                             data-category="<?= htmlspecialchars($category) ?>">
                        <div class="card-topline">
                            <span class="category-icon <?= $catClass ?>" aria-hidden="true"><?= $catIcon ?></span>
                            <span class="category-label"><?= htmlspecialchars($category) ?></span>
                        </div>
                        <h3><?= htmlspecialchars($event['name']) ?></h3>
                        <p><?= htmlspecialchars($event['description'] ?: 'Temukan pengalaman seru di event ini.') ?></p>

                        <div class="card-event-info">
                            <div class="info-item">
                                <span class="info-icon" aria-hidden="true">📍</span>
                                <span class="info-text" title="<?= htmlspecialchars($eventLocation) ?>"><?= htmlspecialchars($eventLocation) ?></span>
                            </div>
                            <div class="info-item">
                                <span class="info-icon" aria-hidden="true">🕒</span>
                                <span class="info-text"><?= htmlspecialchars($eventTime) ?></span>
                            </div>
                        </div>

                        <div class="ticket-details">
                            <strong><?= format_rupiah($event['price']) ?></strong>
                            <span class="<?= $isAvailable ? '' : 'sold-out' ?>">
                                <?= $isAvailable ? "{$event['slots']} tiket tersedia" : 'Tiket habis' ?>
                            </span>
                        </div>
                        <div class="card-footer">
                            <span class="event-meta">📅 <?= format_tanggal_id($event['date']) ?></span>
                            <button class="button button-card order-button" 
                                    type="button" 
                                    <?= !$isAvailable ? 'disabled' : '' ?>
                                    onclick="openOrderModal(<?= htmlspecialchars(json_encode([
                                        'id' => (int)$event['id'],
                                        'owner_id' => (int)$event['owner_id'],
                                        'name' => $event['name'],
                                        'price' => (int)$event['price'],
                                        'priceFormatted' => format_rupiah($event['price']),
                                        'slots' => (int)$event['slots'],
                                        'dateFormatted' => format_tanggal_id($event['date']),
                                        'time' => $eventTime,
                                        'location' => $eventLocation,
                                        'category' => $category
                                    ])) ?>)">
                                <?= $isAvailable ? 'Pesan' : 'Habis' ?>
                            </button>
                        </div>
                    </article>
                <?php endforeach; ?>
            <?php endif; ?>
        </div>
        <p id="noMatchesText" class="empty-state" style="display:none;">Tidak ada event yang cocok dengan pencarian.</p>
    </section>
</main>

<!-- Modal Pemesanan Tiket & Pembayaran -->
<div class="modal-backdrop" id="orderModal" role="dialog" aria-modal="true" aria-labelledby="modalTitle" aria-hidden="true">
    <div class="modal">
        <button class="modal-close" id="closeModal" type="button" aria-label="Tutup formulir pemesanan" onclick="closeOrderModal()">×</button>
        <h2 id="modalTitle" style="font-size: 1.6rem; margin: 0 0 16px;">Konfirmasi Pesanan</h2>

        <?php if (!is_logged_in()): ?>
            <div style="margin-bottom: 10px;">
                <h3 id="selectedEvent" style="margin: 0; font-size: 1.25rem; font-weight: 700; color: var(--ink);">-</h3>
            </div>
            <div class="modal-event-chips" style="display:flex; flex-wrap:wrap; gap:8px; margin-bottom: 16px; font-size:0.82rem; color:var(--muted);">
                <span style="background:var(--paper); padding:4px 8px; border-radius:6px; border:1px solid var(--line);">📅 <span id="selectedDate">-</span></span>
                <span style="background:var(--paper); padding:4px 8px; border-radius:6px; border:1px solid var(--line);">🕒 <span id="selectedTime">-</span></span>
                <span style="background:var(--paper); padding:4px 8px; border-radius:6px; border:1px solid var(--line); max-width:100%; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">📍 <span id="selectedLocation">-</span></span>
            </div>
            <p class="modal-details"><span id="selectedPrice">-</span> · <span id="selectedSlots">-</span></p>
            <div class="auth-warning" id="authWarning" style="margin-top: 14px;">
                <p>Kamu perlu login terlebih dahulu untuk memesan tiket event ini.</p>
                <a class="button button-primary" href="login.php">Login sekarang →</a>
            </div>
        <?php elseif ($currentUser['role'] === 'admin'): ?>
            <div style="margin-bottom: 10px;">
                <h3 id="selectedEvent" style="margin: 0; font-size: 1.25rem; font-weight: 700; color: var(--ink);">-</h3>
            </div>
            <div class="modal-event-chips" style="display:flex; flex-wrap:wrap; gap:8px; margin-bottom: 16px; font-size:0.82rem; color:var(--muted);">
                <span style="background:var(--paper); padding:4px 8px; border-radius:6px; border:1px solid var(--line);">📅 <span id="selectedDate">-</span></span>
                <span style="background:var(--paper); padding:4px 8px; border-radius:6px; border:1px solid var(--line);">🕒 <span id="selectedTime">-</span></span>
                <span style="background:var(--paper); padding:4px 8px; border-radius:6px; border:1px solid var(--line); max-width:100%; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">📍 <span id="selectedLocation">-</span></span>
            </div>
            <p class="modal-details"><span id="selectedPrice">-</span> · <span id="selectedSlots">-</span></p>
            <div class="auth-warning" id="authWarning" style="margin-top: 14px;">
                <p>Akun admin tidak dapat melakukan pemesanan tiket acara.</p>
            </div>
        <?php else: ?>
            <!-- Multi-step Form -->
            <form id="orderFormMain" action="proses/order_process.php" method="POST" enctype="multipart/form-data">
                <input type="hidden" name="event_id" id="hiddenEventId" value="" />

                <!-- LANGKAH 1: Kuantitas Tiket -->
                <div id="step1Section">
                    <div style="margin-bottom: 10px;">
                        <h3 id="selectedEvent" style="margin: 0; font-size: 1.25rem; font-weight: 700; color: var(--ink);">-</h3>
                    </div>
                    <div class="modal-event-chips" style="display:flex; flex-wrap:wrap; gap:8px; margin-bottom: 16px; font-size:0.82rem; color:var(--muted);">
                        <span style="background:var(--paper); padding:4px 8px; border-radius:6px; border:1px solid var(--line);">📅 <span id="selectedDate">-</span></span>
                        <span style="background:var(--paper); padding:4px 8px; border-radius:6px; border:1px solid var(--line);">🕒 <span id="selectedTime">-</span></span>
                        <span style="background:var(--paper); padding:4px 8px; border-radius:6px; border:1px solid var(--line); max-width:100%; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">📍 <span id="selectedLocation">-</span></span>
                    </div>
                    <p class="modal-details"><span id="selectedPrice">-</span> · <span id="selectedSlots">-</span></p>

                    <div style="margin-bottom: 16px;">
                        <label for="ticketQuantity">Jumlah tiket (maks. 10 tiket per transaksi)</label>
                        <input id="ticketQuantity" name="quantity" type="number" min="1" max="10" value="1" required />
                    </div>

                    <div class="payment-summary" style="margin-bottom: 16px;">
                        <div class="summary-row">
                            <span>Harga Satuan</span>
                            <span id="previewUnitPrice">-</span>
                        </div>
                        <div class="summary-row summary-total">
                            <span>Estimasi Total</span>
                            <span id="previewTotalPrice">-</span>
                        </div>
                    </div>

                    <p class="form-error" id="formError" role="alert"></p>
                    <button class="button button-submit" type="button" id="nextToPaymentBtn" onclick="goToStep2()">
                        Lanjut ke Pembayaran <span aria-hidden="true">→</span>
                    </button>
                </div>

                <!-- LANGKAH 2: Metode Bayar & Upload Bukti -->
                <div id="step2Section" hidden>
                    <div class="payment-summary">
                        <div class="summary-row">
                            <span>Event</span>
                            <span id="payEventName">-</span>
                        </div>
                        <div class="summary-row">
                            <span>Jadwal &amp; Lokasi</span>
                            <span id="payMeta" style="font-size:0.82rem; text-align:right; max-width:60%; font-weight:600;">-</span>
                        </div>
                        <div class="summary-row">
                            <span>Jumlah</span>
                            <span id="payQuantity">-</span>
                        </div>
                        <div class="summary-row">
                            <span>Harga Satuan</span>
                            <span id="payUnitPrice">-</span>
                        </div>
                        <div class="summary-row summary-total">
                            <span>Total Pembayaran</span>
                            <span id="payTotalPrice">-</span>
                        </div>
                    </div>

                    <div class="form-group" style="margin-top: 16px;">
                        <label for="paymentMethod">Pilih metode pembayaran</label>
                        <select id="paymentMethod" name="payment_method" required onchange="renderPaymentDetail(this)">
                            <option value="">Pilih metode pembayaran</option>
                        </select>
                    </div>

                    <!-- Dynamic Payment Detail Box (Rekening / QRIS) -->
                    <div id="paymentDetailBox" style="margin-top: 14px; margin-bottom: 16px; display: none;"></div>

                    <div class="form-group">
                        <label>Unggah bukti transfer (JPG / PNG / WEBP, maks. 3MB)</label>
                        <div class="upload-area" id="uploadArea" onclick="document.getElementById('proofUpload').click()">
                            <input type="file" id="proofUpload" name="payment_proof" accept="image/jpeg,image/png,image/webp" style="display:none;" onchange="previewProofImage(this)" required />
                            <div class="upload-placeholder" id="uploadPlaceholder">
                                <span class="upload-icon" aria-hidden="true">📁</span>
                                <p>Klik atau pilih file gambar bukti transfer</p>
                            </div>
                            <div class="upload-preview" id="uploadPreview" hidden>
                                <img id="proofImage" src="" alt="Preview Bukti Transfer" />
                                <button type="button" class="button button-small button-quiet" onclick="removeProofImage(event)" style="margin-top: 6px;">Ganti gambar</button>
                            </div>
                        </div>
                    </div>

                    <button class="button button-submit" type="submit" id="confirmPaymentBtn">Konfirmasi &amp; Bayar</button>
                    <button class="button button-quiet" type="button" onclick="goToStep1()" style="width:100%; margin-top:8px;">← Kembali</button>
                </div>
            </form>
        <?php endif; ?>
    </div>
</div>

<script>
let currentEvent = null;

function formatRupiahJs(number) {
    return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(number);
}

// Filter Pencarian & Kategori
const searchInput = document.getElementById('eventSearch');
const categorySelect = document.getElementById('categoryFilter');
const cards = document.querySelectorAll('.ticket-card');
const noMatches = document.getElementById('noMatchesText');

function filterTickets() {
    const query = searchInput.value.trim().toLowerCase();
    const category = categorySelect.value;
    let visibleCount = 0;

    cards.forEach(card => {
        const name = card.dataset.name;
        const cat = card.dataset.category;
        const matchesQuery = !query || name.includes(query);
        const matchesCategory = !category || cat === category;

        if (matchesQuery && matchesCategory) {
            card.style.display = '';
            visibleCount++;
        } else {
            card.style.display = 'none';
        }
    });

    if (noMatches) {
        noMatches.style.display = (visibleCount === 0 && cards.length > 0) ? 'block' : 'none';
    }
}

if (searchInput) searchInput.addEventListener('input', filterTickets);
if (categorySelect) categorySelect.addEventListener('change', filterTickets);

// Modal Handling
const modal = document.getElementById('orderModal');
const qtyInput = document.getElementById('ticketQuantity');
const organizerPaymentAccounts = <?= json_encode($organizerPayments) ?>;

function openOrderModal(eventData) {
    currentEvent = eventData;
    const hiddenId = document.getElementById('hiddenEventId');
    if (hiddenId) hiddenId.value = eventData.id;

    document.querySelectorAll('#selectedEvent').forEach(el => el.textContent = eventData.name);
    document.querySelectorAll('#selectedPrice').forEach(el => el.textContent = eventData.priceFormatted);
    document.querySelectorAll('#selectedSlots').forEach(el => el.textContent = `${eventData.slots} tiket tersedia`);

    document.querySelectorAll('#selectedDate').forEach(el => el.textContent = eventData.dateFormatted || '-');
    document.querySelectorAll('#selectedTime').forEach(el => el.textContent = eventData.time || '19:00 WIB');
    document.querySelectorAll('#selectedLocation').forEach(el => el.textContent = eventData.location || 'Lokasi Acara');

    const elUnitPrice = document.getElementById('previewUnitPrice');
    if (elUnitPrice) elUnitPrice.textContent = eventData.priceFormatted;

    if (qtyInput) {
        qtyInput.max = Math.min(10, eventData.slots);
        qtyInput.value = 1;
        updatePreviewTotal();
    }

    goToStep1();
    if (modal) {
        modal.classList.add('is-open');
        modal.setAttribute('aria-hidden', 'false');
        document.body.classList.add('modal-open');
    }
}

function closeOrderModal() {
    if (modal) {
        modal.classList.remove('is-open');
        modal.setAttribute('aria-hidden', 'true');
        document.body.classList.remove('modal-open');
    }
    removeProofImage();
}

function updatePreviewTotal() {
    if (!currentEvent || !qtyInput) return;
    const qty = parseInt(qtyInput.value) || 1;
    const total = qty * currentEvent.price;
    const el = document.getElementById('previewTotalPrice');
    if (el) el.textContent = formatRupiahJs(total);
}

if (qtyInput) {
    qtyInput.addEventListener('input', updatePreviewTotal);
}

function goToStep1() {
    const s1 = document.getElementById('step1Section');
    const s2 = document.getElementById('step2Section');
    if (s1) s1.hidden = false;
    if (s2) s2.hidden = true;
}

function goToStep2() {
    if (!currentEvent || !qtyInput) return;
    const qty = parseInt(qtyInput.value) || 1;
    if (qty < 1 || qty > currentEvent.slots || qty > 10) {
        alert('Kuantitas tiket tidak valid.');
        return;
    }

    document.getElementById('payEventName').textContent = currentEvent.name;
    const payMeta = document.getElementById('payMeta');
    if (payMeta) {
        payMeta.textContent = `${currentEvent.time || ''} · ${currentEvent.location || ''}`;
    }
    document.getElementById('payQuantity').textContent = qty + ' tiket';
    document.getElementById('payUnitPrice').textContent = currentEvent.priceFormatted;
    const totalPriceFormatted = formatRupiahJs(qty * currentEvent.price);
    document.getElementById('payTotalPrice').textContent = totalPriceFormatted;

    // Populasi metode pembayaran milik organizer event ini
    const sel = document.getElementById('paymentMethod');
    if (sel) {
        sel.innerHTML = '<option value="">Pilih metode pembayaran</option>';
        
        // Pilihan 1: QRIS
        const qrisOpt = document.createElement('option');
        qrisOpt.value = 'qris';
        qrisOpt.textContent = 'QRIS (Gopay / OVO / Dana / ShopeePay / BCA Mobile)';
        sel.appendChild(qrisOpt);

        // Rekening bank milik organizer event ini
        const ownerId = currentEvent.owner_id;
        const accounts = organizerPaymentAccounts[ownerId] || [];
        accounts.forEach(acc => {
            const opt = document.createElement('option');
            opt.value = `bank_${acc.id}`;
            opt.textContent = `Transfer ${acc.bank_name}`;
            opt.dataset.bank = acc.bank_name;
            opt.dataset.acc = acc.account_number;
            opt.dataset.name = acc.account_name;
            sel.appendChild(opt);
        });
    }

    const detailBox = document.getElementById('paymentDetailBox');
    if (detailBox) {
        detailBox.style.display = 'none';
        detailBox.innerHTML = '';
    }

    document.getElementById('step1Section').hidden = true;
    document.getElementById('step2Section').hidden = false;
}

function renderPaymentDetail(selectEl) {
    const detailBox = document.getElementById('paymentDetailBox');
    if (!detailBox) return;

    const val = selectEl.value;
    if (!val) {
        detailBox.style.display = 'none';
        detailBox.innerHTML = '';
        return;
    }

    const qty = parseInt(qtyInput.value) || 1;
    const totalPriceFormatted = formatRupiahJs(qty * currentEvent.price);

    if (val === 'qris') {
        const qrData = `TIKETIN | Event: ${currentEvent.name} | Qty: ${qty} Tiket | Total: ${totalPriceFormatted} | Ref: ${currentEvent.id}-${Date.now().toString().slice(-6)}`;
        const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(qrData)}`;

        detailBox.innerHTML = `
            <div class="qris-card">
                <div class="qris-card-header">
                    <span class="qris-tag">QRIS Standar Nasional</span>
                    <span class="qris-sub">Semua e-wallet &amp; mobile banking</span>
                </div>
                <div class="qris-qr-container">
                    <img src="${qrUrl}" alt="Kode QRIS Tiketin" class="qris-img" />
                </div>
                <div class="qris-caption">
                    <p style="margin:0; font-size: 0.95rem;"><strong>Total Bayar: ${totalPriceFormatted}</strong></p>
                    <p style="margin:4px 0 0; font-size:0.8rem; color:var(--muted); line-height: 1.4;">
                        Pindai kode QRIS di atas untuk menyelesaikan pesanan, lalu simpan tangkapan layar (screenshot) bukti pembayaran untuk diunggah di bawah.
                    </p>
                </div>
            </div>
        `;
        detailBox.style.display = 'block';
    } else {
        const opt = selectEl.options[selectEl.selectedIndex];
        const bankName = opt.dataset.bank || 'Bank Transfer';
        const accNumber = opt.dataset.acc || '-';
        const accName = opt.dataset.name || '-';

        detailBox.innerHTML = `
            <div class="payment-account-card">
                <div class="account-card-top">
                    <span class="account-bank-pill">${bankName}</span>
                    <span class="account-hint">Rekening Penyelenggara Acara</span>
                </div>
                <div class="account-number-group">
                    <span class="account-number-text" id="accNumText">${accNumber}</span>
                    <button type="button" class="button button-small copy-account-button" onclick="copyAccountNumber('${accNumber}', this)">
                        <span class="copy-icon">📋</span> Salin
                    </button>
                </div>
                <div class="account-owner-info">
                    Atas Nama: <strong>${accName}</strong>
                </div>
                <div class="account-guide">
                    💡 Silakan transfer sejumlah <strong>${totalPriceFormatted}</strong> ke rekening di atas, lalu unggah struk / bukti transfer di bawah.
                </div>
            </div>
        `;
        detailBox.style.display = 'block';
    }
}

function copyAccountNumber(text, btn) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => {
            showCopyFeedback(btn);
        }).catch(() => fallbackCopy(text, btn));
    } else {
        fallbackCopy(text, btn);
    }
}

function fallbackCopy(text, btn) {
    const ta = document.createElement('textarea');
    ta.value = text;
    document.body.appendChild(ta);
    ta.select();
    document.execCommand('copy');
    document.body.removeChild(ta);
    showCopyFeedback(btn);
}

function showCopyFeedback(btn) {
    if (!btn) return;
    const oldText = btn.innerHTML;
    btn.innerHTML = '✓ Tersalin!';
    btn.classList.add('copied');
    setTimeout(() => {
        btn.innerHTML = oldText;
        btn.classList.remove('copied');
    }, 2000);
}

function previewProofImage(input) {
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = function(e) {
            document.getElementById('proofImage').src = e.target.result;
            document.getElementById('uploadPlaceholder').hidden = true;
            document.getElementById('uploadPreview').hidden = false;
        };
        reader.readAsDataURL(input.files[0]);
    }
}

function removeProofImage(e) {
    if (e) e.stopPropagation();
    const input = document.getElementById('proofUpload');
    if (input) input.value = '';
    const img = document.getElementById('proofImage');
    if (img) img.src = '';
    const ph = document.getElementById('uploadPlaceholder');
    if (ph) ph.hidden = false;
    const prev = document.getElementById('uploadPreview');
    if (prev) prev.hidden = true;
}

// Close modal on Escape or clicking backdrop
window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && modal && modal.classList.contains('is-open')) {
        closeOrderModal();
    }
});
if (modal) {
    modal.addEventListener('click', (e) => {
        if (e.target === modal) closeOrderModal();
    });
}
</script>

<?php require_once __DIR__ . '/includes/footer.php'; ?>
