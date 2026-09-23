package com.wac.autocore.test;

import com.wac.autocore.theme.ThemeCatalog;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Automatiserade tillgänglighetskontroller (WCAG 2.1 AA) för gränssnitt,
 * färgkontraster, teckenstorlekar och tangentbordsnavigering.
 */
public class WcagAccessibilityTest {

    // WCAG 2.1 AA kontrastkrav:
    // Normal text (< 18pt eller < 14pt fet): minst 4.5:1
    // Stor text (>= 18pt eller >= 14pt fet) och grafiska UI-komponenter: minst 3.0:1
    private static final double MIN_CONTRAST_NORMAL_TEXT = 4.5;
    private static final double MIN_CONTRAST_UI_COMPONENT = 3.0;

    /**
     * Beräknar relativ luminans enligt WCAG 2.1 formel:
     * L = 0.2126 * R + 0.7152 * G + 0.0722 * B
     */
    public static double relativeLuminance(String hex) {
        if (hex == null) return 0.0;
        String clean = hex.trim();
        if (clean.startsWith("#")) {
            clean = clean.substring(1);
        }
        if (clean.length() == 3) {
            clean = "" + clean.charAt(0) + clean.charAt(0)
                    + clean.charAt(1) + clean.charAt(1)
                    + clean.charAt(2) + clean.charAt(2);
        }
        if (clean.length() < 6) return 0.0;
        try {
            int r = Integer.parseInt(clean.substring(0, 2), 16);
            int g = Integer.parseInt(clean.substring(2, 4), 16);
            int b = Integer.parseInt(clean.substring(4, 6), 16);

            double rs = r / 255.0;
            double gs = g / 255.0;
            double bs = b / 255.0;

            double rLin = rs <= 0.03928 ? rs / 12.92 : Math.pow((rs + 0.055) / 1.055, 2.4);
            double gLin = gs <= 0.03928 ? gs / 12.92 : Math.pow((gs + 0.055) / 1.055, 2.4);
            double bLin = bs <= 0.03928 ? bs / 12.92 : Math.pow((bs + 0.055) / 1.055, 2.4);

            return 0.2126 * rLin + 0.7152 * gLin + 0.0722 * bLin;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Beräknar kontrastförhållandet mellan två färger (L1 + 0.05) / (L2 + 0.05).
     */
    public static double contrastRatio(String hex1, String hex2) {
        double l1 = relativeLuminance(hex1);
        double l2 = relativeLuminance(hex2);
        double lighter = Math.max(l1, l2);
        double darker = Math.min(l1, l2);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static Map<String, String> parsePalette(String stylesheetPath) {
        Map<String, String> palette = new HashMap<String, String>();
        InputStream in = WcagAccessibilityTest.class.getResourceAsStream(stylesheetPath);
        if (in == null) {
            File f = new File("WigellAutoCore/autocore/src/resources" + stylesheetPath);
            if (!f.exists()) {
                f = new File("WigellAutoCore/autocore/src" + stylesheetPath);
            }
            if (f.exists()) {
                try {
                    in = new java.io.FileInputStream(f);
                } catch (Exception ignored) {}
            }
        }
        if (in == null) return palette;

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            Pattern p = Pattern.compile("(-wac-[a-zA-Z0-9_-]+):\\s*(#[0-9a-fA-F]{3,8});");
            while ((line = br.readLine()) != null) {
                Matcher m = p.matcher(line);
                while (m.find()) {
                    palette.put(m.group(1), m.group(2));
                }
            }
            br.close();
        } catch (Exception ignored) {}
        return palette;
    }

    /**
     * WCAG 1.4.3 Kontrast (Minimum): Text mot panel/kort i alla teman.
     */
    public static void testWcagCardTextContrast() {
        for (ThemeCatalog.Theme t : ThemeCatalog.all()) {
            if ("default".equals(t.slug)) continue; // Plain Modena hanteras av OS/JavaFX
            Map<String, String> pal = parsePalette(t.stylesheet);
            String card = pal.get("-wac-card");
            String text = pal.get("-wac-text");
            if (card != null && text != null) {
                double ratio = contrastRatio(card, text);
                TestRunner.assertTrue(ratio >= MIN_CONTRAST_NORMAL_TEXT,
                        String.format("Tema '%s': Text/Kort-kontrast %.2f:1 måste vara >= 4.5:1", t.name, ratio));
            }
        }
    }

    /**
     * WCAG 1.4.3 Kontrast: Primärknapp och accent-text mot accentbakgrund.
     */
    public static void testWcagAccentButtonContrast() {
        for (ThemeCatalog.Theme t : ThemeCatalog.all()) {
            if ("default".equals(t.slug)) continue;
            Map<String, String> pal = parsePalette(t.stylesheet);
            String accent = pal.get("-wac-accent");
            String onAccent = pal.get("-wac-on-accent");
            if (accent != null && onAccent != null) {
                double ratio = contrastRatio(accent, onAccent);
                TestRunner.assertTrue(ratio >= MIN_CONTRAST_UI_COMPONENT,
                        String.format("Tema '%s': Knapp/Text-kontrast %.2f:1 måste vara >= 3.0:1 (WCAG AA komponent)", t.name, ratio));
            }
        }
    }

    /**
     * WCAG 1.4.3 Kontrast: Sidomenytext mot sidomenybakgrund.
     */
    public static void testWcagSidebarContrast() {
        for (ThemeCatalog.Theme t : ThemeCatalog.all()) {
            if ("default".equals(t.slug)) continue;
            Map<String, String> pal = parsePalette(t.stylesheet);
            String sidebar = pal.get("-wac-sidebar");
            String sideText = pal.get("-wac-side-text");
            if (sidebar != null && sideText != null) {
                double ratio = contrastRatio(sidebar, sideText);
                TestRunner.assertTrue(ratio >= MIN_CONTRAST_NORMAL_TEXT,
                        String.format("Tema '%s': Sidomenykontrast %.2f:1 måste vara >= 4.5:1", t.name, ratio));
            }
        }
    }

    /**
     * WCAG 2.4.7 Synlig fokusindikator: Verifierar att komponentstilar har :focused regler.
     */
    public static void testWcagFocusIndicatorsPresent() {
        InputStream in = WcagAccessibilityTest.class.getResourceAsStream("/com/wac/autocore/theme/components.css");
        if (in == null) {
            File f = new File("WigellAutoCore/autocore/src/resources/com/wac/autocore/theme/components.css");
            if (f.exists()) {
                try {
                    in = new java.io.FileInputStream(f);
                } catch (Exception ignored) {}
            }
        }
        TestRunner.assertTrue(in != null, "components.css skall finnas");

        boolean hasFocusedRule = false;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(":focused")) {
                    hasFocusedRule = true;
                    break;
                }
            }
            br.close();
        } catch (Exception ignored) {}

        TestRunner.assertTrue(hasFocusedRule, "WCAG 2.4.7 kräver att :focused-stil finns för tangentbordsnavigering");
    }

    /**
     * WCAG 1.4.4 Textstorlek: Verifierar att ingen text i CSS är mindre än 10px.
     */
    public static void testWcagMinimumFontSize() {
        for (ThemeCatalog.Theme t : ThemeCatalog.all()) {
            InputStream in = WcagAccessibilityTest.class.getResourceAsStream(t.stylesheet);
            if (in == null) {
                File f = new File("WigellAutoCore/autocore/src/resources" + t.stylesheet);
                if (f.exists()) {
                    try {
                        in = new java.io.FileInputStream(f);
                    } catch (Exception ignored) {}
                }
            }
            if (in == null) continue;

            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                String line;
                Pattern p = Pattern.compile("-fx-font-size:\\s*([0-9]+)px");
                while ((line = br.readLine()) != null) {
                    Matcher m = p.matcher(line);
                    while (m.find()) {
                        int size = Integer.parseInt(m.group(1));
                        TestRunner.assertTrue(size >= 10,
                                String.format("Tema '%s' innehåller textstorlek %dpx som understiger minimigränsen 10px", t.name, size));
                    }
                }
                br.close();
            } catch (Exception ignored) {}
        }
    }
}
