package com.wac.autocore.test;

import com.wac.autocore.config.FeatureFlags;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.AutoCoreApp;
import com.wac.autocore.ui.components.UiComponents;
import com.wac.autocore.ui.navigation.PageRouter;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * KVALITET: sökfunktionen styrs av växeln globalSearch i config/features.properties, och sökfältet
 * hör hemma till höger i sidhuvudet - i samma rad som sidtiteln, inte i menyn och inte över
 * innehållet.
 *
 * Fyra saker prövas: att växeln läses ur filen (en borttappad eller felstavad fil får inte tyst
 * sätta på allt), att den faktiskt styr ingången, att fältet bara byggs när växeln är på, och att
 * fältet hamnar till höger i sidhuvudet och flyttar med när man byter sida. En växel som finns men
 * inte används är bara en kommentar.
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

    /** Sökfältet byggs bara när växeln är på, och det bär fältet som dropdownen kopplas till. */
    public void testTheSearchBarIsBuiltOnlyWhenTheFlagIsOn() {
        GarageSystem garage = new GarageSystem();
        PageRouter router = new PageRouter(garage, new VBox());

        try {
            System.setProperty(FLAG, "false");
            TestRunner.assertTrue(AutoCoreApp.buildSearchBar(garage, router) == null,
                    "Med växeln av ska inget sökfält byggas");

            System.setProperty(FLAG, "true");
            HBox bar = AutoCoreApp.buildSearchBar(garage, router);
            TestRunner.assertNotNull(bar, "Med växeln på ska sökfältet byggas");
            TestRunner.assertNotNull(findSearchField(bar),
                    "Sökfältet ska innehålla fältet som dropdownen kopplas till");
        } finally {
            System.clearProperty(FLAG);
        }
    }

    /** Fältet står till höger i sidhuvudet, i samma rad som sidtiteln, och följer med vid sidbyte. */
    public void testTheSearchFieldSitsInThePageHeader() {
        GarageSystem garage = new GarageSystem();
        PageRouter router = new PageRouter(garage, new VBox());

        try {
            System.setProperty(FLAG, "true");
            HBox bar = AutoCoreApp.buildSearchBar(garage, router);
            UiComponents.setHeaderRight(bar);

            VBox overview = UiComponents.pageHead("Overview", "status", "AutoCore");
            TestRunner.assertTrue(isRightInHeader(overview, bar),
                    "Sökfältet ska ligga till höger i samma rad som sidtiteln");

            VBox customers = UiComponents.pageHead("Kunder", "status", "AutoCore");
            TestRunner.assertTrue(isRightInHeader(customers, bar),
                    "Vid sidbyte ska sökfältet flytta med till det nya sidhuvudet");
        } finally {
            UiComponents.setHeaderRight(null);
            System.clearProperty(FLAG);
        }
    }

    /** Sant om noden ligger sist i sidhuvudets rad, alltså längst till höger bredvid sidtiteln. */
    private static boolean isRightInHeader(VBox head, Node node) {
        for (Node child : head.getChildren()) {
            if (child instanceof HBox) {
                HBox row = (HBox) child;
                int last = row.getChildren().size() - 1;
                return last > 0 && row.getChildren().get(last) == node;
            }
        }
        return false;
    }

    private static TextField findSearchField(Node node) {
        if (node instanceof TextField) {
            return (TextField) node;
        }
        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                TextField found = findSearchField(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
