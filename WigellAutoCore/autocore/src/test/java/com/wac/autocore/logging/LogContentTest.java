package com.wac.autocore.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.read.ListAppender;

import com.wac.autocore.data.Db;
import com.wac.autocore.exception.AccessDeniedException;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.security.Identity;
import com.wac.autocore.security.IdentityHolder;
import com.wac.autocore.security.Role;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.GuardedGarageSystem;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * E4-3 and E4-7: every line carries a trace-id, and the log holds no secrets or personal data.
 *
 * Both tests run everywhere, CI included, and neither needs a log file to exist beforehand. The
 * second one takes the pattern out of logback-spring.xml, writes real lines through the real guard
 * into a temporary folder, and reads them back. Removing the trace-id from the pattern therefore
 * turns this test red, which is the point: a proof that only runs where the app has already been
 * started proves nothing on someone else's machine.
 *
 * What the scan cannot catch is a name: there is no shape to look for, which is why the code logs
 * ids and keys, never the user's text.
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

    /**
     * The app's own pattern, from the file the app ships. Writing through it into a folder of our
     * own is what makes the proof runnable anywhere: no file has to exist beforehand.
     */
    @Test
    void theLogTheAppWritesCarriesATraceAndNoSecret() throws IOException {
        String pattern = configuredPattern();

        // The trace-id comes from the pattern, so the pattern is the first thing asked. Deleting
        // it from logback-spring.xml is how this test is meant to be broken on purpose.
        assertTrue(pattern.contains("%X{traceId"),
                "Loggmönstret i logback-spring.xml skriver inget trace-id");

        Path folder = Files.createTempDirectory("autocore-logg-");
        Path logFile = folder.resolve("autocore.log");
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger appLog = context.getLogger("com.wac.autocore");
        Level previousLevel = appLog.getLevel();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern(pattern);
        encoder.start();

        FileAppender<ILoggingEvent> file = new FileAppender<ILoggingEvent>();
        file.setContext(context);
        file.setFile(logFile.toString());
        file.setAppend(false);
        file.setEncoder(encoder);
        file.start();

        appLog.setLevel(Level.DEBUG);
        appLog.addAppender(file);
        try {
            File dbFile = File.createTempFile("logcontent-", ".db");
            dbFile.deleteOnExit();
            dbFile.delete();
            Db db = new Db(dbFile.getAbsolutePath());
            db.ensureReady();

            IdentityHolder identity = new IdentityHolder();
            identity.set(new Identity("kontroll", Role.READ_ONLY));
            GarageSystem garage = new GuardedGarageSystem(new SqliteRepositoryFactory(db), identity);

            assertThrows(AccessDeniedException.class,
                    () -> garage.createCustomer("Kalle Testsson", "070-9998877", "kalle@exempel.se"));
            assertThrows(AccessDeniedException.class,
                    () -> garage.createCustomer("Kalle Testsson", "070-9998877", "kalle@exempel.se"));
            dbFile.delete();
        } finally {
            appLog.detachAppender(file);
            appLog.setLevel(previousLevel);
            file.stop();
            encoder.stop();
        }

        List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
        assertFalse(lines.isEmpty(), "Provet skrev ingen loggrad alls");

        List<String> problems = new ArrayList<String>();
        int guardedLines = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!STARTS_A_RECORD.matcher(line).find()) {
                continue;
            }
            // The field is on every line the pattern writes, and a real id on every line an
            // operation writes. A start-up line outside an operation carries the placeholder.
            if (!line.contains("[trace=")) {
                problems.add("rad " + (i + 1) + " saknar trace-fältet");
            }
            if (line.contains("Åtkomst nekad")) {
                guardedLines++;
                if (!GUARDED_LINE.matcher(line).find()) {
                    problems.add("rad " + (i + 1) + " är en spärrad operation utan spårbart id");
                }
            }
            for (Map.Entry<String, Pattern> secret : SECRETS.entrySet()) {
                if (secret.getValue().matcher(line).find()) {
                    // What was found, never the value: printing it would put it in the build log.
                    problems.add("rad " + (i + 1) + " innehåller " + secret.getKey());
                }
            }
        }

        assertEquals(2, guardedLines, "De två nekade anropen ska ge två rader i filen");
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

    /** A line the guard wrote: a real trace-id and the operation that was refused. */
    private static final Pattern GUARDED_LINE = Pattern.compile("\\[trace=[0-9a-f]{8} op=[A-Z_]+\\]");

    /** The pattern the app itself logs with, taken from the configuration the app ships. */
    private static String configuredPattern() throws IOException {
        try (InputStream in = LogContentTest.class.getResourceAsStream("/logback-spring.xml")) {
            assertNotNull(in, "logback-spring.xml ska ligga i src/main/resources");
            String xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Matcher found = Pattern.compile("<property\\s+name=\"PATTERN\"\\s+value=\"([^\"]+)\"").matcher(xml);
            assertTrue(found.find(), "logback-spring.xml ska sätta en PATTERN-egenskap");
            return found.group(1);
        }
    }
}