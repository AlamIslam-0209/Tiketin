# 🎫 PANDUAN KOLABORASI & PEMBAGIAN TUGAS TIM PROYEK TIKETIN

Dokumen ini adalah acuan resmi pembagian tugas dan pencatatan kontribusi kelompok untuk proyek **Tiketin - Sistem Informasi Pemesanan Tiket Event**. 

Seluruh poin pada dokumen ini disusun **100% berdasarkan fitur dan kode yang telah selesai dibangun dan berfungsi nyata pada sistem Tiketin saat ini**, sehingga merefleksikan hasil kolaborasi utuh dari 4 anggota tim: **Alam**, **Assyifa**, **Alif**, dan **Faiz**.

---

## 👥 1. Struktur Tim & Matriks Tanggung Jawab

| No | Nama Anggota | Role / Posisi | Modul Tanggung Jawab | Branch Fitur Git |
|:--:|:---|:---|:---|:---|
| 1 | **Alam** | **Project Lead & Core Architect** | Database Architecture, Authentication, Session Guard & Helper System | `feat/core-architecture-alam` |
| 2 | **Assyifa** | **Organizer & Application Specialist** | Alur Pengajuan Calon Organizer, Dashboard Organizer, Manajemen Event & Rekening | `feat/organizer-assyifa` |
| 3 | **Alif** | **Customer Experience & Ticketing Specialist** | Landing Page, Katalog Event, Sistem Booking & Upload Bukti Bayar, Halaman Tiket Saya | `feat/ticketing-alif` |
| 4 | **Faiz** | **Administration & Governance Specialist** | Dashboard Admin, Moderasi Akun & Event Global, Sistem Approval / Rejection Organizer | `feat/admin-faiz` |

---

## 📂 2. Pemetaan File & Modul yang Terealisasi di Kode Saat Ini

Setiap file dalam repositori Tiketin memiliki pembagian kepemilikan modul yang jelas dan saling terintegrasi:

```
Tiketin/
├── config/
│   └── database.php                --> [Alam] Koneksi database PDO, konfigurasi UTF-8, helper get_db()
├── database.sql                    --> [Alam] DDL skema database (users, events, tickets, orders, applications, payment)
├── includes/
│   ├── auth_check.php              --> [Alam] Session guard, require_login(), require_role(), is_logged_in()
│   ├── functions.php               --> [Alam] Flash message (set_flash, get_flash), format_rupiah(), format_tanggal_id()
│   ├── header.php                  --> [Alif] Navbar global, menu dinamis berdasarkan role user/organizer/admin
│   └── footer.php                  --> [Alif] Footer identitas brand dan copyright Tiketin
├── assets/                         --> [Alif] File CSS styling interaktif dan aset visual
├── login.php                       --> [Alam] Form login multi-role dengan validasi username & password
├── register.php                    --> [Alam] Form pendaftaran pengguna baru (default role 'user')
├── logout.php                      --> [Alam] Script pembersihan sesi user dan redirect aman
├── index.php                       --> [Alif] Beranda utama, hero section, katalog event, modal beli tiket & rekening transfer
├── tiket-saya.php                  --> [Alif] Dashboard tiket user, tab Mendatang vs Riwayat, kode tiket unik, modal bukti bayar
├── apply.php                       --> [Assyifa] Form pengajuan organizer, profil organisasi, kontak, dan draft event perdana
├── organizer.php                   --> [Assyifa] Dashboard organizer, rekap tiket terjual, CRUD event, manajemen rekening pembayaran
├── admin.php                       --> [Faiz] Panel admin, kartu statistik ringkasan, verifikasi pengajuan, audit akun & event
└── proses/
    ├── auth_process.php            --> [Alam] Backend handler verifikasi login & pendaftaran akun ke tabel users
    ├── order_process.php           --> [Alif] Backend handler reservasi tiket, validasi upload bukti bayar, potong kuota event
    ├── payment_process.php         --> [Assyifa] Backend handler tambah & hapus rekening pembayaran organizer (tabel payment)
    ├── event_process.php           --> [Assyifa & Faiz] Handler CRUD event organizer, verifikasi order tiket, dan moderasi admin
    └── application_process.php     --> [Assyifa & Faiz] Handler kirim pengajuan organizer (Assyifa) & keputusan admin approve/reject (Faiz)
```

---

## ✅ 3. Checklist Fitur yang Selesai Terealisasi (Status Selesai / Checked)

Seluruh checklist di bawah ini mendeskripsikan **fitur nyata yang sudah berjalan pada kode saat ini**:

### 🔵 Alam — Project Lead & Core Architect (`feat/core-architecture-alam`)
- [] **Arsitektur Database**: Merancang skema tabel MySQL lengkap (`users`, `events`, `orders`, `tickets`, `applications`, `payment`) dengan relasi foreign key pada `database.sql`.
- [] **Koneksi Database PDO**: Membangun `config/database.php` menggunakan PDO dengan error mode exception dan wrapper function `get_db()`.
- [] **Sistem Autentikasi**: Membangun alur masuk dan daftar akun pada `login.php`, `register.php`, dan `proses/auth_process.php` yang mendukung case-insensitive username (`LOWER(username)`).
- [] **Middleware & Role Restriction**: Membangun mekanisme proteksi akses di `includes/auth_check.php` dengan fungsi `require_login()`, `require_role('admin')`, `require_role('organizer')`, dan `current_user()`.
- [] **Global Helper & Session Flash**: Mengembangkan sistem notifikasi pesan di `includes/functions.php` (`set_flash()`, `get_flash()`) serta fungsi utilitas `format_rupiah()` dan `format_tanggal_id()`.
- [] **Pembersihan Sesi**: Mengimplementasikan `logout.php` yang menghapus sesi secara aman dan mengarahkan kembali ke beranda.

---

### 🟣 Assyifa — Organizer & Application Specialist (`feat/organizer-assyifa`)
- [x] **Formulir Pengajuan Organizer (`apply.php`)**: Membangun antarmuka pendaftaran calon organizer yang menginput identitas diri, nama organisasi, nomor kontak, serta form draf event pertama yang ingin diselenggarakan.
- [x] **Status Tracking Pengajuan**: Menyediakan notifikasi status peninjauan di `apply.php` (menampilkan box kuning jika pengajuan berstatus *pending*, dan box merah disertai alasan jika berstatus *rejected* dengan opsi pengajuan ulang).
- [x] **Backend Pengajuan (`proses/application_process.php` - Action `apply`)**: Memvalidasi data pendaftaran, mencegah pengajuan ganda saat masih *pending*, dan menyimpan berkas ke tabel `applications`.
- [x] **Dashboard Organizer (`organizer.php`)**: Membangun antarmuka dashboard khusus role `organizer` yang menampilkan ringkasan jumlah event aktif, total tiket terjual, dan sisa slot tiket.
- [x] **CRUD Event Organizer (`proses/event_process.php`)**: Mengimplementasikan aksi `create`, `update`, dan `delete` event yang hanya dapat dikelola oleh pemilik event tersebut (`owner_id = currentUser.id`).
- [x] **Manajemen Rekening Pembayaran (`proses/payment_process.php`)**: Membangun fitur tambah dan hapus nomor rekening bank / QRIS milik organizer pada tabel `payment` agar muncul otomatis sebagai opsi pembayaran saat pembeli memesan tiket.
- [x] **Verifikasi Order Masuk**: Mengembangkan tabel riwayat pesanan tiket masuk di dashboard organizer untuk memantau nama pembeli, jumlah tiket, dan memperbarui status order.

---

### 🟢 Alif — Customer Experience & Ticketing Specialist (`feat/ticketing-alif`)
- [] **Antarmuka Beranda & Navbar Publik (`index.php`, `includes/header.php`, `includes/footer.php`)**: Merancang hero section *"Satu tempat untuk semua momen"*, navigasi interaktif, dan tautan login/register dinamis sesuai status sesi user.
- [] **Katalog & Kartu Event (`index.php`)**: Menampilkan daftar event dari database secara urut tanggal, lengkap dengan nama organizer, tanggal, jam, lokasi, badge kategori, slot tiket tersisa, dan tombol "Pesan Tiket".
- [] **Modal Pemesanan & Alur Checkout (`index.php`)**: Membangun pop-up modal pemesanan tiket dengan batasan 1–10 tiket per transaksi, kalkulasi subtotal harga, serta pilihan metode transfer bank milik organizer dari tabel `payment` maupun QRIS.
- [] **Upload Bukti Pembayaran (`proses/order_process.php`)**: Membangun validasi upload file bukti transfer pembayaran (pengecekan ekstensi gambar JPG/PNG/WEBP, verifikasi MIME type, batasan ukuran 5 MB, dan penyimpanan unik ke direktori `uploads/proofs/`).
- [] **Penerbitan Kode Tiket Unik (`proses/order_process.php`)**: Mengembangkan generator kode tiket otomatis berformat unik (`TKT-YYYYMM-XXXX`) pada tabel `tickets` serta pengurangan kuota slot event secara atomik.
- [] **Dashboard Tiket Saya (`tiket-saya.php`)**: Membangun halaman riwayat pesanan pengguna yang membagi tiket ke dalam dua tab: **Tiket Mendatang** dan **Riwayat Transaksi**.
- [] **Tampilan Kartu Tiket & Modal Bukti Transfer (`tiket-saya.php`)**: Menampilkan detail event, kode tiket yang terbit, badge status transaksi (*Confirmed*, *Pending*, *Rejected*), dan modal interaktif untuk melihat screenshot bukti transfer yang telah diunggah.

---

### 🟠 Faiz — Administration & Governance Specialist (`feat/admin-faiz`)
- [] **Dashboard Utama Admin (`admin.php`)**: Membangun panel kendali yang hanya dapat diakses oleh role `admin`, dilengkapi 3 kartu ringkasan metrik: Total Akun Terdaftar, Pengajuan Menunggu Persetujuan, dan Total Event Tayang.
- [] **Sistem Peninjauan Pengajuan Organizer (`admin.php`)**: Menyediakan daftar seluruh calon organizer yang mendaftar beserta detail organisasi, kontak pemohon, dan spesifikasi draf event perdananya.
- [] **Logika Keputusan Admin (`proses/application_process.php` - Action `decide`)**:
  - **Aksi Setujui (*Approve*)**: Menggunakan transaksi database (`beginTransaction`) untuk mengubah status pengajuan menjadi `approved`, menaikkan role user dari `user` menjadi `organizer`, memperbarui nama organisasi, dan secara otomatis langsung menerbitkan draf event pertama ke tabel `events`.
  - **Aksi Tolak (*Reject*)**: Memvalidasi input wajib alasan penolakan dan menyimpan keterangan penolakan ke database agar calon organizer dapat membaca alasannya di `apply.php`.
- [] **Daftar & Audit Seluruh Akun Pengguna (`admin.php`)**: Menampilkan daftar seluruh user yang terdaftar di platform Tiketin beserta badge role masing-masing (`Admin`, `Organizer`, `User`) dan tanggal registrasi.
- [] **Moderasi Event Global (`admin.php` & `proses/event_process.php`)**:
  - Menyediakan form edit event interaktif langsung di halaman admin untuk mengubah nama, jadwal, lokasi, kategori, harga, dan kuota event yang melanggar ketentuan.
  - Menyediakan tombol hapus event dengan konfirmasi dialog untuk menjaga ketertiban konten di platform Tiketin.

---

## 🤝 4. Kesepakatan Tim (Team Collaboration Agreement)

1. **Prinsip Branching**:
   - Seluruh anggota tim bekerja di branch masing-masing sesuai nama fiturnya.
   - Branch `main` dilindungi dan hanya menerima penggabungan kode melalui Pull Request yang telah disetujui.
2. **Aturan Penamaan Branch**:
   - **Alam**: `feat/core-architecture`
   - **Assyifa**: `feat/organizer`
   - **Alif**: `feat/ticketing`
   - **Faiz**: `feat/admin`
3. **Format Pesan Commit (Conventional Commits)**:
   - `feat: <penjelasan fitur>` (contoh: `feat: implementasi form booking tiket dan validasi upload bukti transfer`)
   - `fix: <penjelasan perbaikan>` (contoh: `fix: perbaiki query penghitungan sisa slot tiket`)
   - `style: <perubahan visual>` (contoh: `style: perbaiki responsivitas modal pemesanan tiket`)
   - `refactor: <perapihan kode>` (contoh: `refactor: optimasi pengecekan role di auth_check.php`)
4. **Alur Merge**:
   - Setiap anggota membuat Pull Request ke `main` di GitHub.
   - **Alam** bertindak sebagai Lead Reviewer yang memeriksa kelayakan kode sebelum melakukan *Merge pull request*.

---

## 🛠️ 5. Panduan Praktis Perintah Git Step-by-Step

Panduan ini dapat langsung dijalankan oleh masing-masing anggota tim di terminal mereka:

---

### Langkah 1: Clone Repository & Masuk ke Folder Proyek
```bash
git clone https://github.com/AlamIslam-0209/Tiketin.git
cd Tiketin
```

---

### Langkah 2: Membuat dan Pindah ke Feature Branch Masing-Masing
Pastikan berada pada branch `main` terbaru:
```bash
git checkout main
git pull origin main

# Untuk Assyifa:
git checkout -b feat/organizer

# Untuk Alif:
git checkout -b feat/ticketing

# Untuk Faiz:
git checkout -b feat/admin

# Untuk Alam:
git checkout -b feat/core-architecture
```

Periksa bahwa branch yang aktif sudah sesuai:
```bash
git branch
```

---

### Langkah 3: Melakukan Modifikasi / Pemolesan Kode
Lakukan perubahan, penambahan komentar penjelas, atau optimasi pada file yang menjadi bagian tanggung jawab masing-masing (lihat Bab 2).

---

### Langkah 4: Staging dan Pembuatan Commit
```bash
git status
git add .
git commit -m "feat: optimasi modul dan alur pemrosesan data"
```

---

### Langkah 6: Push Branch ke GitHub
```bash
# Untuk Assyifa:
git push -u origin feat/organizer-assyifa

# Untuk Alif:
git push -u origin feat/ticketing-alif

# Untuk Faiz:
git push -u origin feat/admin-faiz

# Untuk Alam:
git push -u origin feat/core-architecture-alam
```

---

### Langkah 7: Pengajuan Pull Request (PR) di Website GitHub
1. Buka repositori `https://github.com/AlamIslam-0209/Tiketin` di browser.
2. Klik tombol hijau/kuning **"Compare & pull request"** yang muncul di bagian atas halaman.
3. Pastikan konfigurasi branch:
   - **base repository / branch**: `dev`
   - **head repository / branch**: `feat/nama-branch-anda`
4. Masukkan judul PR yang informatif, contoh:
   - `[FEAT] Implementasi Modul Dashboard & Manajemen Event Organizer` (Assyifa)
   - `[FEAT] Integrasi Alur Checkout Tiket & Halaman Tiket Saya` (Alif)
   - `[FEAT] Implementasi Panel Kendali Admin & Moderasi Pengajuan` (Faiz)
5. Isi deskripsi PR dengan mencantumkan daftar file yang dikerjakan dan fitur yang diselesaikan.
6. Klik tombol **Create pull request**.

---

### Langkah 8: Review & Merge oleh Project Lead (Alam)
1. Alam membuka tab **Pull Requests** di GitHub.
2. Memeriksa tab **Files changed** untuk meninjau perubahan kode.
3. Memberikan review approval, lalu klik tombol **Merge pull request** -> **Confirm merge**.
4. Dengan ini, kontribusi masing-masing anggota tim resmi tercatat di riwayat commit `dev`.

---

### Langkah 9: Sinkronisasi Ulang di Komputer Masing-Masing
Setelah PR di-merge ke `dev`, seluruh anggota tim memperbarui repositori lokal mereka:
```bash
git checkout dev
git pull origin dev
```

---



---
*Tiketin Project - 2026. Dokumentasi resmi kolaborasi tim Kelompok 7.*
