package com.wac.autocore.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Turns a password into the text the users table keeps, and compares the two. The text is
 * {@code pbkdf2$<iterations>$<salt>$<hash>}, base64 for the two last parts, and the salt travels
 * with the row so every account has its own.
 *
 * A hash is not a secret. It cannot be turned back into the password, and the salt is public on
 * purpose: it is what makes two equal passwords give two different hashes.
 */
public final class PasswordHash {

    private static final String PREFIX = "pbkdf2";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * The cost new hashes are made with. OWASP's current recommendation for PBKDF2-HMAC-SHA256 is
     * 600 000, and the row keeps the number it was made with, so raising this later only affects
     * new hashes and old rows keep working.
     */
    public static final int ITERATIONS = 600000;

    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;

    private PasswordHash() {}

    /**
     * True when the typed password is the one the stored hash was made from. A column that cannot
     * be read — wrong shape, unknown algorithm, bad base64 — is a no, never an exception: a login
     * that cannot tell must not let anyone in.
     */
    public static boolean matches(String typed, String stored) {
        if (typed == null || stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(typed, salt, iterations, expected.length);
            // Same time for a wrong password as for a right one, so the clock tells nothing.
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException | GeneralSecurityException unreadable) {
            return false;
        }
    }

    /** A new hash for a new password, with a fresh salt and the current cost. */
    public static String hash(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Ett tomt lösenord kan inte hashas.");
        }
        try {
            byte[] salt = new byte[SALT_BYTES];
            new SecureRandom().nextBytes(salt);
            byte[] hash = derive(password, salt, ITERATIONS, HASH_BYTES);

            Base64.Encoder base64 = Base64.getEncoder();
            return PREFIX + "$" + ITERATIONS + "$" + base64.encodeToString(salt)
                    + "$" + base64.encodeToString(hash);
        } catch (GeneralSecurityException cannotHash) {
            throw new IllegalStateException("Kunde inte hasha lösenordet", cannotHash);
        }
    }

    /**
     * True when the row was made with a lower cost than the one used today, so a successful
     * sign-in can replace it. The password itself is what has to be at hand then.
     */
    public static boolean needsRehash(String stored) {
        if (stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            return Integer.parseInt(parts[1]) < ITERATIONS;
        } catch (NumberFormatException unreadable) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations, int length)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, length * 8);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }
}
