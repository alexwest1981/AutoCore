package com.wac.autocore.data;

import com.wac.autocore.exception.DataAccessException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * The frame around a write that touches more than one table: one connection, the caller's step in
 * the middle, and everything or nothing. The one who does the work fills in the step; the frame
 * around it is the same every time.
 */
public abstract class DatabaseWork<T> {

    private final Db db;

    protected DatabaseWork(Db db) {
        this.db = db;
    }

    /** Does the work on the connection the frame opened. */
    protected abstract T step(Connection connection) throws SQLException;

    public final T run() {
        try (Connection connection = db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = step(connection);
                connection.commit();
                return result;
            } catch (SQLException e) {
                connection.rollback();
                throw new DataAccessException("Could not write to the database.", e);
            } catch (RuntimeException e) {
                // A rule that says no in the middle of a write must not leave half of it behind.
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not write to the database.", e);
        }
    }
}
