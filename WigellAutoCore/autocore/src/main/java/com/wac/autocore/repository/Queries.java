package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Opens the connection, runs the statement and closes everything again, so each repository keeps
 * only its SQL and its row mapping. A failed statement leaves here as DataAccessException.
 *
 * Each method has a twin that takes the connection from the caller instead, so several statements
 * can be part of one transaction. The twin leaves the frame to whoever called it.
 */
final class Queries {

    private final Db db;

    /** Puts the values into the statement's placeholders. */
    interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    /** Turns one row into an object. */
    interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }

    Queries(Db db) {
        this.db = db;
    }

    /** Runs an INSERT, UPDATE or DELETE that takes no values. */
    void write(String sql) {
        write(sql, null);
    }

    /** Runs an INSERT, UPDATE or DELETE. */
    void write(String sql, Binder binder) {
        try (Connection connection = db.getConnection()) {
            write(connection, sql, binder);
        } catch (SQLException e) {
            throw new DataAccessException("Could not write to the database.", e);
        }
    }

    /** Runs the statement on a connection the caller owns, so it can be part of a transaction. */
    static void write(Connection connection, String sql, Binder binder) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(statement);
            }
            statement.executeUpdate();
        }
    }

    /** Runs an INSERT and returns the id the database gave the row, or 0 when it gave none. */
    int insert(String sql, Binder binder) {
        try (Connection connection = db.getConnection()) {
            return insert(connection, sql, binder);
        } catch (SQLException e) {
            throw new DataAccessException("Could not write to the database.", e);
        }
    }

    /** Runs the insert on a connection the caller owns, and returns the id it gave the row. */
    static int insert(Connection connection, String sql, Binder binder) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(statement);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    /**
     * Writes a reference, or NULL when it is zero. A missing reference is not a zero that points at
     * nothing, and the keys in the schema do not accept one.
     */
    static void setReference(PreparedStatement statement, int position, int id) throws SQLException {
        if (id > 0) {
            statement.setInt(position, id);
        } else {
            statement.setNull(position, Types.INTEGER);
        }
    }

    /** The rows the query gives, in the order the database returns them. */
    <T> List<T> read(String sql, RowMapper<T> mapper) {
        return read(sql, null, mapper);
    }

    /** The rows the query gives, with the placeholders bound first. */
    <T> List<T> read(String sql, Binder binder, RowMapper<T> mapper) {
        try (Connection connection = db.getConnection()) {
            return read(connection, sql, binder, mapper);
        } catch (SQLException e) {
            throw new DataAccessException("Could not read from the database.", e);
        }
    }

    /** Reads on a connection the caller owns, so a step sees what the same transaction wrote. */
    static <T> List<T> read(Connection connection, String sql, Binder binder, RowMapper<T> mapper)
            throws SQLException {
        List<T> rows = new ArrayList<T>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(statement);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(mapper.map(resultSet));
                }
            }
        }
        return rows;
    }

    /** The first row the query gives, or null when it gives none. */
    <T> T readOne(String sql, Binder binder, RowMapper<T> mapper) {
        List<T> rows = read(sql, binder, mapper);
        return rows.isEmpty() ? null : rows.get(0);
    }
}
