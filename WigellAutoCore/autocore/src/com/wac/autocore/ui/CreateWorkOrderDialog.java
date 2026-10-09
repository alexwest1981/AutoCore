package com.wac.autocore.ui;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.UiFormatters;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.wac.autocore.seed.SeedText;

/** The dialog that creates work orders: one order per mechanic from the booking's plan. */
final class CreateWorkOrderDialog {

    private CreateWorkOrderDialog() {}

    static void show(GarageSystem garage, Booking defaultBooking, Runnable onSuccess) {
        // A booking whose services already sit on work orders must not be pickable again. A
        // booking that is only partly split (some services left) should still be fillable.
        List<Booking> bookings = new ArrayList<Booking>();
        for (Booking b : garage.getBookings()) {
            if ("BOOKED".equalsIgnoreCase(b.getStatus()) || "CONFIRMED".equalsIgnoreCase(b.getStatus())) {
                if (WorkOrderPlan.hasServicesLeftForAWorkOrder(garage, b)) {
                    bookings.add(b);
                }
            }
        }

        if (defaultBooking != null && !bookings.contains(defaultBooking)) {
            bookings.add(0, defaultBooking);
        }

        if (bookings.isEmpty()) {
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("overview.empty.bookings"));
            return;
        }

        List<Mechanic> mechanics = garage.getMechanics();
        if (mechanics.isEmpty()) {
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.workorder.create.title"));
        dialog.setHeaderText(I18n.get("dialog.workorder.create.header"));
        ActionDialogs.styleDialog(dialog);

        VBox content = new VBox(14);
        content.setPadding(new Insets(16, 20, 16, 20));
        content.setPrefWidth(640);

        GridPane grid = ActionDialogs.createGrid();
        grid.setPrefWidth(640);

        ComboBox<Booking> bookingBox = new ComboBox<Booking>();
        bookingBox.getItems().addAll(bookings);
        bookingBox.setPromptText(I18n.get("dialog.workorder.booking_select"));
        bookingBox.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(bookingBox, Priority.ALWAYS);
        bookingBox.setConverter(new StringConverter<Booking>() {
            @Override
            public String toString(Booking b) {
                return b == null ? "" : I18n.get("table.col.booking") + " #" + b.getId() + " - " + SeedText.resolve(b.getDescription()) + " (" + b.getDate() + ")";
            }
            @Override
            public Booking fromString(String string) { return null; }
        });

        // The type is the same for all work orders one run creates: one per mechanic, one type.
        // Nothing is picked to begin with, so the field shows its prompt until one is chosen.
        ComboBox<String> typeBox = new ComboBox<String>();
        typeBox.getItems().addAll(WorkOrder.TYPES);
        typeBox.setPromptText(I18n.get("dialog.workorder.type_select"));
        typeBox.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(typeBox, Priority.ALWAYS);
        typeBox.setConverter(new StringConverter<String>() {
            @Override
            public String toString(String type) {
                return type == null ? "" : UiFormatters.workOrderTypeWord(type);
            }
            @Override
            public String fromString(String string) { return null; }
        });

        grid.add(new Label(I18n.get("dialog.workorder.booking_select") + ":"), 0, 0);
        grid.add(bookingBox, 1, 0);
        grid.add(new Label(I18n.get("table.col.type") + ":"), 0, 1);
        grid.add(typeBox, 1, 1);

        Label servicesTitle = new Label(I18n.get("dialog.workorder.plan_title"));
        servicesTitle.setStyle("-fx-font-weight: bold;");

        VBox serviceList = new VBox(8);

        // The key in the plan is the mechanic id, not the object: the mechanic list can come
        // from different calls.
        final Map<Integer, List<ServiceItem>> plan = new java.util.LinkedHashMap<Integer, List<ServiceItem>>();

        java.util.function.Consumer<Booking> syncFromBooking = b -> {
            plan.clear();
            serviceList.getChildren().clear();
            if (b == null) {
                return;
            }

            plan.putAll(WorkOrderPlan.planWorkOrders(garage, b));

            for (Map.Entry<Integer, List<ServiceItem>> entry : plan.entrySet()) {
                Mechanic m = WorkOrderPlan.mechanicById(garage, entry.getKey().intValue());
                StringBuilder names = new StringBuilder();
                int minutes = 0;
                double price = 0.0;
                for (ServiceItem s : entry.getValue()) {
                    if (names.length() > 0) {
                        names.append(", ");
                    }
                    names.append(SeedText.resolve(s.getName()));
                    minutes += s.getEstimatedMinutes();
                    price += s.getPrice();
                }
                Label row = new Label((m != null ? m.getName() + " (" + SeedText.resolve(m.getSpecialization()) + ")" : "?")
                        + ": " + names
                        + "  ·  " + minutes + " min  ·  " + UiFormatters.formatMoney(price));
                serviceList.getChildren().add(row);
            }

            if (serviceList.getChildren().isEmpty()) {
                serviceList.getChildren().add(new Label(I18n.get("dialog.workorder.no_qualified")));
            }
        };

        bookingBox.valueProperty().addListener((obs, oldB, newB) -> syncFromBooking.accept(newB));

        if (defaultBooking != null && bookings.contains(defaultBooking)) {
            bookingBox.getSelectionModel().select(defaultBooking);
        }
        syncFromBooking.accept(bookingBox.getValue());

        ScrollPane scroll = new ScrollPane(serviceList);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(130);

        content.getChildren().addAll(grid, servicesTitle, scroll);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // OK is locked until the chosen booking has something to do and a type is picked — a work
        // order without services, or without a type, cannot be created anyway.
        javafx.beans.property.BooleanProperty bookingReady = new javafx.beans.property.SimpleBooleanProperty();
        bookingReady.bind(javafx.beans.binding.Bindings.createBooleanBinding(
                () -> WorkOrderPlan.hasServices(bookingBox.getValue()) && typeBox.getValue() != null,
                bookingBox.valueProperty(), typeBox.valueProperty()));
        ActionDialogs.requireFilled(dialog, bookingReady);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Booking b = bookingBox.getValue();
                if (b == null) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }
                if (plan.isEmpty()) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"),
                            I18n.get("dialog.workorder.no_qualified"));
                    return;
                }

                // One work order per mechanic, each with its own services.
                int created = 0;
                for (Map.Entry<Integer, List<ServiceItem>> entry : plan.entrySet()) {
                    List<Integer> ids = new ArrayList<Integer>();
                    for (ServiceItem s : entry.getValue()) {
                        ids.add(Integer.valueOf(s.getId()));
                    }
                    if (garage.createWorkOrder(b.getId(), entry.getKey().intValue(), ids, typeBox.getValue()) != null) {
                        created++;
                    }
                }

                if (created == 0) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"),
                            I18n.get("dialog.workorder.create_failed"));
                    return;
                }
                if (created < plan.size()) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"),
                            I18n.get("dialog.workorder.partial_failed")
                                    .replace("{0}", String.valueOf(created))
                                    .replace("{1}", String.valueOf(plan.size())));
                }

                com.wac.autocore.service.MechanicSchedule.getInstance().syncFromDatabase();
                if (onSuccess != null) onSuccess.run();
            }
        });
    }
}
