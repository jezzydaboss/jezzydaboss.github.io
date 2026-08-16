package com.zybooks.cs360_reynolds_jimmy;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/***********************************************************
 *  DatabaseKeyProvider
 *
 *  ENHANCEMENT (Milestone Four, CS 499): provides the passphrase
 *  used to encrypt the SQLCipher database (see AppDatabase.java).
 *
 *  This exists specifically to avoid the single most common way
 *  database encryption gets implemented badly: hardcoding the
 *  encryption key as a string literal in source code. A hardcoded
 *  key is trivially recoverable by anyone who decompiles the APK,
 *  which makes the encryption almost entirely cosmetic - the data
 *  is technically encrypted, but the key to decrypt it ships in
 *  the same package.
 *
 *  Instead, this class:
 *   1. Generates a random 256-bit key using SecureRandom the first
 *      time the app runs (not a fixed, predictable value).
 *   2. Stores that key in EncryptedSharedPreferences, which is
 *      itself backed by a key generated and held in the Android
 *      Keystore - hardware-backed on most devices - rather than
 *      anywhere in the app's own storage or source code.
 *   3. Retrieves the same key on every subsequent app launch, so
 *      the database can still be opened, without the key ever
 *      being stored in plaintext anywhere or embedded in source.
 ***********************************************************/
public class DatabaseKeyProvider {

    private static final String PREFS_FILE_NAME = "db_key_prefs";
    private static final String KEY_ALIAS = "sqlcipher_passphrase";
    private static final int KEY_LENGTH_BYTES = 32; // 256-bit key

    /**
     * Returns the database passphrase as a char[] (SQLCipher's
     * SupportFactory expects a char array, not a String, so the key
     * material can be zeroed out of memory after use rather than
     * lingering as an immutable String on the heap).
     */
    public static char[] getOrCreateDatabaseKey(Context context) {
        try {
            SharedPreferences encryptedPrefs = getEncryptedPrefs(context);

            String existingKey = encryptedPrefs.getString(KEY_ALIAS, null);
            if (existingKey != null) {
                return existingKey.toCharArray();
            }

            // first run - generate a new random key and persist it
            byte[] rawKey = new byte[KEY_LENGTH_BYTES];
            new SecureRandom().nextBytes(rawKey);
            String encodedKey = Base64.encodeToString(rawKey, Base64.NO_WRAP);

            encryptedPrefs.edit().putString(KEY_ALIAS, encodedKey).apply();
            return encodedKey.toCharArray();

        } catch (GeneralSecurityException | IOException e) {
            // ENHANCEMENT: fail loudly rather than silently falling back
            // to an unencrypted database or a hardcoded key - either of
            // those would defeat the entire point of this enhancement.
            throw new RuntimeException("Could not obtain a secure database encryption key", e);
        }
    }

    private static SharedPreferences getEncryptedPrefs(Context context) throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }
}
