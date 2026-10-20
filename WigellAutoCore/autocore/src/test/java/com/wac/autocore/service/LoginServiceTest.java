package com.wac.autocore.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wac.autocore.data.Db;
import com.wac.autocore.repository.UserRepository;
import com.wac.autocore.security.Identity;
import com.wac.autocore.security.Role;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;

/**
 * Signing in against a database of the proof's own, built from the migrations. The accounts are
 * the three migration 004 writes, so this is the same path the window takes.
 */
class LoginServiceTest {

    private File file;

    private LoginService login;

    @BeforeEach
    void buildDatabase() {
        try {
            file = File.createTempFile("login-", ".db");
            file.deleteOnExit();
            // Nothing to copy: the program builds the database from the migration files.
            file.delete();

            Db db = new Db(file.getAbsolutePath());
            db.ensureReady();
            login = new LoginService(new UserRepository(db));
        } catch (Exception cannotStart) {
            throw new IllegalStateException("Kunde inte bygga provets databas", cannotStart);
        }
    }

    @AfterEach
    void removeDatabase() {
        if (file != null) {
            file.delete();
        }
    }

    @Test
    void theAdminSignsInWithTheRightPassword() {
        Optional<Identity> identity = login.signIn("admin", "Admin123");

        assertTrue(identity.isPresent());
        assertEquals("admin", identity.get().username());
        assertEquals(Role.ADMIN, identity.get().role());
    }

    @Test
    void eachAccountGetsItsOwnRole() {
        assertEquals(Role.STAFF, login.signIn("staff", "Staff123").orElseThrow().role());
        assertEquals(Role.READ_ONLY, login.signIn("readonly", "ReadOnly123").orElseThrow().role());
    }

    @Test
    void aWrongPasswordIsRefused() {
        assertTrue(login.signIn("admin", "Admin1234").isEmpty());
        assertTrue(login.signIn("admin", "").isEmpty());
    }

    @Test
    void anUnknownNameIsRefusedTheSameWayAsAWrongPassword() {
        assertTrue(login.signIn("nobody", "Admin123").isEmpty());
        assertTrue(login.signIn("", "Admin123").isEmpty());
        assertTrue(login.signIn(null, "Admin123").isEmpty());
    }

    @Test
    void theNameIsReadApartFromSurroundingSpaces() {
        assertTrue(login.signIn("  admin  ", "Admin123").isPresent());
    }
}
