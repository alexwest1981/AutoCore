package com.wac.autocore.ui.navigation;

import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.components.TableFactory.FilterableTable;
import com.wac.autocore.ui.views.EntityPages;
import com.wac.autocore.ui.views.OverviewView;
import javafx.scene.layout.VBox;

/**
 * Hanterar sidnavigering och kopplar samman aktiva tabeller med sökfiltret.
 */
public class PageRouter {

    private final GarageSystem garage;
    private final VBox pageBox;
    private SidebarView sidebar;

    private FilterableTable<?> activeTable;
    private String currentPageKey;
    private String currentSearchQuery = "";

    public PageRouter(GarageSystem garage, VBox pageBox) {
        this(garage, pageBox, null);
    }

    public PageRouter(GarageSystem garage, VBox pageBox, SidebarView sidebar) {
        this.garage = garage;
        this.pageBox = pageBox;
        this.sidebar = sidebar;
    }

    public void setSidebar(SidebarView sidebar) {
        this.sidebar = sidebar;
    }

    public void setActiveTable(FilterableTable<?> table) {
        this.activeTable = table;
        if (this.activeTable != null && currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
            this.activeTable.applySearch(currentSearchQuery);
        }
    }

    public void applySearch(String query) {
        this.currentSearchQuery = query;
        if (activeTable != null) {
            activeTable.applySearch(query);
        }
    }

    public String getCurrentSearchQuery() {
        return currentSearchQuery;
    }

    public void smartNavigateForSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        applySearch(query.trim());
        if (!"overview".equals(currentPageKey)) {
            return;
        }

        final String q = query.trim().toLowerCase();
        for (com.wac.autocore.model.Customer c : garage.getCustomers()) {
            if (c.getName().toLowerCase().contains(q) || c.getPhone().toLowerCase().contains(q) || c.getEmail().toLowerCase().contains(q)) {
                navigate("customers");
                return;
            }
        }
        for (com.wac.autocore.model.Vehicle v : garage.getVehicles()) {
            if (v.getRegistrationNumber().toLowerCase().contains(q) || v.getBrand().toLowerCase().contains(q) || v.getModel().toLowerCase().contains(q)) {
                navigate("vehicles");
                return;
            }
        }
        for (com.wac.autocore.model.Mechanic m : garage.getMechanics()) {
            if (m.getName().toLowerCase().contains(q) || m.getSpecialization().toLowerCase().contains(q)) {
                navigate("mechanics");
                return;
            }
        }
        navigate("workorders");
    }

    public String getCurrentPageKey() {
        return currentPageKey;
    }

    public void navigate(String key) {
        this.currentPageKey = key;
        if (sidebar != null) {
            sidebar.setSelectedPage(key);
        }
        activeTable = null;
        pageBox.getChildren().clear();

        if ("overview".equals(key)) {
            pageBox.getChildren().add(OverviewView.build(garage, () -> navigate("overview"), this));
        } else if ("customers".equals(key)) {
            pageBox.getChildren().add(EntityPages.buildCustomersPage(garage, this));
        } else if ("vehicles".equals(key)) {
            pageBox.getChildren().add(EntityPages.buildVehiclesPage(garage, this));
        } else if ("bookings".equals(key)) {
            pageBox.getChildren().add(EntityPages.buildBookingsPage(garage, this));
        } else if ("workorders".equals(key)) {
            pageBox.getChildren().add(EntityPages.buildWorkOrdersPage(garage, this));
        } else if ("services".equals(key)) {
            pageBox.getChildren().add(EntityPages.buildServicesPage(garage, this));
        } else if ("mechanics".equals(key)) {
            pageBox.getChildren().add(EntityPages.buildMechanicsPage(garage, this));
        } else if ("invoices".equals(key)) {
            pageBox.getChildren().add(EntityPages.buildInvoicesPage(garage, this));
        } else if ("payments".equals(key)) {
            pageBox.getChildren().add(EntityPages.buildPaymentsPage(garage, this));
        }
    }
}
