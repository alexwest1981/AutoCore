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
        updateToggleButtons();
    }

    private Label enLabel;
    private Label svLabel;
    private StackPane switchTrack;
    private StackPane switchThumb;

    private void updateToggleButtons() {
        if (switchTrack == null || switchThumb == null) return;
        boolean isSv = I18n.isSwedish();

        // En: thumb to the left (translateX = 0), Sv: thumb to the right (translateX = 20)
        double targetX = isSv ? 20.0 : 0.0;
        try {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                    javafx.util.Duration.millis(140), switchThumb);
            tt.setToX(targetX);
            tt.play();
        } catch (Exception e) {
            switchThumb.setTranslateX(targetX);
        }

        if (isSv) {
            switchTrack.setStyle("-fx-background-color: -wac-accent; -fx-border-color: transparent; -fx-border-radius: 12; -fx-border-width: 1; -fx-background-radius: 12; -fx-cursor: hand;");
            svLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 12px;");
            enLabel.setStyle("-fx-text-fill: #71717a; -fx-font-weight: normal; -fx-font-size: 12px;");
        } else {
            switchTrack.setStyle("-fx-background-color: #3f3f46; -fx-border-color: rgba(255,255,255,0.25); -fx-border-radius: 12; -fx-border-width: 1; -fx-background-radius: 12; -fx-cursor: hand;");
            enLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 12px;");
            svLabel.setStyle("-fx-text-fill: #71717a; -fx-font-weight: normal; -fx-font-size: 12px;");
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

        VBox nav = new VBox(0);
        nav.setPadding(new Insets(14, 0, 0, 0));
        overviewBtn = addNav(nav, "overview", I18n.get("nav.section.overview"));

        VBox groups = new VBox(22);
        groups.setPadding(new Insets(18, 0, 16, 0));
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
        sidebar.setMinWidth(240);
        sidebar.setPrefWidth(240);
        sidebar.setMaxWidth(240);
        sidebar.getChildren().addAll(brandRow, navScroll, langToggle);
        return sidebar;
    }

    private HBox buildLanguageToggle() {
        enLabel = new Label("EN");
        enLabel.getStyleClass().add("lang-switch-label");
        enLabel.setMinWidth(26);
        enLabel.setPrefWidth(26);
        enLabel.setMaxWidth(26);
        enLabel.setAlignment(Pos.CENTER);

        svLabel = new Label("SV");
        svLabel.getStyleClass().add("lang-switch-label");
        svLabel.setMinWidth(26);
        svLabel.setPrefWidth(26);
        svLabel.setMaxWidth(26);
        svLabel.setAlignment(Pos.CENTER);

        switchThumb = new StackPane();
        switchThumb.getStyleClass().add("lang-switch-thumb");
        switchThumb.setPrefSize(18, 18);
        switchThumb.setMinSize(18, 18);
        switchThumb.setMaxSize(18, 18);
        switchThumb.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 9; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.35), 3, 0, 0, 1);");

        switchTrack = new StackPane(switchThumb);
        switchTrack.getStyleClass().add("lang-switch-track");
        switchTrack.setPrefSize(44, 24);
        switchTrack.setMinSize(44, 24);
        switchTrack.setMaxSize(44, 24);
        switchTrack.setAlignment(Pos.CENTER_LEFT);
        switchTrack.setPadding(new Insets(3));

        updateToggleButtons();

        HBox switchRow = new HBox(10, enLabel, switchTrack, svLabel);
        switchRow.getStyleClass().add("lang-switch-row");
        switchRow.setAlignment(Pos.CENTER);
        switchRow.setMinWidth(124);
        switchRow.setPrefWidth(124);
        switchRow.setMaxWidth(124);
        switchRow.setCursor(Cursor.HAND);
        switchRow.setOnMouseClicked(e -> {
            I18n.setLanguage(I18n.isSwedish() ? I18n.LANG_EN : I18n.LANG_SV);
        });

        HBox container = new HBox(switchRow);
        container.getStyleClass().add("sidebar-lang-container");
        container.setAlignment(Pos.CENTER);
        container.setMinWidth(240);
        container.setPrefWidth(240);
        container.setMaxWidth(240);
        return container;
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

        HBox head = new HBox(t);
        head.getStyleClass().add("nav-group-head");
        head.setPadding(new Insets(0, 14, 6, 14));

        VBox list = new VBox(2);
        for (NavSpec s : items) {
            addNav(list, s.key, I18n.get(s.i18nKey));
        }

        VBox groupBox = new VBox(0, head, list);
        groupBox.getStyleClass().add("nav-group-box");
        parent.getChildren().add(groupBox);
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
