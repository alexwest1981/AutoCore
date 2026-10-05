package com.wac.autocore.ui.navigation;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Invoice;
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
        // Funktionen styrs av växeln globalSearch i config/features.properties. Är den av finns
        // inget fält i menyn som kan anropa den här, men spärren ligger i den här flaskhalsen
        // så att ingen annan väg in heller går förbi den.
        if (!com.wac.autocore.config.FeatureFlags.isEnabled("globalSearch")) {
            return;
        }
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


    public void smartNavigateForSearch(String query) {
        applySearch(query);
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
     * 3. Fakturor: bokningar där hela arbetet är klart och ingen faktura finns. En bokning ger
     *    ett märke, för det är en faktura som ska skapas, oavsett hur många arbetsordrar den har.
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
                countBookingsReadyForInvoice(garage.getBookings(), garage.getWorkOrders(),
                        garage.getInvoices()));
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
     * Bokningar där hela arbetet är klart och ingen faktura finns, alltså antalet fakturor som
     * väntar på att skapas. En bokning ger ett märke, även om den har flera arbetsordrar, eftersom
     * fakturan täcker hela bokningen. Samma regel som fakturavyn använder.
     */
    public static int countBookingsReadyForInvoice(List<Booking> bookings, List<WorkOrder> orders,
                                                   List<Invoice> invoices) {
        int count = 0;
        for (Booking booking : bookings) {
            if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
                continue;
            }
            boolean hasOrder = false;
            boolean allCompleted = true;
            boolean invoiced = false;
            for (WorkOrder order : orders) {
                if (order.getBookingId() != booking.getId()) {
                    continue;
                }
                hasOrder = true;
                if (order.getStatus() == null || !"COMPLETED".equalsIgnoreCase(order.getStatus())) {
                    allCompleted = false;
                }
                for (Invoice invoice : invoices) {
                    if (invoice.getWorkOrderId() == order.getId()) {
                        invoiced = true;
                        break;
                    }
                }
            }
            if (hasOrder && allCompleted && !invoiced) {
                count++;
            }
        }
        return count;
    }
}
