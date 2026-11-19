<?php
require_once __DIR__ . '/config/database.php';
require_once __DIR__ . '/includes/auth_check.php';
require_once __DIR__ . '/includes/functions.php';

$pageTitle = 'Tiketin — Temukan Event Favoritmu';
$activeNav = 'home';

$events = [];
$organizerPayments = [];
$eventNow = new DateTimeImmutable('now', new DateTimeZone('Asia/Jakarta'));
if (isset($pdo)) {
    try {
        $stmt = $pdo->query("SELECT e.*, u.organization, u.full_name AS organizer_name FROM events e LEFT JOIN users u ON e.owner_id = u.id ORDER BY e.date ASC");
        $events = filter_and_sort_active_events($stmt->fetchAll(), $eventNow);

        // Peta rekening aktif per organizer biar langsung tampil di modal bayar
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

    <?php
    $categoryOrder = ['Konser', 'Wisata', 'Seminar', 'Lainnya'];
    $categoryCounts = array_fill_keys($categoryOrder, 0);
    foreach ($events as $ev) {
        $evCat = $ev['category'] ?: 'Lainnya';
        if (!array_key_exists($evCat, $categoryCounts)) {
            $evCat = 'Lainnya';
        }
        $categoryCounts[$evCat]++;
    }
    $totalEvents = count($events);
    ?>
    <section class="tickets-section container" id="tickets" aria-labelledby="ticketsTitle">
        <div class="section-heading">
            <div class="section-copy">
                <p class="eyebrow">Koleksi tiket</p>
                <h2 id="ticketsTitle">Jelajahi event pilihan.</h2>
                <p class="section-intro">
                    Cari berdasarkan nama atau lokasi, lalu pilih kategori yang kamu mau.
                </p>
            </div>

            <div class="section-search" role="search" aria-label="Pencarian event">
                <label class="sr-only" for="eventSearch">Cari nama atau lokasi event</label>
                <div class="search-field">
                    <svg class="search-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <circle cx="11" cy="11" r="8"></circle>
                        <path d="m21 21-4.3-4.3"></path>
                    </svg>
                    <input id="eventSearch" type="search" placeholder="Cari nama atau lokasi event..." autocomplete="off" />
                    <button type="button" id="clearSearchBtn" class="search-clear" aria-label="Hapus kata kunci pencarian" hidden>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M18 6 6 18"></path>
                            <path d="m6 6 12 12"></path>
                        </svg>
                    </button>
                </div>
            </div>
        </div>

        <div class="ticket-toolbar">
            <div class="category-chips" id="categoryChips" role="group" aria-label="Filter berdasarkan kategori">
                <button type="button"
                        class="filter-chip is-active"
                        data-category=""
                        aria-pressed="true"
                        aria-label="Semua kategori, <?= $totalEvents ?> event">
                    <span class="chip-label">Semua</span>
                    <span class="chip-count" aria-hidden="true"><?= $totalEvents ?></span>
                </button>
                <?php foreach ($categoryOrder as $catName): $catTotal = $categoryCounts[$catName]; ?>
                    <button type="button"
                            class="filter-chip<?= $catTotal === 0 ? ' is-zero' : '' ?>"
                            data-category="<?= htmlspecialchars($catName) ?>"
                            aria-pressed="false"
                            aria-label="<?= htmlspecialchars($catName) ?>, <?= $catTotal ?> event">
                        <span class="chip-label"><?= htmlspecialchars($catName) ?></span>
                        <span class="chip-count" aria-hidden="true"><?= $catTotal ?></span>
                    </button>
                <?php endforeach; ?>
            </div>

            <div class="results-meta">
                <p class="results-summary" id="resultsSummary" role="status" aria-live="polite" aria-atomic="true">
                    Menampilkan <strong><?= $totalEvents ?></strong> event
                </p>
                <button type="button" id="inlineResetBtn" class="results-reset" hidden>Reset filter</button>
            </div>
        </div>

        <div class="ticket-grid" id="ticketGrid">
            <?php if (empty($events)): ?>
                <p class="empty-state">Belum ada event yang tersedia saat ini.</p>
            <?php else: ?>
                <?php foreach ($events as $event): 
                    $rawCategory = $event['category'] ?: 'Lainnya';
                    $category = $rawCategory;
                    $filterCategory = in_array($rawCategory, ['Konser', 'Wisata', 'Seminar'], true) ? $rawCategory : 'Lainnya';
                    $catKey = normalize_event_category_key($category);
                    $slots = (int)$event['slots'];
                    $isAvailable = $slots > 0;
                    $ticketStatusClass = !$isAvailable ? 'sold-out' : ($slots <= 10 ? 'low-stock' : '');
                    $ticketStatusText = !$isAvailable ? 'Tiket habis' : ($slots <= 10 ? "Sisa {$slots} tiket" : "{$slots} tiket tersedia");
                    $eventTime = $event['time'] ?: '19:00 WIB';
                    $eventLocation = $event['location'] ?: 'Lokasi diumumkan segera';
                    $imageUrl = resolve_event_image_url($event['image_path'] ?? null);
                    $fallbackSvgUrl = get_category_svg_data_uri($category);
                ?>
                    <article class="ticket-card has-media<?= !$isAvailable ? ' is-sold-out' : '' ?>" 
                             data-name="<?= htmlspecialchars(strtolower($event['name'])) ?>" 
                             data-location="<?= htmlspecialchars(strtolower($eventLocation)) ?>"
                             data-category="<?= htmlspecialchars($filterCategory) ?>">
                        <div class="card-media-16x9 <?= $imageUrl ? 'is-zoomable' : 'is-fallback' ?>"
                             <?php if ($imageUrl): ?>
                             role="button"
                             tabindex="0"
                             aria-label="Perbesar foto acara <?= htmlspecialchars($event['name']) ?>"
                             onclick="openImageLightbox(<?= htmlspecialchars(json_encode($imageUrl)) ?>, <?= htmlspecialchars(json_encode($event['name'])) ?>)"
                             onkeydown="if(event.key==='Enter'||event.key===' '){event.preventDefault();openImageLightbox(<?= htmlspecialchars(json_encode($imageUrl)) ?>, <?= htmlspecialchars(json_encode($event['name'])) ?>);}"
                             <?php endif; ?>>
                            <?php if ($imageUrl): ?>
                                <img src="<?= htmlspecialchars($imageUrl) ?>" alt="Foto acara <?= htmlspecialchars($event['name']) ?>" loading="lazy" />
                            <?php else: ?>
                                <?= render_event_category_svg($category) ?>
                            <?php endif; ?>
                            <div class="media-floating-top">
                                <span class="media-cat-pill cat-<?= $catKey ?>">
                                    <span class="cat-dot dot-<?= $catKey ?>" aria-hidden="true"></span>
                                    <span><?= htmlspecialchars($category) ?></span>
                                </span>
                                <?php if (!$isAvailable): ?>
                                    <span class="media-status-pill is-sold-out">Habis</span>
                                <?php elseif ($slots <= 10): ?>
                                    <span class="media-status-pill is-low-stock">Sisa <?= $slots ?> tiket</span>
                                <?php endif; ?>
                            </div>
                            <?php if ($imageUrl): ?>
                                <span class="media-zoom-badge" aria-hidden="true">
                                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="11" y1="8" x2="11" y2="14"/><line x1="8" y1="11" x2="14" y2="11"/></svg>
                                    <span>Perbesar</span>
                                </span>
                            <?php endif; ?>
                        </div>

                        <div class="ticket-card-body">
                            <h3><?= htmlspecialchars($event['name']) ?></h3>
                            <p><?= htmlspecialchars($event['description'] ?: 'Temukan pengalaman seru di event ini.') ?></p>

                            <div class="card-event-info">
                                <div class="info-item">
                                    <span class="info-icon"><?= render_card_meta_icon('map-pin', 14) ?></span>
                                    <span class="info-text" title="<?= htmlspecialchars($eventLocation) ?>"><?= htmlspecialchars($eventLocation) ?></span>
                                </div>
                                <div class="info-item">
                                    <span class="info-icon"><?= render_card_meta_icon('clock', 14) ?></span>
                                    <span class="info-text"><?= htmlspecialchars($eventTime) ?></span>
                                </div>
                            </div>

                            <div class="ticket-details">
                                <strong><?= format_rupiah($event['price']) ?></strong>
                                <span class="<?= $ticketStatusClass ?>">
                                    <?= htmlspecialchars($ticketStatusText) ?>
                                </span>
                            </div>
                            <div class="card-footer">
                                <span class="event-meta"><?= render_card_meta_icon('calendar', 14) ?><span><?= format_tanggal_id($event['date']) ?></span></span>
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
                                            'category' => $category,
                                            'imageUrl' => $imageUrl,
                                            'fallbackSvgUrl' => $fallbackSvgUrl
                                        ])) ?>)">
                                    <?= $isAvailable ? 'Pesan' : 'Habis' ?>
                                </button>
                            </div>
                        </div>
                    </article>
                <?php endforeach; ?>
            <?php endif; ?>
        </div>

        <div id="noMatchesBox" class="catalog-empty-state" hidden>
            <div class="catalog-empty-icon" aria-hidden="true">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="11" cy="11" r="8"></circle>
                    <path d="m21 21-4.3-4.3"></path>
                    <path d="M8 11h6"></path>
                </svg>
            </div>
            <h3>Event tidak ditemukan</h3>
            <p id="noMatchesText">Tidak ada event yang cocok dengan pencarianmu saat ini.</p>
            <button type="button" class="button button-quiet" id="resetFiltersBtn">Reset pencarian &amp; filter</button>
        </div>
    </section>
</main>

<div class="modal-backdrop" id="orderModal" role="dialog" aria-modal="true" aria-labelledby="modalTitle" aria-hidden="true">
    <div class="modal">
        <button class="modal-close" id="closeModal" type="button" aria-label="Tutup formulir pemesanan" onclick="closeOrderModal()">×</button>
        <h2 id="modalTitle" style="font-size: 1.6rem; margin: 0 0 16px;">Konfirmasi Pesanan</h2>

        <div class="modal-event-media" id="modalEventMedia" hidden>
            <img id="modalEventMediaImg" src="" alt="Foto acara" />
            <span class="media-zoom-chip" id="modalEventZoomChip" hidden>Klik untuk perbesar</span>
        </div>

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
            <form id="orderFormMain" action="proses/order_process.php" method="POST" enctype="multipart/form-data">
                <input type="hidden" name="event_id" id="hiddenEventId" value="" />

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

                    <div id="paymentDetailBox" style="margin-top: 14px; margin-bottom: 16px; display: none;"></div>

                    <div class="form-group">
                        <label>Unggah bukti transfer (JPG / PNG / WEBP, maks. 5MB)</label>
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

function escapeHtml(str) {
    return String(str ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function buildFallbackQrSvgDataUri() {
    const size = 21;
    const rects = [];
    const inFinder = (r, c, top, left) => {
        const dr = r - top;
        const dc = c - left;
        if (dr < 0 || dr > 6 || dc < 0 || dc > 6) return null;
        if (dr === 0 || dr === 6 || dc === 0 || dc === 6) return true;
        if (dr >= 2 && dr <= 4 && dc >= 2 && dc <= 4) return true;
        return false;
    };
    for (let r = 0; r < size; r++) {
        for (let c = 0; c < size; c++) {
            const f1 = inFinder(r, c, 0, 0);
            const f2 = inFinder(r, c, 0, size - 7);
            const f3 = inFinder(r, c, size - 7, 0);
            let dark = f1 !== null ? f1 : (f2 !== null ? f2 : (f3 !== null ? f3 : false));
            if (f1 === null && f2 === null && f3 === null) {
                if ((r === 7 && c < 8) || (c === 7 && r < 8) || (r === 7 && c >= size - 8) || (c === size - 8 && r < 8) || (r === size - 8 && c < 8) || (c === 7 && r >= size - 8)) {
                    dark = false;
                } else {
                    dark = ((r * 7 + c * 11 + r * c) % 3 === 0) || (r === 6 && c % 2 === 0) || (c === 6 && r % 2 === 0);
                }
            }
            if (dark) {
                rects.push(`<rect x="${c + 2}" y="${r + 2}" width="1" height="1" fill="#20252a"/>`);
            }
        }
    }
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 25 25" shape-rendering="crispEdges"><rect width="25" height="25" fill="#ffffff"/>${rects.join('')}</svg>`;
    return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg);
}

function formatRupiahJs(number) {
    return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(number);
}

const searchInput = document.getElementById('eventSearch');
const clearSearchBtn = document.getElementById('clearSearchBtn');
const chipButtons = document.querySelectorAll('#categoryChips .filter-chip');
const cards = document.querySelectorAll('.ticket-card');
const resultsSummary = document.getElementById('resultsSummary');
const inlineResetBtn = document.getElementById('inlineResetBtn');
const noMatchesBox = document.getElementById('noMatchesBox');
const noMatchesText = document.getElementById('noMatchesText');
const resetFiltersBtn = document.getElementById('resetFiltersBtn');
let activeCategory = '';

function updateChipStates() {
    chipButtons.forEach(btn => {
        const isActive = (btn.dataset.category || '') === activeCategory;
        btn.classList.toggle('is-active', isActive);
        btn.setAttribute('aria-pressed', isActive ? 'true' : 'false');
    });
}

function updateResultsSummary(visibleCount, totalCount, isFiltered) {
    if (resultsSummary) {
        resultsSummary.replaceChildren();
        resultsSummary.append('Menampilkan ');
        const strong = document.createElement('strong');
        strong.textContent = String(visibleCount);
        resultsSummary.append(strong);
        resultsSummary.append(isFiltered ? ` dari ${totalCount} event` : ' event');
    }
    if (inlineResetBtn) {
        inlineResetBtn.hidden = !isFiltered;
    }
}

function updateEmptyStateMessage(rawQuery, category) {
    if (!noMatchesText) return;
    if (rawQuery && category) {
        noMatchesText.textContent = `Tidak ada event "${rawQuery}" dalam kategori ${category}.`;
    } else if (rawQuery) {
        noMatchesText.textContent = `Tidak ada event atau lokasi yang cocok dengan "${rawQuery}".`;
    } else if (category) {
        noMatchesText.textContent = `Belum ada event untuk kategori ${category} saat ini.`;
    } else {
        noMatchesText.textContent = 'Tidak ada event yang cocok dengan pencarianmu saat ini.';
    }
}

function filterTickets() {
    const rawQuery = searchInput ? searchInput.value.trim() : '';
    const query = rawQuery.toLowerCase();
    const totalCount = cards.length;
    let visibleCount = 0;

    if (clearSearchBtn && searchInput) {
        clearSearchBtn.hidden = searchInput.value.length === 0;
    }

    cards.forEach(card => {
        const name = card.dataset.name || '';
        const location = card.dataset.location || '';
        const cat = card.dataset.category || '';
        const matchesQuery = !query || name.includes(query) || location.includes(query);
        const matchesCategory = !activeCategory || cat === activeCategory;

        if (matchesQuery && matchesCategory) {
            card.style.display = '';
            visibleCount++;
        } else {
            card.style.display = 'none';
        }
    });

    const isFiltered = Boolean(query || activeCategory);
    updateResultsSummary(visibleCount, totalCount, isFiltered);

    if (noMatchesBox) {
        const showEmpty = visibleCount === 0 && totalCount > 0;
        noMatchesBox.hidden = !showEmpty;
        if (showEmpty) {
            updateEmptyStateMessage(rawQuery, activeCategory);
        }
    }
}

function resetAllFilters(focusTarget) {
    if (searchInput) searchInput.value = '';
    activeCategory = '';
    updateChipStates();
    filterTickets();
    if (focusTarget) focusTarget.focus();
}

if (searchInput) {
    searchInput.addEventListener('input', filterTickets);
    searchInput.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && searchInput.value.length > 0) {
            e.preventDefault();
            searchInput.value = '';
            filterTickets();
        }
    });
}

if (clearSearchBtn && searchInput) {
    clearSearchBtn.addEventListener('click', () => {
        searchInput.value = '';
        filterTickets();
        searchInput.focus();
    });
}

chipButtons.forEach(btn => {
    btn.addEventListener('click', () => {
        activeCategory = btn.dataset.category || '';
        updateChipStates();
        filterTickets();
    });
});

if (resetFiltersBtn) {
    resetFiltersBtn.addEventListener('click', () => resetAllFilters(searchInput));
}

if (inlineResetBtn) {
    inlineResetBtn.addEventListener('click', () => resetAllFilters(chipButtons[0] || searchInput));
}

const modal = document.getElementById('orderModal');
const qtyInput = document.getElementById('ticketQuantity');
const organizerPaymentAccounts = <?= json_encode($organizerPayments) ?>;

function openOrderModal(eventData) {
    currentEvent = eventData;
    const hiddenId = document.getElementById('hiddenEventId');
    if (hiddenId) hiddenId.value = eventData.id;

    const modalMedia = document.getElementById('modalEventMedia');
    const modalMediaImg = document.getElementById('modalEventMediaImg');
    const modalZoomChip = document.getElementById('modalEventZoomChip');
    if (modalMedia && modalMediaImg) {
        const hasRealImage = Boolean(eventData.imageUrl);
        modalMediaImg.src = hasRealImage ? eventData.imageUrl : (eventData.fallbackSvgUrl || '');
        modalMediaImg.alt = `Foto acara ${eventData.name || ''}`;
        modalMedia.classList.toggle('is-zoomable', hasRealImage);
        if (modalZoomChip) modalZoomChip.hidden = !hasRealImage;
        modalMedia.onclick = hasRealImage
            ? () => openImageLightbox(eventData.imageUrl, eventData.name)
            : null;
        modalMedia.hidden = false;
    }

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
    const modalMedia = document.getElementById('modalEventMedia');
    if (s1) s1.hidden = false;
    if (s2) s2.hidden = true;
    if (modalMedia) modalMedia.hidden = false;
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

    // Isi pilihan metode bayar khusus sesuai rekening organizer event ini
    const sel = document.getElementById('paymentMethod');
    if (sel) {
        sel.innerHTML = '<option value="">Pilih metode pembayaran</option>';
        
        const qrisOpt = document.createElement('option');
        qrisOpt.value = 'qris';
        qrisOpt.textContent = 'QRIS (Gopay / OVO / Dana / ShopeePay / BCA Mobile)';
        sel.appendChild(qrisOpt);

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

    const modalMedia = document.getElementById('modalEventMedia');
    if (modalMedia) modalMedia.hidden = true;
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
                    <img src="${qrUrl}" alt="Kode QRIS Tiketin" class="qris-img" onerror="this.onerror=null; this.src=buildFallbackQrSvgDataUri();" />
                </div>
                <div class="qris-caption">
                    <p style="margin:0; font-size: 0.95rem;"><strong>Total Bayar: ${escapeHtml(totalPriceFormatted)}</strong></p>
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

        const safeBankName = escapeHtml(bankName);
        const safeAccNumber = escapeHtml(accNumber);
        const safeAccName = escapeHtml(accName);
        const safeTotal = escapeHtml(totalPriceFormatted);

        detailBox.innerHTML = `
            <div class="payment-account-card">
                <div class="account-card-top">
                    <span class="account-bank-pill">${safeBankName}</span>
                    <span class="account-hint">Rekening Penyelenggara Acara</span>
                </div>
                <div class="account-number-group">
                    <span class="account-number-text" id="accNumText">${safeAccNumber}</span>
                    <button type="button" class="button button-small copy-account-button" data-acc="${safeAccNumber}" onclick="copyAccountNumber(this.dataset.acc, this)">
                        <span class="copy-icon">📋</span> Salin
                    </button>
                </div>
                <div class="account-owner-info">
                    Atas Nama: <strong>${safeAccName}</strong>
                </div>
                <div class="account-guide">
                    💡 Silakan transfer sejumlah <strong>${safeTotal}</strong> ke rekening di atas, lalu unggah struk / bukti transfer di bawah.
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

// Biarkan Esc menutup lightbox dulu kalau lagi terbuka di atas modal pesan
window.addEventListener('keydown', (e) => {
    const lightbox = document.getElementById('imageLightboxModal');
    if (e.key === 'Escape' && lightbox && lightbox.classList.contains('is-open')) {
        return;
    }
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
