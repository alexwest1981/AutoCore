package com.wac.autocore.test;

import com.wac.autocore.data.Db;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.service.GarageSystem;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * L3: a database created before the changes has to start the program and read as before.
 * Section 6: nothing that works today is allowed to stop working.
 *
 * The old database is a file from before the migrations were written, and it is read-only on
 * disk. Every run copies it to a temporary file first, so the proof can be run as often as
 * anyone likes and nothing outside /tmp is touched. The third run copies nothing, because the
 * program has to be able to make a database as well as lift an old one.
 *
 * What is compared is every row of every table, with the money written in kronor on both sides,
 * because the unit is what changed: the old database kept kronor, the new one keeps öre. The
 * same amount has to come out of both.
 */
public final class OldDatabaseProof {

    private static final String OLD_DATABASE =
            "WigellAutoCore/autocore/test/data/autocore-original.db";

    /** The money columns, so the same amount can be recognised in both units. */
    private static final String[][] MONEY = {
        {"service_items", "price"},
        {"work_order_service_items", "price"},
        {"invoices", "amount"},
        {"invoices", "discount"},
        {"invoices", "total_amount"},
        {"invoice_lines", "price"},
        {"invoice_lines", "discount"},
        {"payments", "amount"}
    };

    /**
     * The columns a database from between the two eras has, but the register does not know about.
     * The proof keeps its own list: this is the shape of that era, not a copy of what the code
     * adds. If the code's list loses a column, the first run is the one that fails.
     */
    private static final String[][] FROM_BETWEEN = {
        {"work_orders", "type", "TEXT"},
        {"work_orders", "original_work_order_id", "INTEGER"},
        {"work_orders", "planned_date", "TEXT"},
        {"work_orders", "planned_time", "TEXT"},
        {"work_orders", "customer_instructions", "TEXT"},
        {"work_orders", "other_comments", "TEXT"},
        {"booking_service_items", "package_name", "TEXT"},
        {"invoice_lines", "package_name", "TEXT"},
        {"work_order_service_items", "package_name", "TEXT"}
    };

    /** The tables the baseline creates, so a database made from nothing can be checked whole. */
    private static final String[] BASELINE_TABLES = {
        "customers", "vehicles", "mechanics", "service_items", "bookings", "work_orders",
        "booking_service_items", "service_packages", "service_package_items",
        "booking_mechanics", "work_order_service_items", "invoices", "invoice_lines",
        "payments", "settings"
    };

    private OldDatabaseProof() {}

    public static void main(String[] args) throws Exception {
        File old = new File(args.length > 0 ? args[0] : OLD_DATABASE);
        if (!old.isFile()) {
            System.out.println("Hittar ingen gammal databas: " + old.getAbsolutePath());
            System.exit(1);
        }

        System.out.println("L3 · en gammal databas möter de nya reglerna");
        System.out.println("gammal: " + old.getAbsolutePath() + " (rörs aldrig)");
        System.out.println();

        boolean withoutColumns = run(old, "1. kolumnerna saknas, som i en bas från en tidigare sprint", false);
        System.out.println();
        boolean withColumns = run(old, "2. kolumnerna finns, men registret gör det inte", true);
        System.out.println();
        boolean fromNothing = runFromNothing("3. ingen bas alls, den byggs från grunden");

        System.out.println("läses som förut, utan kolumnerna: " + withoutColumns);
        System.out.println("läses som förut, med kolumnerna:  " + withColumns);
        System.out.println("byggs från grunden:               " + fromNothing);
        System.exit(withoutColumns && withColumns && fromNothing ? 0 : 1);
    }

    /** One run against a copy of the old database, with or without the columns already there. */
    private static boolean run(File old, String label, boolean addColumns) throws Exception {
        File working = File.createTempFile("old-database-", ".db");
        working.deleteOnExit();
        Files.copy(old.toPath(), working.toPath(), StandardCopyOption.REPLACE_EXISTING);
        // The old file is read-only on disk, and a copy keeps that. The copy has to be writable:
        // it is the one the migrations run against.
        working.setWritable(true);

        System.out.println(label);
        if (addColumns) {
            addColumns(working);
        }

        Map<String, Map<String, Map<String, String>>> before = read(working, false);
        System.out.println("   före:  " + counts(before));

        System.out.println("   programmet startar mot kopian");
        new GarageSystem(new SqliteRepositoryFactory(new Db(working.getAbsolutePath())));

        Map<String, Map<String, Map<String, String>>> after = read(working, true);
        System.out.println("   efter: " + counts(after));
        money(old, working);

        List<String> lost = new ArrayList<String>();
        List<String> changed = new ArrayList<String>();
        Map<String, Integer> filled = new LinkedHashMap<String, Integer>();
        List<String> newColumns = new ArrayList<String>();
        List<String> addedRows = new ArrayList<String>();

        for (Map.Entry<String, Map<String, Map<String, String>>> table : before.entrySet()) {
            Map<String, Map<String, String>> now = after.get(table.getKey());
            if (now == null) {
                lost.add(table.getKey() + " (hela tabellen)");
                continue;
            }
            List<String> columnsBefore = table.getValue().isEmpty()
                    ? new ArrayList<String>()
                    : new ArrayList<String>(table.getValue().values().iterator().next().keySet());
            for (Map<String, String> row : now.values()) {
                for (String column : row.keySet()) {
                    String named = table.getKey() + "." + column;
                    if (!columnsBefore.contains(column) && !newColumns.contains(named)) {
                        newColumns.add(named);
                    }
                }
            }
            for (Map.Entry<String, Map<String, String>> row : table.getValue().entrySet()) {
                Map<String, String> current = now.get(row.getKey());
                if (current == null) {
                    lost.add(table.getKey() + " " + row.getKey());
                    continue;
                }
                for (Map.Entry<String, String> cell : row.getValue().entrySet()) {
                    String was = cell.getValue();
                    String is = current.get(cell.getKey());
                    boolean empty = was == null
                            || isEmptyMarker(table.getKey(), cell.getKey(), was);
                    if (empty && is != null) {
                        String column = table.getKey() + "." + cell.getKey();
                        Integer count = filled.get(column);
                        filled.put(column, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
                    } else if (was != null && !was.equals(is)) {
                        changed.add(table.getKey() + " " + row.getKey() + " " + cell.getKey()
                                + ": " + was + " -> " + is);
                    }
                }
            }
            for (String key : now.keySet()) {
                if (!table.getValue().containsKey(key)) {
                    addedRows.add(table.getKey() + " " + key);
                }
            }
        }

        boolean intact = lost.isEmpty() && changed.isEmpty();
        System.out.println("   rader som tappades:    " + (lost.isEmpty() ? "inga" : lost.toString()));
        System.out.println("   värden som ändrades:   " + (changed.isEmpty() ? "inga" : changed.toString()));
        System.out.println("   kolumner som fylldes:  " + (filled.isEmpty() ? "inga" : filled.toString()));
        System.out.println("   kolumner som tillkom:  " + (newColumns.isEmpty() ? "inga" : newColumns.toString()));
        System.out.println("   rader som tillkom:     " + (addedRows.isEmpty() ? "inga" : addedRows.toString()));
        System.out.println("   foreign_key_check:     " + foreignKeys(working));
        System.out.println("   schema_version:        " + schemaVersions(working));
        return intact;
    }

    /**
     * A database that is not there at all. The two fixtures above start from tables that already
     * exist; this one has nothing to lean on, so the baseline alone has to leave a schema the
     * rest of the program can use. A fresh checkout has to start as well, not only one that has
     * run before.
     */
    private static boolean runFromNothing(String label) throws Exception {
        File fresh = File.createTempFile("new-database-", ".db");
        fresh.deleteOnExit();
        // The file the copy would have gone into, and then not: here the program makes it itself.
        fresh.delete();

        System.out.println(label);
        System.out.println("   filen finns från början: " + fresh.exists());

        System.out.println("   programmet startar och bygger den");
        new GarageSystem(new SqliteRepositoryFactory(new Db(fresh.getAbsolutePath())));

        Map<String, Map<String, Map<String, String>>> after = read(fresh, true);
        System.out.println("   efter: " + counts(after));
        System.out.println("   schema_version:     " + schemaVersions(fresh));
        System.out.println("   foreign_key_check:  " + foreignKeys(fresh));

        List<String> tables = new ArrayList<String>();
        for (String table : BASELINE_TABLES) {
            if (!after.containsKey(table)) {
                tables.add(table);
            }
        }
        // The columns the tolerance step knows about have to stand in the baseline as well.
        // Otherwise a database made from nothing and one that was patched end up different.
        List<String> columns = new ArrayList<String>();
        for (String[] column : FROM_BETWEEN) {
            if (!hasColumn(fresh, column[0], column[1])) {
                columns.add(column[0] + "." + column[1]);
            }
        }
        Map<String, Map<String, String>> customers = after.get("customers");
        boolean seeded = customers != null && !customers.isEmpty();

        System.out.println("   tabeller som saknas: " + (tables.isEmpty() ? "inga" : tables.toString()));
        System.out.println("   kolumner som saknas: " + (columns.isEmpty() ? "inga" : columns.toString()));
        System.out.println("   fröet kommer in:     " + seeded);
        return tables.isEmpty() && columns.isEmpty() && seeded;
    }

    private static boolean hasColumn(File database, String table, String column) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rows.next()) {
                if (column.equals(rows.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The old app wrote 0 where it meant "no vehicle". There is no row with that id, so the value
     * was never a reference, and the baseline fills it from the booking. It counts as empty,
     * because that is what it was — and it is still reported as a column that got filled.
     */
    private static boolean isEmptyMarker(String table, String column, String value) {
        return "work_orders".equals(table) && "vehicle_id".equals(column) && "0".equals(value);
    }

    /** The columns of a database from between the two eras: there, but not in the register yet. */
    private static void addColumns(File database) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
             Statement statement = connection.createStatement()) {
            for (String[] column : FROM_BETWEEN) {
                statement.executeUpdate("ALTER TABLE " + column[0] + " ADD COLUMN " + column[1]
                        + " " + column[2]);
            }
            statement.executeUpdate("UPDATE work_orders SET type = 'STANDARD' WHERE type IS NULL");
        }
        System.out.println("   kolumnerna ditlagda i förväg, som en bas från mellanläget");
    }

    /**
     * Every table's rows, keyed by their primary key, with the money written in kronor so the two
     * units can be compared. A cell that is empty stays empty here; what the old database held is
     * what this side says it held.
     */
    private static Map<String, Map<String, Map<String, String>>> read(File database, boolean ore)
            throws Exception {
        Map<String, Map<String, Map<String, String>>> out =
                new LinkedHashMap<String, Map<String, Map<String, String>>>();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
             Statement statement = connection.createStatement()) {
            List<String> tables = new ArrayList<String>();
            try (ResultSet result = statement.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")) {
                while (result.next()) {
                    tables.add(result.getString(1));
                }
            }

            for (String table : tables) {
                if ("sqlite_sequence".equals(table)) {
                    continue;
                }
                List<String> key = primaryKey(statement, table);
                Map<String, Map<String, String>> rows =
                        new LinkedHashMap<String, Map<String, String>>();
                try (ResultSet result = statement.executeQuery("SELECT * FROM " + table)) {
                    ResultSetMetaData meta = result.getMetaData();
                    while (result.next()) {
                        Map<String, String> row = new LinkedHashMap<String, String>();
                        StringBuilder rowKey = new StringBuilder();
                        for (int i = 1; i <= meta.getColumnCount(); i++) {
                            String column = meta.getColumnName(i);
                            String value = result.getString(i);
                            if (value != null && isMoney(table, column)) {
                                value = kronor(value, ore);
                            }
                            row.put(column, value);
                            if (key.contains(column)) {
                                rowKey.append(value).append('|');
                            }
                        }
                        rows.put(rowKey.length() == 0 ? "rad " + result.getRow() : rowKey.toString(), row);
                    }
                }
                out.put(table, rows);
            }
        }
        return out;
    }

    /** The columns the row can be recognised by, in the order the table declares them. */
    private static List<String> primaryKey(Statement statement, String table) throws Exception {
        List<String> key = new ArrayList<String>();
        try (ResultSet rows = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rows.next()) {
                if (rows.getInt("pk") > 0) {
                    key.add(rows.getString("name"));
                }
            }
        }
        return key;
    }

    /** The same amount in kronor: the old database kept kronor, the new one keeps öre. */
    private static String kronor(String value, boolean ore) {
        if (!ore) {
            return String.format(Locale.ROOT, "%.2f", Double.parseDouble(value));
        }
        int amount = Integer.parseInt(value);
        return String.format(Locale.ROOT, "%d.%02d", Integer.valueOf(amount / 100),
                Integer.valueOf(Math.abs(amount % 100)));
    }

    private static boolean isMoney(String table, String column) {
        for (String[] pair : MONEY) {
            if (pair[0].equals(table) && pair[1].equals(column)) {
                return true;
            }
        }
        return false;
    }

    /** The amount on each side, so the unit change is seen instead of trusted. */
    private static void money(File old, File working) throws Exception {
        for (String[] pair : MONEY) {
            String sql = "SELECT rowid, " + pair[1] + " FROM " + pair[0]
                    + " WHERE " + pair[1] + " IS NOT NULL ORDER BY rowid";
            List<String> before = column(old, sql, false);
            List<String> after = column(working, sql, true);
            System.out.println("   " + pair[0] + "." + pair[1] + ": "
                    + (before.isEmpty() ? "inga rader" : before.toString())
                    + "  ->  " + (after.isEmpty() ? "inga rader" : after.toString()));
        }
    }

    private static List<String> column(File database, String sql, boolean ore) throws Exception {
        List<String> out = new ArrayList<String>();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                out.add("rad " + result.getInt(1) + " = " + kronor(result.getString(2), ore)
                        + (ore ? " kronor (" + result.getString(2) + " öre)" : " kronor"));
            }
        }
        return out;
    }

    private static String counts(Map<String, Map<String, Map<String, String>>> tables) {
        List<String> out = new ArrayList<String>();
        for (Map.Entry<String, Map<String, Map<String, String>>> entry : tables.entrySet()) {
            out.add(entry.getKey() + "=" + entry.getValue().size());
        }
        return String.join(" ", out);
    }

    private static String foreignKeys(File database) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA foreign_key_check")) {
            return result.next() ? "RADER PEKAR FEL" : "rent";
        }
    }

    private static String schemaVersions(File database) throws Exception {
        List<String> out = new ArrayList<String>();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT version, name FROM schema_version ORDER BY version")) {
            while (result.next()) {
                out.add(result.getInt(1) + " " + result.getString(2));
            }
        }
        return out.toString();
    }
}
