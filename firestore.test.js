const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function serverTime() {
  return new Date(Date.now() - 1000);
}

function validExpensePayload(uid = ALICE_UID, overrides = {}) {
  return {
    userId: uid,
    title: "Makan Siang",
    amount: 35000,
    category: "FOOD",
    paymentMethod: "QRIS",
    dateEpochDay: 20770,
    monthKey: "2026-11",
    note: "Nasi Padang",
    createdAt: serverTime(),
    updatedAt: serverTime(),
    ...overrides,
  };
}

function validBudgetPayload(uid = ALICE_UID, monthKey = "2026-11", overrides = {}) {
  return {
    userId: uid,
    monthKey,
    monthlyIncome: 8000000,
    strategy: "BALANCED_50_30_20",
    savingsTarget: 1600000,
    needsPercent: 50,
    wantsPercent: 30,
    savingsPercent: 20,
    categoryBudgets: {
      FOOD: 1800000,
      TRANSPORT: 700000,
      BILLS: 1100000,
      HEALTH: 400000,
      SHOPPING: 1200000,
      ENTERTAINMENT: 800000,
      OTHER: 400000,
    },
    createdAt: serverTime(),
    updatedAt: serverTime(),
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write expenses or budgets", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("expenses").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp1").set(validExpensePayload()));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("budgets").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("budgets").doc("2026-11").set(validBudgetPayload()));
});

test("Authenticated owner can create, list, update, and delete own expenses", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const expRef = aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_1");
  await assertSucceeds(expRef.set(validExpensePayload(ALICE_UID, { receiptPhotoBase64: "dGVzdC1yZWNlaXB0" })));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("expenses").where("monthKey", "==", "2026-11").get());
  await assertSucceeds(expRef.update({ amount: 42000, title: "Makan Malam", receiptPhotoBase64: "", updatedAt: serverTime() }));
  await assertSucceeds(expRef.delete());
});

test("Cross-user access to expenses and budgets is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_1").set(validExpensePayload()));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("budgets").doc("2026-11").set(validBudgetPayload()));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("expenses").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_1").delete());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("budgets").doc("2026-11").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("budgets").doc("2026-11").set(validBudgetPayload(ALICE_UID)));
});

test("Expense schema validation blocks negative amount, invalid category, ghost fields, oversized receiptPhotoBase64, and userId spoofing", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("expenses");

  await assertFails(col.doc("e_neg").set(validExpensePayload(ALICE_UID, { amount: -5000 })));
  await assertFails(col.doc("e_zero").set(validExpensePayload(ALICE_UID, { amount: 0 })));
  await assertFails(col.doc("e_cat").set(validExpensePayload(ALICE_UID, { category: "CRYPTO" })));
  await assertFails(col.doc("e_ghost").set(validExpensePayload(ALICE_UID, { isAdmin: true })));
  await assertFails(col.doc("e_spoof").set(validExpensePayload(BOB_UID)));
  await assertFails(col.doc("e_month").set(validExpensePayload(ALICE_UID, { monthKey: "2026-13" })));
  await assertFails(col.doc("e_receipt_big").set(validExpensePayload(ALICE_UID, { receiptPhotoBase64: "x".repeat(200001) })));
});

test("Budget schema validation enforces valid monthKey, percent sum == 100, and safe categoryBudgets map", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("budgets");

  await assertSucceeds(col.doc("2026-11").set(validBudgetPayload()));
  await assertSucceeds(col.doc("2026-11").update({ monthlyIncome: 9000000, updatedAt: serverTime() }));

  await assertFails(col.doc("invalid-month").set(validBudgetPayload(ALICE_UID, "invalid-month")));
  await assertFails(col.doc("2026-12").set(validBudgetPayload(ALICE_UID, "2026-11")));
  await assertFails(col.doc("2026-12").set(validBudgetPayload(ALICE_UID, "2026-12", { needsPercent: 60 })));
  await assertFails(col.doc("2026-12").set(validBudgetPayload(ALICE_UID, "2026-12", {
    categoryBudgets: { FOOD: 100000, HACKED: 999999 },
  })));
});

test("Recurring templates enforce owner isolation and schema validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("recurringTemplates");

  const validTemplate = {
    userId: ALICE_UID,
    title: "Kos Bulanan",
    amount: 1500000,
    category: "BILLS",
    paymentMethod: "TRANSFER",
    note: "Jatuh tempo tgl 5",
    createdAt: serverTime(),
    updatedAt: serverTime(),
  };

  await assertSucceeds(col.doc("tpl_1").set(validTemplate));
  await assertSucceeds(col.get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("recurringTemplates").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("recurringTemplates").doc("tpl_1").delete());
  await assertFails(col.doc("tpl_bad").set({ ...validTemplate, amount: 0 }));
  await assertSucceeds(col.doc("tpl_1").delete());
});

test("User profile enforces owner isolation and displayName validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceProfile = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(aliceProfile.set({
    userId: ALICE_UID,
    displayName: "Rama Pribadi",
    createdAt: serverTime(),
    updatedAt: serverTime(),
  }));
  await assertSucceeds(aliceProfile.get());
  await assertSucceeds(aliceProfile.update({
    displayName: "Rama SakuPintar",
    photoBase64: "dGVzdA==",
    updatedAt: serverTime(),
  }));
  await assertSucceeds(aliceProfile.update({
    photoBase64: "",
    updatedAt: serverTime(),
  }));
  await assertFails(aliceProfile.update({
    photoBase64: "x".repeat(150001),
    updatedAt: serverTime(),
  }));
  await assertFails(aliceDb.collection("users").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({
    displayName: "Hacked",
    updatedAt: serverTime(),
  }));
  await assertFails(aliceProfile.update({
    displayName: "",
    updatedAt: serverTime(),
  }));
});

test("Savings goals enforce owner isolation, non-negative savedAmount, and schema validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const goalsCol = aliceDb.collection("users").doc(ALICE_UID).collection("savingsGoals");

  const validGoal = {
    userId: ALICE_UID,
    title: "Dana Darurat",
    targetAmount: 10000000,
    savedAmount: 1500000,
    category: "EMERGENCY",
    note: "Target 6 bulan",
    createdAt: serverTime(),
    updatedAt: serverTime(),
  };

  await assertSucceeds(goalsCol.doc("goal_1").set(validGoal));
  await assertSucceeds(goalsCol.get());
  await assertSucceeds(goalsCol.doc("goal_1").update({ savedAmount: 2000000, updatedAt: serverTime() }));
  await assertFails(goalsCol.doc("goal_1").update({ savedAmount: -100, updatedAt: serverTime() }));
  await assertFails(goalsCol.doc("goal_bad_cat").set({ ...validGoal, category: "CRYPTO" }));
  await assertFails(goalsCol.doc("goal_zero").set({ ...validGoal, targetAmount: 0 }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("savingsGoals").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("savingsGoals").doc("goal_1").delete());
  await assertSucceeds(goalsCol.doc("goal_1").delete());
});

test("Wallet balances enforce owner isolation and valid paymentMethod keys", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const walletCol = aliceDb.collection("users").doc(ALICE_UID).collection("walletBalances");

  const validWallet = {
    userId: ALICE_UID,
    paymentMethod: "QRIS",
    balance: 750000,
    createdAt: serverTime(),
    updatedAt: serverTime(),
  };

  await assertSucceeds(walletCol.doc("QRIS").set(validWallet));
  await assertSucceeds(walletCol.get());
  await assertSucceeds(walletCol.doc("QRIS").update({ balance: 900000, updatedAt: serverTime() }));
  await assertFails(walletCol.doc("QRIS").update({ balance: -1, updatedAt: serverTime() }));
  await assertFails(walletCol.doc("CASH").set({ ...validWallet, paymentMethod: "QRIS" }));
  await assertFails(walletCol.doc("BITCOIN").set({ ...validWallet, paymentMethod: "BITCOIN" }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("walletBalances").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("walletBalances").doc("QRIS").update({
    balance: 0,
    updatedAt: serverTime(),
  }));
});

test("Cashflow entries enforce owner isolation and schema validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("cashflowEntries");

  const validIncome = {
    userId: ALICE_UID,
    type: "INCOME",
    title: "Gaji Bulanan",
    amount: 7500000,
    targetWallet: "TRANSFER",
    dateEpochDay: 20770,
    monthKey: "2026-11",
    note: "Gaji November",
    createdAt: serverTime(),
    updatedAt: serverTime(),
  };

  await assertSucceeds(col.doc("cf_1").set(validIncome));
  await assertSucceeds(col.get());
  await assertSucceeds(col.doc("cf_2").set({ ...validIncome, type: "TRANSFER", sourceWallet: "TRANSFER", targetWallet: "QRIS" }));
  await assertFails(col.doc("cf_bad").set({ ...validIncome, amount: 0 }));
  await assertFails(col.doc("cf_bad_type").set({ ...validIncome, type: "UNKNOWN" }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cashflowEntries").get());
  await assertSucceeds(col.doc("cf_1").delete());
});

test("Custom categories enforce owner isolation and valid parentCategory", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("customCategories");

  const validCat = {
    userId: ALICE_UID,
    name: "Kopi",
    parentCategory: "FOOD",
    colorHex: 4279150057,
    createdAt: serverTime(),
    updatedAt: serverTime(),
  };

  await assertSucceeds(col.doc("cat_1").set(validCat));
  await assertSucceeds(col.get());
  await assertFails(col.doc("cat_bad").set({ ...validCat, parentCategory: "INVALID" }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("customCategories").get());
  await assertSucceeds(col.doc("cat_1").delete());
});

test("Savings history enforces owner isolation and immutability", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("savingsHistory");

  const validHist = {
    userId: ALICE_UID,
    goalId: "goal_1",
    type: "DEPOSIT",
    amount: 500000,
    balanceAfter: 1500000,
    timestampMillis: Date.now(),
    note: "Setoran gajian",
    createdAt: serverTime(),
    updatedAt: serverTime(),
  };

  await assertSucceeds(col.doc("sh_1").set(validHist));
  await assertSucceeds(col.get());
  await assertFails(col.doc("sh_1").update({ amount: 999999, updatedAt: serverTime() }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("savingsHistory").get());
  await assertSucceeds(col.doc("sh_1").delete());
});

test("Debt records enforce owner isolation, valid types, and settle updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("debtRecords");

  const validDebt = {
    userId: ALICE_UID,
    personName: "Budi",
    type: "OWED_TO_ME",
    amount: 150000,
    dueDateEpochDay: 20780,
    settled: false,
    note: "Patungan makan malam",
    createdAt: serverTime(),
    updatedAt: serverTime(),
  };

  await assertSucceeds(col.doc("debt_1").set(validDebt));
  await assertSucceeds(col.get());
  await assertSucceeds(col.doc("debt_1").update({ settled: true, updatedAt: serverTime() }));
  await assertFails(col.doc("debt_bad").set({ ...validDebt, amount: -100 }));
  await assertFails(col.doc("debt_bad_type").set({ ...validDebt, type: "LOAN" }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("debtRecords").get());
  await assertSucceeds(col.doc("debt_1").delete());
});

test("Custom wallets enforce owner isolation, valid walletType, and non-negative balance", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("customWallets");

  const validWallet = {
    userId: ALICE_UID,
    name: "BCA Utama",
    walletType: "BANK",
    balance: 4250000,
    colorHex: 4279150057,
    createdAt: serverTime(),
    updatedAt: serverTime(),
  };

  await assertSucceeds(col.doc("cw_1").set(validWallet));
  await assertSucceeds(col.get());
  await assertSucceeds(col.doc("cw_1").update({ balance: 5000000, updatedAt: serverTime() }));
  await assertFails(col.doc("cw_1").update({ balance: -500, updatedAt: serverTime() }));
  await assertFails(col.doc("cw_bad").set({ ...validWallet, walletType: "CRYPTO" }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("customWallets").get());
  await assertSucceeds(col.doc("cw_1").delete());
});

test("Subscriptions enforce owner isolation, valid billingCycle, renewalDay, and updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("subscriptions");

  const validSub = {
    userId: ALICE_UID,
    name: "Spotify Duo",
    amount: 71500,
    billingCycle: "MONTHLY",
    renewalDay: 15,
    category: "ENTERTAINMENT",
    paymentMethod: "QRIS",
    active: true,
    note: "Paket keluarga",
    createdAt: serverTime(),
    updatedAt: serverTime(),
  };

  await assertSucceeds(col.doc("sub_1").set(validSub));
  await assertSucceeds(col.get());
  await assertSucceeds(col.doc("sub_1").update({ active: false, amount: 75000, updatedAt: serverTime() }));
  await assertFails(col.doc("sub_bad_cycle").set({ ...validSub, billingCycle: "DAILY" }));
  await assertFails(col.doc("sub_bad_day").set({ ...validSub, renewalDay: 35 }));
  await assertFails(col.doc("sub_zero").set({ ...validSub, amount: 0 }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("subscriptions").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("subscriptions").doc("sub_1").delete());
  await assertSucceeds(col.doc("sub_1").delete());
});

test("Bill schedules enforce owner isolation, valid dueDayOfMonth, autoLogEnabled, and monthKey format", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("billSchedules");

  const validSchedule = {
    userId: ALICE_UID,
    title: "Internet IndiHome",
    dueDayOfMonth: 10,
    reminderEnabled: true,
    autoLogEnabled: true,
    lastAutoLoggedMonthKey: "",
    createdAt: serverTime(),
    updatedAt: serverTime(),
  };

  await assertSucceeds(col.doc("tpl_1").set(validSchedule));
  await assertSucceeds(col.get());
  await assertSucceeds(col.doc("tpl_1").update({ lastAutoLoggedMonthKey: "2026-11", updatedAt: serverTime() }));
  await assertFails(col.doc("tpl_1").update({ lastAutoLoggedMonthKey: "2026-99", updatedAt: serverTime() }));
  await assertFails(col.doc("tpl_bad_day").set({ ...validSchedule, dueDayOfMonth: 0 }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("billSchedules").get());
  await assertSucceeds(col.doc("tpl_1").delete());
});

test("Net worth snapshots enforce owner isolation, valid monthKey, and consistent netWorth arithmetic", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("netWorthSnapshots");

  const validSnap = {
    userId: ALICE_UID,
    monthKey: "2026-11",
    totalAssets: 15000000,
    totalLiabilities: 2000000,
    netWorth: 13000000,
    createdAt: serverTime(),
    updatedAt: serverTime(),
  };

  await assertSucceeds(col.doc("2026-11").set(validSnap));
  await assertSucceeds(col.get());
  await assertSucceeds(col.doc("2026-11").update({
    totalAssets: 16500000,
    totalLiabilities: 1500000,
    netWorth: 15000000,
    updatedAt: serverTime(),
  }));
  await assertFails(col.doc("2026-12").set({ ...validSnap, monthKey: "2026-12", netWorth: 999999 }));
  await assertFails(col.doc("2026-12").set({ ...validSnap, monthKey: "2026-11" }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("netWorthSnapshots").get());
  await assertSucceeds(col.doc("2026-11").delete());
});
