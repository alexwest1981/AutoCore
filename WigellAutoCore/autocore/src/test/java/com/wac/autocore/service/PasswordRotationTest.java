package com.wac.autocore.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wac.autocore.data.Db;
import com.wac.autocore.repository.UserRepository;
import com.wac.autocore.security.Identity;
import com.wac.autocore.security.PasswordHash;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The passwords that arrive from the configuration, tried on an account the proof makes itself.
 * The values are made up for a database that lives in a temporary file while the test runs.
 */
class PasswordRotationTest {

    private File file;

    private UserRepository users;

    private LoginService login;

    /** The account's password before anything is configured. */
    private static final String OLD_PASSWORD = "TestLosenord1";

    private static final String NEW_PASSWORD = "NyttLosenord1";

    @BeforeEach
    void buildDatabase() {
        try {
            file = File.createTempFile("rotation-", ".db");
            file.deleteOnExit();
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
    void aConfiguredPasswordReplacesTheStoredOne() {
        PasswordRotation rotation = new PasswordRotation(users, one("anna", NEW_PASSWORD));

        assertEquals(List.of("anna"), rotation.apply());

        Optional<Identity> signedIn = login.signIn("anna", NEW_PASSWORD);
        assertTrue(signedIn.isPresent());
        assertTrue(login.signIn("anna", OLD_PASSWORD).isEmpty());
    }

    @Test
    void nothingHappensWhenNothingIsConfigured() {
        assertTrue(new PasswordRotation(users, one("anna", "")).apply().isEmpty());
        assertTrue(login.signIn("anna", OLD_PASSWORD).isPresent());
    }

    @Test
    void theSamePasswordTwiceChangesNothingTheSecondTime() {
        Map<String, String> configured = one("anna", NEW_PASSWORD);

        assertEquals(List.of("anna"), new PasswordRotation(users, configured).apply());
        assertTrue(new PasswordRotation(users, configured).apply().isEmpty());
    }

    @Test
    void anAccountTheTableDoesNotHaveIsLeftAlone() {
        PasswordRotation rotation = new PasswordRotation(users, one("nobody", "NyttLosenord1"));

        assertTrue(rotation.apply().isEmpty());
    }

    @Test
    void aShortPasswordIsRefusedBeforeItIsStored() {
        PasswordRotation rotation = new PasswordRotation(users, one("anna", "kort1"));

        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class, rotation::apply);
        assertTrue(refused.getMessage().contains("anna"));
        // The value itself must not travel in the message.
        assertTrue(!refused.getMessage().contains("kort1"));
    }

    @Test
    void aConfiguredPasswordIsStoredAtTheCurrentCost() {
        new PasswordRotation(users, one("anna", NEW_PASSWORD)).apply();

        String stored = users.findByUsername("anna").orElseThrow().passwordHash();
        assertTrue(stored.startsWith("pbkdf2$" + PasswordHash.ITERATIONS + "$"));
        assertTrue(!PasswordHash.needsRehash(stored));
    }

    /** An account of the proof's own, stored the way migration 004 stores one. */
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

    private static Map<String, String> one(String account, String password) {
        Map<String, String> configured = new LinkedHashMap<String, String>();
        configured.put(account, password);
        return configured;
    }
}
