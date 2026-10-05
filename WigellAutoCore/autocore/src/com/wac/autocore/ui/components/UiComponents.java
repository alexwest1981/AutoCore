package com.wac.autocore.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Gemensamma UI-byggstenar och layoutkomponenter för applikationen.
 */
public final class UiComponents {

    private UiComponents() {}

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

    /** Läser logotypen från applikationens resurser. */
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
                                       TableView<?> table, Node... actions) {
        return buildEntityPage(title, sub, eyebrow, null, table, actions);
    }

    /**
     * Samma sida, men med en notisrad överst. Raden visar samma siffra som sidebaren gör för vyn,
     * så att man ser att det finns något att hantera även när man står i vyn. Är det inget att
     * hantera tar raden ingen plats alls.
     */
    public static VBox buildEntityPage(String title, String sub, String eyebrow, Node notice,
                                       TableView<?> table, Node... actions) {
        VBox titles = pageHead(title, sub, eyebrow);
        HBox.setHgrow(titles, Priority.ALWAYS);

        HBox topRow = new HBox(12, titles);
        topRow.setAlignment(Pos.CENTER_LEFT);

        if (actions != null && actions.length > 0) {
            for (Node act : actions) {
                if (act instanceof Button) {
                    Button b = (Button) act;
                    b.setMinWidth(Region.USE_PREF_SIZE);
                }
            }
            HBox actionBox = new HBox(8, actions);
            actionBox.setAlignment(Pos.CENTER_RIGHT);
            actionBox.setMinWidth(Region.USE_PREF_SIZE);
            topRow.getChildren().add(actionBox);
        }

        Label placeholder = new Label(com.wac.autocore.ui.i18n.I18n.get("table.empty"));
        placeholder.getStyleClass().add("text-muted");
        table.setPlaceholder(placeholder);
        HBox.setHgrow(table, Priority.ALWAYS);

        /* Låt tabellen visa ALLA rader utan intern scroll.
           Den yttre ScrollPane i AutoCoreApp hanterar sidscroll. */
        final double CELL_HEIGHT = 32;
        final double HEADER_HEIGHT = 36;
        table.setFixedCellSize(CELL_HEIGHT);
        Runnable resize = () -> {
            int rows = table.getItems().size();
            double h = HEADER_HEIGHT + (rows * CELL_HEIGHT) + 2; // +2 for border
            table.setPrefHeight(h);
            table.setMinHeight(h);
            table.setMaxHeight(h);
        };
        resize.run();
        table.getItems().addListener((javafx.collections.ListChangeListener<Object>) c -> resize.run());

        VBox inner = new VBox();
        inner.getStyleClass().add("panel");
        inner.getChildren().add(table);
        inner.setPadding(new Insets(4, 6, 6, 6));

        if (notice == null) {
            return new VBox(18, topRow, inner);
        }
        return new VBox(18, topRow, notice, inner);
    }

    /**
     * Notisraden: en siffra och en rad om vad som väntar, i temats egen notisfärg. Är det inget
     * att hantera blir raden osynlig och tar ingen plats.
     */
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
        // Ett styck heter annat än flera, så texten väljs efter siffran.
        Label message = new Label(com.wac.autocore.ui.i18n.I18n.get(count == 1 ? singularKey : pluralKey));
        row.getChildren().addAll(number, message);
        return row;
    }
}
