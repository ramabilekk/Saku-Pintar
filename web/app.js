/* ==========================================================================
   SakuPintar v2.0 — 1:1 Android APK Logic & Real-Time Cloud Sync Engine
   Matches Firestore Schema (/firestore.rules) & APK Screens 1:1
   ========================================================================== */

import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.2/firebase-app.js";
import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  onAuthStateChanged,
  signOut,
  updateProfile
} from "https://www.gstatic.com/firebasejs/11.0.2/firebase-auth.js";
import {
  getFirestore,
  collection,
  doc,
  setDoc,
  deleteDoc,
  onSnapshot
} from "https://www.gstatic.com/firebasejs/11.0.2/firebase-firestore.js";

/* --------------------------------------------------------------------------
   1. Category & Payment Visuals (Matches CategoryVisuals.kt & ExpenseCategory.kt)
   -------------------------------------------------------------------------- */
const CATEGORIES = {
  FOOD_DINING: {
    key: "FOOD_DINING",
    labelId: "Makan & Minum",
    labelEn: "Food & Dining",
    shortId: "Makan",
    shortEn: "Food",
    icon: "🍛",
    color: "#EA580C",
    bgSoft: "rgba(234, 88, 12, 0.14)",
    isNeed: true,
    defaultWeight: 0.42
  },
  BILLS_UTILITIES: {
    key: "BILLS_UTILITIES",
    labelId: "Tagihan & Utilitas",
    labelEn: "Bills & Utilities",
    shortId: "Tagihan",
    shortEn: "Bills",
    icon: "⚡",
    color: "#2563EB",
    bgSoft: "rgba(37, 99, 235, 0.14)",
    isNeed: true,
    defaultWeight: 0.28
  },
  TRANSPORTATION: {
    key: "TRANSPORTATION",
    labelId: "Transportasi",
    labelEn: "Transportation",
    shortId: "Transport",
    shortEn: "Transport",
    icon: "🛵",
    color: "#0284C7",
    bgSoft: "rgba(2, 132, 199, 0.14)",
    isNeed: true,
    defaultWeight: 0.18
  },
  HEALTH_PERSONAL: {
    key: "HEALTH_PERSONAL",
    labelId: "Kesehatan & Diri",
    labelEn: "Health & Personal",
    shortId: "Sehat",
    shortEn: "Health",
    icon: "💊",
    color: "#0D9488",
    bgSoft: "rgba(13, 148, 136, 0.14)",
    isNeed: true,
    defaultWeight: 0.12
  },
  SHOPPING: {
    key: "SHOPPING",
    labelId: "Belanja",
    labelEn: "Shopping",
    shortId: "Belanja",
    shortEn: "Shopping",
    icon: "🛍️",
    color: "#7C3AED",
    bgSoft: "rgba(124, 58, 237, 0.14)",
    isNeed: false,
    defaultWeight: 0.45
  },
  ENTERTAINMENT: {
    key: "ENTERTAINMENT",
    labelId: "Hiburan & Langganan",
    labelEn: "Entertainment",
    shortId: "Hiburan",
    shortEn: "Fun",
    icon: "🎬",
    color: "#DB2777",
    bgSoft: "rgba(219, 39, 119, 0.14)",
    isNeed: false,
    defaultWeight: 0.35
  },
  OTHER: {
    key: "OTHER",
    labelId: "Lainnya",
    labelEn: "Others",
    shortId: "Lainnya",
    shortEn: "Others",
    icon: "📦",
    color: "#64748B",
    bgSoft: "rgba(100, 116, 139, 0.14)",
    isNeed: false,
    defaultWeight: 0.20
  }
};

const PAYMENT_METHODS = {
  QRIS: { key: "QRIS", label: "QRIS", icon: "📱", color: "#2563EB" },
  CASH: { key: "CASH", label: "Tunai", icon: "💵", color: "#059669" },
  TRANSFER: { key: "TRANSFER", label: "Transfer", icon: "🏦", color: "#7C3AED" },
  CARD: { key: "CARD", label: "Kartu", icon: "💳", color: "#D97706" }
};

const CURRENCY_SYMBOLS = {
  IDR: "Rp ",
  USD: "$",
  EUR: "€",
  SGD: "S$",
  MYR: "RM ",
  JPY: "¥",
  SAR: "SAR ",
  AUD: "A$"
};

const MONTHS_ID = [
  "Januari", "Februari", "Maret", "April", "Mei", "Juni",
  "Juli", "Agustus", "September", "Oktober", "November", "Desember"
];

const MONTHS_SHORT_ID = [
  "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
  "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
];

const MONTHS_EN = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December"
];

/* --------------------------------------------------------------------------
   2. Application State
   -------------------------------------------------------------------------- */
const now = new Date();
const currentMonthKeyDefault = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;

const state = {
  theme: localStorage.getItem("sakupintar_theme") || "light",
  lang: localStorage.getItem("sakupintar_lang") || "id",
  currency: localStorage.getItem("sakupintar_currency") || "IDR",

  authStep: "LANDING", // "LANDING" | "METHOD_SELECTION"
  authMode: "SIGN_IN", // "SIGN_IN" | "SIGN_UP"
  isAuthenticated: false,
  isGuest: false,
  user: null,
  customAvatarBase64: localStorage.getItem("sakupintar_custom_avatar") || "",

  activeTab: "dashboard", // "dashboard" | "expenses" | "savings" | "budget"
  selectedYearMonth: currentMonthKeyDefault,
  pickerYear: now.getFullYear(),

  // Filters on Harian tab
  searchQuery: "",
  selectedCategoryFilter: "ALL",
  selectedCalendarDate: null,
  isCalendarExpanded: true,

  // Data Collections (Synced with Firestore or Guest localStorage)
  expenses: [],
  budgetsByMonth: {},
  savingsGoals: [],
  walletBalances: { QRIS: 0, CASH: 0, TRANSFER: 0, CARD: 0 },
  recurringTemplates: [],

  // Draft state for AutoBudget tab
  budgetDraftIncome: 5000000,
  budgetDraftStrategy: "BALANCED_50_30_20",
  budgetDraftCategoryLimits: {},

  // Modals temporary state
  pendingReceiptBase64: "",
  activeSavingsTxGoalId: null,
  activeSavingsTxIsDeposit: true,
  activeWalletMethodKey: "QRIS"
};

let firebaseApp = null;
let firebaseAuth = null;
let firestoreDb = null;
let activeUnsubscribes = [];

/* --------------------------------------------------------------------------
   3. Utility Functions
   -------------------------------------------------------------------------- */
function formatMoney(amount) {
  const num = Math.round(Number(amount) || 0);
  const prefix = CURRENCY_SYMBOLS[state.currency] || "Rp ";
  const formatted = Math.abs(num).toLocaleString("id-ID");
  return num < 0 ? `-${prefix}${formatted}` : `${prefix}${formatted}`;
}

function formatCompactMoney(amount) {
  const num = Math.abs(Number(amount) || 0);
  if (num >= 1000000) {
    return `${(num / 1000000).toFixed(num % 1000000 === 0 ? 0 : 1)}jt`;
  }
  if (num >= 1000) {
    return `${Math.round(num / 1000)}rb`;
  }
  return String(Math.round(num));
}

function formatMonthDisplay(yearMonth) {
  const [y, m] = yearMonth.split("-").map(Number);
  const list = state.lang === "en" ? MONTHS_EN : MONTHS_ID;
  return `${list[(m || 1) - 1]} ${y}`;
}

function getDaysInYearMonth(yearMonth) {
  const [y, m] = yearMonth.split("-").map(Number);
  return new Date(y, m, 0).getDate();
}

function getTodayIsoDate() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function formatHumanDate(isoDate) {
  const today = getTodayIsoDate();
  const yestDate = new Date();
  yestDate.setDate(yestDate.getDate() - 1);
  const yesterday = `${yestDate.getFullYear()}-${String(yestDate.getMonth() + 1).padStart(2, "0")}-${String(yestDate.getDate()).padStart(2, "0")}`;

  const [y, m, d] = (isoDate || today).split("-").map(Number);
  const shortMonth = MONTHS_SHORT_ID[(m || 1) - 1] || "";
  if (isoDate === today) return `Hari Ini, ${d} ${shortMonth}`;
  if (isoDate === yesterday) return `Kemarin, ${d} ${shortMonth}`;
  return `${d} ${MONTHS_ID[(m || 1) - 1]} ${y}`;
}

function showToast(msg) {
  const el = document.getElementById("apkToast");
  if (!el) return;
  el.textContent = msg;
  el.classList.remove("hidden");
  clearTimeout(window.__toastTimer);
  window.__toastTimer = setTimeout(() => {
    el.classList.add("hidden");
  }, 2800);
}

function escapeHtml(str) {
  return String(str || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

/* --------------------------------------------------------------------------
   4. Theme & Language Management
   -------------------------------------------------------------------------- */
function applyTheme(theme) {
  state.theme = theme === "dark" ? "dark" : "light";
  document.documentElement.setAttribute("data-theme", state.theme);
  localStorage.setItem("sakupintar_theme", state.theme);

  const isDark = state.theme === "dark";
  const orb = isDark ? "🌙" : "☀️";
  const label = isDark
    ? (state.lang === "en" ? "Night" : "Malam")
    : (state.lang === "en" ? "Day" : "Siang");

  ["authThemeOrb", "drawerThemeOrb"].forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.textContent = orb;
  });
  ["authThemeLabel", "drawerThemeLabel"].forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.textContent = label;
  });
  const sub = document.getElementById("drawerThemeSubtitle");
  if (sub) {
    sub.textContent = isDark ? "Mode Gelap aktif" : "Mode Terang aktif";
  }
}

function applyLanguage(lang) {
  state.lang = lang === "en" ? "en" : "id";
  localStorage.setItem("sakupintar_lang", state.lang);
  const codeEl = document.getElementById("authLangCode");
  if (codeEl) codeEl.textContent = state.lang.toUpperCase();
  const sel = document.getElementById("drawerLanguageSelect");
  if (sel) sel.value = state.lang;
  applyTheme(state.theme);
  renderAll();
}

/* --------------------------------------------------------------------------
   5. Image Compression for Receipt Photos & Custom Avatars (< 900KB Firestore limit)
   -------------------------------------------------------------------------- */
function compressImageFileToBase64(file, maxSide = 720, quality = 0.72) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        const scale = Math.min(1, maxSide / Math.max(img.width, img.height));
        const canvas = document.createElement("canvas");
        canvas.width = Math.max(1, Math.round(img.width * scale));
        canvas.height = Math.max(1, Math.round(img.height * scale));
        const ctx = canvas.getContext("2d");
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
        resolve(canvas.toDataURL("image/jpeg", quality));
      };
      img.onerror = reject;
      img.src = e.target.result;
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

/* --------------------------------------------------------------------------
   6. Firebase Initialization & Cloud / Guest Persistence
   -------------------------------------------------------------------------- */
function getEffectiveFirebaseConfig() {
  const base = window.SAKUPINTAR_FIREBASE_CONFIG || {};
  const savedKey = localStorage.getItem("sakupintar_fb_api_key") || "";
  const savedAppId = localStorage.getItem("sakupintar_fb_app_id") || "";
  return {
    ...base,
    apiKey: (base.apiKey || savedKey).trim(),
    appId: (base.appId || savedAppId).trim()
  };
}

function initFirebaseConnection() {
  const cfg = getEffectiveFirebaseConfig();
  const keyInput = document.getElementById("firebaseApiKeyInput");
  const appInput = document.getElementById("firebaseAppIdInput");
  if (keyInput) keyInput.value = cfg.apiKey || "";
  if (appInput) appInput.value = cfg.appId || "";

  if (!cfg.apiKey || !cfg.projectId) {
    // Allow Guest mode or prompt when Google Sign-In is clicked
    if (localStorage.getItem("sakupintar_session_mode") === "guest") {
      enterGuestSession(false);
    }
    return false;
  }

  try {
    if (!firebaseApp) {
      firebaseApp = initializeApp({
        apiKey: cfg.apiKey,
        authDomain: cfg.authDomain,
        projectId: cfg.projectId,
        storageBucket: cfg.storageBucket,
        messagingSenderId: cfg.messagingSenderId,
        appId: cfg.appId
      });
      firebaseAuth = getAuth(firebaseApp);
      firestoreDb = cfg.databaseId
        ? getFirestore(firebaseApp, cfg.databaseId)
        : getFirestore(firebaseApp);

      onAuthStateChanged(firebaseAuth, (user) => {
        if (user) {
          state.user = user;
          state.isGuest = false;
          state.isAuthenticated = true;
          localStorage.setItem("sakupintar_session_mode", "google");
          subscribeToUserCloudCollections(user.uid);
          syncUserProfileDoc(user);
          switchRootView(true);
        } else if (localStorage.getItem("sakupintar_session_mode") === "guest") {
          enterGuestSession(false);
        } else {
          clearCloudSubscriptions();
          state.user = null;
          state.isAuthenticated = false;
          switchRootView(false);
        }
      });
    }
    return true;
  } catch (err) {
    console.error("Firebase init error:", err);
    return false;
  }
}

function clearCloudSubscriptions() {
  activeUnsubscribes.forEach((unsub) => {
    try { unsub(); } catch (_) {}
  });
  activeUnsubscribes = [];
}

async function syncUserProfileDoc(user) {
  if (!firestoreDb || !user) return;
  try {
    const profileRef = doc(firestoreDb, "users", user.uid);
    const payload = {
      uid: user.uid,
      displayName: (user.displayName || "Sobat SakuPintar").slice(0, 100),
      email: (user.email || "").slice(0, 150),
      updatedAt: Date.now()
    };
    if (state.customAvatarBase64 && state.customAvatarBase64.length < 750000) {
      payload.photoBase64 = state.customAvatarBase64;
    }
    await setDoc(profileRef, payload, { merge: true });
  } catch (e) {
    console.warn("Profile sync notice:", e);
  }
}

function subscribeToUserCloudCollections(uid) {
  clearCloudSubscriptions();
  if (!firestoreDb || !uid) return;

  // 1. User profile doc (for custom avatar & displayName)
  const unsubProfile = onSnapshot(doc(firestoreDb, "users", uid), (snap) => {
    if (snap.exists()) {
      const data = snap.data();
      if (data.photoBase64) {
        state.customAvatarBase64 = data.photoBase64;
        localStorage.setItem("sakupintar_custom_avatar", data.photoBase64);
      }
      renderTopBarAndDrawerProfile();
    }
  });

  // 2. Expenses collection
  const unsubExpenses = onSnapshot(collection(firestoreDb, "users", uid, "expenses"), (snap) => {
    const list = [];
    snap.forEach((docSnap) => {
      const d = docSnap.data();
      list.push({
        id: d.id || docSnap.id,
        title: d.title || "Pengeluaran",
        amount: Number(d.amount) || 0,
        category: CATEGORIES[d.category] ? d.category : "OTHER",
        date: d.date || getTodayIsoDate(),
        yearMonth: d.yearMonth || (d.date ? d.date.slice(0, 7) : state.selectedYearMonth),
        paymentMethod: PAYMENT_METHODS[d.paymentMethod] ? d.paymentMethod : "QRIS",
        note: d.note || "",
        receiptBase64: d.receiptBase64 || null,
        createdAt: Number(d.createdAt) || Date.now()
      });
    });
    list.sort((a, b) => b.date.localeCompare(a.date) || b.createdAt - a.createdAt);
    state.expenses = list;
    renderAll();
  });

  // 3. Budgets collection
  const unsubBudgets = onSnapshot(collection(firestoreDb, "users", uid, "budgets"), (snap) => {
    const map = {};
    snap.forEach((docSnap) => {
      const d = docSnap.data();
      const ym = d.yearMonth || docSnap.id;
      map[ym] = {
        yearMonth: ym,
        monthlyIncome: Number(d.monthlyIncome) || 0,
        savingsTargetPercent: Number(d.savingsTargetPercent) || 20,
        strategy: d.strategy || "BALANCED_50_30_20",
        categoryAllocations: d.categoryAllocations || {},
        updatedAt: Number(d.updatedAt) || Date.now()
      };
    });
    state.budgetsByMonth = map;
    syncBudgetDraftFromSelectedMonth();
    renderAll();
  });

  // 4. SavingsGoals collection
  const unsubSavings = onSnapshot(collection(firestoreDb, "users", uid, "savingsGoals"), (snap) => {
    const goals = [];
    snap.forEach((docSnap) => {
      const d = docSnap.data();
      goals.push({
        id: d.id || docSnap.id,
        title: d.title || "Target Tabungan",
        targetAmount: Number(d.targetAmount) || 0,
        savedAmount: Number(d.savedAmount) || 0,
        category: d.category || "Dana Darurat",
        note: d.note || "",
        createdAt: Number(d.createdAt) || Date.now(),
        updatedAt: Number(d.updatedAt) || Date.now()
      });
    });
    goals.sort((a, b) => b.updatedAt - a.updatedAt);
    state.savingsGoals = goals;
    renderAll();
  });

  // 5. WalletBalances collection
  const unsubWallets = onSnapshot(collection(firestoreDb, "users", uid, "walletBalances"), (snap) => {
    const balances = { QRIS: 0, CASH: 0, TRANSFER: 0, CARD: 0 };
    snap.forEach((docSnap) => {
      const d = docSnap.data();
      const m = d.paymentMethod || docSnap.id;
      if (PAYMENT_METHODS[m]) {
        balances[m] = Number(d.balance) || 0;
      }
    });
    state.walletBalances = balances;
    renderAll();
  });

  // 6. RecurringTemplates collection
  const unsubRecurring = onSnapshot(collection(firestoreDb, "users", uid, "recurringTemplates"), (snap) => {
    const templates = [];
    snap.forEach((docSnap) => {
      const d = docSnap.data();
      templates.push({
        id: d.id || docSnap.id,
        title: d.title || "Tagihan Rutin",
        amount: Number(d.amount) || 0,
        category: CATEGORIES[d.category] ? d.category : "BILLS_UTILITIES",
        paymentMethod: PAYMENT_METHODS[d.paymentMethod] ? d.paymentMethod : "TRANSFER",
        note: d.note || "",
        createdAt: Number(d.createdAt) || Date.now()
      });
    });
    templates.sort((a, b) => b.createdAt - a.createdAt);
    state.recurringTemplates = templates;
    renderAll();
  });

  activeUnsubscribes.push(unsubProfile, unsubExpenses, unsubBudgets, unsubSavings, unsubWallets, unsubRecurring);
}

/* Guest Mode LocalStorage Fallback */
function loadGuestDataFromLocal() {
  try {
    state.expenses = JSON.parse(localStorage.getItem("sakupintar_guest_expenses") || "[]");
    state.budgetsByMonth = JSON.parse(localStorage.getItem("sakupintar_guest_budgets") || "{}");
    state.savingsGoals = JSON.parse(localStorage.getItem("sakupintar_guest_savings") || "[]");
    state.walletBalances = JSON.parse(
      localStorage.getItem("sakupintar_guest_wallets") || '{"QRIS":0,"CASH":0,"TRANSFER":0,"CARD":0}'
    );
    state.recurringTemplates = JSON.parse(localStorage.getItem("sakupintar_guest_recurring") || "[]");
  } catch (_) {}
}

function saveGuestDataToLocal() {
  if (!state.isGuest) return;
  localStorage.setItem("sakupintar_guest_expenses", JSON.stringify(state.expenses));
  localStorage.setItem("sakupintar_guest_budgets", JSON.stringify(state.budgetsByMonth));
  localStorage.setItem("sakupintar_guest_savings", JSON.stringify(state.savingsGoals));
  localStorage.setItem("sakupintar_guest_wallets", JSON.stringify(state.walletBalances));
  localStorage.setItem("sakupintar_guest_recurring", JSON.stringify(state.recurringTemplates));
}

function enterGuestSession(notify = true) {
  clearCloudSubscriptions();
  state.isGuest = true;
  state.isAuthenticated = true;
  state.user = {
    uid: "guest",
    displayName: localStorage.getItem("sakupintar_guest_name") || "Pengunjung (Guest)",
    email: "Mode Pengunjung Lokal"
  };
  localStorage.setItem("sakupintar_session_mode", "guest");
  loadGuestDataFromLocal();
  syncBudgetDraftFromSelectedMonth();
  switchRootView(true);
  if (notify) showToast("Masuk sebagai Guest (Data tersimpan di browser)");
}

/* --------------------------------------------------------------------------
   7. Data Mutations (Firestore + Guest Local Fallback)
   -------------------------------------------------------------------------- */
async function saveExpenseRecord(expenseObj) {
  const id = expenseObj.id || (crypto.randomUUID ? crypto.randomUUID() : `exp_${Date.now()}`);
  const dateStr = expenseObj.date || getTodayIsoDate();
  const ym = dateStr.slice(0, 7);
  const record = {
    id,
    title: String(expenseObj.title || "Pengeluaran").trim().slice(0, 120),
    amount: Math.max(0.01, Number(expenseObj.amount) || 0),
    category: CATEGORIES[expenseObj.category] ? expenseObj.category : "OTHER",
    date: dateStr,
    yearMonth: ym,
    paymentMethod: PAYMENT_METHODS[expenseObj.paymentMethod] ? expenseObj.paymentMethod : "QRIS",
    note: String(expenseObj.note || "").trim().slice(0, 500),
    createdAt: Number(expenseObj.createdAt) || Date.now()
  };
  if (expenseObj.receiptBase64 && expenseObj.receiptBase64.length < 750000) {
    record.receiptBase64 = expenseObj.receiptBase64;
  }

  if (!state.isGuest && state.user && firestoreDb) {
    await setDoc(doc(firestoreDb, "users", state.user.uid, "expenses", id), record);
  } else {
    state.expenses = [record, ...state.expenses.filter((e) => e.id !== id)];
    saveGuestDataToLocal();
    renderAll();
  }
}

async function deleteExpenseRecord(expenseId) {
  if (!state.isGuest && state.user && firestoreDb) {
    await deleteDoc(doc(firestoreDb, "users", state.user.uid, "expenses", expenseId));
  } else {
    state.expenses = state.expenses.filter((e) => e.id !== expenseId);
    saveGuestDataToLocal();
    renderAll();
  }
  showToast("Pengeluaran dihapus");
}

async function saveMonthlyBudgetRecord(budgetObj) {
  const ym = budgetObj.yearMonth || state.selectedYearMonth;
  const record = {
    yearMonth: ym,
    monthlyIncome: Math.max(0, Number(budgetObj.monthlyIncome) || 0),
    savingsTargetPercent: Number(budgetObj.savingsTargetPercent) || 20,
    strategy: budgetObj.strategy || "BALANCED_50_30_20",
    categoryAllocations: budgetObj.categoryAllocations || {},
    updatedAt: Date.now()
  };

  if (!state.isGuest && state.user && firestoreDb) {
    await setDoc(doc(firestoreDb, "users", state.user.uid, "budgets", ym), record);
  } else {
    state.budgetsByMonth[ym] = record;
    saveGuestDataToLocal();
    renderAll();
  }
  showToast("Anggaran bulanan berhasil diterapkan!");
}

async function saveSavingsGoalRecord(goalObj) {
  const id = goalObj.id || (crypto.randomUUID ? crypto.randomUUID() : `goal_${Date.now()}`);
  const nowTs = Date.now();
  const record = {
    id,
    title: String(goalObj.title || "Target Tabungan").trim().slice(0, 120),
    targetAmount: Math.max(1, Number(goalObj.targetAmount) || 1),
    savedAmount: Math.max(0, Number(goalObj.savedAmount) || 0),
    category: String(goalObj.category || "Dana Darurat").slice(0, 60),
    note: String(goalObj.note || "").trim().slice(0, 300),
    createdAt: Number(goalObj.createdAt) || nowTs,
    updatedAt: nowTs
  };

  if (!state.isGuest && state.user && firestoreDb) {
    await setDoc(doc(firestoreDb, "users", state.user.uid, "savingsGoals", id), record);
  } else {
    state.savingsGoals = [record, ...state.savingsGoals.filter((g) => g.id !== id)];
    saveGuestDataToLocal();
    renderAll();
  }
}

async function deleteSavingsGoalRecord(goalId) {
  if (!state.isGuest && state.user && firestoreDb) {
    await deleteDoc(doc(firestoreDb, "users", state.user.uid, "savingsGoals", goalId));
  } else {
    state.savingsGoals = state.savingsGoals.filter((g) => g.id !== goalId);
    saveGuestDataToLocal();
    renderAll();
  }
  showToast("Target celengan dihapus");
}

async function saveWalletBalanceRecord(methodKey, balanceVal) {
  const record = {
    paymentMethod: methodKey,
    balance: Math.max(0, Number(balanceVal) || 0),
    updatedAt: Date.now()
  };

  if (!state.isGuest && state.user && firestoreDb) {
    await setDoc(doc(firestoreDb, "users", state.user.uid, "walletBalances", methodKey), record);
  } else {
    state.walletBalances[methodKey] = record.balance;
    saveGuestDataToLocal();
    renderAll();
  }
  showToast(`Saldo ${PAYMENT_METHODS[methodKey]?.label || methodKey} diperbarui`);
}

async function saveRecurringTemplateRecord(tplObj) {
  const id = tplObj.id || (crypto.randomUUID ? crypto.randomUUID() : `rec_${Date.now()}`);
  const record = {
    id,
    title: String(tplObj.title || "Tagihan Rutin").trim().slice(0, 120),
    amount: Math.max(1, Number(tplObj.amount) || 1),
    category: CATEGORIES[tplObj.category] ? tplObj.category : "BILLS_UTILITIES",
    paymentMethod: PAYMENT_METHODS[tplObj.paymentMethod] ? tplObj.paymentMethod : "TRANSFER",
    note: String(tplObj.note || "").trim().slice(0, 300),
    createdAt: Date.now()
  };

  if (!state.isGuest && state.user && firestoreDb) {
    await setDoc(doc(firestoreDb, "users", state.user.uid, "recurringTemplates", id), record);
  } else {
    state.recurringTemplates = [record, ...state.recurringTemplates.filter((t) => t.id !== id)];
    saveGuestDataToLocal();
    renderAll();
  }
  showToast("Template pengeluaran rutin disimpan");
}

async function deleteRecurringTemplateRecord(tplId) {
  if (!state.isGuest && state.user && firestoreDb) {
    await deleteDoc(doc(firestoreDb, "users", state.user.uid, "recurringTemplates", tplId));
  } else {
    state.recurringTemplates = state.recurringTemplates.filter((t) => t.id !== tplId);
    saveGuestDataToLocal();
    renderAll();
  }
  showToast("Template rutin dihapus");
}

/* --------------------------------------------------------------------------
   8. AutoBudget Calculation Engine (Matches AutoBudgetEngine in APK)
   -------------------------------------------------------------------------- */
function computeAutoBudgetAllocations(income, strategy, historicalExpenses = []) {
  const totalIncome = Math.max(0, Number(income) || 0);
  let needsRatio = 0.50;
  let wantsRatio = 0.30;
  let savingsPercent = 20;

  if (strategy === "FRUGAL_60_10_30") {
    needsRatio = 0.60;
    wantsRatio = 0.10;
    savingsPercent = 30;
  } else if (strategy === "ADAPTIVE_SMART") {
    needsRatio = 0.52;
    wantsRatio = 0.28;
    savingsPercent = 20;
  }

  const needsPool = totalIncome * needsRatio;
  const wantsPool = totalIncome * wantsRatio;

  const allocations = {};
  Object.values(CATEGORIES).forEach((cat) => {
    const raw = (cat.isNeed ? needsPool : wantsPool) * cat.defaultWeight;
    allocations[cat.key] = Math.round(raw / 5000) * 5000;
  });

  // If ADAPTIVE_SMART and user has expenses, blend historical proportions
  if (strategy === "ADAPTIVE_SMART" && historicalExpenses.length > 0) {
    const spendable = needsPool + wantsPool;
    const totalHist = historicalExpenses.reduce((s, e) => s + e.amount, 0);
    if (totalHist > 0) {
      Object.values(CATEGORIES).forEach((cat) => {
        const catHist = historicalExpenses
          .filter((e) => e.category === cat.key)
          .reduce((s, e) => s + e.amount, 0);
        const histShare = catHist / totalHist;
        const blended = allocations[cat.key] * 0.55 + spendable * histShare * 0.45;
        allocations[cat.key] = Math.max(25000, Math.round(blended / 5000) * 5000);
      });
    }
  }

  return { allocations, savingsPercent };
}

function syncBudgetDraftFromSelectedMonth() {
  const existing = state.budgetsByMonth[state.selectedYearMonth];
  if (existing && existing.monthlyIncome > 0) {
    state.budgetDraftIncome = existing.monthlyIncome;
    state.budgetDraftStrategy = existing.strategy || "BALANCED_50_30_20";
    state.budgetDraftCategoryLimits = { ...existing.categoryAllocations };
  } else {
    const monthExps = state.expenses.filter((e) => e.yearMonth === state.selectedYearMonth);
    const { allocations } = computeAutoBudgetAllocations(
      state.budgetDraftIncome,
      state.budgetDraftStrategy,
      monthExps
    );
    state.budgetDraftCategoryLimits = allocations;
  }
}

/* --------------------------------------------------------------------------
   9. Root View & Navigation Switching (Matches MainActivity.kt & MainAppScaffold.kt)
   -------------------------------------------------------------------------- */
function switchRootView(isLoggedIn) {
  const authView = document.getElementById("authView");
  const mainScaffold = document.getElementById("mainAppScaffold");
  if (isLoggedIn) {
    authView.classList.add("hidden");
    mainScaffold.classList.remove("hidden");
    renderAll();
  } else {
    mainScaffold.classList.add("hidden");
    authView.classList.remove("hidden");
    setAuthStep("LANDING");
  }
}

function setAuthStep(step) {
  state.authStep = step;
  const landing = document.getElementById("authStepLanding");
  const method = document.getElementById("authStepMethod");
  if (step === "METHOD_SELECTION") {
    landing.classList.add("hidden");
    method.classList.remove("hidden");
  } else {
    method.classList.add("hidden");
    landing.classList.add("hidden");
    // trigger reflow for animation
    void landing.offsetWidth;
    landing.classList.remove("hidden");
  }
}

function setAuthMode(mode) {
  state.authMode = mode;
  const tabIn = document.getElementById("tabModeSignIn");
  const tabUp = document.getElementById("tabModeSignUp");
  const title = document.getElementById("authMethodTitle");
  const sub = document.getElementById("authMethodSubtitle");
  const desc = document.getElementById("authModeDescription");
  const googleLabel = document.getElementById("btnGoogleSignInLabel");

  if (mode === "SIGN_UP") {
    tabIn.classList.remove("active");
    tabUp.classList.add("active");
    title.textContent = "Halaman Pilihan Sign Up";
    sub.textContent = "Pilih metode pendaftaran di bawah ini untuk membuat akun SakuPintar baru.";
    desc.textContent = "Pengguna baru? Daftar dengan akun Google agar catatan pengeluaran & anggaran tersimpan otomatis di cloud.";
    googleLabel.textContent = "Daftar dengan Akun Google";
  } else {
    tabUp.classList.remove("active");
    tabIn.classList.add("active");
    title.textContent = "Halaman Pilihan Sign In";
    sub.textContent = "Pilih metode masuk di bawah ini untuk memulihkan data keuangan Anda dari cloud atau masuk sebagai pengunjung.";
    desc.textContent = "Sudah punya akun? Masuk dengan Google untuk memulihkan & menyinkronkan data anggaran Anda.";
    googleLabel.textContent = "Masuk dengan Google";
  }
}

function setActiveTab(tabKey) {
  state.activeTab = tabKey;
  ["dashboard", "expenses", "savings", "budget"].forEach((k) => {
    const sec = document.getElementById(`tabScreen-${k}`);
    if (sec) sec.classList.toggle("hidden", k !== tabKey);
  });

  document.querySelectorAll("[data-nav-tab]").forEach((btn) => {
    btn.classList.toggle("active", btn.getAttribute("data-nav-tab") === tabKey);
  });

  // Extended FAB (+ Catat) is shown on Harian & Beranda for quick access
  const fab = document.getElementById("fabAddExpense");
  if (fab) {
    fab.classList.toggle("hidden", tabKey === "savings" || tabKey === "budget");
  }

  renderTopBarAndDrawerProfile();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

/* --------------------------------------------------------------------------
   10. Rendering Functions (1:1 APK Screens)
   -------------------------------------------------------------------------- */
function renderAll() {
  if (!state.isAuthenticated) return;

  // Update all current-month-label spans
  const monthText = formatMonthDisplay(state.selectedYearMonth);
  document.querySelectorAll(".current-month-label").forEach((el) => {
    el.textContent = monthText;
  });

  renderTopBarAndDrawerProfile();
  renderDashboardTab();
  renderDailyExpensesTab();
  renderSavingsTab();
  renderAutoBudgetTab();
}

function renderTopBarAndDrawerProfile() {
  const firstName = ((state.user?.displayName || "Sobat").trim().split(/\s+/)[0]) || "Sobat";
  const initials = (state.user?.displayName || "SP")
    .split(/\s+/)
    .map((w) => w[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();

  const iconEl = document.getElementById("topRouteIconBox");
  const titleEl = document.getElementById("topRouteTitle");
  const subEl = document.getElementById("topRouteSubtitle");

  if (state.activeTab === "dashboard") {
    iconEl.textContent = "📊";
    titleEl.textContent = `Halo, ${firstName}`;
    subEl.textContent = "Pantau pengeluaran & anggaran bulan ini";
  } else if (state.activeTab === "expenses") {
    iconEl.textContent = "🧾";
    titleEl.textContent = "Catatan Pengeluaran Harian";
    subEl.textContent = "Kelola transaksi harian & pengeluaran rutin";
  } else if (state.activeTab === "savings") {
    iconEl.textContent = "🐷";
    titleEl.textContent = "Celengan & Saldo Dompet";
    subEl.textContent = "Pantau tabungan impian & sumber dana aktif";
  } else {
    iconEl.textContent = "✨";
    titleEl.textContent = "Perencana Anggaran Bulanan";
    subEl.textContent = "Alokasi pintar 50/30/20 & target batas harian";
  }

  // Cloud status pill
  const pill = document.getElementById("cloudSyncStatusPill");
  const text = document.getElementById("cloudSyncText");
  if (!state.isGuest && state.user) {
    pill.classList.add("online");
    text.textContent = "Cloud Sync";
  } else {
    pill.classList.remove("online");
    text.textContent = "Guest";
  }

  // Avatar in TopAppBar & Profile Drawer
  const avatarSrc = state.customAvatarBase64 || state.user?.photoURL || "";
  const topBtn = document.getElementById("btnOpenProfileDrawer");
  const drawerAvatar = document.getElementById("drawerAvatarBox");

  if (avatarSrc) {
    topBtn.innerHTML = `<img src="${escapeHtml(avatarSrc)}" alt="Profil" />`;
    drawerAvatar.innerHTML = `<img src="${escapeHtml(avatarSrc)}" alt="Profil" />`;
  } else {
    topBtn.innerHTML = `<span>${escapeHtml(initials)}</span>`;
    drawerAvatar.innerHTML = `<span>${escapeHtml(initials)}</span>`;
  }

  document.getElementById("drawerUserName").textContent = state.user?.displayName || "Sobat SakuPintar";
  document.getElementById("drawerUserEmail").textContent = state.isGuest
    ? "Mode Pengunjung (Data Lokal)"
    : (state.user?.email || "Akun Google Terhubung");
  document.getElementById("drawerAccountBadge").textContent = state.isGuest
    ? "👤 Mode Pengunjung"
    : "☁️ Sinkronisasi Cloud Aktif";
  document.getElementById("btnSignOutLabel").textContent = state.isGuest
    ? "Masuk / Ganti ke Akun Google"
    : "Keluar dari Akun";
}

/* TAB 1: BERANDA (DashboardScreen.kt) */
function renderDashboardTab() {
  const monthExps = state.expenses.filter((e) => e.yearMonth === state.selectedYearMonth);
  const totalSpent = monthExps.reduce((s, e) => s + e.amount, 0);
  const budget = state.budgetsByMonth[state.selectedYearMonth];
  const hasBudget = Boolean(budget && budget.monthlyIncome > 0);

  const totalCategoryBudget = hasBudget
    ? Object.values(budget.categoryAllocations || {}).reduce((s, v) => s + (Number(v) || 0), 0)
    : 0;
  const remainingBudget = totalCategoryBudget - totalSpent;
  const savingsTargetAmount = hasBudget
    ? (budget.monthlyIncome * (budget.savingsTargetPercent || 20)) / 100
    : 0;

  const daysInMonth = getDaysInYearMonth(state.selectedYearMonth);
  const todayIso = getTodayIsoDate();
  const isCurrentMonth = state.selectedYearMonth === todayIso.slice(0, 7);
  const currentDayNum = isCurrentMonth ? Number(todayIso.slice(8, 10)) : 1;
  const remainingDays = isCurrentMonth ? Math.max(1, daysInMonth - currentDayNum + 1) : daysInMonth;
  const dailySafeLimit = hasBudget ? Math.max(0, remainingBudget / remainingDays) : 0;
  const todaySpent = monthExps
    .filter((e) => e.date === todayIso)
    .reduce((s, e) => s + e.amount, 0);

  // Pace Status Badge
  const paceBadge = document.getElementById("dashboardPaceBadge");
  paceBadge.classList.remove("warning", "danger");
  if (!hasBudget) {
    paceBadge.textContent = "✨ Belum Diatur";
  } else if (remainingBudget < 0) {
    paceBadge.classList.add("danger");
    paceBadge.textContent = "⚠️ Melebihi Anggaran";
  } else if (totalSpent > totalCategoryBudget * 0.8) {
    paceBadge.classList.add("warning");
    paceBadge.textContent = "⚠️ Mendekati Batas";
  } else {
    paceBadge.textContent = "🛡️ Aman Terkendali";
  }

  // Hero Caption & Amount
  const heroCaption = document.getElementById("dashboardHeroCaption");
  const heroAmount = document.getElementById("dashboardHeroAmount");
  const activeSec = document.getElementById("dashboardBudgetActiveSection");
  const noBudgetSec = document.getElementById("dashboardNoBudgetSection");

  if (hasBudget) {
    heroCaption.textContent = "Sisa Anggaran Belanja";
    heroAmount.textContent = formatMoney(remainingBudget);
    activeSec.classList.remove("hidden");
    noBudgetSec.classList.add("hidden");

    const pct = totalCategoryBudget > 0 ? Math.min(100, Math.round((totalSpent / totalCategoryBudget) * 100)) : 0;
    const fillEl = document.getElementById("dashboardBudgetProgressFill");
    fillEl.style.width = `${pct}%`;
    fillEl.classList.toggle("danger", remainingBudget < 0);

    document.getElementById("dashboardSpentLabel").textContent = `Terpakai ${formatMoney(totalSpent)}`;
    document.getElementById("dashboardTotalBudgetLabel").textContent = `Dari ${formatMoney(totalCategoryBudget)}`;
    document.getElementById("dashboardDailyLimitVal").textContent = formatMoney(dailySafeLimit);
    document.getElementById("dashboardTodaySpentSub").textContent = `Hari ini: ${formatMoney(todaySpent)}`;
    document.getElementById("dashboardSavingsTargetVal").textContent = formatMoney(savingsTargetAmount);
    document.getElementById("dashboardSavingsRatioSub").textContent = `${budget.savingsTargetPercent || 20}% pendapatan`;
  } else {
    heroCaption.textContent = "Total Pengeluaran Bulan Ini";
    heroAmount.textContent = formatMoney(totalSpent);
    activeSec.classList.add("hidden");
    noBudgetSec.classList.remove("hidden");
  }

  // Quick Bento Row Summaries
  const totalSavedAll = state.savingsGoals.reduce((s, g) => s + g.savedAmount, 0);
  const totalWalletAll = Object.values(state.walletBalances).reduce((s, v) => s + (Number(v) || 0), 0);
  document.getElementById("bentoSavingsWalletSummary").textContent =
    `Tabungan: ${formatMoney(totalSavedAll)} • Dompet: ${formatMoney(totalWalletAll)}`;

  const receiptsCount = monthExps.filter((e) => Boolean(e.receiptBase64)).length;
  document.getElementById("bentoExportSubtitle").textContent = receiptsCount > 0
    ? `${monthExps.length} transaksi • ${receiptsCount} foto struk siap ekspor`
    : "Simpan ke Excel (.xls), PDF, CSV, atau bagikan langsung";

  // 7-Day Spending Bar Chart
  renderSevenDayChart(dailySafeLimit);

  // Category Progress List
  const catListEl = document.getElementById("dashboardCategoryProgressList");
  const spentByCategory = {};
  monthExps.forEach((e) => {
    spentByCategory[e.category] = (spentByCategory[e.category] || 0) + e.amount;
  });

  catListEl.innerHTML = Object.values(CATEGORIES)
    .map((cat) => {
      const allocated = hasBudget ? Number(budget.categoryAllocations?.[cat.key] || 0) : 0;
      const spent = spentByCategory[cat.key] || 0;
      if (!hasBudget && spent === 0) return "";
      const rem = allocated - spent;
      const pct = allocated > 0 ? Math.min(100, Math.round((spent / allocated) * 100)) : (spent > 0 ? 100 : 0);
      const isOver = allocated > 0 && spent > allocated;
      const statusText = allocated > 0
        ? (isOver ? `Lewat ${formatMoney(spent - allocated)}` : `Sisa ${formatMoney(rem)}`)
        : formatMoney(spent);

      return `
        <div class="sculpted-card category-progress-row">
          <div class="cat-icon-badge" style="background: ${cat.bgSoft}; color: ${cat.color};">${cat.icon}</div>
          <div class="category-progress-main">
            <div class="category-progress-top">
              <strong style="font-size: 13.5px;">${escapeHtml(cat.labelId)}</strong>
              <span class="mono" style="font-size: 12px; font-weight: 800; color: ${isOver ? "var(--coral-danger)" : "var(--text-muted)"};">
                ${statusText}
              </span>
            </div>
            <div class="category-progress-sub mono">
              ${formatMoney(spent)}${allocated > 0 ? ` / ${formatMoney(allocated)}` : ""}
            </div>
            <div class="mini-progress-track">
              <div class="mini-progress-fill" style="width: ${pct}%; background: ${isOver ? "var(--coral-danger)" : cat.color};"></div>
            </div>
          </div>
        </div>
      `;
    })
    .join("") || `<div class="sculpted-card" style="text-align:center; color: var(--text-muted); font-size: 13px;">Belum ada alokasi atau pengeluaran tercatat di bulan ini.</div>`;

  // Recent 4 Expenses
  const recentEl = document.getElementById("dashboardRecentExpensesList");
  const top4 = monthExps.slice(0, 4);
  if (top4.length === 0) {
    recentEl.innerHTML = `
      <div class="sculpted-card" style="text-align: center; padding: 24px 16px;">
        <div style="font-size: 26px; margin-bottom: 6px;">🧾</div>
        <div style="font-size: 14px; font-weight: 800;">Belum Ada Pengeluaran Tercatat</div>
        <div style="font-size: 12px; color: var(--text-muted); margin-top: 3px;">Tekan tombol + Catat untuk mulai mencatat pengeluaran harian Anda.</div>
      </div>
    `;
  } else {
    recentEl.innerHTML = top4.map((exp) => renderExpenseItemHtml(exp, false)).join("");
  }
}

function renderSevenDayChart(dailySafeLimit) {
  const container = document.getElementById("sevenDayChartContainer");
  const limitPill = document.getElementById("chartSafeLimitPill");
  if (!container) return;

  if (dailySafeLimit > 0) {
    limitPill.textContent = `Batas ${formatCompactMoney(dailySafeLimit)}`;
    limitPill.classList.remove("hidden");
  } else {
    limitPill.classList.add("hidden");
  }

  const days = [];
  for (let i = 6; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    const iso = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
    const total = state.expenses
      .filter((e) => e.date === iso)
      .reduce((s, e) => s + e.amount, 0);
    const label = i === 0
      ? "Hari Ini"
      : d.toLocaleDateString("id-ID", { weekday: "short" });
    days.push({ iso, total, label, isToday: i === 0 });
  }

  const maxVal = Math.max(
    dailySafeLimit * 1.15,
    ...days.map((d) => d.total),
    50000
  );

  const safeLineBottomPct = dailySafeLimit > 0
    ? Math.min(92, Math.max(12, Math.round((dailySafeLimit / maxVal) * 92)))
    : 0;

  container.innerHTML = `
    ${dailySafeLimit > 0 ? `<div class="safe-limit-dashed-line" style="bottom: ${safeLineBottomPct + 22}px;"></div>` : ""}
    ${days
      .map((d) => {
        const barPct = d.total > 0 ? Math.max(8, Math.round((d.total / maxVal) * 100)) : 4;
        const isOver = dailySafeLimit > 0 && d.total > dailySafeLimit;
        return `
          <div class="day-bar-col">
            <span class="day-bar-amt">${d.total > 0 ? formatCompactMoney(d.total) : "-"}</span>
            <div class="day-bar-track">
              <div class="day-bar-fill ${d.isToday ? "today" : ""} ${isOver ? "over-limit" : ""}" style="height: ${barPct}%;"></div>
            </div>
            <span class="day-bar-label ${d.isToday ? "today" : ""}">${escapeHtml(d.label)}</span>
          </div>
        `;
      })
      .join("")}
  `;
}

/* TAB 2: HARIAN (DailyExpensesScreen.kt) */
function renderDailyExpensesTab() {
  // 1. Category Filter Strip
  const stripEl = document.getElementById("expenseCategoryFilterStrip");
  const filterItems = [
    { key: "ALL", label: "Semua", icon: "✨" },
    ...Object.values(CATEGORIES).map((c) => ({ key: c.key, label: c.shortId, icon: c.icon }))
  ];
  stripEl.innerHTML = filterItems
    .map(
      (f) => `
      <button type="button" class="apk-filter-chip ${state.selectedCategoryFilter === f.key ? "active" : ""}" data-cat-filter="${f.key}">
        <span>${f.icon}</span>
        <span>${escapeHtml(f.label)}</span>
      </button>
    `
    )
    .join("");

  // 2. Filter Expenses for Selected Month
  const monthExps = state.expenses.filter((e) => e.yearMonth === state.selectedYearMonth);
  const filteredExps = monthExps.filter((e) => {
    const matchesCat = state.selectedCategoryFilter === "ALL" || e.category === state.selectedCategoryFilter;
    const matchesDate = !state.selectedCalendarDate || e.date === state.selectedCalendarDate;
    const q = state.searchQuery.trim().toLowerCase();
    const catLabel = CATEGORIES[e.category]?.labelId.toLowerCase() || "";
    const matchesQuery =
      !q ||
      e.title.toLowerCase().includes(q) ||
      (e.note || "").toLowerCase().includes(q) ||
      catLabel.includes(q);
    return matchesCat && matchesDate && matchesQuery;
  });

  const totalFiltered = filteredExps.reduce((s, e) => s + e.amount, 0);
  const receiptsCount = filteredExps.filter((e) => Boolean(e.receiptBase64)).length;

  document.getElementById("expensesFilteredCountLabel").textContent = state.selectedCalendarDate
    ? `Tanggal: ${formatHumanDate(state.selectedCalendarDate)} (${filteredExps.length} transaksi)`
    : `${filteredExps.length} transaksi di ${formatMonthDisplay(state.selectedYearMonth)}`;
  document.getElementById("expensesFilteredTotalVal").textContent = formatMoney(totalFiltered);

  const recBadge = document.getElementById("expensesReceiptCountBadge");
  if (receiptsCount > 0) {
    recBadge.textContent = `🧾 ${receiptsCount} foto struk terlampir`;
    recBadge.classList.remove("hidden");
  } else {
    recBadge.classList.add("hidden");
  }

  // 3. Monthly Calendar Grid
  renderMonthlyCalendarGrid(monthExps);

  // 4. Recurring Templates Row
  renderRecurringTemplatesRow();

  // 5. Grouped Daily Expenses List
  const listContainer = document.getElementById("groupedDailyExpensesContainer");
  if (filteredExps.length === 0) {
    listContainer.innerHTML = `
      <div class="sculpted-card" style="text-align: center; padding: 28px 16px;">
        <div style="font-size: 28px; margin-bottom: 6px;">🧾</div>
        <div style="font-size: 14.5px; font-weight: 800;">Tidak Ada Transaksi Ditemukan</div>
        <div style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">
          Tekan tombol <strong>+ Catat</strong> di kanan bawah untuk menambahkan pengeluaran baru.
        </div>
      </div>
    `;
    return;
  }

  const groups = {};
  filteredExps.forEach((e) => {
    if (!groups[e.date]) groups[e.date] = [];
    groups[e.date].push(e);
  });

  listContainer.innerHTML = Object.keys(groups)
    .sort((a, b) => b.localeCompare(a))
    .map((dateStr) => {
      const items = groups[dateStr];
      const daySum = items.reduce((s, i) => s + i.amount, 0);
      return `
        <div class="date-group-header">
          <span class="date-group-title">${escapeHtml(formatHumanDate(dateStr))}</span>
          <span class="date-group-total-pill">${formatMoney(daySum)}</span>
        </div>
        ${items.map((exp) => renderExpenseItemHtml(exp, true)).join("")}
      `;
    })
    .join("");
}

function renderExpenseItemHtml(exp, allowDelete = true) {
  const cat = CATEGORIES[exp.category] || CATEGORIES.OTHER;
  const pm = PAYMENT_METHODS[exp.paymentMethod] || PAYMENT_METHODS.QRIS;
  return `
    <div class="sculpted-card detailed-expense-card">
      <div class="expense-accent-bar" style="background: ${cat.color};"></div>
      <div class="cat-icon-badge" style="background: ${cat.bgSoft}; color: ${cat.color};">${cat.icon}</div>
      <div class="expense-main-info">
        <div class="expense-title-text">${escapeHtml(exp.title)}</div>
        <div class="expense-meta-row">
          <span class="expense-cat-tag" style="background: ${cat.bgSoft}; color: ${cat.color};">${escapeHtml(cat.shortId)}</span>
          <span style="font-size: 11px; color: var(--text-muted); font-weight: 600;">• ${pm.icon} ${escapeHtml(pm.label)}</span>
        </div>
        ${exp.note ? `<div class="expense-note-text">${escapeHtml(exp.note)}</div>` : ""}
      </div>
      ${
        exp.receiptBase64
          ? `<button type="button" class="receipt-thumb-badge" data-view-receipt="${escapeHtml(exp.id)}" title="Lihat Foto Struk">
               <img src="${escapeHtml(exp.receiptBase64)}" alt="Struk" />
             </button>`
          : ""
      }
      <div class="expense-right-col">
        <span class="expense-amount-neg">-${formatMoney(exp.amount)}</span>
        ${
          allowDelete
            ? `<button type="button" class="expense-delete-btn" data-delete-expense="${escapeHtml(exp.id)}">🗑️ Hapus</button>`
            : `<span style="font-size: 11px; color: var(--text-muted);">${escapeHtml(formatHumanDate(exp.date))}</span>`
        }
      </div>
    </div>
  `;
}

function renderMonthlyCalendarGrid(monthExps) {
  const body = document.getElementById("monthlyCalendarBody");
  const toggleBtn = document.getElementById("btnToggleCalendarExpand");
  body.classList.toggle("hidden", !state.isCalendarExpanded);
  toggleBtn.textContent = state.isCalendarExpanded ? "▴" : "▾";

  const [y, m] = state.selectedYearMonth.split("-").map(Number);
  const daysInMonth = new Date(y, m, 0).getDate();
  const firstDayJs = new Date(y, m - 1, 1).getDay(); // 0=Sun..6=Sat
  const leadingEmpty = (firstDayJs + 6) % 7; // Monday=0

  const totalsByDate = {};
  monthExps.forEach((e) => {
    totalsByDate[e.date] = (totalsByDate[e.date] || 0) + e.amount;
  });

  const budget = state.budgetsByMonth[state.selectedYearMonth];
  const totalCatBudget = budget
    ? Object.values(budget.categoryAllocations || {}).reduce((s, v) => s + (Number(v) || 0), 0)
    : 0;
  const dailySafeLimit = totalCatBudget > 0 ? totalCatBudget / daysInMonth : 0;

  const todayIso = getTodayIsoDate();
  let cellsHtml = "";
  for (let i = 0; i < leadingEmpty; i++) {
    cellsHtml += `<div class="calendar-day-cell empty"></div>`;
  }

  for (let day = 1; day <= daysInMonth; day++) {
    const iso = `${state.selectedYearMonth}-${String(day).padStart(2, "0")}`;
    const spent = totalsByDate[iso] || 0;
    const isSelected = state.selectedCalendarDate === iso;
    const isToday = todayIso === iso;
    const isOver = dailySafeLimit > 0 && spent > dailySafeLimit;
    const dotColor = isSelected
      ? "#FFFFFF"
      : isOver
      ? "var(--coral-danger)"
      : "var(--primary)";

    cellsHtml += `
      <div class="calendar-day-cell ${isToday ? "today" : ""} ${isSelected ? "selected" : ""}" data-calendar-date="${iso}">
        <span>${day}</span>
        ${spent > 0 ? `<span class="calendar-day-dot" style="background: ${dotColor};"></span>` : ""}
      </div>
    `;
  }

  document.getElementById("monthlyCalendarDaysGrid").innerHTML = cellsHtml;

  const bar = document.getElementById("calendarSelectedDateBar");
  if (state.selectedCalendarDate) {
    bar.classList.remove("hidden");
    const daySpent = totalsByDate[state.selectedCalendarDate] || 0;
    document.getElementById("calendarSelectedDateText").textContent = `Tanggal: ${formatHumanDate(state.selectedCalendarDate)}`;
    document.getElementById("calendarSelectedDateTotal").textContent = formatMoney(daySpent);
  } else {
    bar.classList.add("hidden");
  }
}

function renderRecurringTemplatesRow() {
  const container = document.getElementById("recurringTemplatesContainer");
  if (state.recurringTemplates.length === 0) {
    container.innerHTML = `
      <div class="sculpted-card" style="width: 100%; background: var(--surface-variant-alpha); padding: 12px 14px; font-size: 12px; color: var(--text-muted);">
        Belum ada tagihan atau langganan rutin. Tekan <strong>+ Tambah Rutin</strong> untuk menyimpan pengeluaran bulanan tetap (seperti Kost, WiFi, Listrik, atau Netflix).
      </div>
    `;
    return;
  }

  container.innerHTML = state.recurringTemplates
    .map((tpl) => {
      const cat = CATEGORIES[tpl.category] || CATEGORIES.BILLS_UTILITIES;
      return `
        <div class="recurring-card" data-record-recurring="${escapeHtml(tpl.id)}">
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <span class="cat-icon-badge" style="width: 32px; height: 32px; font-size: 15px; background: ${cat.bgSoft}; color: ${cat.color};">${cat.icon}</span>
            <button type="button" class="calendar-icon-btn" style="width: 24px; height: 24px; font-size: 11px;" data-delete-recurring="${escapeHtml(tpl.id)}" title="Hapus Rutin">✕</button>
          </div>
          <div style="font-size: 13px; font-weight: 800; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${escapeHtml(tpl.title)}</div>
          <div class="mono" style="font-size: 12.5px; font-weight: 800; color: var(--primary);">${formatMoney(tpl.amount)}</div>
          <div style="font-size: 11px; color: var(--primary); font-weight: 700;">＋ Ketuk untuk catat</div>
        </div>
      `;
    })
    .join("");
}

/* TAB 3: TABUNGAN (SavingsScreen.kt) */
function renderSavingsTab() {
  const totalSaved = state.savingsGoals.reduce((s, g) => s + g.savedAmount, 0);
  const totalTarget = state.savingsGoals.reduce((s, g) => s + g.targetAmount, 0);
  const totalWallet = Object.values(state.walletBalances).reduce((s, v) => s + (Number(v) || 0), 0);

  document.getElementById("savingsTotalSavedVal").textContent = formatMoney(totalSaved);
  document.getElementById("savingsTotalWalletVal").textContent = formatMoney(totalWallet);

  const progWrap = document.getElementById("savingsTargetProgressWrap");
  if (totalTarget > 0) {
    progWrap.classList.remove("hidden");
    const pct = Math.min(100, Math.round((totalSaved / totalTarget) * 100));
    document.getElementById("savingsTotalProgressFill").style.width = `${pct}%`;
    document.getElementById("savingsPercentOfTargetLabel").textContent = `${pct}% dari total target`;
    document.getElementById("savingsTotalTargetValLabel").textContent = `Target: ${formatMoney(totalTarget)}`;
  } else {
    progWrap.classList.add("hidden");
  }

  // 2x2 Wallets Grid
  const walletsGrid = document.getElementById("walletsGridContainer");
  walletsGrid.innerHTML = Object.values(PAYMENT_METHODS)
    .map((pm) => {
      const bal = Number(state.walletBalances[pm.key] || 0);
      return `
        <div class="sculpted-card clickable" data-edit-wallet="${pm.key}">
          <div class="wallet-card-top">
            <span class="cat-icon-badge" style="width: 36px; height: 36px; font-size: 17px; background: rgba(37, 99, 235, 0.12); color: ${pm.color};">${pm.icon}</span>
            <span style="font-size: 12px; color: var(--text-muted);">✏️</span>
          </div>
          <div style="font-size: 12px; color: var(--text-muted); font-weight: 600;">${escapeHtml(pm.label)}</div>
          <div class="mono" style="font-size: 14.5px; font-weight: 800; margin-top: 2px;">${formatMoney(bal)}</div>
        </div>
      `;
    })
    .join("");

  // Savings Goals List
  const goalsContainer = document.getElementById("savingsGoalsListContainer");
  if (state.savingsGoals.length === 0) {
    goalsContainer.innerHTML = `
      <div class="sculpted-card" style="text-align: center; padding: 26px 16px;">
        <div style="font-size: 28px; margin-bottom: 6px;">🐷</div>
        <div style="font-size: 14.5px; font-weight: 800;">Belum Ada Target Celengan</div>
        <div style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">
          Buat celengan untuk Dana Darurat, Liburan, Gadget, atau impian Anda dan pantau progresnya.
        </div>
      </div>
    `;
    return;
  }

  goalsContainer.innerHTML = state.savingsGoals
    .map((goal) => {
      const pct = goal.targetAmount > 0 ? Math.min(100, Math.round((goal.savedAmount / goal.targetAmount) * 100)) : 0;
      const rem = Math.max(0, goal.targetAmount - goal.savedAmount);
      const isDone = rem <= 0 && goal.targetAmount > 0;
      return `
        <div class="sculpted-card savings-goal-card">
          <div style="display: flex; align-items: center; justify-content: space-between; gap: 10px;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <div class="cat-icon-badge" style="background: rgba(245, 158, 11, 0.16); color: var(--gold-accent);">🐷</div>
              <div>
                <div style="font-size: 14.5px; font-weight: 800;">${escapeHtml(goal.title)}</div>
                <div style="font-size: 11.5px; color: var(--text-muted);">
                  ${escapeHtml(goal.category)}${goal.note ? ` • ${escapeHtml(goal.note)}` : ""}
                </div>
              </div>
            </div>
            <button type="button" class="expense-delete-btn" data-delete-goal="${escapeHtml(goal.id)}">🗑️</button>
          </div>

          <div>
            <div style="display: flex; justify-content: space-between; align-items: baseline;">
              <span class="mono" style="font-size: 16px; font-weight: 800; color: var(--primary);">${formatMoney(goal.savedAmount)}</span>
              <span style="font-size: 12px; font-weight: 800; color: ${isDone ? "var(--sky-positive)" : "var(--text-muted)"};">${pct}%</span>
            </div>
            <div style="font-size: 11.5px; color: var(--text-muted); margin: 2px 0 6px;">
              Target: ${formatMoney(goal.targetAmount)} • ${isDone ? "🎉 Target Tercapai!" : `Kurang ${formatMoney(rem)} lagi`}
            </div>
            <div class="mini-progress-track" style="height: 8px;">
              <div class="mini-progress-fill" style="width: ${pct}%; background: ${isDone ? "var(--sky-positive)" : "var(--primary)"};"></div>
            </div>
          </div>

          <div class="savings-goal-actions">
            <button type="button" class="btn-tonal-apk" data-goal-tx="${escapeHtml(goal.id)}" data-tx-deposit="true">＋ Setor</button>
            <button type="button" class="btn-outline-apk" data-goal-tx="${escapeHtml(goal.id)}" data-tx-deposit="false" ${goal.savedAmount <= 0 ? "disabled" : ""}>－ Tarik</button>
          </div>
        </div>
      `;
    })
    .join("");
}

/* TAB 4: ANGGARAN (AutoBudgetScreen.kt) */
function renderAutoBudgetTab() {
  const incomeInput = document.getElementById("budgetMonthlyIncomeInput");
  if (document.activeElement !== incomeInput) {
    incomeInput.value = Math.round(state.budgetDraftIncome);
  }

  // Highlight active income preset chip
  document.querySelectorAll("[data-income-preset]").forEach((btn) => {
    const val = Number(btn.getAttribute("data-income-preset"));
    btn.classList.toggle("active", Math.abs(state.budgetDraftIncome - val) < 1);
  });

  // Highlight active strategy card
  document.querySelectorAll("[data-strategy]").forEach((card) => {
    const st = card.getAttribute("data-strategy");
    const isSel = st === state.budgetDraftStrategy;
    card.classList.toggle("selected", isSel);
    const radio = card.querySelector('input[type="radio"]');
    if (radio) radio.checked = isSel;
  });

  const savingsPct = state.budgetDraftStrategy === "FRUGAL_60_10_30" ? 30 : 20;
  const savingsAmt = (state.budgetDraftIncome * savingsPct) / 100;
  const totalCategoryAllocated = Object.values(state.budgetDraftCategoryLimits).reduce(
    (s, v) => s + (Number(v) || 0),
    0
  );
  const daysInMonth = getDaysInYearMonth(state.selectedYearMonth);
  const dailySafe = daysInMonth > 0 ? totalCategoryAllocated / daysInMonth : 0;

  document.getElementById("budgetPreviewSpendableVal").textContent = formatMoney(totalCategoryAllocated);
  document.getElementById("budgetPreviewSavingsVal").textContent = formatMoney(savingsAmt);
  document.getElementById("budgetPreviewDailySafeText").textContent =
    `Estimasi Batas Harian: ${formatMoney(dailySafe)} / hari (${daysInMonth} hari)`;

  // 3-Segment Stacked Bar
  const needsSum = Object.values(CATEGORIES)
    .filter((c) => c.isNeed)
    .reduce((s, c) => s + (Number(state.budgetDraftCategoryLimits[c.key]) || 0), 0);
  const wantsSum = Object.values(CATEGORIES)
    .filter((c) => !c.isNeed)
    .reduce((s, c) => s + (Number(state.budgetDraftCategoryLimits[c.key]) || 0), 0);
  const grandTotal = Math.max(1, needsSum + wantsSum + savingsAmt);

  document.getElementById("segBarNeeds").style.width = `${Math.round((needsSum / grandTotal) * 100)}%`;
  document.getElementById("segBarWants").style.width = `${Math.round((wantsSum / grandTotal) * 100)}%`;
  document.getElementById("segBarSavings").style.width = `${Math.round((savingsAmt / grandTotal) * 100)}%`;

  // Category Budget Adjust Cards
  const adjustList = document.getElementById("budgetCategoryAdjustList");
  adjustList.innerHTML = Object.values(CATEGORIES)
    .map((cat) => {
      const amt = Math.round(Number(state.budgetDraftCategoryLimits[cat.key]) || 0);
      return `
        <div class="sculpted-card">
          <div style="display: flex; align-items: center; gap: 11px;">
            <div class="cat-icon-badge" style="background: ${cat.bgSoft}; color: ${cat.color};">${cat.icon}</div>
            <div style="flex: 1;">
              <div style="font-size: 13.5px; font-weight: 800;">${escapeHtml(cat.labelId)}</div>
              <div style="font-size: 11px; color: var(--text-muted);">${cat.isNeed ? "Kebutuhan Pokok" : "Gaya Hidup"}</div>
            </div>
            <strong class="mono" style="font-size: 14px; color: var(--primary);">${formatMoney(amt)}</strong>
          </div>
          <div class="budget-adjust-controls">
            <input type="number" min="0" step="10000" value="${amt}" data-cat-limit-input="${cat.key}" />
            <button type="button" class="btn-step-50k" data-cat-step="${cat.key}" data-step-delta="-50000">- 50rb</button>
            <button type="button" class="btn-step-50k plus" data-cat-step="${cat.key}" data-step-delta="50000">+ 50rb</button>
          </div>
        </div>
      `;
    })
    .join("");
}

/* --------------------------------------------------------------------------
   11. Smart AI Features: Monthly Financial Advisor & Quick Expense Parser
   -------------------------------------------------------------------------- */
function generateMonthlyAiInsight() {
  const monthExps = state.expenses.filter((e) => e.yearMonth === state.selectedYearMonth);
  const totalSpent = monthExps.reduce((s, e) => s + e.amount, 0);
  const budget = state.budgetsByMonth[state.selectedYearMonth];
  const totalBudget = budget
    ? Object.values(budget.categoryAllocations || {}).reduce((s, v) => s + (Number(v) || 0), 0)
    : 0;

  // Find top category
  const byCat = {};
  monthExps.forEach((e) => {
    byCat[e.category] = (byCat[e.category] || 0) + e.amount;
  });
  let topCatKey = "FOOD_DINING";
  let topCatVal = 0;
  Object.entries(byCat).forEach(([k, v]) => {
    if (v > topCatVal) {
      topCatVal = v;
      topCatKey = k;
    }
  });
  const topCatLabel = CATEGORIES[topCatKey]?.labelId || "Makan & Minum";

  let status = "AMAN";
  let statusColor = "var(--emerald-safe)";
  let headline = "";
  let tips = [];

  if (monthExps.length === 0) {
    headline = `Belum ada transaksi tercatat di ${formatMonthDisplay(state.selectedYearMonth)}. Mulailah mencatat pengeluaran harian agar AI dapat menganalisis kebiasaan belanja Anda.`;
    tips = [
      "Susun anggaran otomatis 50/30/20 di tab Anggaran.",
      "Gunakan fitur Isi Cepat dengan AI saat mencatat pengeluaran harian.",
      "Simpan tagihan bulanan tetap di bagian Pengeluaran Rutin."
    ];
  } else if (totalBudget > 0 && totalSpent > totalBudget) {
    status = "BOROS";
    statusColor = "var(--coral-danger)";
    headline = `Pengeluaran bulan ini (${formatMoney(totalSpent)}) telah melampaui anggaran belanja (${formatMoney(totalBudget)}). Kategori terbesar ada pada ${topCatLabel} (${formatMoney(topCatVal)}).`;
    tips = [
      `Tahan pengeluaran non-esensial pada kategori ${topCatLabel} selama sisa minggu ini.`,
      "Gunakan metode pembayaran dengan anggaran terbatas (seperti saldo QRIS harian).",
      "Evaluasi kembali alokasi kategori di tab Anggaran agar tetap menjaga target tabungan 20%."
    ];
  } else if (totalBudget > 0 && totalSpent > totalBudget * 0.78) {
    status = "PERHATIAN";
    statusColor = "var(--gold-accent)";
    headline = `Anda telah menggunakan ${Math.round((totalSpent / totalBudget) * 100)}% dari anggaran bulan ini. Pengeluaran tertinggi tercatat pada ${topCatLabel} sebesar ${formatMoney(topCatVal)}.`;
    tips = [
      `Batasi pengeluaran harian agar tidak melewati batas aman harian.`,
      `Kurangi frekuensi belanja impulsif di kategori ${topCatLabel}.`,
      "Sisihkan sisa saldo harian langsung ke Celengan Impian."
    ];
  } else {
    headline = `Keuangan Anda di ${formatMonthDisplay(state.selectedYearMonth)} sangat sehat dan terkendali! Total pengeluaran ${formatMoney(totalSpent)} masih berada di jalur aman.`;
    tips = [
      `Pertahankan ritme pengeluaran pada kategori ${topCatLabel} (${formatMoney(topCatVal)}).`,
      "Alokasikan surplus anggaran ke Celengan Dana Darurat di tab Tabungan.",
      "Lampirkan foto struk pada transaksi penting untuk arsip laporan PDF/Excel bulanan."
    ];
  }

  const bodyEl = document.getElementById("aiInsightBody");
  bodyEl.innerHTML = `
    <div class="ai-result-panel">
      <div style="display: flex; align-items: center; justify-content: space-between;">
        <span style="padding: 3px 9px; border-radius: 999px; font-size: 11px; font-weight: 800; background: rgba(16,185,129,0.14); color: ${statusColor};">
          Status: ${status}
        </span>
        <span style="font-size: 11.5px; font-weight: 700; color: var(--primary);">Terbesar: ${escapeHtml(topCatLabel)}</span>
      </div>
      <p style="font-size: 12.5px; line-height: 1.5; font-weight: 600;">${escapeHtml(headline)}</p>
      <div style="height: 1px; background: var(--outline-soft); margin: 2px 0;"></div>
      ${tips
        .map(
          (t) => `
          <div class="ai-tip-row">
            <span style="color: var(--primary); font-weight: 800;">✓</span>
            <span>${escapeHtml(t)}</span>
          </div>
        `
        )
        .join("")}
      <button type="button" id="btnRefreshAiInsight" class="section-link-btn" style="align-self: flex-end; margin-top: 2px;">
        🔄 Perbarui Analisis
      </button>
    </div>
  `;

  document.getElementById("btnRefreshAiInsight")?.addEventListener("click", generateMonthlyAiInsight);
}

function parseQuickExpenseSentence(rawText) {
  const text = String(rawText || "").trim();
  if (!text) return false;

  // Parse amount like "28rb", "28k", "1.5jt", "25000", "25.000"
  let amount = 0;
  const rbMatch = text.match(/(\d+(?:[.,]\d+)?)\s*(rb|ribu|k)\b/i);
  const jtMatch = text.match(/(\d+(?:[.,]\d+)?)\s*(jt|juta)\b/i);
  const plainMatch = text.replace(/\./g, "").match(/\b(\d{4,9})\b/);

  if (jtMatch) {
    amount = parseFloat(jtMatch[1].replace(",", ".")) * 1000000;
  } else if (rbMatch) {
    amount = parseFloat(rbMatch[1].replace(",", ".")) * 1000;
  } else if (plainMatch) {
    amount = parseInt(plainMatch[1], 10);
  }

  const lower = text.toLowerCase();
  let category = "FOOD_DINING";
  if (/bensin|ojek|grab|gojek|parkir|tol|kereta|bus|transport/.test(lower)) {
    category = "TRANSPORTATION";
  } else if (/listrik|air|wifi|internet|pulsa|token|kost|sewa|tagihan/.test(lower)) {
    category = "BILLS_UTILITIES";
  } else if (/belanja|indomaret|alfamart|supermarket|baju|sepatu|shopee|tokopedia/.test(lower)) {
    category = "SHOPPING";
  } else if (/nonton|bioskop|netflix|spotify|game|hiburan|wisata/.test(lower)) {
    category = "ENTERTAINMENT";
  } else if (/obat|dokter|apotek|vitamin|skincare|gym|sehat/.test(lower)) {
    category = "HEALTH_PERSONAL";
  }

  let payment = "QRIS";
  if (/tunai|cash/.test(lower)) payment = "CASH";
  else if (/transfer|tf|bank|bca|mandiri|bri/.test(lower)) payment = "TRANSFER";
  else if (/kartu|debit|kredit|card/.test(lower)) payment = "CARD";

  // Clean title
  const cleanedTitle = text
    .replace(/(\d+(?:[.,]\d+)?)\s*(rb|ribu|k|jt|juta)\b/gi, "")
    .replace(/\b(bayar|pakai|via|dengan|qris|tunai|cash|transfer|kartu)\b/gi, "")
    .replace(/\s+/g, " ")
    .trim();

  if (cleanedTitle) {
    document.getElementById("expenseTitleInput").value =
      cleanedTitle.charAt(0).toUpperCase() + cleanedTitle.slice(1);
  }
  if (amount > 0) {
    document.getElementById("expenseAmountInput").value = Math.round(amount);
  }
  document.getElementById("expenseCategorySelect").value = category;
  document.getElementById("expensePaymentSelect").value = payment;
  showToast("✨ Kolom berhasil diisi otomatis!");
  return true;
}

/* --------------------------------------------------------------------------
   12. Export Report Engine (Excel .xls with Receipts, PDF A4 Print, CSV, Share)
   -------------------------------------------------------------------------- */
function getExportTargetExpenses() {
  return state.expenses.filter((e) => e.yearMonth === state.selectedYearMonth);
}

function openExportModal() {
  const exps = getExportTargetExpenses();
  const total = exps.reduce((s, e) => s + e.amount, 0);
  const recCount = exps.filter((e) => Boolean(e.receiptBase64)).length;

  document.getElementById("exportModalSubtitle").textContent = `Periode ${formatMonthDisplay(state.selectedYearMonth)}`;
  document.getElementById("exportModalCountText").textContent = `${exps.length} transaksi tercatat`;
  document.getElementById("exportModalReceiptText").textContent =
    recCount > 0 ? `${recCount} foto struk disertakan otomatis` : "Tanpa lampiran foto struk";
  document.getElementById("exportModalTotalVal").textContent = formatMoney(total);

  document.getElementById("exportReportModal").classList.remove("hidden");
}

function exportToExcelFile() {
  const exps = getExportTargetExpenses();
  const total = exps.reduce((s, e) => s + e.amount, 0);
  const monthLabel = formatMonthDisplay(state.selectedYearMonth);
  const receipts = exps.filter((e) => Boolean(e.receiptBase64));

  const rowsHtml = exps
    .map((e, idx) => {
      const cat = CATEGORIES[e.category]?.labelId || e.category;
      const pm = PAYMENT_METHODS[e.paymentMethod]?.label || e.paymentMethod;
      return `
        <tr>
          <td style="padding:6px;border:1px solid #CBD5E1;text-align:center;">${idx + 1}</td>
          <td style="padding:6px;border:1px solid #CBD5E1;">${escapeHtml(e.date)}</td>
          <td style="padding:6px;border:1px solid #CBD5E1;font-weight:bold;">${escapeHtml(e.title)}</td>
          <td style="padding:6px;border:1px solid #CBD5E1;">${escapeHtml(cat)}</td>
          <td style="padding:6px;border:1px solid #CBD5E1;">${escapeHtml(pm)}</td>
          <td style="padding:6px;border:1px solid #CBD5E1;text-align:right;font-weight:bold;">${Math.round(e.amount)}</td>
          <td style="padding:6px;border:1px solid #CBD5E1;">${escapeHtml(e.note || "-")}</td>
          <td style="padding:6px;border:1px solid #CBD5E1;text-align:center;">${e.receiptBase64 ? "Ada Foto Struk" : "-"}</td>
        </tr>
      `;
    })
    .join("");

  const htmlDoc = `
    <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
    <head><meta charset="UTF-8" /></head>
    <body style="font-family: Calibri, Arial, sans-serif;">
      <h2 style="color:#1E3A8A;margin-bottom:4px;">LAPORAN PENGELUARAN SAKUPINTAR — ${escapeHtml(monthLabel.toUpperCase())}</h2>
      <p style="color:#475569;margin-top:0;">Total Pengeluaran: <strong>${formatMoney(total)}</strong> (${exps.length} Transaksi • ${receipts.length} Foto Struk)</p>
      <table style="border-collapse:collapse;width:100%;">
        <thead>
          <tr style="background:#1E3A8A;color:#FFFFFF;">
            <th style="padding:8px;border:1px solid #1E3A8A;">No</th>
            <th style="padding:8px;border:1px solid #1E3A8A;">Tanggal</th>
            <th style="padding:8px;border:1px solid #1E3A8A;">Nama Pengeluaran</th>
            <th style="padding:8px;border:1px solid #1E3A8A;">Kategori</th>
            <th style="padding:8px;border:1px solid #1E3A8A;">Metode Bayar</th>
            <th style="padding:8px;border:1px solid #1E3A8A;">Nominal</th>
            <th style="padding:8px;border:1px solid #1E3A8A;">Catatan</th>
            <th style="padding:8px;border:1px solid #1E3A8A;">Lampiran Struk</th>
          </tr>
        </thead>
        <tbody>${rowsHtml}</tbody>
      </table>
    </body>
    </html>
  `;

  const blob = new Blob([htmlDoc], { type: "application/vnd.ms-excel;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `Rekap_SakuPintar_${state.selectedYearMonth}.xls`;
  a.click();
  URL.revokeObjectURL(url);
  showToast("📗 File Excel (.xls) berhasil diunduh!");
}

function exportToCsvFile() {
  const exps = getExportTargetExpenses();
  const header = ["No", "Tanggal", "Nama Pengeluaran", "Kategori", "Metode Pembayaran", "Nominal", "Catatan", "Foto Struk"];
  const lines = [header.join(",")];

  exps.forEach((e, i) => {
    const row = [
      i + 1,
      `"${e.date}"`,
      `"${(e.title || "").replace(/"/g, '""')}"`,
      `"${CATEGORIES[e.category]?.labelId || e.category}"`,
      `"${PAYMENT_METHODS[e.paymentMethod]?.label || e.paymentMethod}"`,
      Math.round(e.amount),
      `"${(e.note || "").replace(/"/g, '""')}"`,
      e.receiptBase64 ? `"Ada Struk"` : `"Tidak Ada"`
    ];
    lines.push(row.join(","));
  });

  const blob = new Blob(["\uFEFF" + lines.join("\n")], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `Rekap_SakuPintar_${state.selectedYearMonth}.csv`;
  a.click();
  URL.revokeObjectURL(url);
  showToast("📋 File CSV berhasil diunduh!");
}

function exportToPdfPrint() {
  const exps = getExportTargetExpenses();
  const total = exps.reduce((s, e) => s + e.amount, 0);
  const monthLabel = formatMonthDisplay(state.selectedYearMonth);
  const receipts = exps.filter((e) => Boolean(e.receiptBase64));

  const printArea = document.getElementById("printOnlyReportArea");
  printArea.innerHTML = `
    <div class="print-banner">
      <div style="font-size: 20px; font-weight: 800;">SakuPintar — Laporan Keuangan Bulanan</div>
      <div style="font-size: 13px; opacity: 0.9; margin-top: 4px;">
        Periode: ${escapeHtml(monthLabel)} • Total Pengeluaran: <strong>${formatMoney(total)}</strong> (${exps.length} Transaksi)
      </div>
    </div>

    <table class="print-table">
      <thead>
        <tr>
          <th>No</th>
          <th>Tanggal</th>
          <th>Nama Pengeluaran</th>
          <th>Kategori</th>
          <th>Metode</th>
          <th>Nominal</th>
          <th>Catatan</th>
        </tr>
      </thead>
      <tbody>
        ${exps
          .map(
            (e, idx) => `
          <tr>
            <td>${idx + 1}</td>
            <td>${escapeHtml(e.date)}</td>
            <td><strong>${escapeHtml(e.title)}</strong></td>
            <td>${escapeHtml(CATEGORIES[e.category]?.labelId || e.category)}</td>
            <td>${escapeHtml(PAYMENT_METHODS[e.paymentMethod]?.label || e.paymentMethod)}</td>
            <td style="font-family: monospace; font-weight: 700;">${formatMoney(e.amount)}</td>
            <td>${escapeHtml(e.note || "-")}</td>
          </tr>
        `
          )
          .join("")}
      </tbody>
    </table>

    ${
      receipts.length > 0
        ? `
        <h3 style="margin-top: 22px; font-size: 15px;">Lampiran Bukti Foto Struk (${receipts.length} Struk)</h3>
        <div class="print-receipt-grid">
          ${receipts
            .map(
              (r, idx) => `
            <div class="print-receipt-card">
              <div style="font-size: 12px; font-weight: 800;">#${idx + 1} — ${escapeHtml(r.title)} (${escapeHtml(r.date)})</div>
              <div style="font-size: 11.5px; color: #2563EB; font-weight: 700;">${formatMoney(r.amount)} • ${escapeHtml(PAYMENT_METHODS[r.paymentMethod]?.label || r.paymentMethod)}</div>
              <img src="${escapeHtml(r.receiptBase64)}" alt="Struk" />
            </div>
          `
            )
            .join("")}
        </div>
      `
        : ""
    }
  `;

  window.print();
}

async function shareOrCopySummaryText() {
  const exps = getExportTargetExpenses();
  const total = exps.reduce((s, e) => s + e.amount, 0);
  const text = [
    `📊 Rekap SakuPintar — ${formatMonthDisplay(state.selectedYearMonth)}`,
    `Total Pengeluaran: ${formatMoney(total)} (${exps.length} transaksi)`,
    ...exps.slice(0, 8).map((e) => `• ${e.date}: ${e.title} (${formatMoney(e.amount)})`)
  ].join("\n");

  if (navigator.share) {
    try {
      await navigator.share({ title: `Rekap SakuPintar ${formatMonthDisplay(state.selectedYearMonth)}`, text });
      return;
    } catch (_) {}
  }
  try {
    await navigator.clipboard.writeText(text);
    showToast("🔗 Ringkasan rekap berhasil disalin ke clipboard!");
  } catch (_) {
    showToast("Ringkasan siap dibagikan");
  }
}

/* --------------------------------------------------------------------------
   13. Live GitHub Releases APK Lookup
   -------------------------------------------------------------------------- */
async function fetchLatestApkReleaseInfo() {
  const apiUrl = window.SAKUPINTAR_FIREBASE_CONFIG?.releasesApiUrl;
  if (!apiUrl) return;
  try {
    const res = await fetch(apiUrl, { headers: { Accept: "application/vnd.github+json" } });
    if (!res.ok) return;
    const data = await res.json();
    const apkAsset = (data.assets || []).find((a) => a.name && a.name.endsWith(".apk"));
    const dlUrl = apkAsset?.browser_download_url || data.html_url;
    const tag = data.tag_name || "v2.0 APK";

    const badge = document.getElementById("landingApkVersionBadge");
    if (badge) badge.textContent = `${tag} APK`;

    ["landingApkDownloadBtn", "dashboardApkDownloadBtn", "modalApkDirectLink"].forEach((id) => {
      const a = document.getElementById(id);
      if (a && dlUrl) a.href = dlUrl;
    });
  } catch (_) {}
}

/* --------------------------------------------------------------------------
   14. Event Listeners & Interactive Wiring
   -------------------------------------------------------------------------- */
function bindAllEvents() {
  // Theme & Language toggles
  const toggleThemeHandler = () => applyTheme(state.theme === "dark" ? "light" : "dark");
  document.getElementById("authThemeToggleBtn")?.addEventListener("click", toggleThemeHandler);
  document.getElementById("drawerThemeToggleBtn")?.addEventListener("click", toggleThemeHandler);

  document.getElementById("authLangToggleBtn")?.addEventListener("click", () => {
    applyLanguage(state.lang === "id" ? "en" : "id");
  });
  document.getElementById("drawerLanguageSelect")?.addEventListener("change", (e) => {
    applyLanguage(e.target.value);
  });
  document.getElementById("drawerCurrencySelect")?.addEventListener("change", (e) => {
    state.currency = e.target.value;
    localStorage.setItem("sakupintar_currency", state.currency);
    renderAll();
  });

  // Auth Flow Step Transitions (LANDING <-> METHOD_SELECTION)
  document.getElementById("btnGoToMethodSelection")?.addEventListener("click", () => {
    setAuthStep("METHOD_SELECTION");
  });
  document.getElementById("btnBackToLanding")?.addEventListener("click", () => {
    setAuthStep("LANDING");
  });
  document.getElementById("tabModeSignIn")?.addEventListener("click", () => setAuthMode("SIGN_IN"));
  document.getElementById("tabModeSignUp")?.addEventListener("click", () => setAuthMode("SIGN_UP"));

  // Google Sign-In & Guest Sign-In
  document.getElementById("btnGoogleSignIn")?.addEventListener("click", async () => {
    const errBanner = document.getElementById("authErrorBanner");
    errBanner.classList.add("hidden");

    if (!initFirebaseConnection() || !firebaseAuth) {
      document.getElementById("cloudConfigModal").classList.remove("hidden");
      return;
    }
    try {
      const provider = new GoogleAuthProvider();
      provider.setCustomParameters({ prompt: "select_account" });
      await signInWithPopup(firebaseAuth, provider);
    } catch (err) {
      errBanner.textContent = `Gagal masuk dengan Google: ${err.message || "Pastikan domain web ini sudah terdaftar di Authorized Domains Firebase Console."}`;
      errBanner.classList.remove("hidden");
    }
  });

  document.getElementById("btnGuestSignIn")?.addEventListener("click", () => {
    enterGuestSession(true);
  });

  // Navigation Tabs (NavigationRail + Bottom NavigationBar)
  document.querySelectorAll("[data-nav-tab]").forEach((btn) => {
    btn.addEventListener("click", () => {
      setActiveTab(btn.getAttribute("data-nav-tab"));
    });
  });

  // Quick Navigation Buttons inside Beranda
  document.getElementById("btnHeroGoToAutoBudget")?.addEventListener("click", () => setActiveTab("budget"));
  document.getElementById("bentoGoToSavings")?.addEventListener("click", () => setActiveTab("savings"));
  document.getElementById("bentoOpenExportSheet")?.addEventListener("click", openExportModal);
  document.getElementById("btnDashboardAdjustBudget")?.addEventListener("click", () => setActiveTab("budget"));
  document.getElementById("btnDashboardSeeAllExpenses")?.addEventListener("click", () => setActiveTab("expenses"));
  document.getElementById("btnGenerateAiInsight")?.addEventListener("click", generateMonthlyAiInsight);
  document.getElementById("btnOpenApkGuide")?.addEventListener("click", () => {
    document.getElementById("apkGuideModal").classList.remove("hidden");
  });

  // Month Stepper & MonthYearPicker Dialog
  document.querySelectorAll("[data-month-step]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const step = Number(btn.getAttribute("data-month-step"));
      const [y, m] = state.selectedYearMonth.split("-").map(Number);
      const d = new Date(y, m - 1 + step, 1);
      state.selectedYearMonth = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
      state.selectedCalendarDate = null;
      syncBudgetDraftFromSelectedMonth();
      renderAll();
    });
  });

  document.querySelectorAll("[data-open-month-picker]").forEach((btn) => {
    btn.addEventListener("click", () => {
      state.pickerYear = Number(state.selectedYearMonth.split("-")[0]);
      renderMonthYearPickerGrid();
      document.getElementById("monthYearPickerModal").classList.remove("hidden");
    });
  });

  document.getElementById("pickerYearPrevBtn")?.addEventListener("click", () => {
    state.pickerYear -= 1;
    renderMonthYearPickerGrid();
  });
  document.getElementById("pickerYearNextBtn")?.addEventListener("click", () => {
    state.pickerYear += 1;
    renderMonthYearPickerGrid();
  });
  document.getElementById("btnJumpToCurrentMonth")?.addEventListener("click", () => {
    state.selectedYearMonth = currentMonthKeyDefault;
    state.selectedCalendarDate = null;
    syncBudgetDraftFromSelectedMonth();
    document.getElementById("monthYearPickerModal").classList.add("hidden");
    renderAll();
  });

  // Extended FAB (+ Catat) & Expense Editor Modal
  document.getElementById("fabAddExpense")?.addEventListener("click", () => {
    openExpenseEditorModal(state.selectedCalendarDate || getTodayIsoDate());
  });

  document.getElementById("btnRunAiQuickParse")?.addEventListener("click", () => {
    const val = document.getElementById("aiQuickParseInput").value;
    parseQuickExpenseSentence(val);
  });

  document.querySelectorAll("[data-quick-preset]").forEach((chip) => {
    chip.addEventListener("click", () => {
      const [title, amt, cat, pm] = chip.getAttribute("data-quick-preset").split("|");
      document.getElementById("expenseTitleInput").value = title;
      document.getElementById("expenseAmountInput").value = amt;
      document.getElementById("expenseCategorySelect").value = cat;
      document.getElementById("expensePaymentSelect").value = pm;
    });
  });

  const handleReceiptSelection = async (file) => {
    if (!file) return;
    try {
      const b64 = await compressImageFileToBase64(file, 720, 0.72);
      state.pendingReceiptBase64 = b64;
      document.getElementById("expenseReceiptPreviewImg").src = b64;
      document.getElementById("expenseReceiptPreviewWrap").classList.remove("hidden");
      document.getElementById("btnRemoveAttachedReceipt").classList.remove("hidden");
      showToast("📷 Foto struk berhasil dilampirkan!");
    } catch (_) {
      showToast("Gagal memproses foto struk");
    }
  };

  document.getElementById("expenseReceiptFileInput")?.addEventListener("change", (e) => {
    handleReceiptSelection(e.target.files?.[0]);
  });
  document.getElementById("aiReceiptQuickScanInput")?.addEventListener("change", (e) => {
    handleReceiptSelection(e.target.files?.[0]);
  });
  document.getElementById("btnRemoveAttachedReceipt")?.addEventListener("click", () => {
    state.pendingReceiptBase64 = "";
    document.getElementById("expenseReceiptPreviewWrap").classList.add("hidden");
    document.getElementById("btnRemoveAttachedReceipt").classList.add("hidden");
  });

  document.getElementById("expenseEditorForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    await saveExpenseRecord({
      title: document.getElementById("expenseTitleInput").value,
      amount: Number(document.getElementById("expenseAmountInput").value),
      date: document.getElementById("expenseDateInput").value,
      category: document.getElementById("expenseCategorySelect").value,
      paymentMethod: document.getElementById("expensePaymentSelect").value,
      note: document.getElementById("expenseNoteInput").value,
      receiptBase64: state.pendingReceiptBase64 || null
    });
    document.getElementById("expenseEditorModal").classList.add("hidden");
    showToast("✅ Pengeluaran berhasil disimpan!");
  });

  // Harian Tab Search, Category Filter, Calendar & Recurring Events
  const searchInput = document.getElementById("expenseSearchInput");
  const clearSearchBtn = document.getElementById("btnClearExpenseSearch");
  searchInput?.addEventListener("input", () => {
    state.searchQuery = searchInput.value;
    clearSearchBtn.classList.toggle("hidden", !state.searchQuery);
    renderDailyExpensesTab();
  });
  clearSearchBtn?.addEventListener("click", () => {
    state.searchQuery = "";
    searchInput.value = "";
    clearSearchBtn.classList.add("hidden");
    renderDailyExpensesTab();
  });

  document.getElementById("btnToggleCalendarExpand")?.addEventListener("click", () => {
    state.isCalendarExpanded = !state.isCalendarExpanded;
    renderDailyExpensesTab();
  });
  document.getElementById("btnClearSelectedDate")?.addEventListener("click", () => {
    state.selectedCalendarDate = null;
    renderDailyExpensesTab();
  });
  document.getElementById("btnAddExpenseOnSelectedDate")?.addEventListener("click", () => {
    openExpenseEditorModal(state.selectedCalendarDate || getTodayIsoDate());
  });

  document.getElementById("btnExpensesOpenExport")?.addEventListener("click", openExportModal);
  document.getElementById("btnExpensesShareSummary")?.addEventListener("click", shareOrCopySummaryText);

  document.getElementById("btnOpenAddRecurringModal")?.addEventListener("click", () => {
    document.getElementById("addRecurringForm").reset();
    document.getElementById("addRecurringModal").classList.remove("hidden");
  });
  document.getElementById("addRecurringForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    await saveRecurringTemplateRecord({
      title: document.getElementById("recurringTitleInput").value,
      amount: Number(document.getElementById("recurringAmountInput").value),
      category: document.getElementById("recurringCategorySelect").value,
      paymentMethod: document.getElementById("recurringPaymentSelect").value
    });
    document.getElementById("addRecurringModal").classList.add("hidden");
  });

  // Tabungan Tab Events
  document.getElementById("btnOpenCreateSavingsGoal")?.addEventListener("click", () => {
    document.getElementById("createSavingsGoalForm").reset();
    document.getElementById("createSavingsGoalModal").classList.remove("hidden");
  });
  document.getElementById("createSavingsGoalForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    await saveSavingsGoalRecord({
      title: document.getElementById("goalTitleInput").value,
      category: document.getElementById("goalCategorySelect").value,
      targetAmount: Number(document.getElementById("goalTargetAmountInput").value),
      savedAmount: Number(document.getElementById("goalInitialSavedInput").value || 0),
      note: document.getElementById("goalNoteInput").value
    });
    document.getElementById("createSavingsGoalModal").classList.add("hidden");
    showToast("🐷 Target celengan berhasil dibuat!");
  });

  document.querySelectorAll("[data-savings-quick]").forEach((chip) => {
    chip.addEventListener("click", () => {
      document.getElementById("savingsTxAmountInput").value = chip.getAttribute("data-savings-quick");
    });
  });

  document.getElementById("savingsTxForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const goal = state.savingsGoals.find((g) => g.id === state.activeSavingsTxGoalId);
    if (!goal) return;
    const delta = Math.max(0, Number(document.getElementById("savingsTxAmountInput").value) || 0);
    const updatedSaved = state.activeSavingsTxIsDeposit
      ? goal.savedAmount + delta
      : Math.max(0, goal.savedAmount - delta);
    await saveSavingsGoalRecord({ ...goal, savedAmount: updatedSaved });
    document.getElementById("savingsTxModal").classList.add("hidden");
    showToast(state.activeSavingsTxIsDeposit ? "Dana berhasil disetor ke celengan!" : "Dana ditarik dari celengan");
  });

  document.getElementById("walletBalanceForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const val = Number(document.getElementById("walletBalanceAmountInput").value) || 0;
    await saveWalletBalanceRecord(state.activeWalletMethodKey, val);
    document.getElementById("walletBalanceModal").classList.add("hidden");
  });

  // Anggaran Tab Events
  document.getElementById("budgetMonthlyIncomeInput")?.addEventListener("input", (e) => {
    state.budgetDraftIncome = Math.max(0, Number(e.target.value) || 0);
    const monthExps = state.expenses.filter((x) => x.yearMonth === state.selectedYearMonth);
    state.budgetDraftCategoryLimits = computeAutoBudgetAllocations(
      state.budgetDraftIncome,
      state.budgetDraftStrategy,
      monthExps
    ).allocations;
    renderAutoBudgetTab();
  });

  document.querySelectorAll("[data-income-preset]").forEach((chip) => {
    chip.addEventListener("click", () => {
      state.budgetDraftIncome = Number(chip.getAttribute("data-income-preset"));
      const monthExps = state.expenses.filter((x) => x.yearMonth === state.selectedYearMonth);
      state.budgetDraftCategoryLimits = computeAutoBudgetAllocations(
        state.budgetDraftIncome,
        state.budgetDraftStrategy,
        monthExps
      ).allocations;
      renderAutoBudgetTab();
    });
  });

  document.querySelectorAll("[data-strategy]").forEach((card) => {
    card.addEventListener("click", () => {
      state.budgetDraftStrategy = card.getAttribute("data-strategy");
      const monthExps = state.expenses.filter((x) => x.yearMonth === state.selectedYearMonth);
      state.budgetDraftCategoryLimits = computeAutoBudgetAllocations(
        state.budgetDraftIncome,
        state.budgetDraftStrategy,
        monthExps
      ).allocations;
      renderAutoBudgetTab();
    });
  });

  document.getElementById("btnResetCategoryAllocations")?.addEventListener("click", () => {
    const monthExps = state.expenses.filter((x) => x.yearMonth === state.selectedYearMonth);
    state.budgetDraftCategoryLimits = computeAutoBudgetAllocations(
      state.budgetDraftIncome,
      state.budgetDraftStrategy,
      monthExps
    ).allocations;
    renderAutoBudgetTab();
    showToast("🔄 Alokasi kategori direset ke perhitungan otomatis");
  });

  document.getElementById("btnSaveMonthlyBudget")?.addEventListener("click", async () => {
    const savingsTargetPercent = state.budgetDraftStrategy === "FRUGAL_60_10_30" ? 30 : 20;
    await saveMonthlyBudgetRecord({
      yearMonth: state.selectedYearMonth,
      monthlyIncome: state.budgetDraftIncome,
      savingsTargetPercent,
      strategy: state.budgetDraftStrategy,
      categoryAllocations: state.budgetDraftCategoryLimits
    });
    setActiveTab("dashboard");
  });

  // Profile Drawer & Edit Profile
  document.getElementById("btnOpenProfileDrawer")?.addEventListener("click", () => {
    document.getElementById("profileDisplayNameInput").value = state.user?.displayName || "";
    document.getElementById("profileDrawerModal").classList.remove("hidden");
  });

  document.getElementById("btnToggleEditProfile")?.addEventListener("click", () => {
    const panel = document.getElementById("editProfilePanel");
    panel.classList.toggle("hidden");
  });

  document.getElementById("profilePhotoInput")?.addEventListener("change", async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const b64 = await compressImageFileToBase64(file, 256, 0.78);
    state.customAvatarBase64 = b64;
    localStorage.setItem("sakupintar_custom_avatar", b64);
    renderTopBarAndDrawerProfile();
    showToast("Foto profil siap disimpan");
  });

  document.getElementById("btnSyncGoogleAvatar")?.addEventListener("click", () => {
    state.customAvatarBase64 = "";
    localStorage.removeItem("sakupintar_custom_avatar");
    renderTopBarAndDrawerProfile();
    showToast("Menggunakan foto profil Google");
  });

  document.getElementById("btnSaveProfileChanges")?.addEventListener("click", async () => {
    const newName = document.getElementById("profileDisplayNameInput").value.trim() || "Sobat SakuPintar";
    if (!state.isGuest && state.user && firebaseAuth?.currentUser) {
      try {
        await updateProfile(firebaseAuth.currentUser, { displayName: newName });
        state.user.displayName = newName;
        await syncUserProfileDoc(firebaseAuth.currentUser);
      } catch (_) {}
    } else {
      localStorage.setItem("sakupintar_guest_name", newName);
      if (state.user) state.user.displayName = newName;
    }
    renderTopBarAndDrawerProfile();
    document.getElementById("editProfilePanel").classList.add("hidden");
    showToast("✅ Profil berhasil diperbarui!");
  });

  document.getElementById("btnSignOutAccount")?.addEventListener("click", async () => {
    document.getElementById("profileDrawerModal").classList.add("hidden");
    localStorage.removeItem("sakupintar_session_mode");
    if (firebaseAuth && !state.isGuest) {
      try { await signOut(firebaseAuth); } catch (_) {}
    }
    state.isAuthenticated = false;
    state.isGuest = false;
    state.user = null;
    switchRootView(false);
  });

  // Cloud Config Modal
  ["btnOpenCloudConfigFromAuth", "btnOpenCloudConfigFromDrawer"].forEach((id) => {
    document.getElementById(id)?.addEventListener("click", () => {
      document.getElementById("profileDrawerModal")?.classList.add("hidden");
      document.getElementById("cloudConfigModal").classList.remove("hidden");
    });
  });

  document.getElementById("cloudConfigForm")?.addEventListener("submit", (e) => {
    e.preventDefault();
    const apiKey = document.getElementById("firebaseApiKeyInput").value.trim();
    const appId = document.getElementById("firebaseAppIdInput").value.trim();
    if (apiKey) localStorage.setItem("sakupintar_fb_api_key", apiKey);
    if (appId) localStorage.setItem("sakupintar_fb_app_id", appId);
    document.getElementById("cloudConfigModal").classList.add("hidden");
    showToast("Konfigurasi disimpan, memuat ulang koneksi...");
    setTimeout(() => window.location.reload(), 600);
  });

  // Export Format Actions
  document.getElementById("btnExportExcelFormat")?.addEventListener("click", exportToExcelFile);
  document.getElementById("btnExportPdfFormat")?.addEventListener("click", exportToPdfPrint);
  document.getElementById("btnExportCsvFormat")?.addEventListener("click", exportToCsvFile);
  document.getElementById("btnCopyShareSummaryText")?.addEventListener("click", shareOrCopySummaryText);

  // Delegated Clicks for Dynamic Cards & Modals
  document.addEventListener("click", async (e) => {
    // Close modal button
    const closeBtn = e.target.closest("[data-close-modal]");
    if (closeBtn) {
      const id = closeBtn.getAttribute("data-close-modal");
      document.getElementById(id)?.classList.add("hidden");
      return;
    }

    // Backdrop click to close
    if (e.target.classList.contains("modal-backdrop")) {
      e.target.classList.add("hidden");
      return;
    }

    // Category filter chip on Harian
    const catChip = e.target.closest("[data-cat-filter]");
    if (catChip) {
      state.selectedCategoryFilter = catChip.getAttribute("data-cat-filter");
      renderDailyExpensesTab();
      return;
    }

    // Calendar day cell click
    const dayCell = e.target.closest("[data-calendar-date]");
    if (dayCell) {
      const iso = dayCell.getAttribute("data-calendar-date");
      state.selectedCalendarDate = state.selectedCalendarDate === iso ? null : iso;
      renderDailyExpensesTab();
      return;
    }

    // Delete expense
    const delExpBtn = e.target.closest("[data-delete-expense]");
    if (delExpBtn) {
      const id = delExpBtn.getAttribute("data-delete-expense");
      await deleteExpenseRecord(id);
      return;
    }

    // View receipt photo
    const viewRecBtn = e.target.closest("[data-view-receipt]");
    if (viewRecBtn) {
      const id = viewRecBtn.getAttribute("data-view-receipt");
      const exp = state.expenses.find((x) => x.id === id);
      if (exp && exp.receiptBase64) {
        document.getElementById("receiptModalTitle").textContent = exp.title;
        document.getElementById("receiptModalSub").textContent = `${formatMoney(exp.amount)} • ${formatHumanDate(exp.date)}`;
        document.getElementById("receiptModalImg").src = exp.receiptBase64;
        document.getElementById("receiptPhotoModal").classList.remove("hidden");
      }
      return;
    }

    // Delete recurring template
    const delRecBtn = e.target.closest("[data-delete-recurring]");
    if (delRecBtn) {
      e.stopPropagation();
      await deleteRecurringTemplateRecord(delRecBtn.getAttribute("data-delete-recurring"));
      return;
    }

    // Record recurring template in 1 tap
    const recCard = e.target.closest("[data-record-recurring]");
    if (recCard) {
      const id = recCard.getAttribute("data-record-recurring");
      const tpl = state.recurringTemplates.find((t) => t.id === id);
      if (tpl) {
        await saveExpenseRecord({
          title: tpl.title,
          amount: tpl.amount,
          category: tpl.category,
          paymentMethod: tpl.paymentMethod,
          date: state.selectedCalendarDate || getTodayIsoDate(),
          note: "Pengeluaran rutin"
        });
        showToast(`✅ "${tpl.title}" dicatat ke pengeluaran hari ini!`);
      }
      return;
    }

    // Edit Wallet Balance
    const walletCard = e.target.closest("[data-edit-wallet]");
    if (walletCard) {
      const key = walletCard.getAttribute("data-edit-wallet");
      state.activeWalletMethodKey = key;
      document.getElementById("walletModalTitle").textContent = `Atur Saldo ${PAYMENT_METHODS[key]?.label || key}`;
      document.getElementById("walletBalanceAmountInput").value = Math.round(state.walletBalances[key] || 0);
      document.getElementById("walletBalanceModal").classList.remove("hidden");
      return;
    }

    // Savings Goal Deposit / Withdraw
    const goalTxBtn = e.target.closest("[data-goal-tx]");
    if (goalTxBtn) {
      const goalId = goalTxBtn.getAttribute("data-goal-tx");
      const isDeposit = goalTxBtn.getAttribute("data-tx-deposit") === "true";
      const goal = state.savingsGoals.find((g) => g.id === goalId);
      if (!goal) return;
      state.activeSavingsTxGoalId = goalId;
      state.activeSavingsTxIsDeposit = isDeposit;
      document.getElementById("savingsTxModalTitle").textContent =
        `${isDeposit ? "Setor ke" : "Tarik dari"} ${goal.title}`;
      document.getElementById("savingsTxModalSub").textContent =
        `Saldo terkumpul saat ini: ${formatMoney(goal.savedAmount)}`;
      document.getElementById("savingsTxAmountInput").value = "";
      document.getElementById("savingsTxModal").classList.remove("hidden");
      return;
    }

    // Delete Savings Goal
    const delGoalBtn = e.target.closest("[data-delete-goal]");
    if (delGoalBtn) {
      await deleteSavingsGoalRecord(delGoalBtn.getAttribute("data-delete-goal"));
      return;
    }

    // Category budget +/- 50k step buttons
    const stepBtn = e.target.closest("[data-cat-step]");
    if (stepBtn) {
      const catKey = stepBtn.getAttribute("data-cat-step");
      const delta = Number(stepBtn.getAttribute("data-step-delta"));
      const cur = Number(state.budgetDraftCategoryLimits[catKey] || 0);
      state.budgetDraftCategoryLimits[catKey] = Math.max(0, cur + delta);
      renderAutoBudgetTab();
      return;
    }

    // Month Picker cell click
    const monthCell = e.target.closest("[data-pick-month]");
    if (monthCell) {
      const m = Number(monthCell.getAttribute("data-pick-month"));
      state.selectedYearMonth = `${state.pickerYear}-${String(m).padStart(2, "0")}`;
      state.selectedCalendarDate = null;
      syncBudgetDraftFromSelectedMonth();
      document.getElementById("monthYearPickerModal").classList.add("hidden");
      renderAll();
    }
  });

  // Delegated input for manual category budget inputs
  document.addEventListener("input", (e) => {
    const catInput = e.target.closest("[data-cat-limit-input]");
    if (catInput) {
      const key = catInput.getAttribute("data-cat-limit-input");
      state.budgetDraftCategoryLimits[key] = Math.max(0, Number(catInput.value) || 0);
      // Update summary numbers without losing focus
      const savingsPct = state.budgetDraftStrategy === "FRUGAL_60_10_30" ? 30 : 20;
      const savingsAmt = (state.budgetDraftIncome * savingsPct) / 100;
      const totalAllocated = Object.values(state.budgetDraftCategoryLimits).reduce(
        (s, v) => s + (Number(v) || 0),
        0
      );
      const days = getDaysInYearMonth(state.selectedYearMonth);
      document.getElementById("budgetPreviewSpendableVal").textContent = formatMoney(totalAllocated);
      document.getElementById("budgetPreviewSavingsVal").textContent = formatMoney(savingsAmt);
      document.getElementById("budgetPreviewDailySafeText").textContent =
        `Estimasi Batas Harian: ${formatMoney(totalAllocated / days)} / hari (${days} hari)`;
    }
  });
}

function openExpenseEditorModal(defaultDate) {
  document.getElementById("expenseEditorForm").reset();
  document.getElementById("aiQuickParseInput").value = "";
  document.getElementById("expenseDateInput").value = defaultDate || getTodayIsoDate();
  state.pendingReceiptBase64 = "";
  document.getElementById("expenseReceiptPreviewWrap").classList.add("hidden");
  document.getElementById("btnRemoveAttachedReceipt").classList.add("hidden");
  document.getElementById("expenseEditorModal").classList.remove("hidden");
}

function renderMonthYearPickerGrid() {
  document.getElementById("pickerYearLabel").textContent = state.pickerYear;
  const [selY, selM] = state.selectedYearMonth.split("-").map(Number);
  const grid = document.getElementById("pickerMonthsGrid");
  grid.innerHTML = MONTHS_SHORT_ID.map((name, idx) => {
    const mNum = idx + 1;
    const isActive = state.pickerYear === selY && mNum === selM;
    return `
      <button type="button" class="month-cell-btn ${isActive ? "active" : ""}" data-pick-month="${mNum}">
        ${name}
      </button>
    `;
  }).join("");
}

/* --------------------------------------------------------------------------
   15. Bootstrap Application
   -------------------------------------------------------------------------- */
function bootstrap() {
  applyTheme(state.theme);
  const currSel = document.getElementById("drawerCurrencySelect");
  if (currSel) currSel.value = state.currency;
  const langSel = document.getElementById("drawerLanguageSelect");
  if (langSel) langSel.value = state.lang;

  syncBudgetDraftFromSelectedMonth();
  bindAllEvents();
  initFirebaseConnection();
  fetchLatestApkReleaseInfo();
}

bootstrap();
