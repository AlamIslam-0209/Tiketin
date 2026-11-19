# Tiketin Desktop — JavaFX 21 Edition

Aplikasi desktop pemesanan dan pengelolaan tiket acara berbasis **Java 17 & JavaFX 21** lintas platform (**macOS Apple Silicon, macOS Intel, Windows, Linux**) yang terhubung langsung ke MySQL (`tiketin_db2`).

## Fitur Utama

- **Jelajah Acara & Filter Spesifik:** Katalog kartu **4:3 foto event** responsif, *command search bar* (`⌘K` / `Ctrl+K`), tab kategori, urutkan jadwal/harga/kuota, serta modal filter rentang tanggal, harga, kota/venue, penyelenggara, dan kuota tersedia.
- **Pemesanan 1-Frame 2 Kolom & QRIS:** Halaman `Pesan Tiket` menampilkan detail acara + pratinjau poster 4:3 (*lightbox* saat diklik) di kiri, serta ringkasan pesanan, **QR Code dinamis** untuk QRIS, metode transfer bank, dan *Drag & Drop* / pilih file bukti bayar di kanan.
- **Tiket Saya & Status Terpakai Otomatis:** Filter status `Menunggu`, `Berhasil`, **`Terpakai`** (otomatis ketika tanggal dan jam acara terlewati), dan `Dibatalkan`, salin kode tiket cepat, serta pratinjau bukti bayar.
- **Autentikasi & Pengaturan Akun 1-Frame:** Masuk/Daftar di halaman tersendiri dan edit profil beserta unggah avatar dari menu Pengaturan tanpa membuka jendela terpisah.
- **Studio Organizer & Pengajuan:** Form pengajuan organizer beserta unggah foto poster acara perdana, pelacakan status (*Proses* / *Ditolak* dengan alasan), manajemen acara CRUD + upload foto 4:3, akordeon verifikasi pesanan masuk, dan manajemen rekening transfer/QRIS berdampingan 2 kolom.
- **Pusat Admin & Governance:** Review & persetujuan pengajuan organizer beserta pratinjau foto proposal, direktori pengguna (*read-only* demi keamanan role), dan moderasi acara global.

## Prasyarat

- **JDK 17+**
- **Apache Maven 3.8+**
- **MySQL / MariaDB** berjalan di `127.0.0.1:3306` dengan database `tiketin_db2` (konfigurasi di `src/main/resources/database.properties`)

## Cara Menjalankan

### macOS (Klik Ganda atau Terminal)
Klik ganda **`run.command`** di Finder atau jalankan di terminal:
```bash
./run.sh
```

### Windows
Klik ganda **`run.bat`** atau jalankan lewat CMD/PowerShell:
```bat
run.bat
```

### Langsung via Maven
```bash
mvn javafx:run
```
