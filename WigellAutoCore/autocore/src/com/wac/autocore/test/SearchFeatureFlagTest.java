package com.wac.autocore.test;

import com.wac.autocore.config.FeatureFlags;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.navigation.PageRouter;
import javafx.scene.layout.VBox;

/**
 * KVALITET: sökfunktionen styrs av växeln globalSearch i config/features.properties.
 *
 * Två saker prövas: att växeln läses ur filen (en borttappad eller felstavad fil får inte tyst
 * sätta på allt), och att den faktiskt styr ingången - en växel som finns men inte används är
 * bara en kommentar.
 */
public class SearchFeatureFlagTest {

    private static final String FLAG = "autocore.features.globalSearch";

    /** Växeln läses ur filen: globalSearch är på, och en nyckel som inte finns är av. */
    public void testTheFlagIsReadFromTheConfigFile() {
        TestRunner.assertTrue(FeatureFlags.isEnabled("globalSearch"),
                "globalSearch ska vara på i config/features.properties (saknas filen blir allt av)");
        TestRunner.assertFalse(FeatureFlags.isEnabled("finns.inte"),
                "En nyckel som inte står i filen ska vara av");
    }

    /** Avstängd växel stänger sökvägen; påslagen släpper den igenom och bygger sökvyn. */
    public void testTheFlagGatesTheSearchEntryPoint() {
        GarageSystem garage = new GarageSystem();
        VBox pageBox = new VBox();
        PageRouter router = new PageRouter(garage, pageBox);

        try {
            System.setProperty(FLAG, "false");
            router.navigate("overview");
            router.applySearch("Anna");
            TestRunner.assertEquals("overview", router.getCurrentPageKey(),
                    "Med växeln av ska en sökning inte byta sida");

            System.setProperty(FLAG, "true");
            router.applySearch("Anna");
            TestRunner.assertEquals("search", router.getCurrentPageKey(),
                    "Med växeln på ska en sökning öppna sökvyn");
            TestRunner.assertTrue(pageBox.getChildren().size() > 0,
                    "Sökvyn ska ha byggts i sidbehållaren");
        } finally {
            System.clearProperty(FLAG);
        }
    }
}
