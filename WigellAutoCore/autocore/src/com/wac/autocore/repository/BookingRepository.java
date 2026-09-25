package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Booking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class BookingRepository {

    public void save(Booking booking) throws SQLException {
        if (booking.getId() == 0 || findById(booking.getId()) == null) {
            insert(booking);
        } else {
            update(booking);
        }
    }

    public List<Booking> findAll() throws SQLException {
        List<Booking> bookings = new ArrayList<Booking>();
        String sql = "SELECT id, vehicle_id, date, description, status, start_time, end_time, " +
                "mechanic_id, service_item_id FROM bookings";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                bookings.add(buildBooking(resultSet));
            }
        }

        return bookings;
    }

    public Booking findById(int id) throws SQLException {
        String sql = "SELECT id, vehicle_id, date, description, status, start_time, end_time, " +
                "mechanic_id, service_item_id FROM bookings WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return buildBooking(resultSet);
                }
            }
        }

        return null;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM bookings WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private void insert(Booking booking) throws SQLException {
        String sql = "INSERT INTO bookings (vehicle_id, date, description, status, start_time, " +
                "end_time, mechanic_id, service_item_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, booking.getVehicleId());
            setDate(statement, 2, booking.getDate());
            statement.setString(3, booking.getDescription());
            statement.setString(4, booking.getStatus());
            setTime(statement, 5, booking.getStartTime());
            setTime(statement, 6, booking.getEndTime());

            if (booking.getMechanicId() == 0) {
                statement.setNull(7, java.sql.Types.INTEGER);
            } else {
                statement.setInt(7, booking.getMechanicId());
            }
            if (booking.getServiceItemId() == 0) {
                statement.setNull(8, java.sql.Types.INTEGER);
            } else {
                statement.setInt(8, booking.getServiceItemId());
            }

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    booking.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    private void update(Booking booking) throws SQLException {
        String sql = "UPDATE bookings SET vehicle_id = ?, date = ?, description = ?, status = ?, " +
                "start_time = ?, end_time = ?, mechanic_id = ?, service_item_id = ? WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, booking.getVehicleId());
            setDate(statement, 2, booking.getDate());
            statement.setString(3, booking.getDescription());
            statement.setString(4, booking.getStatus());
            setTime(statement, 5, booking.getStartTime());
            setTime(statement, 6, booking.getEndTime());
            if (booking.getMechanicId() == 0) {
                statement.setNull(7, java.sql.Types.INTEGER);
            } else {
                statement.setInt(7, booking.getMechanicId());
            }

            if (booking.getServiceItemId() == 0) {
                statement.setNull(8, java.sql.Types.INTEGER);
            } else {
                statement.setInt(8, booking.getServiceItemId());
            }

            statement.setInt(9, booking.getId());
            statement.executeUpdate();
        }
    }


    private void setTime(PreparedStatement statement, int position, LocalTime time) throws SQLException {
        if (time == null) {
            statement.setNull(position, Types.VARCHAR);
        }
        else {
            statement.setString(position, time.toString());
        }
    }

    private void setDate(PreparedStatement statement, int position, LocalDate date) throws SQLException {
        if (date == null) {
            statement.setNull(position, Types.VARCHAR);
        } else {
            statement.setString(position, date.toString());
        }
    }

    private Booking buildBooking(ResultSet resultSet) throws SQLException {
        String dateText = resultSet.getString("date");

        // 1. Skapa bokningsobjektet först
        Booking booking = new Booking(
                resultSet.getInt("id"),
                resultSet.getInt("vehicle_id"),
                dateText == null ? null : LocalDate.parse(dateText),
                resultSet.getString("description")
        );

        String status = resultSet.getString("status");
        if (status != null) {
            booking.setStatus(status);
        }

        String startText = resultSet.getString("start_time");
        if (startText != null) {
            try {
                booking.setStartTime(LocalTime.parse(startText));
            } catch (java.time.format.DateTimeParseException e) {
                System.out.println("Hoppade över trasig tid i databasen: " + startText);
                booking.setStartTime(LocalTime.of(8, 0));
            }
        }

        String endText = resultSet.getString("end_time");
        if (endText != null) {
            try {
                booking.setEndTime(LocalTime.parse(endText));
            } catch (java.time.format.DateTimeParseException e) {
                booking.setEndTime(LocalTime.of(9, 0));
            }
        }

        int mechanicId = resultSet.getInt("mechanic_id");
        if (resultSet.wasNull()) {
            booking.setMechanicId(0);
        } else {
            booking.setMechanicId(mechanicId);
        }

        int serviceItemId = resultSet.getInt("service_item_id");
        if (resultSet.wasNull()) {
            booking.setServiceItemId(0);
        } else {
            booking.setServiceItemId(serviceItemId);
        }

        return booking;
    }
}
