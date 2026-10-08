package com.wac.autocore.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Settings that should survive a restart. One table and two operations, on purpose. */
public final class Settings {

    private Settings() {}

    /** Returns the stored value for {@code key}, or {@code fallback} when nothing is stored. */
    public static String get(String key, String fallback) {
        String sql = "SELECT value FROM settings WHERE key = ?";
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    String value = rows.getString(1);
                    if (value != null) {
                        return value;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Could not read setting '" + key + "': " + e.getMessage());
        }
        return fallback;
    }

    /** Stores {@code value} under {@code key}, replacing any previous value. */
    public static void put(String key, String value) {
        String sql = "INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)";
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Could not save setting '" + key + "': " + e.getMessage());
        }
    }
}
