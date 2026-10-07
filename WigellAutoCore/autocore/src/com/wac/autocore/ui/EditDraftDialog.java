package com.wac.autocore.ui;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.seed.SeedText;
import com.wac.autocore.ui.components.MultiSelectComboBox;
import com.wac.autocore.ui.util.UiFormatters;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.EntityLookup;
import javafx.geometry.Insets;
import javafx.collections.ListChangeListener;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;


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
        content.setPrefWidth(580);

        GridPane grid = ActionDialogs.createGrid();

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

        ComboBox<Mechanic> mechanicBox = new ComboBox<Mechanic>();
        mechanicBox.getItems().addAll(garage.getMechanics());
        mechanicBox.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(mechanicBox, Priority.ALWAYS);
        mechanicBox.setConverter(new StringConverter<Mechanic>() {
            @Override
            public String toString(Mechanic m) {
                return m == null ? "" : m.getName();
            }

            @Override
            public Mechanic fromString(String text) {
                return null;
            }
        });


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

        Label mechanicHint = new Label();
        mechanicHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -wac-muted;");

        // Orderns mekaniker förvald. Teamet följer tjänsterna först när användaren ändrar dem.
        for (Mechanic m : garage.getMechanics()) {
            if (m.getId() == workOrder.getMechanicId()) {
                mechanicBox.setValue(m);
                break;
            }
        }

        serviceBox.getSelectedItems().addListener((ListChangeListener<ServiceItem>) change ->
                updateMechanics(garage, serviceBox.getSelectedItems(), mechanicBox, mechanicHint));

        DatePicker datePicker = new DatePicker();
        datePicker.setMaxWidth(Double.MAX_VALUE);
        if (workOrder.getPlannedDate() != null && !workOrder.getPlannedDate().isEmpty()) {
            datePicker.setValue(LocalDate.parse(workOrder.getPlannedDate()));
        }

        TextArea instructions = new TextArea();
        instructions.setPrefRowCount(2);
        instructions.setWrapText(true);
        instructions.setText(workOrder.getCustomerInstructions() == null ? "" : workOrder.getCustomerInstructions());

        TextArea otherComments = new TextArea();
        otherComments.setPrefRowCount(2);
        otherComments.setWrapText(true);
        otherComments.setText(workOrder.getOtherComments() == null ? "" : workOrder.getOtherComments());


        TextArea description = new TextArea();
        description.setPrefRowCount(3);
        description.setWrapText(true);
        description.setText(workOrder.getDescription() == null ? "" : workOrder.getDescription());

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

        dialog.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) {
                return;
            }
            Vehicle vehicle = vehicleBox.getValue();
            Mechanic mechanic = mechanicBox.getValue();
            if (vehicle == null || mechanic == null) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.draft.create_failed"));
                return;
            }

            List<Integer> serviceIds = new ArrayList<Integer>();
            for (ServiceItem service : serviceBox.getSelectedItems()) {
                serviceIds.add(service.getId());
            }

            WorkOrder updated = garage.updateDraft(workOrder.getId(), vehicle.getId(), description.getText(),
                    mechanic.getId(), serviceIds,
                    datePicker.getValue() == null ? null : datePicker.getValue().toString(),
                    instructions.getText(), otherComments.getText());

            if (updated == null) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.draft.create_failed"));
                return;
            }
            if (onSuccess != null) {
                onSuccess.run();
            }
        });
    }

    /** Fyller mekanikerfältet utifrån tjänsterna, samma regel som bokningen använder. */
    private static void updateMechanics(GarageSystem garage, List<ServiceItem> services,
            ComboBox<Mechanic> mechanicBox, Label mechanicHint) {
        List<Mechanic> team = garage.getRequiredMechanics(services);
        List<Mechanic> qualified = garage.getQualifiedMechanics(services);

        Mechanic current = mechanicBox.getValue();
        boolean stillQualified = current != null && qualified.contains(current);
        if (!stillQualified && !team.isEmpty()) {
            mechanicBox.setValue(team.get(0));
        }

        if (!services.isEmpty() && team.isEmpty()) {
            mechanicHint.setText(I18n.get("dialog.booking.no_mechanic_for_selected_services"));
            mechanicHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #f87171;");
        } else {
            mechanicHint.setText("");
            mechanicHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -wac-muted;");
        }
    }
}
