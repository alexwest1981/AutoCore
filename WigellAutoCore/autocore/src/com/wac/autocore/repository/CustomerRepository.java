package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CustomerRepository {

    public void save(Customer customer) throws SQLException {
        if (customer.getId() == 0 || findById(customer.getId()) == null) {
            insert(customer);
        } else {
            update(customer);
        }
    }

    public List<Customer> findAll() throws SQLException {
        List<Customer> customers = new ArrayList<Customer>();
        String sql = "SELECT id, name, phone, email, vip FROM customers";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                customers.add(buildCustomer(resultSet));
            }
        }

        return customers;
    }

    public Customer findById(int id) throws SQLException {
        String sql = "SELECT id, name, phone, email, vip FROM customers WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return buildCustomer(resultSet);
                }
            }
        }

        return null;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM customers WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private void insert(Customer customer) throws SQLException {
        String sql = "INSERT INTO customers (name, phone, email, vip) VALUES (?, ?, ?, ?)";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, customer.getName());
            statement.setString(2, customer.getPhone());
            statement.setString(3, customer.getEmail());
            statement.setInt(4, customer.isVip() ? 1 : 0);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    customer.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    private void update(Customer customer) throws SQLException {
        String sql = "UPDATE customers SET name = ?, phone = ?, email = ?, vip = ? WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, customer.getName());
            statement.setString(2, customer.getPhone());
            statement.setString(3, customer.getEmail());
            statement.setInt(4, customer.isVip() ? 1 : 0);
            statement.setInt(5, customer.getId());
            statement.executeUpdate();
        }
    }

    private Customer buildCustomer(ResultSet resultSet) throws SQLException {
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
