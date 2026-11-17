<?php
// Kumpulan helper umum untuk format data, sanitasi, upload foto event, dan fallback SVG

// Ubah angka jadi format Rupiah
function format_rupiah($amount): string
{
    return 'Rp ' . number_format((float)$amount, 0, ',', '.');
}

// Ubah tanggal YYYY-MM-DD jadi format Indonesia yang gampang dibaca
function format_tanggal_id(?string $dateStr, bool $shortMonth = true): string
{
    if (!$dateStr) return '-';
    
    $timestamp = strtotime($dateStr);
    if (!$timestamp) return $dateStr;

    $day = date('j', $timestamp);
    $monthIndex = (int)date('n', $timestamp);
    $year = date('Y', $timestamp);

    $monthsShort = [
        1 => 'Jan', 2 => 'Feb', 3 => 'Mar', 4 => 'Apr', 5 => 'Mei', 6 => 'Jun',
        7 => 'Jul', 8 => 'Agu', 9 => 'Sep', 10 => 'Okt', 11 => 'Nov', 12 => 'Des'
    ];

    $monthsLong = [
        1 => 'Januari', 2 => 'Februari', 3 => 'Maret', 4 => 'April', 5 => 'Mei', 6 => 'Juni',
        7 => 'Juli', 8 => 'Agustus', 9 => 'September', 10 => 'Oktober', 11 => 'November', 12 => 'Desember'
    ];

    $monthName = $shortMonth ? ($monthsShort[$monthIndex] ?? '') : ($monthsLong[$monthIndex] ?? '');

    return "{$day} {$monthName} {$year}";
}

// Bersihin string biar aman dari XSS saat dicetak ke HTML
function sanitize($data): string
{
    return htmlspecialchars(trim((string)$data), ENT_QUOTES, 'UTF-8');
}

// Bikin kode pesanan acak yang unik
function generate_order_code(): string
{
    return 'TKT-' . date('Ymd') . '-' . strtoupper(substr(bin2hex(random_bytes(4)), 0, 6));
}

function get_payment_method_label(?string $methodKey): string
{
    $labels = [
        'qris'    => 'QRIS',
        'bca'     => 'Bank BCA',
        'bri'     => 'Bank BRI',
        'bsi'     => 'Bank BSI',
        'mandiri' => 'Bank Mandiri',
    ];

    return $labels[strtolower((string)$methodKey)] ?? ($methodKey ?: '-');
}

// Cek apakah file foto event beneran ada di uploads/events, lalu balikin URL relatifnya
function resolve_event_image_url(?string $imagePath): ?string
{
    if ($imagePath === null) {
        return null;
    }
    $clean = basename(trim($imagePath));
    if ($clean === '' || $clean === '.' || $clean === '..') {
        return null;
    }
    $diskPath = __DIR__ . '/../uploads/events/' . $clean;
    if (!is_file($diskPath)) {
        return null;
    }
    return 'uploads/events/' . rawurlencode($clean);
}

// Samain nama kategori jadi kunci standar untuk styling dan ilustrasi SVG
function normalize_event_category_key(?string $category): string
{
    $cat = strtolower(trim((string)$category));
    if (str_contains($cat, 'konser') || str_contains($cat, 'musik')) {
        return 'konser';
    }
    if (str_contains($cat, 'wisata') || str_contains($cat, 'travel') || str_contains($cat, 'alam')) {
        return 'wisata';
    }
    if (str_contains($cat, 'seminar') || str_contains($cat, 'workshop') || str_contains($cat, 'tech')) {
        return 'seminar';
    }
    return 'lainnya';
}

// Bikin ilustrasi SVG cadangan per kategori kalau event belum punya foto
function render_event_category_svg(?string $category, string $className = 'event-fallback-svg'): string
{
    $key = normalize_event_category_key($category);
    $label = sanitize($category ?: 'Acara Pilihan');
    $cls = htmlspecialchars($className, ENT_QUOTES, 'UTF-8');

    if ($key === 'konser') {
        $inner = '
            <rect width="640" height="360" fill="#F9DDD5"/>
            <circle cx="320" cy="110" r="185" fill="#FCEBE6" fill-opacity="0.78"/>
            <circle cx="320" cy="126" r="112" fill="#FFF5F2" fill-opacity="0.88"/>
            <polygon points="108,0 262,254 378,254 198,0" fill="#E9785D" fill-opacity="0.16"/>
            <polygon points="442,0 262,254 378,254 532,0" fill="#E9785D" fill-opacity="0.16"/>
            <path d="M 164 256 A 156 146 0 0 1 476 256" fill="none" stroke="#E9785D" stroke-opacity="0.52" stroke-width="4"/>
            <rect x="0" y="248" width="640" height="112" fill="#B94E3A" fill-opacity="0.22"/>
            <rect x="64" y="144" width="46" height="104" rx="10" fill="#B94E3A" fill-opacity="0.82"/>
            <circle cx="87" cy="176" r="12" fill="#8A4130"/>
            <circle cx="87" cy="216" r="16" fill="#8A4130"/>
            <rect x="530" y="144" width="46" height="104" rx="10" fill="#B94E3A" fill-opacity="0.82"/>
            <circle cx="553" cy="176" r="12" fill="#8A4130"/>
            <circle cx="553" cy="216" r="16" fill="#8A4130"/>
            <circle cx="320" cy="146" r="14" fill="#8A4130"/>
            <rect x="307" y="164" width="26" height="62" rx="13" fill="#8A4130"/>
            <path d="M 278 142 A 42 54 0 0 0 278 202" fill="none" stroke="#E06338" stroke-opacity="0.72" stroke-width="3.5" stroke-linecap="round"/>
            <path d="M 362 142 A 42 54 0 0 1 362 202" fill="none" stroke="#E06338" stroke-opacity="0.72" stroke-width="3.5" stroke-linecap="round"/>
            <rect x="0" y="298" width="640" height="62" fill="#8A4130"/>
            <g fill="#8A4130">
                <circle cx="48" cy="292" r="18"/><ellipse cx="48" cy="318" rx="28" ry="22"/>
                <circle cx="122" cy="286" r="20"/><ellipse cx="122" cy="314" rx="30" ry="24"/>
                <circle cx="198" cy="294" r="18"/><ellipse cx="198" cy="320" rx="28" ry="22"/>
                <circle cx="276" cy="288" r="20"/><ellipse cx="276" cy="315" rx="30" ry="24"/>
                <circle cx="364" cy="288" r="20"/><ellipse cx="364" cy="315" rx="30" ry="24"/>
                <circle cx="442" cy="294" r="18"/><ellipse cx="442" cy="320" rx="28" ry="22"/>
                <circle cx="518" cy="286" r="20"/><ellipse cx="518" cy="314" rx="30" ry="24"/>
                <circle cx="592" cy="292" r="18"/><ellipse cx="592" cy="318" rx="28" ry="22"/>
            </g>
            <line x1="136" y1="294" x2="148" y2="256" stroke="#8A4130" stroke-width="5" stroke-linecap="round"/>
            <circle cx="150" cy="250" r="8" fill="#FFF4DC"/>
            <line x1="504" y1="294" x2="492" y2="256" stroke="#8A4130" stroke-width="5" stroke-linecap="round"/>
            <circle cx="490" cy="250" r="8" fill="#FFF4DC"/>
        ';
    } elseif ($key === 'wisata') {
        $inner = '
            <rect width="640" height="360" fill="#E9EFDC"/>
            <circle cx="488" cy="88" r="52" fill="#F3E3B6" fill-opacity="0.58"/>
            <circle cx="488" cy="88" r="34" fill="#FFF4DC"/>
            <g fill="#FFFFFF" fill-opacity="0.86">
                <circle cx="144" cy="88" r="20"/><circle cx="170" cy="80" r="26"/><circle cx="198" cy="90" r="18"/>
                <circle cx="324" cy="64" r="16"/><circle cx="346" cy="58" r="21"/><circle cx="368" cy="66" r="15"/>
            </g>
            <polygon points="0,250 105,156 218,214 372,126 518,208 640,148 640,360 0,360" fill="#A8B88C"/>
            <polygon points="0,284 138,198 298,270 462,182 640,262 640,360 0,360" fill="#68774B"/>
            <polygon points="0,310 176,268 366,302 536,256 640,292 640,360 0,360" fill="#4F5C37"/>
            <polygon points="82,238 64,290 100,290" fill="#3F4C2B"/>
            <polygon points="122,252 108,296 136,296" fill="#4F5C37"/>
            <polygon points="562,226 540,288 584,288" fill="#3F4C2B"/>
            <polygon points="342,190 362,190 352,212" fill="#E06338"/>
            <circle cx="352" cy="182" r="14" fill="#E06338"/>
            <circle cx="352" cy="182" r="5.5" fill="#FFFFFF"/>
        ';
    } elseif ($key === 'seminar') {
        $inner = '
            <rect width="640" height="360" fill="#E9E4F2"/>
            <rect x="0" y="256" width="640" height="104" fill="#DDD6EA"/>
            <rect x="190" y="52" width="330" height="180" rx="18" fill="#FFFFFF" stroke="#C7BDDA" stroke-width="3"/>
            <rect x="218" y="78" width="150" height="14" rx="7" fill="#54496A"/>
            <rect x="218" y="102" width="102" height="10" rx="5" fill="#D1C7E2"/>
            <rect x="222" y="168" width="24" height="44" rx="5" fill="#D1C7E2"/>
            <rect x="258" y="148" width="24" height="64" rx="5" fill="#9B8FB5"/>
            <rect x="294" y="132" width="24" height="80" rx="5" fill="#7E7299"/>
            <rect x="330" y="114" width="24" height="98" rx="5" fill="#E9785D"/>
            <rect x="384" y="118" width="106" height="11" rx="5.5" fill="#C7BDDA"/>
            <rect x="384" y="144" width="84" height="11" rx="5.5" fill="#D8D0E8"/>
            <rect x="384" y="170" width="102" height="11" rx="5.5" fill="#D8D0E8"/>
            <rect x="116" y="178" width="46" height="78" rx="8" fill="#7E7299"/>
            <circle cx="139" cy="134" r="14" fill="#54496A"/>
            <ellipse cx="139" cy="164" rx="20" ry="15" fill="#54496A"/>
            <line x1="156" y1="162" x2="188" y2="128" stroke="#54496A" stroke-width="4.5" stroke-linecap="round"/>
            <g>
                <circle cx="110" cy="276" r="16" fill="#54496A"/><ellipse cx="110" cy="312" rx="28" ry="26" fill="#54496A"/>
                <circle cx="214" cy="276" r="16" fill="#6D6380"/><ellipse cx="214" cy="312" rx="28" ry="26" fill="#6D6380"/>
                <circle cx="320" cy="276" r="16" fill="#54496A"/><ellipse cx="320" cy="312" rx="28" ry="26" fill="#54496A"/>
                <circle cx="426" cy="276" r="16" fill="#6D6380"/><ellipse cx="426" cy="312" rx="28" ry="26" fill="#6D6380"/>
                <circle cx="530" cy="276" r="16" fill="#54496A"/><ellipse cx="530" cy="312" rx="28" ry="26" fill="#54496A"/>
            </g>
        ';
    } else {
        $inner = '
            <rect width="640" height="360" fill="#F3F1ED"/>
            <circle cx="536" cy="48" r="126" fill="#E3E6E2" fill-opacity="0.62"/>
            <circle cx="104" cy="306" r="116" fill="#E06338" fill-opacity="0.1"/>
            <circle cx="320" cy="154" r="46" fill="#FFFFFF" fill-opacity="0.92" stroke="#E3E6E2" stroke-width="3"/>
            <polygon points="320,126 333,154 320,182 307,154" fill="#E06338"/>
            <text x="320" y="238" text-anchor="middle" font-family="sans-serif" font-size="18" font-weight="600" fill="#525A62">Acara Pilihan</text>
        ';
    }

    return '<svg class="' . $cls . '" viewBox="0 0 640 360" preserveAspectRatio="xMidYMid slice" role="img" aria-label="Ilustrasi kategori ' . $label . '" xmlns="http://www.w3.org/2000/svg">' . $inner . '</svg>';
}

// Ubah SVG kategori jadi data URI supaya bisa dipakai langsung di preview JS
function get_category_svg_data_uri(?string $category): string
{
    $svg = render_event_category_svg($category, 'event-fallback-svg');
    return 'data:image/svg+xml;charset=UTF-8,' . rawurlencode($svg);
}

// Validasi dan simpan foto event ke uploads/events; kalau lagi edit dan nggak ganti foto, pakai yang lama
function handle_event_image_upload(
    string $fieldName = 'event_image',
    string $prefix = 'event',
    bool $required = true,
    ?string $existingImage = null
): array {
    $cleanExisting = ($existingImage !== null && trim($existingImage) !== '')
        ? basename(trim($existingImage))
        : null;

    $file = $_FILES[$fieldName] ?? null;
    $hasUpload = is_array($file)
        && isset($file['error'])
        && (int)$file['error'] !== UPLOAD_ERR_NO_FILE
        && trim((string)($file['name'] ?? '')) !== '';

    if (!$hasUpload) {
        if (is_array($file) && isset($file['error']) && in_array((int)$file['error'], [UPLOAD_ERR_INI_SIZE, UPLOAD_ERR_FORM_SIZE], true)) {
            return [
                'ok' => false,
                'filename' => null,
                'error' => 'Ukuran foto acara melebihi batas maksimal server.',
                'uploaded_new' => false,
            ];
        }

        if ($cleanExisting !== null) {
            return [
                'ok' => true,
                'filename' => $cleanExisting,
                'error' => null,
                'uploaded_new' => false,
            ];
        }

        if ($required) {
            return [
                'ok' => false,
                'filename' => null,
                'error' => 'Foto acara (rasio 16:9) wajib diunggah.',
                'uploaded_new' => false,
            ];
        }

        return [
            'ok' => true,
            'filename' => null,
            'error' => null,
            'uploaded_new' => false,
        ];
    }

    if ((int)$file['error'] !== UPLOAD_ERR_OK) {
        return [
            'ok' => false,
            'filename' => null,
            'error' => 'Terjadi kesalahan saat mengunggah foto acara.',
            'uploaded_new' => false,
        ];
    }

    $ext = strtolower(pathinfo((string)$file['name'], PATHINFO_EXTENSION));
    $allowedExts = ['jpg', 'jpeg', 'png', 'webp', 'gif'];
    if (!in_array($ext, $allowedExts, true)) {
        return [
            'ok' => false,
            'filename' => null,
            'error' => 'Format foto acara harus JPG, JPEG, PNG, WEBP, atau GIF.',
            'uploaded_new' => false,
        ];
    }

    if ((int)$file['size'] <= 0 || (int)$file['size'] > 5 * 1024 * 1024) {
        return [
            'ok' => false,
            'filename' => null,
            'error' => 'Ukuran foto acara harus antara 1 byte hingga maksimal 5 MB.',
            'uploaded_new' => false,
        ];
    }

    $tmpName = (string)($file['tmp_name'] ?? '');
    if ($tmpName === '' || !is_file($tmpName)) {
        return [
            'ok' => false,
            'filename' => null,
            'error' => 'File sementara foto acara tidak ditemukan.',
            'uploaded_new' => false,
        ];
    }

    if (function_exists('finfo_open')) {
        $finfo = @finfo_open(FILEINFO_MIME_TYPE);
        if ($finfo) {
            $mimeType = @finfo_file($finfo, $tmpName);
            @finfo_close($finfo);
            $allowedMimes = [
                'image/jpeg',
                'image/pjpeg',
                'image/png',
                'image/x-png',
                'image/webp',
                'image/gif',
            ];
            if ($mimeType && !in_array($mimeType, $allowedMimes, true)) {
                return [
                    'ok' => false,
                    'filename' => null,
                    'error' => 'Tipe file tidak valid. Harap unggah file gambar yang sah.',
                    'uploaded_new' => false,
                ];
            }
        }
    }

    if (function_exists('getimagesize') && @getimagesize($tmpName) === false) {
        return [
            'ok' => false,
            'filename' => null,
            'error' => 'File gambar rusak atau bukan gambar yang valid.',
            'uploaded_new' => false,
        ];
    }

    $safePrefix = preg_replace('/[^a-z0-9_-]/i', '', $prefix) ?: 'event';
    $savedFilename = $safePrefix . '_' . date('Ymd_His') . '_' . bin2hex(random_bytes(3)) . '.' . $ext;
    $uploadDir = __DIR__ . '/../uploads/events/';
    if (!is_dir($uploadDir)) {
        @mkdir($uploadDir, 0755, true);
    }
    @chmod($uploadDir, 0755);

    $targetPath = $uploadDir . $savedFilename;
    $moved = is_uploaded_file($tmpName)
        ? @move_uploaded_file($tmpName, $targetPath)
        : @copy($tmpName, $targetPath);

    if (!$moved) {
        return [
            'ok' => false,
            'filename' => null,
            'error' => 'Gagal menyimpan foto acara ke server. Periksa izin folder uploads/events/.',
            'uploaded_new' => false,
        ];
    }
    @chmod($targetPath, 0644);

    return [
        'ok' => true,
        'filename' => $savedFilename,
        'error' => null,
        'uploaded_new' => true,
    ];
}

// Render komponen upload foto event biar seragam di form organizer, pengajuan, dan admin
function render_event_image_upload_field(array $options = []): string
{
    $widgetId = htmlspecialchars((string)($options['id'] ?? 'eventImageWidget'), ENT_QUOTES, 'UTF-8');
    $inputId = htmlspecialchars((string)($options['input_id'] ?? 'eventImageInput'), ENT_QUOTES, 'UTF-8');
    $inputName = htmlspecialchars((string)($options['input_name'] ?? 'event_image'), ENT_QUOTES, 'UTF-8');
    $label = htmlspecialchars((string)($options['label'] ?? 'Foto Acara (Rasio 16:9)'), ENT_QUOTES, 'UTF-8');
    $required = (bool)($options['required'] ?? true);
    $existingUrl = $options['existing_url'] ?? null;
    $existingName = $options['existing_name'] ?? null;
    $badgeText = htmlspecialchars((string)($options['badge_text'] ?? 'Wajib 1 foto · Rasio 16:9 utuh'), ENT_QUOTES, 'UTF-8');
    $hintText = htmlspecialchars((string)($options['hint_text'] ?? 'Tampil utuh (fit) tanpa terpotong di kartu & modal pemesanan · JPG, PNG, WEBP maks. 5MB'), ENT_QUOTES, 'UTF-8');

    $hasExisting = is_string($existingUrl) && $existingUrl !== '';
    $safeUrl = $hasExisting ? htmlspecialchars($existingUrl, ENT_QUOTES, 'UTF-8') : '';
    $safeName = $hasExisting ? htmlspecialchars((string)($existingName ?: basename($existingUrl)), ENT_QUOTES, 'UTF-8') : '';

    $dropHidden = $hasExisting ? ' hidden' : '';
    $prevHidden = $hasExisting ? '' : ' hidden';
    $reqAttr = $required ? '1' : '0';
    $hasSavedAttr = $hasExisting ? '1' : '0';

    $uploadSvg = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>';
    $imageSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>';
    $trashSvg = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>';

    return '
    <div class="form-field event-upload-widget" id="' . $widgetId . '" data-upload-widget data-required="' . $reqAttr . '" data-has-saved="' . $hasSavedAttr . '" data-saved-url="' . $safeUrl . '" data-saved-name="' . $safeName . '">
        <div class="event-upload-head">
            <label for="' . $inputId . '">' . $label . ($required ? ' <span class="req-star" aria-hidden="true">*</span>' : '') . '</label>
            <span class="upload-req-badge" data-upload-badge>' . $badgeText . '</span>
        </div>
        <p class="upload-hint-text">' . $hintText . '</p>

        <input type="file" id="' . $inputId . '" name="' . $inputName . '" accept="image/jpeg,image/png,image/webp,image/gif" class="sr-only-file" data-upload-input />

        <div class="event-dropzone" data-upload-dropzone tabindex="0" role="button" aria-label="Pilih atau seret foto acara rasio 16:9"' . $dropHidden . '>
            <div class="dropzone-icon-bubble">' . $imageSvg . '</div>
            <strong class="dropzone-title">Seret &amp; letakkan foto acara ke sini</strong>
            <span class="dropzone-sub">Atau pilih 1 gambar dari perangkatmu (rasio 16:9)</span>
            <button type="button" class="button button-quiet button-small" data-upload-pick>
                ' . $uploadSvg . '
                <span>Pilih Foto Acara</span>
            </button>
        </div>

        <div class="event-upload-preview" data-upload-preview' . $prevHidden . '>
            <button type="button" class="event-upload-thumb-16x9" data-upload-zoom aria-label="Perbesar pratinjau foto acara">
                <img src="' . $safeUrl . '" alt="Pratinjau foto acara" data-upload-img />
                <span class="media-zoom-chip">Perbesar</span>
            </button>
            <div class="event-upload-info">
                <span class="upload-status-pill">FOTO UTAMA · FIT 16:9</span>
                <strong class="upload-file-name" data-upload-filename>' . ($safeName ?: 'Foto acara terpilih') . '</strong>
                <span class="upload-file-meta" data-upload-meta>' . ($hasExisting ? 'Foto acara aktif tersimpan · klik Ganti Foto bila ingin memperbarui' : '') . '</span>
                <div class="event-upload-actions">
                    <button type="button" class="button button-quiet button-small" data-upload-replace>
                        ' . $uploadSvg . '
                        <span>Ganti Foto</span>
                    </button>
                    <button type="button" class="button button-danger button-small" data-upload-clear>
                        ' . $trashSvg . '
                        <span>Hapus</span>
                    </button>
                </div>
            </div>
        </div>

        <p class="form-error event-upload-error" data-upload-error role="alert"></p>
    </div>';
}

// Render ikon SVG monokrom ringan untuk info metadata di kartu event
function render_card_meta_icon(string $name, int $size = 14): string
{
    $paths = [
        'map-pin' => '<path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/>',
        'clock' => '<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>',
        'calendar' => '<rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>',
    ];
    $inner = $paths[$name] ?? '';
    if ($inner === '') {
        return '';
    }
    return '<svg class="meta-svg-icon" width="' . $size . '" height="' . $size . '" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' . $inner . '</svg>';
}
