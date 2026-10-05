package com.wac.autocore.repository;

import com.wac.autocore.data.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Kopplingen mellan en bokning och de mekaniker som ska utföra den.
 *
 * En bokning kan innehålla tjänster som kräver olika specialister, och då behövs fler än en
 * mekaniker. Tabellen bookings har bara en kolumn för mekaniker, den som gäller tiden, så
 * resten bor här.
 */
public class BookingMechanicRepository {

    /** Ersätter bokningens mekaniker med listan. Tom lista rensar kopplingarna. */
    public void saveForBooking(int bookingId, List<Integer> mechanicIds) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM booking_mechanics WHERE booking_id = ?")) {
                delete.setInt(1, bookingId);
                delete.executeUpdate();
            }
            if (mechanicIds == null || mechanicIds.isEmpty()) {
                return;
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT OR IGNORE INTO booking_mechanics (booking_id, mechanic_id) VALUES (?, ?)")) {
                for (Integer mechanicId : mechanicIds) {
                    if (mechanicId == null || mechanicId.intValue() <= 0) {
                        continue;
                    }
                    insert.setInt(1, bookingId);
                    insert.setInt(2, mechanicId.intValue());
                    insert.executeUpdate();
                }
            }
        }
    }

    /** Bokningens mekaniker, i den ordning de lades in. */
    public List<Integer> findMechanicIds(int bookingId) throws SQLException {
        List<Integer> ids = new ArrayList<Integer>();
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT mechanic_id FROM booking_mechanics WHERE booking_id = ? ORDER BY mechanic_id")) {
            statement.setInt(1, bookingId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(Integer.valueOf(resultSet.getInt("mechanic_id")));
                }
            }
        }
        return ids;
    }

    /** Lägger in bokningens mekaniker från bookings-tabellen om kopplingen saknas. */
    public void ensureLinked(int bookingId) throws SQLException {
        if (!findMechanicIds(bookingId).isEmpty()) {
            return;
        }
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT OR IGNORE INTO booking_mechanics (booking_id, mechanic_id) "
                             + "SELECT id, mechanic_id FROM bookings "
                             + "WHERE id = ? AND mechanic_id IS NOT NULL AND mechanic_id > 0")) {
            statement.setInt(1, bookingId);
            statement.executeUpdate();
        }
    }
}
