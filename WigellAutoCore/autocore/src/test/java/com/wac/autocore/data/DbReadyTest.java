package com.wac.autocore.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wac.autocore.exception.DatabaseInitException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * A database that could not be prepared must not report itself as ready.
 *
 * The flag is what made one failure look like eighteen: the first attempt threw, the instance
 * remembered that it had started anyway, and every call after it went on to query a database
 * without tables. The message that met the reader was "no such table: work_orders", which points
 * at the wrong thing entirely.
 */
class DbReadyTest {

    @TempDir
    Path folder;

    @Test
    void aStartThatFailedIsTriedAgainInsteadOfBeingRememberedAsDone() throws Exception {
        Path blocked = folder.resolve("blocked");

        // A file where the database needs a directory, so the first start cannot open it.
        Files.write(blocked, new byte[] {1});
        Db db = new Db(blocked.resolve("autocore.db").toString());

        DatabaseInitException first = assertThrows(DatabaseInitException.class, db::ensureReady);
        assertTrue(first.getMessage().contains("kunde inte förberedas"),
                "Första felet ska vara databasens eget, inte ett om en tabell som saknas");

        // The obstacle is removed. The second attempt has to do the work for real.
        Files.delete(blocked);
        Files.createDirectory(blocked);

        db.ensureReady();

        try (Connection connection = db.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SELECT count(*) FROM work_orders")) {
            assertTrue(rows.next());
            assertEquals(6, rows.getInt(1), "Den färska databasen ska ha demodata, alltså tabeller");
        }
    }
}
