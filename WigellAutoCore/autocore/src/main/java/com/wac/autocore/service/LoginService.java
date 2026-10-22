package com.wac.autocore.service;

import com.wac.autocore.repository.UserRepository;
import com.wac.autocore.repository.UserRepository.UserRow;
import com.wac.autocore.security.Identity;
import com.wac.autocore.security.PasswordHash;
import com.wac.autocore.security.Role;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Signing in: a name and a password become an identity, or nothing at all.
 *
 * Both refusals — no such name, wrong password — answer the same way on purpose. The window must
 * not tell an outsider which names exist.
 */
public class LoginService {

    private final UserRepository users;

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    public LoginService(UserRepository users) {
        this.users = users;
    }

    /**
     * The identity the name and password belong to, or empty when they do not belong together.
     * A role text the system does not know is refused loudly by {@link Role#fromText}: a broken row
     * is a fault to fix, not a person to keep out.
     */
    public Optional<Identity> signIn(String username, String password) {

        if (password == null || password.isEmpty()) {
            return Optional.empty();
        }

        Optional<UserRow> row = users.findByUsername(username);

        if (row.isEmpty() || !PasswordHash.matches(password, row.get().passwordHash())) {
            log.warn("Inloggning nekad för {}", username);
            return Optional.empty();
        }

        // The one moment the password is at hand is a successful sign-in, so that is when an old
        // row is brought up to the current cost. Nothing else changes.
        if (PasswordHash.needsRehash(row.get().passwordHash())) {
            users.savePasswordHash(row.get().username(), PasswordHash.hash(password));
        }

        Identity identity = new Identity(row.get().username(), Role.fromText(row.get().role()));
        log.info("Inloggning: {} ({})", identity.username(), identity.role());

        return Optional.of(identity);
    }
}
