package com.wac.autocore.repository;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** The invoice_lines table: the rows a sent invoice is built from. */
public class InvoiceLineRepository {

    private static final String COLUMNS = "id, invoice_id, service_item_id, service_name, price, discount, package_name";

    public List<InvoiceLine> findByInvoiceId(int invoiceId) throws SQLException {
        return Queries.read("SELECT " + COLUMNS + " FROM invoice_lines WHERE invoice_id = ? ORDER BY id",
                statement -> statement.setInt(1, invoiceId), InvoiceLineRepository::build);
    }

    /** Saves the rows on an open connection, so everything runs in one transaction. */
    public void saveLines(Connection connection, Invoice invoice) throws SQLException {
        String sql = "INSERT INTO invoice_lines (invoice_id, service_item_id, service_name, price, discount, package_name) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (InvoiceLine line : invoice.getLines()) {
                line.setInvoiceId(invoice.getId());

                statement.setInt(1, line.getInvoiceId());
                statement.setInt(2, line.getServiceItemId());
                statement.setString(3, line.getServiceName());
                statement.setDouble(4, line.getPrice());
                statement.setDouble(5, line.getDiscount());
                statement.setString(6, line.getPackageName().isEmpty() ? null : line.getPackageName());
                statement.executeUpdate();

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        line.setId(generatedKeys.getInt(1));
                    }
                }
            }
        }
    }

    public void deleteByInvoiceId(Connection connection, int invoiceId) throws SQLException {
        String sql = "DELETE FROM invoice_lines WHERE invoice_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, invoiceId);
            statement.executeUpdate();
        }
    }

    private static InvoiceLine build(ResultSet resultSet) throws SQLException {
        InvoiceLine line = new InvoiceLine(
                resultSet.getInt("id"),
                resultSet.getInt("invoice_id"),
                resultSet.getInt("service_item_id"),
                resultSet.getString("service_name"),
                resultSet.getDouble("price"),
                resultSet.getDouble("discount"));

        line.setPackageName(resultSet.getString("package_name"));

        return line;
    }
}
