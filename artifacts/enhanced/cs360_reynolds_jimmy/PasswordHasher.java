package com.zybooks.cs360_reynolds_jimmy;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/***********************************************************
 *  PasswordHasher
 *
 *  ENHANCEMENT (Milestone Four, CS 499): this artifact was named
 *  the "encrypted database wrapper" but stored and compared
 *  passwords as plaintext strings directly in a SQL WHERE clause
 *  (see the original UserDao.login() and Room's generated SQL:
 *  "SELECT * FROM users WHERE username = ? AND password = ?").
 *  This class replaces that with PBKDF2 (Password-Based Key
 *  Derivation Function 2), a standard, salted, iterated password
 *  hashing algorithm - not a fast general-purpose hash like
 *  SHA-256 alone, which is unsuitable for passwords because it's
 *  too cheap to brute-force at scale.
 *
 *  Design choices, and why:
 *   - PBKDF2WithHmacSHA1 (not SHA256) was chosen specifically for
 *     Android compatibility: PBKDF2WithHmacSHA1 has been available
 *     via javax.crypto.SecretKeyFactory since Android API level 10,
 *     while PBKDF2WithHmacSHA256 requires API 26+. Since this app's
 *     minSdkVersion was not confirmed to be 26+, SHA1-backed PBKDF2
 *     (which is still considered acceptable for password hashing -
 *     the security here comes from the PBKDF2 iteration count and
 *     salt, not from SHA-1's collision resistance, which is not
 *     what's being relied on) is the safer compatibility choice.
 *     If minSdkVersion is confirmed to be 26 or higher, swapping
 *     the algorithm string to "PBKDF2WithHmacSHA256" is a one-line
 *     change.
 *   - A new random 16-byte salt is generated PER USER at registration
 *     time via SecureRandom (a cryptographically secure random source,
 *     not java.util.Random) and stored alongside the hash. This means
 *     two users with the same password get completely different
 *     stored hashes, which defeats precomputed rainbow-table attacks.
 *   - 65536 iterations is a commonly recommended minimum for PBKDF2
 *     as of recent OWASP guidance for HMAC-SHA1-backed PBKDF2; this
 *     can be increased over time as hardware gets faster, since the
 *     iteration count is stored in the class, not derived per-user
 *     (a production system would ideally store the iteration count
 *     used per-hash to allow safely increasing it over time without
 *     invalidating existing users' stored hashes).
 *
 *  This class has NO Android dependencies (only javax.crypto and
 *  java.security, both part of the standard JDK) - it was written
 *  and unit-tested as plain Java in isolation before being wired
 *  into the Android-specific UserRepository, specifically so its
 *  core correctness could be verified with a real compiler and a
 *  real test run rather than only reviewed by eye.
 ***********************************************************/
public class PasswordHasher {

    private static final int SALT_LENGTH_BYTES = 16;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA1";

    /**
     * Generates a new cryptographically random salt, base64-encoded for
     * storage as a String column alongside the password hash.
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashes the given plaintext password with the given base64-encoded
     * salt, returning a base64-encoded hash suitable for storage.
     */
    public static String hash(String plainTextPassword, String base64Salt) {
        try {
            byte[] salt = Base64.getDecoder().decode(base64Salt);
            PBEKeySpec spec = new PBEKeySpec(
                    plainTextPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hashBytes = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // ENHANCEMENT: fail loudly instead of silently returning an
            // unhashed or null value - a failure here must never result
            // in a password being stored or compared insecurely.
            throw new RuntimeException("Password hashing failed - " + ALGORITHM + " unavailable", e);
        }
    }

    /**
     * Verifies a plaintext password attempt against a previously stored
     * salt + hash pair. This is what UserRepository uses at login time,
     * instead of comparing raw password strings in a SQL query.
     */
    public static boolean verify(String plainTextPasswordAttempt, String base64Salt, String expectedHash) {
        String computedHash = hash(plainTextPasswordAttempt, base64Salt);
        return constantTimeEquals(computedHash, expectedHash);
    }

    /**
     * ENHANCEMENT: a naive String.equals() comparison of two hashes can
     * leak timing information (it returns as soon as it finds the first
     * mismatched character), which in theory helps an attacker guess a
     * hash byte-by-byte. Comparing every character regardless of an
     * early mismatch keeps the comparison time constant, closing that
     * side channel. This matters less for a locally-run mobile app than
     * for a network-facing service, but it costs nothing to do correctly.
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
