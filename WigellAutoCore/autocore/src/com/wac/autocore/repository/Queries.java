package com.wac.autocore.repository;

import com.wac.autocore.data.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Opens the connection, runs the statement and closes everything again, so each repository keeps
 * only its SQL and its row mapping.
 */
final class Queries {

    /** Puts the values into the statement's placeholders. */
    interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    /** Turns one row into an object. */
    interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }

    private Queries() {}

    /** Runs an INSERT, UPDATE or DELETE that takes no values. */
    static void write(String sql) throws SQLException {
        write(sql, null);
    }

    /** Runs an INSERT, UPDATE or DELETE. */
    static void write(String sql, Binder binder) throws SQLException {
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(statement);
            }
            statement.executeUpdate();
        }
    }

    /** Runs an INSERT and returns the id the database gave the row, or 0 when it gave none. */
    static int insert(String sql, Binder binder) throws SQLException {
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(statement);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    /** The rows the query gives, in the order the database returns them. */
    static <T> List<T> read(String sql, RowMapper<T> mapper) throws SQLException {
        return read(sql, null, mapper);
    }

    /** The rows the query gives, with the placeholders bound first. */
    static <T> List<T> read(String sql, Binder binder, RowMapper<T> mapper) throws SQLException {
        List<T> rows = new ArrayList<T>();
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
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
    static <T> T readOne(String sql, Binder binder, RowMapper<T> mapper) throws SQLException {
        List<T> rows = read(sql, binder, mapper);
        return rows.isEmpty() ? null : rows.get(0);
    }
}
