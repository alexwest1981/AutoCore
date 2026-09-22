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
    private TopNavView topNav;

    private FilterableTable<?> activeTable;
    private String currentPageKey;
    private String lastNonSearchPage = "overview";
    private String currentSearchQuery = "";

    public PageRouter(GarageSystem garage, VBox pageBox) {
        this(garage, pageBox, null);
    }

    public PageRouter(GarageSystem garage, VBox pageBox, SidebarView sidebar) {
        this.garage = garage;
        this.pageBox = pageBox;
        this.sidebar = sidebar;
        com.wac.autocore.ui.i18n.I18n.addListener(lang -> {
            if (currentPageKey != null) {
                navigate(currentPageKey);
            }
        });
    }

    public void setSidebar(SidebarView sidebar) {
        this.sidebar = sidebar;
    }

    public void setTopNav(TopNavView topNav) {
        this.topNav = topNav;
    }

    public void setActiveTable(FilterableTable<?> table) {
        this.activeTable = table;
        if (this.activeTable != null && currentSearchQuery != null && !currentSearchQuery.isEmpty() && !"search".equals(currentPageKey)) {
            this.activeTable.applySearch(currentSearchQuery);
        }
    }

    public void applySearch(String query) {
        this.currentSearchQuery = query == null ? "" : query;
        String trimmed = this.currentSearchQuery.trim();

        if (trimmed.isEmpty()) {
            if ("search".equals(currentPageKey)) {
                navigate(lastNonSearchPage != null && !"search".equals(lastNonSearchPage) ? lastNonSearchPage : "overview");
            } else if (activeTable != null) {
                activeTable.applySearch("");
            }
            return;
        }

        if (!"search".equals(currentPageKey)) {
            this.lastNonSearchPage = currentPageKey != null ? currentPageKey : "overview";
            navigate("search");
        } else {
            // Redan på söksidan – uppdatera vyn i realtid
            activeTable = null;
            pageBox.getChildren().clear();
            pageBox.getChildren().add(com.wac.autocore.ui.views.SearchResultsView.build(garage, this, trimmed));
        }
    }

    public String getCurrentSearchQuery() {
        return currentSearchQuery;
    }

    public void smartNavigateForSearch(String query) {
        applySearch(query);
    }

    public String getCurrentPageKey() {
        return currentPageKey;
    }

    public void navigateToWorkOrder(int workOrderId) {
        navigate("workorders");
        if (workOrderId > 0 && activeTable != null) {
            applySearch(String.valueOf(workOrderId));
            javafx.scene.control.TableView<?> tv = activeTable.getTableView();
            int idx = 0;
            for (Object item : tv.getItems()) {
                if (item instanceof com.wac.autocore.model.WorkOrder && ((com.wac.autocore.model.WorkOrder) item).getId() == workOrderId) {
                    tv.getSelectionModel().select(idx);
                    break;
                }
                idx++;
            }
        }
    }

    public void navigate(String key) {
        this.currentPageKey = key;
        if (!"search".equals(key)) {
            this.lastNonSearchPage = key;
        }
        if (sidebar != null) {
            sidebar.setSelectedPage(key);
        }
        if (topNav != null) {
            topNav.setSelectedPage(key);
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
        } else if ("search".equals(key)) {
            pageBox.getChildren().add(com.wac.autocore.ui.views.SearchResultsView.build(garage, this, currentSearchQuery));
        }
    }
}
