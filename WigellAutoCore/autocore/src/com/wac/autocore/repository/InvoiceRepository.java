package com.wac.autocore.repository;

import com.wac.autocore.data.DatabaseWork;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;

public class InvoiceRepository {

    private static final String COLUMNS = "id, work_order_id, invoice_date, amount, discount, total_amount, paid";

    private final InvoiceLineRepository invoiceLineRepository;

    public InvoiceRepository(InvoiceLineRepository invoiceLineRepository) {
        this.invoiceLineRepository = invoiceLineRepository;
    }

    public void save(final Invoice invoice) {
        final boolean isNew = invoice.getId() == 0 || findById(invoice.getId()) == null;

        new DatabaseWork<Void>() {
            @Override
            protected Void step(Connection connection) throws SQLException {
                if (isNew) {
                    insert(connection, invoice);
                } else {
                    update(connection, invoice);
                }
                return null;
            }
        }.run();
    }

    public List<Invoice> findAll() {
        return Queries.read("SELECT " + COLUMNS + " FROM invoices", resultSet -> build(resultSet));
    }

    public Invoice findById(int id) {
        return Queries.readOne("SELECT " + COLUMNS + " FROM invoices WHERE id = ?",
                statement -> statement.setInt(1, id), resultSet -> build(resultSet));
    }

    public void delete(final int id) {
        // The lines point at this invoice and nothing else clears them, so both go in one transaction.
        new DatabaseWork<Void>() {
            @Override
            protected Void step(Connection connection) throws SQLException {
                invoiceLineRepository.deleteByInvoiceId(connection, id);
                Queries.write(connection, "DELETE FROM invoices WHERE id = ?",
                        statement -> statement.setInt(1, id));
                return null;
            }
        }.run();
    }

    private void insert(Connection connection, Invoice invoice) throws SQLException {
        String sql = "INSERT INTO invoices (work_order_id, invoice_date, amount, discount, total_amount, paid) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        int id = Queries.insert(connection, sql, statement -> {
            Queries.setReference(statement, 1, invoice.getWorkOrderId());
            setDate(statement, 2, invoice.getInvoiceDate());
            statement.setDouble(3, invoice.getAmount());
            statement.setDouble(4, invoice.getDiscount());
            statement.setDouble(5, invoice.getTotalAmount());
            statement.setBoolean(6, invoice.isPaid());
        });

        if (id > 0) {
            invoice.setId(id);
        }

        invoiceLineRepository.saveLines(connection, invoice);
    }

    private void update(Connection connection, Invoice invoice) throws SQLException {
        String sql = "UPDATE invoices SET work_order_id = ?, invoice_date = ?, amount = ?, discount = ?, "
                + "total_amount = ?, paid = ? WHERE id = ?";

        Queries.write(connection, sql, statement -> {
            Queries.setReference(statement, 1, invoice.getWorkOrderId());
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

    private Invoice build(ResultSet resultSet) throws SQLException {
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
