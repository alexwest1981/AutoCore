package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.exception.DataAccessException;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.PaymentType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

public class PaymentRepository {

    private final Queries queries;

    public PaymentRepository(Db db) {
        this.queries = new Queries(db);
    }

    private static final String COLUMNS = "id, invoice_id, amount, payment_type, payment_date, successful";

    public void save(Payment payment) {
        if (payment.getId() == 0 || findById(payment.getId()) == null) {
            insert(payment);
        } else {
            update(payment);
        }
    }

    public List<Payment> findAll() {
        return queries.read("SELECT " + COLUMNS + " FROM payments", PaymentRepository::build);
    }

    public Payment findById(int id) {
        return queries.readOne("SELECT " + COLUMNS + " FROM payments WHERE id = ?",
                statement -> statement.setInt(1, id), PaymentRepository::build);
    }

    public void delete(int id) {
        queries.write("DELETE FROM payments WHERE id = ?", statement -> statement.setInt(1, id));
    }

    private void insert(Payment payment) {
        String sql = "INSERT INTO payments (invoice_id, amount, payment_type, payment_date, successful) "
                + "VALUES (?, ?, ?, ?, ?)";

        int id = queries.insert(sql, statement -> {
            statement.setInt(1, payment.getInvoiceId());
            statement.setInt(2, payment.getAmount());
            statement.setString(3, payment.getPaymentType().name());
            setPaymentDate(statement, 4, payment.getPaymentDate());
            statement.setBoolean(5, payment.isSuccessful());
        });

        if (id > 0) {
            payment.setId(id);
        }
    }

    private void update(Payment payment) {
        String sql = "UPDATE payments SET invoice_id = ?, amount = ?, payment_type = ?, payment_date = ?, "
                + "successful = ? WHERE id = ?";

        queries.write(sql, statement -> {
            statement.setInt(1, payment.getInvoiceId());
            statement.setInt(2, payment.getAmount());
            statement.setString(3, payment.getPaymentType().name());
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
                resultSet.getInt("amount"),
                PaymentType.fromText(resultSet.getString("payment_type"))
        );

        String dateText = resultSet.getString("payment_date");

        if (dateText != null) {

            try {
                payment.setPaymentDate(LocalDateTime.parse(dateText));
            } catch (java.time.format.DateTimeParseException e) {
                throw new DataAccessException("Could not read the payment date: " + dateText, e);
            }
        }

        payment.setSuccessful(resultSet.getBoolean("successful"));

        return payment;
    }
}
