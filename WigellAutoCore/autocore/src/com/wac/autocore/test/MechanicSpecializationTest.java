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

    private static boolean containsSwedishLetters(List<String> texts) {
        for (String text : texts) {
            if (text != null && text.matches(".*[åäöÅÄÖ].*")) {
                return true;
            }
        }
        return false;
    }
}
