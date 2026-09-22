package com.wac.autocore.ui.navigation;

import com.wac.autocore.ui.i18n.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Sidomeny med varumärkesikon, kollapsbara sektioner för navigation
 * och språkväxlare (SV/EN) i botten.
 */
public class SidebarView {

    private final VBox container;
    private final List<Button> navButtons = new ArrayList<Button>();
    private final Map<String, Button> navButtonMap = new LinkedHashMap<String, Button>();
    private final List<GroupHeader> groupHeaders = new ArrayList<GroupHeader>();
    private final Consumer<String> onNavigate;

    private Label brandSub;
    private Button overviewBtn;
    private Label langToggleLabel;
    private Button svToggleBtn;
    private Button enToggleBtn;

    public SidebarView(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
        this.container = buildSidebar();
        I18n.addListener(lang -> refreshTexts());
    }

    public VBox getView() {
        return container;
    }

    public void setSelectedPage(String key) {
        for (Button b : navButtons) {
            b.getStyleClass().remove("selected");
            if (key != null && key.equals(b.getUserData())) {
                b.getStyleClass().add("selected");
            }
        }
    }

    public void refreshTexts() {
        if (brandSub != null) {
            brandSub.setText(I18n.get("nav.brand.subtitle"));
        }
        if (overviewBtn != null) {
            overviewBtn.setText(I18n.get("nav.section.overview"));
        }
        for (GroupHeader g : groupHeaders) {
            g.label.setText(I18n.get(g.i18nKey).toUpperCase());
        }
        for (Map.Entry<String, Button> entry : navButtonMap.entrySet()) {
            entry.getValue().setText(I18n.get("nav.item." + entry.getKey()));
        }
        if (langToggleLabel != null) {
            langToggleLabel.setText(I18n.get("nav.lang.toggle_label"));
        }
        updateToggleButtons();
    }

    private void updateToggleButtons() {
        if (svToggleBtn == null || enToggleBtn == null) return;
        boolean isSv = I18n.isSwedish();
        svToggleBtn.getStyleClass().remove("active");
        enToggleBtn.getStyleClass().remove("active");
        if (isSv) {
            svToggleBtn.getStyleClass().add("active");
        } else {
            enToggleBtn.getStyleClass().add("active");
        }
    }

    private VBox buildSidebar() {
        StackPane mark = new StackPane();
        mark.getStyleClass().add("brand-mark");
        mark.setPrefSize(38, 38);
        Label letter = new Label("AC");
        letter.getStyleClass().add("letter");
        mark.getChildren().add(letter);

        VBox brandTitles = new VBox(2);
        Label brand = new Label(I18n.get("nav.brand.title"));
        brand.getStyleClass().add("brand-title");
        brandSub = new Label(I18n.get("nav.brand.subtitle"));
        brandSub.getStyleClass().add("brand-sub");
        brandTitles.getChildren().addAll(brand, brandSub);

        HBox brandRow = new HBox(12, mark, brandTitles);
        brandRow.getStyleClass().add("brand-row");
        brandRow.setAlignment(Pos.CENTER_LEFT);

        VBox nav = new VBox(3);
        nav.setPadding(new Insets(14, 0, 0, 0));
        overviewBtn = addNav(nav, "overview", I18n.get("nav.section.overview"));

        VBox groups = new VBox(2);
        addGroup(groups, "nav.section.customers", navItem("customers", "nav.item.customers"));
        addGroup(groups, "nav.section.vehicles", navItem("vehicles", "nav.item.vehicles"));
        addGroup(groups, "nav.section.bookings", navItem("bookings", "nav.item.bookings"));
        addGroup(groups, "nav.section.workshop",
                navItem("services", "nav.item.services"),
                navItem("mechanics", "nav.item.mechanics"),
                navItem("workorders", "nav.item.workorders"));
        addGroup(groups, "nav.section.finance",
                navItem("invoices", "nav.item.invoices"),
                navItem("payments", "nav.item.payments"));
        nav.getChildren().add(groups);

        ScrollPane navScroll = new ScrollPane(nav);
        navScroll.setFitToWidth(true);
        navScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        navScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        navScroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(navScroll, Priority.ALWAYS);

        HBox langToggle = buildLanguageToggle();

        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(236);
        sidebar.setMinWidth(200);
        sidebar.getChildren().addAll(brandRow, navScroll, langToggle);
        return sidebar;
    }

    private HBox buildLanguageToggle() {
        langToggleLabel = new Label(I18n.get("nav.lang.toggle_label"));
        langToggleLabel.getStyleClass().add("lang-toggle-label");

        svToggleBtn = new Button("SV");
        svToggleBtn.getStyleClass().addAll("lang-btn", "lang-btn-sv");
        svToggleBtn.setOnAction(e -> I18n.setLanguage(I18n.LANG_SV));

        enToggleBtn = new Button("EN");
        enToggleBtn.getStyleClass().addAll("lang-btn", "lang-btn-en");
        enToggleBtn.setOnAction(e -> I18n.setLanguage(I18n.LANG_EN));

        updateToggleButtons();

        HBox togglePill = new HBox(2, svToggleBtn, enToggleBtn);
        togglePill.getStyleClass().add("lang-toggle-pill");
        togglePill.setAlignment(Pos.CENTER);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(8, langToggleLabel, spacer, togglePill);
        row.getStyleClass().add("sidebar-lang-container");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static class NavSpec {
        final String key;
        final String i18nKey;
        NavSpec(String key, String i18nKey) {
            this.key = key;
            this.i18nKey = i18nKey;
        }
    }

    private static NavSpec navItem(String key, String i18nKey) {
        return new NavSpec(key, i18nKey);
    }

    private static class GroupHeader {
        final String i18nKey;
        final Label label;
        GroupHeader(String i18nKey, Label label) {
            this.i18nKey = i18nKey;
            this.label = label;
        }
    }

    private void addGroup(VBox parent, String i18nKey, NavSpec... items) {
        Label t = new Label(I18n.get(i18nKey).toUpperCase());
        t.getStyleClass().add("side-label");
        groupHeaders.add(new GroupHeader(i18nKey, t));

        Label chev = new Label("\u25BE");
        chev.getStyleClass().add("side-label");

        Region spr = new Region();
        HBox.setHgrow(spr, Priority.ALWAYS);

        HBox head = new HBox(6, t, spr, chev);
        head.setPadding(new Insets(10, 12, 4, 14));
        head.setCursor(Cursor.HAND);

        VBox list = new VBox(2);
        for (NavSpec s : items) {
            addNav(list, s.key, I18n.get(s.i18nKey));
        }

        head.setOnMouseClicked(e -> {
            boolean show = !list.isVisible();
            list.setVisible(show);
            list.setManaged(show);
            chev.setText(show ? "\u25BE" : "\u25B8");
        });

        parent.getChildren().add(new VBox(1, head, list));
    }

    private Button addNav(VBox nav, String key, String label) {
        Button b = new Button(label);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setUserData(key);
        b.getStyleClass().addAll("ghost", "nav-item");
        b.setOnAction(e -> {
            if (onNavigate != null) {
                onNavigate.accept(key);
            }
        });
        navButtons.add(b);
        if (!"overview".equals(key)) {
            navButtonMap.put(key, b);
        }
        nav.getChildren().add(b);
        return b;
    }
}
