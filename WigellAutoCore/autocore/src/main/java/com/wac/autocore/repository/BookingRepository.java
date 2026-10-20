package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.data.DatabaseWork;
import com.wac.autocore.exception.DataAccessException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.BookingStatus;
import com.wac.autocore.model.ServiceItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class BookingRepository {

    private final Db db;

    private final Queries queries;

    private static final String COLUMNS = "id, vehicle_id, date, description, status, start_time, end_time, "
            + "mechanic_id, service_item_id";

    private static final String SERVICE_ITEM_COLUMNS =
            "id, name, description, price, estimated_minutes, specialization";

    private final BookingServiceItemRepository bookingServiceItemRepository;
    private final BookingMechanicRepository bookingMechanicRepository;

    public BookingRepository(Db db, BookingServiceItemRepository bookingServiceItemRepository,
                             BookingMechanicRepository bookingMechanicRepository) {
        this.db = db;
        this.queries = new Queries(db);
        this.bookingServiceItemRepository = bookingServiceItemRepository;
        this.bookingMechanicRepository = bookingMechanicRepository;
    }

    public void save(final Booking booking) {
        final boolean isNew = booking.getId() == 0 || findById(booking.getId()) == null;

        new DatabaseWork<Void>(db) {
            @Override
            protected Void step(Connection connection) throws SQLException {
                if (isNew) {
                    insert(connection, booking);
                } else {
                    update(connection, booking);
                }
                bookingServiceItemRepository.save(connection, booking);
                // The mechanics are stored in the same sweep, so a booking with several specialists keeps them.
                bookingMechanicRepository.saveForBooking(connection, booking.getId(), booking.getMechanicIds());
                return null;
            }
        }.run();
    }

    public List<Booking> findAll() {
        return queries.read("SELECT " + COLUMNS + " FROM bookings", resultSet -> build(resultSet));
    }

    public Booking findById(int id) {
        return queries.readOne("SELECT " + COLUMNS + " FROM bookings WHERE id = ?",
                statement -> statement.setInt(1, id), resultSet -> build(resultSet));
    }

    public void delete(final int id) {
        // The service rows and the mechanic links point at this booking, and nothing else clears
        // them, so all three go in one transaction.
        new DatabaseWork<Void>(db) {
            @Override
            protected Void step(Connection connection) throws SQLException {
                bookingServiceItemRepository.deleteByBookingId(connection, id);
                bookingMechanicRepository.deleteByBookingId(connection, id);
                queries.write(connection, "DELETE FROM bookings WHERE id = ?",
                        statement -> statement.setInt(1, id));
                return null;
            }
        }.run();
    }

    private void insert(Connection connection, Booking booking) throws SQLException {
        String sql = "INSERT INTO bookings (vehicle_id, date, description, status, start_time, "
                + "end_time, mechanic_id, service_item_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        int id = queries.insert(connection, sql, statement -> {
            statement.setInt(1, booking.getVehicleId());
            setDate(statement, 2, booking.getDate());
            statement.setString(3, booking.getDescription());
            statement.setString(4, booking.getStatus().name());
            setTime(statement, 5, booking.getStartTime());
            setTime(statement, 6, booking.getEndTime());
            Queries.setReference(statement, 7, booking.getMechanicId());
            Queries.setReference(statement, 8, booking.getServiceItemId());
        });

        if (id > 0) {
            booking.setId(id);
        }
    }

    private void update(Connection connection, Booking booking) throws SQLException {
        String sql = "UPDATE bookings SET vehicle_id = ?, date = ?, description = ?, status = ?, "
                + "start_time = ?, end_time = ?, mechanic_id = ?, service_item_id = ? WHERE id = ?";

        queries.write(connection, sql, statement -> {
            statement.setInt(1, booking.getVehicleId());
            setDate(statement, 2, booking.getDate());
            statement.setString(3, booking.getDescription());
            statement.setString(4, booking.getStatus().name());
            setTime(statement, 5, booking.getStartTime());
            setTime(statement, 6, booking.getEndTime());
            Queries.setReference(statement, 7, booking.getMechanicId());
            Queries.setReference(statement, 8, booking.getServiceItemId());
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

    private Booking build(ResultSet resultSet) throws SQLException {
        String dateText = resultSet.getString("date");

        LocalDate bookingDate = null;

        // A row written before the format was fixed can hold anything, and one bad row must not stop the list.
        if (dateText != null) {

            try {
                bookingDate = LocalDate.parse(dateText);
            } catch (java.time.format.DateTimeParseException e) {
                throw new DataAccessException("Could not read the date of booking " + resultSet.getInt("id"), e);
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
            booking.setStatus(BookingStatus.fromText(resultSet.getString("status")));
        }

        String startText = resultSet.getString("start_time");
        if (startText != null) {
            try {
                booking.setStartTime(LocalTime.parse(startText));
            } catch (java.time.format.DateTimeParseException e) {
                throw new DataAccessException("Could not read the start time of booking " + booking.getId(), e);
            }
        }

        String endText = resultSet.getString("end_time");
        if (endText != null) {
            try {
                booking.setEndTime(LocalTime.parse(endText));
            } catch (java.time.format.DateTimeParseException e) {
                throw new DataAccessException("Could not read the end time of booking " + booking.getId(), e);
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

    private ServiceItem findServiceItemById(int id) throws SQLException {
        return queries.readOne("SELECT " + SERVICE_ITEM_COLUMNS + " FROM service_items WHERE id = ?",
                statement -> statement.setInt(1, id), resultSet -> {
                    ServiceItem item = new ServiceItem(
                            resultSet.getInt("id"),
                            resultSet.getString("name"),
                            resultSet.getString("description"),
                            resultSet.getInt("price"),
                            resultSet.getInt("estimated_minutes")
                    );
                    // The requirement has to come along, otherwise the service looks
                    // requirement-free and every mechanic looks qualified for it.
                    item.setSpecialization(resultSet.getString("specialization"));
                    return item;
                });
    }
}
