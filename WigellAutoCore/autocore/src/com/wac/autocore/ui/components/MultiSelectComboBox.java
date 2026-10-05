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

/**
 * Flervalsfält: valda poster som chips i fältet, en kryssruta per rad i listan. Listan ritas i ett
 * lager ovanpå vyn, inte i ett popup-fönster, som blir suddigt i den här skrivbordsmiljön.
 */
public class MultiSelectComboBox<T> extends HBox {

    /** Nyckel på det lager vi lagt in i scenen, så att flera fält kan dela samma lager. */
    private static final String LAYER_KEY = "multi-select-overlay-layer";

    private final ObservableList<T> items = FXCollections.observableArrayList();
    private final ObservableList<T> selected = FXCollections.observableArrayList();
    private final Function<T, String> labelProvider;
    private final Function<T, String> subLabelProvider;
    private final String placeholder;
    /** Avgör när två poster är samma sak. Modellerna saknar equals, så sätt den till id:t. */
    private Function<T, Object> keyFunction = item -> item;
    /** Chipset har mindre plats än listraden. Standard: samma text som raden. */
    private Function<T, String> chipTextProvider;

    private final HBox chips = new HBox(6);
    private final ScrollPane chipScroll = new ScrollPane(chips);
    private final Label placeholderLabel = new Label();
    /** Pilen: samma nodbygge som appens ComboBoxar, så att CSS ger den samma form och plats. */
    private final Region caret = new Region();
    private final StackPane caretButton = new StackPane(caret);
    private final VBox rows = new VBox(2);
    /** Rad och kryssruta per nyckel, så att en rad kan uppdateras utan att byggas om. */
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
        // Höjd och luft kommer från multi-select-reglerna i components.css (31 px).

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

        // Chipraden är den del som ska växa, så att pilen alltid hamnar vid fältets högerkant.
        HBox.setHgrow(chipScroll, Priority.ALWAYS);
        chipScroll.setMaxWidth(Double.MAX_VALUE);
        // Klick var som helst i fältet öppnar listan. Filtret ligger på fältet och kör före barnen,
        // så chipraden kan inte sluka klicket. Krysset på ett chip hoppas över.
        addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (isInsideClass((Node) e.getTarget(), "multi-select-chip-close")) {
                return;
            }
            // Fältet tar fokus själv, annars stjäl chipraden det och fokusringen uteblir.
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
            // Raderna uppdateras på plats, de byggs inte om.
            updateRows();
        });
        renderChips();
        renderRows();
    }

    // --- data -----------------------------------------------------------------


    public void setItems(List<T> values) {
        items.setAll(values);
        renderRows();
    }

    /** De valda posterna, i den ordning de valdes. */
    public ObservableList<T> getSelectedItems() {
        return selected;
    }

    public void setSelectedItems(List<T> values) {
        selected.clear();
        for (T item : values) {
            // Dubbletter i indata ska inte kunna bli två val av samma sak.
            addSelectedItem(item);
        }
    }


    /** Texten i chipset i fältet. Utan egen provider används radtexten. */
    public void setChipTextProvider(Function<T, String> provider) {
        this.chipTextProvider = provider;
        renderChips();
    }

    /** Nyckeln per post, till exempel ServiceItem::getId. */
    public void setKeyProvider(Function<T, Object> keyProvider) {
        if (keyProvider != null) {
            this.keyFunction = keyProvider;
        }
        renderChips();
        renderRows();
    }

    /** Sant om posten redan är vald, jämfört med nyckeln. */
    public boolean isSelectedItem(T item) {
        return isSelectedKey(keyFunction.apply(item));
    }

    /** Lägger till posten bara om nyckeln inte redan finns bland de valda. */
    public boolean addSelectedItem(T item) {
        if (isSelectedItem(item)) {
            return false;
        }
        selected.add(item);
        return true;
    }

    /** Tar bort posten med samma nyckel, oavsett vilken instans som ligger i listan. */
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

    // --- rendering ------------------------------------------------------------

    private void renderChips() {
        chips.getChildren().clear();
        for (T item : selected) {
            chips.getChildren().add(chip(item));
        }
        boolean empty = selected.isEmpty();
        // Chipraden stannar i layouten även när den är tom, annars växer ingenting i fältet och
        // pilen hamnar direkt efter platshållartexten i stället för vid högerkanten.
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
        // Chipsen ska hålla sin egen höjd inuti fältet, inte töjas till fältets.
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
        if (isSelectedItem(item)) {
            row.getStyleClass().add("selected");
        }
        row.setOnMouseClicked(e -> {
            if (isSelectedItem(item)) {
                removeSelectedItem(item);
            } else {
                addSelectedItem(item);
            }
            e.consume();
        });
        rowByKey.put(keyFunction.apply(item), row);
        checkBoxByKey.put(keyFunction.apply(item), check);
        return row;
    }

    /** Sant om en post med den nyckeln ligger vald. */
    private boolean isSelectedKey(Object key) {
        for (T chosenItem : selected) {
            if (java.util.Objects.equals(keyFunction.apply(chosenItem), key)) {
                return true;
            }
        }
        return false;
    }

    /** Kryssar i de rader som redan finns i stället för att bygga om listan. */
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

    // --- listan ---------------------------------------------------------------

    /** Öppnar listan om den är stängd, annars stänger den. */
    public void toggleList() {
        if (open) {
            hideList();
        } else {
            showList();
        }
    }

    /** Öppnar listan. Gör inget om den redan är open. */
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

        // Bredden följer det längsta innehållet, så att inget namn klipps eller trycks ihop.
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

        // Placera listan direkt under fältet, i scenens koordinater, på hela pixlar.
        Bounds b = localToScene(getBoundsInLocal());
        panel.autosize();
        panel.applyCss();
        panel.relocate(Math.round(b.getMinX()), Math.round(b.getMaxY()) + 2);
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

    /** Stänger listan om den är open. */
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

    /** Sant om noden eller någon av dess föräldrar har den stilmallen. */
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

    /** Lägger listan i ett lager ovanpå vyn. Skapas en gång och delas av scenens fält. */
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
