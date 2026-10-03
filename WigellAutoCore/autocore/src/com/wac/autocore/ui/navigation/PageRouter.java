package com.wac.autocore.ui.navigation;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.components.TableFactory.FilterableTable;
import com.wac.autocore.ui.views.EntityPages;
import com.wac.autocore.ui.views.OverviewView;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Hanterar sidnavigering och kopplar samman aktiva tabeller med sökfiltret.
 */
public class PageRouter {

    private final GarageSystem garage;
    private final VBox pageBox;
    private SidebarView sidebar;

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
            activeTable.applySearch(String.valueOf(workOrderId));
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
            updateNavCounts();
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

    /**
     * Räknarna i menyn visar posterna som väntar på att bli hanterade: arbetsordrar som inte
     * påbörjats, bokningar som ingen arbetsorder skapats ur än, obetalda fakturor och betalningar
     * som inte gick igenom.
     *
     * Anropas från navigate(), som är enda vägen till en ny vy — därför följer räknaren med
     * varje ändring som ritar om sidan, utan egna lyssnare.
     */
    private void updateNavCounts() {
        sidebar.setNavCount("workorders", countNewWorkOrders(garage.getWorkOrders()));
        sidebar.setNavCount("bookings", countNewBookings(garage.getBookings()));
        sidebar.setNavCount("invoices", countUnpaidInvoices(garage.getInvoices()));
        sidebar.setNavCount("payments", countFailedPayments(garage.getPayments()));
    }

    /** Obetalda fakturor är pengar som ska in, alltså det som väntar på hantering. */
    public static int countUnpaidInvoices(List<Invoice> invoices) {
        int count = 0;
        for (Invoice invoice : invoices) {
            if (!invoice.isPaid()) {
                count++;
            }
        }
        return count;
    }

    /** En betalning som inte gick igenom ska göras om, och är därför kvar att hantera. */
    public static int countFailedPayments(List<Payment> payments) {
        int count = 0;
        for (Payment payment : payments) {
            if (!payment.isSuccessful()) {
                count++;
            }
        }
        return count;
    }

    public static int countNewWorkOrders(List<WorkOrder> orders) {
        int count = 0;
        for (WorkOrder order : orders) {
            if ("CREATED".equalsIgnoreCase(order.getStatus())) {
                count++;
            }
        }
        return count;
    }

    public static int countNewBookings(List<Booking> bookings) {
        int count = 0;
        for (Booking booking : bookings) {
            if ("BOOKED".equalsIgnoreCase(booking.getStatus())) {
                count++;
            }
        }
        return count;
    }
}
