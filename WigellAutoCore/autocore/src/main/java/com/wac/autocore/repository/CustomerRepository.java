package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Customer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class CustomerRepository {

    private final Queries queries;

    public CustomerRepository(Db db) {
        this.queries = new Queries(db);
    }

    private static final String COLUMNS = "id, name, phone, email, vip";

    public void save(Customer customer) {
        if (customer.getId() == 0 || findById(customer.getId()) == null) {
            insert(customer);
        } else {
            update(customer);
        }
    }

    public List<Customer> findAll() {
        return queries.read("SELECT " + COLUMNS + " FROM customers", CustomerRepository::build);
    }

    public Customer findById(int id) {
        return queries.readOne("SELECT " + COLUMNS + " FROM customers WHERE id = ?",
                statement -> statement.setInt(1, id), CustomerRepository::build);
    }

    public void delete(int id) {
        queries.write("DELETE FROM customers WHERE id = ?", statement -> statement.setInt(1, id));
    }

    private void insert(Customer customer) {
        String sql = "INSERT INTO customers (name, phone, email, vip) VALUES (?, ?, ?, ?)";

        // Only set the id when the database gave one, as before.
        int id = queries.insert(sql, statement -> {
            statement.setString(1, customer.getName());
            statement.setString(2, customer.getPhone());
            statement.setString(3, customer.getEmail());
            statement.setInt(4, customer.isVip() ? 1 : 0);
        });

        if (id > 0) {
            customer.setId(id);
        }
    }

    private void update(Customer customer) {
        String sql = "UPDATE customers SET name = ?, phone = ?, email = ?, vip = ? WHERE id = ?";

        queries.write(sql, statement -> {
            statement.setString(1, customer.getName());
            statement.setString(2, customer.getPhone());
            statement.setString(3, customer.getEmail());
            statement.setInt(4, customer.isVip() ? 1 : 0);
            statement.setInt(5, customer.getId());
        });
    }

    private static Customer build(ResultSet resultSet) throws SQLException {
        Customer customer = new Customer(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("phone"),
                resultSet.getString("email")
        );

        customer.setVip(resultSet.getInt("vip") == 1);

        return customer;
    }
}
