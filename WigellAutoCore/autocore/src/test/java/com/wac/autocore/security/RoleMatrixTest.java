package com.wac.autocore.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RoleMatrixTest {

    @Test
    void theAdministratorMayDoEverything() {
        for (Operation operation : Operation.values()) {
            assertTrue(RoleMatrix.allows(Role.ADMIN, operation), "administratören nekas " + operation);
        }
    }

    @Test
    void aStaffMemberMayDoEverythingExceptTheFive() {
        assertFalse(RoleMatrix.allows(Role.STAFF, Operation.DELETE_CUSTOMER));
        assertFalse(RoleMatrix.allows(Role.STAFF, Operation.DELETE_VEHICLE));
        assertFalse(RoleMatrix.allows(Role.STAFF, Operation.DELETE_MECHANIC));
        assertFalse(RoleMatrix.allows(Role.STAFF, Operation.DELETE_WORK_ORDER));
        assertFalse(RoleMatrix.allows(Role.STAFF, Operation.CHANGE_SETTINGS));

        assertEquals(Operation.values().length - 5, countAllowed(Role.STAFF));
        assertTrue(RoleMatrix.allows(Role.STAFF, Operation.CANCEL_WORK_ORDER));
        assertTrue(RoleMatrix.allows(Role.STAFF, Operation.TAKE_PAYMENT));
    }

    @Test
    void cancellingAnOrderIsNotTheSameAsRemovingIt() {
        // A staff member calls off an order during the day, which is ordinary work. Removing the row
        // afterwards is the administrator's answer for a mistake.
        assertTrue(RoleMatrix.allows(Role.STAFF, Operation.CANCEL_WORK_ORDER));
        assertFalse(RoleMatrix.allows(Role.STAFF, Operation.DELETE_WORK_ORDER));

        assertTrue(RoleMatrix.allows(Role.ADMIN, Operation.DELETE_WORK_ORDER));
        assertFalse(RoleMatrix.allows(Role.READ_ONLY, Operation.DELETE_WORK_ORDER));
    }

    @Test
    void aReadOnlyAccountReadsAndChoosesItsLanguage() {
        assertTrue(RoleMatrix.allows(Role.READ_ONLY, Operation.READ));
        assertTrue(RoleMatrix.allows(Role.READ_ONLY, Operation.CHANGE_LANGUAGE));
        assertEquals(2, countAllowed(Role.READ_ONLY));

        assertFalse(RoleMatrix.allows(Role.READ_ONLY, Operation.CANCEL_WORK_ORDER));
        assertFalse(RoleMatrix.allows(Role.READ_ONLY, Operation.CREATE_CUSTOMER));
        assertFalse(RoleMatrix.allows(Role.READ_ONLY, Operation.TAKE_PAYMENT));
        assertFalse(RoleMatrix.allows(Role.READ_ONLY, Operation.CHANGE_SETTINGS));
    }

    @Test
    void theLanguageIsNotABusinessSetting() {
        // The two are neighbours in the settings table but not in the matrix: choosing a language is
        // something everyone does, changing the workshop's settings is not.
        for (Role role : Role.values()) {
            assertTrue(RoleMatrix.allows(role, Operation.CHANGE_LANGUAGE), "språkval för " + role);
        }

        assertFalse(RoleMatrix.allows(Role.STAFF, Operation.CHANGE_SETTINGS));
        assertFalse(RoleMatrix.allows(Role.READ_ONLY, Operation.CHANGE_SETTINGS));
        assertTrue(RoleMatrix.allows(Role.ADMIN, Operation.CHANGE_SETTINGS));
    }

    @Test
    void theRuleIsTheSameEveryTimeTheMatrixIsAsked() {
        assertTrue(RoleMatrix.allows(Role.ADMIN, Operation.CANCEL_WORK_ORDER));
        assertFalse(RoleMatrix.allows(Role.READ_ONLY, Operation.CANCEL_WORK_ORDER));
    }

    @Test
    void whatIsNotARoleIsRefused() {
        assertFalse(RoleMatrix.allows(null, Operation.READ));
        assertFalse(RoleMatrix.allows(Role.ADMIN, null));
    }

    @Test
    void theMatrixCanBeReadAsText() {
        String text = RoleMatrix.describe();
        assertTrue(text.contains("ADMIN"));
        assertTrue(text.contains("STAFF"));
        assertTrue(text.contains("READ_ONLY"));
        assertTrue(text.contains("bara läsa"));
    }

    private static int countAllowed(Role role) {
        int allowed = 0;
        for (Operation operation : Operation.values()) {
            if (RoleMatrix.allows(role, operation)) {
                allowed++;
            }
        }
        return allowed;
    }
}
