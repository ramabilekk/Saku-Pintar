// SakuPintar Web v2.0 — Two-Way Cloud Firestore Sync with Android APK + Guest Mode + APK Promo

const CATEGORY_META = {
  FOOD: { labelId: "Makanan & Minuman", labelEn: "Food & Dining", color: "#2563eb", short: "FD", group: "NEEDS", weight: 0.38 },
  TRANSPORT: { labelId: "Transportasi", labelEn: "Transport", color: "#0891b2", short: "TR", group: "NEEDS", weight: 0.18 },
  BILLS: { labelId: "Tagihan & Utilitas", labelEn: "Bills & Utilities", color: "#7c3aed", short: "BL", group: "NEEDS", weight: 0.32 },
  HEALTH: { labelId: "Kesehatan", labelEn: "Health", color: "#059669", short: "HL", group: "NEEDS", weight: 0.12 },
  SHOPPING: { labelId: "Belanja", labelEn: "Shopping", color: "#d97706", short: "SH", group: "WANTS", weight: 0.45 },
  ENTERTAINMENT: { labelId: "Hiburan", labelEn: "Entertainment", color: "#ec4899", short: "EN", group: "WANTS", weight: 0.35 },
  OTHER: { labelId: "Lainnya", labelEn: "Other", color: "#64748b", short: "OT", group: "WANTS", weight: 0.20 },
};

const PAYMENT_METHODS = [
  { key: "QRIS", labelId: "QRIS / E-Wallet", labelEn: "QRIS / E-Wallet", icon: "📱" },
  { key: "CASH", labelId: "Tunai (Cash)", labelEn: "Cash", icon: "💵" },
  { key: "TRANSFER", labelId: "Transfer Bank", labelEn: "Bank Transfer", icon: "🏦" },
  { key: "CARD", labelId: "Kartu Debit/Kredit", labelEn: "Debit/Credit Card", icon: "💳" },
];

const GOAL_CATEGORIES = {
  EMERGENCY: { labelId: "Dana Darurat", labelEn: "Emergency Fund", icon: "🛡️" },
  GADGET: { labelId: "Gadget & Elektronik", labelEn: "Gadgets", icon: "💻" },
  TRAVEL: { labelId: "Liburan", labelEn: "Travel", icon: "✈️" },
  VEHICLE: { labelId: "Kendaraan", labelEn: "Vehicle", icon: "🛵" },
  EDUCATION: { labelId: "Pendidikan", labelEn: "Education", icon: "🎓" },
  HOME: { labelId: "Rumah", labelEn: "Home", icon: "🏠" },
  OTHER: { labelId: "Lainnya", labelEn: "Other", icon: "🎯" },
};

const STORAGE_KEYS = {
  THEME: "sakupintar_web_theme",
  LANG: "sakupintar_web_lang",
  GUEST_DATA: "sakupintar_guest_workspace_v2",
  FIREBASE_CFG: "sakupintar_firebase_web_cfg",
  AUTH_PREF: "sakupintar_web_auth_pref",
};

const I18N = {
  id: {
    brandTagline: "Sinkron Web & Android APK",
    themeLight: "Terang",
    themeDark: "Gelap",
    navDownloadApk: "Unduh APK",
    heroBadgeSync: "Sinkron Real-Time Web ↔ APK",
    heroBadgeFree: "Gratis Tanpa Langganan",
    heroTitle: "Kelola Keuangan dari Browser, Sinkron Otomatis ke HP Android Anda",
    heroSubtitle:
      "Catat pengeluaran harian, foto struk belanja, atur anggaran 50/30/20, dan pantau celengan target langsung di browser. Masuk dengan Google agar semua data langsung tersinkron dua arah dengan aplikasi Android SakuPintar.",
    heroCtaApk: "Download APK Android",
    heroCtaWeb: "Gunakan di Browser",
    heroCtaGuide: "Cara Pasang APK",
    apkCardEyebrow: "APLIKASI ANDROID RESMI",
    apkCardTitle: "SakuPintar untuk Android",
    apkCardDesc:
      "Lebih cepat di genggaman dengan kamera struk instan, notifikasi cerdas, dan offline cache native Material 3.",
    apkFeat1: "Sinkron otomatis lewat akun Google yang sama",
    apkFeat2: "Foto struk langsung dari kamera & rekap PDF/CSV",
    apkFeat3: "Cepat tanpa OTP rumit — cukup Google atau Guest",
    apkDownloadNow: "Download APK Sekarang",
    btnGoogleSignIn: "Masuk dengan Google (Sinkron APK)",
    btnGuestMode: "Mode Guest",
    btnFirebaseConfig: "Konfigurasi Cloud",
    btnSignOut: "Keluar",
    tabDashboard: "Ringkasan",
    tabExpenses: "Pengeluaran",
    tabBudget: "Anggaran",
    tabSavings: "Celengan & Dompet",
    metricRemainingBudget: "Sisa Anggaran Bulan Ini",
    metricTotalSpent: "Total Pengeluaran",
    metricDailySafe: "Batas Aman Harian",
    metricTotalWallet: "Total Saldo Dompet",
    dashCategoryTitle: "Distribusi Pengeluaran Kategori",
    btnAddExpense: "Tambah Transaksi",
    dashRecentTitle: "Transaksi Terbaru",
    btnSeeAll: "Lihat Semua",
    bannerTitle: "Ingin catat pengeluaran lebih praktis tanpa buka browser?",
    bannerSub:
      "Pasang SakuPintar APK v2.0 di HP Android Anda — login dengan akun Google yang sama untuk sinkron otomatis!",
    expensesHeading: "Catatan Pengeluaran Harian",
    btnExportCsv: "Ekspor CSV / Excel",
    btnPrintPdf: "Cetak / Simpan PDF",
    searchPlaceholder: "Cari judul atau catatan...",
    filterAllCat: "Semua Kategori",
    filterAllMethod: "Semua Metode",
    budgetPlannerTitle: "Atur Anggaran Pintar",
    labelIncome: "Pemasukan Bulanan (Rp)",
    labelStrategy: "Strategi Alokasi",
    labelNeedsPct: "Kebutuhan %",
    labelWantsPct: "Keinginan %",
    labelSavingsPct: "Tabungan %",
    btnSaveBudget: "Simpan & Sinkronkan Anggaran",
    budgetCatBreakdown: "Batas Per Kategori Bulan Ini",
    walletsHeading: "Saldo Dompet & Metode Bayar",
    walletsSub: "Klik simpan pada tiap metode untuk menyinkronkan saldo dengan aplikasi Android.",
    goalsHeading: "Celengan Target",
    btnAddGoal: "Buat Target Baru",
    footerDesc: "Aplikasi Web & Android Pengelola Keuangan Pribadi.",
    labelTitle: "Judul Pengeluaran",
    labelAmount: "Nominal (Rp)",
    labelDate: "Tanggal",
    labelCategory: "Kategori",
    labelMethod: "Metode Bayar",
    labelNote: "Catatan (Opsional)",
    labelReceipt: "Foto Struk (Otomatis dikompres agar sinkron dengan APK)",
    btnRemoveReceipt: "Hapus Foto",
    btnCancel: "Batal",
    btnSave: "Simpan",
    labelGoalTitle: "Nama Target",
    labelGoalTarget: "Target (Rp)",
    labelGoalSaved: "Saldo Awal (Rp)",
    firebaseModalTitle: "Hubungkan Firebase Web (Sinkron APK)",
  },
  en: {
    brandTagline: "Web & Android APK Sync",
    themeLight: "Light",
    themeDark: "Dark",
    navDownloadApk: "Get APK",
    heroBadgeSync: "Real-Time Web ↔ APK Sync",
    heroBadgeFree: "Free Forever",
    heroTitle: "Manage Your Money in Browser, Synced Instantly with Your Android Phone",
    heroSubtitle:
      "Log daily expenses, attach receipt photos, plan smart 50/30/20 budgets, and track savings goals right from your browser. Sign in with Google for two-way live sync with the SakuPintar Android APK.",
    heroCtaApk: "Download Android APK",
    heroCtaWeb: "Use in Browser",
    heroCtaGuide: "Install Guide",
    apkCardEyebrow: "OFFICIAL ANDROID APP",
    apkCardTitle: "SakuPintar for Android",
    apkCardDesc:
      "Faster on mobile with instant camera receipt capture, smart alerts, and native Material 3 offline caching.",
    apkFeat1: "Automatic sync via the same Google account",
    apkFeat2: "Camera receipt capture & clean PDF/CSV reports",
    apkFeat3: "Fast login without OTP friction — Google or Guest",
    apkDownloadNow: "Download APK Now",
    btnGoogleSignIn: "Sign in with Google (APK Sync)",
    btnGuestMode: "Guest Mode",
    btnFirebaseConfig: "Cloud Config",
    btnSignOut: "Sign Out",
    tabDashboard: "Overview",
    tabExpenses: "Expenses",
    tabBudget: "Budget",
    tabSavings: "Savings & Wallets",
    metricRemainingBudget: "Remaining Monthly Budget",
    metricTotalSpent: "Total Spent",
    metricDailySafe: "Daily Safe Limit",
    metricTotalWallet: "Total Wallet Balance",
    dashCategoryTitle: "Category Spending Breakdown",
    btnAddExpense: "Add Expense",
    dashRecentTitle: "Recent Transactions",
    btnSeeAll: "View All",
    bannerTitle: "Want quicker expense logging without opening a browser?",
    bannerSub:
      "Install SakuPintar APK v2.0 on your Android device — sign in with the same Google account to auto-sync!",
    expensesHeading: "Daily Expense Log",
    btnExportCsv: "Export CSV / Excel",
    btnPrintPdf: "Print / Save PDF",
    searchPlaceholder: "Search title or notes...",
    filterAllCat: "All Categories",
    filterAllMethod: "All Methods",
    budgetPlannerTitle: "Smart Budget Planner",
    labelIncome: "Monthly Income (Rp)",
    labelStrategy: "Allocation Strategy",
    labelNeedsPct: "Needs %",
    labelWantsPct: "Wants %",
    labelSavingsPct: "Savings %",
    btnSaveBudget: "Save & Sync Budget",
    budgetCatBreakdown: "Monthly Category Limits",
    walletsHeading: "Wallet & Payment Method Balances",
    walletsSub: "Save each balance to sync across web and Android app.",
    goalsHeading: "Savings Goals",
    btnAddGoal: "New Goal",
    footerDesc: "Web & Android Personal Finance Manager.",
    labelTitle: "Expense Title",
    labelAmount: "Amount (Rp)",
    labelDate: "Date",
    labelCategory: "Category",
    labelMethod: "Payment Method",
    labelNote: "Notes (Optional)",
    labelReceipt: "Receipt Photo (Auto-compressed for APK sync)",
    btnRemoveReceipt: "Remove Photo",
    btnCancel: "Cancel",
    btnSave: "Save",
    labelGoalTitle: "Goal Name",
    labelGoalTarget: "Target (Rp)",
    labelGoalSaved: "Initial Saved (Rp)",
    firebaseModalTitle: "Connect Firebase Web (APK Sync)",
  },
};

const now = new Date();
const currentMonthKey = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;

const state = {
  lang: localStorage.getItem(STORAGE_KEYS.LANG) || "id",
  theme: localStorage.getItem(STORAGE_KEYS.THEME) || "light",
  activeTab: "dashboard",
  selectedMonthKey: currentMonthKey,
  mode: "guest", // "guest" | "cloud"
  user: null,
  expenses: [],
  budgetsByMonth: {},
  goals: [],
  wallets: { QRIS: 0, CASH: 0, TRANSFER: 0, CARD: 0 },
  pendingReceiptBase64: "",
  firebaseRuntime: null, // { app, auth, db, modules }
  unsubscribes: [],
};

// ---------------- Utility Helpers ----------------

function $(id) {
  return document.getElementById(id);
}

function escHtml(str) {
  return String(str ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function formatRupiah(amount) {
  const num = Math.round(Number(amount) || 0);
  const abs = Math.abs(num);
  const formatted = new Intl.NumberFormat("id-ID").format(abs);
  return num < 0 ? `-Rp ${formatted}` : `Rp ${formatted}`;
}

function showToast(msg) {
  const el = $("toast");
  if (!el) return;
  el.textContent = msg;
  el.classList.add("visible");
  clearTimeout(showToast._timer);
  showToast._timer = setTimeout(() => el.classList.remove("visible"), 3200);
}

function safeId(prefix = "sp") {
  const rand = Math.random().toString(36).slice(2, 10);
  return `${prefix}_${Date.now()}_${rand}`.replace(/[^a-zA-Z0-9_-]/g, "");
}

function dateInputToEpochDay(dateStr) {
  const parts = String(dateStr || "").split("-").map(Number);
  if (parts.length !== 3 || parts.some(Number.isNaN)) {
    const t = new Date();
    return Math.floor(Date.UTC(t.getFullYear(), t.getMonth(), t.getDate()) / 86400000);
  }
  const [y, m, d] = parts;
  return Math.floor(Date.UTC(y, m - 1, d) / 86400000);
}

function epochDayToDateStr(epochDay) {
  const ms = Number(epochDay || 0) * 86400000;
  const d = new Date(ms);
  const y = d.getUTCFullYear();
  const m = String(d.getUTCMonth() + 1).padStart(2, "0");
  const day = String(d.getUTCDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

function todayDateStr() {
  const t = new Date();
  return `${t.getFullYear()}-${String(t.getMonth() + 1).padStart(2, "0")}-${String(t.getDate()).padStart(2, "0")}`;
}

function formatMonthLabel(monthKey) {
  const [y, m] = monthKey.split("-").map(Number);
  const dt = new Date(y, (m || 1) - 1, 1);
  return new Intl.DateTimeFormat(state.lang === "id" ? "id-ID" : "en-US", {
    month: "long",
    year: "numeric",
  }).format(dt);
}

function shiftMonthKey(monthKey, delta) {
  const [y, m] = monthKey.split("-").map(Number);
  const dt = new Date(y, (m || 1) - 1 + delta, 1);
  return `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, "0")}`;
}

function categoryName(key) {
  const meta = CATEGORY_META[key] || CATEGORY_META.OTHER;
  return state.lang === "id" ? meta.labelId : meta.labelEn;
}

function methodName(key) {
  const found = PAYMENT_METHODS.find((m) => m.key === key);
  if (!found) return key;
  return state.lang === "id" ? found.labelId : found.labelEn;
}

function openModal(id) {
  const el = $(id);
  if (el) el.classList.add("open");
}

function closeModal(id) {
  const el = $(id);
  if (el) el.classList.remove("open");
}

// ---------------- Theme & i18n ----------------

function applyTheme(theme) {
  state.theme = theme === "dark" ? "dark" : "light";
  document.documentElement.setAttribute("data-theme", state.theme);
  localStorage.setItem(STORAGE_KEYS.THEME, state.theme);
  $("themeLightBtn")?.classList.toggle("active", state.theme === "light");
  $("themeDarkBtn")?.classList.toggle("active", state.theme === "dark");
}

function applyLanguage(lang) {
  state.lang = lang === "en" ? "en" : "id";
  document.documentElement.setAttribute("lang", state.lang);
  localStorage.setItem(STORAGE_KEYS.LANG, state.lang);
  $("langIdBtn")?.classList.toggle("active", state.lang === "id");
  $("langEnBtn")?.classList.toggle("active", state.lang === "en");

  const dict = I18N[state.lang];
  document.querySelectorAll("[data-i18n]").forEach((el) => {
    const key = el.getAttribute("data-i18n");
    if (dict[key]) el.textContent = dict[key];
  });
  document.querySelectorAll("[data-i18n-placeholder]").forEach((el) => {
    const key = el.getAttribute("data-i18n-placeholder");
    if (dict[key]) el.setAttribute("placeholder", dict[key]);
  });

  renderAll();
}

// ---------------- GitHub Release APK Lookup ----------------

async function loadLatestApkRelease() {
  const cfg = window.SAKUPINTAR_FIREBASE_CONFIG || {};
  const fallbackUrl = cfg.releasesPageUrl || "https://github.com/ramabilekk/Saku-Pintar/releases/latest";
  const apiUrl = cfg.releasesApiUrl || "https://api.github.com/repos/ramabilekk/Saku-Pintar/releases/latest";

  try {
    const res = await fetch(apiUrl, { headers: { Accept: "application/vnd.github+json" } });
    if (!res.ok) return;
    const data = await res.json();
    const tag = data.tag_name || "v2.0";
    const apkAsset = Array.isArray(data.assets)
      ? data.assets.find((a) => String(a.name || "").toLowerCase().endsWith(".apk"))
      : null;

    const downloadUrl = apkAsset?.browser_download_url || data.html_url || fallbackUrl;
    const sizeMb = apkAsset?.size ? `${(apkAsset.size / (1024 * 1024)).toFixed(1)} MB` : "Android APK";

    if ($("apkVersionBadge")) $("apkVersionBadge").textContent = tag;
    if ($("apkAssetName")) $("apkAssetName").textContent = apkAsset?.name || `SakuPintar-${tag}.apk`;
    if ($("apkAssetSize")) $("apkAssetSize").textContent = sizeMb;

    ["topApkDownloadBtn", "heroApkBtn", "apkDirectDownloadLink", "bannerApkLink", "guideApkBtn"].forEach(
      (btnId) => {
        const el = $(btnId);
        if (el) el.setAttribute("href", downloadUrl);
      }
    );
  } catch {
    // Keep fallback GitHub Releases latest link cleanly
  }
}

// ---------------- Image Compression (Matches Android 200k Firestore Limit) ----------------

async function compressReceiptFile(file) {
  const dataUrl = await new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ""));
    reader.onerror = () => reject(new Error("Gagal membaca file gambar"));
    reader.readAsDataURL(file);
  });

  const img = await new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error("Format gambar tidak didukung"));
    image.src = dataUrl;
  });

  const maxSide = 960;
  const ratio = Math.min(1, maxSide / Math.max(img.width, img.height));
  const w = Math.max(1, Math.round(img.width * ratio));
  const h = Math.max(1, Math.round(img.height * ratio));

  const canvas = document.createElement("canvas");
  canvas.width = w;
  canvas.height = h;
  const ctx = canvas.getContext("2d");
  ctx.drawImage(img, 0, 0, w, h);

  let quality = 0.82;
  let base64 = "";
  while (quality >= 0.32) {
    const out = canvas.toDataURL("image/jpeg", quality);
    const stripped = out.replace(/^data:image\/[^;]+;base64,/, "");
    if (stripped.length <= 185000) {
      base64 = stripped;
      break;
    }
    quality -= 0.1;
  }

  if (!base64) {
    canvas.width = Math.max(1, Math.round(w * 0.65));
    canvas.height = Math.max(1, Math.round(h * 0.65));
    canvas.getContext("2d").drawImage(img, 0, 0, canvas.width, canvas.height);
    const out = canvas.toDataURL("image/jpeg", 0.55);
    base64 = out.replace(/^data:image\/[^;]+;base64,/, "").slice(0, 190000);
  }
  return base64;
}

// ---------------- Budget Auto Composer (Matches Android Logic) ----------------

function computeCategoryBudgets(monthlyIncome, needsPct, wantsPct, strategy, monthKey) {
  const spendablePool = Math.max(0, Math.round((monthlyIncome * (needsPct + wantsPct)) / 100));
  const needsTotal = Math.round((monthlyIncome * needsPct) / 100);
  const wantsTotal = Math.max(0, spendablePool - needsTotal);

  const raw = {};
  if (strategy === "ADAPTIVE_HISTORY") {
    const prevKey = shiftMonthKey(monthKey, -1);
    const prevTotals = {};
    let prevSum = 0;
    state.expenses
      .filter((e) => e.monthKey === prevKey)
      .forEach((e) => {
        prevTotals[e.category] = (prevTotals[e.category] || 0) + Number(e.amount || 0);
        prevSum += Number(e.amount || 0);
      });

    if (prevSum > 0) {
      Object.keys(CATEGORY_META).forEach((cat) => {
        raw[cat] = Math.round((spendablePool * (prevTotals[cat] || 0)) / prevSum);
      });
      return normalizeBudgetMap(raw, spendablePool);
    }
  }

  Object.entries(CATEGORY_META).forEach(([cat, meta]) => {
    const base = meta.group === "NEEDS" ? needsTotal : wantsTotal;
    raw[cat] = Math.round(base * meta.weight);
  });
  return normalizeBudgetMap(raw, spendablePool);
}

function normalizeBudgetMap(raw, targetSum) {
  const keys = Object.keys(CATEGORY_META);
  const rounded = {};
  let sum = 0;
  keys.forEach((k) => {
    const val = Math.max(0, Math.round((raw[k] || 0) / 10000) * 10000);
    rounded[k] = val;
    sum += val;
  });
  const diff = targetSum - sum;
  if (diff !== 0) {
    rounded.FOOD = Math.max(0, (rounded.FOOD || 0) + diff);
  }
  return rounded;
}

function getBudgetForMonth(monthKey) {
  if (state.budgetsByMonth[monthKey]) return state.budgetsByMonth[monthKey];
  const income = 5000000;
  return {
    monthKey,
    monthlyIncome: income,
    strategy: "BALANCED_50_30_20",
    savingsTarget: 1000000,
    needsPercent: 50,
    wantsPercent: 30,
    savingsPercent: 20,
    categoryBudgets: computeCategoryBudgets(income, 50, 30, "BALANCED_50_30_20", monthKey),
    _isDefault: true,
  };
}

// ---------------- Guest Local Storage Layer ----------------

function loadGuestWorkspace() {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.GUEST_DATA);
    if (!raw) {
      state.expenses = [];
      state.budgetsByMonth = {};
      state.goals = [];
      state.wallets = { QRIS: 0, CASH: 0, TRANSFER: 0, CARD: 0 };
      return;
    }
    const parsed = JSON.parse(raw);
    state.expenses = Array.isArray(parsed.expenses) ? parsed.expenses : [];
    state.budgetsByMonth = parsed.budgetsByMonth || {};
    state.goals = Array.isArray(parsed.goals) ? parsed.goals : [];
    state.wallets = {
      QRIS: Number(parsed.wallets?.QRIS || 0),
      CASH: Number(parsed.wallets?.CASH || 0),
      TRANSFER: Number(parsed.wallets?.TRANSFER || 0),
      CARD: Number(parsed.wallets?.CARD || 0),
    };
  } catch {
    state.expenses = [];
    state.budgetsByMonth = {};
    state.goals = [];
    state.wallets = { QRIS: 0, CASH: 0, TRANSFER: 0, CARD: 0 };
  }
}

function persistGuestWorkspace() {
  if (state.mode !== "guest") return;
  localStorage.setItem(
    STORAGE_KEYS.GUEST_DATA,
    JSON.stringify({
      expenses: state.expenses,
      budgetsByMonth: state.budgetsByMonth,
      goals: state.goals,
      wallets: state.wallets,
    })
  );
}

// ---------------- Firebase Web Resolution & Real-Time Firestore Sync ----------------

async function resolveFirebaseConfig() {
  const base = { ...(window.SAKUPINTAR_FIREBASE_CONFIG || {}) };

  try {
    const stored = JSON.parse(localStorage.getItem(STORAGE_KEYS.FIREBASE_CFG) || "null");
    if (stored && stored.apiKey && stored.appId) {
      return { ...base, ...stored };
    }
  } catch {
    // ignore malformed local config
  }

  if (base.apiKey && base.appId) {
    return base;
  }

  try {
    const res = await fetch("/__/firebase/init.json");
    if (res.ok) {
      const autoCfg = await res.json();
      if (autoCfg?.apiKey && autoCfg?.appId) {
        return { ...base, ...autoCfg };
      }
    }
  } catch {
    // Not deployed on Firebase Hosting
  }

  return base;
}

async function ensureFirebaseRuntime() {
  if (state.firebaseRuntime) return state.firebaseRuntime;

  const cfg = await resolveFirebaseConfig();
  if (!cfg.apiKey || !cfg.appId) {
    return null;
  }

  const [appMod, authMod, fsMod] = await Promise.all([
    import("https://www.gstatic.com/firebasejs/11.6.0/firebase-app.js"),
    import("https://www.gstatic.com/firebasejs/11.6.0/firebase-auth.js"),
    import("https://www.gstatic.com/firebasejs/11.6.0/firebase-firestore.js"),
  ]);

  const firebaseConfig = {
    apiKey: cfg.apiKey,
    authDomain: cfg.authDomain || `${cfg.projectId}.firebaseapp.com`,
    projectId: cfg.projectId,
    storageBucket: cfg.storageBucket,
    messagingSenderId: cfg.messagingSenderId,
    appId: cfg.appId,
  };

  const app = appMod.getApps().length ? appMod.getApps()[0] : appMod.initializeApp(firebaseConfig);
  const auth = authMod.getAuth(app);
  const db = cfg.databaseId
    ? fsMod.getFirestore(app, cfg.databaseId)
    : fsMod.getFirestore(app);

  state.firebaseRuntime = { app, auth, db, cfg, appMod, authMod, fsMod };

  authMod.onAuthStateChanged(auth, async (user) => {
    if (user) {
      state.mode = "cloud";
      state.user = user;
      updateAuthUi();
      try {
        await ensureUserProfileDoc(state.firebaseRuntime, user);
      } catch (e) {
        console.warn("User profile init warning:", e);
      }
      attachCloudListeners(state.firebaseRuntime, user);
    } else {
      clearCloudListeners();
      state.mode = "guest";
      state.user = null;
      loadGuestWorkspace();
      updateAuthUi();
      renderAll();
    }
  });

  return state.firebaseRuntime;
}

function clearCloudListeners() {
  state.unsubscribes.forEach((fn) => {
    try {
      fn();
    } catch {
      // ignore
    }
  });
  state.unsubscribes = [];
}

async function ensureUserProfileDoc(rt, user) {
  const { db, fsMod } = rt;
  const userRef = fsMod.doc(db, "users", user.uid);
  const snap = await fsMod.getDoc(userRef);
  const displayName = (user.displayName || user.email || "Pengguna SakuPintar").slice(0, 50);

  if (!snap.exists()) {
    await fsMod.setDoc(userRef, {
      userId: user.uid,
      displayName,
      createdAt: fsMod.serverTimestamp(),
      updatedAt: fsMod.serverTimestamp(),
    });
  }
}

function attachCloudListeners(rt, user) {
  clearCloudListeners();
  const { db, fsMod } = rt;
  const uid = user.uid;

  // 1. Expenses listener
  const expensesCol = fsMod.collection(db, "users", uid, "expenses");
  const unsubExp = fsMod.onSnapshot(
    fsMod.query(expensesCol, fsMod.orderBy("dateEpochDay", "desc")),
    (snapshot) => {
      state.expenses = snapshot.docs.map((d) => ({ id: d.id, ...d.data() }));
      renderAll();
    },
    (err) => {
      console.error("Expenses snapshot error:", err);
      showToast("Gagal sinkron pengeluaran: " + (err.message || "Periksa koneksi"));
    }
  );

  // 2. Budgets listener
  const budgetsCol = fsMod.collection(db, "users", uid, "budgets");
  const unsubBud = fsMod.onSnapshot(
    budgetsCol,
    (snapshot) => {
      const next = {};
      snapshot.docs.forEach((d) => {
        next[d.id] = { id: d.id, ...d.data() };
      });
      state.budgetsByMonth = next;
      renderAll();
    },
    (err) => console.error("Budgets snapshot error:", err)
  );

  // 3. Savings Goals listener
  const goalsCol = fsMod.collection(db, "users", uid, "savingsGoals");
  const unsubGoals = fsMod.onSnapshot(
    goalsCol,
    (snapshot) => {
      state.goals = snapshot.docs.map((d) => ({ id: d.id, ...d.data() }));
      renderAll();
    },
    (err) => console.error("Goals snapshot error:", err)
  );

  // 4. Wallet Balances listener
  const walletsCol = fsMod.collection(db, "users", uid, "walletBalances");
  const unsubWallets = fsMod.onSnapshot(
    walletsCol,
    (snapshot) => {
      const map = { QRIS: 0, CASH: 0, TRANSFER: 0, CARD: 0 };
      snapshot.docs.forEach((d) => {
        if (d.id in map) {
          map[d.id] = Number(d.data()?.balance || 0);
        }
      });
      state.wallets = map;
      renderAll();
    },
    (err) => console.error("Wallets snapshot error:", err)
  );

  state.unsubscribes.push(unsubExp, unsubBud, unsubGoals, unsubWallets);
}

async function signInWithGoogleCloud() {
  const rt = await ensureFirebaseRuntime();
  if (!rt) {
    if ($("currentHostDomain")) $("currentHostDomain").textContent = window.location.host || "localhost";
    openModal("firebaseSetupModal");
    showToast("Lengkapi Web API Key & App ID Firebase terlebih dahulu.");
    return;
  }

  const { auth, authMod } = rt;
  const provider = new authMod.GoogleAuthProvider();
  provider.setCustomParameters({ prompt: "select_account" });

  try {
    await authMod.signInWithPopup(auth, provider);
    localStorage.setItem(STORAGE_KEYS.AUTH_PREF, "cloud");
    showToast("Berhasil masuk dengan Google. Sinkron APK aktif!");
  } catch (err) {
    if (err?.code === "auth/popup-blocked") {
      await authMod.signInWithRedirect(auth, provider);
      return;
    }
    if (err?.code === "auth/unauthorized-domain") {
      if ($("currentHostDomain")) $("currentHostDomain").textContent = window.location.host || "localhost";
      openModal("firebaseSetupModal");
      showToast(`Tambahkan domain "${window.location.host}" ke Authorized Domains Firebase Console.`);
      return;
    }
    showToast("Login Google dibatalkan atau gagal: " + (err?.message || ""));
  }
}

async function switchToGuestMode() {
  clearCloudListeners();
  if (state.firebaseRuntime?.auth) {
    try {
      await state.firebaseRuntime.authMod.signOut(state.firebaseRuntime.auth);
    } catch {
      // ignore
    }
  }
  state.mode = "guest";
  state.user = null;
  localStorage.setItem(STORAGE_KEYS.AUTH_PREF, "guest");
  loadGuestWorkspace();
  updateAuthUi();
  renderAll();
  showToast("Berpindah ke Mode Guest (tersimpan lokal di browser).");
}

function updateAuthUi() {
  const avatarEl = $("userAvatar");
  const nameEl = $("userDisplayName");
  const badgeEl = $("syncStatusBadge");
  const subEl = $("userSubStatus");
  const signOutBtn = $("signOutBtn");
  const googleBtn = $("googleSignInBtn");
  const guestBtn = $("guestModeBtn");

  if (state.mode === "cloud" && state.user) {
    const displayName = state.user.displayName || state.user.email || "Akun Google";
    if (state.user.photoURL) {
      avatarEl.innerHTML = `<img src="${escHtml(state.user.photoURL)}" alt="Avatar" referrerpolicy="no-referrer" />`;
    } else {
      avatarEl.textContent = displayName.charAt(0).toUpperCase();
    }
    nameEl.textContent = displayName;
    badgeEl.textContent = state.lang === "id" ? "☁️ Sinkron APK Aktif" : "☁️ APK Sync Active";
    badgeEl.className = "badge-chip emerald";
    subEl.textContent = `${state.user.email || ""} — Terhubung ke Cloud Firestore yang sama dengan APK Android.`;
    signOutBtn.style.display = "inline-flex";
    googleBtn.style.display = "none";
    guestBtn.style.display = "inline-flex";
  } else {
    avatarEl.textContent = "G";
    nameEl.textContent = state.lang === "id" ? "Mode Guest (Browser Lokal)" : "Guest Mode (Local Browser)";
    badgeEl.textContent = state.lang === "id" ? "💾 Lokal Browser" : "💾 Local Browser";
    badgeEl.className = "badge-chip";
    subEl.textContent =
      state.lang === "id"
        ? "Data tersimpan di browser ini. Masuk dengan Google untuk sinkron real-time dengan APK Android."
        : "Saved in this browser. Sign in with Google for two-way live sync with Android APK.";
    signOutBtn.style.display = "none";
    googleBtn.style.display = "inline-flex";
    guestBtn.style.display = "none";
  }
}

// ---------------- CRUD Operations (Web <-> APK Parity) ----------------

async function saveExpenseFromForm(e) {
  e.preventDefault();
  const editId = $("expenseEditId").value.trim();
  const title = $("expenseTitleInput").value.trim().slice(0, 80);
  const amount = Math.round(Number($("expenseAmountInput").value));
  const dateStr = $("expenseDateInput").value || todayDateStr();
  const category = $("expenseCategoryInput").value;
  const paymentMethod = $("expenseMethodInput").value;
  const note = $("expenseNoteInput").value.trim().slice(0, 200);
  const receipt = state.pendingReceiptBase64 || "";

  if (!title || !Number.isFinite(amount) || amount <= 0 || amount > 10000000000) {
    showToast("Mohon isi judul dan nominal yang valid.");
    return;
  }

  const dateEpochDay = dateInputToEpochDay(dateStr);
  const monthKey = dateStr.slice(0, 7);

  if (state.mode === "cloud" && state.user && state.firebaseRuntime) {
    const { db, fsMod } = state.firebaseRuntime;
    const uid = state.user.uid;
    const docId = editId || safeId("exp");
    const ref = fsMod.doc(db, "users", uid, "expenses", docId);

    try {
      if (editId) {
        const payload = {
          title,
          amount,
          category,
          paymentMethod,
          dateEpochDay,
          monthKey,
          note: note ? note : fsMod.deleteField(),
          receiptPhotoBase64: receipt ? receipt : fsMod.deleteField(),
          updatedAt: fsMod.serverTimestamp(),
        };
        await fsMod.updateDoc(ref, payload);
      } else {
        const payload = {
          userId: uid,
          title,
          amount,
          category,
          paymentMethod,
          dateEpochDay,
          monthKey,
          createdAt: fsMod.serverTimestamp(),
          updatedAt: fsMod.serverTimestamp(),
        };
        if (note) payload.note = note;
        if (receipt) payload.receiptPhotoBase64 = receipt;
        await fsMod.setDoc(ref, payload);
      }
      closeModal("expenseModal");
      showToast("Transaksi disimpan & disinkronkan ke APK.");
    } catch (err) {
      console.error("Save expense error:", err);
      showToast("Gagal menyimpan ke cloud: " + (err.message || ""));
    }
    return;
  }

  // Guest mode
  const item = {
    id: editId || safeId("exp"),
    userId: "guest",
    title,
    amount,
    category,
    paymentMethod,
    dateEpochDay,
    monthKey,
    note: note || undefined,
    receiptPhotoBase64: receipt || undefined,
  };
  if (editId) {
    state.expenses = state.expenses.map((x) => (x.id === editId ? item : x));
  } else {
    state.expenses = [item, ...state.expenses];
  }
  persistGuestWorkspace();
  closeModal("expenseModal");
  renderAll();
  showToast("Transaksi disimpan di browser.");
}

async function deleteExpenseById(id) {
  if (!window.confirm(state.lang === "id" ? "Hapus transaksi ini?" : "Delete this expense?")) return;

  if (state.mode === "cloud" && state.user && state.firebaseRuntime) {
    const { db, fsMod } = state.firebaseRuntime;
    try {
      await fsMod.deleteDoc(fsMod.doc(db, "users", state.user.uid, "expenses", id));
      showToast("Transaksi dihapus dari cloud & APK.");
    } catch (err) {
      showToast("Gagal menghapus: " + (err.message || ""));
    }
    return;
  }

  state.expenses = state.expenses.filter((x) => x.id !== id);
  persistGuestWorkspace();
  renderAll();
  showToast("Transaksi dihapus.");
}

async function saveBudgetFromForm(e) {
  e.preventDefault();
  const income = Math.max(0, Math.round(Number($("budgetIncomeInput").value) || 0));
  const strategy = $("budgetStrategySelect").value;
  const needsPercent = Math.round(Number($("needsPercentInput").value) || 0);
  const wantsPercent = Math.round(Number($("wantsPercentInput").value) || 0);
  const savingsPercent = Math.round(Number($("savingsPercentInput").value) || 0);

  if (needsPercent + wantsPercent + savingsPercent !== 100) {
    showToast("Total Kebutuhan + Keinginan + Tabungan harus tepat 100%.");
    return;
  }

  const savingsTarget = Math.max(0, Math.round((income * savingsPercent) / 100));
  const categoryBudgets = computeCategoryBudgets(
    income,
    needsPercent,
    wantsPercent,
    strategy,
    state.selectedMonthKey
  );

  if (state.mode === "cloud" && state.user && state.firebaseRuntime) {
    const { db, fsMod } = state.firebaseRuntime;
    const ref = fsMod.doc(db, "users", state.user.uid, "budgets", state.selectedMonthKey);
    try {
      const existing = await fsMod.getDoc(ref);
      if (existing.exists()) {
        await fsMod.updateDoc(ref, {
          monthlyIncome: income,
          strategy,
          savingsTarget,
          needsPercent,
          wantsPercent,
          savingsPercent,
          categoryBudgets,
          updatedAt: fsMod.serverTimestamp(),
        });
      } else {
        await fsMod.setDoc(ref, {
          userId: state.user.uid,
          monthKey: state.selectedMonthKey,
          monthlyIncome: income,
          strategy,
          savingsTarget,
          needsPercent,
          wantsPercent,
          savingsPercent,
          categoryBudgets,
          createdAt: fsMod.serverTimestamp(),
          updatedAt: fsMod.serverTimestamp(),
        });
      }
      showToast("Anggaran tersimpan & sinkron dengan APK.");
    } catch (err) {
      showToast("Gagal simpan anggaran: " + (err.message || ""));
    }
    return;
  }

  state.budgetsByMonth[state.selectedMonthKey] = {
    monthKey: state.selectedMonthKey,
    monthlyIncome: income,
    strategy,
    savingsTarget,
    needsPercent,
    wantsPercent,
    savingsPercent,
    categoryBudgets,
  };
  persistGuestWorkspace();
  renderAll();
  showToast("Anggaran bulan ini disimpan.");
}

async function saveWalletBalance(methodKey, rawAmount) {
  const balance = Math.max(0, Math.min(100000000000, Math.round(Number(rawAmount) || 0)));

  if (state.mode === "cloud" && state.user && state.firebaseRuntime) {
    const { db, fsMod } = state.firebaseRuntime;
    const ref = fsMod.doc(db, "users", state.user.uid, "walletBalances", methodKey);
    try {
      const existing = await fsMod.getDoc(ref);
      if (existing.exists()) {
        await fsMod.updateDoc(ref, {
          balance,
          updatedAt: fsMod.serverTimestamp(),
        });
      } else {
        await fsMod.setDoc(ref, {
          userId: state.user.uid,
          paymentMethod: methodKey,
          balance,
          createdAt: fsMod.serverTimestamp(),
          updatedAt: fsMod.serverTimestamp(),
        });
      }
      showToast(`Saldo ${methodKey} tersinkron ke APK.`);
    } catch (err) {
      showToast("Gagal simpan saldo: " + (err.message || ""));
    }
    return;
  }

  state.wallets[methodKey] = balance;
  persistGuestWorkspace();
  renderAll();
  showToast(`Saldo ${methodKey} diperbarui.`);
}

async function saveGoalFromForm(e) {
  e.preventDefault();
  const title = $("goalTitleInput").value.trim().slice(0, 60);
  const targetAmount = Math.round(Number($("goalTargetInput").value) || 0);
  const savedAmount = Math.max(0, Math.round(Number($("goalInitialInput").value) || 0));
  const category = $("goalCategoryInput").value;
  const note = $("goalNoteInput").value.trim().slice(0, 120);

  if (!title || targetAmount <= 0) {
    showToast("Nama target dan nominal target wajib valid.");
    return;
  }

  if (state.mode === "cloud" && state.user && state.firebaseRuntime) {
    const { db, fsMod } = state.firebaseRuntime;
    const goalId = safeId("goal");
    const payload = {
      userId: state.user.uid,
      title,
      targetAmount,
      savedAmount,
      category,
      createdAt: fsMod.serverTimestamp(),
      updatedAt: fsMod.serverTimestamp(),
    };
    if (note) payload.note = note;

    try {
      await fsMod.setDoc(fsMod.doc(db, "users", state.user.uid, "savingsGoals", goalId), payload);
      closeModal("goalModal");
      $("goalForm").reset();
      showToast("Target celengan dibuat & tersinkron ke APK.");
    } catch (err) {
      showToast("Gagal membuat target: " + (err.message || ""));
    }
    return;
  }

  state.goals.push({
    id: safeId("goal"),
    title,
    targetAmount,
    savedAmount,
    category,
    note: note || undefined,
  });
  persistGuestWorkspace();
  closeModal("goalModal");
  $("goalForm").reset();
  renderAll();
  showToast("Target celengan ditambahkan.");
}

async function adjustGoalAmount(goalId, delta) {
  const goal = state.goals.find((g) => g.id === goalId);
  if (!goal) return;
  const nextAmount = Math.max(0, Math.min(100000000000, Math.round(Number(goal.savedAmount || 0) + delta)));

  if (state.mode === "cloud" && state.user && state.firebaseRuntime) {
    const { db, fsMod } = state.firebaseRuntime;
    const ref = fsMod.doc(db, "users", state.user.uid, "savingsGoals", goalId);
    try {
      await fsMod.updateDoc(ref, {
        savedAmount: nextAmount,
        updatedAt: fsMod.serverTimestamp(),
      });
      showToast("Saldo celengan diperbarui.");
    } catch (err) {
      showToast("Gagal ubah celengan: " + (err.message || ""));
    }
    return;
  }

  goal.savedAmount = nextAmount;
  persistGuestWorkspace();
  renderAll();
  showToast("Saldo celengan diperbarui.");
}

async function deleteGoalById(goalId) {
  if (!window.confirm(state.lang === "id" ? "Hapus target celengan ini?" : "Delete this goal?")) return;
  if (state.mode === "cloud" && state.user && state.firebaseRuntime) {
    const { db, fsMod } = state.firebaseRuntime;
    try {
      await fsMod.deleteDoc(fsMod.doc(db, "users", state.user.uid, "savingsGoals", goalId));
      showToast("Target celengan dihapus.");
    } catch (err) {
      showToast("Gagal hapus target: " + (err.message || ""));
    }
    return;
  }
  state.goals = state.goals.filter((g) => g.id !== goalId);
  persistGuestWorkspace();
  renderAll();
  showToast("Target celengan dihapus.");
}

// ---------------- Export CSV & Print PDF ----------------

function exportMonthExpensesCsv() {
  const rows = state.expenses.filter((e) => e.monthKey === state.selectedMonthKey);
  if (!rows.length) {
    showToast("Belum ada transaksi pada bulan ini untuk diekspor.");
    return;
  }

  const escapeCsv = (v) => `"${String(v ?? "").replace(/"/g, '""')}"`;
  const header = ["Tanggal", "Judul", "Kategori", "Metode", "Nominal (Rp)", "Catatan", "Foto Struk"];
  const lines = [header.map(escapeCsv).join(",")];

  rows.forEach((e) => {
    lines.push(
      [
        epochDayToDateStr(e.dateEpochDay),
        e.title,
        categoryName(e.category),
        methodName(e.paymentMethod),
        e.amount,
        e.note || "",
        e.receiptPhotoBase64 ? "Terlampir di App/Web" : "-",
      ]
        .map(escapeCsv)
        .join(",")
    );
  });

  const blob = new Blob(["\uFEFF" + lines.join("\r\n")], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `SakuPintar-${state.selectedMonthKey}.csv`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
  showToast("File CSV / Excel berhasil diunduh.");
}

// ---------------- Rendering ----------------

function renderExpenseCardHtml(exp, { showActions = true } = {}) {
  const meta = CATEGORY_META[exp.category] || CATEGORY_META.OTHER;
  const dateStr = epochDayToDateStr(exp.dateEpochDay);
  const receiptHtml = exp.receiptPhotoBase64
    ? `<img src="data:image/jpeg;base64,${escHtml(exp.receiptPhotoBase64)}" alt="Struk" class="receipt-thumb" data-receipt-b64="${escHtml(exp.receiptPhotoBase64)}" />`
    : "";

  const actionsHtml = showActions
    ? `
      <button type="button" class="btn btn-outline btn-sm no-print" data-edit-exp="${escHtml(exp.id)}" title="Edit">✏️</button>
      <button type="button" class="btn btn-danger btn-sm no-print" data-del-exp="${escHtml(exp.id)}" title="Hapus">🗑️</button>
    `
    : "";

  return `
    <div class="expense-item">
      <div class="expense-left">
        <div class="expense-cat-dot" style="background:${meta.color}">${meta.short}</div>
        <div class="expense-info">
          <div class="expense-title">${escHtml(exp.title)}</div>
          <div class="expense-meta">${escHtml(dateStr)} · ${escHtml(categoryName(exp.category))} · ${escHtml(methodName(exp.paymentMethod))}${exp.note ? ` · ${escHtml(exp.note)}` : ""}</div>
        </div>
      </div>
      <div class="expense-right">
        ${receiptHtml}
        <span class="expense-amount mono">${formatRupiah(exp.amount)}</span>
        ${actionsHtml}
      </div>
    </div>
  `;
}

function renderDashboard() {
  const monthExpenses = state.expenses.filter((e) => e.monthKey === state.selectedMonthKey);
  const totalSpent = monthExpenses.reduce((acc, e) => acc + Number(e.amount || 0), 0);

  const budget = getBudgetForMonth(state.selectedMonthKey);
  const spendableBudget = Math.max(0, Number(budget.monthlyIncome || 0) - Number(budget.savingsTarget || 0));
  const remaining = spendableBudget - totalSpent;
  const pct = spendableBudget > 0 ? Math.min(999, Math.round((totalSpent / spendableBudget) * 100)) : 0;

  const [y, m] = state.selectedMonthKey.split("-").map(Number);
  const daysInMonth = new Date(y, m, 0).getDate();
  const today = new Date();
  const isCurrent = today.getFullYear() === y && today.getMonth() + 1 === m;
  const remainingDays = isCurrent ? Math.max(1, daysInMonth - today.getDate() + 1) : daysInMonth;
  const dailySafe = Math.max(0, Math.round(remaining / remainingDays));

  const totalWallets = Object.values(state.wallets).reduce((a, b) => a + Number(b || 0), 0);
  const totalSavings = state.goals.reduce((a, g) => a + Number(g.savedAmount || 0), 0);

  $("dashRemainingBudget").textContent = formatRupiah(remaining);
  $("dashBudgetUsageSub").textContent = `${pct}% terpakai dari batas belanja ${formatRupiah(spendableBudget)}`;
  $("dashTotalSpent").textContent = formatRupiah(totalSpent);
  $("dashTxCountSub").textContent = `${monthExpenses.length} transaksi bulan ini`;
  $("dashDailySafe").textContent = formatRupiah(dailySafe);
  $("dashDailySafeSub").textContent = `Untuk ${remainingDays} hari tersisa`;
  $("dashTotalWallet").textContent = formatRupiah(totalWallets);
  $("dashSavingsProgressSub").textContent = `Total celengan: ${formatRupiah(totalSavings)}`;

  // Category distribution
  const spentByCat = {};
  monthExpenses.forEach((e) => {
    spentByCat[e.category] = (spentByCat[e.category] || 0) + Number(e.amount || 0);
  });

  const barsHtml = Object.entries(CATEGORY_META)
    .map(([key, meta]) => {
      const spent = spentByCat[key] || 0;
      const limit = Number(budget.categoryBudgets?.[key] || 0);
      const ratio = limit > 0 ? Math.min(100, Math.round((spent / limit) * 100)) : spent > 0 ? 100 : 0;
      const fillColor = limit > 0 && spent > limit ? "#dc2626" : meta.color;
      return `
        <div class="cat-row">
          <div class="cat-row-top">
            <span>${escHtml(categoryName(key))}</span>
            <span class="mono">${formatRupiah(spent)} / ${formatRupiah(limit)}</span>
          </div>
          <div class="progress-track">
            <div class="progress-fill" style="width:${ratio}%; background:${fillColor}"></div>
          </div>
        </div>
      `;
    })
    .join("");
  $("dashCategoryBars").innerHTML = barsHtml;

  // Recent 5 transactions
  const recent = monthExpenses.slice(0, 5);
  $("dashRecentList").innerHTML = recent.length
    ? recent.map((e) => renderExpenseCardHtml(e, { showActions: false })).join("")
    : `<div class="empty-state">Belum ada transaksi di ${escHtml(formatMonthLabel(state.selectedMonthKey))}.</div>`;
}

function renderExpensesTab() {
  const q = ($("expenseSearchInput").value || "").trim().toLowerCase();
  const cat = $("expenseCategoryFilter").value;
  const method = $("expenseMethodFilter").value;

  const filtered = state.expenses.filter((e) => {
    if (e.monthKey !== state.selectedMonthKey) return false;
    if (cat !== "ALL" && e.category !== cat) return false;
    if (method !== "ALL" && e.paymentMethod !== method) return false;
    if (q) {
      const hay = `${e.title || ""} ${e.note || ""}`.toLowerCase();
      if (!hay.includes(q)) return false;
    }
    return true;
  });

  $("expensesListContainer").innerHTML = filtered.length
    ? filtered.map((e) => renderExpenseCardHtml(e, { showActions: true })).join("")
    : `<div class="empty-state">Tidak ada pengeluaran yang cocok dengan filter bulan ini.</div>`;
}

function renderBudgetTab() {
  const budget = getBudgetForMonth(state.selectedMonthKey);
  $("budgetIncomeInput").value = budget.monthlyIncome;
  $("budgetStrategySelect").value = budget.strategy;
  $("needsPercentInput").value = budget.needsPercent;
  $("wantsPercentInput").value = budget.wantsPercent;
  $("savingsPercentInput").value = budget.savingsPercent;
  updateBudgetSummaryHint();

  const monthExpenses = state.expenses.filter((e) => e.monthKey === state.selectedMonthKey);
  const spentByCat = {};
  monthExpenses.forEach((e) => {
    spentByCat[e.category] = (spentByCat[e.category] || 0) + Number(e.amount || 0);
  });

  $("budgetCategoryProgressList").innerHTML = Object.entries(CATEGORY_META)
    .map(([key, meta]) => {
      const spent = spentByCat[key] || 0;
      const limit = Number(budget.categoryBudgets?.[key] || 0);
      const ratio = limit > 0 ? Math.min(100, Math.round((spent / limit) * 100)) : 0;
      const over = limit > 0 && spent > limit;
      return `
        <div class="cat-row">
          <div class="cat-row-top">
            <span>${escHtml(categoryName(key))}</span>
            <span class="mono" style="color:${over ? "var(--danger)" : "inherit"}">${formatRupiah(spent)} / ${formatRupiah(limit)}</span>
          </div>
          <div class="progress-track">
            <div class="progress-fill" style="width:${ratio}%; background:${over ? "#dc2626" : meta.color}"></div>
          </div>
        </div>
      `;
    })
    .join("");
}

function updateBudgetSummaryHint() {
  const income = Math.max(0, Number($("budgetIncomeInput").value) || 0);
  const n = Number($("needsPercentInput").value) || 0;
  const w = Number($("wantsPercentInput").value) || 0;
  const s = Number($("savingsPercentInput").value) || 0;
  const sum = n + w + s;
  const savingsRp = Math.round((income * s) / 100);
  const spendRp = Math.round((income * (n + w)) / 100);

  const el = $("budgetAllocationSummary");
  if (sum !== 100) {
    el.innerHTML = `<span style="color:var(--danger);font-weight:700">Total persentase saat ini ${sum}% — harus tepat 100%.</span>`;
  } else {
    el.textContent = `Batas belanja: ${formatRupiah(spendRp)} · Target tabungan: ${formatRupiah(savingsRp)}`;
  }
}

function renderSavingsAndWalletsTab() {
  $("walletsGrid").innerHTML = PAYMENT_METHODS.map(
    (m) => `
      <div class="wallet-card">
        <div style="display:flex;align-items:center;justify-content:space-between">
          <strong>${m.icon} ${escHtml(methodName(m.key))}</strong>
          <span class="mono" style="font-size:0.75rem;color:var(--text-muted)">${m.key}</span>
        </div>
        <input
          type="number"
          class="input mono"
          min="0"
          max="100000000000"
          step="1000"
          value="${Number(state.wallets[m.key] || 0)}"
          data-wallet-input="${m.key}"
        />
        <button type="button" class="btn btn-tonal btn-sm no-print" data-save-wallet="${m.key}">
          💾 Simpan Saldo
        </button>
      </div>
    `
  ).join("");

  if (!state.goals.length) {
    $("goalsGrid").innerHTML = `<div class="empty-state" style="grid-column:1 / -1">Belum ada celengan target. Buat target baru agar tersinkron ke Android APK!</div>`;
    return;
  }

  $("goalsGrid").innerHTML = state.goals
    .map((g) => {
      const catMeta = GOAL_CATEGORIES[g.category] || GOAL_CATEGORIES.OTHER;
      const saved = Number(g.savedAmount || 0);
      const target = Math.max(1, Number(g.targetAmount || 1));
      const pct = Math.min(100, Math.round((saved / target) * 100));
      return `
        <article class="goal-card">
          <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:10px">
            <div>
              <div style="font-size:0.78rem;color:var(--text-muted);font-weight:700">${catMeta.icon} ${escHtml(state.lang === "id" ? catMeta.labelId : catMeta.labelEn)}</div>
              <strong style="font-size:1.05rem">${escHtml(g.title)}</strong>
              ${g.note ? `<div style="font-size:0.8rem;color:var(--text-muted);margin-top:2px">${escHtml(g.note)}</div>` : ""}
            </div>
            <button type="button" class="btn btn-danger btn-sm no-print" data-del-goal="${escHtml(g.id)}" title="Hapus">🗑️</button>
          </div>
          <div style="display:flex;justify-content:space-between;font-size:0.85rem;font-weight:700">
            <span class="mono">${formatRupiah(saved)}</span>
            <span class="mono" style="color:var(--text-muted)">/ ${formatRupiah(target)} (${pct}%)</span>
          </div>
          <div class="progress-track">
            <div class="progress-fill" style="width:${pct}%;background:var(--emerald)"></div>
          </div>
          <div style="display:flex;gap:8px;flex-wrap:wrap" class="no-print">
            <button type="button" class="btn btn-emerald btn-sm" data-goal-delta="${escHtml(g.id)}:50000">+50rb</button>
            <button type="button" class="btn btn-emerald btn-sm" data-goal-delta="${escHtml(g.id)}:100000">+100rb</button>
            <button type="button" class="btn btn-outline btn-sm" data-goal-delta="${escHtml(g.id)}:-50000">-50rb</button>
          </div>
        </article>
      `;
    })
    .join("");
}

function renderAll() {
  $("currentMonthLabel").textContent = formatMonthLabel(state.selectedMonthKey);
  renderDashboard();
  renderExpensesTab();
  renderBudgetTab();
  renderSavingsAndWalletsTab();
}

// ---------------- Event Wiring ----------------

function switchTab(tab) {
  state.activeTab = tab;
  document.querySelectorAll(".nav-pill").forEach((b) => {
    b.classList.toggle("active", b.getAttribute("data-tab") === tab);
  });
  document.querySelectorAll(".tab-panel").forEach((p) => {
    p.classList.toggle("active", p.id === `panel-${tab}`);
  });
}

function openNewExpenseModal() {
  $("expenseModalTitle").textContent = state.lang === "id" ? "Tambah Pengeluaran" : "Add Expense";
  $("expenseForm").reset();
  $("expenseEditId").value = "";
  $("expenseDateInput").value = todayDateStr();
  state.pendingReceiptBase64 = "";
  $("expenseReceiptPreviewWrap").style.display = "none";
  openModal("expenseModal");
}

function openEditExpenseModal(id) {
  const exp = state.expenses.find((e) => e.id === id);
  if (!exp) return;
  $("expenseModalTitle").textContent = state.lang === "id" ? "Edit Pengeluaran" : "Edit Expense";
  $("expenseEditId").value = exp.id;
  $("expenseTitleInput").value = exp.title || "";
  $("expenseAmountInput").value = exp.amount || "";
  $("expenseDateInput").value = epochDayToDateStr(exp.dateEpochDay);
  $("expenseCategoryInput").value = exp.category || "OTHER";
  $("expenseMethodInput").value = exp.paymentMethod || "QRIS";
  $("expenseNoteInput").value = exp.note || "";
  state.pendingReceiptBase64 = exp.receiptPhotoBase64 || "";
  if (state.pendingReceiptBase64) {
    $("expenseReceiptPreviewImg").src = `data:image/jpeg;base64,${state.pendingReceiptBase64}`;
    $("expenseReceiptPreviewWrap").style.display = "flex";
  } else {
    $("expenseReceiptPreviewWrap").style.display = "none";
  }
  openModal("expenseModal");
}

function bindEvents() {
  // Theme & Language
  $("themeLightBtn")?.addEventListener("click", () => applyTheme("light"));
  $("themeDarkBtn")?.addEventListener("click", () => applyTheme("dark"));
  $("langIdBtn")?.addEventListener("click", () => applyLanguage("id"));
  $("langEnBtn")?.addEventListener("click", () => applyLanguage("en"));

  // Hero CTAs
  $("heroStartWebBtn")?.addEventListener("click", () => {
    $("workspaceAnchor")?.scrollIntoView({ behavior: "smooth" });
  });
  $("heroGuideBtn")?.addEventListener("click", () => openModal("apkGuideModal"));

  // Auth
  $("googleSignInBtn")?.addEventListener("click", signInWithGoogleCloud);
  $("guestModeBtn")?.addEventListener("click", switchToGuestMode);
  $("signOutBtn")?.addEventListener("click", switchToGuestMode);
  $("openFirebaseSetupBtn")?.addEventListener("click", () => {
    if ($("currentHostDomain")) $("currentHostDomain").textContent = window.location.host || "localhost";
    try {
      const saved = JSON.parse(localStorage.getItem(STORAGE_KEYS.FIREBASE_CFG) || "null");
      if (saved) {
        $("fbApiKeyInput").value = saved.apiKey || "";
        $("fbAppIdInput").value = saved.appId || "";
      }
    } catch {
      // ignore
    }
    openModal("firebaseSetupModal");
  });

  // Tabs & Month Switcher
  document.querySelectorAll(".nav-pill").forEach((btn) => {
    btn.addEventListener("click", () => switchTab(btn.getAttribute("data-tab")));
  });
  $("prevMonthBtn")?.addEventListener("click", () => {
    state.selectedMonthKey = shiftMonthKey(state.selectedMonthKey, -1);
    renderAll();
  });
  $("nextMonthBtn")?.addEventListener("click", () => {
    state.selectedMonthKey = shiftMonthKey(state.selectedMonthKey, 1);
    renderAll();
  });

  // Dashboard & Expenses Actions
  $("quickAddFromDashBtn")?.addEventListener("click", openNewExpenseModal);
  $("addExpenseBtn")?.addEventListener("click", openNewExpenseModal);
  $("dashGoExpensesBtn")?.addEventListener("click", () => switchTab("expenses"));
  $("exportCsvBtn")?.addEventListener("click", exportMonthExpensesCsv);
  $("printPdfBtn")?.addEventListener("click", () => window.print());

  $("expenseSearchInput")?.addEventListener("input", renderExpensesTab);
  $("expenseCategoryFilter")?.addEventListener("change", renderExpensesTab);
  $("expenseMethodFilter")?.addEventListener("change", renderExpensesTab);

  // Delegated clicks for Expense Edit / Delete / Receipt Zoom
  document.addEventListener("click", (evt) => {
    const closeTarget = evt.target.closest("[data-close-modal]");
    if (closeTarget) {
      closeModal(closeTarget.getAttribute("data-close-modal"));
      return;
    }

    const editBtn = evt.target.closest("[data-edit-exp]");
    if (editBtn) {
      openEditExpenseModal(editBtn.getAttribute("data-edit-exp"));
      return;
    }

    const delBtn = evt.target.closest("[data-del-exp]");
    if (delBtn) {
      deleteExpenseById(delBtn.getAttribute("data-del-exp"));
      return;
    }

    const receiptThumb = evt.target.closest("[data-receipt-b64]");
    if (receiptThumb) {
      $("receiptLightboxImg").src = `data:image/jpeg;base64,${receiptThumb.getAttribute("data-receipt-b64")}`;
      openModal("receiptModal");
      return;
    }

    const saveWalletBtn = evt.target.closest("[data-save-wallet]");
    if (saveWalletBtn) {
      const methodKey = saveWalletBtn.getAttribute("data-save-wallet");
      const input = document.querySelector(`[data-wallet-input="${methodKey}"]`);
      if (input) saveWalletBalance(methodKey, input.value);
      return;
    }

    const goalDeltaBtn = evt.target.closest("[data-goal-delta]");
    if (goalDeltaBtn) {
      const [goalId, delta] = goalDeltaBtn.getAttribute("data-goal-delta").split(":");
      adjustGoalAmount(goalId, Number(delta));
      return;
    }

    const delGoalBtn = evt.target.closest("[data-del-goal]");
    if (delGoalBtn) {
      deleteGoalById(delGoalBtn.getAttribute("data-del-goal"));
    }
  });

  // Expense Form + Receipt Upload
  $("expenseForm")?.addEventListener("submit", saveExpenseFromForm);
  $("expenseReceiptInput")?.addEventListener("change", async (evt) => {
    const file = evt.target.files?.[0];
    if (!file) return;
    try {
      const compressed = await compressReceiptFile(file);
      state.pendingReceiptBase64 = compressed;
      $("expenseReceiptPreviewImg").src = `data:image/jpeg;base64,${compressed}`;
      $("expenseReceiptPreviewWrap").style.display = "flex";
      showToast("Foto struk dikompres & siap disinkronkan.");
    } catch (err) {
      showToast(err.message || "Gagal memproses gambar.");
    }
  });
  $("removeReceiptBtn")?.addEventListener("click", () => {
    state.pendingReceiptBase64 = "";
    $("expenseReceiptInput").value = "";
    $("expenseReceiptPreviewWrap").style.display = "none";
  });

  // Budget Form & Strategy Presets
  $("budgetStrategySelect")?.addEventListener("change", () => {
    const val = $("budgetStrategySelect").value;
    if (val === "BALANCED_50_30_20") {
      $("needsPercentInput").value = 50;
      $("wantsPercentInput").value = 30;
      $("savingsPercentInput").value = 20;
    } else if (val === "FRUGAL_60_10_30") {
      $("needsPercentInput").value = 60;
      $("wantsPercentInput").value = 10;
      $("savingsPercentInput").value = 30;
    }
    updateBudgetSummaryHint();
  });
  ["budgetIncomeInput", "needsPercentInput", "wantsPercentInput", "savingsPercentInput"].forEach((id) => {
    $(id)?.addEventListener("input", updateBudgetSummaryHint);
  });
  $("budgetForm")?.addEventListener("submit", saveBudgetFromForm);

  // Goals Modal
  $("addGoalBtn")?.addEventListener("click", () => {
    $("goalForm").reset();
    $("goalInitialInput").value = 0;
    openModal("goalModal");
  });
  $("goalForm")?.addEventListener("submit", saveGoalFromForm);

  // Firebase Web Config Modal
  $("firebaseConfigForm")?.addEventListener("submit", async (evt) => {
    evt.preventDefault();
    const apiKey = $("fbApiKeyInput").value.trim();
    const appId = $("fbAppIdInput").value.trim();
    if (!apiKey || !appId) {
      showToast("Web API Key dan App ID wajib diisi.");
      return;
    }
    localStorage.setItem(STORAGE_KEYS.FIREBASE_CFG, JSON.stringify({ apiKey, appId }));
    state.firebaseRuntime = null;
    closeModal("firebaseSetupModal");
    showToast("Konfigurasi Firebase disimpan! Sekarang silakan klik Masuk dengan Google.");
  });

  $("clearFirebaseCfgBtn")?.addEventListener("click", () => {
    localStorage.removeItem(STORAGE_KEYS.FIREBASE_CFG);
    $("fbApiKeyInput").value = "";
    $("fbAppIdInput").value = "";
    state.firebaseRuntime = null;
    showToast("Konfigurasi Firebase lokal direset.");
  });

  // Close modal when clicking backdrop
  document.querySelectorAll(".modal-backdrop").forEach((bd) => {
    bd.addEventListener("click", (evt) => {
      if (evt.target === bd) closeModal(bd.id);
    });
  });
}

// ---------------- Bootstrap ----------------

async function bootstrap() {
  applyTheme(state.theme);
  applyLanguage(state.lang);
  bindEvents();
  loadGuestWorkspace();
  updateAuthUi();
  renderAll();
  loadLatestApkRelease();

  try {
    await ensureFirebaseRuntime();
  } catch (err) {
    console.warn("Firebase runtime deferred:", err);
  }
}

bootstrap();
