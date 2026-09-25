package com.wac.autocore.test;

import com.wac.autocore.theme.ThemeCatalog;
import com.wac.autocore.ui.i18n.I18n;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Automatiserade tester för kodkvalitet, arkitekturrenhet och resursintegritet:
 * - Nyckelparitet och noll tomma strängar i språkfiler
 * - Temaintegritet (alla katalogiserade stilmallar existerar)
 * - Skiktad arkitektur (Service-lagret är frikopplat från JavaFX UI-komponenter)
 * - Komplexitetsbegränsningar (inga enorma monolitiska klasser > 1200 rader)
 */
public class CodeQualityTest {

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
     * KVALITET: Kontrollerar 100% paritet mellan en.json och sv.json och att inga värden är tomma.
     */
    public static void testTranslationQualityAndCompleteness() {
        Map<String, String> en = I18n.loadDictionary("en");
        Map<String, String> sv = I18n.loadDictionary("sv");

        TestRunner.assertTrue(!en.isEmpty(), "en.json får inte vara tom");
        TestRunner.assertTrue(!sv.isEmpty(), "sv.json får inte vara tom");

        Set<String> missingInSv = new TreeSet<String>(en.keySet());
        missingInSv.removeAll(sv.keySet());

        Set<String> missingInEn = new TreeSet<String>(sv.keySet());
        missingInEn.removeAll(en.keySet());

        TestRunner.assertTrue(missingInSv.isEmpty(), "Följande nycklar saknas i sv.json: " + missingInSv);
        TestRunner.assertTrue(missingInEn.isEmpty(), "Följande nycklar saknas i en.json: " + missingInEn);
        TestRunner.assertEquals(en.size(), sv.size(), "Båda språkfilerna skall ha samma antal nycklar");

        for (Map.Entry<String, String> entry : sv.entrySet()) {
            TestRunner.assertTrue(entry.getValue() != null && !entry.getValue().trim().isEmpty(),
                    "sv.json nyckeln '" + entry.getKey() + "' får inte vara tom");
        }
        for (Map.Entry<String, String> entry : en.entrySet()) {
            TestRunner.assertTrue(entry.getValue() != null && !entry.getValue().trim().isEmpty(),
                    "en.json nyckeln '" + entry.getKey() + "' får inte vara tom");
        }
    }

    /**
     * KVALITET: Kontrollerar att alla teman i ThemeCatalog finns och har giltigt innehåll.
     */
    public static void testThemeCatalogIntegrity() {
        for (ThemeCatalog.Theme t : ThemeCatalog.all()) {
            InputStream in = CodeQualityTest.class.getResourceAsStream(t.stylesheet);
            if (in == null) {
                File f = new File("WigellAutoCore/autocore/src/resources" + t.stylesheet);
                if (f.exists()) {
                    try {
                        in = new java.io.FileInputStream(f);
                    } catch (Exception ignored) {}
                }
            }
            TestRunner.assertTrue(in != null, "Tema-stilmall saknas för tema: " + t.name + " (" + t.stylesheet + ")");
            try {
                if (in != null) in.close();
            } catch (Exception ignored) {}
        }
    }

    /**
     * KVALITET / ARKITEKTUR: Säkerställer att domän-/service-lagret inte importerar JavaFX UI-vyer direkt.
     */
    public static void testServiceLayerDecoupledFromGui() throws Exception {
        File serviceDir = new File(SRC_ROOT, "com/wac/autocore/service");
        List<File> serviceFiles = listJavaFiles(serviceDir);
        TestRunner.assertTrue(!serviceFiles.isEmpty(), "Service-filer måste hittas");

        for (File f : serviceFiles) {
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                String trimmed = line.trim();
                if (trimmed.startsWith("import com.wac.autocore.ui.views.") ||
                        trimmed.startsWith("import com.wac.autocore.gui.")) {
                    TestRunner.assertTrue(false,
                            String.format("Arkitekturfel i %s:%d: Service-lagret får inte vara beroende av UI-vyer (%s)",
                                    f.getName(), lineNum, trimmed));
                }
            }
            br.close();
        }
    }

    /**
     * KVALITET: Kontrollerar att inga filer har vuxit till ohanterliga "god-classes" (> 1200 rader).
     */
    public static void testSourceFileLengthLimits() throws Exception {
        List<File> javaFiles = listJavaFiles(SRC_ROOT);
        int maxLines = 1200;

        for (File f : javaFiles) {
            BufferedReader br = new BufferedReader(new FileReader(f));
            int lines = 0;
            while (br.readLine() != null) {
                lines++;
            }
            br.close();

            TestRunner.assertTrue(lines <= maxLines,
                    String.format("Klass '%s' överskrider modularitetsgränsen (%d > %d rader). Överväg uppdelning.",
                            f.getName(), lines, maxLines));
        }
    }
}
