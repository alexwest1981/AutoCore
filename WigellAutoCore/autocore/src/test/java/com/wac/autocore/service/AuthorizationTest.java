package com.wac.autocore.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wac.autocore.data.Db;
import com.wac.autocore.exception.AccessDeniedException;
import com.wac.autocore.exception.AppException;
import com.wac.autocore.exception.NotAuthenticatedException;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.security.Identity;
import com.wac.autocore.security.IdentityHolder;
import com.wac.autocore.security.Role;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;

/**
 * E3-5: the rules hold when the core is called directly, without the window. Three cases are
 * tried for the same operation: nobody signed in, a role that may not, and a role that may.
 *
 * The two refusals are checked to be different answers, which is what the order asks for: not
 * signed in is not the same as signed in without the right.
 */
class AuthorizationTest {

    private File file;

    private Db db;

    private IdentityHolder identity;

    @BeforeEach
    void buildDatabase() {
        try {
            file = File.createTempFile("authorization-", ".db");
            file.deleteOnExit();
            file.delete();

            db = new Db(file.getAbsolutePath());
            db.ensureReady();
            identity = new IdentityHolder();
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

    /** A signed-in garage. Nobody is in the holder until {@link #signIn} is called. */
    private GarageSystem garage() {
        return new GuardedGarageSystem(new SqliteRepositoryFactory(db), identity);
    }

    private void signIn(String name, Role role) {
        identity.set(new Identity(name, role));
    }

    @Test
    void nobodySignedInIsToldToSignIn() {
        GarageSystem garage = garage();

        NotAuthenticatedException refused = assertThrows(NotAuthenticatedException.class,
                () -> garage.createCustomer("Ingen inloggad", "070-1111111", "ingen@exempel.com"));

        assertEquals("dialog.access.sign_in", refused.getMessageKey());
    }

    @Test
    void aReadOnlyAccountIsToldItMayNot() {
        signIn("lasare", Role.READ_ONLY);
        GarageSystem garage = garage();

        AccessDeniedException refused = assertThrows(AccessDeniedException.class,
                () -> garage.createCustomer("Nekad kund", "070-2222222", "nekad@exempel.com"));

        assertEquals("dialog.access.denied", refused.getMessageKey());
    }

    @Test
    void anAdministratorIsLetThrough() {
        signIn("chefen", Role.ADMIN);
        GarageSystem garage = garage();

        garage.createCustomer("Tillåten kund", "070-3333333", "tillaten@exempel.com");

        assertTrue(garage.getCustomers().stream()
                .anyMatch(customer -> "Tillåten kund".equals(customer.getName())));
    }

    @Test
    void theTwoRefusalsAreNotTheSameAnswer() {
        GarageSystem garage = garage();

        AppException nobody = assertThrows(AppException.class,
                () -> garage.createCustomer("Ingen", "070-4444444", "ingen@exempel.com"));

        signIn("lasare", Role.READ_ONLY);
        AppException withoutRight = assertThrows(AppException.class,
                () -> garage.createCustomer("Nekad", "070-5555555", "nekad@exempel.com"));

        assertNotEquals(nobody.getClass(), withoutRight.getClass());
        assertNotEquals(nobody.getMessageKey(), withoutRight.getMessageKey());
    }

    @Test
    void theFourExceptionsHoldForAStaffMember() {
        signIn("verkstaden", Role.STAFF);
        GarageSystem garage = garage();

        // Staff may work with customers, vehicles and mechanics during the day...
        assertEquals(true, garage.getCustomers().stream().findFirst().isPresent());

        // ...but not remove them, and not change the settings.
        assertThrows(AccessDeniedException.class, () -> garage.deleteCustomer(1));
        assertThrows(AccessDeniedException.class, () -> garage.deleteVehicle(1));
        assertThrows(AccessDeniedException.class, () -> garage.deleteMechanic(1));
        assertThrows(AccessDeniedException.class, () -> garage.saveSetting("language", "sv"));
    }

    @Test
    void aReadOnlyAccountMayReadEverything() {
        signIn("lasare", Role.READ_ONLY);
        GarageSystem garage = garage();

        assertFalse(garage.getCustomers().isEmpty());
        assertFalse(garage.getWorkOrders().isEmpty());
        // No invoice in the seed, but the read must be allowed rather than refused.
        assertDoesNotThrow(garage::getInvoices);
    }

    @Test
    void aRefusedWriteLeavesNothingBehind() {
        signIn("lasare", Role.READ_ONLY);
        GarageSystem garage = garage();
        int before = garage.getCustomers().size();

        assertThrows(AccessDeniedException.class,
                () -> garage.createCustomer("Ska inte finnas", "070-6666666", "nej@exempel.com"));

        assertEquals(before, garage.getCustomers().size());
        assertTrue(garage.getCustomers().stream()
                .noneMatch(customer -> "Ska inte finnas".equals(customer.getName())));
    }

    @Test
    void theHolderIsWhatDecidesNotTheGarageObject() {
        GarageSystem garage = garage();

        assertThrows(NotAuthenticatedException.class,
                () -> garage.createCustomer("Ingen", "070-7777777", "ingen@exempel.com"));

        // Same garage object, identity added afterwards: the guard asks the holder each time.
        signIn("chefen", Role.ADMIN);
        garage.createCustomer("Nu tillåten", "070-8888888", "nu@exempel.com");

        assertTrue(garage.getCustomers().stream()
                .anyMatch(customer -> "Nu tillåten".equals(customer.getName())));
    }

    @Test
    void signingOutClosesTheDoorAgain() {
        signIn("chefen", Role.ADMIN);
        GarageSystem garage = garage();
        garage.createCustomer("Före utloggning", "070-9999999", "fore@exempel.com");

        identity.clear();

        assertThrows(NotAuthenticatedException.class,
                () -> garage.createCustomer("Efter utloggning", "070-1010101", "efter@exempel.com"));
    }

    @Test
    void anIdentityIsRequiredByTheHolderItself() {
        Optional<Identity> nobody = identity.current();

        assertTrue(nobody.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> identity.set(null));
    }

    @Test
    void nobodySignedInCannotReadEither() {
        GarageSystem garage = garage();

        // The same answer as for a write, and the same text, so the caller is told to sign in
        // rather than that the role is wrong.
        NotAuthenticatedException lists = assertThrows(NotAuthenticatedException.class, garage::getCustomers);
        assertEquals("dialog.access.sign_in", lists.getMessageKey());

        assertThrows(NotAuthenticatedException.class, garage::getWorkOrders);
        assertThrows(NotAuthenticatedException.class, () -> garage.findCustomer(1));
        assertThrows(NotAuthenticatedException.class, () -> garage.getSetting("language", "sv"));
        assertThrows(NotAuthenticatedException.class, () -> garage.canDeleteCustomer(1));
    }

    @Test
    void everyRoleMayReadOnceSignedIn() {
        for (Role role : Role.values()) {
            signIn("lasare-" + role, role);

            GarageSystem garage = garage();

            assertDoesNotThrow(garage::getCustomers, "getCustomers för " + role);
            assertDoesNotThrow(garage::getVehicles, "getVehicles för " + role);
            assertDoesNotThrow(garage::getBookings, "getBookings för " + role);
            assertDoesNotThrow(garage::getWorkOrders, "getWorkOrders för " + role);
            assertDoesNotThrow(garage::getServiceItems, "getServiceItems för " + role);
            assertDoesNotThrow(garage::getMechanics, "getMechanics för " + role);
            assertDoesNotThrow(() -> garage.findCustomer(1), "findCustomer för " + role);
        }
    }

    @Test
    void signingOutClosesTheReadingToo() {
        signIn("lasare", Role.READ_ONLY);
        GarageSystem garage = garage();
        assertDoesNotThrow(garage::getCustomers);

        identity.clear();

        assertThrows(NotAuthenticatedException.class, garage::getCustomers);
    }

    @Test
    void everyRoleMayKeepItsLanguageChoice() {
        for (Role role : Role.values()) {
            signIn("sprak-" + role, role);

            GarageSystem garage = garage();

            assertDoesNotThrow(() -> garage.saveLanguage("en"), "saveLanguage för " + role);
            assertEquals("en", garage.getSetting(GarageSystem.LANGUAGE_KEY, "sv"));
        }
    }

    @Test
    void aLanguageChoiceIsStillAWriteNobodyUnsignedCanMake() {
        GarageSystem garage = garage();

        assertThrows(NotAuthenticatedException.class, () -> garage.saveLanguage("en"));
    }

    @Test
    void savingTheSettingsStaysWithTheAdministrator() {
        signIn("verkstaden", Role.STAFF);
        GarageSystem garage = garage();

        // Choosing a language is allowed, changing the workshop's settings is not. They are two
        // operations even though both end up in the settings table.
        assertDoesNotThrow(() -> garage.saveLanguage("en"));
        assertThrows(AccessDeniedException.class, () -> garage.saveSetting("language", "en"));
    }
}
