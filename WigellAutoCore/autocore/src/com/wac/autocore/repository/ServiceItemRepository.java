package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.ServiceItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServiceItemRepository {

    public void save(ServiceItem serviceItem) throws SQLException {
        if (serviceItem.getId() == 0 || findById(serviceItem.getId()) == null) {
            insert(serviceItem);
        } else {
            update(serviceItem);
        }
    }

    public List<ServiceItem> findAll() throws SQLException {
        List<ServiceItem> serviceItems = new ArrayList<ServiceItem>();
        String sql = "SELECT id, name, description, price, estimated_minutes FROM service_items";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                serviceItems.add(buildServiceItem(resultSet));
            }
        }

        return serviceItems;
    }

    public ServiceItem findById(int id) throws SQLException {
        String sql = "SELECT id, name, description, price, estimated_minutes FROM service_items WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return buildServiceItem(resultSet);
                }
            }
        }

        return null;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM service_items WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private void insert(ServiceItem serviceItem) throws SQLException {
        String sql = "INSERT INTO service_items (name, description, price, estimated_minutes) VALUES (?, ?, ?, ?)";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, serviceItem.getName());
            statement.setString(2, serviceItem.getDescription());
            statement.setDouble(3, serviceItem.getPrice());
            statement.setInt(4, serviceItem.getEstimatedMinutes());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    serviceItem.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    private void update(ServiceItem serviceItem) throws SQLException {
        String sql = "UPDATE service_items SET name = ?, description = ?, price = ?, estimated_minutes = ? WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, serviceItem.getName());
            statement.setString(2, serviceItem.getDescription());
            statement.setDouble(3, serviceItem.getPrice());
            statement.setInt(4, serviceItem.getEstimatedMinutes());
            statement.setInt(5, serviceItem.getId());
            statement.executeUpdate();
        }
    }

    private ServiceItem buildServiceItem(ResultSet resultSet) throws SQLException {
        return new ServiceItem(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getDouble("price"),
                resultSet.getInt("estimated_minutes")
        );
    }
}
