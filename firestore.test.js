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
    displayName: "أحمد",
    preferredLanguage: "ar",
    themeMode: "SYSTEM",
    mushafPaper: "WHITE",
    fontScale: 1.0,
    lastReadPage: 1,
    lastReadSurah: 1,
    lastReadAyah: 1,
    preferredReciterId: "ar.alafasy",
    adhanEnabled: true,
    adhanVoiceId: "makkah",
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
  await assertFails(unauthDb.collection("reciters").where("active", "==", true).get());
  await assertFails(unauthDb.collection("duaWall").where("visibility", "==", "COMMUNITY").get());
  await assertFails(unauthDb.collection("khatmahGroups").where("memberUids", "array-contains", ALICE_UID).get());
});

test("User settings (Phase 1 theme/RTL/lastReadPage) enforce strict owner isolation and partial updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserSettings(ALICE_UID, past)));

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc("ghost_user").set({
      ...validUserSettings("ghost_user", past),
      isAdmin: true,
    })
  );

  // Alice updates themeMode, language, and lastReadPage while preserving createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "en",
      themeMode: "DARK",
      mushafPaper: "SEPIA",
      lastReadPage: 255,
      lastReadSurah: 13,
      lastReadAyah: 28,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject out-of-range page 605
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      lastReadPage: 605,
      updatedAt: new Date(Date.now() - 500),
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

test("Subcollections (bookmarks with pageNumber, Phase 2 adhkarLogs/aiChats) enforce schema & ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_2_255").set({
      surahNumber: 2,
      ayahNumber: 255,
      pageNumber: 42,
      collectionName: "Favorites",
      note: "Ayat al-Kursi",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bookmarks").get());

  // Phase 2 prepared adhkarLogs & aiChats
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("adhkarLogs").doc("2026-11-23_morning").set({
      dateKey: "2026-11-23",
      category: "MORNING",
      completedCount: 5,
      targetCount: 10,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("aiChats").doc("chat_1").set({
      role: "user",
      content: "ما تفسير آية الكرسي؟",
      surahRef: "2:255",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("aiChats").get());
});

test("Reciters catalog (Phase 1/3) permits active read and blocks non-admin writes", async () => {
  const past = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("reciters").doc("ar.alafasy").set({
      nameAr: "مشاري راشد العفاسي",
      nameEn: "Mishary Rashid Alafasy",
      styleAr: "مرتل",
      styleEn: "Murattal",
      editionId: "ar.alafasy",
      everyAyahFolder: "Alafasy_128kbps",
      sortOrder: 1,
      active: true,
      createdAt: past,
      updatedAt: past,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_admin", { admin: true, email: "other@gmail.com", email_verified: true }).firestore();
  const unverifiedSuperDb = testEnv.authenticatedContext("unverified_super", { email: "habiybullohy@gmail.com", email_verified: false }).firestore();
  const adminDb = testEnv.authenticatedContext("super_admin", { email: "habiybullohy@gmail.com", email_verified: true }).firestore();

  await assertSucceeds(aliceDb.collection("reciters").where("active", "==", true).get());
  await assertFails(aliceDb.collection("reciters").get()); // must filter active == true
  await assertFails(spoofedAdminDb.collection("reciters").get()); // spoofed claim denied
  await assertFails(unverifiedSuperDb.collection("reciters").get()); // unverified email denied
  await assertFails(
    aliceDb.collection("reciters").doc("fake").set({
      nameAr: "Fake",
      nameEn: "Fake",
      styleAr: "مرتل",
      styleEn: "Murattal",
      editionId: "fake",
      everyAyahFolder: "fake",
      sortOrder: 9,
      active: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Admin can manage reciters
  await assertSucceeds(
    adminDb.collection("reciters").doc("ar.husary").set({
      nameAr: "محمود خليل الحصري",
      nameEn: "Mahmoud Khalil Al-Husary",
      styleAr: "مرتل",
      styleEn: "Murattal",
      editionId: "ar.husary",
      everyAyahFolder: "Husary_128kbps",
      sortOrder: 2,
      active: true,
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Community Dua Wall and Khatmah groups continue to enforce strict parity", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("duaWall").doc("post_1").set({
      authorUid: ALICE_UID,
      alias: "طالب نور",
      category: "HEALING",
      requestText: "نسألكم الدعاء بالشفاء العاجل.",
      visibility: "COMMUNITY",
      ameenCount: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(aliceDb.collection("duaWall").where("visibility", "==", "COMMUNITY").get());
  await assertFails(aliceDb.collection("duaWall").get());
  await assertSucceeds(
    bobDb.collection("duaWall").doc("post_1").update({
      ameenCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("duaWall").doc("post_1").delete());
});

test("Phase 2 & 3 per-prayer Adhan, adminConfigs, adhanVoices, adhkarItems, and exclusive Super Admin lock enforce parity", async () => {
  const past = new Date(Date.now() - 5000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const spoofedDb = testEnv.authenticatedContext("spoof", { admin: true, email: "attacker@gmail.com", email_verified: true }).firestore();
  const adminDb = testEnv.authenticatedContext("super_admin", { email: "habiybullohy@gmail.com", email_verified: true }).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserSettings(ALICE_UID, past)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      perPrayerAdhanModes: {
        fajr: "FULL_ADHAN",
        dhuhr: "TAKBIR_ONLY",
        asr: "GENTLE_CHIME",
        maghrib: "FULL_ADHAN",
        isha: "SILENT",
      },
      customAdhanName: "my_custom_adhan.mp3",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      perPrayerAdhanModes: { fajr: "INVALID_MODE" },
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    adminDb.collection("adminConfigs").doc("ai_settings").set({
      configKey: "ai_settings",
      valueJson: JSON.stringify({ enabled: true, modelName: "gemini-3.8-flash", systemPrompt: "أنت مساعد إسلامي." }),
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(aliceDb.collection("adminConfigs").doc("ai_settings").get());
  await assertFails(
    aliceDb.collection("adminConfigs").doc("ai_settings").set({
      configKey: "ai_settings",
      valueJson: "{}",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    adminDb.collection("adhanVoices").doc("makkah").set({
      nameAr: "أذان المسجد الحرام",
      nameEn: "Makkah Al-Haram",
      muezzin: "الشيخ علي ملا",
      audioUrl: "https://example.com/makkah.mp3",
      active: true,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(aliceDb.collection("adhanVoices").where("active", "==", true).get());
  await assertFails(aliceDb.collection("adhanVoices").doc("hacked").delete());

  await assertSucceeds(
    adminDb.collection("adhkarItems").doc("ruqyah_1").set({
      category: "RUQYAH",
      titleAr: "سورة الفاتحة والشفاء",
      titleEn: "Al-Fatihah Ruqyah",
      arabic: "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
      transliteration: "Bismillah",
      translation: "In the name of Allah",
      targetCount: 7,
      referenceAr: "صحيح البخاري",
      referenceEn: "Sahih al-Bukhari",
      iconName: "HealthAndSafety",
      sortOrder: 1,
      active: true,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(aliceDb.collection("adhkarItems").where("active", "==", true).get());

  // Phase 3: Only habiybullohy@gmail.com (verified) can list users or mutate admin configs/catalogs
  await assertFails(aliceDb.collection("users").get());
  await assertFails(spoofedDb.collection("users").get());
  await assertFails(
    spoofedDb.collection("adminConfigs").doc("app_operations").set({
      configKey: "app_operations",
      valueJson: "{}",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(adminDb.collection("adhkarItems").get());
  await assertSucceeds(adminDb.collection("adhanVoices").get());
  await assertSucceeds(
    adminDb.collection("adminConfigs").doc("app_operations").set({
      configKey: "app_operations",
      valueJson: JSON.stringify({ bannerEnabled: true, bannerTitleAr: "تنبيه", maintenanceMode: false }),
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("User profile fields, anti-self-promotion, verification, suspension, and delegated admin appointment enforce zero-trust rules", async () => {
  const past = new Date(Date.now() - 5000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const superAdminDb = testEnv.authenticatedContext("super_admin", { email: "habiybullohy@gmail.com", email_verified: true }).firestore();

  // Regular user cannot self-create as ADMIN or verified
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validUserSettings(ALICE_UID, past),
      email: "alice@example.com",
      photoUrl: "https://example.com/alice.jpg",
      adminRole: "ADMIN",
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validUserSettings(ALICE_UID, past),
      accountVerified: true,
    })
  );

  // Regular user creates valid profile with email, photoUrl, and safe defaults
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validUserSettings(ALICE_UID, past),
      email: "alice@example.com",
      photoUrl: "https://example.com/alice.jpg",
      accountVerified: false,
      accountSuspended: false,
      adminRole: "USER",
    })
  );
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set({
      ...validUserSettings(BOB_UID, past),
      email: "bob@example.com",
      photoUrl: "https://example.com/bob.jpg",
      accountVerified: false,
      accountSuspended: false,
      adminRole: "USER",
    })
  );

  // Owner can edit their own displayName, but CANNOT verify, suspend, or promote themselves
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "أسامة المحدّث",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      accountVerified: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      adminRole: "ADMIN",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Super Admin verifies Bob and appoints Alice as ADMIN
  await assertSucceeds(
    superAdminDb.collection("users").doc(BOB_UID).update({
      accountVerified: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    superAdminDb.collection("users").doc(ALICE_UID).update({
      adminRole: "ADMIN",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Appointed Admin Alice can now list users, suspend Bob, and update app_operations
  await assertSucceeds(aliceDb.collection("users").get());
  await assertSucceeds(
    aliceDb.collection("users").doc(BOB_UID).update({
      accountSuspended: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    aliceDb.collection("adminConfigs").doc("app_operations").set({
      configKey: "app_operations",
      valueJson: JSON.stringify({ supportWhatsapp: "+1 (581) 509-1598", supportTelegram: "+963 984 215 420", supportEmail: "habiybullohy@gmail.com" }),
      createdAt: past,
      updatedAt: past,
    })
  );

  // Suspended Bob cannot unsuspend himself
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).update({
      accountSuspended: false,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // If Alice is suspended, she immediately loses admin privileges
  await assertSucceeds(
    superAdminDb.collection("users").doc(ALICE_UID).update({
      accountSuspended: true,
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(aliceDb.collection("users").get());
});

test("Custom profile photo data URI and islamicVideos catalog enforce rules parity", async () => {
  const past = new Date(Date.now() - 5000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const superAdminDb = testEnv.authenticatedContext("super_admin", { email: "habiybullohy@gmail.com", email_verified: true }).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserSettings(ALICE_UID, past)));

  // User can upload a compressed base64 avatar (~18 KB)
  const compressedAvatar = "data:image/jpeg;base64," + "A".repeat(18000);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      photoUrl: compressedAvatar,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Regular user cannot create an islamicVideos entry
  await assertFails(
    aliceDb.collection("islamicVideos").doc("vid_1").set({
      category: "EXPLAIN_ISLAM",
      titleAr: "مقدمة عن الإسلام",
      titleEn: "Introduction to Islam",
      titleUz: "Islomga kirish",
      descriptionAr: "شرح مبسط لأركان الإسلام",
      descriptionEn: "Simple explanation of Islam",
      descriptionUz: "Islom asoslari haqida",
      videoUrl: "https://example.com/intro.mp4",
      thumbnailUrl: "",
      durationLabel: "05:30",
      sortOrder: 1,
      active: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Admin can publish EXPLAIN_ISLAM and ISLAMIC_CARTOON videos
  await assertSucceeds(
    superAdminDb.collection("islamicVideos").doc("vid_1").set({
      category: "ISLAMIC_CARTOON",
      titleAr: "كرتون أمانة المسلم",
      titleEn: "Cartoon: Honesty in Islam",
      titleUz: "Multfilm: Omonatdorlik",
      descriptionAr: "قصة كرتونية تعليمية للأطفال",
      descriptionEn: "Educational Islamic cartoon for children",
      descriptionUz: "Bolalar uchun tarbiyaviy multfilm",
      videoUrl: "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
      thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
      durationLabel: "04:15",
      sortOrder: 1,
      active: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    superAdminDb.collection("islamicVideos").doc("vid_hidden").set({
      category: "EXPLAIN_ISLAM",
      titleAr: "مسودة مخفية",
      titleEn: "Hidden Draft",
      titleUz: "Yashirin",
      descriptionAr: "محتوى غير منشور",
      descriptionEn: "Draft",
      descriptionUz: "Draft",
      videoUrl: "https://example.com/draft.mp4",
      thumbnailUrl: "",
      durationLabel: "03:00",
      sortOrder: 2,
      active: false,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Signed-in users can list active videos, but unfiltered list is denied
  await assertSucceeds(aliceDb.collection("islamicVideos").where("active", "==", true).get());
  await assertFails(aliceDb.collection("islamicVideos").get());
  await assertSucceeds(superAdminDb.collection("islamicVideos").get());
});
