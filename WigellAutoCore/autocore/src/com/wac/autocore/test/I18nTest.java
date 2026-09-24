package com.wac.autocore.test;

import com.wac.autocore.ui.i18n.I18n;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Enhetstester för I18n-motorn och språkinläsning.
 */
public class I18nTest {

    public static void main(String[] args) {
        System.out.println("\nKör: I18nTest");
        testLanguageSwitching();
        testEnglishTranslations();
        testSwedishTranslations();
        testMissingKeyFallback();
        testParameterizedMessages();
        testTranslationParity();
    }

    public static void testLanguageSwitching() {
        I18n.setLanguage("en");
        TestRunner.assertEquals("Overview", I18n.get("nav.section.overview"), "testLanguageSwitching (en)");

        I18n.setLanguage("sv");
        TestRunner.assertEquals("Översikt", I18n.get("nav.section.overview"), "testLanguageSwitching (sv)");
    }

    public static void testEnglishTranslations() {
        I18n.setLanguage("en");
        TestRunner.assertEquals("Show customers", I18n.get("nav.item.customers"), "testEnglishTranslations - customers");
        TestRunner.assertEquals("Active work orders", I18n.get("overview.kpi.workorders"), "testEnglishTranslations - kpi");
        TestRunner.assertEquals("Booked", I18n.get("status.booked"), "testEnglishTranslations - status");
        TestRunner.assertEquals("New Customer", I18n.get("dialog.customer.create.title"), "testEnglishTranslations - dialog");
    }

    public static void testSwedishTranslations() {
        I18n.setLanguage("sv");
        TestRunner.assertEquals("Visa kunder", I18n.get("nav.item.customers"), "testSwedishTranslations - customers");
        TestRunner.assertEquals("Aktiva arbetsordrar", I18n.get("overview.kpi.workorders"), "testSwedishTranslations - kpi");
        TestRunner.assertEquals("Bokad", I18n.get("status.booked"), "testSwedishTranslations - status");
        TestRunner.assertEquals("Ny kund", I18n.get("dialog.customer.create.title"), "testSwedishTranslations - dialog");
    }

    public static void testMissingKeyFallback() {
        I18n.setLanguage("sv");
        String missing = "non.existent.key.test";
        TestRunner.assertEquals(missing, I18n.get(missing), "testMissingKeyFallback - returns key itself");
    }

    public static void testParameterizedMessages() {
        I18n.setLanguage("en");
        String formattedEn = I18n.get("search.results.subtitle", "Volvo");
        TestRunner.assertEquals("Results matching \"Volvo\"", formattedEn, "testParameterizedMessages (en)");

        I18n.setLanguage("sv");
        String formattedSv = I18n.get("search.results.subtitle", "Volvo");
        TestRunner.assertEquals("Träffar som matchar \"Volvo\"", formattedSv, "testParameterizedMessages (sv)");
    }

    public static void testUiLanguageChange() {
        I18n.setLanguage("en");
        try {
            com.wac.autocore.ui.navigation.SidebarView sidebar = new com.wac.autocore.ui.navigation.SidebarView(key -> {});
            TestRunner.assertNotNull(sidebar.getView(), "Sidebar-vy skall initieras");
        } catch (Throwable t) {
            System.out.println("  (Info: Hoppar över direkt instansiering av SidebarView: grafikmiljö saknas)");
        }
        I18n.setLanguage("sv");
        TestRunner.assertTrue(I18n.isSwedish(), "Aktivt språk skall vara svenska");
        TestRunner.assertEquals("Översikt", I18n.get("nav.section.overview"), "Nav översikt på svenska");
        TestRunner.assertEquals("Visa kunder", I18n.get("nav.item.customers"), "Nav kunder på svenska");

        I18n.setLanguage("en");
        TestRunner.assertTrue(!I18n.isSwedish(), "Aktivt språk skall vara engelska");
        TestRunner.assertEquals("Overview", I18n.get("nav.section.overview"), "Nav översikt på engelska");
    }

    /**
     * Verifierar att en.json och sv.json har exakt samma uppsättning nycklar.
     */
    public static void testTranslationParity() {
        Map<String, String> en = I18n.loadDictionary("en");
        Map<String, String> sv = I18n.loadDictionary("sv");

        TestRunner.assertTrue(!en.isEmpty(), "en.json får inte vara tom");
        TestRunner.assertTrue(!sv.isEmpty(), "sv.json får inte vara tom");

        Set<String> missingInSv = new TreeSet<>(en.keySet());
        missingInSv.removeAll(sv.keySet());

        Set<String> missingInEn = new TreeSet<>(sv.keySet());
        missingInEn.removeAll(en.keySet());

        TestRunner.assertTrue(missingInSv.isEmpty(), "Följande nycklar saknas i sv.json: " + missingInSv);
        TestRunner.assertTrue(missingInEn.isEmpty(), "Följande nycklar saknas i en.json: " + missingInEn);
        TestRunner.assertEquals(en.size(), sv.size(), "Båda språkfilerna skall ha exakt samma antal nycklar (" + en.size() + ")");
    }
}
