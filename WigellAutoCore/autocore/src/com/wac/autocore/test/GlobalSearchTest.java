package com.wac.autocore.test;

import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.util.GlobalSearch;
import com.wac.autocore.ui.util.GlobalSearch.SearchResults;

/**
 * Automatiserade enhetstester för GlobalSearch-motorn.
 */
public class GlobalSearchTest {

    private final GarageSystem garage = new GarageSystem();

    public void testEmptyAndNullQueries() {
        SearchResults resNull = GlobalSearch.search(garage, null);
        assertCondition(resNull.isEmpty(), "Null query should be empty");
        assertCondition(resNull.getTotalMatches() == 0, "Total matches for null should be 0");
        assertCondition(resNull.getSectionsWithMatchesCount() == 0, "Sections for null should be 0");

        SearchResults resEmpty = GlobalSearch.search(garage, "   ");
        assertCondition(resEmpty.isEmpty(), "Whitespace query should be empty");
    }

    public void testCustomerSearch() {
        SearchResults res = GlobalSearch.search(garage, "Anna");
        assertCondition(!res.getCustomers().isEmpty(), "Should find customer with name Anna");
        assertCondition(res.getCustomers().get(0).getName().toLowerCase().contains("anna"),
                "First customer should match Anna");
    }

    public void testVehicleSearch() {
        SearchResults res = GlobalSearch.search(garage, "Volvo");
        assertCondition(!res.getVehicles().isEmpty(), "Should find vehicles with brand Volvo");
        assertCondition(res.getSectionsWithMatchesCount() >= 1, "Should have at least 1 section for Volvo");
    }

    public void testCrossEntitySearch() {
        // Om vi söker på ett namn som har både kundprofil och fordon/ordrar
        SearchResults res = GlobalSearch.search(garage, "Anna");
        assertCondition(!res.getCustomers().isEmpty(), "Should match customers");
        assertCondition(res.getTotalMatches() >= 1, "Total matches should be >= 1");
    }

    public void testMechanicSearch() {
        SearchResults res = GlobalSearch.search(garage, "Johan");
        assertCondition(!res.getMechanics().isEmpty(), "Should find mechanic Johan");
        assertCondition(res.getMechanics().get(0).getName().contains("Johan"),
                "Mechanic name should contain Johan");

        SearchResults resSpec = GlobalSearch.search(garage, "Diagnostics");
        assertCondition(!resSpec.getMechanics().isEmpty(), "Should find mechanic with specialization Diagnostics");
    }

    public void testCaseInsensitiveSearch() {
        SearchResults lower = GlobalSearch.search(garage, "volvo");
        SearchResults upper = GlobalSearch.search(garage, "VOLVO");
        SearchResults mixed = GlobalSearch.search(garage, "VoLvO");

        assertCondition(lower.getTotalMatches() == upper.getTotalMatches(),
                "Lowercase and uppercase search counts must match");
        assertCondition(lower.getTotalMatches() == mixed.getTotalMatches(),
                "Mixed case search count must match");
    }

    public void testNonExistentQuery() {
        SearchResults res = GlobalSearch.search(garage, "ZXY_NOT_FOUND_TERM_9999");
        assertCondition(res.isEmpty(), "Non existent query should return empty results");
        assertCondition(res.getTotalMatches() == 0, "Total matches should be 0");
        assertCondition(res.getSectionsWithMatchesCount() == 0, "Sections with matches should be 0");
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Test misslyckades: " + message);
        }
    }
}
