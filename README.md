# SakuPintar v2.0

**SakuPintar** adalah aplikasi pengelola keuangan pribadi lintas platform (**Android APK** dan **Portal Web**) dengan **sinkronisasi Cloud Firestore dua arah secara real-time**. Catat pengeluaran harian, foto struk belanja, susun anggaran otomatis, pantau saldo dompet serta celengan target langsung dari HP Android maupun browser—semua tersinkron otomatis lewat akun Google yang sama.

---

## ✨ Fitur Utama

### 📱 Aplikasi Android & 🌐 Portal Web (Sinkron Dua Arah)
- **Masuk dengan Google & Mode Guest**
  - Autentikasi cepat dan aman menggunakan **Akun Google** (Android `CredentialManager` & Web Google Sign-In) untuk sinkronisasi otomatis ke Cloud Firestore.
  - Tersedia **Mode Guest** agar pengguna bisa langsung mencoba fitur tanpa login terlebih dahulu (penyimpanan lokal di perangkat/browser).
- **Dashboard Ringkasan Pintar**
  - Memantau sisa anggaran bulan berjalan, total pengeluaran, **batas aman pengeluaran harian** berdasarkan sisa hari di bulan tersebut, total saldo dompet, serta progres per kategori.
- **Catatan Pengeluaran Harian & Foto Struk**
  - Tambah, ubah, dan hapus transaksi beserta catatan serta lampiran **foto struk** (dikompresi otomatis agar ringan dan kompatibel antara Android dan Web).
  - Filter berdasarkan pencarian kata kunci, kategori (`FOOD`, `TRANSPORT`, `BILLS`, `HEALTH`, `SHOPPING`, `ENTERTAINMENT`, `OTHER`), metode pembayaran (`QRIS`, `CASH`, `TRANSFER`, `CARD`), dan pemilih bulan.
- **Rekap & Ekspor Laporan Rapi**
  - Ekspor laporan pengeluaran ke **CSV / Excel** (UTF-8 BOM) dan cetak/simpan ke **PDF** dengan tata letak bersih dan status lampiran struk.
- **Perencana Anggaran Pintar (`BudgetAutoComposer`)**
  - Dukungan strategi **Seimbang (50/30/20)**, **Hemat Agresif (60/10/30)**, **Adaptif dari Riwayat Bulan Lalu**, serta **Kustom** dengan validasi total alokasi 100%.
  - Pembagian otomatis plafon per kategori dalam kelipatan Rp10.000.
- **Celengan Target & Saldo Dompet**
  - Kelola saldo per metode pembayaran (`QRIS`, `CASH`, `TRANSFER`, `CARD`).
  - Buat target tabungan (Dana Darurat, Gadget, Liburan, Kendaraan, Pendidikan, Rumah, dll.) dengan tombol tambah/kurang saldo cepat.
- **Wawasan Cerdas (Android)**
  - Analisis pola pengeluaran berbasis **Firebase AI Logic (Gemini)** langsung di aplikasi Android.
- **Personalisasi Tema & Bahasa**
  - Pergantian tema **Terang / Gelap** dengan transisi halus dan dukungan dwibahasa **Bahasa Indonesia (ID)** & **English (EN)**.
- **Portal Promosi & Unduh APK Otomatis**
  - Halaman web otomatis mengambil informasi versi rilis terbaru, ukuran berkas, dan tautan unduhan `.apk` langsung dari **GitHub Releases**.

---

## 🚀 Cara Menjalankan & Mengakses

### 1. Menggunakan Versi Web (GitHub Pages)
Jika repositori ini sudah diaktifkan GitHub Pages-nya (`Settings → Pages → Source: GitHub Actions`), portal web dapat diakses langsung di browser melalui:

```text
https://ramabilekk.github.io/Saku-Pintar/
```

### 2. Mengunduh Aplikasi Android (APK)
Unduh rilis APK terbaru melalui halaman **GitHub Releases**:

```text
https://github.com/ramabilekk/Saku-Pintar/releases/latest
```

### 3. Menjalankan Portal Web Secara Lokal
Karena portal web menggunakan ES Modules (`<script type="module">`), jalankan lewat server HTTP lokal di dalam folder `web/`:

```bash
cd web
python -m http.server 8080
```

Kemudian buka `http://localhost:8080` di browser.

### 4. Build & Test Aplikasi Android Secara Lokal
Pastikan JDK 21 dan Android SDK tersedia, lalu jalankan:

```bash
gradle assembleDebug
gradle :app:testDebugUnitTest
```
