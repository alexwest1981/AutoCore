package com.wac.autocore.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * KVALITET: håller siffrorna i dokumenten i kapp med sviten.
 *
 * Dokumenten påstår hur många tester sviten har ("Alla 113 tester ..."). Den siffran skrivs för
 * hand, och den blev fel så fort någon lade till ett test: acceptanskraven stod på 88 medan sviten
 * körde 110, och ingen märkte det förrän någon råkade läsa raden. Provet nedan räknar testerna med
 * samma regler som körningen använder och jämför med vad dokumentet säger, så nästa gång faller
 * bygget i stället för att siffran glider ifrån.
 *
 * Ett dokument som ljuger om sin egen omfattning är sämre än ett utan siffror.
 */
public class DocumentationTest {

    private static final File ACCEPTANCE = new File("ACCEPTANSKRAV.md");
    private static final Pattern DOCUMENTED_COUNT = Pattern.compile("Alla\\s+(\\d+)\\s+tester");
    private static final String TEST_PACKAGE = "com.wac.autocore.test";

    /**
     * KVALITET: antalet tester som dokumenten påstår ska vara antalet tester som faktiskt körs.
     */
    public static void testTheDocumentedTestCountMatchesTheSuite() throws Exception {
        int actual = countTestsInPackage();
        TestRunner.assertTrue(actual > 0, "Inga tester hittades i " + TEST_PACKAGE);

        List<String> found = countsIn(read(ACCEPTANCE));
        TestRunner.assertTrue(!found.isEmpty(),
                ACCEPTANCE.getName() + " säger ingenstans hur många tester sviten har. Skriv "
                        + "\"Alla " + actual + " tester\" så kontrollen har något att jämföra med");

        List<String> wrong = new ArrayList<String>();
        for (String count : found) {
            if (!count.equals(String.valueOf(actual))) {
                wrong.add(count);
            }
        }
        TestRunner.assertTrue(wrong.isEmpty(),
                ACCEPTANCE.getName() + " påstår " + wrong + " tester, men sviten kör " + actual
                        + ". Uppdatera siffran i dokumentet, eller den här kontrollen om antalet avsiktligt ändrats");
    }

    /**
     * KVALITET: kontrollen ska kunna falla. En siffra som inte stämmer får inte gå igenom
     * jämförelsen, och en text utan siffra ska inte heller godtas som om den vore rätt.
     */
    public static void testTheCountCheckCanFail() {
        TestRunner.assertTrue(statedCountsAreCorrect("Alla 113 tester körs automatiskt.", 113),
                "En siffra som stämmer ska gå igenom");
        TestRunner.assertFalse(statedCountsAreCorrect("Alla 88 tester körs automatiskt.", 113),
                "En siffra som inte stämmer ska fällas");
        TestRunner.assertFalse(statedCountsAreCorrect("Sviten körs automatiskt.", 113),
                "En text utan siffra ska fällas, inte tigas igenom");
    }

    // ──────────────────────────────────────────────────────────────────────── räkningen

    /**
     * Räknar testerna i det kompilerade paketet med samma regler som köraren använder: en klassfil
     * som slutar på Test, och metoder som börjar på test och inte tar några argument.
     */
    private static int countTestsInPackage() throws Exception {
        File packageDir = new File(DocumentationTest.class.getResource("").toURI());
        File[] compiled = packageDir.listFiles();
        TestRunner.assertNotNull(compiled, "Kunde inte läsa det kompilerade testpaketet " + packageDir);

        int count = 0;
        for (File file : compiled) {
            String name = file.getName();
            if (!name.endsWith("Test.class") || name.contains("$")) {
                continue;
            }
            Class<?> testClass = Class.forName(TEST_PACKAGE + "." + name.substring(0, name.length() - 6));
            for (Method method : testClass.getDeclaredMethods()) {
                if (method.getName().startsWith("test") && method.getParameterTypes().length == 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private static List<String> countsIn(String text) {
        List<String> counts = new ArrayList<String>();
        Matcher matcher = DOCUMENTED_COUNT.matcher(text);
        while (matcher.find()) {
            counts.add(matcher.group(1));
        }
        return counts;
    }

    private static boolean statedCountsAreCorrect(String text, int actual) {
        List<String> counts = countsIn(text);
        if (counts.isEmpty()) {
            return false;
        }
        for (String count : counts) {
            if (!count.equals(String.valueOf(actual))) {
                return false;
            }
        }
        return true;
    }

    private static String read(File file) throws Exception {
        TestRunner.assertTrue(file.exists(), file + " saknas — kontrollen behöver den för att kunna jämföra");
        StringBuilder text = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append('\n');
            }
        } finally {
            reader.close();
        }
        return text.toString();
    }
}
