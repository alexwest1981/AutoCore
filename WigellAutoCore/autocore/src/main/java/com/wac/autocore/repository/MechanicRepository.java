package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Mechanic;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class MechanicRepository {

    private final Queries queries;

    public MechanicRepository(Db db) {
        this.queries = new Queries(db);
    }

    private static final String COLUMNS = "id, name, phone, specialization, available";

    public void save(Mechanic mechanic) {
        if (mechanic.getId() == 0 || findById(mechanic.getId()) == null) {
            insert(mechanic);
        } else {
            update(mechanic);
        }
    }

    public List<Mechanic> findAll() {
        return queries.read("SELECT " + COLUMNS + " FROM mechanics", MechanicRepository::build);
    }

    public Mechanic findById(int id) {
        return queries.readOne("SELECT " + COLUMNS + " FROM mechanics WHERE id = ?",
                statement -> statement.setInt(1, id), MechanicRepository::build);
    }

    public void delete(int id) {
        queries.write("DELETE FROM mechanics WHERE id = ?", statement -> statement.setInt(1, id));
    }

    private void insert(Mechanic mechanic) {
        String sql = "INSERT INTO mechanics (name, phone, specialization, available) VALUES (?, ?, ?, ?)";

        int id = queries.insert(sql, statement -> {
            statement.setString(1, mechanic.getName());
            statement.setString(2, mechanic.getPhone());
            statement.setString(3, mechanic.getSpecialization());
            statement.setBoolean(4, mechanic.isAvailable());
        });

        if (id > 0) {
            mechanic.setId(id);
        }
    }

    private void update(Mechanic mechanic) {
        String sql = "UPDATE mechanics SET name = ?, phone = ?, specialization = ?, available = ? WHERE id = ?";

        queries.write(sql, statement -> {
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
