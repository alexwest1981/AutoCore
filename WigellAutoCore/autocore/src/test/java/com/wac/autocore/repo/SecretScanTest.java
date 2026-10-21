package com.wac.autocore.repo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The repository may not hold a secret. This reads every file git tracks and looks for the shapes a
 * secret takes when someone writes it down: a password, a key or a token assigned to something, and
 * a private key block.
 *
 * What it cannot do is know a value. A password sitting in a file under an innocent name passes,
 * which is why the assignment was checked by hand as well.
 */
class SecretScanTest {

    /** A string literal handed to something called password, secret, key or token. */
    private static final Pattern ASSIGNED_LITERAL = Pattern.compile(
            "(?i)\\b([a-z0-9_]*(?:password|passwd|pwd|secret|api[_-]?key|apikey|token|l[öo]senord)"
                    + "[a-z0-9_]*)\\s*=\\s*([\"'])(.{3,})\\2");

    /** The same, written the way a properties file writes it: no quotes, one value per line. */
    private static final Pattern ASSIGNED_PROPERTY = Pattern.compile(
            "(?i)^\\s*[a-z0-9_.-]*(password|passwd|secret|token|api[_-]?key)\\s*=\\s*"
                    + "(?!\\$\\{)(\\S.*)$");

    private static final Pattern PRIVATE_KEY =
            Pattern.compile("-----BEGIN [A-Z ]*PRIVATE KEY-----");

    /**
     * The test files below need a made-up password to build a temporary database, and that password
     * is the point of the test. They unlock nothing and are not deployed. Everything else is read.
     */
    private static final Set<String> ALLOWED_FIXTURES = Set.of(
            "WigellAutoCore/autocore/src/test/java/com/wac/autocore/security/PasswordHashTest.java",
            "WigellAutoCore/autocore/src/test/java/com/wac/autocore/service/LoginServiceTest.java",
            "WigellAutoCore/autocore/src/test/java/com/wac/autocore/service/PasswordRotationTest.java");

    @Test
    void noTrackedFileHoldsASecret() throws Exception {
        Path root = repositoryRoot();
        List<String> problems = new ArrayList<String>();

        for (String tracked : trackedFiles(root)) {
            if (ALLOWED_FIXTURES.contains(tracked) || !looksLikeText(tracked)) {
                continue;
            }
            problems.addAll(findings(root.resolve(tracked), tracked));
        }

        if (!problems.isEmpty()) {
            fail("Hemligheter i spårade filer:" + System.lineSeparator()
                    + String.join(System.lineSeparator(), problems));
        }
    }

    @Test
    void theScanWouldCatchAWrittenDownPassword() {
        // The rule itself is worth a test: a scan that never fires proves nothing.
        assertTrue(ASSIGNED_LITERAL.matcher("String password = \"hemligt123\";").find());
        assertTrue(ASSIGNED_PROPERTY.matcher("admin.password=hemligt123").find());

        // What must pass: a placeholder, and a variable that merely is called password.
        assertFalse(ASSIGNED_PROPERTY.matcher("admin.password=${AUTOCORE_ADMIN_PASSWORD:}").find());
        assertFalse(ASSIGNED_LITERAL.matcher("final PasswordField password = new PasswordField();").find());
    }

    private static List<String> findings(Path file, String name) throws IOException {
        List<String> problems = new ArrayList<String>();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        boolean properties = name.toLowerCase(java.util.Locale.ROOT).endsWith(".properties");
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (PRIVATE_KEY.matcher(line).find()) {
                problems.add(name + ":" + (i + 1) + " ser ut som en privat nyckel");
                continue;
            }
            Matcher literal = ASSIGNED_LITERAL.matcher(line);
            if (literal.find()) {
                // The name of the thing only. Printing the value would put it in the build log.
                problems.add(name + ":" + (i + 1) + " ger " + literal.group(1) + " ett skrivet värde");
                continue;
            }
            Matcher property = ASSIGNED_PROPERTY.matcher(line);
            if (properties && property.find()) {
                problems.add(name + ":" + (i + 1) + " ger " + property.group(1) + " ett skrivet värde");
            }
        }
        return problems;
    }

    private static boolean looksLikeText(String path) {
        String lower = path.toLowerCase(java.util.Locale.ROOT);
        for (String extension : new String[] {".java", ".properties", ".sql", ".json", ".css", ".md",
                ".txt", ".xml", ".yml", ".yaml", ".sh", ".toml", ".editorconfig", ".gitignore"}) {
            if (lower.endsWith(extension)) {
                return true;
            }
        }
        return false;
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

    private static List<String> trackedFiles(Path root) throws IOException, InterruptedException {
        Process git = new ProcessBuilder("git", "ls-files")
                .directory(root.toFile())
                .redirectErrorStream(true)
                .start();
        List<String> files = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(git.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while (line != null) {
                if (!line.trim().isEmpty()) {
                    files.add(line.trim());
                }
                line = reader.readLine();
            }
        }
        if (git.waitFor() != 0) {
            throw new IllegalStateException("git ls-files svarade med fel");
        }
        return files;
    }
}
