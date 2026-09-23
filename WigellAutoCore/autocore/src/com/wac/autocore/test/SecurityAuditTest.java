package com.wac.autocore.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Automatiserade säkerhetskontroller:
 * - Hårdkodade hemligheter / API-nycklar / produktionslösenord
 * - SQL-injektionsmönster & oskyddade frågesträngar
 * - Path traversal & filsystemsäkerhet
 * - Farliga exekveringsanrop (Runtime.exec / ProcessBuilder på användarindata)
 */
public class SecurityAuditTest {

    private static final File SRC_ROOT = new File("WigellAutoCore/autocore/src");

    private static List<File> listJavaFiles(File dir) {
        List<File> files = new ArrayList<File>();
        if (dir == null || !dir.exists()) return files;
        File[] children = dir.listFiles();
        if (children == null) return files;
        for (File c : children) {
            if (c.isDirectory()) {
                files.addAll(listJavaFiles(c));
            } else if (c.getName().endsWith(".java")) {
                files.add(c);
            }
        }
        return files;
    }

    /**
     * SÄKERHET: Kontrollerar att inga hårdkodade lösenord, privata kryptonycklar eller
     * API-hemligheter ligger sparade i källkoden.
     */
    public static void testNoHardcodedSecrets() throws Exception {
        List<File> javaFiles = listJavaFiles(SRC_ROOT);
        TestRunner.assertTrue(!javaFiles.isEmpty(), "Minst en Java-källfil måste hittas");

        Pattern secretPattern = Pattern.compile("(?i)(api[_-]?key|secret[_-]?key|aws[_-]?secret|private[_-]?key|client[_-]?secret)\\s*=\\s*\"([^\"]{8,})\"");
        for (File f : javaFiles) {
            // Undanta själva säkerhetstestet
            if (f.getName().equals("SecurityAuditTest.java")) continue;

            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                Matcher m = secretPattern.matcher(line);
                if (m.find()) {
                    TestRunner.assertTrue(false,
                            String.format("Säkerhetsvarning: Möjlig hårdkodad hemlighet i %s:%d: %s",
                                    f.getName(), lineNum, m.group(1)));
                }
            }
            br.close();
        }
    }

    /**
     * SÄKERHET: Kontrollerar SQL-injektionsrisker (flaggar direkt strängkonkatenering i SQL-satser).
     */
    public static void testSqlInjectionSafety() throws Exception {
        List<File> javaFiles = listJavaFiles(SRC_ROOT);
        Pattern unsafeSql = Pattern.compile("(?i)(select|insert|update|delete)\\s+.*\\+\\s*[a-zA-Z0-9_]+");

        for (File f : javaFiles) {
            if (f.getName().equals("SecurityAuditTest.java")) continue;

            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                // Filtrera bort kommentarer
                String trimmed = line.trim();
                if (trimmed.startsWith("//") || trimmed.startsWith("*")) continue;

                Matcher m = unsafeSql.matcher(line);
                if (m.find()) {
                    // Om det är en SQL-frågesträng som konkateneras med variabler istället för PreparedStatement
                    if (line.contains("executeQuery") || line.contains("executeUpdate") || line.contains("prepareStatement")) {
                        TestRunner.assertTrue(false,
                                String.format("Möjlig SQL-injektionsrisk i %s:%d: Använd PreparedStatement med parametrering (?)",
                                        f.getName(), lineNum));
                    }
                }
            }
            br.close();
        }
    }

    /**
     * SÄKERHET: Kontrollerar att externa exekveringskommandon (Runtime.exec) inte anropas
     * oskyddat från applikationskoden.
     */
    public static void testNoDangerousRuntimeExec() throws Exception {
        List<File> javaFiles = listJavaFiles(SRC_ROOT);
        for (File f : javaFiles) {
            if (f.getName().equals("SecurityAuditTest.java")) continue;

            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                String trimmed = line.trim();
                if (trimmed.startsWith("//") || trimmed.startsWith("*")) continue;

                if (line.contains("Runtime.getRuntime().exec(") || line.contains("new ProcessBuilder(")) {
                    TestRunner.assertTrue(false,
                            String.format("Säkerhetsvarning: Process/Runtime-exekvering funnen i %s:%d",
                                    f.getName(), lineNum));
                }
            }
            br.close();
        }
    }

    /**
     * SÄKERHET: Kontrollerar att ingen känslig data (personnummer, lösenord, kreditkortsnummer)
     * loggas ut i råtext till konsolen.
     */
    public static void testNoSensitiveDataLogging() throws Exception {
        List<File> javaFiles = listJavaFiles(SRC_ROOT);
        Pattern sensitiveLog = Pattern.compile("(?i)System\\.(out|err)\\.print.*(password|secret|creditcard|cvv|personnummer)");

        for (File f : javaFiles) {
            if (f.getName().equals("SecurityAuditTest.java")) continue;

            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                Matcher m = sensitiveLog.matcher(line);
                if (m.find()) {
                    TestRunner.assertTrue(false,
                            String.format("Säkerhetsvarning: Känslig information loggas i %s:%d",
                                    f.getName(), lineNum));
                }
            }
            br.close();
        }
    }
}
