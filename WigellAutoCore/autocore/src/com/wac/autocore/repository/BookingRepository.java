package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.ServiceItem;

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

    private static final String COLUMNS = "id, vehicle_id, date, description, status, start_time, end_time, "
            + "mechanic_id, service_item_id";

    private static final String SERVICE_ITEM_COLUMNS =
            "id, name, description, price, estimated_minutes, specialization";

    private final BookingServiceItemRepository bookingServiceItemRepository = new BookingServiceItemRepository();
    private final BookingMechanicRepository bookingMechanicRepository = new BookingMechanicRepository();

    public void save(Booking booking) throws SQLException {
        if (booking.getId() == 0 || findById(booking.getId()) == null) {
            insert(booking);
        } else {
            update(booking);
        }
        bookingServiceItemRepository.save(booking);
        // The mechanics are stored in the same sweep, so a booking with several specialists keeps them.
        bookingMechanicRepository.saveForBooking(booking.getId(), booking.getMechanicIds());
    }

    public List<Booking> findAll() throws SQLException {
        return Queries.read("SELECT " + COLUMNS + " FROM bookings", resultSet -> build(resultSet));
    }

    public Booking findById(int id) throws SQLException {
        return Queries.readOne("SELECT " + COLUMNS + " FROM bookings WHERE id = ?",
                statement -> statement.setInt(1, id), resultSet -> build(resultSet));
    }

    public void delete(int id) throws SQLException {
        // The service rows and the mechanic links point at this booking, and nothing else clears
        // them, so all three go in one connection.
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM bookings WHERE id = ?")) {

            bookingServiceItemRepository.deleteByBookingId(connection, id);
            bookingMechanicRepository.deleteByBookingId(connection, id);

            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private void insert(Booking booking) throws SQLException {
        String sql = "INSERT INTO bookings (vehicle_id, date, description, status, start_time, "
                + "end_time, mechanic_id, service_item_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        int id = Queries.insert(sql, statement -> {
            statement.setInt(1, booking.getVehicleId());
            setDate(statement, 2, booking.getDate());
            statement.setString(3, booking.getDescription());
            statement.setString(4, booking.getStatus());
            setTime(statement, 5, booking.getStartTime());
            setTime(statement, 6, booking.getEndTime());
            setMechanicId(statement, 7, booking.getMechanicId());
            setServiceItemId(statement, 8, booking.getServiceItemId());
        });

        if (id > 0) {
            booking.setId(id);
        }
    }

    private void update(Booking booking) throws SQLException {
        String sql = "UPDATE bookings SET vehicle_id = ?, date = ?, description = ?, status = ?, "
                + "start_time = ?, end_time = ?, mechanic_id = ?, service_item_id = ? WHERE id = ?";

        Queries.write(sql, statement -> {
            statement.setInt(1, booking.getVehicleId());
            setDate(statement, 2, booking.getDate());
            statement.setString(3, booking.getDescription());
            statement.setString(4, booking.getStatus());
            setTime(statement, 5, booking.getStartTime());
            setTime(statement, 6, booking.getEndTime());
            setMechanicId(statement, 7, booking.getMechanicId());
            setServiceItemId(statement, 8, booking.getServiceItemId());
            statement.setInt(9, booking.getId());
        });
    }

    private static void setTime(PreparedStatement statement, int position, LocalTime time) throws SQLException {
        if (time == null) {
            statement.setNull(position, Types.VARCHAR);
        }
        else {
            statement.setString(position, time.toString());
        }
    }

    private static void setDate(PreparedStatement statement, int position, LocalDate date) throws SQLException {
        if (date == null) {
            statement.setNull(position, Types.VARCHAR);
        } else {
            statement.setString(position, date.toString());
        }
    }

    /** A missing mechanic or service is stored as NULL, not as a zero that points at nothing. */
    private static void setMechanicId(PreparedStatement statement, int position, int mechanicId) throws SQLException {
        if (mechanicId == 0) {
            statement.setNull(position, Types.INTEGER);
        } else {
            statement.setInt(position, mechanicId);
        }
    }

    private static void setServiceItemId(PreparedStatement statement, int position, int serviceItemId) throws SQLException {
        if (serviceItemId == 0) {
            statement.setNull(position, Types.INTEGER);
        } else {
            statement.setInt(position, serviceItemId);
        }
    }

    private Booking build(ResultSet resultSet) throws SQLException {
        String dateText = resultSet.getString("date");

        LocalDate bookingDate = null;

        // A row written before the format was fixed can hold anything, and one bad row must not
        // stop the list.
        if (dateText != null) {

            try {
                bookingDate = LocalDate.parse(dateText);
            } catch (java.time.format.DateTimeParseException e) {
                System.out.println("Hoppade över ett trasigt datum i databasen: " + dateText);
            }
        }

        Booking booking = new Booking(
                resultSet.getInt("id"),
                resultSet.getInt("vehicle_id"),
                bookingDate,
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

        loadServiceItems(booking);
        loadMechanics(booking);

        return booking;
    }

    // The first mechanic sits in bookings, the rest in the join table.
    private void loadMechanics(Booking booking) throws SQLException {
        List<Integer> ids = bookingMechanicRepository.findMechanicIds(booking.getId());
        if (ids.isEmpty() && booking.getMechanicId() > 0) {
            ids = new ArrayList<Integer>();
            ids.add(Integer.valueOf(booking.getMechanicId()));
        }
        booking.setMechanicIds(ids);
    }

    private void loadServiceItems(Booking booking) throws SQLException {
        List<ServiceItem> items = bookingServiceItemRepository.findByBookingId(booking.getId());

        if (!items.isEmpty()) {
            booking.setLoadedServiceItems(items);
        } else if (booking.getServiceItemId() > 0) {
            ServiceItem item = findServiceItemById(booking.getServiceItemId());
            if (item != null) {
                List<ServiceItem> singleList = new ArrayList<ServiceItem>();
                singleList.add(item);
                booking.setLoadedServiceItems(singleList);
            }
        }

        // The package names are set after the services, because loading the services clears them.
        booking.setServicePackages(bookingServiceItemRepository.findPackageNames(booking.getId()));
    }

    private static ServiceItem findServiceItemById(int id) throws SQLException {
        return Queries.readOne("SELECT " + SERVICE_ITEM_COLUMNS + " FROM service_items WHERE id = ?",
                statement -> statement.setInt(1, id), resultSet -> {
                    ServiceItem item = new ServiceItem(
                            resultSet.getInt("id"),
                            resultSet.getString("name"),
                            resultSet.getString("description"),
                            resultSet.getDouble("price"),
                            resultSet.getInt("estimated_minutes")
                    );
                    // The requirement has to come along, otherwise the service looks
                    // requirement-free and every mechanic looks qualified for it.
                    item.setSpecialization(resultSet.getString("specialization"));
                    return item;
                });
    }
}
