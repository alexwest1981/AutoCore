package com.wac.autocore.test;

import com.wac.autocore.ui.MechanicDialogs;
import com.wac.autocore.ui.i18n.I18n;

import java.util.List;

/**
 * Prov för rullistan med specialiseringar när en mekaniker läggs till: den innehöll tre svenska
 * texter som låg fast i koden, så på engelska visades en blandning av svenska och engelska alternativ.
 */
public class MechanicSpecializationTest {

    public void testTheSuggestionsFollowTheActiveLanguage() {
        String original = I18n.getLanguage();
        try {
            // Utan garage läses bara de fasta förslagen, så provet rör inte databasens tjänster.
            I18n.setLanguage("en");
            List<String> english = MechanicDialogs.suggestSpecializations(null);
            TestRunner.assertFalse(containsSwedishLetters(english),
                    "Ingen text i förslagen får vara svensk när gränssnittet kör på engelska, men listan var: " + english);
            TestRunner.assertTrue(english.contains("Brakes") && english.contains("Tyres & Wheels"),
                    "De fasta förslagen ska komma från språkfilen, men listan var: " + english);

            I18n.setLanguage("sv");
            List<String> swedish = MechanicDialogs.suggestSpecializations(null);
            TestRunner.assertTrue(containsSwedishLetters(swedish),
                    "På svenska ska förslagen vara svenska, men listan var: " + swedish);

            TestRunner.assertEquals(english.size(), swedish.size(),
                    "Samma förslag ska erbjudas på båda språken");
            TestRunner.assertFalse(english.equals(swedish),
                    "Listan ska byta språk när gränssnittet gör det");
        } finally {
            I18n.setLanguage(original);
        }
    }

    /**
     * Valet sparas som nyckel i demodatans ordlista, inte som texten användaren såg. Sparas texten
     * står den kvar i det språk den skrevs i och visas oöversatt efter ett språkbyte.
     */
    public void testTheChosenSpecializationIsStoredAsAKeyNotAsText() {
        String original = I18n.getLanguage();
        try {
            I18n.setLanguage("en");
            TestRunner.assertEquals("seed.mechanic.brakes.specialization",
                    MechanicDialogs.specializationToStore("Brakes"),
                    "Ett fast val ska sparas som nyckel, inte som engelsk text");

            I18n.setLanguage("sv");
            TestRunner.assertEquals("seed.mechanic.brakes.specialization",
                    MechanicDialogs.specializationToStore("Bromsar"),
                    "Samma val på svenska ska bli samma nyckel");
            TestRunner.assertEquals("seed.mechanic.brakes.specialization",
                    MechanicDialogs.specializationToStore("Brakes"),
                    "En text som skrevs på engelska ska kännas igen även när gränssnittet är svenskt");
            TestRunner.assertEquals("seed.mechanic.general_service.specialization",
                    MechanicDialogs.specializationToStore("   "),
                    "Tomt val ska bli den allmänna servicen, inte en text i aktivt språk");
            TestRunner.assertEquals("Elsystem",
                    MechanicDialogs.specializationToStore("Elsystem"),
                    "Fritext som inte är ett av våra val ska sparas precis som den skrevs");
        } finally {
            I18n.setLanguage(original);
        }
    }

    private static boolean containsSwedishLetters(List<String> texts) {
        for (String text : texts) {
            if (text != null && text.matches(".*[åäöÅÄÖ].*")) {
                return true;
            }
        }
        return false;
    }
}
