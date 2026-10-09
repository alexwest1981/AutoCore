package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class ServicePackageRepository {

    private static final String COLUMNS = "id, name, description";

    private static final String ITEM_COLUMNS =
            "s.id, s.name, s.description, s.price, s.estimated_minutes, s.specialization";

    public void save(ServicePackage servicePackage) throws SQLException {
        if (servicePackage.getId() == 0) {
            insert(servicePackage);
        } else {
            update(servicePackage);
        }
        saveItems(servicePackage);
    }

    private void insert(ServicePackage servicePackage) throws SQLException {
        int id = Queries.insert("INSERT INTO service_packages (name, description) VALUES (?, ?)", statement -> {
            statement.setString(1, servicePackage.getName());
            statement.setString(2, servicePackage.getDescription());
        });

        if (id > 0) {
            servicePackage.setId(id);
        }
    }

    private void update(ServicePackage servicePackage) throws SQLException {
        Queries.write("UPDATE service_packages SET name = ?, description = ? WHERE id = ?", statement -> {
            statement.setString(1, servicePackage.getName());
            statement.setString(2, servicePackage.getDescription());
            statement.setInt(3, servicePackage.getId());
        });
    }

    private void saveItems(ServicePackage servicePackage) throws SQLException {
        String deleteLinks = "DELETE FROM service_package_items WHERE package_id = ?";
        String insertLink = "INSERT OR IGNORE INTO service_package_items (package_id, service_item_id) VALUES (?, ?)";

        // One connection for the removal and the inserts, so a package never sits without its items.
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
        String sql = "SELECT " + ITEM_COLUMNS + " FROM service_package_items spi "
                + "JOIN service_items s ON spi.service_item_id = s.id "
                + "WHERE spi.package_id = ? "
                + "ORDER BY s.id ASC";

        return Queries.read(sql, statement -> statement.setInt(1, packageId), resultSet -> new ServiceItem(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getDouble("price"),
                resultSet.getInt("estimated_minutes"),
                resultSet.getString("specialization")));
    }

    public List<ServicePackage> findAll() throws SQLException {
        List<ServicePackage> packages = Queries.read("SELECT " + COLUMNS + " FROM service_packages ORDER BY name",
                ServicePackageRepository::build);

        for (ServicePackage servicePackage : packages) {
            servicePackage.setServiceItems(findItems(servicePackage.getId()));
        }
        return packages;
    }

    public ServicePackage findById(int id) throws SQLException {
        ServicePackage servicePackage = Queries.readOne("SELECT " + COLUMNS + " FROM service_packages WHERE id = ?",
                statement -> statement.setInt(1, id), ServicePackageRepository::build);

        if (servicePackage != null) {
            servicePackage.setServiceItems(findItems(servicePackage.getId()));
        }
        return servicePackage;
    }

    public void delete(int id) throws SQLException {
        // The item links point at this package and nothing else clears them, so both go in one connection.
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

    private static ServicePackage build(ResultSet resultSet) throws SQLException {
        return new ServicePackage(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("description"));
    }
}
