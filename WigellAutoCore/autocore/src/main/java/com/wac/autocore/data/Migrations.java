package com.wac.autocore.data;

import com.wac.autocore.exception.DatabaseInitException;
import com.wac.autocore.util.Resources;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * The schema's history. One file per change, run in order, and a register of which ones have
 * run. The register is what makes a second start cost nothing.
 *
 * In a file, every statement ends with a semicolon and lines starting with -- are comments.
 */
final class Migrations {

    private static final String RESOURCE_PATH = "/com/wac/autocore/db/migrations/";

    /** In order. A new change gets the next number and is added last. */
    private static final String[] FILES = {
        "001_baslinjen.sql",
        "002_nycklar.sql",
        "003_ore.sql",
        "004_users.sql"
    };

    /**
     * The columns the baseline expects and a database from an earlier sprint does not have. The
     * baseline's CREATE TABLE IF NOT EXISTS leaves a table it finds alone, so those columns are
     * added here, and only the ones that are missing. The same columns stand in
     * 001_baslinjen.sql — a column added there belongs here too, and the L3 proof fails if the two
     * drift apart.
     */
    private static final String[][] ADDED_COLUMNS = {
        {"work_orders", "type", "TEXT"},
        {"work_orders", "original_work_order_id", "INTEGER"},
        {"work_orders", "planned_date", "TEXT"},
        {"work_orders", "planned_time", "TEXT"},
        {"work_orders", "customer_instructions", "TEXT"},
        {"work_orders", "other_comments", "TEXT"},
        {"booking_service_items", "package_name", "TEXT"},
        {"invoice_lines", "package_name", "TEXT"},
        {"work_order_service_items", "package_name", "TEXT"}
    };

    private Migrations() {}

    static void apply(Db db) {
        try (Connection connection = db.getConnection()) {
            createRegister(connection);

            List<Integer> done = applied(connection);

            // Only when a file is going to run: a database that has them all is left alone.
            if (done.size() < FILES.length) {
                addMissingColumns(connection);
            }

            for (int i = 0; i < FILES.length; i++) {
                int version = i + 1;
                if (!done.contains(Integer.valueOf(version))) {
                    run(connection, version, FILES[i]);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseInitException("Databasen kunde inte förberedas: " + e.getMessage(), e);
        }
    }

    /** Runs one file, and writes it off in the same transaction as the change itself. */
    private static void run(Connection connection, int version, String file) throws SQLException {
        List<String> statements = statements(read(file));

        // A migration may rebuild a table, and a rebuild drops it. The keys have to be off for
        // that, and they cannot be switched inside a transaction.
        setKeys(connection, false);
        try {
            connection.setAutoCommit(false);
            try {
                for (String sql : statements) {
                    execute(connection, sql);
                }
                checkKeys(connection);
                record(connection, version, file);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw new SQLException(file + ": " + e.getMessage(), e);
            } finally {
                connection.setAutoCommit(true);
            }
        } finally {
            setKeys(connection, true);
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void createRegister(Connection connection) throws SQLException {
        execute(connection, "CREATE TABLE IF NOT EXISTS schema_version ("
                + "version INTEGER PRIMARY KEY, "
                + "name TEXT NOT NULL, "
                + "applied_at TEXT NOT NULL)");
    }

    private static List<Integer> applied(Connection connection) throws SQLException {
        List<Integer> versions = new ArrayList<Integer>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT version FROM schema_version")) {
            while (rows.next()) {
                versions.add(Integer.valueOf(rows.getInt(1)));
            }
        }
        return versions;
    }

    /**
     * Adds the columns an older database is missing, so the file's own statements can run. A
     * table that is not there yet is left alone: the baseline creates it whole, with the columns
     * already in place.
     */
    private static void addMissingColumns(Connection connection) throws SQLException {
        for (String[] column : ADDED_COLUMNS) {
            if (!tableExists(connection, column[0])) {
                continue;
            }
            if (!hasColumn(connection, column[0], column[1])) {
                execute(connection, "ALTER TABLE " + column[0] + " ADD COLUMN " + column[1]
                        + " " + column[2]);
            }
        }
    }

    /** True when the table is in the file. Nothing is created here, only looked for. */
    private static boolean tableExists(Connection connection, String table) throws SQLException {
        String sql = "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    private static boolean hasColumn(Connection connection, String table, String column)
            throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rows.next()) {
                if (column.equals(rows.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void record(Connection connection, int version, String name) throws SQLException {
        String sql = "INSERT INTO schema_version (version, name, applied_at) "
                + "VALUES (?, ?, datetime('now'))";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, version);
            statement.setString(2, name);
            statement.executeUpdate();
        }
    }

    private static void setKeys(Connection connection, boolean on) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = " + (on ? "ON" : "OFF"));
        }
    }

    /** Lets the database go through every row before the change is kept. */
    private static void checkKeys(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("PRAGMA foreign_key_check")) {
            if (rows.next()) {
                throw new SQLException("Raden " + rows.getInt(2) + " i " + rows.getString(1)
                        + " pekar på något som inte finns.");
            }
        }
    }

    private static String read(String file) {
        URL url = Resources.find(RESOURCE_PATH + file);
        String sql = Resources.text(url);
        if (sql.isEmpty()) {
            throw new DatabaseInitException("Migreringen " + file + " gick inte att läsa.");
        }
        return sql;
    }

    private static List<String> statements(String sql) {
        List<String> out = new ArrayList<String>();
        for (String part : sql.split(";")) {
            String statement = withoutComments(part);
            if (!statement.isEmpty()) {
                out.add(statement);
            }
        }
        return out;
    }

    private static String withoutComments(String part) {
        StringBuilder out = new StringBuilder();
        for (String line : part.split("\n")) {
            if (!line.trim().startsWith("--")) {
                out.append(line).append('\n');
            }
        }
        return out.toString().trim();
    }
}
