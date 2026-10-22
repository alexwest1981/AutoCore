package com.wac.autocore.ui.motion;

import javafx.geometry.Bounds;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * A segmented control whose indicator sits on the selected item.
 *
 * The pill is a Rectangle because width is a writable property on a shape, which makes it easy to
 * stretch over the chosen button. It used to slide there; it is placed instead, so nothing moves.
 *
 * The indicator is unmanaged, so the HBox lays out the buttons as if it were not there.
 */
public final class SegmentedSwitch extends HBox {

    private final Rectangle pill = new Rectangle();
    private final List<Button> buttons = new ArrayList<Button>();
    private int selected;
    private IntConsumer onSelect;
    private boolean labelsSet;

    private final String itemStyleClass;

    public SegmentedSwitch(String... labels) {
        this("kanban-toggle-btn-compact", "segmented-pill", 6, labels);
    }

    /** Chips: the same sliding pill, with pill-shaped items. */
    public SegmentedSwitch(String itemStyleClass, String pillStyleClass, double arc, String... labels) {
        this.itemStyleClass = itemStyleClass;
        getStyleClass().add("segmented");
        pill.getStyleClass().add(pillStyleClass);
        pill.setManaged(false);
        pill.setArcWidth(arc);
        pill.setArcHeight(arc);
        getChildren().add(pill);

        setLabels(labels);
        widthProperty().addListener((obs, was, now) -> placePill(selected));
        heightProperty().addListener((obs, was, now) -> placePill(selected));

        setOnKeyPressed(e -> {
            int next = selected;
            if (e.getCode() == KeyCode.LEFT) next = Math.max(0, selected - 1);
            else if (e.getCode() == KeyCode.RIGHT) next = Math.min(buttons.size() - 1, selected + 1);
            else return;
            e.consume();
            if (next != selected) choose(next);
        });
    }

    /** Re-labels an existing switch (the language can change under it) without moving the pill. */
    public void setLabels(String... labels) {
        if (!labelsSet && labels.length > 0) {
            for (String label : labels) {
                final Button b = new Button(label);
                b.getStyleClass().add(itemStyleClass);
                b.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                b.setOnAction(e -> choose(buttons.indexOf(b)));
                b.setAccessibleRole(AccessibleRole.RADIO_BUTTON);
                buttons.add(b);
                getChildren().add(b);
            }
            labelsSet = true;
        } else if (labelsSet) {
            for (int i = 0; i < buttons.size() && i < labels.length; i++) {
                buttons.get(i).setText(labels[i]);
            }
        }
        markActive();
        placePill(selected);
    }

    public void setOnSelect(IntConsumer handler) {
        this.onSelect = handler;
    }

    public int getSelectedIndex() {
        return selected;
    }

    /** Selects without animating, for a render that reuses this control. */
    public void select(int index) {
        if (index < 0 || index >= buttons.size()) return;
        selected = index;
        markActive();
        placePill(index);
    }

    private void choose(int index) {
        if (index < 0 || index >= buttons.size()) return;
        boolean changed = index != selected;
        selected = index;
        markActive();
        placePill(index);
        if (changed && onSelect != null) {
            onSelect.accept(index);
        }
    }

    /** Every layout pass puts the pill exactly where the selected button is. */
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        placePill(selected);
    }

    private void markActive() {
        for (int i = 0; i < buttons.size(); i++) {
            Button b = buttons.get(i);
            b.getStyleClass().remove("active");
            b.getStyleClass().remove("is-current");
            if (i == selected) {
                // is-current: the theme styles .active, and a name it does not know cannot
                // lose the cascade to it
                b.getStyleClass().add("is-current");
            }
        }
    }

    private void placePill(int index) {
        if (index < 0 || index >= buttons.size()) return;
        Button target = buttons.get(index);
        // layoutBounds: boundsInParent includes the button's drop shadow, which put the
        // pill 3 px left and 14 px high
        Bounds b = target.getLayoutBounds();
        if (b.getWidth() <= 0) {
            return; // layoutChildren will place it as soon as the buttons have a size
        }
        double x = target.getLayoutX() + b.getMinX();
        double w = b.getWidth();
        double y = target.getLayoutY() + b.getMinY();
        double h = b.getHeight();
        pill.setY(y);
        pill.setHeight(h);

        // The pill is placed where the chosen segment is. It used to slide there over 200 ms,
        // which read as the whole switch moving; the colour change is the feedback.
        pill.setX(x);
        pill.setWidth(w);
    }
}
