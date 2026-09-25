package com.wac.autocore.data;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Db {

    private static final String DATABASE_PATH = "data/autocore.db";

    private static boolean ready = false;

    public static Connection getConnection() throws SQLException {
        File dataDirectory = new File("data");
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }
        return DriverManager.getConnection("jdbc:sqlite:" + DATABASE_PATH);
    }

    public static void ensureReady() {
        if (ready) {
            return;
        }
        ready = true;
        initTables();
        SeedData.seedIfEmpty();
    }

    public static void initTables() {
        String[] createStatements = {
            "CREATE TABLE IF NOT EXISTS customers ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "phone TEXT, "
                + "email TEXT, "
                + "vip INTEGER DEFAULT 0)",

            "CREATE TABLE IF NOT EXISTS vehicles ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "registration_number TEXT NOT NULL, "
                + "brand TEXT, "
                + "model TEXT, "
                + "year INTEGER, "
                + "customer_id INTEGER)",

            "CREATE TABLE IF NOT EXISTS mechanics ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "phone TEXT, "
                + "specialization TEXT, "
                + "available INTEGER DEFAULT 1)",

            "CREATE TABLE IF NOT EXISTS service_items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "description TEXT, "
                + "price REAL, "
                + "estimated_minutes INTEGER)",

            "CREATE TABLE IF NOT EXISTS bookings ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "vehicle_id INTEGER, "
                + "start_time TEXT, "
                + "end_time TEXT, "
                + "mechanic_id INTEGER, "
                + "service_item_id INTEGER, "
                + "date TEXT, "
                + "description TEXT, "
                + "status TEXT)",

            "CREATE TABLE IF NOT EXISTS work_orders ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "booking_id INTEGER, "
                + "mechanic_id INTEGER, "
                + "status TEXT)",

            "CREATE TABLE IF NOT EXISTS work_order_service_items ("
                + "work_order_id INTEGER NOT NULL, "
                + "service_item_id INTEGER NOT NULL, "
                + "PRIMARY KEY (work_order_id, service_item_id))",

            "CREATE TABLE IF NOT EXISTS invoices ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "work_order_id INTEGER, "
                + "invoice_date TEXT, "
                + "amount REAL, "
                + "discount REAL, "
                + "total_amount REAL, "
                + "paid INTEGER DEFAULT 0)",

            "CREATE TABLE IF NOT EXISTS payments ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "invoice_id INTEGER, "
                + "amount REAL, "
                + "payment_type TEXT, "
                + "payment_date TEXT, "
                + "successful INTEGER DEFAULT 0)"
        };

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            for (String sql : createStatements) {
                statement.executeUpdate(sql);
            }
            System.out.println("Databas redo: " + DATABASE_PATH);

        } catch (SQLException e) {
            System.out.println("Kunde inte skapa tabellerna: " + e.getMessage());
        }

        SeedData.seedIfEmpty();
    }
}
