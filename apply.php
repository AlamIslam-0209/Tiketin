<?php
require_once __DIR__ . '/config/database.php';
require_once __DIR__ . '/includes/auth_check.php';
require_once __DIR__ . '/includes/functions.php';

$currentUser = require_role('user', 'login.php');

$pageTitle = 'Ajukan Jadi Organizer — Tiketin';
$activeNav = '';

$prev = null;
if (isset($pdo)) {
    try {
        $stmt = $pdo->prepare("SELECT * FROM applications WHERE user_id = ? ORDER BY id DESC LIMIT 1");
        $stmt->execute([$currentUser['id']]);
        $prev = $stmt->fetch();
    } catch (Exception $e) {
        $prev = null;
    }
}

require_once __DIR__ . '/includes/header.php';
?>

<main class="page-main container">
    <div class="page-heading">
        <p class="eyebrow">Langkah berikutnya</p>
        <h1>Ajukan jadi organizer.</h1>
        <p>Kirim profil dan event pertamamu. Admin akan meninjau satu kali; setelah disetujui, kamu bebas menambah event sendiri.</p>
    </div>

    <?php if ($prev && $prev['status'] === 'pending'): ?>
        <div class="notice">
            <strong>Pengajuan Sedang Ditinjau</strong>
            <p>Pengajuan organizer Anda telah terkirim dan sedang menunggu persetujuan dari tim admin Tiketin. Mohon tunggu beberapa saat.</p>
        </div>
    <?php else: ?>
        <?php if ($prev && $prev['status'] === 'rejected'): ?>
            <div class="notice" style="border-left-color: #b91c1c;">
                <strong style="color: #b91c1c;">Pengajuan Sebelumnya Ditolak</strong>
                <p>Alasan penolakan: <?= htmlspecialchars($prev['reason']) ?></p>
                <p style="margin-top:4px;">Anda dapat memperbaiki data di bawah ini dan mengirimkan pengajuan ulang.</p>
            </div>
        <?php endif; ?>

        <form class="panel form-panel" id="applicationForm" action="proses/application_process.php?action=apply" method="POST">
            <h2>Identitas &amp; organisasi</h2>
            <div class="form-grid">
                <div>
                    <label for="fullName">Nama lengkap</label>
                    <input id="fullName" name="fullName" value="<?= htmlspecialchars($prev ? $prev['full_name'] : $currentUser['full_name']) ?>" required />
                </div>
                <div>
                    <label for="contact">Kontak (WhatsApp / Email)</label>
                    <input id="contact" name="contact" value="<?= htmlspecialchars($prev ? $prev['contact'] : '') ?>" placeholder="08123456789 atau email@domain.com" required />
                </div>
            </div>

            <div>
                <label for="organization">Nama organisasi / komunitas</label>
                <input id="organization" name="organization" value="<?= htmlspecialchars($prev ? $prev['organization'] : '') ?>" placeholder="Contoh: Senja Suara Creative" required />
            </div>

            <h2>Event pertama yang ingin dibuat</h2>
            <p class="panel-intro">Data event ini akan langsung aktif begitu pengajuanmu disetujui admin.</p>

            <div class="form-field">
                <label for="eventName">Nama event</label>
                <input id="eventName" name="name" value="<?= htmlspecialchars($prev ? $prev['event_name'] : '') ?>" required placeholder="Contoh: Konser Senja Bahagia" />
            </div>

            <div class="form-grid">
                <div>
                    <label for="eventDate">Tanggal</label>
                    <input id="eventDate" name="date" type="date" value="<?= htmlspecialchars($prev ? $prev['event_date'] : '') ?>" required />
                </div>
                <div>
                    <label for="eventTime">Jam mulai acara</label>
                    <input id="eventTime" name="time" type="text" value="<?= htmlspecialchars($prev ? ($prev['event_time'] ?? '') : '') ?>" placeholder="Contoh: 19:00 WIB" required />
                </div>
            </div>

            <div class="form-grid">
                <div>
                    <label for="eventLocation">Lokasi acara</label>
                    <input id="eventLocation" name="location" type="text" value="<?= htmlspecialchars($prev ? ($prev['event_location'] ?? '') : '') ?>" placeholder="Contoh: Balai Sarbini, Jakarta" required />
                </div>
                <div>
                    <label for="eventCategory">Kategori</label>
                    <select id="eventCategory" name="category">
                        <?php 
                        $selectedCat = $prev ? $prev['event_category'] : 'Konser';
                        foreach (['Konser', 'Wisata', 'Seminar', 'Lainnya'] as $cat): ?>
                            <option value="<?= $cat ?>" <?= $selectedCat === $cat ? 'selected' : '' ?>><?= $cat ?></option>
                        <?php endforeach; ?>
                    </select>
                </div>
            </div>

            <div class="form-grid">
                <div>
                    <label for="eventPrice">Harga tiket (Rp)</label>
                    <input id="eventPrice" name="price" type="number" min="0" step="1000" value="<?= $prev ? (int)$prev['event_price'] : '75000' ?>" required />
                </div>
                <div>
                    <label for="eventSlots">Kuota tiket</label>
                    <input id="eventSlots" name="slots" type="number" min="1" value="<?= $prev ? (int)$prev['event_slots'] : '50' ?>" required />
                </div>
            </div>

            <div>
                <label for="eventDescription">Deskripsi event</label>
                <textarea id="eventDescription" name="description" rows="3" placeholder="Ceritakan konsep dan daya tarik event..."><?= htmlspecialchars($prev ? $prev['event_description'] : '') ?></textarea>
            </div>

            <div class="form-actions">
                <button class="button button-submit" type="submit">Kirim pengajuan</button>
            </div>
        </form>
    <?php endif; ?>
</main>

<?php require_once __DIR__ . '/includes/footer.php'; ?>
