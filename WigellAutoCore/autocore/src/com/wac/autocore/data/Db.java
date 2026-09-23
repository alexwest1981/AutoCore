package com.wac.autocore.data;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

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
            loadSampleDataIfEmpty(connection);
            System.out.println("Databas redo: " + DATABASE_PATH);

        } catch (SQLException e) {
            System.out.println("Kunde inte skapa tabellerna: " + e.getMessage());
        }
    }

    private static void loadSampleDataIfEmpty(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM customers")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        String insertCustomer = "INSERT INTO customers (id, name, phone, email, vip) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertCustomer)) {
            addCustomer(ps, 1, "Anna Andersson", "070-1111111", "anna.andersson@email.se", 0);
            addCustomer(ps, 2, "Erik Eriksson", "070-2222222", "erik.eriksson@email.se", 1);
            addCustomer(ps, 3, "Maria Svensson", "070-3333333", "maria.svensson@email.se", 0);
            addCustomer(ps, 4, "Olof Palme", "070-4444444", "olof.palme@email.se", 0);
            addCustomer(ps, 5, "Sven Melander", "070-5555555", "sven.melander@email.se", 0);
            addCustomer(ps, 6, "Gustav Vasa", "070-6666666", "gustav.vasa@email.se", 0);
        }

        String insertVehicle = "INSERT INTO vehicles (id, registration_number, brand, model, year, customer_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertVehicle)) {
            addVehicle(ps, 1, "ABC123", "Volvo", "V70", 2012, 1);
            addVehicle(ps, 2, "DEF456", "Volkswagen", "Passat", 2018, 2);
            addVehicle(ps, 3, "GHI789", "Toyota", "Corolla", 2020, 3);
            addVehicle(ps, 4, "XYZ999", "BMW", "320d", 2021, 4);
            addVehicle(ps, 5, "AAA001", "Audi", "A4", 2019, 5);
            addVehicle(ps, 6, "BBB002", "Mercedes", "C220", 2022, 6);
        }

        String insertMechanic = "INSERT INTO mechanics (id, name, phone, specialization, available) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertMechanic)) {
            addMechanic(ps, 1, "Johan Karlsson", "070-5551111", "General service", 1);
            addMechanic(ps, 2, "Sara Nilsson", "070-5552222", "Brakes", 1);
            addMechanic(ps, 3, "Mikael Berg", "070-5553333", "Diagnostics", 1);
        }

        String insertServiceItem = "INSERT INTO service_items (id, name, description, price, estimated_minutes) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertServiceItem)) {
            addServiceItem(ps, 1, "Oil change", "Engine oil and oil filter replacement", 1295.0, 45);
            addServiceItem(ps, 2, "Brake service", "Inspection and replacement of front brake pads", 2495.0, 90);
            addServiceItem(ps, 3, "Diagnostics", "Electronic fault code diagnostics", 995.0, 60);
            addServiceItem(ps, 4, "Annual service", "Standard annual vehicle service", 3495.0, 120);
        }

        String insertBooking = "INSERT INTO bookings (id, vehicle_id, date, description, status) VALUES (?, ?, ?, ?, ?)";
        LocalDate today = LocalDate.now();
        try (PreparedStatement ps = conn.prepareStatement(insertBooking)) {
            addBooking(ps, 1, 1, today.toString(), "Oljeservice & filterbyte", "CREATED");
            addBooking(ps, 2, 2, today.toString(), "Bromskontroll fram", "CREATED");
            addBooking(ps, 3, 3, today.plusDays(1).toString(), "Helrenovering bromsar", "CREATED");
            addBooking(ps, 4, 4, today.toString(), "Bromsok och belägg", "CREATED");
            addBooking(ps, 5, 5, today.toString(), "Felkodsläsning OBD2", "CREATED");
            addBooking(ps, 6, 6, today.plusDays(1).toString(), "Elektronikfelsökning", "CREATED");
        }

        String insertWorkOrder = "INSERT INTO work_orders (id, booking_id, mechanic_id, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertWorkOrder)) {
            addWorkOrder(ps, 1, 1, 1, "IN_PROGRESS");
            addWorkOrder(ps, 2, 2, 1, "CREATED");
            addWorkOrder(ps, 3, 4, 2, "IN_PROGRESS");
            addWorkOrder(ps, 4, 5, 3, "IN_PROGRESS");
            addWorkOrder(ps, 5, 3, 1, "CREATED");
            addWorkOrder(ps, 6, 6, 3, "CREATED");
        }

        String insertWosi = "INSERT INTO work_order_service_items (work_order_id, service_item_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertWosi)) {
            addWosi(ps, 1, 1);
            addWosi(ps, 2, 2);
            addWosi(ps, 3, 2);
            addWosi(ps, 4, 3);
            addWosi(ps, 5, 2);
            addWosi(ps, 5, 4);
            addWosi(ps, 6, 3);
        }
    }

    private static void addCustomer(PreparedStatement ps, int id, String name, String phone, String email, int vip) throws SQLException {
        ps.setInt(1, id);
        ps.setString(2, name);
        ps.setString(3, phone);
        ps.setString(4, email);
        ps.setInt(5, vip);
        ps.executeUpdate();
    }

    private static void addVehicle(PreparedStatement ps, int id, String reg, String brand, String model, int year, int custId) throws SQLException {
        ps.setInt(1, id);
        ps.setString(2, reg);
        ps.setString(3, brand);
        ps.setString(4, model);
        ps.setInt(5, year);
        ps.setInt(6, custId);
        ps.executeUpdate();
    }

    private static void addMechanic(PreparedStatement ps, int id, String name, String phone, String spec, int avail) throws SQLException {
        ps.setInt(1, id);
        ps.setString(2, name);
        ps.setString(3, phone);
        ps.setString(4, spec);
        ps.setInt(5, avail);
        ps.executeUpdate();
    }

    private static void addServiceItem(PreparedStatement ps, int id, String name, String desc, double price, int min) throws SQLException {
        ps.setInt(1, id);
        ps.setString(2, name);
        ps.setString(3, desc);
        ps.setDouble(4, price);
        ps.setInt(5, min);
        ps.executeUpdate();
    }

    private static void addBooking(PreparedStatement ps, int id, int vId, String date, String desc, String status) throws SQLException {
        ps.setInt(1, id);
        ps.setInt(2, vId);
        ps.setString(3, date);
        ps.setString(4, desc);
        ps.setString(5, status);
        ps.executeUpdate();
    }

    private static void addWorkOrder(PreparedStatement ps, int id, int bId, int mId, String status) throws SQLException {
        ps.setInt(1, id);
        ps.setInt(2, bId);
        ps.setInt(3, mId);
        ps.setString(4, status);
        ps.executeUpdate();
    }

    private static void addWosi(PreparedStatement ps, int woId, int siId) throws SQLException {
        ps.setInt(1, woId);
        ps.setInt(2, siId);
        ps.executeUpdate();
    }
}
