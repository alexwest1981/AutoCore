package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.ServiceItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** The booking_service_items table: the services on a booking. */
public class BookingServiceItemRepository {

    public List<ServiceItem> findByBookingId(int bookingId) throws SQLException {
        String sql = "SELECT s.id, s.name, s.description, s.price, s.estimated_minutes, s.specialization " +
                "FROM booking_service_items bsi " +
                "JOIN service_items s ON bsi.service_item_id = s.id " +
                "WHERE bsi.booking_id = ? " +
                "ORDER BY s.id ASC";

        List<ServiceItem> items = new ArrayList<ServiceItem>();

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, bookingId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ServiceItem item = new ServiceItem(
                            resultSet.getInt("id"),
                            resultSet.getString("name"),
                            resultSet.getString("description"),
                            resultSet.getDouble("price"),
                            resultSet.getInt("estimated_minutes"));
                    // The requirement has to come along, otherwise every service looks requirement-free and every mechanic becomes qualified.
                    item.setSpecialization(resultSet.getString("specialization"));
                    items.add(item);
                }
            }
        }

        return items;
    }

    /** The package name per service id. Services picked on their own are left out. */
    public java.util.Map<Integer, String> findPackageNames(int bookingId) throws SQLException {
        String sql = "SELECT service_item_id, package_name FROM booking_service_items "
                + "WHERE booking_id = ? AND package_name IS NOT NULL";

        java.util.Map<Integer, String> names = new java.util.LinkedHashMap<Integer, String>();

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, bookingId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    names.put(Integer.valueOf(resultSet.getInt("service_item_id")), resultSet.getString("package_name"));
                }
            }
        }

        return names;
    }

    public void save(Booking booking) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            save(connection, booking);
        }
    }

    /** Saves the services on an open connection, so everything runs in one transaction. */
    public void save(Connection connection, Booking booking) throws SQLException {
        String deleteLinks = "DELETE FROM booking_service_items WHERE booking_id = ?";
        String insertLink = "INSERT OR IGNORE INTO booking_service_items (booking_id, service_item_id, package_name) "
                + "VALUES (?, ?, ?)";

        try (PreparedStatement delete = connection.prepareStatement(deleteLinks);
             PreparedStatement insert = connection.prepareStatement(insertLink)) {

            delete.setInt(1, booking.getId());
            delete.executeUpdate();

            for (Integer serviceItemId : booking.getServiceItemIds()) {
                if (serviceItemId != null && serviceItemId > 0) {
                    insert.setInt(1, booking.getId());
                    insert.setInt(2, serviceItemId);
                    // An empty package name means the service was picked on its own, not through a package.
                    String packageName = booking.getServicePackageName(serviceItemId);
                    insert.setString(3, packageName.isEmpty() ? null : packageName);
                    insert.executeUpdate();
                }
            }
        }
    }

    public void deleteByBookingId(Connection connection, int bookingId) throws SQLException {
        String sql = "DELETE FROM booking_service_items WHERE booking_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookingId);
            statement.executeUpdate();
        }
    }
}
