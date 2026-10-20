package com.wac.autocore.security;

/** Who is using the system: a username and a role. No password, the identity is what is left after login. */
public record Identity(String username, Role role) {

    public Identity {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("An identity needs a username.");
        }
        if (role == null) {
            throw new IllegalArgumentException("An identity needs a role.");
        }
    }
}