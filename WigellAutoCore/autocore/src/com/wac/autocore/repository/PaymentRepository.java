package com.wac.autocore.repository;

import com.wac.autocore.model.Payment;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

public class PaymentRepository {

    private static final String COLUMNS = "id, invoice_id, amount, payment_type, payment_date, successful";

    public void save(Payment payment) throws SQLException {
        if (payment.getId() == 0 || findById(payment.getId()) == null) {
            insert(payment);
        } else {
            update(payment);
        }
    }

    public List<Payment> findAll() throws SQLException {
        return Queries.read("SELECT " + COLUMNS + " FROM payments", PaymentRepository::build);
    }

    public Payment findById(int id) throws SQLException {
        return Queries.readOne("SELECT " + COLUMNS + " FROM payments WHERE id = ?",
                statement -> statement.setInt(1, id), PaymentRepository::build);
    }

    public void delete(int id) throws SQLException {
        Queries.write("DELETE FROM payments WHERE id = ?", statement -> statement.setInt(1, id));
    }

    private void insert(Payment payment) throws SQLException {
        String sql = "INSERT INTO payments (invoice_id, amount, payment_type, payment_date, successful) "
                + "VALUES (?, ?, ?, ?, ?)";

        int id = Queries.insert(sql, statement -> {
            statement.setInt(1, payment.getInvoiceId());
            statement.setDouble(2, payment.getAmount());
            statement.setString(3, payment.getPaymentType());
            setPaymentDate(statement, 4, payment.getPaymentDate());
            statement.setBoolean(5, payment.isSuccessful());
        });

        if (id > 0) {
            payment.setId(id);
        }
    }

    private void update(Payment payment) throws SQLException {
        String sql = "UPDATE payments SET invoice_id = ?, amount = ?, payment_type = ?, payment_date = ?, "
                + "successful = ? WHERE id = ?";

        Queries.write(sql, statement -> {
            statement.setInt(1, payment.getInvoiceId());
            statement.setDouble(2, payment.getAmount());
            statement.setString(3, payment.getPaymentType());
            setPaymentDate(statement, 4, payment.getPaymentDate());
            statement.setBoolean(5, payment.isSuccessful());
            statement.setInt(6, payment.getId());
        });
    }

    private void setPaymentDate(PreparedStatement statement, int position, LocalDateTime paymentDate) throws SQLException {
        if (paymentDate == null) {
            statement.setNull(position, Types.VARCHAR);
        } else {
            statement.setString(position, paymentDate.toString());
        }
    }

    private static Payment build(ResultSet resultSet) throws SQLException {
        Payment payment = new Payment(
                resultSet.getInt("id"),
                resultSet.getInt("invoice_id"),
                resultSet.getDouble("amount"),
                resultSet.getString("payment_type")
        );

        String dateText = resultSet.getString("payment_date");

        // A row written before the format was fixed can hold anything, and one bad row must not
        // stop the list.
        if (dateText != null) {

            try {
                payment.setPaymentDate(LocalDateTime.parse(dateText));
            } catch (java.time.format.DateTimeParseException e) {
                System.out.println("Hoppade över ett trasigt datum i databasen: " + dateText);
            }
        }

        payment.setSuccessful(resultSet.getBoolean("successful"));

        return payment;
    }
}
