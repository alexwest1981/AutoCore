package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.ScheduleEntry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** The work orders that still hold booked time, read the way the schedule draws them. */
public class ScheduleRepository {

    private final Queries queries;

    public ScheduleRepository(Db db) {
        this.queries = new Queries(db);
    }

    public List<ScheduleEntry> findActive() {
        // A row without a date has no hour to land on, and the row mapper cannot skip it.
        String sql = "SELECT wo.id AS wo_id, wo.mechanic_id, "
                + "b.id AS booking_id, b.date, b.start_time, b.description, "
                + "v.registration_number, c.name AS customer_name "
                + "FROM work_orders wo "
                + "JOIN bookings b ON wo.booking_id = b.id "
                + "LEFT JOIN vehicles v ON b.vehicle_id = v.id "
                + "LEFT JOIN customers c ON v.customer_id = c.id "
                + "WHERE wo.status IN ('CREATED', 'CONFIRMED', 'IN_PROGRESS') "
                + "AND b.date IS NOT NULL AND TRIM(b.date) <> ''";

        return queries.read(sql, ScheduleRepository::build);
    }

    private static ScheduleEntry build(ResultSet resultSet) throws SQLException {
        return new ScheduleEntry(
                resultSet.getInt("wo_id"),
                resultSet.getInt("mechanic_id"),
                resultSet.getInt("booking_id"),
                LocalDate.parse(resultSet.getString("date")),
                resultSet.getString("start_time"),
                resultSet.getString("description"),
                resultSet.getString("registration_number"),
                resultSet.getString("customer_name")
        );
    }
}
