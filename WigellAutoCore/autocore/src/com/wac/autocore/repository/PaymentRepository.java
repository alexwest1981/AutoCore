package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Payment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PaymentRepository {

    public void save(Payment payment) throws SQLException {
        if (payment.getId() == 0 || findById(payment.getId()) == null) {
            insert(payment);
        } else {
            update(payment);
        }
    }

    public List<Payment> findAll() throws SQLException {
        List<Payment> payments = new ArrayList<Payment>();
        String sql = "SELECT id, invoice_id, amount, payment_type, payment_date, successful FROM payments";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                payments.add(buildPayment(resultSet));
            }
        }

        return payments;
    }

    public Payment findById(int id) throws SQLException {
        String sql = "SELECT id, invoice_id, amount, payment_type, payment_date, successful FROM payments WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return buildPayment(resultSet);
                }
            }
        }

        return null;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM payments WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private void insert(Payment payment) throws SQLException {
        String sql = "INSERT INTO payments (invoice_id, amount, payment_type, payment_date, successful) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, payment.getInvoiceId());
            statement.setDouble(2, payment.getAmount());
            statement.setString(3, payment.getPaymentType());
            setPaymentDate(statement, 4, payment.getPaymentDate());
            statement.setBoolean(5, payment.isSuccessful());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    payment.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    private void update(Payment payment) throws SQLException {
        String sql = "UPDATE payments SET invoice_id = ?, amount = ?, payment_type = ?, payment_date = ?, "
                + "successful = ? WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, payment.getInvoiceId());
            statement.setDouble(2, payment.getAmount());
            statement.setString(3, payment.getPaymentType());
            setPaymentDate(statement, 4, payment.getPaymentDate());
            statement.setBoolean(5, payment.isSuccessful());
            statement.setInt(6, payment.getId());
            statement.executeUpdate();
        }
    }

    private void setPaymentDate(PreparedStatement statement, int position, LocalDateTime paymentDate) throws SQLException {
        if (paymentDate == null) {
            statement.setNull(position, Types.VARCHAR);
        } else {
            statement.setString(position, paymentDate.toString());
        }
    }

    private Payment buildPayment(ResultSet resultSet) throws SQLException {
        Payment payment = new Payment(
                resultSet.getInt("id"),
                resultSet.getInt("invoice_id"),
                resultSet.getDouble("amount"),
                resultSet.getString("payment_type")
        );

        String dateText = resultSet.getString("payment_date");
        if (dateText != null) {
            payment.setPaymentDate(LocalDateTime.parse(dateText));
        }

        payment.setSuccessful(resultSet.getBoolean("successful"));

        return payment;
    }
}
