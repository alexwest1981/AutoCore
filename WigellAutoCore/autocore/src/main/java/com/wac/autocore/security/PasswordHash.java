package com.wac.autocore.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Compares a typed password with the hash the users table keeps. The text in the column is
 * {@code pbkdf2$<iterations>$<salt>$<hash>}, base64 for the two last parts, and the salt travels
 * with the row so every account may have its own.
 *
 * Nothing here writes a hash. The demo accounts come from the migration, which is the only place
 * that decides how a password is turned into a hash.
 */
public final class PasswordHash {

    private static final String PREFIX = "pbkdf2";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

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

            PBEKeySpec spec = new PBEKeySpec(typed.toCharArray(), salt, iterations, expected.length * 8);
            try {
                byte[] actual = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
                // Same time for a wrong password as for a right one, so the clock tells nothing.
                return MessageDigest.isEqual(expected, actual);
            } finally {
                spec.clearPassword();
            }
        } catch (IllegalArgumentException | GeneralSecurityException unreadable) {
            return false;
        }
    }
}
