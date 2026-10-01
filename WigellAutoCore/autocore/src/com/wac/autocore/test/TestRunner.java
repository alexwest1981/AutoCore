package com.wac.autocore.test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Lättviktig och fristående test-runner för automatiserade enhetstester.
 * Körs utan externa beroenden via JDK 8.
 */
public class TestRunner {

    private static int totalPassed = 0;
    private static int totalFailed = 0;
    private static final List<String> failures = new ArrayList<String>();

    /** Modulindelning. Klasser som inte nämns här körs som "unit". */
    private static final String[][] GROUP_OF = {
        {"smoke", "SmokeTest"},
        {"bevis", "EvidenceVerificationTest"},
        {"quality", "CodeQualityTest"},
        {"security", "SecurityAuditTest"},
        {"wcag", "WcagAccessibilityTest"},
    };
    private static final String DEFAULT_GROUP = "unit";

    public static void main(String[] args) {
        com.wac.autocore.data.Db.initTables();

        System.out.println("==================================================");
        System.out.println("    Wigell AutoCore - Automatiserade Enhetstester");
        System.out.println("==================================================");

        String wanted = (args.length == 0 || "all".equalsIgnoreCase(args[0]))
                ? null
                : args[0].toLowerCase();
        if ("evidence".equals(wanted)) {
            wanted = "bevis";
        }

        // Klasserna hittas i den kompilerade testkatalogen: en ny *Test.java körs
        // automatiskt utan att den här filen behöver ändras.
        for (Class<?> clazz : discoverTestClasses()) {
            String group = groupOf(clazz.getSimpleName());
            if (wanted == null || wanted.equals(group)) {
                runClass(clazz);
            }
        }

        System.out.println("--------------------------------------------------");
        System.out.printf("Resultat: %d tester körda. \u001B[32m%d godkända\u001B[0m, \u001B[31m%d misslyckade\u001B[0m.%n",
                totalPassed + totalFailed, totalPassed, totalFailed);

        if (!failures.isEmpty()) {
            System.out.println("\nMisslyckade tester:");
            for (String f : failures) {
                System.out.println("  ❌ " + f);
            }
            System.out.println("==================================================");
            System.exit(1);
        } else {
            System.out.println("\n\u001B[32m✔ ALLA TESTER GODKÄNDA!\u001B[0m");
            System.out.println("==================================================");
            System.exit(0);
        }
    }


    /**
     * Modul för en testklass. Nya klasser hamnar i "unit" utan att listan ändras.
     */
    private static String groupOf(String simpleName) {
        for (String[] entry : GROUP_OF) {
            if (entry[1].equals(simpleName)) {
                return entry[0];
            }
        }
        return DEFAULT_GROUP;
    }

    /**
     * Varje klass i testpaketet vars namn slutar på "Test".
     * ponytail: läser klassfilerna från katalogen på classpath; räcker för out/ och CI,
     * byt mot ett index om testerna någon gång paketeras i en jar.
     */
    private static List<Class<?>> discoverTestClasses() {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        try {
            File out = new File(TestRunner.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            File[] files = new File(out, "com/wac/autocore/test").listFiles();
            if (files == null) {
                System.err.println("Hittade ingen testkatalog under " + out);
                return classes;
            }
            for (File file : files) {
                String name = file.getName();
                if (!name.endsWith("Test.class") || name.contains("$")) {
                    continue;
                }
                try {
                    classes.add(Class.forName("com.wac.autocore.test."
                            + name.substring(0, name.length() - ".class".length())));
                } catch (Throwable ignored) {
                    // En klass som inte går att ladda rapporteras av runClass i stället.
                }
            }
        } catch (Exception e) {
            System.err.println("Kunde inte lista testklasserna: " + e);
        }
        Collections.sort(classes, new Comparator<Class<?>>() {
            public int compare(Class<?> a, Class<?> b) {
                return a.getSimpleName().compareTo(b.getSimpleName());
            }
        });
        return classes;
    }

    private static void runClass(Class<?> clazz) {
        System.out.println("\nKör: " + clazz.getSimpleName());
        Object instance;
        try {
            instance = clazz.newInstance();
        } catch (Exception e) {
            System.err.println("Kunde inte instansiera " + clazz.getName() + ": " + e);
            totalFailed++;
            return;
        }

        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().startsWith("test") && m.getParameterTypes().length == 0) {
                try {
                    m.invoke(instance);
                    totalPassed++;
                    System.out.println("  \u001B[32m✔ " + m.getName() + "\u001B[0m");
                } catch (Throwable t) {
                    totalFailed++;
                    Throwable cause = t.getCause() != null ? t.getCause() : t;
                    failures.add(clazz.getSimpleName() + "." + m.getName() + ": " + cause.getMessage());
                    System.out.println("  \u001B[31m❌ " + m.getName() + " -> " + cause.getMessage() + "\u001B[0m");
                }
            }
        }
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected != null && expected.equals(actual)) {
            return;
        }
        throw new AssertionError((message != null ? message + ": " : "") +
                "Förväntade [" + expected + "] men fick [" + actual + "]");
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message != null ? message : "Förväntade true men fick false");
        }
    }

    public static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message != null ? message : "Förväntade false men fick true");
        }
    }

    public static void assertNotNull(Object actual, String message) {
        if (actual == null) {
            throw new AssertionError(message != null ? message : "Förväntade icke-null men fick null");
        }
    }


}
