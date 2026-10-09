package com.wac.autocore.repository;

import com.wac.autocore.data.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/** The mechanics on a booking. More than one when the services call for different specialists. */
public class BookingMechanicRepository {

    /** Drops a booking's mechanic links, on a connection the caller owns. */
    public void deleteByBookingId(Connection connection, int bookingId) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM booking_mechanics WHERE booking_id = ?")) {
            delete.setInt(1, bookingId);
            delete.executeUpdate();
        }
    }

    /** Replaces the booking's mechanics with the list. An empty list clears the links. */
    public void saveForBooking(int bookingId, List<Integer> mechanicIds) throws SQLException {
        // One connection for the removal and the insert, so the booking never sits without its
        // mechanics if the second half fails.
        try (Connection connection = Db.getConnection()) {
            deleteByBookingId(connection, bookingId);
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
        return Queries.read("SELECT mechanic_id FROM booking_mechanics WHERE booking_id = ? ORDER BY mechanic_id",
                statement -> statement.setInt(1, bookingId), resultSet -> Integer.valueOf(resultSet.getInt("mechanic_id")));
    }
}
