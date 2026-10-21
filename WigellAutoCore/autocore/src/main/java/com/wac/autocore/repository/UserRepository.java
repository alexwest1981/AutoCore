package com.wac.autocore.repository;

import com.wac.autocore.data.Db;

import java.util.List;
import java.util.Optional;

/**
 * The users table. Reads one row at a time; the password is only ever the stored hash, and
 * {@code LoginService} is what compares it with what someone typed.
 */
public class UserRepository {

    /** One row as the table keeps it. The hash does not leave the core. */
    public record UserRow(String username, String passwordHash, String role) {}

    private static final String COLUMNS = "username, password_hash, role";

    private final Queries queries;

    public UserRepository(Db db) {
        this.queries = new Queries(db);
    }

    /** The row for a username, or empty when there is none. The name is matched as it is stored. */
    public Optional<UserRow> findByUsername(String username) {
        String name = username == null ? "" : username.trim();
        if (name.isEmpty()) {
            return Optional.empty();
        }

        List<UserRow> rows = queries.read(
                "SELECT " + COLUMNS + " FROM users WHERE username = ?",
                statement -> statement.setString(1, name),
                resultSet -> new UserRow(
                        resultSet.getString("username"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("role")));

        return rows.isEmpty() ? Optional.<UserRow>empty() : Optional.of(rows.get(0));
    }

    /** Replaces the stored hash for a user. The password itself never reaches this layer. */
    public void savePasswordHash(String username, String passwordHash) {
        queries.write("UPDATE users SET password_hash = ? WHERE username = ?",
                statement -> {
                    statement.setString(1, passwordHash);
                    statement.setString(2, username);
                });
    }
}
