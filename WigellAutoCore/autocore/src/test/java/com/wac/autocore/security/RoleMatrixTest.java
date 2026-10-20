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
    void aStaffMemberMayDoEverythingExceptTheFour() {
        assertFalse(RoleMatrix.allows(Role.STAFF, Operation.DELETE_CUSTOMER));
        assertFalse(RoleMatrix.allows(Role.STAFF, Operation.DELETE_VEHICLE));
        assertFalse(RoleMatrix.allows(Role.STAFF, Operation.DELETE_MECHANIC));
        assertFalse(RoleMatrix.allows(Role.STAFF, Operation.CHANGE_SETTINGS));

        assertEquals(Operation.values().length - 4, countAllowed(Role.STAFF));
        assertTrue(RoleMatrix.allows(Role.STAFF, Operation.CANCEL_WORK_ORDER));
        assertTrue(RoleMatrix.allows(Role.STAFF, Operation.TAKE_PAYMENT));
    }

    @Test
    void aReadOnlyAccountReadsAndNothingElse() {
        assertTrue(RoleMatrix.allows(Role.READ_ONLY, Operation.READ));
        assertEquals(1, countAllowed(Role.READ_ONLY));

        assertFalse(RoleMatrix.allows(Role.READ_ONLY, Operation.CANCEL_WORK_ORDER));
        assertFalse(RoleMatrix.allows(Role.READ_ONLY, Operation.CREATE_CUSTOMER));
        assertFalse(RoleMatrix.allows(Role.READ_ONLY, Operation.TAKE_PAYMENT));
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
