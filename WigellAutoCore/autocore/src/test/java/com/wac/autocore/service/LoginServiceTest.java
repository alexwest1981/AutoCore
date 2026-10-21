package com.wac.autocore.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wac.autocore.data.Db;
import com.wac.autocore.repository.UserRepository;
import com.wac.autocore.security.Identity;
import com.wac.autocore.security.PasswordHash;
import com.wac.autocore.security.Role;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;

/**
 * Signing in against a database of the proof's own, built from the migrations. The account used
 * for signing in is made here with a password nobody uses, so the demo accounts' values never
 * enter the repository.
 */
class LoginServiceTest {

    private File file;

    private LoginService login;

    private UserRepository users;

    /** A password nobody uses, stored at 210 000 rounds the way migration 004 stores them. */
    private static final String FIXTURE_PASSWORD = "TestLosenord1";

    @BeforeEach
    void buildDatabase() {
        try {
            file = File.createTempFile("login-", ".db");
            file.deleteOnExit();
            // Nothing to copy: the program builds the database from the migration files.
            file.delete();

            Db db = new Db(file.getAbsolutePath());
            db.ensureReady();
            users = new UserRepository(db);
            login = new LoginService(users);
            insertFixtureAccount(db);
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
    void theRightPasswordGivesTheAccountAndItsRole() {
        Optional<Identity> identity = login.signIn("anna", FIXTURE_PASSWORD);

        assertTrue(identity.isPresent());
        assertEquals("anna", identity.get().username());
        assertEquals(Role.STAFF, identity.get().role());
    }

    @Test
    void theMigrationStillCreatesThreeAccountsWithOneRoleEach() {
        assertEquals(Role.ADMIN, Role.fromText(users.findByUsername("admin").orElseThrow().role()));
        assertEquals(Role.STAFF, Role.fromText(users.findByUsername("staff").orElseThrow().role()));
        assertEquals(Role.READ_ONLY, Role.fromText(users.findByUsername("readonly").orElseThrow().role()));
    }

    @Test
    void aWrongPasswordIsRefused() {
        assertTrue(login.signIn("anna", "TestLosenord2").isEmpty());
        assertTrue(login.signIn("anna", "").isEmpty());
    }

    @Test
    void anUnknownNameIsRefusedTheSameWayAsAWrongPassword() {
        assertTrue(login.signIn("nobody", FIXTURE_PASSWORD).isEmpty());
        assertTrue(login.signIn("", FIXTURE_PASSWORD).isEmpty());
        assertTrue(login.signIn(null, FIXTURE_PASSWORD).isEmpty());
    }

    @Test
    void theNameIsReadApartFromSurroundingSpaces() {
        assertTrue(login.signIn("  anna  ", FIXTURE_PASSWORD).isPresent());
    }

    /**
     * A row written before today's cost is replaced on the first successful sign-in, and only then:
     * the password is the only thing that can make a new hash.
     */
    @Test
    void anOldRowIsBroughtUpToTheCurrentCostWhenItsOwnerSignsIn() {
        String before = users.findByUsername("anna").orElseThrow().passwordHash();
        assertTrue(PasswordHash.needsRehash(before));

        login.signIn("anna", FIXTURE_PASSWORD);

        String after = users.findByUsername("anna").orElseThrow().passwordHash();
        assertTrue(!PasswordHash.needsRehash(after));
        assertTrue(after.startsWith("pbkdf2$" + PasswordHash.ITERATIONS + "$"));
        assertTrue(PasswordHash.matches(FIXTURE_PASSWORD, after));
    }

    /** One account of the proof's own, with the 210 000 rounds migration 004 uses. */
    private static void insertFixtureAccount(Db db) {
        String hash = "pbkdf2$210000$AAECAwQFBgcICQoLDA0ODw=="
                + "$w+2Z/MQIGBDn4rft8lPuBP2kPJ5qFVCMMGbPhzAe1wI=";
        try (java.sql.Connection connection = db.getConnection();
             java.sql.PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO users (username, password_hash, role, created_at) VALUES (?, ?, ?, ?)")) {
            insert.setString(1, "anna");
            insert.setString(2, hash);
            insert.setString(3, "STAFF");
            insert.setString(4, "2026-10-21");
            insert.executeUpdate();
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Kunde inte lägga in provkontot", e);
        }
    }
}
