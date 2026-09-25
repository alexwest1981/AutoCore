package com.wac.autocore.test;

import com.wac.autocore.seed.SeedText;
import com.wac.autocore.ui.i18n.I18n;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhetstester för demodatans ordlista (seed).
 *
 * Ordlistan är skild från gränssnittets språkfiler: den håller texterna för raderna som seedas in
 * i en tom databas, och databasen lagrar nyckeln så att samma rad kan läsas på båda språken.
 * Testerna vaktar pariteten mellan sv.json och en.json, att språkbytet följer med, att fritext
 * passerar orörd, och att varje nyckel som källkoden använder finns i båda filerna.
 */
public class SeedTextTest {

    private static final File SRC_ROOT = new File("WigellAutoCore/autocore/src/com/wac/autocore");
    private static final Pattern KEY_IN_SOURCE = Pattern.compile("\"(seed\\.[a-z0-9_.]+)\"");

    public static void main(String[] args) throws Exception {
        System.out.println("\nKör: SeedTextTest");
        testDictionariesAreSeparateFromInterfaceTexts();
        testDictionariesHaveSameKeys();
        testNoEmptyValues();
        testResolutionFollowsLanguage();
        testPlainTextPassesThrough();
        testEverySeedKeyInSourceExists();
    }

    /**
     * Demodatan får inte ligga i gränssnittets språkfiler, och gränssnittets nycklar får inte
     * ligga i demodatans filer.
     */
    public static void testDictionariesAreSeparateFromInterfaceTexts() {
        Map<String, String> seedEn = SeedText.loadDictionary(SeedText.LANG_EN);
        Map<String, String> seedSv = SeedText.loadDictionary(SeedText.LANG_SV);
        TestRunner.assertTrue(!seedEn.isEmpty(), "Engelska demodata-ordlistan är tom");
        TestRunner.assertTrue(!seedSv.isEmpty(), "Svenska demodata-ordlistan är tom");

        for (String key : I18n.loadDictionary(I18n.LANG_EN).keySet()) {
            TestRunner.assertFalse(SeedText.isKey(key),
                    "Gränssnittsnyckeln " + key + " ser ut som en demodata-nyckel");
            TestRunner.assertFalse(seedEn.containsKey(key),
                    "Gränssnittsnyckeln " + key + " ligger även i demodatans ordlista");
        }
    }

    public static void testDictionariesHaveSameKeys() {
        Set<String> en = new TreeSet<String>(SeedText.loadDictionary(SeedText.LANG_EN).keySet());
        Set<String> sv = new TreeSet<String>(SeedText.loadDictionary(SeedText.LANG_SV).keySet());
        TestRunner.assertEquals(en, sv, "sv.json och en.json har inte samma nycklar");
    }

    public static void testNoEmptyValues() {
        for (String lang : new String[]{SeedText.LANG_SV, SeedText.LANG_EN}) {
            for (Map.Entry<String, String> e : SeedText.loadDictionary(lang).entrySet()) {
                TestRunner.assertNotNull(e.getValue(), "Tomt värde för " + e.getKey() + " (" + lang + ")");
                TestRunner.assertTrue(!e.getValue().trim().isEmpty(),
                        "Tomt värde för " + e.getKey() + " (" + lang + ")");
            }
        }
    }

    /**
     * Språket för demodatan sätts av språkbytet i gränssnittet, så en rad i databasen följer med
     * utan att skrivas om.
     */
    public static void testResolutionFollowsLanguage() {
        I18n.setLanguage(I18n.LANG_SV);
        TestRunner.assertEquals("Oljebyte", SeedText.resolve("seed.service.oil_change.name"),
                "Svensk tjänstetext");
        TestRunner.assertEquals("Bromsar", SeedText.resolve("seed.mechanic.brakes.specialization"),
                "Svensk specialisering");
        TestRunner.assertEquals("Inspektion av främre bromsar",
                SeedText.resolve("seed.booking.front_brake_inspection.description"),
                "Svensk bokningsbeskrivning");

        I18n.setLanguage(I18n.LANG_EN);
        TestRunner.assertEquals("Oil change", SeedText.resolve("seed.service.oil_change.name"),
                "Engelsk tjänstetext");
        TestRunner.assertEquals("Brakes", SeedText.resolve("seed.mechanic.brakes.specialization"),
                "Engelsk specialisering");
        TestRunner.assertEquals("Front brake inspection",
                SeedText.resolve("seed.booking.front_brake_inspection.description"),
                "Engelsk bokningsbeskrivning");
    }

    /** Text som användaren själv har skrivit är inte en nyckel och ska visas precis som den är. */
    public static void testPlainTextPassesThrough() {
        TestRunner.assertEquals("Ny bromsservice", SeedText.resolve("Ny bromsservice"),
                "Fritext ska passera orörd");
        TestRunner.assertEquals("", SeedText.resolve(""), "Tom sträng ska passera orörd");
        TestRunner.assertEquals(null, SeedText.resolve(null), "null ska passera orörd");
        TestRunner.assertFalse(SeedText.isKey("Ny bromsservice"), "Fritext ska inte räknas som nyckel");
    }

    /**
     * Varje nyckel som seederna skriver in i databasen måste finnas i båda ordlistorna, annars
     * skulle raden visas som en nyckel i gränssnittet.
     */
    public static void testEverySeedKeyInSourceExists() throws Exception {
        Set<String> used = new TreeSet<String>();
        for (File f : new File[]{new File(SRC_ROOT, "data/SeedData.java"),
                new File(SRC_ROOT, "service/MechanicSchedule.java")}) {
            TestRunner.assertTrue(f.exists(), "Hittade inte " + f.getPath());
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = KEY_IN_SOURCE.matcher(line);
                while (m.find()) {
                    used.add(m.group(1));
                }
            }
            br.close();
        }

        TestRunner.assertTrue(used.size() >= 20,
                "Hittade bara " + used.size() + " demodata-nycklar i seederna");

        Map<String, String> en = SeedText.loadDictionary(SeedText.LANG_EN);
        Map<String, String> sv = SeedText.loadDictionary(SeedText.LANG_SV);
        for (String key : used) {
            TestRunner.assertTrue(en.containsKey(key), "Saknas i en.json: " + key);
            TestRunner.assertTrue(sv.containsKey(key), "Saknas i sv.json: " + key);
        }
    }
}
