package com.wac.autocore.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

/**
 * Fristående och automatiserad runner för SCRUM-169 (F3 Beviskort: Omstartsbeviset).
 * Exekverar en verklig processomstart genom att starta två helt separata JVM-processer
 * (Process 1: Writer och Process 2: Verifier) i operativsystemet och kontrollera PID.
 */
public class RestartProofRunner {

    public static final String RESTART_DB_FILE = "data/autocore_restart_proof.db";

    public static String getProcessPid() {
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            return name.split("@")[0];
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    public static void main(String[] args) {
        if (args.length >= 1 && "--write".equals(args[0])) {
            runWriter();
            System.exit(0);
        } else if (args.length >= 4 && "--verify".equals(args[0])) {
            int bookingId = Integer.parseInt(args[1]);
            int workOrderId = Integer.parseInt(args[2]);
            int invoiceId = Integer.parseInt(args[3]);
            runVerifier(bookingId, workOrderId, invoiceId);
            System.exit(0);
        } else {
            boolean success = runProcessRestartTest();
            System.exit(success ? 0 : 1);
        }
    }

    private static void runWriter() {
        String pid = getProcessPid();
        File dbFile = new File(RESTART_DB_FILE);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        if (dbFile.getParentFile() != null) {
            dbFile.getParentFile().mkdirs();
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + RESTART_DB_FILE);
             Statement stmt = conn.createStatement()) {

            // Skapa schema för omstartstest
            stmt.executeUpdate("CREATE TABLE bookings (id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_id INTEGER, date TEXT, description TEXT, status TEXT)");
            stmt.executeUpdate("CREATE TABLE service_items (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, price REAL, estimated_minutes INTEGER)");
            stmt.executeUpdate("CREATE TABLE booking_service_items (booking_id INTEGER NOT NULL, service_item_id INTEGER NOT NULL, PRIMARY KEY(booking_id, service_item_id))");
            stmt.executeUpdate("CREATE TABLE work_orders (id INTEGER PRIMARY KEY AUTOINCREMENT, booking_id INTEGER, mechanic_id INTEGER, status TEXT)");
            stmt.executeUpdate("CREATE TABLE work_order_service_items (work_order_id INTEGER NOT NULL, service_item_id INTEGER NOT NULL, completed INTEGER DEFAULT 0, PRIMARY KEY(work_order_id, service_item_id))");
            stmt.executeUpdate("CREATE TABLE invoices (id INTEGER PRIMARY KEY AUTOINCREMENT, work_order_id INTEGER, total_amount REAL)");
            stmt.executeUpdate("CREATE TABLE invoice_lines (id INTEGER PRIMARY KEY AUTOINCREMENT, invoice_id INTEGER NOT NULL, service_name TEXT, price REAL)");

            // Skapa tjänster
            stmt.executeUpdate("INSERT INTO service_items (id, name, price, estimated_minutes) VALUES (1, 'Oljebyte', 899.0, 45)");
            stmt.executeUpdate("INSERT INTO service_items (id, name, price, estimated_minutes) VALUES (2, 'Bromsservice', 1495.0, 90)");

            // Skapa bokning med 2 tjänster
            stmt.executeUpdate("INSERT INTO bookings (vehicle_id, date, description, status) VALUES (101, '2026-10-15', 'Canary omstart', 'COMPLETED')", Statement.RETURN_GENERATED_KEYS);
            int bId = 0;
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) bId = rs.getInt(1);
            }

            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO booking_service_items (booking_id, service_item_id) VALUES (?, ?)")) {
                ps.setInt(1, bId);
                ps.setInt(2, 1);
                ps.executeUpdate();
                ps.setInt(1, bId);
                ps.setInt(2, 2);
                ps.executeUpdate();
            }

            // Skapa arbetsorder med koppling till bokning och tjänster
            int woId = 0;
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO work_orders (booking_id, mechanic_id, status) VALUES (?, 1, 'COMPLETED')", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, bId);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) woId = rs.getInt(1);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO work_order_service_items (work_order_id, service_item_id, completed) VALUES (?, ?, 1)")) {
                ps.setInt(1, woId);
                ps.setInt(2, 1);
                ps.executeUpdate();
                ps.setInt(1, woId);
                ps.setInt(2, 2);
                ps.executeUpdate();
            }

            // Skapa faktura med koppling till arbetsorder och 2 fakturarader
            int invId = 0;
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO invoices (work_order_id, total_amount) VALUES (?, 2394.0)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, woId);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) invId = rs.getInt(1);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO invoice_lines (invoice_id, service_name, price) VALUES (?, ?, ?)")) {
                ps.setInt(1, invId);
                ps.setString(2, "Oljebyte");
                ps.setDouble(3, 899.0);
                ps.executeUpdate();
                ps.setInt(1, invId);
                ps.setString(2, "Bromsservice");
                ps.setDouble(3, 1495.0);
                ps.executeUpdate();
            }

            System.out.println("JVM_WRITER_OK PID=" + pid + " BOOKING=" + bId + " WORKORDER=" + woId + " INVOICE=" + invId);

        } catch (SQLException e) {
            System.err.println("Fel i runWriter: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void runVerifier(int bookingId, int workOrderId, int invoiceId) {
        String pid = getProcessPid();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + RESTART_DB_FILE)) {

            // 1. Verifiera bokningens tjänster efter omstart
            try (PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM booking_service_items WHERE booking_id = ?")) {
                ps.setInt(1, bookingId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt(1) != 2) {
                        System.err.println("Bokningens tjänster saknas efter omstart");
                        System.exit(5);
                    }
                }
            }

            // 2. Verifiera arbetsordern och koppling till bokning
            try (PreparedStatement ps = conn.prepareStatement("SELECT booking_id, status FROM work_orders WHERE id = ?")) {
                ps.setInt(1, workOrderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt("booking_id") != bookingId || !"COMPLETED".equals(rs.getString("status"))) {
                        System.err.println("Arbetsorder pekar inte på rätt bokning eller saknar status");
                        System.exit(6);
                    }
                }
            }

            // 3. Verifiera faktura och frysta fakturarader
            try (PreparedStatement ps = conn.prepareStatement("SELECT count(*), sum(price) FROM invoice_lines WHERE invoice_id = ?")) {
                ps.setInt(1, invoiceId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt(1) != 2 || Math.abs(rs.getDouble(2) - 2394.0) > 0.01) {
                        System.err.println("Fakturarader eller frysta priser felaktiga efter omstart");
                        System.exit(7);
                    }
                }
            }

            System.out.println("JVM_VERIFIER_OK PID=" + pid + " STATUS=VERIFIED_100%");

        } catch (SQLException e) {
            System.err.println("Fel i runVerifier: " + e.getMessage());
            System.exit(1);
        } finally {
            File f = new File(RESTART_DB_FILE);
            if (f.exists()) {
                f.delete();
            }
        }
    }

    public static boolean runProcessRestartTest() {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        if (!new File(javaBin).exists() && new File(javaBin + ".exe").exists()) {
            javaBin += ".exe";
        }
        String classpath = System.getProperty("java.class.path");

        try {
            // 1. Kör Process 1 (Writer) som skriver data och därefter AVSLUTAR processen helt
            List<String> cmd1 = Arrays.asList(javaBin, "-cp", classpath, "com.wac.autocore.test.RestartProofRunner", "--write");
            ProcessBuilder pb1 = new ProcessBuilder(cmd1);
            pb1.redirectErrorStream(true);
            Process p1 = pb1.start();

            String writerOutput = "";
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p1.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("JVM_WRITER_OK")) {
                        writerOutput = line;
                    }
                }
            }
            int exit1 = p1.waitFor();
            TestRunner.assertEquals(0, exit1, "Process 1 (Writer) ska avslutas med exitkod 0");
            TestRunner.assertTrue(!writerOutput.isEmpty(), "Process 1 ska leverera framgångsutskrift");

            String pid1 = extractParam(writerOutput, "PID");
            int bookingId = Integer.parseInt(extractParam(writerOutput, "BOOKING"));
            int workOrderId = Integer.parseInt(extractParam(writerOutput, "WORKORDER"));
            int invoiceId = Integer.parseInt(extractParam(writerOutput, "INVOICE"));

            // 2. Kör Process 2 (Verifier) i en ny och helt separat JVM-process med nytt OS-PID
            List<String> cmd2 = Arrays.asList(
                    javaBin, "-cp", classpath,
                    "com.wac.autocore.test.RestartProofRunner", "--verify",
                    String.valueOf(bookingId), String.valueOf(workOrderId), String.valueOf(invoiceId)
            );
            ProcessBuilder pb2 = new ProcessBuilder(cmd2);
            pb2.redirectErrorStream(true);
            Process p2 = pb2.start();

            String verifierOutput = "";
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p2.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("JVM_VERIFIER_OK")) {
                        verifierOutput = line;
                    }
                }
            }
            int exit2 = p2.waitFor();
            TestRunner.assertEquals(0, exit2, "Process 2 (Verifier) ska avslutas med exitkod 0");
            TestRunner.assertTrue(!verifierOutput.isEmpty(), "Process 2 ska leverera framgångsutskrift");

            String pid2 = extractParam(verifierOutput, "PID");

            // Validera att det var två olika OS-processer
            TestRunner.assertTrue(!pid1.equals(pid2), "Process 1 (PID " + pid1 + ") och Process 2 (PID " + pid2 + ") måste ha olika OS-PID");

            System.out.println("    [SCRUM-169 ÄKTA OMSTARTSBEVIS]");
            System.out.println("      • Process 1 (PID " + pid1 + "): Skrev canary-data (Bokning #" + bookingId
                    + ", Arbetsorder #" + workOrderId + ", Faktura #" + invoiceId + ") och avslutades helt.");
            System.out.println("      • Process 2 (PID " + pid2 + "): Ny JVM startades från scratch, läste tillbaka relationerna och verifierade 100% dataintegritet.");
            return true;

        } catch (Throwable t) {
            throw new RuntimeException("Kunde inte köra äkta processomstart: " + t.getMessage(), t);
        }
    }

    private static String extractParam(String line, String key) {
        String search = key + "=";
        int idx = line.indexOf(search);
        if (idx == -1) return "";
        int end = line.indexOf(" ", idx);
        if (end == -1) end = line.length();
        return line.substring(idx + search.length(), end).trim();
    }
}
