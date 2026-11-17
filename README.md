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

## 🛠️ Teknologi yang Digunakan

### Android App (`app/`)
- **Bahasa & UI:** Kotlin, Jetpack Compose, Material Design 3 (Edge-to-Edge), Navigation Compose
- **Backend & AI:** Firebase Auth (Google Sign-In via Jetpack Credential Manager), Cloud Firestore (Named Database), Firebase AI Logic, Firebase App Check
- **Media & Build:** Coil, Gradle Kotlin DSL (`compileSdk 36`, `minSdk 24`, `targetSdk 36`), JDK 21
- **Testing:** JUnit4, Robolectric, Kotlin Coroutines Test

### Portal Web (`web/`)
- **Frontend:** HTML5, CSS3 responsif (Light/Dark theme + Print/PDF styling), Vanilla JavaScript ES Modules, PWA Web Manifest
- **Cloud Sync:** Firebase JS SDK v11 (Auth + Firestore `onSnapshot` real-time ke database bernama yang sama dengan APK)

### Keamanan & Otomasi
- **Firestore Security Rules (`firestore.rules`):** Zero-trust per-user isolation (`users/{userId}/...`), validasi tipe integer, batas panjang string, validasi format bulan (`YYYY-MM`), dan pemeriksaan `request.time`.
- **GitHub Actions (`.github/workflows/`):**
  - `build-apk.yml` — Otomatis membangun Debug APK (`v2.0.<run_number>`) dan menerbitkannya ke **GitHub Releases**.
  - `deploy-web.yml` — Otomatis menerbitkan folder `web/` ke **GitHub Pages** dan menyuntikkan konfigurasi Firebase Web dari GitHub Secrets.

---

## 📂 Struktur Direktori

```text
├── app/                     # Kode sumber aplikasi Android (Kotlin & Jetpack Compose)
├── web/                     # Portal Web + Promosi APK (HTML, CSS, JS, PWA Manifest)
│   ├── index.html
│   ├── styles.css
│   ├── app.js
│   ├── firebase-config.js
│   ├── icon.svg
│   └── manifest.webmanifest
├── .github/workflows/       # CI/CD otomatis Build APK Release & Deploy GitHub Pages
│   ├── build-apk.yml
│   └── deploy-web.yml
├── firestore.rules          # Aturan keamanan Cloud Firestore
├── firestore.test.js        # Pengujian aturan keamanan Firestore
├── firebase.json            # Konfigurasi Firebase Hosting & Firestore Emulator
└── gradle/                  # Gradle Version Catalog & konfigurasi build
```

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

---

## ⚙️ Konfigurasi Firebase & GitHub Secrets

Untuk mengaktifkan sinkronisasi penuh Google Sign-In dan Cloud Firestore pada build CI/CD:

1. **GitHub Pages**
   - Buka **Settings → Pages → Build and deployment → Source**, lalu pilih **`GitHub Actions`**.
2. **GitHub Repository Secrets** (`Settings → Secrets and variables → Actions`)
   - `FIREBASE_WEB_API_KEY` — Web API Key dari Firebase Web App (`</>`)
   - `FIREBASE_WEB_APP_ID` — Web App ID dari Firebase Web App (`</>`)
   - `GOOGLE_SERVICES_JSON` *(opsional untuk CI APK)* — Isi file `app/google-services.json` jika belum disertakan di repositori
   - `DEBUG_KEYSTORE_BASE64` *(opsional untuk CI APK)* — Keystore debug terenkode Base64 agar SHA-1 signing APK tetap konsisten dengan Firebase
3. **Firebase Authentication Authorized Domains**
   - Di **Firebase Console → Authentication → Settings → Authorized domains**, tambahkan domain host web Anda (misalnya `ramabilekk.github.io`) agar login Google di browser diizinkan.

---

## 📄 Catatan Keamanan
- Jangan pernah menaruh API key atau file kredensial sensitif secara langsung ke dalam repositori publik.
- Pengujian lokal aturan Firestore dapat dijalankan melalui konfigurasi emulator di `firebase.json` dan `firestore.test.js`.
