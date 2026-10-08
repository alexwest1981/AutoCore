package com.wac.autocore.repository;

import com.wac.autocore.data.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** The mechanics on a booking. More than one when the services call for different specialists. */
public class BookingMechanicRepository {

    /** Replaces the booking's mechanics with the list. An empty list clears the links. */
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

        /** The booking's mechanics, in the order they were added. */
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

}
