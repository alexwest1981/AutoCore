package com.wac.autocore.ui;

import com.wac.autocore.exception.AppException;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.security.Operation;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.components.UiComponents;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.Actions;
import com.wac.autocore.ui.util.EntityLookup;
import com.wac.autocore.ui.util.UiFormatters;
import com.wac.autocore.model.WorkOrderStatus;

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

/** The details view for a work order: the facts, the services and the actions the status allows. */
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
        // Without the class the button gets JavaFX's default style and disappears against the theme's accent.
        dialog.getDialogPane().lookupButton(ButtonType.OK).getStyleClass().add("primary-button");

        // The list behind the dialog is fetched when it closes, not on every button press.
        dialog.setOnHidden(e -> {
            if (onRefresh != null) {
                onRefresh.run();
            }
        });

        dialog.showAndWait();
    }

    /** The content is rebuilt after every action, so the status and the buttons always match. */
    private static VBox buildContent(GarageSystem garage, WorkOrder workOrder, Dialog<ButtonType> dialog) {
        VBox content = new VBox(14);
        content.setPadding(new Insets(18, 22, 18, 22));
        content.setPrefWidth(640);

        // The details view is information, not a form. Without column rules the values land
        // right after their labels instead of being pushed out to the right.
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

        // A reclamation shows which work order it is for.
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

        // Rows of services are a table with four columns, so it gets four column rules of its own.
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
        int sum = 0;
        int totalMin = 0;
        int completedCount = 0;
        int totalCount = (workOrder.getServiceItemIds() != null) ? workOrder.getServiceItemIds().size() : 0;

        if (workOrder.getServiceItemIds() != null) {
            for (Integer sid : workOrder.getServiceItemIds()) {
                String name = null;
                Integer price = null;
                int estMinutes = 0;

                ServiceItem service = garage.findServiceItem(sid.intValue());
                if (service != null) {
                    name = service.getName();
                    estMinutes = service.getEstimatedMinutes();
                    price = service.getPrice();
                }

                Integer frozenPrice = workOrder.getCompletedServicePrice(sid.intValue());
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
                if (price == null) price = 0;
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
                } else if (workOrder.getStatus() == WorkOrderStatus.COMPLETED) {
                    // The service was never performed, but the order is closed.
                    statusChip.setText(I18n.get("status.not_performed"));
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
        // The other direction of the link: which reclamations point at this order.
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

        if (workOrder.getStatus() == WorkOrderStatus.CREATED
                && Actions.allowed(garage, Operation.UPDATE_WORK_ORDER)) {
            content.getChildren().add(headerRow(garage, workOrder, dialog));
        }
        content.getChildren().addAll(infoGrid, servicesTitle, notice, linesGrid,
                reclamationsTitle, reclamationsBox);
        content.getChildren().add(actionRow(garage, workOrder, dialog));
        return content;
    }

    /** The edit button in the top right, so a draft can be completed later. */
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

    /** Shows only the actions the status allows. Cancel comes last, set apart to the right. */
    private static HBox actionRow(GarageSystem garage, WorkOrder workOrder, Dialog<ButtonType> dialog) {
        WorkOrderStatus status = workOrder.getStatus();
        boolean multiService = workOrder.getServiceItemIds() != null && workOrder.getServiceItemIds().size() > 1;
        boolean allMarked = workOrder.getCompletedServiceItems() != null
                && workOrder.getCompletedServiceItems().containsAll(workOrder.getServiceItemIds());

        HBox row = new HBox(8);

        // Each button asks the order whether the change is allowed, the same table the service checks.
        // It also asks the role: a read-only account opens this dialog to read it, and is offered no
        // action at all.
        if (workOrder.canChangeTo(WorkOrderStatus.CONFIRMED)) {
            addAction(row, actionButton("confirm", garage, workOrder, dialog), garage,
                    Operation.CHANGE_WORK_ORDER_STATUS);
        }

        if (workOrder.canChangeTo(WorkOrderStatus.IN_PROGRESS)) {
            addAction(row, actionButton("start", garage, workOrder, dialog), garage,
                    Operation.CHANGE_WORK_ORDER_STATUS);
        }

        // Marking services is work inside the status, not a change of status.
        if (status == WorkOrderStatus.IN_PROGRESS) {
            Button markBtn = UiComponents.secondaryButton(I18n.get("entity.workorders.action_mark_performed"));
            markBtn.setOnAction(e -> ActionDialogs.showMarkPerformedDialog(garage, workOrder,
                    () -> refreshDialog(garage, workOrder, dialog)));
            addAction(row, markBtn, garage, Operation.UPDATE_WORK_ORDER);
        }

        if (workOrder.canChangeTo(WorkOrderStatus.COMPLETED) && (!multiService || allMarked)) {
            addAction(row, actionButton("complete", garage, workOrder, dialog), garage,
                    Operation.CHANGE_WORK_ORDER_STATUS);
        }

        // A reclamation is a new order, not a change of status, so it follows the status directly.
        if (status == WorkOrderStatus.COMPLETED) {
            addAction(row, reclamationButton(garage, workOrder, dialog), garage, Operation.CREATE_WORK_ORDER);
        }

        if (workOrder.canChangeTo(WorkOrderStatus.CANCELLED)
                && Actions.allowed(garage, Operation.CHANGE_WORK_ORDER_STATUS)) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().addAll(spacer, actionButton("cancel", garage, workOrder, dialog));
        }

        // Only an order that was called off by mistake gets this far; the rule answers for the
        // rest, and the same question decides whether the button is drawn at all.
        if (garage.canDeleteWorkOrder(workOrder.getId())) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().addAll(spacer, deleteButton(garage, workOrder, dialog));
        }

        if (row.getChildren().isEmpty()) {
            // Two different empty rows: the order is finished, or the person looking may not do the
            // work. The second one would be a lie if it said there was nothing left to do.
            boolean mayChange = Actions.allowed(garage, Operation.CHANGE_WORK_ORDER_STATUS)
                    || Actions.allowed(garage, Operation.UPDATE_WORK_ORDER);
            Label emptyLabel = new Label(I18n.get(mayChange ? "dialog.workorder.no_actions"
                    : "dialog.workorder.read_only"));
            emptyLabel.getStyleClass().add("small");
            row.getChildren().add(emptyLabel);
        }

        return row;
    }

    /**
     * Removes an order that was created and called off by mistake. The rule has already said yes
     * before the button is drawn, but it is asked again on the way through the service, so a page
     * that was drawn before something changed still cannot remove what it should not.
     */
    private static Button deleteButton(GarageSystem garage, WorkOrder workOrder, Dialog<ButtonType> dialog) {

        Button button = UiComponents.secondaryButton(I18n.get("entity.workorders.action_delete"));
        button.getStyleClass().add("danger-button");

        button.setOnAction(e -> {
            Alert ask = ActionDialogs.confirm(I18n.get("dialog.workorder.delete.title"),
                    I18n.get("dialog.workorder.delete.header"),
                    I18n.get("dialog.workorder.delete.confirm", String.valueOf(workOrder.getId())));

            if (ask.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }

            try {
                garage.deleteWorkOrder(workOrder.getId());
            } catch (AppException refused) {
                ActionDialogs.showAppError(refused);
                return;
            }

            // The order is gone, so there is nothing left for the dialog to show.
            dialog.close();
        });

        return button;
    }

    /**
     * Draws the action only when the role may carry it out. A role that may read the order gets an
     * empty row and the line that says there is nothing to do here, rather than buttons that would
     * end in a refusal.
     */
    private static void addAction(HBox row, Button button, GarageSystem garage, Operation operation) {
        if (Actions.allowed(garage, operation)) {
            row.getChildren().add(Actions.button(button, garage, operation));
        }
    }

    /** The reclamation is created from the completed work and shows in the list as soon as the
     *  dialog redraws. */
    private static Button reclamationButton(GarageSystem garage, WorkOrder workOrder, Dialog<ButtonType> dialog) {        Button button = UiComponents.secondaryButton(I18n.get("entity.workorders.action_reclamation"));
        button.setOnAction(e -> ActionDialogs.showCreateReclamationDialog(garage, workOrder,
                () -> refreshDialog(garage, workOrder, dialog)));
        return button;
    }

    private static Button actionButton(String key, GarageSystem garage, WorkOrder workOrder, Dialog<ButtonType> dialog) {
        Button button = UiComponents.secondaryButton(I18n.get("entity.workorders.action_" + key));
        button.setOnAction(e -> {
            if ("confirm".equals(key)) {
                try {
                    garage.confirmWorkOrder(workOrder.getId());
                } catch (AppException refused) {
                    ActionDialogs.showAppError(refused);
                    return;
                }
                refreshDialog(garage, workOrder, dialog);
            } else if ("start".equals(key)) {
                garage.startWorkOrder(workOrder.getId());
                refreshDialog(garage, workOrder, dialog);
            } else if ("complete".equals(key)) {
                garage.completeWorkOrder(workOrder.getId());
                refreshDialog(garage, workOrder, dialog);
            } else if ("cancel".equals(key)) {
                // Cancelling cannot be undone, so it gets a yes of its own first.
                Alert alert = ActionDialogs.confirm(I18n.get("dialog.workorder.cancel.title"),
                        I18n.get("dialog.workorder.cancel.header"),
                        I18n.get("dialog.workorder.cancel.confirm", String.valueOf(workOrder.getId())));
                // The question mark JavaFX puts there is not part of our styling.
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
                    // The width is set after the theme has applied its style, otherwise the button
                    // shrinks and the text clips.
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

    /** Fetches the order again and redraws the content, so the status shows right away. */
    private static void refreshDialog(GarageSystem garage, WorkOrder workOrder, Dialog<ButtonType> dialog) {
        WorkOrder updated = garage.findWorkOrder(workOrder.getId());
        if (updated == null) {
            updated = workOrder;
        }
        dialog.getDialogPane().setContent(scrollContent(buildContent(garage, updated, dialog)));
        refitWindow(dialog);
    }

    /** The other direction: the reclamations that point at this order. */
    private static List<WorkOrder> reclamationsOf(GarageSystem garage, int workOrderId) {
        List<WorkOrder> found = new ArrayList<WorkOrder>();
        for (WorkOrder order : garage.getWorkOrders()) {
            if (order.getOriginalWorkOrderId() == workOrderId) {
                found.add(order);
            }
        }
        return found;
    }

    /** The content scrolls when it is taller than the window, so the button row is always visible. */
    private static ScrollPane scrollContent(VBox content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
        scroll.setMaxHeight(620);
        return scroll;
    }

    /** Shrinks the window to fit the content. Without it a shorter order leaves a gap above the button row. */
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
