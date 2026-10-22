package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.data.DatabaseWork;
import com.wac.autocore.exception.DataAccessException;
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

    private final Db db;

    private final Queries queries;

    private static final String COLUMNS = "id, work_order_id, invoice_date, invoice_number, credit_of, "
            + "credit_reason, amount, discount, total_amount, paid";

    private final InvoiceLineRepository invoiceLineRepository;

    public InvoiceRepository(Db db, InvoiceLineRepository invoiceLineRepository) {
        this.db = db;
        this.queries = new Queries(db);
        this.invoiceLineRepository = invoiceLineRepository;
    }

    /**
     * Writes a new invoice, or marks an existing one paid. An invoice that has been issued is never
     * rewritten: the number, the amounts and the lines stay as they were sent, and a correction is
     * a credit invoice of its own.
     */
    public void save(final Invoice invoice) {
        final boolean isNew = invoice.getId() == 0 || findById(invoice.getId()) == null;

        new DatabaseWork<Void>(db) {
            @Override
            protected Void step(Connection connection) throws SQLException {
                if (isNew) {
                    insert(connection, invoice);
                } else {
                    markPaid(connection, invoice);
                }
                return null;
            }
        }.run();
    }

    public List<Invoice> findAll() {
        return queries.read("SELECT " + COLUMNS + " FROM invoices", resultSet -> build(resultSet));
    }

    public Invoice findById(int id) {
        return queries.readOne("SELECT " + COLUMNS + " FROM invoices WHERE id = ?",
                statement -> statement.setInt(1, id), resultSet -> build(resultSet));
    }

    /** The invoice a credit invoice takes back, or null when this one has not been credited. */
    public Invoice findCreditFor(int invoiceId) {
        return queries.readOne("SELECT " + COLUMNS + " FROM invoices WHERE credit_of = ? ORDER BY id",
                statement -> statement.setInt(1, invoiceId), resultSet -> build(resultSet));
    }

    private void insert(Connection connection, Invoice invoice) throws SQLException {
        // The number is taken in the same transaction as the row, so two invoices cannot be given
        // the same one. shortcoming: one installation at a time, since the series is per database.
        String number = invoice.getNumber().isEmpty()
                ? nextNumber(connection, invoice.getInvoiceDate())
                : invoice.getNumber();

        String sql = "INSERT INTO invoices (work_order_id, invoice_date, invoice_number, credit_of, "
                + "credit_reason, amount, discount, total_amount, paid) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int id = Queries.insert(connection, sql, statement -> {
            Queries.setReference(statement, 1, invoice.getWorkOrderId());
            setDate(statement, 2, invoice.getInvoiceDate());
            statement.setString(3, number);
            Queries.setReference(statement, 4, invoice.getCreditOf());
            statement.setString(5, invoice.getCreditReason().isEmpty() ? null : invoice.getCreditReason());
            statement.setInt(6, invoice.getAmount());
            statement.setInt(7, invoice.getDiscount());
            statement.setInt(8, invoice.getTotalAmount());
            statement.setBoolean(9, invoice.isPaid());
        });

        if (id > 0) {
            invoice.setId(id);
        }
        invoice.setNumber(number);

        invoiceLineRepository.saveLines(connection, invoice);
    }

    /** The year's series is the only one: YYYY-NNNN, next number in the year of the invoice date. */
    private String nextNumber(Connection connection, LocalDate invoiceDate) throws SQLException {
        int year = (invoiceDate == null ? LocalDate.now() : invoiceDate).getYear();
        String prefix = year + "-";

        int highest = 0;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT invoice_number FROM invoices WHERE invoice_number LIKE ?")) {
            statement.setString(1, prefix + "%");
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    String existing = rows.getString(1);
                    try {
                        highest = Math.max(highest, Integer.parseInt(existing.substring(prefix.length())));
                    } catch (NumberFormatException | StringIndexOutOfBoundsException ignored) {
                        // A number outside the series is not one of ours and cannot raise the count.
                    }
                }
            }
        }

        return prefix + String.format("%04d", highest + 1);
    }

    /** The only change an issued invoice allows. Everything else about it is history. */
    private void markPaid(Connection connection, Invoice invoice) throws SQLException {
        queries.write(connection, "UPDATE invoices SET paid = ? WHERE id = ?", statement -> {
            statement.setBoolean(1, invoice.isPaid());
            statement.setInt(2, invoice.getId());
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
                throw new DataAccessException("Could not read the invoice date: " + dateText, e);
            }
        }

        Invoice invoice = new Invoice(
                resultSet.getInt("id"),
                resultSet.getInt("work_order_id"),
                invoiceDate,
                resultSet.getInt("amount")
        );

        invoice.setNumber(resultSet.getString("invoice_number"));
        invoice.setCreditOf(resultSet.getInt("credit_of"));
        invoice.setCreditReason(resultSet.getString("credit_reason"));
        invoice.setDiscount(resultSet.getInt("discount"));
        invoice.setPaid(resultSet.getBoolean("paid"));

        for (InvoiceLine line : invoiceLineRepository.findByInvoiceId(invoice.getId())) {
            invoice.addLine(line);
        }

        return invoice;
    }
}
