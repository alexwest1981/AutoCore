package com.wac.autocore.ui.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.List;
import java.util.function.Function;

/** Multi-select field with chips and check boxes. The list is drawn on top of the view, not in a popup. */
public class MultiSelectComboBox<T> extends HBox {

    /** Key for the layer we put into the scene, so several fields can share one layer. */
    private static final String LAYER_KEY = "multi-select-overlay-layer";

    private final ObservableList<T> items = FXCollections.observableArrayList();
    private final ObservableList<T> selected = FXCollections.observableArrayList();
    private final Function<T, String> labelProvider;
    private final Function<T, String> subLabelProvider;
    private final String placeholder;
    /** Decides when two items are the same thing. The models have no equals, so set it to the id. */
    private Function<T, Object> keyFunction = item -> item;
    /** Keys the field will not pick. The row stays in the list but greyed out, so it is visible that
     *  the item is already on the booking by some other route. */
    private final java.util.Set<Object> blockedKeys = new java.util.HashSet<Object>();
    /** The chip has less room than the list row. Default: the same text as the row. */
    private Function<T, String> chipTextProvider;

    private final HBox chips = new HBox(6);
    private final ScrollPane chipScroll = new ScrollPane(chips);
    private final Label placeholderLabel = new Label();
    /** The arrow: same node structure as the app's ComboBoxes, so CSS gives it the same shape and place. */
    private final Region caret = new Region();
    private final StackPane caretButton = new StackPane(caret);
    private final VBox rows = new VBox(2);
    /** Row and check box per key, so a row can be updated without being rebuilt. */
    private final java.util.Map<Object, HBox> rowByKey = new java.util.LinkedHashMap<Object, HBox>();
    private final java.util.Map<Object, CheckBox> checkBoxByKey = new java.util.LinkedHashMap<Object, CheckBox>();
    private final ScrollPane rowScroll = new ScrollPane(rows);
    private final VBox panel = new VBox(rowScroll);

    private Pane overlay;
    private boolean open;
    private javafx.event.EventHandler<javafx.scene.input.MouseEvent> outsideClick;
    private javafx.event.EventHandler<javafx.scene.input.KeyEvent> escape;

    public MultiSelectComboBox(String placeholder, Function<T, String> labelProvider) {
        this(placeholder, labelProvider, null);
    }

    public MultiSelectComboBox(String placeholder, Function<T, String> labelProvider,
                               Function<T, String> subLabelProvider) {
        this.placeholder = placeholder == null ? "" : placeholder;
        this.labelProvider = labelProvider;
        this.subLabelProvider = subLabelProvider;

        getStyleClass().add("multi-select");
        setFocusTraversable(true);
        setAlignment(Pos.CENTER_LEFT);
        // Height and padding come from the multi-select rules in components.css (31 px).

        chips.setAlignment(Pos.CENTER_LEFT);
        chipScroll.getStyleClass().add("multi-select-scroll");
        chipScroll.setFitToHeight(true);
        chipScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chipScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        HBox.setHgrow(chipScroll, Priority.ALWAYS);

        placeholderLabel.getStyleClass().add("multi-select-placeholder");
        placeholderLabel.setText(this.placeholder);

        caretButton.getStyleClass().add("arrow-button");
        caret.getStyleClass().add("arrow");
        caret.setMouseTransparent(true);

        // The chip row is the part that grows, so the arrow always ends up at the field's right edge.
        HBox.setHgrow(chipScroll, Priority.ALWAYS);
        chipScroll.setMaxWidth(Double.MAX_VALUE);
        // A click anywhere in the field opens the list. The filter sits on the field and runs
        // before the children, so the chip row cannot swallow the click. The cross on a chip is
        // passed over.
        addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (isInsideClass((Node) e.getTarget(), "multi-select-chip-close")) {
                return;
            }
            // The field takes focus itself, otherwise the chip row steals it and the focus ring never shows.
            requestFocus();
            toggleList();
        });

        getChildren().addAll(placeholderLabel, chipScroll, caretButton);

        rows.setPadding(new Insets(4));
        rowScroll.getStyleClass().add("multi-select-popup-scroll");
        rowScroll.setFitToWidth(true);
        rowScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        panel.getStyleClass().add("multi-select-popup");
        panel.setVisible(false);
        panel.setManaged(false);

        selected.addListener((javafx.collections.ListChangeListener<T>) c -> {
            renderChips();
            // The rows are updated in place, they are not rebuilt.
            updateRows();
        });
        renderChips();
        renderRows();
    }

    public void setItems(List<T> values) {
        items.setAll(values);
        renderRows();
    }

    /** The selected items, in the order they were selected. */
    public ObservableList<T> getSelectedItems() {
        return selected;
    }

    public void setSelectedItems(List<T> values) {
        selected.clear();
        for (T item : values) {
            // Duplicates in the input must not become two selections of the same thing.
            addSelectedItem(item);
        }
    }

    /** The text in the field's chip. Without a provider of its own, the row text is used. */
    public void setChipTextProvider(Function<T, String> provider) {
        this.chipTextProvider = provider;
        renderChips();
    }

    /** The key per item, for example ServiceItem::getId. */
    public void setKeyProvider(Function<T, Object> keyProvider) {
        if (keyProvider != null) {
            this.keyFunction = keyProvider;
        }
        renderChips();
        renderRows();
    }

    /** True if the item is already selected, compared by key. */
    public boolean isSelectedItem(T item) {
        return isSelectedKey(keyFunction.apply(item));
    }

    /** Marks the items that cannot be picked, compared by key. An item that was picked before it
     *  got blocked stays picked, so lifting the block brings it back on the booking. */
    public void setBlockedItems(List<T> values) {
        blockedKeys.clear();
        for (T item : values) {
            blockedKeys.add(keyFunction.apply(item));
        }
        renderChips();
        renderRows();
    }

    /** Adds the item only if the key is not already among the selected ones or blocked. */
    public boolean addSelectedItem(T item) {
        if (isSelectedItem(item) || blockedKeys.contains(keyFunction.apply(item))) {
            return false;
        }
        selected.add(item);
        return true;
    }

    /** Removes the item with the same key, whichever instance sits in the list. */
    public boolean removeSelectedItem(T item) {
        Object key = keyFunction.apply(item);
        for (int i = 0; i < selected.size(); i++) {
            if (java.util.Objects.equals(keyFunction.apply(selected.get(i)), key)) {
                selected.remove(i);
                return true;
            }
        }
        return false;
    }

    private void renderChips() {
        chips.getChildren().clear();
        int shown = 0;
        for (T item : selected) {
            // A service the booking already holds through a package is listed under that package, so
            // it is not shown as a chip too. Dropping the package brings the chip back.
            if (blockedKeys.contains(keyFunction.apply(item))) {
                continue;
            }
            chips.getChildren().add(chip(item));
            shown++;
        }
        boolean empty = shown == 0;
        // The chip row stays in the layout even when it is empty, otherwise nothing in the field
        // grows and the arrow lands right after the placeholder text instead of at the right edge.
        placeholderLabel.setVisible(empty);
        placeholderLabel.setManaged(empty);
        chipScroll.setVisible(!empty);
        chipScroll.setManaged(true);
    }

    private Node chip(T item) {
        Label theLabel = new Label(chipText(item));
        theLabel.getStyleClass().add("multi-select-chip-text");

        Label close = new Label("×");
        close.getStyleClass().add("multi-select-chip-close");
        close.setOnMouseClicked(e -> {
            removeSelectedItem(item);
            e.consume();
        });

        HBox chip = new HBox(6, theLabel, close);
        chip.getStyleClass().add("multi-select-chip");
        chip.setAlignment(Pos.CENTER_LEFT);
        // The chips keep their own height inside the field, they are not stretched to the field's.
        chip.setMaxHeight(Region.USE_PREF_SIZE);
        chip.setMinHeight(Region.USE_PREF_SIZE);
        return chip;
    }

    private void renderRows() {
        rows.getChildren().clear();
        rowByKey.clear();
        checkBoxByKey.clear();
        if (items.isEmpty()) {
            Label emptyLabel = new Label("Inga val");
            emptyLabel.getStyleClass().add("multi-select-empty");
            rows.getChildren().add(emptyLabel);
            return;
        }
        for (T item : items) {
            rows.getChildren().add(row(item));
        }
    }

    private Node row(T item) {
        CheckBox check = new CheckBox();
        check.getStyleClass().add("multi-select-check");
        check.setSelected(isSelectedItem(item));
        check.setMouseTransparent(true);

        Label theLabel = new Label(rowText(item));
        theLabel.getStyleClass().add("multi-select-row-text");

        VBox labels = new VBox(1, theLabel);
        HBox.setHgrow(labels, Priority.ALWAYS);
        if (subLabelProvider != null) {
            String sub = subLabelProvider.apply(item);
            if (sub != null && !sub.isEmpty()) {
                Label subLabel = new Label(sub);
                subLabel.getStyleClass().add("multi-select-row-sub");
                labels.getChildren().add(subLabel);
            }
        }

        HBox row = new HBox(10, check, labels);
        row.getStyleClass().add("multi-select-row");
        row.setAlignment(Pos.CENTER_LEFT);
        boolean blocked = blockedKeys.contains(keyFunction.apply(item));
        if (isSelectedItem(item)) {
            row.getStyleClass().add("selected");
        }
        if (blocked) {
            row.getStyleClass().add("blocked");
        } else {
            row.setOnMouseClicked(e -> {
                if (isSelectedItem(item)) {
                    removeSelectedItem(item);
                } else {
                    addSelectedItem(item);
                }
                e.consume();
            });
        }
        rowByKey.put(keyFunction.apply(item), row);
        checkBoxByKey.put(keyFunction.apply(item), check);
        return row;
    }

    /** True if an item with that key is selected. */
    private boolean isSelectedKey(Object key) {
        for (T chosenItem : selected) {
            if (java.util.Objects.equals(keyFunction.apply(chosenItem), key)) {
                return true;
            }
        }
        return false;
    }

    /** Ticks the rows that already exist instead of rebuilding the list. */
    private void updateRows() {
        for (java.util.Map.Entry<Object, HBox> e : rowByKey.entrySet()) {
            boolean chosenItem = isSelectedKey(e.getKey());
            CheckBox box = checkBoxByKey.get(e.getKey());
            if (box != null) {
                box.setSelected(chosenItem);
            }
            if (chosenItem) {
                if (!e.getValue().getStyleClass().contains("selected")) {
                    e.getValue().getStyleClass().add("selected");
                }
            } else {
                e.getValue().getStyleClass().remove("selected");
            }
        }
    }

    private String rowText(T item) {
        return labelProvider == null ? String.valueOf(item) : labelProvider.apply(item);
    }

    private String chipText(T item) {
        if (chipTextProvider != null) {
            return chipTextProvider.apply(item);
        }
        return rowText(item);
    }

    /** Opens the list if it is closed, otherwise closes it. */
    public void toggleList() {
        if (open) {
            hideList();
        } else {
            showList();
        }
    }

    /** Opens the list. Does nothing if it is already open. */
    public void showList() {
        if (open || getScene() == null) {
            return;
        }
        renderRows();

        Scene scene = getScene();
        StackPane overlayLayer = overlayLayer(scene);
        overlay = new Pane();
        overlay.setPickOnBounds(false);
        overlay.setMouseTransparent(false);
        overlayLayer.getChildren().add(overlay);

        // The width follows the longest content, so no name gets clipped or squeezed.
        double textbredd = 0;
        for (T item : items) {
            Text measurer = new Text(rowText(item));
            measurer.setFont(Font.font("System", 13));
            textbredd = Math.max(textbredd, measurer.getLayoutBounds().getWidth());
        }
        double width = Math.round(Math.max(300, Math.min(textbredd + 70, 620)));
        panel.setPrefWidth(width);
        panel.setMinWidth(width);
        panel.setMaxWidth(width);
        rowScroll.setPrefWidth(width);
        rowScroll.setMaxWidth(width);
        rowScroll.setPrefHeight(Region.USE_COMPUTED_SIZE);
        rowScroll.setMaxHeight(280);

        if (panel.getParent() != overlay) {
            overlay.getChildren().add(panel);
        }
        panel.setManaged(true);
        panel.setVisible(true);
        open = true;
        if (!getStyleClass().contains("showing")) {
            getStyleClass().add("showing");
        }

        // Place the list right below the field, in scene coordinates, on whole pixels. If it does
        // not fit there it goes above instead: in a dialog that is exactly as tall as its content
        // the last field sits near the bottom edge, and then the bottom row was clipped by the window.
        Bounds b = localToScene(getBoundsInLocal());
        panel.autosize();
        panel.applyCss();

        double panelHeight = panel.prefHeight(width);
        double x = Math.round(b.getMinX());
        double y = Math.round(b.getMaxY()) + 2;
        if (y + panelHeight > scene.getHeight()) {
            y = Math.round(b.getMinY()) - panelHeight - 2;
        }
        // If it does not fit above either (a very short window) it is pressed inside the edge, so
        // the last row is always clickable.
        x = Math.max(0, Math.min(x, scene.getWidth() - width));
        y = Math.max(0, Math.min(y, scene.getHeight() - panelHeight));

        panel.relocate(x, y);
        panel.toFront();

        outsideClick = ev -> {
            Node target = (Node) ev.getTarget();
            if (!isInside(target, panel) && !isInside(target, this)) {
                hideList();
            }
        };
        escape = ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) {
                hideList();
            }
        };
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, outsideClick);
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, escape);
    }

    /** Closes the list if it is open. */
    public void hideList() {
        if (!open) {
            return;
        }
        open = false;
        getStyleClass().remove("showing");
        if (panel.getParent() instanceof Pane) {
            ((Pane) panel.getParent()).getChildren().remove(panel);
        }
        panel.setVisible(false);
        panel.setManaged(false);
        Scene scene = getScene();
        if (scene != null) {
            if (outsideClick != null) {
                scene.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, outsideClick);
            }
            if (escape != null) {
                scene.removeEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, escape);
            }
        }
        if (overlay != null) {
            overlay.getChildren().remove(panel);
            if (overlay.getParent() instanceof StackPane) {
                ((StackPane) overlay.getParent()).getChildren().remove(overlay);
            }
            overlay = null;
        }
        outsideClick = null;
        escape = null;
    }

    /** True if the node or any of its parents has that stylesheet class. */
    private static boolean isInsideClass(Node target, String styleClass) {
        Node n = target;
        while (n != null) {
            if (n.getStyleClass().contains(styleClass)) {
                return true;
            }
            n = n.getParent();
        }
        return false;
    }

    private static boolean isInside(Node target, Node parent) {
        Node n = target;
        while (n != null) {
            if (n == parent) {
                return true;
            }
            n = n.getParent();
        }
        return false;
    }

    /** Puts the list in a layer on top of the view. Created once and shared by the scene's fields. */
    private static StackPane overlayLayer(Scene scene) {
        Parent root = scene.getRoot();
        if (root instanceof StackPane && Boolean.TRUE.equals(root.getProperties().get(LAYER_KEY))) {
            return (StackPane) root;
        }
        StackPane overlayLayer = new StackPane(root);
        overlayLayer.getProperties().put(LAYER_KEY, Boolean.TRUE);
        overlayLayer.setAlignment(Pos.TOP_LEFT);
        scene.setRoot(overlayLayer);
        return overlayLayer;
    }

}
