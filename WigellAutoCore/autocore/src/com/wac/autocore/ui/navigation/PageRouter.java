package com.wac.autocore.ui.navigation;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.ActionDialogs;
import com.wac.autocore.ui.components.TableFactory.FilterableTable;
import com.wac.autocore.ui.views.EntityPages;
import com.wac.autocore.ui.views.OverviewView;
import javafx.scene.layout.VBox;

import java.util.List;

/** Handles page navigation and keeps track of the active table. */
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
    }

    public void navigateToWorkOrder(int workOrderId) {
        navigate("workorders");
        if (workOrderId <= 0 || activeTable == null) {
            return;
        }
        // The order may sit on another page than the one on screen, so search the whole list and
        // turn to the right page before selecting the row — otherwise it would silently not show.
        for (int index = 0; index < activeTable.getBaseList().size(); index++) {
            Object item = activeTable.getBaseList().get(index);
            if (!(item instanceof com.wac.autocore.model.WorkOrder)) {
                continue;
            }
            if (((com.wac.autocore.model.WorkOrder) item).getId() != workOrderId) {
                continue;
            }
            activeTable.showPage(index / activeTable.getPageSize());
            javafx.scene.control.TableView<?> tv = activeTable.getTableView();
            int rowOnPage = index % activeTable.getPageSize();
            if (rowOnPage < tv.getItems().size()) {
                tv.getSelectionModel().select(rowOnPage);
                tv.scrollTo(rowOnPage);
            }
            return;
        }
    }

    public void navigate(String key) {
        // A drop-in is a dialog, not a page. You can be in the middle of something else and still
        // need to add a customer already standing in the workshop, so the current page stays behind.
        if ("dropin".equals(key)) {
            openDropInDialog();
            return;
        }
        this.currentPageKey = key;
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
        }
    }

    // The drop-in is created in a dialog on top of the current page, and the page is reloaded
    // afterwards so the new booking shows right away.
    private void openDropInDialog() {
        ActionDialogs.showCreateDropInWorkOrderDialog(garage, () -> {
            if (currentPageKey != null) {
                navigate(currentPageKey);
            }
        });
    }

    /** The badges in the menu show what is waiting on the page, not everything that is there. */
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

    /** Unpaid invoices are money to come in, that is what is waiting to be handled. */
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
     * Reclamations not yet dealt with. A reclamation is an ordinary work order, so it waits
     * until it is completed. Cancelled ones do not count, they wait for nothing.
     */
    public static int countOpenReclamations(List<WorkOrder> orders) {
        int count = 0;
        for (WorkOrder order : orders) {
            if (!order.isReclamation()) {
                continue;
            }
            String status = order.getStatus();
            if (status != null && ("COMPLETED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status))) {
                continue;
            }
            count++;
        }
        return count;
    }

    /** Bookings without a work order. Cancelled ones do not count. */
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

    /** Bookings where the work is done and no invoice exists. One booking gives one badge. */
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
