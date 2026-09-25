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
        return eyebrowLabel == null
                ? new VBox(2, t, s)
                : new VBox(1, eyebrowLabel, t, s);
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

    public static VBox buildEntityPage(String title, String sub, String eyebrow,
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

        VBox inner = new VBox();
        inner.getStyleClass().add("panel");
        inner.getChildren().add(table);
        inner.setPadding(new Insets(4, 6, 6, 6));

        VBox.setVgrow(table, Priority.ALWAYS);
        return new VBox(18, topRow, inner);
    }
}
