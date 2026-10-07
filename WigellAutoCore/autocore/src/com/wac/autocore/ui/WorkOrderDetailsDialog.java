package com.wac.autocore.ui;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.EntityLookup;
import com.wac.autocore.ui.util.UiFormatters;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import com.wac.autocore.seed.SeedText;

/** Detaljvyn för en arbetsorder: upplysningar och tjänsterna med sitt läge. */
final class WorkOrderDetailsDialog {

    private WorkOrderDetailsDialog() {}

    static void show(GarageSystem garage, WorkOrder workOrder) {
        if (workOrder == null) return;

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.workorder.details_title") + " #" + workOrder.getId());
        dialog.setHeaderText(I18n.get("table.col.workorder") + " #" + workOrder.getId()
                + " (" + I18n.get("table.col.booking") + " #" + workOrder.getBookingId() + ")");
        ActionDialogs.styleDialog(dialog);

        VBox content = new VBox(14);
        content.setPadding(new Insets(18, 22, 18, 22));
        content.setPrefWidth(640);

        // Detaljvyn är upplysningar, inte ett formulär. Utan kolumnregler hamnar värdena direkt
        // efter sina etiketter i stället för att tryckas ut till höger.
        GridPane infoGrid = ActionDialogs.createGrid();
        infoGrid.getColumnConstraints().clear();
        infoGrid.add(new Label(I18n.get("table.col.type") + ":"), 0, 0);
        infoGrid.add(new Label(UiFormatters.workOrderTypeWord(workOrder.getType())), 1, 0);

        infoGrid.add(new Label(I18n.get("table.col.status") + ":"), 0, 1);
        Label statusBadge = new Label(UiFormatters.statusWord(workOrder.getStatus()));
        statusBadge.getStyleClass().add("badge");
        String badgeCls = UiFormatters.badgeClass(UiFormatters.statusWord(workOrder.getStatus()));
        if (!badgeCls.isEmpty()) {
            statusBadge.getStyleClass().add(badgeCls);
        }
        infoGrid.add(statusBadge, 1, 1);

        infoGrid.add(new Label(I18n.get("table.col.mechanic") + ":"), 0, 2);
        infoGrid.add(new Label(EntityLookup.mechanicName(garage, workOrder.getMechanicId())), 1, 2);

        infoGrid.add(new Label(I18n.get("table.col.customer") + ":"), 0, 3);
        infoGrid.add(new Label(EntityLookup.workOrderCustomerName(garage, workOrder)), 1, 3);

        infoGrid.add(new Label(I18n.get("table.col.vehicle") + ":"), 0, 4);
        infoGrid.add(new Label(EntityLookup.workOrderVehicleReg(garage, workOrder)), 1, 4);

        infoGrid.add(new Label(I18n.get("table.col.description") + ":"), 0, 5);
        infoGrid.add(new Label(workOrder.getDescription() == null || workOrder.getDescription().trim().isEmpty()
                ? "-"
                : SeedText.resolve(workOrder.getDescription())), 1, 5);

        Invoice inv = EntityLookup.invoiceForWorkOrder(garage, workOrder.getId());
        if (inv != null) {
            infoGrid.add(new Label(I18n.get("table.col.invoice") + ":"), 0, 6);
            infoGrid.add(new Label("#" + inv.getId() + " (" + inv.getInvoiceDate() + " - "
                    + (inv.isPaid() ? I18n.get("status.paid") : I18n.get("status.unpaid")) + ")"), 1, 6);
        }

        Label servicesTitle = new Label(I18n.get("table.col.services"));
        servicesTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label notice = new Label(I18n.get("dialog.workorder.historical_notice"));
        notice.getStyleClass().addAll("srow-sub", "small");

        // Rader av tjänster är en tabell med fyra kolumner, så den får fyra egna kolumnregler.
        GridPane linesGrid = ActionDialogs.createGrid();
        linesGrid.getColumnConstraints().clear();
        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setMinWidth(100);
            column.setPrefWidth(140);
            column.setHgrow(Priority.ALWAYS);
            linesGrid.getColumnConstraints().add(column);
        }
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
                    statusChip.setText(I18n.get("status.completed"));
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
}
