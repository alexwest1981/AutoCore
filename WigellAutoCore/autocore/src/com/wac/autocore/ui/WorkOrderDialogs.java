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
import javafx.scene.layout.Priority;
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
        // En bokning vars tjänster redan ligger på arbetsordrar ska inte gå att välja igen. En
        // bokning som bara är delvis uppdelad (några tjänster kvar) ska däremot gå att fylla på.
        List<Booking> bookings = new ArrayList<Booking>();
        for (Booking b : garage.getBookings()) {
            if ("BOOKED".equalsIgnoreCase(b.getStatus()) || "CONFIRMED".equalsIgnoreCase(b.getStatus())) {
                if (hasServicesLeftForAWorkOrder(garage, b)) {
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
        javafx.scene.layout.ColumnConstraints col0 = new javafx.scene.layout.ColumnConstraints();
        col0.setMinWidth(140);
        col0.setPrefWidth(150);
        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);

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

        // Planen: en arbetsorder per mekaniker. Vilka tjänster som hamnar hos vem följer av
        // behörigheten — bokningens mekaniker används när hen är behörig, annars den som är det.
        // Nyckeln är mekaniker-id, inte objektet: mekanikerlistan kan komma från olika anrop.
        final java.util.LinkedHashMap<Integer, List<ServiceItem>> plan =
                new java.util.LinkedHashMap<Integer, List<ServiceItem>>();

        java.util.function.Consumer<Booking> syncFromBooking = b -> {
            plan.clear();
            serviceList.getChildren().clear();
            if (b == null) {
                return;
            }

            plan.putAll(planWorkOrders(garage, b));

            for (java.util.Map.Entry<Integer, List<ServiceItem>> entry : plan.entrySet()) {
                Mechanic m = mechanicById(garage, entry.getKey().intValue());
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
        bookingHasServices.set(hasServices(bookingBox.getValue()));
        bookingBox.valueProperty().addListener((obs, oldB, newB) -> bookingHasServices.set(hasServices(newB)));
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
                for (java.util.Map.Entry<Integer, List<ServiceItem>> entry : plan.entrySet()) {
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

    /** Sant om bokningen har minst en tjänst som ingen arbetsorder tagit hand om. */
    private static boolean hasServicesLeftForAWorkOrder(GarageSystem garage, Booking booking) {
        for (Integer serviceItemId : booking.getServiceItemIds()) {
            boolean claimed = false;
            for (WorkOrder order : garage.getWorkOrders()) {
                if (order.getBookingId() == booking.getId()
                        && order.getServiceItemIds().contains(serviceItemId)) {
                    claimed = true;
                    break;
                }
            }
            if (!claimed) {
                return true;
            }
        }
        return false;
    }

    /**
     * Planen för hur bokningens tjänster fördelas: en post per mekaniker, med den mekanikerns
     * tjänster. Bokningens mekaniker används när hen är behörig, annars den första behöriga — så
     * samma person får så många av tjänsterna som möjligt. Nyckeln är mekaniker-id, inte objektet,
     * eftersom mekanikerlistan kan komma från olika anrop.
     */
    public static java.util.LinkedHashMap<Integer, List<ServiceItem>> planWorkOrders(
            GarageSystem garage, Booking booking) {

        java.util.LinkedHashMap<Integer, List<ServiceItem>> plan =
                new java.util.LinkedHashMap<Integer, List<ServiceItem>>();
        if (booking == null) {
            return plan;
        }

        Mechanic booked = mechanicById(garage, booking.getMechanicId());
        for (ServiceItem service : booking.getServiceItems()) {
            Mechanic who = mechanicForService(garage, booked, service);
            if (who == null) {
                continue;
            }
            List<ServiceItem> mine = plan.get(Integer.valueOf(who.getId()));
            if (mine == null) {
                mine = new ArrayList<ServiceItem>();
                plan.put(Integer.valueOf(who.getId()), mine);
            }
            mine.add(service);
        }
        return plan;
    }

    /** Mekanikern med angivet id, eller null. */
    private static Mechanic mechanicById(GarageSystem garage, int mechanicId) {
        for (Mechanic m : garage.getMechanics()) {
            if (m.getId() == mechanicId) {
                return m;
            }
        }
        return null;
    }

    /**
     * Mekanikern som ska utföra tjänsten: bokningens mekaniker om hen är behörig, annars den första
     * behöriga. Ordningen är bokningens, så samma person får så många av tjänsterna som möjligt.
     */
    private static Mechanic mechanicForService(GarageSystem garage, Mechanic booked, ServiceItem service) {
        List<Mechanic> qualified = garage.getQualifiedMechanics(service);
        if (booked != null && containsId(qualified, booked.getId())) {
            return booked;
        }
        return qualified.isEmpty() ? null : qualified.get(0);
    }

    private static boolean containsId(List<Mechanic> mechanics, int mechanicId) {
        for (Mechanic m : mechanics) {
            if (m.getId() == mechanicId) {
                return true;
            }
        }
        return false;
    }

    public static void showWorkOrderDetailsDialog(GarageSystem garage, WorkOrder workOrder) {
        if (workOrder == null) return;

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.workorder.details_title") + " #" + workOrder.getId());
        dialog.setHeaderText(I18n.get("table.col.workorder") + " #" + workOrder.getId()
                + " (" + I18n.get("table.col.booking") + " #" + workOrder.getBookingId() + ")");
        ActionDialogs.styleDialog(dialog);

        VBox content = new VBox(14);
        content.setPadding(new Insets(18, 22, 18, 22));
        content.setPrefWidth(640);

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
        Label hTime = new Label(I18n.get("table.col.estimated_time"));
        hTime.setStyle("-fx-font-weight: bold;");
        Label h2 = new Label(I18n.get("table.col.price"));
        h2.setStyle("-fx-font-weight: bold;");
        Label hStatus = new Label(I18n.get("table.col.status"));
        hStatus.setStyle("-fx-font-weight: bold;");

        linesGrid.add(h1, 0, 0);
        linesGrid.add(hTime, 1, 0);
        linesGrid.add(h2, 2, 0);
        linesGrid.add(hStatus, 3, 0);

        int row = 1;
        double sum = 0.0;
        int totalMin = 0;
        int completedCount = 0;
        int totalCount = (workOrder.getServiceItemIds() != null) ? workOrder.getServiceItemIds().size() : 0;

        if (workOrder.getServiceItemIds() != null) {
            for (Integer sid : workOrder.getServiceItemIds()) {
                String name = null;
                Double price = null;
                int estMinutes = 0;

                for (ServiceItem s : garage.getServiceItems()) {
                    if (s.getId() == sid.intValue()) {
                        name = s.getName();
                        estMinutes = s.getEstimatedMinutes();
                        price = s.getPrice();
                        break;
                    }
                }

                Double frozenPrice = workOrder.getCompletedServicePrice(sid.intValue());
                if (frozenPrice != null) {
                    price = frozenPrice;
                } else if (inv != null && inv.getLines() != null) {
                    for (InvoiceLine line : inv.getLines()) {
                        if (line.getServiceItemId() == sid.intValue()) {
                            name = line.getServiceName();
                            price = line.getPrice();
                            break;
                        }
                    }
                }

                if (name == null) name = "Service #" + sid;
                if (price == null) price = 0.0;
                sum += price;
                totalMin += estMinutes;

                boolean done = workOrder.getCompletedServiceItems() != null
                        && workOrder.getCompletedServiceItems().contains(sid);
                if (done) {
                    completedCount++;
                }

                Label statusChip = new Label();
                statusChip.getStyleClass().add("badge");
                if (done) {
                    statusChip.setText("✔ " + I18n.get("status.completed"));
                    statusChip.getStyleClass().add("green");
                } else if ("COMPLETED".equals(workOrder.getStatus())) {
                    statusChip.setText(I18n.get("status.cancelled"));
                    statusChip.getStyleClass().add("grey");
                } else {
                    statusChip.setText(I18n.get("status.to_be_performed"));
                    statusChip.getStyleClass().add("yellow");
                }

                linesGrid.add(new Label(SeedText.resolve(name)), 0, row);
                linesGrid.add(new Label(estMinutes + " min"), 1, row);
                linesGrid.add(new Label(UiFormatters.formatMoney(price)), 2, row);
                linesGrid.add(statusChip, 3, row);
                row++;
            }
        }

        Label totalLabel = new Label(I18n.get("table.col.total") + ":");
        totalLabel.setStyle("-fx-font-weight: bold;");
        Label totalTimeVal = new Label(totalMin + " min");
        totalTimeVal.setStyle("-fx-font-weight: bold;");
        Label totalVal = new Label(UiFormatters.formatMoney(inv != null ? inv.getAmount() : sum));
        totalVal.setStyle("-fx-font-weight: bold;");
        Label totalStatusVal = new Label(completedCount + "/" + totalCount + " " + I18n.get("status.completed").toLowerCase());
        totalStatusVal.setStyle("-fx-font-weight: bold;");

        linesGrid.add(totalLabel, 0, row);
        linesGrid.add(totalTimeVal, 1, row);
        linesGrid.add(totalVal, 2, row);
        linesGrid.add(totalStatusVal, 3, row);

        content.getChildren().addAll(infoGrid, servicesTitle, notice, linesGrid);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
    }

    /**
     * SCRUM-160 (D2) och SCRUM-158 (C3): markerar vilka arbeten på arbetsordern som är utförda.
     * Priset som gäller i det ögonblicket frysas på raden.
     */
    public static void showMarkPerformedDialog(GarageSystem garage, WorkOrder workOrder, Runnable onSuccess) {
        if (workOrder == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.workorder.mark_performed_title") + " #" + workOrder.getId());
        dialog.setHeaderText(I18n.get("dialog.workorder.mark_performed_header"));
        ActionDialogs.styleDialog(dialog);

        VBox content = new VBox(12);
        content.setPadding(new Insets(18, 22, 18, 22));
        content.setPrefWidth(580);

        Label info = new Label(I18n.get("dialog.workorder.mark_performed_desc"));
        info.setWrapText(true);

        VBox serviceBox = new VBox(6);
        List<CheckBox> boxes = new ArrayList<CheckBox>();
        for (Integer serviceItemId : workOrder.getServiceItemIds()) {
            String name = "Service #" + serviceItemId;
            double price = 0.0;
            for (ServiceItem s : garage.getServiceItems()) {
                if (s.getId() == serviceItemId.intValue()) {
                    name = SeedText.resolve(s.getName());
                    price = s.getPrice();
                    break;
                }
            }
            CheckBox box = new CheckBox(name + " (" + UiFormatters.formatMoney(price) + ")");

            boolean alreadyDone = workOrder.getCompletedServiceItems().contains(serviceItemId);
            Double frozen = workOrder.getCompletedServicePrice(serviceItemId.intValue());
            if (alreadyDone && frozen != null) {
                box.setText(name + " (" + UiFormatters.formatMoney(frozen.doubleValue()) + ")");
            }

            box.setUserData(serviceItemId);
            box.setSelected(alreadyDone);
            box.setDisable(alreadyDone);
            boxes.add(box);
            serviceBox.getChildren().add(box);
        }

        content.getChildren().addAll(info, serviceBox);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                List<Integer> selected = new ArrayList<Integer>();
                for (CheckBox box : boxes) {
                    if (box.isSelected()) {
                        selected.add((Integer) box.getUserData());
                    }
                }
                if (selected.isEmpty()) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }

                int[] ids = new int[selected.size()];
                for (int i = 0; i < ids.length; i++) {
                    ids[i] = selected.get(i);
                }

                garage.markServicesAsCompleted(workOrder.getId(), ids);
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }
        });
    }

    /** En bokning utan tjänster kan inte bli en arbetsorder. */
    private static boolean hasServices(Booking booking) {
        return booking != null && !booking.getServiceItemIds().isEmpty();
    }
}
