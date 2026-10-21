package com.wac.autocore.service;

import com.wac.autocore.repository.UserRepository;
import com.wac.autocore.repository.UserRepository.UserRow;
import com.wac.autocore.security.PasswordHash;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The demo accounts' passwords, when someone hands them over out of band instead of taking them
 * from the migration. Each one arrives in an environment variable, is hashed here with a fresh
 * salt, and replaces the stored hash only when it differs from what is already on file.
 *
 * Nothing is written when no variable is set, which is how the migration's accounts keep working
 * untouched. The value never goes to a log and never leaves this class.
 */
public class PasswordRotation {

    /** Short passwords are refused before they are ever stored. */
    static final int MIN_LENGTH = 8;

    private final UserRepository users;

    private final Map<String, String> configured;

    /** @param configured the account name and the password given for it, in that order */
    public PasswordRotation(UserRepository users, Map<String, String> configured) {
        this.users = users;
        this.configured = new LinkedHashMap<String, String>(configured);
    }

    /**
     * Applies the configured passwords. Returns the accounts whose hash was replaced, so the start
     * can say what it did without saying what it was.
     *
     * An account the table does not have is left alone: the migration is what decides which
     * accounts exist.
     */
    public List<String> apply() {
        List<String> changed = new ArrayList<String>();
        for (Map.Entry<String, String> entry : configured.entrySet()) {
            String account = entry.getKey();
            String password = entry.getValue() == null ? "" : entry.getValue().trim();
            if (password.isEmpty()) {
                continue;
            }
            if (password.length() < MIN_LENGTH) {
                throw new IllegalArgumentException(
                        "Lösenordet som gavs för " + account + " är kortare än " + MIN_LENGTH + " tecken.");
            }

            Optional<UserRow> row = users.findByUsername(account);
            if (row.isEmpty()) {
                continue;
            }
            if (PasswordHash.matches(password, row.get().passwordHash())) {
                continue;
            }

            users.savePasswordHash(account, PasswordHash.hash(password));
            changed.add(account);
        }
        return changed;
    }
}
