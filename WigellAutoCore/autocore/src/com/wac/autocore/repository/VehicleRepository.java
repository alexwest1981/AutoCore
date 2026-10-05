package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Vehicle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VehicleRepository {

/** Sparar fordonet. Numret är redan normaliserat i modellen. */
    public void save(Vehicle vehicle) throws SQLException {
        String registrationNumber = vehicle.getRegistrationNumber();
        if (registrationNumber == null || registrationNumber.trim().isEmpty()) {
            throw new IllegalStateException("Fordonet saknar registreringsnummer.");
        }

        if (vehicle.getId() == 0 || findById(vehicle.getId()) == null) {
            insert(vehicle);
        } else {
            update(vehicle);
        }
    }

    /** Sant om ett annat fordon redan har numret. Frågan stängs innan den som anropade skriver. */
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
        List<Vehicle> vehicles = new ArrayList<Vehicle>();
        String sql = "SELECT id, registration_number, brand, model, year, customer_id FROM vehicles";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                vehicles.add(buildVehicle(resultSet));
            }
        }

        return vehicles;
    }

    public Vehicle findById(int id) throws SQLException {
        String sql = "SELECT id, registration_number, brand, model, year, customer_id FROM vehicles WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1,id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return buildVehicle(resultSet);
                }
            }
        }

        return null;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM vehicles WHERE id = ?";

        try (Connection connection = Db.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1,id);
            statement.executeUpdate();
        }
    }

    private void insert(Vehicle vehicle) throws SQLException {
        String sql = "INSERT INTO vehicles (registration_number, brand, model, year, customer_id) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = Db.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            if (registrationNumberTaken(connection, vehicle.getRegistrationNumber(), 0)) {
                throw new IllegalStateException("Registreringsnumret " + vehicle.getRegistrationNumber()
                        + " tillhör redan ett annat fordon.");
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
        String sql = "UPDATE vehicles SET registration_number = ?, brand = ?, model = ?, year = ?, customer_id = ? WHERE id = ?";

        try (Connection connection = Db.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

            if (registrationNumberTaken(connection, vehicle.getRegistrationNumber(), vehicle.getId())) {
                throw new IllegalStateException("Registreringsnumret " + vehicle.getRegistrationNumber()
                        + " tillhör redan ett annat fordon.");
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

    private Vehicle buildVehicle(ResultSet resultSet) throws SQLException {
        Vehicle vehicle = new Vehicle(
                resultSet.getInt("id"),
                resultSet.getString("registration_number"),
                resultSet.getString("brand"),
                resultSet.getString("model"),
                resultSet.getInt("year"),
                resultSet.getInt("customer_id")
        );

        return vehicle;
    }
}
