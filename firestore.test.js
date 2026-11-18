const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_owner_123";
const BOB_UID = "bob_owner_456";

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

test("Unauthenticated requests are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("members").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("payments").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("expenses").get());
});

test("OwnerProfile: valid create/update succeeds; cross-user & ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      gymName: "Iron Forge Gym",
      ownerName: "Tariq Mahmood",
      ownerPhone: "+923001234567",
      email: "owner@ironforge.pk",
      language: "en",
      activeBranchId: "main",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      language: "ur",
      gymName: "Iron Forge Elite",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user read/write blocked
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      gymName: "Hijacked Gym",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Shadow field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Members: Kotlin create/partial update succeed; @DocumentId id field & cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  const memberPayload = {
    userId: ALICE_UID,
    membershipId: "GYM-1001",
    branchId: "main",
    fullName: "Ahmed Khan",
    fatherName: "Rashid Khan",
    cnic: "35202-1234567-1",
    mobile: "+923001112233",
    whatsapp: "+923001112233",
    emergencyContact: "+923009998877",
    address: "Gulberg III, Lahore",
    gender: "Male",
    dateOfBirth: "1996-05-12",
    bloodGroup: "O+",
    packageName: "Pro Bodybuilding",
    monthlyFee: 3000,
    pendingAmount: 3000,
    joiningDateMillis: Date.now() - 86400000 * 30,
    startDateMillis: Date.now() - 86400000 * 30,
    expiryDateMillis: Date.now() + 86400000 * 335,
    paidThroughMillis: Date.now(),
    dueDayOfMonth: 15,
    status: "ACTIVE",
    heightCm: 178.5,
    weightKg: 84.2,
    bodyFatPercent: 12.4,
    fitnessGoal: "Classic Physique",
    competitionPrep: true,
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("members").doc("mem_1").set(memberPayload)
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("members").doc("mem_1").update({
      pendingAmount: 0,
      lastPaymentDateMillis: Date.now() - 1000,
      paidThroughMillis: Date.now() + 86400000 * 30,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // @DocumentId `id` field inside payload must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("members").doc("mem_2").set({
      ...memberPayload,
      id: "mem_2",
    })
  );

  // Cross-user list & get blocked
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("members").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("members").doc("mem_1").get());

  // Empty subcollection list for owner succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("members").get());
});

test("Payments, BodyLogs, Attendance, Trainers, Workouts, Diets, Supplements, Expenses, Equipment, AuditLogs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceUser = aliceDb.collection("users").doc(ALICE_UID);
  const now = new Date(Date.now() - 3000);

  await assertSucceeds(
    aliceUser.collection("payments").doc("pay_1").set({
      userId: ALICE_UID,
      memberId: "mem_1",
      memberName: "Ahmed Khan",
      membershipId: "GYM-1001",
      branchId: "main",
      amount: 3000,
      discount: 0,
      paymentMethod: "CASH",
      forMonthLabel: "Nov 2026",
      dueDateMillis: Date.now(),
      paidAtMillis: Date.now(),
      receiptNumber: "RCP-1001",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceUser.collection("bodyLogs").doc("log_1").set({
      userId: ALICE_UID,
      memberId: "mem_1",
      memberName: "Ahmed Khan",
      recordedAtMillis: Date.now(),
      weightKg: 84.5,
      bodyFatPercent: 12.1,
      muscleMassKg: 42.3,
      chestCm: 112.0,
      waistCm: 79.0,
      armsCm: 43.0,
      thighsCm: 64.0,
      photoTag: "PROGRESS",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceUser.collection("attendance").doc("att_1").set({
      userId: ALICE_UID,
      memberId: "mem_1",
      memberName: "Ahmed Khan",
      membershipId: "GYM-1001",
      branchId: "main",
      method: "QR",
      checkInMillis: Date.now() - 3600000,
      dateKey: "2026-11-17",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceUser.collection("attendance").doc("att_1").update({
      checkOutMillis: Date.now(),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceUser.collection("trainers").doc("tr_1").set({
      userId: ALICE_UID,
      name: "Coach Imran",
      mobile: "+923211112233",
      salary: 45000,
      specialization: "Hypertrophy & Contest Prep",
      branchId: "main",
      active: true,
      attendanceDaysThisMonth: 22,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceUser.collection("workoutPlans").doc("wp_1").set({
      userId: ALICE_UID,
      dayCategory: "CHEST",
      title: "Heavy Chest & Upper Pec Split",
      exercises: [{ name: "Incline Barbell Press", sets: 4, reps: "8-10", weightKg: 90, videoUrl: "", instructions: "Control eccentric" }],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceUser.collection("dietPlans").doc("dp_1").set({
      userId: ALICE_UID,
      title: "Lean Bulk 3200 kcal",
      goal: "Lean Muscle Gain",
      breakfast: "6 egg whites, 80g oats, banana",
      lunch: "250g grilled chicken breast, 200g basmati rice",
      snacks: "1 scoop whey + 30g almonds",
      dinner: "220g beef steak, steamed broccoli",
      totalCalories: 3200,
      totalProteinGrams: 225,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceUser.collection("supplements").doc("sup_1").set({
      userId: ALICE_UID,
      name: "Gold Standard Whey 5lb",
      category: "WHEY_PROTEIN",
      brand: "Optimum Nutrition",
      stockQty: 14,
      soldQty: 6,
      unitPrice: 24500,
      expiryDateMillis: Date.now() + 86400000 * 300,
      branchId: "main",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceUser.collection("expenses").doc("exp_1").set({
      userId: ALICE_UID,
      category: "RENT",
      title: "November Hall Rent",
      amount: 120000,
      expenseDateMillis: Date.now(),
      branchId: "main",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceUser.collection("equipment").doc("eq_1").set({
      userId: ALICE_UID,
      name: "Hammer Strength Chest Press",
      category: "Plate Loaded",
      purchaseDateMillis: Date.now() - 86400000 * 180,
      cost: 280000,
      nextMaintenanceMillis: Date.now() + 86400000 * 3,
      condition: "GOOD",
      branchId: "main",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceUser.collection("auditLogs").doc("aud_1").set({
      userId: ALICE_UID,
      action: "COLLECT_FEE",
      actorEmail: "owner@ironforge.pk",
      details: "Collected Rs. 3000 from Ahmed Khan",
      timestampMillis: Date.now(),
      createdAt: now,
      updatedAt: now,
    })
  );

  // Audit logs must be immutable (update forbidden)
  await assertFails(
    aliceUser.collection("auditLogs").doc("aud_1").update({
      details: "Tampered",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
