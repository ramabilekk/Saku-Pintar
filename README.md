# SakuPintar v2.0

**SakuPintar** adalah aplikasi Android pengelola keuangan pribadi yang dilengkapi **sinkronisasi Cloud Firestore secara real-time** dan **asisten pintar berbasis Gemini AI**. Catat pengeluaran harian, lampirkan foto struk belanja, susun anggaran bulanan otomatis, serta pantau saldo dompet dan celengan impian langsung dari HP Android Anda.

---

## ✨ Fitur Utama

- **Masuk dengan Google & Mode Guest (Pengunjung)**
  - Autentikasi cepat dan aman menggunakan **Akun Google** (`CredentialManager`) untuk mencadangkan dan menyinkronkan data keuangan ke Cloud Firestore secara otomatis.
  - Tersedia **Mode Guest (Pengunjung)** agar pengguna dapat langsung menggunakan aplikasi tanpa login terlebih dahulu.
- **Beranda & Ringkasan Keuangan Pintar**
  - Memantau sisa anggaran belanja bulan berjalan, total pengeluaran, **batas aman pengeluaran harian** berdasarkan sisa hari di bulan tersebut, target tabungan, grafik pengeluaran 7 hari terakhir, serta progres alokasi per kategori.
- **Catatan Pengeluaran Harian, Kalender Bulanan & Foto Struk**
  - Tambah dan hapus transaksi harian lengkap dengan kategori, metode pembayaran (`QRIS`, `Tunai`, `Transfer`, `Kartu`), catatan, serta lampiran **foto struk belanja** (dari kamera atau galeri).
  - Dilengkapi **Isi Cepat dengan AI** (ketik kalimat singkat atau scan foto struk untuk mengisi otomatis), **Kalender Bulanan** interaktif per tanggal, serta **Pengeluaran Rutin** untuk mencatat tagihan tetap hanya dalam 1 ketukan.
- **Ekspor Rekap Laporan Bulanan (Excel, PDF & CSV)**
  - Ekspor laporan pengeluaran bulanan ke **Excel (`.xlsx`)**, **Dokumen PDF (`.pdf`)**, atau **CSV (`.csv`)** lengkap dengan ringkasan eksekutif dan galeri lampiran bukti foto struk, serta fitur bagikan langsung ke aplikasi lain.
- **Perencana Anggaran Otomatis (`50/30/20`, `60/10/30` & Adaptif)**
  - Menyusun alokasi anggaran bulanan otomatis berdasarkan pendapatan dengan pilihan strategi **Seimbang (50/30/20)**, **Hemat Maksimal (60/10/30)**, atau **Adaptif Berbasis Riwayat**, disertai pengatur plafon manual per kategori.
- **Celengan Impian & Saldo Dompet**
  - Pantau saldo aktif pada masing-masing dompet/sumber dana (`QRIS`, `Tunai`, `Transfer`, `Kartu`).
  - Buat target tabungan (Dana Darurat, Liburan, Gadget, Pendidikan, dll.) lengkap dengan pencatatan setor dan tarik dana.
- **Asisten Keuangan AI (Gemini)**
  - Analisis pola belanja bulanan dan rekomendasi penghematan cerdas langsung di dalam aplikasi.
- **Personalisasi Tema, Bahasa & Mata Uang**
  - Pilihan **Tema Terang / Gelap**, dukungan dwibahasa **Indonesia (ID)** & **English (EN)**, serta pilihan berbagai simbol mata uang (`IDR`, `USD`, `EUR`, `SGD`, `MYR`, `JPY`, `SAR`, `AUD`).

---

## 🚀 Cara Mengunduh & Menjalankan Aplikasi Android (APK)

### 1. Mengunduh File APK Siap Pakai
Unduh rilis APK Android terbaru melalui halaman **GitHub Releases**:

```text
https://github.com/ramabilekk/Saku-Pintar/releases/latest
```

### 2. Build & Test APK Secara Lokal
Pastikan JDK 21 dan Android SDK tersedia, lalu jalankan perintah berikut:

```bash
gradle assembleDebug
gradle :app:testDebugUnitTest
```
