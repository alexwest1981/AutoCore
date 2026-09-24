package com.wac.autocore.ui;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.MechanicSchedule;
import com.wac.autocore.ui.navigation.PageRouter;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;

/**
 * Facade för alla modala formulärdialoger i AutoCore GUI.
 * Delegerar till fokuserade, domänspecifika dialogklasser:
 * - {@link CustomerDialogs}
 * - {@link VehicleDialogs}
 * - {@link BookingDialogs}
 * - {@link WorkOrderDialogs}
 * - {@link BillingDialogs}
 * - {@link MechanicDialogs}
 * - {@link ServiceItemDialogs}
 * - {@link SlotDetailsDialog}
 */
public final class ActionDialogs {

    private ActionDialogs() {}

    // =========================================================================
    // Delade UI-hjälpmetoder för dialoger (används av domändialogerna)
    // =========================================================================

    static void styleDialog(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        if (!pane.getStyleClass().contains("root")) {
            pane.getStyleClass().add("root");
        }
        javafx.scene.Scene appScene = com.wac.autocore.theme.ThemeManager.getCurrentScene();
        if (appScene != null && appScene.getWindow() != null) {
            try {
                dialog.initOwner(appScene.getWindow());
                dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            } catch (Exception ignored) {}
        }
        dialog.setOnShowing(evt -> {
            javafx.scene.Scene currentAppScene = com.wac.autocore.theme.ThemeManager.getCurrentScene();
            if (currentAppScene != null) {
                javafx.scene.Scene dScene = pane.getScene();
                if (dScene != null) {
                    dScene.getStylesheets().setAll(currentAppScene.getStylesheets());
                    if (dScene.getRoot() != null && !dScene.getRoot().getStyleClass().contains("root")) {
                        dScene.getRoot().getStyleClass().add("root");
                    }
                }
            }
        });
    }

    static GridPane createGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(14, 14, 14, 14));
        return grid;
    }

    static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        javafx.scene.Scene appScene = com.wac.autocore.theme.ThemeManager.getCurrentScene();
        if (appScene != null && appScene.getWindow() != null) {
            try {
                alert.initOwner(appScene.getWindow());
                alert.initModality(javafx.stage.Modality.WINDOW_MODAL);
            } catch (Exception ignored) {}
        }
        alert.setOnShowing(evt -> {
            javafx.scene.Scene currentAppScene = com.wac.autocore.theme.ThemeManager.getCurrentScene();
            if (currentAppScene != null) {
                javafx.scene.Scene aScene = alert.getDialogPane().getScene();
                if (aScene != null) {
                    aScene.getStylesheets().setAll(currentAppScene.getStylesheets());
                }
            }
        });
        alert.showAndWait();
    }

    // =========================================================================
    // 1. Kund (CustomerDialogs)
    // =========================================================================

    public static void showCreateCustomerDialog(GarageSystem garage, Runnable onSuccess) {
        CustomerDialogs.showCreateCustomerDialog(garage, onSuccess);
    }

    public static void showEditCustomerDialog(GarageSystem garage, Customer customer, Runnable onSuccess) {
        CustomerDialogs.showEditCustomerDialog(garage, customer, onSuccess);
    }

    public static void showDeleteCustomerConfirmation(GarageSystem garage, Customer customer, Runnable onSuccess) {
        CustomerDialogs.showDeleteCustomerConfirmation(garage, customer, onSuccess);
    }

    // =========================================================================
    // 2. Fordon (VehicleDialogs)
    // =========================================================================

    public static void showCreateVehicleDialog(GarageSystem garage, Runnable onSuccess) {
        VehicleDialogs.showCreateVehicleDialog(garage, onSuccess);
    }

    public static void showEditVehicleDialog(GarageSystem garage, Vehicle vehicle, Runnable onSuccess) {
        VehicleDialogs.showEditVehicleDialog(garage, vehicle, onSuccess);
    }

    public static void showDeleteVehicleConfirmation(GarageSystem garage, Vehicle vehicle, Runnable onSuccess) {
        VehicleDialogs.showDeleteVehicleConfirmation(garage, vehicle, onSuccess);
    }

    // =========================================================================
    // 3. Bokning (BookingDialogs)
    // =========================================================================

    public static void showCreateBookingDialog(GarageSystem garage, Runnable onSuccess) {
        BookingDialogs.showCreateBookingDialog(garage, onSuccess);
    }

    public static void showCreateBookingDialog(GarageSystem garage, LocalDate defaultDate,
                                               Mechanic defaultMechanic, Integer defaultHour, Runnable onSuccess) {
        BookingDialogs.showCreateBookingDialog(garage, defaultDate, defaultMechanic, defaultHour, onSuccess);
    }

    public static void showEditBookingDialog(GarageSystem garage, Booking booking, Runnable onSuccess) {
        BookingDialogs.showEditBookingDialog(garage, booking, onSuccess);
    }

    public static void showCancelBookingConfirmation(GarageSystem garage, Booking booking, Runnable onSuccess) {
        BookingDialogs.showCancelBookingConfirmation(garage, booking, onSuccess);
    }

    public static void showDeleteBookingConfirmation(GarageSystem garage, Booking booking, Runnable onSuccess) {
        BookingDialogs.showDeleteBookingConfirmation(garage, booking, onSuccess);
    }

    // =========================================================================
    // 4. Arbetsorder (WorkOrderDialogs)
    // =========================================================================

    public static void showCreateWorkOrderDialog(GarageSystem garage, Runnable onSuccess) {
        WorkOrderDialogs.showCreateWorkOrderDialog(garage, onSuccess);
    }

    // =========================================================================
    // 5. Fakturering & Betalning (BillingDialogs)
    // =========================================================================

    public static void showCreateInvoiceDialog(GarageSystem garage, Runnable onSuccess) {
        BillingDialogs.showCreateInvoiceDialog(garage, onSuccess);
    }

    public static void showProcessPaymentDialog(GarageSystem garage, Invoice preselected, Runnable onSuccess) {
        BillingDialogs.showProcessPaymentDialog(garage, preselected, onSuccess);
    }

    // =========================================================================
    // 6. Mekaniker (MechanicDialogs)
    // =========================================================================

    public static void showCreateMechanicDialog(GarageSystem garage, Runnable onSuccess) {
        MechanicDialogs.showCreateMechanicDialog(garage, onSuccess);
    }

    public static void showEditMechanicDialog(GarageSystem garage, Mechanic mechanic, Runnable onSuccess) {
        MechanicDialogs.showEditMechanicDialog(garage, mechanic, onSuccess);
    }

    public static void showDeleteMechanicConfirmation(GarageSystem garage, Mechanic mechanic, Runnable onSuccess) {
        MechanicDialogs.showDeleteMechanicConfirmation(garage, mechanic, onSuccess);
    }

    // =========================================================================
    // 7. Tjänster (ServiceItemDialogs)
    // =========================================================================

    public static void showCreateServiceItemDialog(GarageSystem garage, Runnable onSuccess) {
        ServiceItemDialogs.showCreateServiceItemDialog(garage, onSuccess);
    }

    public static void showEditServiceItemDialog(GarageSystem garage, ServiceItem serviceItem, Runnable onSuccess) {
        ServiceItemDialogs.showEditServiceItemDialog(garage, serviceItem, onSuccess);
    }

    public static void showDeleteServiceItemConfirmation(GarageSystem garage, ServiceItem serviceItem, Runnable onSuccess) {
        ServiceItemDialogs.showDeleteServiceItemConfirmation(garage, serviceItem, onSuccess);
    }

    // =========================================================================
    // 8. Tidsluckor / Arbetsorderdetaljer (SlotDetailsDialog)
    // =========================================================================

    public static void showSlotDetailsDialog(GarageSystem garage,
                                             MechanicSchedule.TimeSlot slot,
                                             PageRouter router,
                                             Runnable onRefresh) {
        SlotDetailsDialog.showSlotDetailsDialog(garage, slot, router, onRefresh);
    }

    public static void showWorkOrderDetailsDialog(GarageSystem garage, int workOrderId,
                                                  PageRouter router, Runnable onRefresh) {
        SlotDetailsDialog.showWorkOrderDetailsDialog(garage, workOrderId, router, onRefresh);
    }
}
