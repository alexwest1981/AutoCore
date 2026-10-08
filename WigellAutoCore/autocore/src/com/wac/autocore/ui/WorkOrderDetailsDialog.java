package com.wac.autocore.ui;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.components.UiComponents;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.EntityLookup;
import com.wac.autocore.ui.util.UiFormatters;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import com.wac.autocore.seed.SeedText;

import java.util.ArrayList;
import java.util.List;

/** Detaljvyn för en arbetsorder: upplysningar, tjänsterna och de åtgärder som läget tillåter. */
final class WorkOrderDetailsDialog {

    private WorkOrderDetailsDialog() {}

    static void show(GarageSystem garage, WorkOrder workOrder, Runnable onRefresh) {
        if (workOrder == null) return;

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.workorder.details_title") + " #" + workOrder.getId());
        dialog.setHeaderText(I18n.get("table.col.workorder") + " #" + workOrder.getId()
                + " (" + I18n.get("table.col.booking") + " #" + workOrder.getBookingId() + ")");
        ActionDialogs.styleDialog(dialog);

        dialog.getDialogPane().setContent(scrollContent(buildContent(garage, workOrder, dialog)));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        // Utan klass far knappen JavaFX standardstil och forsvinner mot temats accent.
        dialog.getDialogPane().lookupButton(ButtonType.OK).getStyleClass().add("primary-button");

        // Listan bakom dialogen hämtas när den stängs, inte vid varje knapptryck.
        dialog.setOnHidden(e -> {
            if (onRefresh != null) {
                onRefresh.run();
            }
        });

        dialog.showAndWait();
    }

    /** Innehållet byggs om efter varje åtgärd, så statusen och knapparna alltid stämmer. */
    private static VBox buildContent(GarageSystem garage, WorkOrder workOrder, Dialog<ButtonType> dialog) {
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

        // En reklamation visar vilken arbetsorder den gäller.
        int infoRow = 6;
        if (workOrder.getOriginalWorkOrderId() > 0) {
            infoGrid.add(new Label(I18n.get("table.col.reclamation_of") + ":"), 0, infoRow);
            infoGrid.add(new Label("#" + workOrder.getOriginalWorkOrderId()), 1, infoRow);
            infoRow++;
        }

        if (inv != null) {
            infoGrid.add(new Label(I18n.get("table.col.invoice") + ":"), 0, infoRow);
            infoGrid.add(new Label("#" + inv.getId() + " (" + inv.getInvoiceDate() + " - "
                    + (inv.isPaid() ? I18n.get("status.paid") : I18n.get("status.unpaid")) + ")"), 1, infoRow);
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
        // Andra riktningen av kopplingen: vilka reklamationer som pekar på den här ordern.
        Label reclamationsTitle = new Label(I18n.get("dialog.workorder.reclamations_title"));
        reclamationsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        VBox reclamationsBox = new VBox(6);
        List<WorkOrder> reclamations = reclamationsOf(garage, workOrder.getId());
        if (reclamations.isEmpty()) {
            Label none = new Label(I18n.get("dialog.workorder.no_reclamations"));
            none.getStyleClass().addAll("srow-sub", "small");
            reclamationsBox.getChildren().add(none);
        } else {
            for (WorkOrder reclamation : reclamations) {
                String text = reclamation.getDescription() == null || reclamation.getDescription().trim().isEmpty()
                        ? "-"
                        : SeedText.resolve(reclamation.getDescription());
                Label line = new Label("#" + reclamation.getId() + " · "
                        + UiFormatters.statusWord(reclamation.getStatus()) + " · " + text);
                line.setWrapText(true);
                reclamationsBox.getChildren().add(line);
            }
        }

        if ("CREATED".equals(workOrder.getStatus())) {
            }

        if ("CREATED".equals(workOrder.getStatus())) {
            content.getChildren().add(headerRow(garage, workOrder, dialog));
        }
        content.getChildren().addAll(infoGrid, servicesTitle, notice, linesGrid,
                reclamationsTitle, reclamationsBox);
        content.getChildren().add(actionRow(garage, workOrder, dialog));
        return content;
    }

    /** Redigera-knappen högst upp till höger, så att ett utkast kan kompletteras senare. */
    private static HBox headerRow(GarageSystem garage, WorkOrder workOrder, Dialog<ButtonType> dialog) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_RIGHT);

        Button edit = new Button(I18n.get("entity.workorders.action_edit"));
        edit.getStyleClass().add("secondary-button");
        edit.setOnAction(e -> ActionDialogs.showEditDraftDialog(garage, workOrder,
                () -> refreshDialog(garage, workOrder, dialog)));

        row.getChildren().add(edit);
        return row;
    }

    /** Visar bara de åtgärder som statusen tillåter. Avbryt hamnar sist, avskild till höger. */
    private static HBox actionRow(GarageSystem garage, WorkOrder workOrder, Dialog<ButtonType> dialog) {
        String status = workOrder.getStatus();
        boolean multiService = workOrder.getServiceItemIds() != null && workOrder.getServiceItemIds().size() > 1;
        boolean allMarked = workOrder.getCompletedServiceItems() != null
                && workOrder.getCompletedServiceItems().containsAll(workOrder.getServiceItemIds());

        HBox row = new HBox(8);

        if ("CREATED".equals(status)) {
            row.getChildren().add(actionButton("confirm", garage, workOrder, dialog));
        } else if ("CONFIRMED".equals(status)) {
            row.getChildren().add(actionButton("start", garage, workOrder, dialog));
        } else if ("IN_PROGRESS".equals(status)) {
            Button markBtn = UiComponents.secondaryButton(I18n.get("entity.workorders.action_mark_performed"));
            markBtn.setOnAction(e -> ActionDialogs.showMarkPerformedDialog(garage, workOrder,
                    () -> refreshDialog(garage, workOrder, dialog)));
            row.getChildren().add(markBtn);
            if (!multiService || allMarked) {
                row.getChildren().add(actionButton("complete", garage, workOrder, dialog));
            }
        } else if ("COMPLETED".equals(status)) {
            // Ett utfört arbete har en enda åtgärd: att reklameras.
            row.getChildren().add(reclamationButton(garage, workOrder, dialog));
        } else if ("CANCELLED".equals(status)) {
            row.getChildren().add(actionButton("start", garage, workOrder, dialog));
        }

        if ("CREATED".equals(status) || "CONFIRMED".equals(status) || "IN_PROGRESS".equals(status)) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().addAll(spacer, actionButton("cancel", garage, workOrder, dialog));
        }

        if (row.getChildren().isEmpty()) {
            Label emptyLabel = new Label(I18n.get("dialog.workorder.no_actions"));
            emptyLabel.getStyleClass().add("small");
            row.getChildren().add(emptyLabel);
        }

        return row;
    }

    /** Reklamationen skapas från det utförda arbetet och syns i listan så snart dialogen ritas om. */
    private static Button reclamationButton(GarageSystem garage, WorkOrder workOrder, Dialog<ButtonType> dialog) {
        Button button = UiComponents.secondaryButton(I18n.get("entity.workorders.action_reclamation"));
        button.setOnAction(e -> ActionDialogs.showCreateReclamationDialog(garage, workOrder,
                () -> refreshDialog(garage, workOrder, dialog)));
        return button;
    }

    private static Button actionButton(String key, GarageSystem garage, WorkOrder workOrder, Dialog<ButtonType> dialog) {
        Button button = UiComponents.secondaryButton(I18n.get("entity.workorders.action_" + key));
        button.setOnAction(e -> {
            if ("confirm".equals(key)) {
                garage.confirmWorkOrder(workOrder.getId());
                refreshDialog(garage, workOrder, dialog);
            } else if ("start".equals(key)) {
                garage.startWorkOrder(workOrder.getId());
                refreshDialog(garage, workOrder, dialog);
            } else if ("complete".equals(key)) {
                garage.completeWorkOrder(workOrder.getId());
                refreshDialog(garage, workOrder, dialog);
            } else if ("cancel".equals(key)) {
                // Att avbryta går inte att ångra, så det får ett eget ja först.
                Alert alert = ActionDialogs.confirm(I18n.get("dialog.workorder.cancel.title"),
                        I18n.get("dialog.workorder.cancel.header"),
                        I18n.get("dialog.workorder.cancel.confirm", String.valueOf(workOrder.getId())));
                // Frågetecknet som JavaFX lägger dit hör inte till vår stil.
                alert.setGraphic(null);
                ButtonType close = new ButtonType(I18n.get("common.close"), ButtonBar.ButtonData.CANCEL_CLOSE);
                ButtonType cancelOrder = new ButtonType(I18n.get("entity.workorders.action_cancel"),
                        ButtonBar.ButtonData.OK_DONE);
                alert.getButtonTypes().setAll(close, cancelOrder);
                alert.getDialogPane().lookupButton(cancelOrder).getStyleClass().add("danger-button");
                Button closeButton = (Button) alert.getDialogPane().lookupButton(close);
                Button cancelButton = (Button) alert.getDialogPane().lookupButton(cancelOrder);
                alert.getDialogPane().setMinWidth(420);
                alert.setOnShown(event -> {
                    for (Node bar : alert.getDialogPane().lookupAll(".button-bar > .container")) {
                        if (bar instanceof HBox) {
                            HBox row = (HBox) bar;
                            row.setAlignment(Pos.CENTER);
                            for (Node child : row.getChildren()) {
                                if (child instanceof Region && !(child instanceof Button)) {
                                    HBox.setHgrow(child, Priority.NEVER);
                                    ((Region) child).setMinWidth(0);
                                    ((Region) child).setPrefWidth(0);
                                }
                            }
                        }
                    }
                    // Bredden sätts efter att temat lagt på sin stil, annars krymper knappen och texten klipps.
                    closeButton.getStyleClass().add("secondary-button");
                    closeButton.setMinWidth(closeButton.prefWidth(-1));
                    cancelButton.setMinWidth(cancelButton.prefWidth(-1));
                    cancelButton.setPrefWidth(160);
                });

                alert.showAndWait().ifPresent(response -> {
                    if (response == cancelOrder) {
                        garage.cancelWorkOrder(workOrder.getId());
                        refreshDialog(garage, workOrder, dialog);
                    }
                });
            }
        });
        return button;
    }

    /** Hämtar ordern på nytt och ritar om innehållet, så statusen visas direkt. */
    private static void refreshDialog(GarageSystem garage, WorkOrder workOrder, Dialog<ButtonType> dialog) {
        WorkOrder updated = workOrder;
        for (WorkOrder w : garage.getWorkOrders()) {
            if (w.getId() == workOrder.getId()) {
                updated = w;
                break;
            }
        }
        dialog.getDialogPane().setContent(scrollContent(buildContent(garage, updated, dialog)));
        refitWindow(dialog);
    }

    /** Andra riktningen: de reklamationer som pekar på den här ordern. */
    private static List<WorkOrder> reclamationsOf(GarageSystem garage, int workOrderId) {
        List<WorkOrder> found = new ArrayList<WorkOrder>();
        for (WorkOrder order : garage.getWorkOrders()) {
            if (order.getOriginalWorkOrderId() == workOrderId) {
                found.add(order);
            }
        }
        return found;
    }

    /** Innehållet rullar när det är högre än fönstret, så knappraden alltid syns. */
    private static ScrollPane scrollContent(VBox content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
        scroll.setMaxHeight(620);
        return scroll;
    }

    /** Krymper fonstret efter innehållet. Utan det lamnar en kortare order en glipa over knappraden. */
    private static void refitWindow(Dialog<ButtonType> dialog) {
        if (dialog.getDialogPane().getScene() == null || dialog.getDialogPane().getScene().getWindow() == null) {
            return;
        }
        javafx.stage.Window window = dialog.getDialogPane().getScene().getWindow();
        Platform.runLater(() -> {
            if (window.getScene() == null) {
                return;
            }
            window.setHeight(0);
            window.sizeToScene();
        });
    }
}
