package com.wac.autocore.ui.components;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.seed.SeedText;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.UiFormatters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The services on the booking: the row per service, the count and the sum.
 * Once the job has started the services cannot be changed.
 */
class BookingServicesField {

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
        // A ScrollPane sizes itself to its content, so the box grows with the list on its own. The
        // ceiling is what keeps a long list scrolling inside a dialog that may not grow any further.
        this.scroll.setMaxHeight(MAX_HEIGHT);
        this.scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
    }

    Label getSummary() { return summary; }

    ScrollPane getScroll() { return scroll; }

    /** Redraws the list and the summary. */
    void render() {
        container.getChildren().clear();
        List<ServiceItem> picked = form.getSelectedServices();

        if (picked.isEmpty()) {
            Label emptyLbl = new Label(I18n.get("dialog.booking.no_services_selected"));
            emptyLbl.setStyle("-fx-text-fill: -wac-muted; -fx-font-size: 11px; -fx-font-style: italic; -fx-padding: 2 0;");
            container.getChildren().add(emptyLbl);
            summary.setText("");
            return;
        }

        // A package's services are on the booking because the package is, so they are listed under it
        // and are not removable one by one here. What is left over was picked by hand. Ids are
        // compared, not objects: the package's items and the field's items are different instances.
        Set<Integer> inPackage = new HashSet<Integer>();
        for (ServicePackage pkg : form.getChosenPackages()) {
            List<ServiceItem> kept = new ArrayList<ServiceItem>();
            for (ServiceItem item : pkg.getServiceItems()) {
                if (inPackage.add(item.getId())) {
                    kept.add(item);
                }
            }
            if (kept.isEmpty()) {
                continue;
            }
            container.getChildren().add(heading(SeedText.resolve(pkg.getName())));
            for (ServiceItem item : kept) {
                container.getChildren().add(serviceRow(item, true));
            }
        }

        List<ServiceItem> loose = new ArrayList<ServiceItem>();
        for (ServiceItem item : picked) {
            if (!inPackage.contains(item.getId())) {
                loose.add(item);
            }
        }
        if (!loose.isEmpty()) {
            container.getChildren().add(heading(I18n.get("dialog.booking.services_not_in_package")));
            for (ServiceItem item : loose) {
                container.getChildren().add(serviceRow(item, false));
            }
        }

        int totalMin = 0;
        double totalCost = 0.0;
        for (ServiceItem item : picked) {
            totalMin += item.getEstimatedMinutes();
            totalCost += item.getPrice();
        }
        // Two lines rather than one: the box is narrow, and a separator ends up stranded at the
        // end of the first line as soon as the text wraps.
        summary.setText(I18n.get("dialog.booking.total_time", totalMin) + "\n"
                + I18n.get("dialog.booking.total_price", UiFormatters.formatMoney(totalCost)));
        summary.setStyle("-fx-font-weight: bold; -fx-text-fill: -wac-accent; -fx-font-size: 12px; -fx-padding: 2 0 0 2;");
    }

    /** The package name, or the heading for the services that were picked on their own. */
    private static Label heading(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -wac-accent; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 2 0 0 2;");
        return label;
    }

    private HBox serviceRow(ServiceItem item, boolean fromPackage) {
        HBox row = new HBox(8);
        row.setStyle("-fx-background-color: -wac-card; -fx-border-color: -wac-line; -fx-border-radius: 4; -fx-background-radius: 4; -fx-alignment: center-left;");
        // A service inside a package sits a little to the right, so the grouping is readable.
        row.setPadding(fromPackage ? new Insets(4, 8, 4, 16) : new Insets(4, 8, 4, 8));

        Label nameLbl = new Label(SeedText.resolve(item.getName()));
        nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: -wac-text; -fx-font-size: 12px;");

        Label detailLbl = new Label(UiFormatters.formatMoney(item.getPrice()) + " · " + item.getEstimatedMinutes() + " min");
        detailLbl.setStyle("-fx-text-fill: -wac-muted; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(nameLbl, detailLbl, spacer);

        // A service that comes from a package is removed by dropping the package, not one by one.
        if (!locked && !fromPackage) {
            Button removeBtn = new Button("✕");
            removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f87171; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 0 4; -fx-font-weight: bold;");
            removeBtn.setOnAction(ev -> form.removeService(item));
            row.getChildren().add(removeBtn);
        }
        return row;
    }
}
