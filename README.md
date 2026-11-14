# Tiketin Desktop

Aplikasi desktop pemesanan dan pengelolaan tiket event berbasis **Java 17 (Swing + FlatLaf)** yang terhubung ke database MySQL (`tiketin_db2`).

## Fitur Utama

- **Guest & Pembeli:** Jelajah katalog event, pencarian dan filter kategori, pemesanan tiket 2 langkah dengan kalkulasi subtotal otomatis, pembayaran transfer bank atau QRIS, upload bukti bayar (klik pemilih file OS atau *Drag & Drop* + pratinjau gambar), serta halaman **Tiket Saya** (tab *Mendatang* & *Riwayat* lengkap dengan kode tiket dan pratinjau bukti bayar).
- **Pengajuan Organizer:** Pengguna biasa dapat mengajukan diri menjadi organizer beserta draf event perdana, dengan pelacakan status (*Proses*, *Ditolak* beserta alasan, *Disetujui*).
- **Dashboard Organizer:** Ringkasan metrik, CRUD event, verifikasi pesanan masuk (setujui / batalkan dengan pengembalian kuota otomatis dan pratinjau bukti bayar), serta manajemen rekening pembayaran.
- **Panel Admin:** Ringkasan statistik, peninjauan pengajuan organizer (*Setujui* / *Tolak* dengan alasan), daftar pengguna, dan moderasi seluruh event.

## Prasyarat

- **Java 17** atau lebih baru (JRE/JDK)
- **MySQL / MariaDB** berjalan di `127.0.0.1:3306` dengan database `tiketin_db2` (konfigurasi dapat disesuaikan di `src/main/resources/database.properties`)
- *(Opsional)* **Apache Maven 3.8+** jika ingin melakukan kompilasi ulang dari source

## Cara Menjalankan

### macOS / Linux
```bash
./run.sh
```
khusus macOS bisa klik 2 kali run.command

### Windows
Klik dua kali `run.bat` atau jalankan lewat CMD/PowerShell:
```bat
run.bat
```

### Langsung via Java JAR
```bash
java -jar target/tiketin-desktop-2.0.0-jar-with-dependencies.jar
```

### Build Ulang dari Source (Opsional)
```bash
mvn clean package
```
