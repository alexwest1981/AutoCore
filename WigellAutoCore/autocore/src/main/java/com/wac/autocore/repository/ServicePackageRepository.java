package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.data.DatabaseWork;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ServicePackageRepository {

    private final Db db;

    private final Queries queries;

    public ServicePackageRepository(Db db) {
        this.db = db;
        this.queries = new Queries(db);
    }

    private static final String COLUMNS = "id, name, description";

    private static final String ITEM_COLUMNS =
            "s.id, s.name, s.description, s.price, s.estimated_minutes, s.specialization";

    public void save(final ServicePackage servicePackage) {
        final boolean isNew = servicePackage.getId() == 0;

        new DatabaseWork<Void>(db) {
            @Override
            protected Void step(Connection connection) throws SQLException {
                if (isNew) {
                    insert(connection, servicePackage);
                } else {
                    update(connection, servicePackage);
                }
                saveItems(connection, servicePackage);
                return null;
            }
        }.run();
    }

    private void insert(Connection connection, ServicePackage servicePackage) throws SQLException {
        int id = queries.insert(connection, "INSERT INTO service_packages (name, description) VALUES (?, ?)",
                statement -> {
                    statement.setString(1, servicePackage.getName());
                    statement.setString(2, servicePackage.getDescription());
                });

        if (id > 0) {
            servicePackage.setId(id);
        }
    }

    private void update(Connection connection, ServicePackage servicePackage) throws SQLException {
        queries.write(connection, "UPDATE service_packages SET name = ?, description = ? WHERE id = ?",
                statement -> {
                    statement.setString(1, servicePackage.getName());
                    statement.setString(2, servicePackage.getDescription());
                    statement.setInt(3, servicePackage.getId());
                });
    }

    private void saveItems(Connection connection, ServicePackage servicePackage) throws SQLException {
        String deleteLinks = "DELETE FROM service_package_items WHERE package_id = ?";
        String insertLink = "INSERT OR IGNORE INTO service_package_items (package_id, service_item_id) VALUES (?, ?)";

        try (PreparedStatement delete = connection.prepareStatement(deleteLinks);
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

    private List<ServiceItem> findItems(int packageId) {
        String sql = "SELECT " + ITEM_COLUMNS + " FROM service_package_items spi "
                + "JOIN service_items s ON spi.service_item_id = s.id "
                + "WHERE spi.package_id = ? "
                + "ORDER BY s.id ASC";

        return queries.read(sql, statement -> statement.setInt(1, packageId), resultSet -> new ServiceItem(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getInt("price"),
                resultSet.getInt("estimated_minutes"),
                resultSet.getString("specialization")));
    }

    public List<ServicePackage> findAll() {
        List<ServicePackage> packages = queries.read("SELECT " + COLUMNS + " FROM service_packages ORDER BY name",
                ServicePackageRepository::build);

        for (ServicePackage servicePackage : packages) {
            servicePackage.setServiceItems(findItems(servicePackage.getId()));
        }
        return packages;
    }

    public ServicePackage findById(int id) {
        ServicePackage servicePackage = queries.readOne("SELECT " + COLUMNS + " FROM service_packages WHERE id = ?",
                statement -> statement.setInt(1, id), ServicePackageRepository::build);

        if (servicePackage != null) {
            servicePackage.setServiceItems(findItems(servicePackage.getId()));
        }
        return servicePackage;
    }

    public void delete(final int id) {
        // The item links point at this package and nothing else clears them, so both go in one
        // transaction.
        new DatabaseWork<Void>(db) {
            @Override
            protected Void step(Connection connection) throws SQLException {
                queries.write(connection, "DELETE FROM service_package_items WHERE package_id = ?",
                        statement -> statement.setInt(1, id));
                queries.write(connection, "DELETE FROM service_packages WHERE id = ?",
                        statement -> statement.setInt(1, id));
                return null;
            }
        }.run();
    }

    private static ServicePackage build(ResultSet resultSet) throws SQLException {
        return new ServicePackage(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("description"));
    }
}
