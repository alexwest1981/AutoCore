package com.wac.autocore.ui;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.MechanicSchedule;
import com.wac.autocore.ui.navigation.PageRouter;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.GridPane;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    public static void styleDialog(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        if (!pane.getStyleClass().contains("root")) {
            pane.getStyleClass().add("root");
        }
        dialog.setResizable(true);
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

    public static GridPane createGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPadding(new Insets(18, 22, 18, 22));
        grid.setPrefWidth(580);
        return grid;
    }

    /**
     * Låser OK-knappen tills varje angivet fält har ett värde, så ett halvfyllt formulär inte går
     * att skicka och inget behöver skrivas om efteråt.
     *
     * Textfält räknas som ifyllda först när de innehåller något annat än blanksteg, rullistor och
     * datumväljare när ett val är gjort. Fält som är frivilliga lämnas helt enkelt utanför anropet.
     *
     * Låsningen ersätter inte kontrollen som körs när man väl trycker OK — den ligger kvar som
     * sista vakt — den bara sparar irritationen av att fylla i allt en gång till.
     */
    public static void requireFilled(Dialog<?> dialog, Node... fields) {
        final List<Node> required = new ArrayList<Node>();
        List<Observable> sources = new ArrayList<Observable>();
        for (Node field : fields) {
            Observable source = valueSourceOf(field);
            if (source != null) {
                required.add(field);
                sources.add(source);
            }
        }
        requireFilled(dialog, Bindings.createBooleanBinding(
                () -> areAllFilled(required),
                sources.toArray(new Observable[sources.size()])));
    }

    /**
     * Låser OK-knappen tills ett eget villkor är sant. Används när kravet inte går att uttrycka som
     * "fältet är ifyllt", t.ex. att en bokning måste innehålla minst en tjänst.
     */
    public static void requireFilled(Dialog<?> dialog, ObservableBooleanValue filled) {
        Node ok = dialog.getDialogPane().lookupButton(ButtonType.OK);
        if (ok != null) {
            ok.disableProperty().bind(Bindings.not(filled));
        }
    }

    /** Värdet som avgör om fältet är ifyllt, eller null för noder som inte går att fylla i. */
    private static Observable valueSourceOf(Node field) {
        if (field instanceof TextInputControl) {
            return ((TextInputControl) field).textProperty();
        }
        if (field instanceof ComboBoxBase) {
            return ((ComboBoxBase<?>) field).valueProperty();
        }
        return null;
    }

    private static boolean areAllFilled(List<Node> fields) {
        for (Node field : fields) {
            if (isBlank(field)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBlank(Node field) {
        if (field instanceof TextInputControl) {
            String text = ((TextInputControl) field).getText();
            return text == null || text.trim().isEmpty();
        }
        if (field instanceof ComboBoxBase) {
            return ((ComboBoxBase<?>) field).getValue() == null;
        }
        return false;
    }

    /**
     * Bekräftelseruta där texten radbryts i stället för att klippas av. Alertens egen textrad bryter
     * inte, så en mening med ett namn i blev avklippt. Texten hålls inom en rimlig bredd så rutan
     * inte växer över skärmen.
     */
    public static Alert confirm(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(420);
        alert.getDialogPane().setContent(messageLabel);
        styleDialog(alert);
        return alert;
    }

    /**
     * Höjer dialogen när innehållet behöver mer plats (t.ex. fler tjänster i en bokning).
     * Bara uppåt: en storlek användaren själv dragit fram behålls.
     */
    public static void growToFitContent(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        if (pane.getScene() == null || pane.getScene().getWindow() == null) {
            return;
        }
        javafx.stage.Window window = pane.getScene().getWindow();
        double needed = pane.prefHeight(pane.getWidth());
        if (window instanceof javafx.stage.Stage && needed > window.getHeight()) {
            ((javafx.stage.Stage) window).setHeight(needed);
        }
    }

    public static void showError(String title, String message) {
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

    public static void showCreateWorkOrderDialog(GarageSystem garage, com.wac.autocore.model.Booking defaultBooking, Runnable onSuccess) {
        WorkOrderDialogs.showCreateWorkOrderDialog(garage, defaultBooking, onSuccess);
    }

    public static void showWorkOrderDetailsDialog(GarageSystem garage, WorkOrder workOrder) {
        WorkOrderDialogs.showWorkOrderDetailsDialog(garage, workOrder);
    }

    public static void showMarkPerformedDialog(GarageSystem garage, WorkOrder workOrder, Runnable onSuccess) {
        WorkOrderDialogs.showMarkPerformedDialog(garage, workOrder, onSuccess);
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

    public static void showInvoiceLinesDialog(Invoice invoice) {
        BillingDialogs.showInvoiceLinesDialog(invoice);
    }

    public static void showInvoiceDocumentDialog(GarageSystem garage, Invoice invoice) {
        BillingDialogs.showInvoiceDocumentDialog(garage, invoice);
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
