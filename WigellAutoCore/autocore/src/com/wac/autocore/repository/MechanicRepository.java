package com.wac.autocore.repository;

import com.wac.autocore.model.Mechanic;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class MechanicRepository {

    private static final String COLUMNS = "id, name, phone, specialization, available";

    public void save(Mechanic mechanic) throws SQLException {
        if (mechanic.getId() == 0 || findById(mechanic.getId()) == null) {
            insert(mechanic);
        } else {
            update(mechanic);
        }
    }

    public List<Mechanic> findAll() throws SQLException {
        return Queries.read("SELECT " + COLUMNS + " FROM mechanics", MechanicRepository::build);
    }

    public Mechanic findById(int id) throws SQLException {
        return Queries.readOne("SELECT " + COLUMNS + " FROM mechanics WHERE id = ?",
                statement -> statement.setInt(1, id), MechanicRepository::build);
    }

    public void delete(int id) throws SQLException {
        Queries.write("DELETE FROM mechanics WHERE id = ?", statement -> statement.setInt(1, id));
    }

    private void insert(Mechanic mechanic) throws SQLException {
        String sql = "INSERT INTO mechanics (name, phone, specialization, available) VALUES (?, ?, ?, ?)";

        int id = Queries.insert(sql, statement -> {
            statement.setString(1, mechanic.getName());
            statement.setString(2, mechanic.getPhone());
            statement.setString(3, mechanic.getSpecialization());
            statement.setBoolean(4, mechanic.isAvailable());
        });

        if (id > 0) {
            mechanic.setId(id);
        }
    }

    private void update(Mechanic mechanic) throws SQLException {
        String sql = "UPDATE mechanics SET name = ?, phone = ?, specialization = ?, available = ? WHERE id = ?";

        Queries.write(sql, statement -> {
            statement.setString(1, mechanic.getName());
            statement.setString(2, mechanic.getPhone());
            statement.setString(3, mechanic.getSpecialization());
            statement.setBoolean(4, mechanic.isAvailable());
            statement.setInt(5, mechanic.getId());
        });
    }

    private static Mechanic build(ResultSet resultSet) throws SQLException {
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
