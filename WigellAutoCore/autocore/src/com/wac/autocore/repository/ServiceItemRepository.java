package com.wac.autocore.repository;

import com.wac.autocore.model.ServiceItem;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ServiceItemRepository {

    private static final String COLUMNS = "id, name, description, price, estimated_minutes, specialization";

    public void save(ServiceItem serviceItem) throws SQLException {
        if (serviceItem.getId() == 0 || findById(serviceItem.getId()) == null) {
            insert(serviceItem);
        } else {
            update(serviceItem);
        }
    }

    public List<ServiceItem> findAll() throws SQLException {
        return Queries.read("SELECT " + COLUMNS + " FROM service_items", ServiceItemRepository::build);
    }

    public ServiceItem findById(int id) throws SQLException {
        return Queries.readOne("SELECT " + COLUMNS + " FROM service_items WHERE id = ?",
                statement -> statement.setInt(1, id), ServiceItemRepository::build);
    }

    public void delete(int id) throws SQLException {
        Queries.write("DELETE FROM service_items WHERE id = ?", statement -> statement.setInt(1, id));
    }

    private void insert(ServiceItem serviceItem) throws SQLException {
        String sql = "INSERT INTO service_items (name, description, price, estimated_minutes, specialization) "
                + "VALUES (?, ?, ?, ?, ?)";

        int id = Queries.insert(sql, statement -> {
            statement.setString(1, serviceItem.getName());
            statement.setString(2, serviceItem.getDescription());
            statement.setDouble(3, serviceItem.getPrice());
            statement.setInt(4, serviceItem.getEstimatedMinutes());
            statement.setString(5, serviceItem.getSpecialization());
        });

        if (id > 0) {
            serviceItem.setId(id);
        }
    }

    private void update(ServiceItem serviceItem) throws SQLException {
        String sql = "UPDATE service_items SET name = ?, description = ?, price = ?, estimated_minutes = ?, "
                + "specialization = ? WHERE id = ?";

        Queries.write(sql, statement -> {
            statement.setString(1, serviceItem.getName());
            statement.setString(2, serviceItem.getDescription());
            statement.setDouble(3, serviceItem.getPrice());
            statement.setInt(4, serviceItem.getEstimatedMinutes());
            statement.setString(5, serviceItem.getSpecialization());
            statement.setInt(6, serviceItem.getId());
        });
    }

    private static ServiceItem build(ResultSet resultSet) throws SQLException {
        return new ServiceItem(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getDouble("price"),
                resultSet.getInt("estimated_minutes"),
                resultSet.getString("specialization")
        );
    }
}
