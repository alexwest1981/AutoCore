package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServicePackageRepository {

    public void save(ServicePackage servicePackage) throws SQLException {
        if (servicePackage.getId() == 0) {
            insert(servicePackage);
        } else {
            update(servicePackage);
        }
        saveItems(servicePackage);
    }

    private void insert(ServicePackage servicePackage) throws SQLException {
        String sql = "INSERT INTO service_packages (name, description) VALUES (?, ?)";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, servicePackage.getName());
            statement.setString(2, servicePackage.getDescription());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    servicePackage.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    private void update(ServicePackage servicePackage) throws SQLException {
        String sql = "UPDATE service_packages SET name = ?, description = ? WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, servicePackage.getName());
            statement.setString(2, servicePackage.getDescription());
            statement.setInt(3, servicePackage.getId());
            statement.executeUpdate();
        }
    }

    private void saveItems(ServicePackage servicePackage) throws SQLException {
        String deleteLinks = "DELETE FROM service_package_items WHERE package_id = ?";
        String insertLink = "INSERT OR IGNORE INTO service_package_items (package_id, service_item_id) VALUES (?, ?)";

        try (Connection connection = Db.getConnection();
             PreparedStatement delete = connection.prepareStatement(deleteLinks);
             PreparedStatement insert = connection.prepareStatement(insertLink)) {

            delete.setInt(1, servicePackage.getId());
            delete.executeUpdate();

            for (ServiceItem item : servicePackage.getServiceItems()) {
                insert.setInt(1, servicePackage.getId());
                insert.setInt(2, item.getId());
                insert.executeUpdate();
            }
        }
    }

    private List<ServiceItem> findItems(int packageId) throws SQLException {
        String sql = "SELECT s.id, s.name, s.description, s.price, s.estimated_minutes, s.specialization "
                + "FROM service_package_items spi "
                + "JOIN service_items s ON spi.service_item_id = s.id "
                + "WHERE spi.package_id = ? "
                + "ORDER BY s.id ASC";

        List<ServiceItem> items = new ArrayList<ServiceItem>();

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, packageId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(new ServiceItem(
                            resultSet.getInt("id"),
                            resultSet.getString("name"),
                            resultSet.getString("description"),
                            resultSet.getDouble("price"),
                            resultSet.getInt("estimated_minutes"),
                            resultSet.getString("specialization")));
                }
            }
        }
        return items;
    }

    public List<ServicePackage> findAll() throws SQLException {
        String sql = "SELECT id, name, description FROM service_packages ORDER BY name";
        List<ServicePackage> packages = new ArrayList<ServicePackage>();

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                packages.add(buildPackage(resultSet));

            }
        }

        for (ServicePackage servicePackage : packages) {
            servicePackage.setServiceItems(findItems(servicePackage.getId()));
        }
        return packages;
    }

    public ServicePackage findById(int id) throws SQLException {
        String sql = "SELECT id, name, description FROM service_packages WHERE id = ?";
        ServicePackage servicePackage = null;

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    servicePackage = buildPackage(resultSet);
                }
            }
        }

        if (servicePackage != null) {
            servicePackage.setServiceItems(findItems(servicePackage.getId()));
        }
        return servicePackage;
    }

    public void delete(int id) throws SQLException {
        try (Connection connection = Db.getConnection();
             PreparedStatement deleteItems = connection.prepareStatement(
                     "DELETE FROM service_package_items WHERE package_id = ?");
             PreparedStatement deletePackage = connection.prepareStatement(
                     "DELETE FROM service_packages WHERE id = ?")) {

            deleteItems.setInt(1, id);
            deleteItems.executeUpdate();

            deletePackage.setInt(1, id);
            deletePackage.executeUpdate();
        }
    }

    private ServicePackage buildPackage(ResultSet resultSet) throws SQLException {
        return new ServicePackage(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("description"));
    }
}
