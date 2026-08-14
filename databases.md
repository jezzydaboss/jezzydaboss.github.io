# Databases — Android Inventory Application

[Home](index.md) | [Software Engineering](software-engineering.md) | [Algorithms](algorithms.md) | [Databases](databases.md) | [Code Review](code-review.md)

---

## Artifact Description

The artifact is the local Room/SQLite database layer of an Android inventory management application, originally built for CS 360, Mobile Architecture and Programming. It handles user login/registration and manages inventory items — name, quantity, location, and an optional image — with a low-stock SMS alert feature.

**Download the full code:** original and enhanced source files are in this repository under `/artifacts/databases/`.

## Justification for Inclusion

I selected this artifact because it's named for exactly the skill it's supposed to demonstrate — the "encrypted database wrapper" — which made my code review unusually direct: I checked whether it actually did what its own name claimed, and it didn't. The `Room.databaseBuilder(...)` call configured no encryption at all; it was a plain, unencrypted SQLite database, and user passwords were stored and compared as raw strings directly inside a SQL query. I also found that `LoginActivity` bypassed the app's own `AppDatabase` singleton and constructed a second, separately-named database directly, and that a failed login silently created a brand-new account instead of reporting an error.

### What I Fixed

- Integrated **SQLCipher** so the database file itself is encrypted at rest
- Added `DatabaseKeyProvider` — the encryption key is randomly generated on first run and stored via **Android Keystore**-backed `EncryptedSharedPreferences`, never hardcoded
- Replaced plaintext password storage with **PBKDF2 password hashing**, salted per user, verified in application code (`PasswordHasher`)
- Fixed the singleton bypass so there is exactly one database file, and it is the encrypted one
- Removed the silent auto-registration — failed logins now show a generic error instead of creating an account
- Added a **unique index** on the username column, closing a real check-then-insert race condition at the database layer

### Figure 1 — Constant-Time Password Verification

![PasswordHasher constant-time comparison](assets/screenshots/artifact3_shot1.png)

*Constant-time hash comparison, closing a timing side-channel in password verification.*

### Figure 2 — Secure Key Storage

![DatabaseKeyProvider Keystore-backed key](assets/screenshots/artifact3_shot2.png)

*The SQLCipher encryption key is randomly generated and stored via the Android Keystore, never hardcoded as a string literal.*

## Verification

Unlike my other two artifacts, I could not compile Android-specific code (Room, AndroidX Security, SQLCipher APIs) in my own working environment. I isolated the one piece with no Android dependency — `PasswordHasher.java`, using only `javax.crypto` — and wrote and ran a real 5-test suite against it; all passed. I then completed a full build in Android Studio: **BUILD SUCCESSFUL**, 35 tasks executed, running on a Pixel emulator. I manually verified on-device that duplicate usernames are correctly rejected and that invalid logins show the correct generic error instead of the original silent auto-registration behavior. One documented limitation surfaced during that build: SQLCipher 4.5.4's native library isn't yet compiled for 16 KB memory page sizes on newer devices — Android runs it in a backward-compatible mode automatically, so nothing is broken, but it's a known constraint worth tracking for a future SQLCipher release.

## Course Outcomes

This enhancement demonstrates progress toward **Outcomes 4 and 5**: choosing industry-standard tools (SQLCipher, PBKDF2, Android Keystore) rather than inventing custom encryption, and developing a security mindset that anticipates how a system's implementation can fail to match its own stated design intent.

## Reflection

The clearest lesson was recognizing the limits of what I could verify myself, and being explicit about exactly where that line sat rather than blurring it — isolating the password-hashing logic for real testing, documenting exactly what a reviewer would need to do to verify the rest, and later actually completing that Android Studio build myself. The most interesting design decision was rejecting a hardcoded encryption key in favor of Keystore-backed storage, since a hardcoded key would have made the encryption almost cosmetic.
