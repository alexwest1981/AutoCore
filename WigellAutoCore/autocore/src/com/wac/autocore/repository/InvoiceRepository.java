package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;

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
        String deleteLinesSql = "DELETE FROM invoice_lines WHERE invoice_id = ?";
        String sql = "DELETE FROM invoices WHERE id = ?";

        try (Connection connection = Db.getConnection();
             PreparedStatement linesStatement = connection.prepareStatement(deleteLinesSql);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            linesStatement.setInt(1, id);
            linesStatement.executeUpdate();

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

            insertLines(connection, invoice);
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

        loadLines(invoice);

        return invoice;
    }

    private void insertLines(Connection connection, Invoice invoice) throws SQLException {
        String sql = "INSERT INTO invoice_lines (invoice_id, service_item_id, service_name, price, discount) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (InvoiceLine line : invoice.getLines()) {
                line.setInvoiceId(invoice.getId());

                statement.setInt(1, line.getInvoiceId());
                statement.setInt(2, line.getServiceItemId());
                statement.setString(3, line.getServiceName());
                statement.setDouble(4, line.getPrice());
                statement.setDouble(5, line.getDiscount());
                statement.executeUpdate();

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        line.setId(generatedKeys.getInt(1));
                    }
                }
            }
        }
    }

    private void loadLines(Invoice invoice) throws SQLException {
        String sql = "SELECT id, invoice_id, service_item_id, service_name, price, discount "
                + "FROM invoice_lines WHERE invoice_id = ? ORDER BY id";

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, invoice.getId());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    invoice.addLine(new InvoiceLine(
                            resultSet.getInt("id"),
                            resultSet.getInt("invoice_id"),
                            resultSet.getInt("service_item_id"),
                            resultSet.getString("service_name"),
                            resultSet.getDouble("price"),
                            resultSet.getDouble("discount")));
                }
            }
        }
    }
}
