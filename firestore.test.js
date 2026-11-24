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

function validUserSettings(uid, now) {
  return {
    userId: uid,
    displayName: "Ahmad",
    preferredLanguage: "en",
    calculationMethod: "MWL",
    city: "Makkah",
    latitude: 21.4225,
    longitude: 39.8262,
    hijriOffset: 0,
    travelerMode: false,
    ramadanMode: false,
    kidsMode: false,
    qadaFajr: 0,
    qadaDhuhr: 0,
    qadaAsr: 0,
    qadaMaghrib: 0,
    qadaIsha: 0,
    streakDays: 1,
    khatmahPagesRead: 12,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated users are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validUserSettings(ALICE_UID, past)));
  await assertFails(unauthDb.collection("duaWall").where("visibility", "==", "COMMUNITY").get());
  await assertFails(unauthDb.collection("khatmahGroups").where("memberUids", "array-contains", ALICE_UID).get());
});

test("User settings and subcollections enforce strict owner isolation and partial updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // Alice creates her own profile
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserSettings(ALICE_UID, past)));

  // Reject ghost field ("Shadow Update Test")
  await assertFails(
    aliceDb.collection("users").doc("ghost_user").set({
      ...validUserSettings("ghost_user", past),
      isAdmin: true,
    })
  );

  // Alice updates her settings partially while preserving createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      ramadanMode: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt overwrite
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot read or write Alice's profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ city: "Hijack", updatedAt: past }));
});

test("Prayer logs, journal entries, bookmarks, tasbih, and obligations enforce schema & ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // PrayerLog
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("prayerLogs").doc("2026-11-23").set({
      dateKey: "2026-11-23",
      fajr: true,
      dhuhr: true,
      asr: false,
      maghrib: false,
      isha: false,
      sunnahRawatib: true,
      duha: false,
      witr: false,
      qiyam: false,
      fastingType: "VOLUNTARY",
      khushuRating: 4,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("prayerLogs").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("prayerLogs").get());

  // JournalEntry (without optional reference, and with optional reference)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("duaJournal").doc("entry_1").set({
      entryType: "DUA",
      title: "Guidance and Steadfastness",
      body: "O Allah keep my heart firm upon Your religion.",
      answered: false,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("duaJournal").doc("entry_bad").set({
      id: "entry_bad", // Forbidden @DocumentId key
      entryType: "DUA",
      title: "Bad",
      body: "Body",
      answered: false,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Bookmark & Tasbih & Obligation
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_1_1").set({
      surahNumber: 1,
      ayahNumber: 1,
      collectionName: "Favorites",
      note: "Opening of the Book",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasbihCounters").doc("subhanallah").set({
      label: "SubhanAllah",
      arabicText: "سُبْحَانَ اللَّهِ",
      count: 33,
      target: 33,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("obligations").doc("charity_1").set({
      kind: "CHARITY",
      title: "Water Well Sadaqah",
      amount: 50.0,
      settled: true,
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Community Dua Wall allows filtered queries and +1 Ameen increments while blocking tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("duaWall").doc("post_1").set({
      authorUid: ALICE_UID,
      alias: "Seeking Light",
      category: "HEALING",
      requestText: "Please make dua for my mother's full recovery.",
      visibility: "COMMUNITY",
      ameenCount: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Filtered query matching rule succeeds; unfiltered fails
  await assertSucceeds(aliceDb.collection("duaWall").where("visibility", "==", "COMMUNITY").get());
  await assertFails(aliceDb.collection("duaWall").get());

  // Bob can increment Ameen by 1, cannot jump by 5 or edit requestText or delete
  await assertSucceeds(
    bobDb.collection("duaWall").doc("post_1").update({
      ameenCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    bobDb.collection("duaWall").doc("post_1").update({
      ameenCount: 10,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    bobDb.collection("duaWall").doc("post_1").update({
      requestText: "Tampered text",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("duaWall").doc("post_1").delete());
});

test("Khatmah groups enforce membership on queries and updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("khatmahGroups").doc("family_1").set({
      ownerUid: ALICE_UID,
      title: "Family Ramadan Khatmah",
      joinCode: "NOOR26",
      memberUids: [ALICE_UID],
      completedJuz: [1, 2],
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("khatmahGroups").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("khatmahGroups").get());

  // Bob joins by appending only himself
  await assertSucceeds(
    bobDb.collection("khatmahGroups").doc("family_1").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // Now Bob can update completedJuz
  await assertSucceeds(
    bobDb.collection("khatmahGroups").doc("family_1").update({
      completedJuz: [1, 2, 3],
      updatedAt: new Date(Date.now() - 500),
    })
  );
  // Bob cannot delete Alice's group
  await assertFails(bobDb.collection("khatmahGroups").doc("family_1").delete());
});
