package com.wac.autocore.ui.navigation;

import com.wac.autocore.exception.AppException;
import com.wac.autocore.model.*;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.ActionDialogs;
import com.wac.autocore.ui.components.TableFactory.FilterableTable;
import com.wac.autocore.ui.views.EntityPages;
import com.wac.autocore.ui.views.OverviewView;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/** Handles page navigation and keeps track of the active table. */
public class PageRouter {

    private final GarageSystem garage;
    private final VBox pageBox;
    private SidebarView sidebar;

    private FilterableTable<?> activeTable;
    private String currentPageKey;
    private boolean reloadPending;
    private boolean showCancelledWorkOrders;

    public PageRouter(GarageSystem garage, VBox pageBox) {
        this(garage, pageBox, null);
    }

    public PageRouter(GarageSystem garage, VBox pageBox, SidebarView sidebar) {
        this.garage = garage;
        this.pageBox = pageBox;
        this.sidebar = sidebar;

        // Whatever wrote to the database, the page on screen asks again. One place covers every
        // dialog, every Kanban click and everything the console does behind the window.
        garage.addChangeListener(this::requestReload);

        com.wac.autocore.ui.i18n.I18n.addListener(lang -> requestReload());
    }

    public void setSidebar(SidebarView sidebar) {
        this.sidebar = sidebar;
    }

    public void setActiveTable(FilterableTable<?> table) {
        this.activeTable = table;
    }

    /** True when the work order list also shows the cancelled ones. */
    public boolean isShowingCancelledWorkOrders() {
        return showCancelledWorkOrders;
    }

    /** Turns the cancelled orders on or off in the list, and redraws the page when it is up. */
    public void setShowCancelledWorkOrders(boolean show) {
        if (show == showCancelledWorkOrders) {
            return;
        }
        showCancelledWorkOrders = show;
        if ("workorders".equals(currentPageKey)) {
            requestReload();
        }
    }

    /**
     * The rows the work order page is built from. A cancelled order waits for nothing and is not
     * work in hand, but it is kept rather than removed: the details dialog still offers to start
     * it again, and that road goes through this list.
     */
    public List<WorkOrder> workOrdersForList() {
        List<WorkOrder> all = garage.getWorkOrders();
        if (showCancelledWorkOrders) {
            return all;
        }
        List<WorkOrder> open = new ArrayList<WorkOrder>();
        for (WorkOrder order : all) {
            if (order.getStatus() != WorkOrderStatus.CANCELLED) {
                open.add(order);
            }
        }
        return open;
    }

    /** True when the order is cancelled, that is one of the rows the list keeps out by default. */
    public boolean isCancelledWorkOrder(int workOrderId) {
        for (WorkOrder order : garage.getWorkOrders()) {
            if (order.getId() == workOrderId) {
                return order.getStatus() == WorkOrderStatus.CANCELLED;
            }
        }
        return false;
    }

    public void navigateToWorkOrder(int workOrderId) {
        // A row that is not in the list cannot be turned to, so a cancelled order is let in before
        // the page is built. Otherwise the jump would land on the right page with nothing marked.
        boolean wasShowingCancelled = showCancelledWorkOrders;
        if (isCancelledWorkOrder(workOrderId)) {
            showCancelledWorkOrders = true;
        }
        if ("workorders".equals(currentPageKey) && wasShowingCancelled != showCancelledWorkOrders) {
            // Already on the page, and the list has to grow before the row can be found in it.
            showCurrentPage();
        } else {
            navigate("workorders");
        }
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
        // The page you are on is not a place to go. Its menu item is disabled, and pressing it
        // again must not throw the page away and build it anew.
        if (key.equals(currentPageKey)) {
            return;
        }
        this.currentPageKey = key;
        if (sidebar != null) {
            sidebar.setSelectedPage(key);
        }
        showCurrentPage();
    }

    /**
     * Redraws the page on screen once the current turn is over. Several writes in one action, and
     * the write plus its own callback, still cost one redraw instead of one each.
     */
    public void requestReload() {
        if (reloadPending || currentPageKey == null) {
            return;
        }
        reloadPending = true;
        javafx.application.Platform.runLater(new Runnable() {
            @Override
            public void run() {
                reloadPending = false;
                showCurrentPage();
            }
        });
    }

    /** Builds the page {@link #currentPageKey} names, in the place the old one stood. */
    private void showCurrentPage() {
        if (sidebar != null) {
            updateNavCounts();
        }
        activeTable = null;
        pageBox.getChildren().clear();

        try {
            if ("overview".equals(currentPageKey)) {
                pageBox.getChildren().add(OverviewView.build(garage, this::requestReload, this));
            } else if ("customers".equals(currentPageKey)) {
                pageBox.getChildren().add(EntityPages.buildCustomersPage(garage, this));
            } else if ("vehicles".equals(currentPageKey)) {
                pageBox.getChildren().add(EntityPages.buildVehiclesPage(garage, this));
            } else if ("bookings".equals(currentPageKey)) {
                pageBox.getChildren().add(EntityPages.buildBookingsPage(garage, this));
            } else if ("workorders".equals(currentPageKey)) {
                pageBox.getChildren().add(EntityPages.buildWorkOrdersPage(garage, this));
            } else if ("services".equals(currentPageKey)) {
                pageBox.getChildren().add(EntityPages.buildServicesPage(garage, this));
            } else if ("mechanics".equals(currentPageKey)) {
                pageBox.getChildren().add(EntityPages.buildMechanicsPage(garage, this));
            } else if ("invoices".equals(currentPageKey)) {
                pageBox.getChildren().add(EntityPages.buildInvoicesPage(garage, this));
            } else if ("payments".equals(currentPageKey)) {
                pageBox.getChildren().add(EntityPages.buildPaymentsPage(garage, this));
            }
        } catch (AppException unreadable) {
            // A page that cannot be read should say so instead of coming up empty.
            pageBox.getChildren().clear();
            pageBox.getChildren().add(com.wac.autocore.ui.components.UiComponents.mutedNote(
                    com.wac.autocore.ui.i18n.I18n.get("view.error.read_failed")));
        }
        if (!pageBox.getChildren().isEmpty()) {
            com.wac.autocore.ui.motion.Motion.enterPage(
                    pageBox.getChildren().get(pageBox.getChildren().size() - 1));
        }
        if (sidebar != null) {
            sidebar.realign();
        }
    }

    // The drop-in is created in a dialog on top of the current page, and the page is reloaded
    // afterwards so the new booking shows right away.
    private void openDropInDialog() {
        ActionDialogs.showCreateDropInWorkOrderDialog(garage, this::requestReload);
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
            WorkOrderStatus status = order.getStatus();
            if (status == WorkOrderStatus.COMPLETED || status == WorkOrderStatus.CANCELLED) {
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
            if (booking.getStatus() == BookingStatus.CANCELLED) {
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
            if (booking.getStatus() == BookingStatus.CANCELLED) {
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
                if (order.getStatus() != WorkOrderStatus.COMPLETED) {
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
