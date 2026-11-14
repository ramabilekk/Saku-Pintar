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
  await assertSucceeds(expRef.set(validExpensePayload()));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("expenses").where("monthKey", "==", "2026-11").get());
  await assertSucceeds(expRef.update({ amount: 42000, title: "Makan Malam", updatedAt: serverTime() }));
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

test("Expense schema validation blocks negative amount, invalid category, ghost fields, and userId spoofing", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("expenses");

  await assertFails(col.doc("e_neg").set(validExpensePayload(ALICE_UID, { amount: -5000 })));
  await assertFails(col.doc("e_zero").set(validExpensePayload(ALICE_UID, { amount: 0 })));
  await assertFails(col.doc("e_cat").set(validExpensePayload(ALICE_UID, { category: "CRYPTO" })));
  await assertFails(col.doc("e_ghost").set(validExpensePayload(ALICE_UID, { isAdmin: true })));
  await assertFails(col.doc("e_spoof").set(validExpensePayload(BOB_UID)));
  await assertFails(col.doc("e_month").set(validExpensePayload(ALICE_UID, { monthKey: "2026-13" })));
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
