package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Mechanic;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MechanicRepository {

    public void save(Mechanic mechanic) throws SQLException {
        if (mechanic.getId() == 0 || findById(mechanic.getId()) == null) {
            insert(mechanic);
        } else {
            update(mechanic);
        }
    }

    public List<Mechanic> findAll() throws SQLException {
        List<Mechanic> mechanics = new ArrayList<Mechanic>();
        String sql = "SELECT id, name, phone, specialization, available FROM mechanics";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                mechanics.add(buildMechanic(resultSet));
            }
        }

        return mechanics;
    }

    public Mechanic findById(int id) throws SQLException {
        String sql = "SELECT id, name, phone, specialization, available FROM mechanics WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1,id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return buildMechanic(resultSet);
                }
            }
        }

        return null;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM mechanics WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1,id);
            statement.executeUpdate();
        }
    }

    private void insert(Mechanic mechanic) throws SQLException {
        String sql = "INSERT INTO mechanics (name, phone, specialization, available) VALUES (?, ?, ?, ?)";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, mechanic.getName());
            statement.setString(2, mechanic.getPhone());
            statement.setString(3, mechanic.getSpecialization());
            statement.setBoolean(4, mechanic.isAvailable());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    mechanic.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    private void update(Mechanic mechanic) throws SQLException {
        String sql = "UPDATE mechanics SET name = ?, phone = ?, specialization = ?, available = ? WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, mechanic.getName());
            statement.setString(2, mechanic.getPhone());
            statement.setString(3, mechanic.getSpecialization());
            statement.setBoolean(4, mechanic.isAvailable());
            statement.setInt(5, mechanic.getId());
            statement.executeUpdate();
        }
    }

    private Mechanic buildMechanic(ResultSet resultSet) throws SQLException {
        Mechanic mechanic = new Mechanic(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("phone"),
                resultSet.getString("specialization")
        );

        mechanic.setAvailable(resultSet.getBoolean("available"));

        return mechanic;
    }
}
