package com.wac.autocore.ui;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
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

/** Dialogen som skapar arbetsordrar: en order per mekaniker ur bokningens plan. */
final class CreateWorkOrderDialog {

    private CreateWorkOrderDialog() {}

    static void show(GarageSystem garage, Booking defaultBooking, Runnable onSuccess) {
        // En bokning vars tjänster redan ligger på arbetsordrar ska inte gå att välja igen. En
        // bokning som bara är delvis uppdelad (några tjänster kvar) ska däremot gå att fylla på.
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

        grid.add(new Label(I18n.get("dialog.workorder.booking_select") + ":"), 0, 0);
        grid.add(bookingBox, 1, 0);

        Label servicesTitle = new Label(I18n.get("dialog.workorder.plan_title"));
        servicesTitle.setStyle("-fx-font-weight: bold;");

        VBox serviceList = new VBox(8);

        // Nyckeln i planen är mekaniker-id, inte objektet: mekanikerlistan kan komma från
        // olika anrop.
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
        } else {
            bookingBox.getSelectionModel().selectFirst();
        }
        syncFromBooking.accept(bookingBox.getValue());

        ScrollPane scroll = new ScrollPane(serviceList);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(130);

        content.getChildren().addAll(grid, servicesTitle, scroll);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // OK är låst tills den valda bokningen har något att utföra — en arbetsorder utan tjänster
        // går ändå inte att skapa.
        javafx.beans.property.BooleanProperty bookingHasServices = new javafx.beans.property.SimpleBooleanProperty();
        bookingHasServices.set(WorkOrderPlan.hasServices(bookingBox.getValue()));
        bookingBox.valueProperty().addListener((obs, oldB, newB) -> bookingHasServices.set(WorkOrderPlan.hasServices(newB)));
        ActionDialogs.requireFilled(dialog, bookingHasServices);

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

                // En arbetsorder per mekaniker, var och en med sina egna tjänster.
                int created = 0;
                for (Map.Entry<Integer, List<ServiceItem>> entry : plan.entrySet()) {
                    List<Integer> ids = new ArrayList<Integer>();
                    for (ServiceItem s : entry.getValue()) {
                        ids.add(Integer.valueOf(s.getId()));
                    }
                    if (garage.createWorkOrder(b.getId(), entry.getKey().intValue(), ids) != null) {
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
