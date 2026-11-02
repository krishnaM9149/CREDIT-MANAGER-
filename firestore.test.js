const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_shop_owner";
const BOB_UID = "bob_other_owner";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: cannot read or write customers, inventory, or transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("customers").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("inventory").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Authenticated owner: can create and query valid Customer, InventoryItem, and ShopTransaction", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  const validCustomer = {
    userId: ALICE_UID,
    name: "Priya Sharma",
    phone: "+919876543210",
    notes: "Prefers dark chocolate truffles",
    creditBalance: 450.0,
    totalBilled: 1200.0,
    totalPaid: 750.0,
    weeklyReminderEnabled: true,
    lastReminderSentAt: "2026-11-01",
    loyaltyPoints: 120,
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1").set(validCustomer)
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customers").where("userId", "==", ALICE_UID).get()
  );

  const validItem = {
    userId: ALICE_UID,
    name: "Belgian Hazelnut Praline",
    category: "Chocolates",
    sku: "CHO-101",
    unit: "box",
    price: 350.0,
    costPrice: 210.0,
    stockQuantity: 25,
    lowStockThreshold: 5,
    expiryDate: "2026-12-15",
    supplierName: "In-House Kitchen",
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("inventory").doc("item_1").set(validItem)
  );

  const validTx = {
    userId: ALICE_UID,
    customerId: "cust_1",
    customerName: "Priya Sharma",
    customerPhone: "+919876543210",
    type: "CREDIT_SALE",
    itemsSummary: "2x Belgian Hazelnut Praline (700.0)",
    totalAmount: 700.0,
    paidAmount: 250.0,
    balanceDelta: 450.0,
    paymentMethod: "CREDIT",
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set(validTx)
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's shop data", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("customers").doc("cust_1").set({
      userId: ALICE_UID,
      name: "Priya Sharma",
      phone: "+919876543210",
      notes: "",
      creditBalance: 100.0,
      totalBilled: 100.0,
      totalPaid: 0.0,
      weeklyReminderEnabled: true,
      lastReminderSentAt: "",
      loyaltyPoints: 10,
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("customers").where("userId", "==", ALICE_UID).get()
  );
});

test("Shadow update & schema validation: rejects ghost fields and negative stock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_bad").set({
      userId: ALICE_UID,
      name: "Ghost User",
      phone: "12345",
      notes: "",
      creditBalance: 0,
      totalBilled: 0,
      totalPaid: 0,
      weeklyReminderEnabled: true,
      lastReminderSentAt: "",
      loyaltyPoints: 0,
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("inventory").doc("item_bad").set({
      userId: ALICE_UID,
      name: "Bad Stock",
      category: "Sweets",
      sku: "BAD-1",
      unit: "kg",
      price: 100,
      costPrice: 50,
      stockQuantity: -10,
      lowStockThreshold: 5,
      expiryDate: "2026-12-01",
      supplierName: "Vendor",
      createdAt: now,
      updatedAt: now,
    })
  );
});
