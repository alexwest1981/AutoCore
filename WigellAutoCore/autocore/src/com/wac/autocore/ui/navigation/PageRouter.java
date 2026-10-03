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
     * Räknarna i menyn visar vad som väntar på att bli hanterat på den sidan, inte allt som finns där:
     * 1. Arbetsordrar: bokningar som ännu inte har någon arbetsorder, alltså jobb att skapa en order för.
     * 2. Bokningar: inget märke. Sidan visar bokningar, den har inget eget arbete att göra.
     * 3. Fakturor: slutförda arbetsordrar som ännu inte har någon faktura.
     * 4. Betalningar: fakturor som väntar på betalning (!isPaid).
     *
     * Anropas från navigate(), som är enda vägen till en ny vy, och därför följer räknaren med
     * varje ändring som ritar om sidan, utan egna lyssnare.
     */
    public void updateNavCounts() {
        if (sidebar == null) {
            return;
        }
        sidebar.setNavCount("workorders",
                countBookingsWithoutWorkOrder(garage.getBookings(), garage.getWorkOrders()));
        sidebar.setNavCount("bookings", 0);
        sidebar.setNavCount("invoices",
                countCompletedOrdersWithoutInvoice(garage.getWorkOrders(), garage.getInvoices()));
        sidebar.setNavCount("payments", countUnpaidInvoices(garage.getInvoices()));
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

    /** Betalda fakturor ger +1 i betalningsmenyn. */
    public static int countPaidInvoices(List<Invoice> invoices) {
        int count = 0;
        for (Invoice invoice : invoices) {
            if (invoice.isPaid()) {
                count++;
            }
        }
        return count;
    }

    /** En betalning som gått igenom / betald faktura ger +1 i betalningsmenyn. */
    public static int countPaidPayments(List<Payment> payments) {
        int count = 0;
        for (Payment payment : payments) {
            if (payment.isSuccessful()) {
                count++;
            }
        }
        return count;
    }

    /** Bakåtkompatibilitet: betalningar som inte gick igenom. */
    public static int countFailedPayments(List<Payment> payments) {
        int count = 0;
        for (Payment payment : payments) {
            if (!payment.isSuccessful()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Räknar aktiva arbetsordrar som är under arbete eller väntar på att slutföras
     * (status CREATED eller IN_PROGRESS). Slutförd arbetsorder (COMPLETED) lämnar räknaren.
     */
    public static int countNewWorkOrders(List<WorkOrder> orders) {
        int count = 0;
        for (WorkOrder order : orders) {
            if (order.getStatus() != null && ("CREATED".equalsIgnoreCase(order.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(order.getStatus()))) {
                count++;
            }
        }
        return count;
    }

    /**
     * Räknar bokningar som väntar på en arbetsorder (status BOOKED).
     * När bokningen får en arbetsorder (WORK_ORDER_CREATED) flyttas den vidare till arbetsordermenyn.
     */
    public static int countNewBookings(List<Booking> bookings) {
        int count = 0;
        for (Booking booking : bookings) {
            if ("BOOKED".equalsIgnoreCase(booking.getStatus())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Bokningar som ännu inte har någon arbetsorder, alltså jobb att skapa en order för.
     * Avbokade bokningar räknas inte, de ska inte bli någon arbetsorder.
     */
    public static int countBookingsWithoutWorkOrder(List<Booking> bookings, List<WorkOrder> orders) {
        int count = 0;
        for (Booking booking : bookings) {
            if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
                continue;
            }
            boolean hasOrder = false;
            for (WorkOrder order : orders) {
                if (order.getBookingId() == booking.getId()) {
                    hasOrder = true;
                    break;
                }
            }
            if (!hasOrder) {
                count++;
            }
        }
        return count;
    }

    /**
     * Slutförda arbetsordrar som ännu inte har någon faktura, alltså jobb att fakturera.
     * Statuslösa och icke slutförda ordrar hoppas över.
     */
    public static int countCompletedOrdersWithoutInvoice(List<WorkOrder> orders, List<Invoice> invoices) {
        int count = 0;
        for (WorkOrder order : orders) {
            if (order.getStatus() == null || !"COMPLETED".equalsIgnoreCase(order.getStatus())) {
                continue;
            }
            boolean hasInvoice = false;
            for (Invoice invoice : invoices) {
                if (invoice.getWorkOrderId() == order.getId()) {
                    hasInvoice = true;
                    break;
                }
            }
            if (!hasInvoice) {
                count++;
            }
        }
        return count;
    }
}
