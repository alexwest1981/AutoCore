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
    }

    public void applySearch(String query) {
        if (activeTable != null) {
            activeTable.applySearch(query);
        }
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
            pageBox.getChildren().add(OverviewView.build(garage, () -> navigate("overview")));
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
