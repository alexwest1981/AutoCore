package com.wac.autocore.repository;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Tabellen invoice_lines: raderna en skickad faktura är byggd av. */
public class InvoiceLineRepository {

    public List<InvoiceLine> findByInvoiceId(int invoiceId) throws SQLException {
        String sql = "SELECT id, invoice_id, service_item_id, service_name, price, discount " +
                "FROM invoice_lines WHERE invoice_id = ? ORDER BY id";

        List<InvoiceLine> lines = new ArrayList<InvoiceLine>();

        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, invoiceId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    lines.add(new InvoiceLine(
                            resultSet.getInt("id"),
                            resultSet.getInt("invoice_id"),
                            resultSet.getInt("service_item_id"),
                            resultSet.getString("service_name"),
                            resultSet.getDouble("price"),
                            resultSet.getDouble("discount")));
                }
            }
        }

        return lines;
    }

/** Sparar raderna på en öppen anslutning, så allt går i samma transaktion. */
    public void saveLines(Connection connection, Invoice invoice) throws SQLException {
        String sql = "INSERT INTO invoice_lines (invoice_id, service_item_id, service_name, price, discount) " +
                "VALUES (?, ?, ?, ?, ?)";

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

    public void deleteByInvoiceId(int invoiceId) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            deleteByInvoiceId(connection, invoiceId);
        }
    }

    public void deleteByInvoiceId(Connection connection, int invoiceId) throws SQLException {
        String sql = "DELETE FROM invoice_lines WHERE invoice_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, invoiceId);
            statement.executeUpdate();
        }
    }
}
