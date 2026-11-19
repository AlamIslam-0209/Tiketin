<?php
require_once __DIR__ . '/config/database.php';
require_once __DIR__ . '/includes/auth_check.php';
require_once __DIR__ . '/includes/functions.php';

$currentUser = require_role('admin', 'login.php');

$pageTitle = 'Dashboard Admin — Tiketin';
$activeNav = '';
$bodyDataDashboard = 'admin';

$users = [];
$applications = [];
$events = [];
$eventNow = new DateTimeImmutable('now', new DateTimeZone('Asia/Jakarta'));

if (isset($pdo)) {
    try {
        $stmtUsers = $pdo->query("SELECT * FROM users ORDER BY id ASC");
        $users = $stmtUsers->fetchAll();

        $stmtApps = $pdo->query("SELECT * FROM applications ORDER BY id DESC");
        $applications = $stmtApps->fetchAll();

        $stmtEvents = $pdo->query("
            SELECT e.*, u.username AS owner_username, u.organization AS owner_org 
            FROM events e 
            LEFT JOIN users u ON e.owner_id = u.id 
            ORDER BY e.date ASC
        ");
        $events = sort_events_by_priority($stmtEvents->fetchAll(), $eventNow);
    } catch (Exception $e) {
        $users = [];
        $applications = [];
        $events = [];
    }
}

$pendingCount = count(array_filter($applications, fn($a) => $a['status'] === 'pending'));

require_once __DIR__ . '/includes/header.php';
?>

<main class="page-main container">
    <div class="page-heading">
        <p class="eyebrow">Ruang admin</p>
        <h1>Kelola Tiketin.</h1>
        <p>Tinjau calon organizer, lihat akun, dan kelola seluruh event yang tayang.</p>
    </div>

    <div class="dashboard-summary" id="dashboardSummary">
        <div class="summary-item">
            <strong><?= count($users) ?></strong>
            <span>Akun</span>
        </div>
        <div class="summary-item">
            <strong><?= $pendingCount ?></strong>
            <span>Menunggu persetujuan</span>
        </div>
        <div class="summary-item">
            <strong><?= count($events) ?></strong>
            <span>Event tayang</span>
        </div>
    </div>

    <section class="panel dashboard-section" aria-labelledby="applicationsTitle">
        <div class="panel-heading">
            <h2 id="applicationsTitle">Pengajuan organizer</h2>
            <span id="applicationCount"><?= $pendingCount ?> menunggu</span>
        </div>
        
        <div class="management-list" id="applicationList">
            <?php if (empty($applications)): ?>
                <p class="empty-state">Belum ada pengajuan calon organizer.</p>
            <?php else: ?>
                <?php foreach ($applications as $app): 
                    $status = $app['status'];
                    $statusLabel = $status === 'approved' ? 'Disetujui' : ($status === 'rejected' ? 'Ditolak' : 'Menunggu');
                    $appImageUrl = resolve_event_image_url($app['image_path'] ?? null);
                ?>
                    <article class="application-row">
                        <div class="application-heading">
                            <h3><?= htmlspecialchars($app['organization']) ?></h3>
                            <span class="status status-<?= $status ?>"><?= $statusLabel ?></span>
                        </div>
                        <p><?= htmlspecialchars($app['full_name']) ?> · <?= htmlspecialchars($app['contact']) ?></p>
                        <p>
                            <strong>Event perdana:</strong> <?= htmlspecialchars($app['event_name']) ?> · 
                            📅 <?= format_tanggal_id($app['event_date']) ?><?= !empty($app['event_time']) ? ' · 🕒 ' . htmlspecialchars($app['event_time']) : '' ?> · 
                            <?= format_rupiah($app['event_price']) ?> · 
                            <?= (int)$app['event_slots'] ?> slot (<?= htmlspecialchars($app['event_category']) ?>)
                        </p>
                        <?php if (!empty($app['event_location'])): ?>
                            <p style="font-size:0.83rem; color:var(--muted); margin-top:2px;">📍 <?= htmlspecialchars($app['event_location']) ?></p>
                        <?php endif; ?>
                        <?php if (!empty($app['event_description'])): ?>
                            <p style="font-size:0.85rem; color:var(--muted);"><?= htmlspecialchars($app['event_description']) ?></p>
                        <?php endif; ?>

                        <div class="application-media-row">
                            <?php if ($appImageUrl): ?>
                                <button type="button"
                                        class="management-thumb-16x9 is-zoomable"
                                        aria-label="Perbesar foto proposal <?= htmlspecialchars($app['event_name']) ?>"
                                        onclick="openImageLightbox(<?= htmlspecialchars(json_encode($appImageUrl)) ?>, <?= htmlspecialchars(json_encode('Proposal: ' . $app['event_name'])) ?>)">
                                    <img src="<?= htmlspecialchars($appImageUrl) ?>" alt="Foto proposal <?= htmlspecialchars($app['event_name']) ?>" loading="lazy" />
                                </button>
                                <span class="application-media-hint">Foto proposal event perdana · Klik untuk perbesar</span>
                            <?php else: ?>
                                <div class="management-thumb-16x9">
                                    <?= render_event_category_svg($app['event_category'] ?? 'Lainnya') ?>
                                </div>
                                <span class="application-media-hint">Belum ada foto proposal (tampilan ilustrasi kategori)</span>
                            <?php endif; ?>
                        </div>

                        <?php if ($status === 'rejected' && !empty($app['reason'])): ?>
                            <p class="rejection-reason">Alasan: <?= htmlspecialchars($app['reason']) ?></p>
                        <?php endif; ?>

                        <?php if ($status === 'pending'): ?>
                            <form action="proses/application_process.php" method="POST" class="application-controls" style="margin-top:12px;">
                                <input type="hidden" name="action" value="decide" />
                                <input type="hidden" name="application_id" value="<?= (int)$app['id'] ?>" />
                                <input type="hidden" name="status" id="status_<?= (int)$app['id'] ?>" value="approved" />

                                <label for="reason_<?= (int)$app['id'] ?>">Alasan penolakan (wajib jika ditolak)</label>
                                <input type="text" id="reason_<?= (int)$app['id'] ?>" name="reason" placeholder="Tulis alasan jika ingin menolak..." />

                                <div class="row-actions" style="margin-top:8px;">
                                    <button class="button button-primary" type="submit" onclick="document.getElementById('status_<?= (int)$app['id'] ?>').value='approved'">
                                        Setujui
                                    </button>
                                    <button class="button button-danger" type="submit" onclick="
                                        const reasonInput = document.getElementById('reason_<?= (int)$app['id'] ?>');
                                        if (!reasonInput.value.trim()) {
                                            alert('Harap isi alasan penolakan terlebih dahulu!');
                                            reasonInput.focus();
                                            return false;
                                        }
                                        document.getElementById('status_<?= (int)$app['id'] ?>').value='rejected';
                                        return true;
                                    ">
                                        Tolak
                                    </button>
                                </div>
                            </form>
                        <?php endif; ?>
                    </article>
                <?php endforeach; ?>
            <?php endif; ?>
        </div>
    </section>

    <section class="panel dashboard-section" aria-labelledby="usersTitle">
        <div class="panel-heading">
            <h2 id="usersTitle">Akun pengguna</h2>
            <span id="userCount"><?= count($users) ?> akun</span>
        </div>
        <div class="management-list" id="userList">
            <?php foreach ($users as $u): ?>
                <div class="management-row">
                    <div class="management-copy">
                        <h3><?= htmlspecialchars($u['username']) ?> <small style="font-weight:normal; color:var(--muted);">(<?= htmlspecialchars($u['full_name']) ?>)</small></h3>
                        <p><?= htmlspecialchars($u['organization'] ?: 'Pengunjung / Tanpa organisasi') ?> · Didaftarkan: <?= date('d M Y', strtotime($u['created_at'])) ?></p>
                    </div>
                    <span class="status status-<?= $u['role'] === 'admin' ? 'approved' : ($u['role'] === 'organizer' ? 'pending' : 'default') ?>">
                        <?= ucfirst($u['role']) ?>
                    </span>
                </div>
            <?php endforeach; ?>
        </div>
    </section>

    <section class="panel dashboard-section" aria-labelledby="eventsTitle">
        <div class="panel-heading">
            <h2 id="eventsTitle">Seluruh event yang tayang</h2>
            <span id="eventCount"><?= count($events) ?> event</span>
        </div>

        <div id="adminEditBox" class="panel form-panel" style="display:none; margin-bottom:24px; border:1.5px solid var(--coral); background:var(--card);">
            <h3 id="adminEditTitle" style="margin-top:0;">Edit Event</h3>
            <form action="proses/event_process.php" method="POST" enctype="multipart/form-data">
                <input type="hidden" name="action" value="update" />
                <input type="hidden" name="event_id" id="adminEventId" value="" />

                <?= render_event_image_upload_field([
                    'id' => 'adminEventUploadWidget',
                    'input_id' => 'adminEventImageInput',
                    'required' => false,
                    'badge_text' => '1 foto · Rasio 16:9 utuh',
                ]) ?>

                <div class="form-field">
                    <label for="adminEventName">Nama event</label>
                    <input id="adminEventName" name="name" required />
                </div>

                <div class="form-grid">
                    <div class="form-field">
                        <label for="adminEventDate">Tanggal</label>
                        <input id="adminEventDate" name="date" type="date" required />
                    </div>
                    <div class="form-field">
                        <label for="adminEventTime">Jam mulai acara</label>
                        <input id="adminEventTime" name="time" type="text" placeholder="Contoh: 19:00 WIB" required />
                    </div>
                </div>

                <div class="form-grid">
                    <div class="form-field">
                        <label for="adminEventLocation">Lokasi acara</label>
                        <input id="adminEventLocation" name="location" type="text" placeholder="Contoh: Balai Sarbini, Jakarta" required />
                    </div>
                    <div class="form-field">
                        <label for="adminEventCategory">Kategori</label>
                        <select id="adminEventCategory" name="category">
                            <option value="Konser">Konser</option>
                            <option value="Wisata">Wisata</option>
                            <option value="Seminar">Seminar</option>
                            <option value="Lainnya">Lainnya</option>
                        </select>
                    </div>
                </div>

                <div class="form-grid">
                    <div class="form-field">
                        <label for="adminEventPrice">Harga tiket (Rp)</label>
                        <input id="adminEventPrice" name="price" type="number" min="0" required />
                    </div>
                    <div class="form-field">
                        <label for="adminEventSlots">Sisa kuota</label>
                        <input id="adminEventSlots" name="slots" type="number" min="0" required />
                    </div>
                </div>

                <div class="form-field">
                    <label for="adminEventDescription">Deskripsi</label>
                    <textarea id="adminEventDescription" name="description" rows="3"></textarea>
                </div>

                <div class="form-actions" style="margin-top:16px;">
                    <button class="button button-submit" type="submit">Simpan perubahan</button>
                    <button class="button button-quiet" type="button" onclick="closeAdminEdit()">Batal</button>
                </div>
            </form>
        </div>

        <div class="management-list" id="eventList">
            <?php if (empty($events)): ?>
                <p class="empty-state">Belum ada event terdaftar.</p>
            <?php else: ?>
                <?php foreach ($events as $evt):
                    $evtImageUrl = resolve_event_image_url($evt['image_path'] ?? null);
                    $evtPayload = array_merge($evt, ['imageUrl' => $evtImageUrl]);
                    $statusInfo = get_event_status_info($evt, $eventNow);
                ?>
                    <article class="management-row<?= $statusInfo['is_expired'] ? ' is-expired' : '' ?>">
                        <?php if ($evtImageUrl): ?>
                            <button type="button"
                                    class="management-thumb-16x9 is-zoomable"
                                    aria-label="Perbesar foto <?= htmlspecialchars($evt['name']) ?>"
                                    onclick="openImageLightbox(<?= htmlspecialchars(json_encode($evtImageUrl)) ?>, <?= htmlspecialchars(json_encode($evt['name'])) ?>)">
                                <img src="<?= htmlspecialchars($evtImageUrl) ?>" alt="Foto <?= htmlspecialchars($evt['name']) ?>" loading="lazy" />
                            </button>
                        <?php else: ?>
                            <div class="management-thumb-16x9">
                                <?= render_event_category_svg($evt['category'] ?? 'Lainnya') ?>
                            </div>
                        <?php endif; ?>
                        <div class="management-copy">
                            <div class="management-title-row">
                                <h3><?= htmlspecialchars($evt['name']) ?></h3>
                                <span class="badge badge-event-<?= htmlspecialchars($statusInfo['key']) ?>"><?= htmlspecialchars($statusInfo['label']) ?></span>
                            </div>
                            <p>
                                📅 <?= format_tanggal_id($evt['date']) ?><?= !empty($evt['time']) ? ' · 🕒 ' . htmlspecialchars($evt['time']) : '' ?> · 
                                <?= format_rupiah($evt['price']) ?> · 
                                <strong><?= (int)$evt['slots'] ?></strong> slot tersisa
                            </p>
                            <?php if (!empty($evt['location'])): ?>
                                <p style="font-size:0.83rem; color:var(--muted); margin-top:2px;">📍 <?= htmlspecialchars($evt['location']) ?></p>
                            <?php endif; ?>
                            <small>Penyelenggara: <?= htmlspecialchars($evt['owner_org'] ?: ($evt['owner_username'] ?: 'Tidak diketahui')) ?></small>
                        </div>
                        <div class="row-actions">
                            <button class="button button-quiet" type="button" onclick="openAdminEdit(<?= htmlspecialchars(json_encode($evtPayload)) ?>)">
                                Edit
                            </button>
                            <a class="button button-danger"
                               href="proses/event_process.php?action=delete&id=<?= (int)$evt['id'] ?>"
                               onclick="return confirm(<?= htmlspecialchars(json_encode('Yakin ingin menghapus event "' . $evt['name'] . '"? Tindakan ini tidak bisa dibatalkan.'), ENT_QUOTES, 'UTF-8') ?>)">
                                Hapus
                            </a>
                        </div>
                    </article>
                <?php endforeach; ?>
            <?php endif; ?>
        </div>
    </section>
</main>

<script>
function openAdminEdit(evt) {
    document.getElementById('adminEventId').value = evt.id;
    document.getElementById('adminEventName').value = evt.name;
    document.getElementById('adminEventDate').value = evt.date;
    document.getElementById('adminEventTime').value = evt.time || '';
    document.getElementById('adminEventLocation').value = evt.location || '';
    document.getElementById('adminEventCategory').value = evt.category;
    document.getElementById('adminEventPrice').value = evt.price;
    document.getElementById('adminEventSlots').value = evt.slots;
    document.getElementById('adminEventDescription').value = evt.description || '';

    setUploadWidgetSavedImage(
        'adminEventUploadWidget',
        evt.imageUrl || '',
        evt.image_path || '',
        !evt.imageUrl
    );

    document.getElementById('adminEditTitle').textContent = `Edit Event: ${evt.name}`;
    const box = document.getElementById('adminEditBox');
    box.style.display = 'block';
    box.scrollIntoView({ behavior: 'smooth', block: 'center' });
    document.getElementById('adminEventName').focus();
}

function closeAdminEdit() {
    resetUploadWidget('adminEventUploadWidget', false);
    document.getElementById('adminEditBox').style.display = 'none';
}
</script>

<?php require_once __DIR__ . '/includes/footer.php'; ?>
