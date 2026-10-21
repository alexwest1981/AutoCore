package com.wac.autocore.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The comparison the window makes. The fixed row is made the way migration 004 makes them, with
 * 210 000 rounds, so an old row is read years after it was written.
 */
class PasswordHashTest {

    /** A password nobody uses: the demo accounts' values never enter the repository. */
    private static final String PASSWORD = "TestLosenord1";

    private static final String OLD_ROW = "pbkdf2$210000$AAECAwQFBgcICQoLDA0ODw=="
            + "$w+2Z/MQIGBDn4rft8lPuBP2kPJ5qFVCMMGbPhzAe1wI=";

    @Test
    void theStoredPasswordIsAccepted() {
        assertTrue(PasswordHash.matches(PASSWORD, OLD_ROW));
    }

    @Test
    void anotherPasswordIsRefused() {
        assertFalse(PasswordHash.matches("TestLosenord2", OLD_ROW));
        assertFalse(PasswordHash.matches("testlosenord1", OLD_ROW));
        assertFalse(PasswordHash.matches("", OLD_ROW));
    }

    @Test
    void aNewHashVerifiesAndCarriesTheCurrentCost() {
        String stored = PasswordHash.hash(PASSWORD);

        assertTrue(stored.startsWith("pbkdf2$" + PasswordHash.ITERATIONS + "$"));
        assertTrue(PasswordHash.matches(PASSWORD, stored));
        assertFalse(PasswordHash.matches("TestLosenord2", stored));
    }

    @Test
    void twoHashesOfTheSamePasswordDiffer() {
        // The salt is per row, so the same password never gives the same text twice.
        assertTrue(!PasswordHash.hash(PASSWORD).equals(PasswordHash.hash(PASSWORD)));
    }

    @Test
    void anOldRowIsMarkedForRehashAndANewOneIsNot() {
        assertTrue(PasswordHash.needsRehash(OLD_ROW), "210 000 varv är under dagens kostnad");
        assertFalse(PasswordHash.needsRehash(PasswordHash.hash(PASSWORD)));
        assertFalse(PasswordHash.needsRehash(null));
        assertFalse(PasswordHash.needsRehash("inte-ett-losenord"));
    }

    @Test
    void anEmptyPasswordIsNotHashed() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHash.hash(""));
        assertThrows(IllegalArgumentException.class, () -> PasswordHash.hash(null));
    }

    @Test
    void aColumnThatCannotBeReadIsANoNotACrash() {
        assertFalse(PasswordHash.matches(PASSWORD, null));
        assertFalse(PasswordHash.matches(null, OLD_ROW));
        assertFalse(PasswordHash.matches(PASSWORD, ""));
        assertFalse(PasswordHash.matches(PASSWORD, PASSWORD));
        assertFalse(PasswordHash.matches(PASSWORD, "bcrypt$210000$abc$def"));
        assertFalse(PasswordHash.matches(PASSWORD, "pbkdf2$inte-ett-tal$abc$def"));
        assertFalse(PasswordHash.matches(PASSWORD, "pbkdf2$210000$inte-base64$def"));
    }
}
