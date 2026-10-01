package com.wac.autocore.ui;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.EntityLookup;
import com.wac.autocore.ui.util.UiFormatters;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import com.wac.autocore.seed.SeedText;

/**
 * Modala dialoger för arbetsorderhantering (skapa arbetsorder).
 */
public final class WorkOrderDialogs {

    private WorkOrderDialogs() {}

    public static void showCreateWorkOrderDialog(GarageSystem garage, Runnable onSuccess) {
        showCreateWorkOrderDialog(garage, null, onSuccess);
    }

    public static void showCreateWorkOrderDialog(GarageSystem garage, Booking defaultBooking, Runnable onSuccess) {
        List<Booking> bookings = new ArrayList<Booking>();
        for (Booking b : garage.getBookings()) {
            if ("BOOKED".equalsIgnoreCase(b.getStatus()) || "CONFIRMED".equalsIgnoreCase(b.getStatus())) {
                bookings.add(b);
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

        VBox content = new VBox(12);
        content.setPadding(new Insets(10));

        GridPane grid = ActionDialogs.createGrid();

        ComboBox<Booking> bookingBox = new ComboBox<Booking>();
        bookingBox.getItems().addAll(bookings);
        bookingBox.setConverter(new StringConverter<Booking>() {
            @Override
            public String toString(Booking b) {
                return b == null ? "" : I18n.get("table.col.booking") + " #" + b.getId() + " - " + SeedText.resolve(b.getDescription()) + " (" + b.getDate() + ")";
            }
            @Override
            public Booking fromString(String string) { return null; }
        });

        ComboBox<Mechanic> mechanicBox = new ComboBox<Mechanic>();
        mechanicBox.getItems().addAll(mechanics);
        mechanicBox.setConverter(new StringConverter<Mechanic>() {
            @Override
            public String toString(Mechanic m) {
                return m == null ? "" : m.getName() + " (" + SeedText.resolve(m.getSpecialization()) + ") - " + (m.isAvailable() ? I18n.get("table.col.available") : I18n.get("table.col.unavailable"));
            }
            @Override
            public Mechanic fromString(String string) { return null; }
        });

        grid.add(new Label(I18n.get("dialog.workorder.booking_select") + ":"), 0, 0);
        grid.add(bookingBox, 1, 0);
        grid.add(new Label(I18n.get("dialog.workorder.mechanic_select") + ":"), 0, 1);
        grid.add(mechanicBox, 1, 1);

        Label servicesTitle = new Label(I18n.get("dialog.workorder.services_select"));
        servicesTitle.setStyle("-fx-font-weight: bold;");

        VBox serviceChecks = new VBox(6);
        List<CheckBox> checkList = new ArrayList<CheckBox>();
        for (ServiceItem s : garage.getServiceItems()) {
            CheckBox cb = new CheckBox(SeedText.resolve(s.getName()) + " (" + s.getPrice() + " " + I18n.get("common.currency") + ", " + s.getEstimatedMinutes() + " min)");
            cb.setUserData(s.getId());
            checkList.add(cb);
            serviceChecks.getChildren().add(cb);
        }

        // Automatisk synkning: när bokning väljs förväljs bokningens mekaniker och tjänst
        java.util.function.Consumer<Booking> syncFromBooking = b -> {
            if (b == null) return;

            // 1. Förvälj mekaniker från bokningen
            if (b.getMechanicId() > 0) {
                for (Mechanic m : mechanics) {
                    if (m.getId() == b.getMechanicId()) {
                        mechanicBox.getSelectionModel().select(m);
                        break;
                    }
                }
            } else if (mechanicBox.getValue() == null && !mechanics.isEmpty()) {
                mechanicBox.getSelectionModel().selectFirst();
            }

            // 2. Förvälj tjänst från bokningen
            if (b.getServiceItemId() > 0) {
                boolean matched = false;
                for (CheckBox cb : checkList) {
                    Integer sId = (Integer) cb.getUserData();
                    if (sId != null && sId == b.getServiceItemId()) {
                        cb.setSelected(true);
                        matched = true;
                    } else {
                        cb.setSelected(false);
                    }
                }
                if (!matched && !checkList.isEmpty()) {
                    checkList.get(0).setSelected(true);
                }
            } else if (!checkList.isEmpty() && checkList.stream().noneMatch(CheckBox::isSelected)) {
                checkList.get(0).setSelected(true);
            }
        };

        bookingBox.valueProperty().addListener((obs, oldB, newB) -> syncFromBooking.accept(newB));

        if (defaultBooking != null && bookings.contains(defaultBooking)) {
            bookingBox.getSelectionModel().select(defaultBooking);
        } else {
            bookingBox.getSelectionModel().selectFirst();
        }
        syncFromBooking.accept(bookingBox.getValue());

        ScrollPane scroll = new ScrollPane(serviceChecks);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(130);

        content.getChildren().addAll(grid, servicesTitle, scroll);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Booking b = bookingBox.getValue();
                Mechanic m = mechanicBox.getValue();

                List<Integer> selectedServiceIds = new ArrayList<Integer>();
                for (CheckBox cb : checkList) {
                    if (cb.isSelected()) {
                        selectedServiceIds.add((Integer) cb.getUserData());
                    }
                }

                if (selectedServiceIds.isEmpty()) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }

                int[] ids = new int[selectedServiceIds.size()];
                for (int i = 0; i < ids.length; i++) ids[i] = selectedServiceIds.get(i);

                garage.createWorkOrder(b.getId(), m.getId(), ids);
                com.wac.autocore.service.MechanicSchedule.getInstance().syncFromDatabase();
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showWorkOrderDetailsDialog(GarageSystem garage, WorkOrder workOrder) {
        if (workOrder == null) return;

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.workorder.details_title") + " #" + workOrder.getId());
        dialog.setHeaderText(I18n.get("table.col.workorder") + " #" + workOrder.getId()
                + " (" + I18n.get("table.col.booking") + " #" + workOrder.getBookingId() + ")");
        ActionDialogs.styleDialog(dialog);

        VBox content = new VBox(12);
        content.setPadding(new Insets(14));

        GridPane infoGrid = ActionDialogs.createGrid();
        infoGrid.add(new Label(I18n.get("table.col.status") + ":"), 0, 0);
        Label statusBadge = new Label(UiFormatters.statusWord(workOrder.getStatus()));
        statusBadge.getStyleClass().add("badge");
        String badgeCls = UiFormatters.badgeClass(UiFormatters.statusWord(workOrder.getStatus()));
        if (!badgeCls.isEmpty()) {
            statusBadge.getStyleClass().add(badgeCls);
        }
        infoGrid.add(statusBadge, 1, 0);

        infoGrid.add(new Label(I18n.get("table.col.mechanic") + ":"), 0, 1);
        infoGrid.add(new Label(EntityLookup.mechanicName(garage, workOrder.getMechanicId())), 1, 1);

        infoGrid.add(new Label(I18n.get("table.col.customer") + ":"), 0, 2);
        infoGrid.add(new Label(EntityLookup.workOrderCustomerName(garage, workOrder)), 1, 2);

        infoGrid.add(new Label(I18n.get("table.col.vehicle") + ":"), 0, 3);
        infoGrid.add(new Label(EntityLookup.workOrderVehicleReg(garage, workOrder)), 1, 3);

        Invoice inv = EntityLookup.invoiceForWorkOrder(garage, workOrder.getId());
        if (inv != null) {
            infoGrid.add(new Label(I18n.get("table.col.invoice") + ":"), 0, 4);
            infoGrid.add(new Label("#" + inv.getId() + " (" + inv.getInvoiceDate() + " - "
                    + (inv.isPaid() ? I18n.get("status.paid") : I18n.get("status.unpaid")) + ")"), 1, 4);
        }

        Label servicesTitle = new Label(I18n.get("table.col.services"));
        servicesTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label notice = new Label(I18n.get("dialog.workorder.historical_notice"));
        notice.getStyleClass().addAll("srow-sub", "small");

        GridPane linesGrid = ActionDialogs.createGrid();
        Label h1 = new Label(I18n.get("table.col.service"));
        h1.setStyle("-fx-font-weight: bold;");
        Label h2 = new Label(I18n.get("table.col.price"));
        h2.setStyle("-fx-font-weight: bold;");
        linesGrid.add(h1, 0, 0);
        linesGrid.add(h2, 1, 0);

        int row = 1;
        double sum = 0.0;
        if (workOrder.getServiceItemIds() != null) {
            for (Integer sid : workOrder.getServiceItemIds()) {
                String name = null;
                Double price = null;
                if (inv != null && inv.getLines() != null) {
                    for (InvoiceLine line : inv.getLines()) {
                        if (line.getServiceItemId() == sid) {
                            name = line.getServiceName();
                            price = line.getPrice();
                            break;
                        }
                    }
                }
                if (name == null) {
                    for (ServiceItem s : garage.getServiceItems()) {
                        if (s.getId() == sid) {
                            name = s.getName();
                            if (price == null) price = s.getPrice();
                            break;
                        }
                    }
                }
                if (name == null) name = "Service #" + sid;
                if (price == null) price = 0.0;
                sum += price;

                linesGrid.add(new Label(SeedText.resolve(name)), 0, row);
                linesGrid.add(new Label(UiFormatters.formatMoney(price)), 1, row);
                row++;
            }
        }

        Label totalLabel = new Label(I18n.get("table.col.total") + ":");
        totalLabel.setStyle("-fx-font-weight: bold;");
        Label totalVal = new Label(UiFormatters.formatMoney(inv != null ? inv.getAmount() : sum));
        totalVal.setStyle("-fx-font-weight: bold;");
        linesGrid.add(totalLabel, 0, row);
        linesGrid.add(totalVal, 1, row);

        content.getChildren().addAll(infoGrid, servicesTitle, notice, linesGrid);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
    }
}
