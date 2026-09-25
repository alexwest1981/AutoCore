package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Invoice;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvoiceRepository {

    public void save(Invoice invoice) throws SQLException {
        if (invoice.getId() == 0 || findById(invoice.getId()) == null) {
            insert(invoice);
        } else {
            update(invoice);
        }
    }

    public List<Invoice> findAll() throws SQLException {
        List<Invoice> invoices = new ArrayList<Invoice>();
        String sql = "SELECT id, work_order_id, invoice_date, amount, discount, total_amount, paid FROM invoices";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                invoices.add(buildInvoice(resultSet));
            }
        }

        return invoices;
    }

    public Invoice findById(int id) throws SQLException {
        String sql = "SELECT id, work_order_id, invoice_date, amount, discount, total_amount, paid FROM invoices WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return buildInvoice(resultSet);
                }
            }
        }

        return null;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM invoices WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private void insert(Invoice invoice) throws SQLException {
        String sql = "INSERT INTO invoices (work_order_id, invoice_date, amount, discount, total_amount, paid) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, invoice.getWorkOrderId());
            setDate(statement, 2, invoice.getInvoiceDate());
            statement.setDouble(3, invoice.getAmount());
            statement.setDouble(4, invoice.getDiscount());
            statement.setDouble(5, invoice.getTotalAmount());
            statement.setBoolean(6, invoice.isPaid());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    invoice.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    private void update(Invoice invoice) throws SQLException {
        String sql = "UPDATE invoices SET work_order_id = ?, invoice_date = ?, amount = ?, discount = ?, "
                + "total_amount = ?, paid = ? WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, invoice.getWorkOrderId());
            setDate(statement, 2, invoice.getInvoiceDate());
            statement.setDouble(3, invoice.getAmount());
            statement.setDouble(4, invoice.getDiscount());
            statement.setDouble(5, invoice.getTotalAmount());
            statement.setBoolean(6, invoice.isPaid());
            statement.setInt(7, invoice.getId());
            statement.executeUpdate();
        }
    }

    private void setDate(PreparedStatement statement, int position, LocalDate date) throws SQLException {
        if (date == null) {
            statement.setNull(position, Types.VARCHAR);
        } else {
            statement.setString(position, date.toString());
        }
    }

    private Invoice buildInvoice(ResultSet resultSet) throws SQLException {
        String dateText = resultSet.getString("invoice_date");

        Invoice invoice = new Invoice(
                resultSet.getInt("id"),
                resultSet.getInt("work_order_id"),
                dateText == null ? null : LocalDate.parse(dateText),
                resultSet.getDouble("amount")
        );

        invoice.setDiscount(resultSet.getDouble("discount"));
        invoice.setPaid(resultSet.getBoolean("paid"));

        return invoice;
    }
}
