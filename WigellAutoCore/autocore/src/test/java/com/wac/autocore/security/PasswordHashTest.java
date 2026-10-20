package com.wac.autocore.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The hashes are the ones migration 004 writes, so this is the same comparison the window makes. */
class PasswordHashTest {

    private static final String ADMIN = "pbkdf2$210000$rqrZS3wEeLjsLfnxODMimQ=="
            + "$OQ40gpCv4Pven7WMDcT3I4OBenMoi45FOY5aIpAayfE=";

    @Test
    void theStoredPasswordIsAccepted() {
        assertTrue(PasswordHash.matches("Admin123", ADMIN));
    }

    @Test
    void anotherPasswordIsRefused() {
        assertFalse(PasswordHash.matches("Admin1234", ADMIN));
        assertFalse(PasswordHash.matches("admin123", ADMIN));
        assertFalse(PasswordHash.matches("", ADMIN));
    }

    @Test
    void aColumnThatCannotBeReadIsANoNotACrash() {
        assertFalse(PasswordHash.matches("Admin123", null));
        assertFalse(PasswordHash.matches(null, ADMIN));
        assertFalse(PasswordHash.matches("Admin123", ""));
        assertFalse(PasswordHash.matches("Admin123", "Admin123"));
        assertFalse(PasswordHash.matches("Admin123", "bcrypt$210000$abc$def"));
        assertFalse(PasswordHash.matches("Admin123", "pbkdf2$inte-ett-tal$abc$def"));
        assertFalse(PasswordHash.matches("Admin123", "pbkdf2$210000$inte-base64$def"));
    }
}
