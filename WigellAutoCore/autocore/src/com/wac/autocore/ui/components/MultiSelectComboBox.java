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
 * Ett flervalsfält: de valda posterna visas som chips inuti fältet, och listan visar varje post med
 * en kryssruta. Ett klick på en rad växlar posten och lämnar listan öppen.
 *
 * Listan ritas inuti fönstret, i ett lager ovanpå vyn, inte i ett eget popup-fönster. Skälet är
 * att ett separat fönster får sin text omskalad av skrivbordsmiljön och blir suddig. Lagret rör
 * inget annat än sig självt och tas bort när listan stängs.
 */
public class MultiSelectComboBox<T> extends HBox {

    /** Nyckel på det lager vi lagt in i scenen, så att flera fält kan dela samma lager. */
    private static final String LAGER_NYCKEL = "multi-select-scene-lager";

    private final ObservableList<T> items = FXCollections.observableArrayList();
    private final ObservableList<T> selected = FXCollections.observableArrayList();
    private final Function<T, String> labelProvider;
    private final Function<T, String> subLabelProvider;
    private final String placeholder;
    /**
     * Nyckeln som avgör om två poster är samma sak. Modellerna i appen har ingen equals, så två
     * anrop till datahämtaren ger olika objekt för samma tjänst. Utan nyckeln skulle samma tjänst
     * kunna väljas två gånger. Standard: posten själv, vilket räcker när anroparen håller samma
     * objekt hela vägen.
     */
    private Function<T, Object> nyckel = item -> item;
    /**
     * Texten i chipset i fältet. Raden i listan får plats med mer, chipset mindre, så de kan skilja
     * sig: mekanikerns specialisering står i listan men inte i chipset. Standard: samma som raden.
     */
    private Function<T, String> chipTextProvider;

    private final HBox chips = new HBox(6);
    private final ScrollPane chipScroll = new ScrollPane(chips);
    private final Label placeholderLabel = new Label();
    private final Label caret = new Label("▾");
    private final VBox rows = new VBox(2);
    /** Rad och kryssruta per nyckel, så att en rad kan uppdateras utan att byggas om. */
    private final java.util.Map<Object, HBox> radPerNyckel = new java.util.LinkedHashMap<Object, HBox>();
    private final java.util.Map<Object, CheckBox> kryssPerNyckel = new java.util.LinkedHashMap<Object, CheckBox>();
    private final ScrollPane rowScroll = new ScrollPane(rows);
    private final VBox panel = new VBox(rowScroll);

    private Pane overlay;
    private boolean öppen;
    private javafx.event.EventHandler<javafx.scene.input.MouseEvent> klickUtanför;
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
        setAlignment(Pos.CENTER_LEFT);
        // Luft, avstånd och höjd kommer från multi-select-reglerna i components.css, så att fältet
        // blir exakt lika högt som appens egna ComboBoxar (31 px).

        chips.setAlignment(Pos.CENTER_LEFT);
        chipScroll.getStyleClass().add("multi-select-scroll");
        chipScroll.setFitToHeight(true);
        chipScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chipScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        HBox.setHgrow(chipScroll, Priority.ALWAYS);

        placeholderLabel.getStyleClass().add("multi-select-placeholder");
        placeholderLabel.setText(this.placeholder);

        caret.getStyleClass().add("multi-select-caret");

        getChildren().addAll(placeholderLabel, chipScroll, caret);
        setOnMouseClicked(e -> toggleLista());

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
            uppdateraRader();
        });
        renderChips();
        renderRows();
    }

    // --- data -----------------------------------------------------------------

    /** Posterna som går att välja. */
    public ObservableList<T> getItems() {
        return items;
    }

    public void setItems(List<T> nya) {
        items.setAll(nya);
        renderRows();
    }

    /** De valda posterna, i den ordning de valdes. */
    public ObservableList<T> getSelectedItems() {
        return selected;
    }

    public void setSelectedItems(List<T> nya) {
        selected.clear();
        for (T item : nya) {
            // Dubbletter i indata ska inte kunna bli två val av samma sak.
            addSelectedItem(item);
        }
    }

    public void clearSelectedItems() {
        selected.clear();
    }

    /** Texten i chipset i fältet. Utan egen provider används radtexten. */
    public void setChipTextProvider(Function<T, String> provider) {
        this.chipTextProvider = provider;
        renderChips();
    }

    /**
     * Bestämmer vad som räknas som samma post. Skicka in id:t, till exempel ServiceItem::getId,
     * när modellen saknar equals och posterna kan komma från olika anrop.
     */
    public void setKeyProvider(Function<T, Object> nyckelProvider) {
        if (nyckelProvider != null) {
            this.nyckel = nyckelProvider;
        }
        renderChips();
        renderRows();
    }

    /** Sant om posten redan är vald, jämfört med nyckeln. */
    public boolean isSelectedItem(T item) {
        return ärValdNyckel(nyckel.apply(item));
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
        Object ny = nyckel.apply(item);
        for (int i = 0; i < selected.size(); i++) {
            if (java.util.Objects.equals(nyckel.apply(selected.get(i)), ny)) {
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
        boolean tomt = selected.isEmpty();
        placeholderLabel.setVisible(tomt);
        placeholderLabel.setManaged(tomt);
        chipScroll.setVisible(!tomt);
        chipScroll.setManaged(!tomt);
    }

    private Node chip(T item) {
        Label text = new Label(chipText(item));
        text.getStyleClass().add("multi-select-chip-text");

        Label close = new Label("×");
        close.getStyleClass().add("multi-select-chip-close");
        close.setOnMouseClicked(e -> {
            removeSelectedItem(item);
            e.consume();
        });

        HBox chip = new HBox(6, text, close);
        chip.getStyleClass().add("multi-select-chip");
        chip.setAlignment(Pos.CENTER_LEFT);
        // Chipsen ska hålla sin egen höjd inuti fältet, inte töjas till fältets.
        chip.setMaxHeight(Region.USE_PREF_SIZE);
        chip.setMinHeight(Region.USE_PREF_SIZE);
        return chip;
    }

    private void renderRows() {
        rows.getChildren().clear();
        radPerNyckel.clear();
        kryssPerNyckel.clear();
        if (items.isEmpty()) {
            Label tom = new Label("Inga val");
            tom.getStyleClass().add("multi-select-empty");
            rows.getChildren().add(tom);
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

        Label text = new Label(text(item));
        text.getStyleClass().add("multi-select-row-text");

        VBox labels = new VBox(1, text);
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
        radPerNyckel.put(nyckel.apply(item), row);
        kryssPerNyckel.put(nyckel.apply(item), check);
        return row;
    }

    /** Sant om en post med den nyckeln ligger vald. */
    private boolean ärValdNyckel(Object ny) {
        for (T vald : selected) {
            if (java.util.Objects.equals(nyckel.apply(vald), ny)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Uppdaterar kryssruta och markering på de rader som redan finns, i stället för att bygga om
     * listan. Anropas vid varje val, även medan listan visas.
     */
    private void uppdateraRader() {
        for (java.util.Map.Entry<Object, HBox> e : radPerNyckel.entrySet()) {
            boolean vald = ärValdNyckel(e.getKey());
            CheckBox kryss = kryssPerNyckel.get(e.getKey());
            if (kryss != null) {
                kryss.setSelected(vald);
            }
            if (vald) {
                if (!e.getValue().getStyleClass().contains("selected")) {
                    e.getValue().getStyleClass().add("selected");
                }
            } else {
                e.getValue().getStyleClass().remove("selected");
            }
        }
    }

    private String text(T item) {
        return labelProvider == null ? String.valueOf(item) : labelProvider.apply(item);
    }

    private String chipText(T item) {
        if (chipTextProvider != null) {
            return chipTextProvider.apply(item);
        }
        return text(item);
    }

    // --- listan ---------------------------------------------------------------

    /** Öppnar listan om den är stängd, annars stänger den. */
    public void toggleLista() {
        if (öppen) {
            döljLista();
        } else {
            visaLista();
        }
    }

    /** Öppnar listan. Gör inget om den redan är öppen. */
    public void visaLista() {
        if (öppen || getScene() == null) {
            return;
        }
        renderRows();

        Scene scen = getScene();
        StackPane lager = lager(scen);
        overlay = new Pane();
        overlay.setPickOnBounds(false);
        overlay.setMouseTransparent(false);
        lager.getChildren().add(overlay);

        // Bredden följer det längsta innehållet, så att inget namn klipps eller trycks ihop.
        double textbredd = 0;
        for (T item : items) {
            Text mät = new Text(text(item));
            mät.setFont(Font.font("System", 13));
            textbredd = Math.max(textbredd, mät.getLayoutBounds().getWidth());
        }
        double bredd = Math.round(Math.max(300, Math.min(textbredd + 70, 620)));
        panel.setPrefWidth(bredd);
        panel.setMinWidth(bredd);
        panel.setMaxWidth(bredd);
        rowScroll.setPrefWidth(bredd);
        rowScroll.setMaxWidth(bredd);
        rowScroll.setPrefHeight(Region.USE_COMPUTED_SIZE);
        rowScroll.setMaxHeight(280);

        if (panel.getParent() != overlay) {
            overlay.getChildren().add(panel);
        }
        panel.setManaged(true);
        panel.setVisible(true);
        öppen = true;
        if (!getStyleClass().contains("showing")) {
            getStyleClass().add("showing");
        }

        // Placera listan direkt under fältet, i scenens koordinater, på hela pixlar.
        Bounds b = localToScene(getBoundsInLocal());
        panel.autosize();
        panel.applyCss();
        panel.relocate(Math.round(b.getMinX()), Math.round(b.getMaxY()) + 2);
        panel.toFront();

        klickUtanför = ev -> {
            Node mål = (Node) ev.getTarget();
            if (!ärInuti(mål, panel) && !ärInuti(mål, this)) {
                döljLista();
            }
        };
        escape = ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) {
                döljLista();
            }
        };
        scen.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, klickUtanför);
        scen.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, escape);
    }

    /** Stänger listan om den är öppen. */
    public void döljLista() {
        if (!öppen) {
            return;
        }
        öppen = false;
        getStyleClass().remove("showing");
        if (panel.getParent() instanceof Pane) {
            ((Pane) panel.getParent()).getChildren().remove(panel);
        }
        panel.setVisible(false);
        panel.setManaged(false);
        Scene scen = getScene();
        if (scen != null) {
            if (klickUtanför != null) {
                scen.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, klickUtanför);
            }
            if (escape != null) {
                scen.removeEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, escape);
            }
        }
        if (overlay != null) {
            overlay.getChildren().remove(panel);
            if (overlay.getParent() instanceof StackPane) {
                ((StackPane) overlay.getParent()).getChildren().remove(overlay);
            }
            overlay = null;
        }
        klickUtanför = null;
        escape = null;
    }

    private static boolean ärInuti(Node mål, Node förälder) {
        Node n = mål;
        while (n != null) {
            if (n == förälder) {
                return true;
            }
            n = n.getParent();
        }
        return false;
    }

    /**
     * Ser till att scenens rot ligger i ett lager där listan kan ritas ovanpå vyn, utan att röra
     * resten av layouten. Lagret skapas en gång och återanvänds av alla flervalsfält i scenen.
     */
    private static StackPane lager(Scene scen) {
        Parent rot = scen.getRoot();
        if (rot instanceof StackPane && Boolean.TRUE.equals(rot.getProperties().get(LAGER_NYCKEL))) {
            return (StackPane) rot;
        }
        StackPane lager = new StackPane(rot);
        lager.getProperties().put(LAGER_NYCKEL, Boolean.TRUE);
        lager.setAlignment(Pos.TOP_LEFT);
        scen.setRoot(lager);
        return lager;
    }

    /** Öppnar listan, till exempel från en knapp eller ett test. */
    public void showPopup() {
        visaLista();
    }

    /** Stänger listan. Samma som döljLista. */
    public void hidePopup() {
        döljLista();
    }

    /** Sant när listan är öppen. */
    public boolean isPopupShowing() {
        return öppen;
    }

    /** Roten i listan. Finns för att kunna mätas och granskas utifrån. */
    public VBox getPopupRoot() {
        return panel;
    }
}
