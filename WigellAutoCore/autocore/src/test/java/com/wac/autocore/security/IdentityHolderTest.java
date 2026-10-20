package com.wac.autocore.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IdentityHolderTest {

    @Test
    void emptyUntilSomeoneSignsIn() {
        assertTrue(new IdentityHolder().current().isEmpty());
    }

    @Test
    void holdsTheIdentityUntilCleared() {
        IdentityHolder holder = new IdentityHolder();
        holder.set(new Identity("anna", Role.STAFF));

        assertEquals("anna", holder.current().get().username());
        assertEquals(Role.STAFF, holder.current().get().role());

        holder.clear();
        assertFalse(holder.current().isPresent());
    }

    @Test
    void anIdentityNeedsNameAndRole() {
        assertThrows(IllegalArgumentException.class, () -> new Identity(" ", Role.ADMIN));
        assertThrows(IllegalArgumentException.class, () -> new Identity("anna", null));
    }

    @Test
    void anUnknownRoleIsRefusedNotGuessed() {
        assertEquals(Role.READ_ONLY, Role.fromText("read_only"));
        assertThrows(IllegalArgumentException.class, () -> Role.fromText("boss"));
    }
}