package com.wac.autocore.ui;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.seed.SeedText;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.components.MultiSelectComboBox;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.EntityLookup;
import com.wac.autocore.ui.util.UiFormatters;

import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

final class EditDraftDialog {

    private EditDraftDialog() {}

    static void show(GarageSystem garage, WorkOrder workOrder, Runnable onSuccess) {

        if (workOrder == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.draft.edit_title"));
        dialog.setHeaderText(I18n.get("dialog.draft.edit_header"));
        ActionDialogs.styleDialog(dialog);

        VBox content = new VBox(12);
        content.setPadding(new Insets(18, 22, 18, 22));
        content.setPrefWidth(820);

        // The same size as the booking dialog, the widest in the app.
        dialog.getDialogPane().setPrefWidth(860);
        dialog.getDialogPane().setMinWidth(760);

        ComboBox<Vehicle> vehicleBox = createVehicleBox(garage, workOrder);
        TextArea description = createDescriptionField(workOrder);
        MultiSelectComboBox<ServiceItem> serviceBox = createServiceBox(garage, workOrder);
        MultiSelectComboBox<Mechanic> mechanicBox = createMechanicBox(garage, workOrder);
        Label mechanicHint = createMechanicHint();
        DatePicker datePicker = createDatePicker(workOrder);
        TextArea instructions = createInstructionsField(workOrder);
        TextArea otherComments = createCommentsField(workOrder);

        // The team follows the services only when the user changes them, not when the dialog opens.
        serviceBox.getSelectedItems().addListener((ListChangeListener<ServiceItem>) change ->
                updateMechanics(garage, serviceBox.getSelectedItems(), mechanicBox, mechanicHint));

        mechanicBox.getSelectedItems().addListener((ListChangeListener<Mechanic>) change ->
                updateHint(garage, serviceBox.getSelectedItems(), mechanicBox, mechanicHint));

        GridPane grid = ActionDialogs.createGrid();
        // The grid inherits 480 from the helper, the field column only gets room once the ceiling is raised.
        grid.setPrefWidth(820);
        grid.add(new Label(I18n.get("table.col.vehicle") + ":"), 0, 0);
        grid.add(vehicleBox, 1, 0);
        grid.add(new Label(I18n.get("dialog.draft.description") + ":"), 0, 1);
        grid.add(description, 1, 1);
        grid.add(new Label(I18n.get("table.col.services") + ":"), 0, 2);
        grid.add(serviceBox, 1, 2);
        grid.add(new Label(I18n.get("table.col.mechanic") + ":"), 0, 3);
        grid.add(mechanicBox, 1, 3);
        grid.add(mechanicHint, 1, 4);
        grid.add(new Label(I18n.get("dialog.draft.planned_date") + ":"), 0, 5);
        grid.add(datePicker, 1, 5);
        grid.add(new Label(I18n.get("dialog.draft.customer_instructions") + ":"), 0, 6);
        grid.add(instructions, 1, 6);
        grid.add(new Label(I18n.get("dialog.draft.other_comments") + ":"), 0, 7);
        grid.add(otherComments, 1, 7);

        content.getChildren().add(grid);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.OK).getStyleClass().add("primary-button");

        dialog.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) {
                return;
            }
            Vehicle vehicle = vehicleBox.getValue();
            List<Mechanic> chosen = mechanicBox.getSelectedItems();
            if (vehicle == null || chosen.isEmpty()) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.draft.create_failed"));
                return;
            }

            LinkedHashMap<Integer, List<ServiceItem>> plan = WorkOrderPlan.planWorkOrders(garage, chosen,
                    serviceBox.getSelectedItems());

            // The first mechanic keeps the order that was opened, the others get a new one each.
            boolean first = true;
            for (Map.Entry<Integer, List<ServiceItem>> entry : plan.entrySet()) {

                List<Integer> serviceIds = new ArrayList<Integer>();
                for (ServiceItem service : entry.getValue()) {
                    serviceIds.add(service.getId());
                }

                WorkOrder saved;
                if (first) {
                    saved = saveDraft(garage, workOrder.getId(), vehicle, entry.getKey().intValue(), serviceIds,
                            datePicker, description, instructions, otherComments);
                    first = false;
                } else {
                    WorkOrder created = garage.createDraft(vehicle.getId(), description.getText());
                    if (created == null) {
                        saved = null;
                    } else {
                        saved = saveDraft(garage, created.getId(), vehicle, entry.getKey().intValue(), serviceIds,
                                datePicker, description, instructions, otherComments);
                    }
                }

                if (saved == null) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.draft.create_failed"));
                    return;
                }
            }

            if (onSuccess != null) {
                onSuccess.run();
            }
        });
    }

    /** The vehicle box shows the owner after the registration number, so two identical cars can be told apart. */
    private static ComboBox<Vehicle> createVehicleBox(GarageSystem garage, WorkOrder workOrder) {

        ComboBox<Vehicle> vehicleBox = new ComboBox<Vehicle>();
        vehicleBox.getItems().addAll(garage.getVehicles());
        vehicleBox.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(vehicleBox, Priority.ALWAYS);

        vehicleBox.setConverter(new StringConverter<Vehicle>() {
            @Override
            public String toString(Vehicle v) {
                if (v == null) return "";
                String owner = EntityLookup.customerName(garage, v.getCustomerId());
                return v.getId() + " - " + v.getRegistrationNumber() + " (" + v.getBrand() + " " + v.getModel() + ") · " + owner;
            }

            @Override
            public Vehicle fromString(String text) {
                return null;
            }
        });

        for (Vehicle v : garage.getVehicles()) {
            if (v.getId() == workOrder.getVehicleId()) {
                vehicleBox.setValue(v);
                break;
            }
        }
        return vehicleBox;
    }

    /** The mechanic pre-selected from the order, the box may change it. */
    private static MultiSelectComboBox<Mechanic> createMechanicBox(GarageSystem garage, WorkOrder workOrder) {

        MultiSelectComboBox<Mechanic> mechanicBox = new MultiSelectComboBox<Mechanic>(
                I18n.get("dialog.booking.mechanic_select"),
                m -> m.getName());
        mechanicBox.setItems(garage.getMechanics());
        mechanicBox.setKeyProvider(m -> m.getId());
        mechanicBox.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(mechanicBox, Priority.ALWAYS);

        List<Mechanic> chosen = new ArrayList<Mechanic>();
        for (Mechanic m : garage.getMechanics()) {
            if (m.getId() == workOrder.getMechanicId()) {
                chosen.add(m);
                break;
            }
        }
        mechanicBox.setSelectedItems(chosen);

        return mechanicBox;
    }

    /** The services already on the draft are pre-selected. */
    private static MultiSelectComboBox<ServiceItem> createServiceBox(GarageSystem garage, WorkOrder workOrder) {

        MultiSelectComboBox<ServiceItem> serviceBox = new MultiSelectComboBox<ServiceItem>(
                I18n.get("dialog.booking.service_select"),
                s -> SeedText.resolve(s.getName()),
                s -> UiFormatters.formatMoney(s.getPrice()) + " · " + s.getEstimatedMinutes() + " min");
        serviceBox.setItems(garage.getServiceItems());
        serviceBox.setKeyProvider(s -> s.getId());

        List<ServiceItem> chosenServices = new ArrayList<ServiceItem>();
        if (workOrder.getServiceItemIds() != null) {
            for (ServiceItem s : garage.getServiceItems()) {
                if (workOrder.getServiceItemIds().contains(s.getId())) {
                    chosenServices.add(s);
                }
            }
        }
        serviceBox.setSelectedItems(chosenServices);
        return serviceBox;
    }

    private static TextArea createDescriptionField(WorkOrder workOrder) {

        TextArea description = new TextArea();
        description.setPrefRowCount(3);
        description.setWrapText(true);
        description.setText(workOrder.getDescription() == null ? "" : workOrder.getDescription());
        return description;
    }

    private static DatePicker createDatePicker(WorkOrder workOrder) {

        DatePicker datePicker = new DatePicker();
        datePicker.setMaxWidth(Double.MAX_VALUE);

        if (workOrder.getPlannedDate() != null && !workOrder.getPlannedDate().isEmpty()) {
            datePicker.setValue(LocalDate.parse(workOrder.getPlannedDate()));
        }
        return datePicker;
    }

    private static TextArea createInstructionsField(WorkOrder workOrder) {

        TextArea instructions = new TextArea();
        instructions.setPrefRowCount(2);
        instructions.setWrapText(true);
        instructions.setText(workOrder.getCustomerInstructions() == null ? "" : workOrder.getCustomerInstructions());
        return instructions;
    }

    private static TextArea createCommentsField(WorkOrder workOrder) {

        TextArea otherComments = new TextArea();
        otherComments.setPrefRowCount(2);
        otherComments.setWrapText(true);
        otherComments.setText(workOrder.getOtherComments() == null ? "" : workOrder.getOtherComments());
        return otherComments;
    }

    /** Empty until the services say something about who can perform them. */
    private static Label createMechanicHint() {

        Label mechanicHint = new Label();
        mechanicHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -wac-muted;");
        return mechanicHint;
    }

    /** Fills the mechanic field from the services, the same rule the booking uses. */
    private static void updateMechanics(GarageSystem garage, List<ServiceItem> services,
                                        MultiSelectComboBox<Mechanic> mechanicBox, Label mechanicHint) {
        List<Mechanic> team = garage.getRequiredMechanics(services);

        if (!team.isEmpty() && !mechanicBox.getSelectedItems().equals(team)) {
            mechanicBox.setSelectedItems(team);
        }
        updateHint(garage, services, mechanicBox, mechanicHint);
    }

    /** Writes the text under the field. Never touches the selection, or the listener sets itself off in a loop. */
    private static void updateHint(GarageSystem garage, List<ServiceItem> services,
                                   MultiSelectComboBox<Mechanic> mechanicBox, Label mechanicHint) {
        List<Mechanic> team = garage.getRequiredMechanics(services);

        // The plan reveals how many orders OK creates, and whether anyone ends up with an unpicked mechanic.
        LinkedHashMap<Integer, List<ServiceItem>> plan =
                WorkOrderPlan.planWorkOrders(garage, mechanicBox.getSelectedItems(), services);
        boolean fleraOrdrar = plan.size() > 1;
        for (Integer id : plan.keySet()) {
            boolean vald = false;
            for (Mechanic m : mechanicBox.getSelectedItems()) {
                if (m.getId() == id.intValue()) {
                    vald = true;
                }
            }
            if (!vald) {
                fleraOrdrar = true;
            }
        }

        if (fleraOrdrar) {
            mechanicHint.setText(I18n.get("dialog.draft.split_warning"));
            mechanicHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -wac-muted;");
        } else if (!services.isEmpty() && team.isEmpty()) {
            mechanicHint.setText(I18n.get("dialog.booking.no_mechanic_for_selected_services"));
            mechanicHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #f87171;");
        } else {
            mechanicHint.setText("");
            mechanicHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -wac-muted;");
        }
    }

    /** Saves a draft with one mechanic's services. The same field values for every order. */
    private static WorkOrder saveDraft(GarageSystem garage, int workOrderId, Vehicle vehicle, int mechanicId,
            List<Integer> serviceIds, DatePicker datePicker, TextArea description, TextArea instructions,
            TextArea otherComments) {
        return garage.updateDraft(workOrderId, vehicle.getId(), description.getText(), mechanicId, serviceIds,
                datePicker.getValue() == null ? null : datePicker.getValue().toString(),
                instructions.getText(), otherComments.getText());
    }
}
