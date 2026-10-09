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
import java.util.List;

public class InvoiceRepository {

    private static final String COLUMNS = "id, work_order_id, invoice_date, amount, discount, total_amount, paid";

    private static final InvoiceLineRepository invoiceLineRepository = new InvoiceLineRepository();

    public void save(Invoice invoice) throws SQLException {
        if (invoice.getId() == 0 || findById(invoice.getId()) == null) {
            insert(invoice);
        } else {
            update(invoice);
        }
    }

    public List<Invoice> findAll() throws SQLException {
        return Queries.read("SELECT " + COLUMNS + " FROM invoices", InvoiceRepository::build);
    }

    public Invoice findById(int id) throws SQLException {
        return Queries.readOne("SELECT " + COLUMNS + " FROM invoices WHERE id = ?",
                statement -> statement.setInt(1, id), InvoiceRepository::build);
    }

    public void delete(int id) throws SQLException {
        // The lines point at this invoice and nothing else clears them, so both go in one connection.
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM invoices WHERE id = ?")) {

            invoiceLineRepository.deleteByInvoiceId(connection, id);

            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private void insert(Invoice invoice) throws SQLException {
        String sql = "INSERT INTO invoices (work_order_id, invoice_date, amount, discount, total_amount, paid) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        // The invoice and its lines go in through one connection, so an invoice never sits without them.
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

            invoiceLineRepository.saveLines(connection, invoice);
        }
    }

    private void update(Invoice invoice) throws SQLException {
        String sql = "UPDATE invoices SET work_order_id = ?, invoice_date = ?, amount = ?, discount = ?, "
                + "total_amount = ?, paid = ? WHERE id = ?";

        Queries.write(sql, statement -> {
            statement.setInt(1, invoice.getWorkOrderId());
            setDate(statement, 2, invoice.getInvoiceDate());
            statement.setDouble(3, invoice.getAmount());
            statement.setDouble(4, invoice.getDiscount());
            statement.setDouble(5, invoice.getTotalAmount());
            statement.setBoolean(6, invoice.isPaid());
            statement.setInt(7, invoice.getId());
        });
    }

    private void setDate(PreparedStatement statement, int position, LocalDate date) throws SQLException {
        if (date == null) {
            statement.setNull(position, Types.VARCHAR);
        } else {
            statement.setString(position, date.toString());
        }
    }

    private static Invoice build(ResultSet resultSet) throws SQLException {
        String dateText = resultSet.getString("invoice_date");

        LocalDate invoiceDate = null;

        // A row written before the format was fixed can hold anything, and one bad row must not
        // stop the list.
        if (dateText != null) {
            try {
                invoiceDate = LocalDate.parse(dateText);
            } catch (java.time.format.DateTimeParseException e) {
                System.out.println("Hoppade över ett trasigt datum i databasen: " + dateText);
            }
        }

        Invoice invoice = new Invoice(
                resultSet.getInt("id"),
                resultSet.getInt("work_order_id"),
                invoiceDate,
                resultSet.getDouble("amount")
        );

        invoice.setDiscount(resultSet.getDouble("discount"));
        invoice.setPaid(resultSet.getBoolean("paid"));

        for (InvoiceLine line : invoiceLineRepository.findByInvoiceId(invoice.getId())) {
            invoice.addLine(line);
        }

        return invoice;
    }
}
