package com.wac.autocore.test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Lättviktig och fristående test-runner för automatiserade enhetstester.
 * Körs utan externa beroenden via JDK 8.
 */
public class TestRunner {

    private static int totalPassed = 0;
    private static int totalFailed = 0;
    private static final List<String> failures = new ArrayList<String>();

    public static void main(String[] args) {
        com.wac.autocore.data.Db.initTables();

        System.out.println("==================================================");
        System.out.println("    Wigell AutoCore - Automatiserade Enhetstester");
        System.out.println("==================================================");

        boolean runAll = args.length == 0 || "all".equalsIgnoreCase(args[0]);
        boolean runUnit = runAll || "unit".equalsIgnoreCase(args[0]);
        boolean runQuality = runAll || "quality".equalsIgnoreCase(args[0]);
        boolean runSecurity = runAll || "security".equalsIgnoreCase(args[0]);
        boolean runWcag = runAll || "wcag".equalsIgnoreCase(args[0]);

        if (runUnit) {
            runClass(UiFormattersTest.class);
            runClass(EntityLookupTest.class);
            runClass(OverviewMetricsTest.class);
            runClass(TableFactoryTest.class);
            runClass(GlobalSearchTest.class);
            runClass(I18nTest.class);
            runClass(SeedTextTest.class);
            runClass(MechanicScheduleTest.class);
            runClass(PersistenceRestartTest.class);
        }
        if (runQuality) {
            runClass(CodeQualityTest.class);
        }
        if (runSecurity) {
            runClass(SecurityAuditTest.class);
        }
        if (runWcag) {
            runClass(WcagAccessibilityTest.class);
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
