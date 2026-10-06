package com.wac.autocore.ui.components;

import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.seed.SeedText;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.UiFormatters;

/**
 * Tjänsterna på bokningen: raden per tjänst, antalet och summan.
 * Är arbetet påbörjat går tjänsterna inte att ändra.
 */
class BookingServicesField {

    private static final double ROW_HEIGHT = 30;
    private static final double MAX_HEIGHT = 330;

    private final BookingFormPane form;
    private final boolean locked;
    private final VBox container = new VBox(4);
    private final Label summary = new Label();
    private final ScrollPane scroll;

    BookingServicesField(BookingFormPane form, boolean locked) {
        this.form = form;
        this.locked = locked;
        this.summary.setWrapText(true);
        this.summary.setMaxWidth(Double.MAX_VALUE);
        this.scroll = new ScrollPane(container);
        this.scroll.setFitToWidth(true);
        this.scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        this.scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
    }

    VBox getContainer() { return container; }

    Label getSummary() { return summary; }

    ScrollPane getScroll() { return scroll; }

    /** Ritar om listan och sammanfattningen. */
    void render() {
        container.getChildren().clear();
        if (form.getSelectedServices().isEmpty()) {
        Label emptyLbl = new Label(I18n.get("dialog.booking.no_services_selected"));
        emptyLbl.setStyle("-fx-text-fill: -wac-muted; -fx-font-size: 11px; -fx-font-style: italic; -fx-padding: 2 0;");
        container.getChildren().add(emptyLbl);
        summary.setText("");
        } else {
        int totalMin = 0;
        double totalCost = 0.0;
        for (ServiceItem item : form.getSelectedServices()) {
        totalMin += item.getEstimatedMinutes();
        totalCost += item.getPrice();

        HBox row = new HBox(8);
        row.setStyle("-fx-background-color: -wac-card; -fx-border-color: -wac-line; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-alignment: center-left;");

        Label nameLbl = new Label(SeedText.resolve(item.getName()));
        nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: -wac-text; -fx-font-size: 12px;");

        Label detailLbl = new Label(UiFormatters.formatMoney(item.getPrice()) + " · " + item.getEstimatedMinutes() + " min");
        detailLbl.setStyle("-fx-text-fill: -wac-muted; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (!locked) {
        Button removeBtn = new Button("✕");
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f87171; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 0 4; -fx-font-weight: bold;");
        removeBtn.setOnAction(ev -> form.getSelectedServices().remove(item));
        row.getChildren().addAll(nameLbl, detailLbl, spacer, removeBtn);
        } else {
        row.getChildren().addAll(nameLbl, detailLbl, spacer);
        }
        container.getChildren().add(row);
        }
        summary.setText(I18n.get("dialog.booking.total_time", totalMin) + "  |  " + I18n.get("dialog.booking.total_price", UiFormatters.formatMoney(totalCost)));
        summary.setStyle("-fx-font-weight: bold; -fx-text-fill: -wac-accent; -fx-font-size: 12px; -fx-padding: 2 0 0 2;");
        }
        // Rutan växer med antalet tjänster i stället för att scrolla i en liten yta. Taket gör
        // att en lång lista fortfarande scrollar, men först när dialogen är så hög den får bli.
        double wanted = 12 + Math.max(1, form.getSelectedServices().size()) * ROW_HEIGHT;
        double height = Math.min(MAX_HEIGHT, wanted);
        scroll.setPrefHeight(height);
        scroll.setMaxHeight(height);
    }
}
