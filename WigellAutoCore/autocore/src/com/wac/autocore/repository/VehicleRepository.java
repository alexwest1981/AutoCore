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

    public void save(Vehicle vehicle) throws SQLException {
        if (vehicle.getId() == 0 || findById(vehicle.getId()) == null) {
            insert(vehicle);
        } else {
            update(vehicle);
        }
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
