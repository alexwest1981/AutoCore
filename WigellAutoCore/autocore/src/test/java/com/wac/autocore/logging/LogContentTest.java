package com.wac.autocore.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.wac.autocore.data.Db;
import com.wac.autocore.exception.AccessDeniedException;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.security.Identity;
import com.wac.autocore.security.IdentityHolder;
import com.wac.autocore.security.Role;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.GuardedGarageSystem;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * E4-3: every line carries a trace-id, and the log holds no secrets or personal data.
 *
 * The first test runs everywhere, CI included. The second reads the real log file and is
 * skipped when there is none. What the scan cannot catch is a name: there is no shape to
 * look for, which is why the code logs ids and keys, never the user's text.
 */
class LogContentTest {

    /** A log record starts with its timestamp; stack trace lines below it do not. */
    private static final Pattern STARTS_A_RECORD = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T");

    /** What must never be in the log, by what it is. */
    private static final Map<String, Pattern> SECRETS = new LinkedHashMap<String, Pattern>();

    static {
        SECRETS.put("lösenordshash", Pattern.compile("pbkdf2\\$"));
        SECRETS.put("lösenord eller token med värde",
                Pattern.compile("(?i)\\b(password|passwd|pwd|l[öo]senord|token|secret)\\s*[=:]\\s*\\S+"));
        SECRETS.put("bearer-token", Pattern.compile("(?i)\\bbearer\\s+\\S{10,}"));
        SECRETS.put("e-postadress", Pattern.compile("[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}"));
        SECRETS.put("telefonnummer", Pattern.compile("\\b0(\\d{1,3}-\\d{5,8}|\\d{9})\\b"));
        SECRETS.put("personnummer", Pattern.compile("\\b(19|20)?\\d{6}[-+]\\d{4}\\b"));
    }

    @Test
    void aGuardedOperationPutsTraceAndOperationOnTheLine() throws Exception {
        File file = File.createTempFile("logcontent-", ".db");
        file.deleteOnExit();
        file.delete();
        Db db = new Db(file.getAbsolutePath());
        db.ensureReady();

        IdentityHolder identity = new IdentityHolder();
        identity.set(new Identity("kontroll", Role.READ_ONLY));
        GarageSystem garage = new GuardedGarageSystem(new SqliteRepositoryFactory(db), identity);

        // Catch what the guard logs, in memory, instead of reading a file.
        Logger guardLog = (Logger) LoggerFactory.getLogger(GuardedGarageSystem.class);
        ListAppender<ILoggingEvent> captured = new ListAppender<ILoggingEvent>();
        captured.start();
        guardLog.addAppender(captured);

        try {
            assertThrows(AccessDeniedException.class,
                    () -> garage.createCustomer("Kalle Testsson", "070-9998877", "kalle@exempel.se"));
            assertThrows(AccessDeniedException.class,
                    () -> garage.createCustomer("Kalle Testsson", "070-9998877", "kalle@exempel.se"));
        } finally {
            guardLog.detachAppender(captured);
            file.delete();
        }

        assertEquals(2, captured.list.size(), "Två nekade försök ska ge två rader");
        ILoggingEvent first = captured.list.get(0);
        ILoggingEvent second = captured.list.get(1);

        // The operation and a trace-id are on the line without the code having to write them.
        assertTrue(first.getMDCPropertyMap().get("traceId").matches("[0-9a-f]{8}"));
        assertEquals("CREATE_CUSTOMER", first.getMDCPropertyMap().get("operation"));
        assertNotEquals(first.getMDCPropertyMap().get("traceId"), second.getMDCPropertyMap().get("traceId"),
                "Varje operation ska få ett eget trace-id");

        // A security event names the user, and nothing the user typed.
        String line = first.getFormattedMessage();
        assertTrue(line.contains("kontroll"));
        assertFalse(line.contains("Kalle"));
        assertFalse(line.contains("070-9998877"));
        assertFalse(line.contains("kalle@exempel.se"));
    }

    @Test
    void theLogFileHoldsNoSecretsAndEveryLineHasATrace() throws IOException {
        Path logFile = logDirectory().resolve("autocore.log");
        Assumptions.assumeTrue(Files.exists(logFile), "Ingen loggfil att läsa, kör appen först");

        List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
        List<String> problems = new ArrayList<String>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (STARTS_A_RECORD.matcher(line).find() && !line.contains("[trace=")) {
                problems.add("rad " + (i + 1) + " saknar trace-id");
            }
            for (Map.Entry<String, Pattern> secret : SECRETS.entrySet()) {
                if (secret.getValue().matcher(line).find()) {
                    // What was found, never the value: printing it would put it in the build log.
                    problems.add("rad " + (i + 1) + " innehåller " + secret.getKey());
                }
            }
        }

        if (!problems.isEmpty()) {
            fail("Loggfilen " + logFile + ":" + System.lineSeparator()
                    + String.join(System.lineSeparator(), problems));
        }
    }

    @Test
    void theScanWouldCatchASecret() {
        // A scan that never fires proves nothing.
        assertTrue(holdsSecret("inloggning med lösenord=hemligt123"));
        assertTrue(holdsSecret("hash pbkdf2$210000$abc"));
        assertTrue(holdsSecret("Authorization: Bearer abcdefghijklmnop"));
        assertTrue(holdsSecret("kund anna@exempel.se"));
        assertTrue(holdsSecret("ring 070-1234567"));
        assertTrue(holdsSecret("pnr 19900101-1234"));

        // A real line from the app must pass.
        assertFalse(holdsSecret("2026-10-21T17:27:49.488+02:00 WARN  [JavaFX Application Thread] "
                + "[trace=bfe7a327 op=CREATE_CUSTOMER] c.w.a.service.GuardedGarageSystem - "
                + "Åtkomst nekad: readonly (READ_ONLY) försökte CREATE_CUSTOMER"));
    }

    private static boolean holdsSecret(String line) {
        for (Pattern secret : SECRETS.values()) {
            if (secret.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }

    /** The same folder the app writes to: AUTOCORE_LOG_DIR, or logs/ at the repository root. */
    private static Path logDirectory() {
        String configured = System.getenv("AUTOCORE_LOG_DIR");
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured);
        }
        return repositoryRoot().resolve("logs");
    }

    /** Walks up from wherever the test runs until it finds the repository. */
    private static Path repositoryRoot() {
        Path here = Paths.get("").toAbsolutePath();
        while (here != null) {
            if (Files.isDirectory(here.resolve(".git"))) {
                return here;
            }
            here = here.getParent();
        }
        throw new IllegalStateException("Hittar inget git-repo ovanför " + Paths.get("").toAbsolutePath());
    }
}