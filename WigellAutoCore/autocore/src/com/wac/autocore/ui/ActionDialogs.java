package com.wac.autocore.ui;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.TimeSlot;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.navigation.PageRouter;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Opens the right dialog for each entity. */
public final class ActionDialogs {

    private ActionDialogs() {}

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
        grid.setPrefWidth(480);
        // The label column widens to its longest text, otherwise long labels get
        // clipped. Only the field column grows, so the fields take the rest without
        // becoming wider than they need to be.
        if (grid.getColumnConstraints().isEmpty()) {
            javafx.scene.layout.ColumnConstraints labelColumn = new javafx.scene.layout.ColumnConstraints();
            javafx.scene.layout.ColumnConstraints fieldColumn = new javafx.scene.layout.ColumnConstraints();
            fieldColumn.setMinWidth(240);
            fieldColumn.setPrefWidth(300);
            fieldColumn.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().addAll(labelColumn, fieldColumn);
        }
        return grid;
    }

    /** Locks the OK button until every field has a value. */
    /** A text field with its prompt, and the value filled in when the form edits something. */
    public static TextField field(String promptKey, String value) {
        TextField field = new TextField();
        field.setPromptText(com.wac.autocore.ui.i18n.I18n.get(promptKey));
        if (value != null) {
            field.setText(value);
        }
        return field;
    }

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

    /** Locks the OK button until a caller-supplied condition is true. */
    public static void requireFilled(Dialog<?> dialog, ObservableBooleanValue filled) {
        Node ok = dialog.getDialogPane().lookupButton(ButtonType.OK);
        if (ok != null) {
            ok.disableProperty().bind(Bindings.not(filled));
        }
    }

    /** The value that decides whether the field is filled in, or null for nodes
     *  that cannot be filled. */
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

    /** Confirmation box with line breaks, because an Alert's text line clips. */
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

    /** Grows the dialog when the content needs more room. Upwards only. */
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

    public static void showCreateCustomerDialog(GarageSystem garage, Runnable onSuccess) {
        CustomerDialogs.showCreateCustomerDialog(garage, onSuccess);
    }

    public static void showEditCustomerDialog(GarageSystem garage, Customer customer, Runnable onSuccess) {
        CustomerDialogs.showEditCustomerDialog(garage, customer, onSuccess);
    }

    public static void showDeleteCustomerConfirmation(GarageSystem garage, Customer customer, Runnable onSuccess) {
        CustomerDialogs.showDeleteCustomerConfirmation(garage, customer, onSuccess);
    }

    public static void showCreateVehicleDialog(GarageSystem garage, Runnable onSuccess) {
        VehicleDialogs.showCreateVehicleDialog(garage, onSuccess);
    }

    public static void showEditVehicleDialog(GarageSystem garage, Vehicle vehicle, Runnable onSuccess) {
        VehicleDialogs.showEditVehicleDialog(garage, vehicle, onSuccess);
    }

    public static void showDeleteVehicleConfirmation(GarageSystem garage, Vehicle vehicle, Runnable onSuccess) {
        VehicleDialogs.showDeleteVehicleConfirmation(garage, vehicle, onSuccess);
    }

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

    public static void showCreateWorkOrderDialog(GarageSystem garage, Runnable onSuccess) {
        WorkOrderDialogs.showCreateWorkOrderDialog(garage, onSuccess);
    }

    public static void showCreateWorkOrderDialog(GarageSystem garage, com.wac.autocore.model.Booking defaultBooking, Runnable onSuccess) {
        WorkOrderDialogs.showCreateWorkOrderDialog(garage, defaultBooking, onSuccess);
    }

    public static void showCreateDraftDialog(GarageSystem garage, Runnable onSuccess) {
        CreateDraftDialog.show(garage, onSuccess);
    }

    public static void showEditDraftDialog(GarageSystem garage, WorkOrder workOrder, Runnable onSuccess) {
        EditDraftDialog.show(garage, workOrder, onSuccess);
    }

    public static void showCreateDropInWorkOrderDialog(GarageSystem garage, Runnable onSuccess) {
        WorkOrderDialogs.showCreateDropInWorkOrderDialog(garage, onSuccess);
    }

    public static void showWorkOrderDetailsDialog(GarageSystem garage, WorkOrder workOrder, Runnable onRefresh) {
        WorkOrderDialogs.showWorkOrderDetailsDialog(garage, workOrder, onRefresh);
    }

    public static void showMarkPerformedDialog(GarageSystem garage, WorkOrder workOrder, Runnable onSuccess) {
        WorkOrderDialogs.showMarkPerformedDialog(garage, workOrder, onSuccess);
    }

    public static void showCreateReclamationDialog(GarageSystem garage, WorkOrder workOrder, Runnable onSuccess) {
        WorkOrderDialogs.showCreateReclamationDialog(garage, workOrder, onSuccess);
    }

    public static void showCreateInvoiceDialog(GarageSystem garage, Runnable onSuccess) {
        BillingDialogs.showCreateInvoiceDialog(garage, onSuccess);
    }

    public static void showCreateInvoiceDialog(GarageSystem garage, WorkOrder preselected, Runnable onSuccess) {
        BillingDialogs.showCreateInvoiceDialog(garage, preselected, onSuccess);
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

    public static void showCreateMechanicDialog(GarageSystem garage, Runnable onSuccess) {
        MechanicDialogs.showCreateMechanicDialog(garage, onSuccess);
    }

    public static void showEditMechanicDialog(GarageSystem garage, Mechanic mechanic, Runnable onSuccess) {
        MechanicDialogs.showEditMechanicDialog(garage, mechanic, onSuccess);
    }

    public static void showDeleteMechanicConfirmation(GarageSystem garage, Mechanic mechanic, Runnable onSuccess) {
        MechanicDialogs.showDeleteMechanicConfirmation(garage, mechanic, onSuccess);
    }

    public static void showCreateServiceItemDialog(GarageSystem garage, Runnable onSuccess) {
        ServiceItemDialogs.showCreateServiceItemDialog(garage, onSuccess);
    }

    public static void showEditServiceItemDialog(GarageSystem garage, ServiceItem serviceItem, Runnable onSuccess) {
        ServiceItemDialogs.showEditServiceItemDialog(garage, serviceItem, onSuccess);
    }

    public static void showDeleteServiceItemConfirmation(GarageSystem garage, ServiceItem serviceItem, Runnable onSuccess) {
        ServiceItemDialogs.showDeleteServiceItemConfirmation(garage, serviceItem, onSuccess);
    }

    public static void showSlotDetailsDialog(GarageSystem garage,
                                             TimeSlot slot,
                                             PageRouter router,
                                             Runnable onRefresh) {
        SlotDetailsDialog.showSlotDetailsDialog(garage, slot, router, onRefresh);
    }

    public static void showWorkOrderDetailsDialog(GarageSystem garage, int workOrderId,
                                                  PageRouter router, Runnable onRefresh) {
        SlotDetailsDialog.showWorkOrderDetailsDialog(garage, workOrderId, router, onRefresh);
    }
}
