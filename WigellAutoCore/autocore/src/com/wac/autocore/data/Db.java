package com.wac.autocore.data;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Db {

    private static final String DEFAULT_PATH = "data/autocore.db";

    private static String databasePath = DEFAULT_PATH;

    private static boolean ready = false;

    /** The file to use: the first argument, or the application's own. */
    public static String pathFrom(String[] args) {
        return args.length > 0 ? args[0] : DEFAULT_PATH;
    }

    /** Points the storage at another file. Another file means another database to prepare. */
    public static void use(String databasePath) {
        if (!databasePath.equals(Db.databasePath)) {
            Db.databasePath = databasePath;
            ready = false;
        }
    }

    public static Connection getConnection() throws SQLException {
        File file = new File(databasePath);
        File directory = file.getParentFile();
        if (directory != null && !directory.exists()) {
            directory.mkdirs();
        }

        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);

        // SQLite leaves the keys switched off until the connection is told otherwise.
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public static void ensureReady() {
        if (ready) {
            return;
        }
        ready = true;
        initTables();
        SeedData.seedIfEmpty();
    }

    /** Makes sure the database holds the schema the code expects. */
    public static void initTables() {
        Migrations.apply();

        System.out.println("Databas redo: " + databasePath);

        // The space in the registration number goes in at the right spot, which SQL cannot do.
        normalizeRegistrationNumbers();
        SeedData.seedIfEmpty();
    }

    /** Fixes registration numbers saved before the model started normalizing them. */
    private static void normalizeRegistrationNumbers() {
        java.util.List<Integer> ids = new java.util.ArrayList<Integer>();
        java.util.List<String> numbers = new java.util.ArrayList<String>();

        try (java.sql.Connection connection = getConnection()) {
            java.sql.Statement read = connection.createStatement();
            java.sql.ResultSet rows = read.executeQuery(
                    "SELECT id, registration_number FROM vehicles WHERE registration_number IS NOT NULL");
            while (rows.next()) {
                ids.add(Integer.valueOf(rows.getInt(1)));
                numbers.add(rows.getString(2));
            }
            rows.close();
            read.close();

            java.sql.PreparedStatement write = connection.prepareStatement(
                    "UPDATE vehicles SET registration_number = ? WHERE id = ?");
            for (int i = 0; i < ids.size(); i++) {
                String normalized = com.wac.autocore.model.Vehicle.normalizeRegistrationNumber(numbers.get(i));
                if (normalized != null && !normalized.equals(numbers.get(i))) {
                    write.setString(1, normalized);
                    write.setInt(2, ids.get(i).intValue());
                    write.executeUpdate();
                }
            }
            write.close();
        } catch (java.sql.SQLException e) {
            // A repair that fails must not stop the start, and there is no caller to tell yet.
            System.out.println("Kunde inte rätta registreringsnumren: " + e.getMessage());
        }
    }
}
