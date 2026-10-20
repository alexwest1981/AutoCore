package com.wac.autocore.service;

import com.wac.autocore.repository.UserRepository;
import com.wac.autocore.repository.UserRepository.UserRow;
import com.wac.autocore.security.Identity;
import com.wac.autocore.security.PasswordHash;
import com.wac.autocore.security.Role;

import java.util.Optional;

/**
 * Signing in: a name and a password become an identity, or nothing at all.
 *
 * Both refusals — no such name, wrong password — answer the same way on purpose. The window must
 * not tell an outsider which names exist.
 */
public class LoginService {

    private final UserRepository users;

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
            return Optional.empty();
        }
        return Optional.of(new Identity(row.get().username(), Role.fromText(row.get().role())));
    }
}
