package com.wac.autocore.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Shared building blocks for the interface. */
public final class UiComponents {

    private UiComponents() {}

    /** A FlowPane wraps at its own width. The threshold is set high so it only wraps when it must. */
    private static final double NO_WRAP_LENGTH = 4000;

    public static VBox pageHead(String title, String sub, String eyebrow) {
        Label eyebrowLabel = null;
        if (eyebrow != null) {
            eyebrowLabel = new Label(eyebrow);
            eyebrowLabel.getStyleClass().add("eyebrow");
        }
        Label t = new Label(title);
        t.getStyleClass().add("page-title");
        Label s = new Label(sub);
        s.getStyleClass().add("page-sub");
        VBox titles = eyebrowLabel == null
                ? new VBox(2, t, s)
                : new VBox(1, eyebrowLabel, t, s);

        HBox row = new HBox(12, titles);
        HBox.setHgrow(titles, Priority.ALWAYS);
        row.setAlignment(Pos.CENTER_LEFT);
        return new VBox(row);
    }

    public static Button primaryButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("button", "primary");
        b.setMinWidth(Region.USE_PREF_SIZE);
        return b;
    }

    public static Button secondaryButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("button", "secondary-button");
        b.setMinWidth(Region.USE_PREF_SIZE);
        return b;
    }

    public static VBox kpi(String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("kpi-label");
        Label v = new Label(value);
        v.getStyleClass().add("kpi-value");
        VBox box = new VBox(6, v, l);
        box.getStyleClass().add("kpi");
        box.setMinWidth(140);
        box.setPrefWidth(220);
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    public static VBox panel(String title, String sub, Node body) {
        Label t = new Label(title);
        t.getStyleClass().add("panel-title");
        Label s = new Label(sub);
        s.getStyleClass().add("panel-sub");
        VBox box = new VBox(10, t, s, body);
        box.getStyleClass().add("panel");
        return box;
    }

    public static Label mutedNote(String text) {
        Label l = new Label(text);
        l.getStyleClass().addAll("srow-sub", "small");
        return l;
    }

    /** Reads the logo from the application's resources. */
    public static javafx.scene.image.Image loadLogoImage() {
        try {
            java.io.InputStream in = UiComponents.class.getResourceAsStream("/com/wac/autocore/images/Logo.png");
            if (in != null) {
                return new javafx.scene.image.Image(in);
            }
            java.io.File fRes = new java.io.File("WigellAutoCore/autocore/src/resources/com/wac/autocore/images/Logo.png");
            if (fRes.exists()) {
                return new javafx.scene.image.Image(fRes.toURI().toString());
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static VBox buildEntityPage(String title, String sub, String eyebrow,
                                       TableFactory.FilterableTable<?> data, Node... actions) {
        return buildEntityPage(title, sub, eyebrow, null, data, actions);
    }

    /** The same page but with a notice row under the header, carrying the number the sidebar shows. */
    public static VBox buildEntityPage(String title, String sub, String eyebrow, Node notice,
                                       TableFactory.FilterableTable<?> data, Node... actions) {
        TableView<?> table = data.getTableView();
        VBox titles = pageHead(title, sub, eyebrow);
        // The heading must never be crushed. It keeps its natural width instead of a guessed
        // one, and the buttons wrap to the next line when they do not fit.
        titles.setMinWidth(Region.USE_PREF_SIZE);

        HBox topRow = new HBox(12, titles);
        topRow.setAlignment(Pos.CENTER_LEFT);

        if (actions != null && actions.length > 0) {
            for (Node act : actions) {
                if (act instanceof Button) {
                    Button b = (Button) act;
                    b.setMinWidth(Region.USE_PREF_SIZE);
                }
            }
            // A FlowPane instead of an HBox: it wraps the buttons to the next line when they do
            // not fit, rather than taking the heading's room.
            FlowPane actionBox = new FlowPane(8, 8);
            actionBox.getChildren().addAll(actions);
            actionBox.setAlignment(Pos.CENTER_RIGHT);
            actionBox.setPrefWrapLength(NO_WRAP_LENGTH);
            HBox.setHgrow(actionBox, Priority.ALWAYS);
            topRow.getChildren().add(actionBox);
        }

        Label placeholder = new Label(com.wac.autocore.ui.i18n.I18n.get("table.empty"));
        placeholder.getStyleClass().add("text-muted");
        table.setPlaceholder(placeholder);
        HBox.setHgrow(table, Priority.ALWAYS);
        fixTableHeight(table);

        VBox inner = new VBox(0, table, TableFactory.buildPager(data));
        inner.getStyleClass().add("panel");
        inner.setPadding(new Insets(4, 6, 6, 6));

        // Notisen ligger på en egen rad under sidhuvudet. Ligger den i sidhuvudet kläms den
        // ihop av knappraden, som tar allt ledigt utrymme.
        if (notice == null) {
            return new VBox(18, topRow, inner);
        }
        return new VBox(18, topRow, notice, inner);
    }

    /** A fixed height for a table page, even when the list is short — then the panel stays put
     *  between views. */
    public static void fixTableHeight(TableView<?> table) {
        final double CELL_HEIGHT = 32;
        final double HEADER_HEIGHT = 36;
        table.setFixedCellSize(CELL_HEIGHT);
        double height = HEADER_HEIGHT + (TableFactory.FilterableTable.PAGE_SIZE * CELL_HEIGHT) + 2; // +2 for border
        table.setPrefHeight(height);
        table.setMinHeight(height);
        table.setMaxHeight(height);
    }

    /** The notice row: a number and a line about what is waiting. Empty, it takes no space. */
    public static Node viewNotice(int count, String singularKey, String pluralKey) {
        HBox row = new HBox(10);
        row.getStyleClass().add("notice");
        row.setAlignment(Pos.CENTER_LEFT);
        if (count <= 0) {
            row.setVisible(false);
            row.setManaged(false);
            return row;
        }
        Label number = new Label(String.valueOf(count));
        number.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        // One is named differently from several, so the text is chosen by the number.
        Label message = new Label(com.wac.autocore.ui.i18n.I18n.get(count == 1 ? singularKey : pluralKey));
        row.getChildren().addAll(number, message);
        return row;
    }

    /** The invoice lines grouped by package, in the order the lines appear. The empty key holds the
     *  lines that came without a package. Both dialogs use this, so they group the same way. */
    public static java.util.LinkedHashMap<String, java.util.List<com.wac.autocore.model.InvoiceLine>> groupLinesByPackage(
            com.wac.autocore.model.Invoice invoice) {
        java.util.LinkedHashMap<String, java.util.List<com.wac.autocore.model.InvoiceLine>> groups =
                new java.util.LinkedHashMap<String, java.util.List<com.wac.autocore.model.InvoiceLine>>();
        if (invoice == null || invoice.getLines() == null) {
            return groups;
        }
        for (com.wac.autocore.model.InvoiceLine line : invoice.getLines()) {
            String key = line.getPackageName();
            java.util.List<com.wac.autocore.model.InvoiceLine> group = groups.get(key);
            if (group == null) {
                group = new java.util.ArrayList<com.wac.autocore.model.InvoiceLine>();
                groups.put(key, group);
            }
            group.add(line);
        }
        return groups;
    }
}
