package com.wac.autocore.repository;

import com.wac.autocore.exception.RuleViolationException;
import com.wac.autocore.exception.DuplicateRegistrationNumberException;
import com.wac.autocore.data.Db;
import com.wac.autocore.model.Vehicle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class VehicleRepository {

    private static final String COLUMNS = "id, registration_number, brand, model, year, customer_id";

    /** Saves the vehicle. The number is already normalized in the model. */
    public void save(Vehicle vehicle) throws SQLException {
        String registrationNumber = vehicle.getRegistrationNumber();
        if (registrationNumber == null || registrationNumber.trim().isEmpty()) {
            throw new RuleViolationException("Fordonet saknar registreringsnummer.");
        }

        if (vehicle.getId() == 0 || findById(vehicle.getId()) == null) {
            insert(vehicle);
        } else {
            update(vehicle);
        }
    }

    /** True if another vehicle already has the number. The query is closed before the caller writes. */
    private boolean registrationNumberTaken(Connection connection, String registrationNumber, int exceptId)
            throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM vehicles WHERE registration_number = ? AND id <> ?");
        statement.setString(1, registrationNumber);
        statement.setInt(2, exceptId);
        ResultSet result = statement.executeQuery();
        boolean taken = result.next() && result.getInt(1) > 0;
        result.close();
        statement.close();
        return taken;
    }

    public List<Vehicle> findAll() throws SQLException {
        return Queries.read("SELECT " + COLUMNS + " FROM vehicles", VehicleRepository::build);
    }

    public Vehicle findById(int id) throws SQLException {
        return Queries.readOne("SELECT " + COLUMNS + " FROM vehicles WHERE id = ?",
                statement -> statement.setInt(1, id), VehicleRepository::build);
    }

    public void delete(int id) throws SQLException {
        Queries.write("DELETE FROM vehicles WHERE id = ?", statement -> statement.setInt(1, id));
    }

    private void insert(Vehicle vehicle) throws SQLException {
        String sql = "INSERT INTO vehicles (registration_number, brand, model, year, customer_id) VALUES (?, ?, ?, ?, ?)";

        // The check and the write share a connection, so the number cannot be taken in between.
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            if (registrationNumberTaken(connection, vehicle.getRegistrationNumber(), 0)) {
                throw new DuplicateRegistrationNumberException("Registreringsnumret "
                        + vehicle.getRegistrationNumber() + " tillhör redan ett annat fordon.");
            }

            statement.setString(1, vehicle.getRegistrationNumber());
            statement.setString(2, vehicle.getBrand());
            statement.setString(3, vehicle.getModel());
            statement.setInt(4, vehicle.getYear());
            statement.setInt(5, vehicle.getCustomerId());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    vehicle.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    private void update(Vehicle vehicle) throws SQLException {
        String sql = "UPDATE vehicles SET registration_number = ?, brand = ?, model = ?, year = ?, customer_id = ? "
                + "WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (registrationNumberTaken(connection, vehicle.getRegistrationNumber(), vehicle.getId())) {
                throw new DuplicateRegistrationNumberException("Registreringsnumret "
                        + vehicle.getRegistrationNumber() + " tillhör redan ett annat fordon.");
            }

            statement.setString(1, vehicle.getRegistrationNumber());
            statement.setString(2, vehicle.getBrand());
            statement.setString(3, vehicle.getModel());
            statement.setInt(4, vehicle.getYear());
            statement.setInt(5, vehicle.getCustomerId());
            statement.setInt(6, vehicle.getId());
            statement.executeUpdate();
        }
    }

    private static Vehicle build(ResultSet resultSet) throws SQLException {
        return new Vehicle(
                resultSet.getInt("id"),
                resultSet.getString("registration_number"),
                resultSet.getString("brand"),
                resultSet.getString("model"),
                resultSet.getInt("year"),
                resultSet.getInt("customer_id")
        );
    }
}
