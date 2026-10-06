package com.wac.autocore.ui.navigation;

import com.wac.autocore.ui.i18n.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Sidomenyn: varumärket, sektionerna och språkväxlaren. */
public class SidebarView {

    private final VBox container;
    private final List<Button> navButtons = new ArrayList<Button>();
    private final Map<String, Button> navButtonMap = new LinkedHashMap<String, Button>();
    private final Map<String, Label> navCountLabels = new LinkedHashMap<String, Label>();
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

/** Räknaren på menyvalet. Noll döljer den. */
    public void setNavCount(String key, int count) {
        Label countLabel = navCountLabels.get(key);
        if (countLabel == null) {
            return;
        }
        if (count <= 0) {
            countLabel.setVisible(false);
            return;
        }
        countLabel.setText("+" + count);
        countLabel.setVisible(true);
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

    private Image loadLogoImage() {
        return com.wac.autocore.ui.components.UiComponents.loadLogoImage();
    }

    private VBox buildSidebar() {
        Node brandNode;
        Image logoImg = loadLogoImage();
        if (logoImg != null && !logoImg.isError()) {
            ImageView logoView = new ImageView(logoImg);
            logoView.setPreserveRatio(true);
            logoView.setFitWidth(170);
            logoView.setSmooth(true);

            StackPane logoContainer = new StackPane(logoView);
            logoContainer.setAlignment(Pos.CENTER);
            logoContainer.setPadding(new Insets(25, 25, 30, 25));
            logoContainer.setMinWidth(240);
            logoContainer.setPrefWidth(240);
            logoContainer.setMaxWidth(240);
            brandNode = logoContainer;
        } else {
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
            brandRow.setPadding(new Insets(10, 16, 16, 16));
            brandNode = brandRow;
        }

        VBox nav = new VBox(0);
        nav.setPadding(new Insets(0, 16, 0, 16));
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
        sidebar.setStyle("-fx-padding: 0;");
        sidebar.setMinWidth(240);
        sidebar.setPrefWidth(240);
        sidebar.setMaxWidth(240);
        sidebar.getChildren().addAll(brandNode, navScroll, langToggle);
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
        switchRow.setMinWidth(140);
        switchRow.setPrefWidth(140);
        switchRow.setMaxWidth(140);
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
        t.setCursor(Cursor.DEFAULT);
        groupHeaders.add(new GroupHeader(i18nKey, t));

        HBox head = new HBox(t);
        head.getStyleClass().add("nav-group-head");
        head.setCursor(Cursor.DEFAULT);
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
        b.setCursor(Cursor.HAND);
        b.getStyleClass().addAll("ghost", "nav-item");

        SVGPath icon = createNavIcon(key);
        if (icon != null && icon.getContent() != null && !icon.getContent().isEmpty()) {
            b.setGraphic(icon);
            b.setGraphicTextGap(10);
        }

        b.setOnAction(e -> {
            if (onNavigate != null) {
                onNavigate.accept(key);
            }
        });
        navButtons.add(b);
        if (!"overview".equals(key)) {
            navButtonMap.put(key, b);
        }

        // Räknaren ligger som ett lager ovanpå knappen i stället för i dess innehåll: då behåller
        // etiketten sin formatering och knappens klick- och fokusbeteende är orört.
        Label count = new Label();
        count.getStyleClass().addAll("badge", "info");
        count.setMouseTransparent(true);
        count.setVisible(false);
        navCountLabels.put(key, count);

        StackPane holder = new StackPane(b, count);
        StackPane.setAlignment(count, Pos.CENTER_RIGHT);
        StackPane.setMargin(count, new Insets(0, 10, 0, 0));
        nav.getChildren().add(holder);
        return b;
    }

    private static SVGPath createNavIcon(String key) {
        SVGPath icon = new SVGPath();
        String path;
        switch (key) {
            case "overview":
                // 4-quadrant dashboard layout
                path = "M 1 1 h 5 v 5 h -5 Z M 8 1 h 5 v 5 h -5 Z M 1 8 h 5 v 5 h -5 Z M 8 8 h 5 v 5 h -5 Z";
                break;
            case "customers":
                // User / Customer profile
                path = "M 7 1 a 3 3 0 1 1 0 6 a 3 3 0 0 1 0 -6 Z M 2 13 c 0 -3 2.5 -4.5 5 -4.5 s 5 1.5 5 4.5 v 1 h -10 Z";
                break;
            case "vehicles":
                // Vehicle / Automobile silhouette
                path = "M 2 8 l 2 -5 h 6 l 2 5 h 2 a 1 1 0 0 1 1 1 v 3 a 1 1 0 0 1 -1 1 h -1 a 1.5 1.5 0 0 1 -3 0 h -4 a 1.5 1.5 0 0 1 -3 0 h -1 a 1 1 0 0 1 -1 -1 v -3 a 1 1 0 0 1 1 -1 Z M 4.5 4.5 l -1.2 2.5 h 7.4 l -1.2 -2.5 Z";
                break;
            case "bookings":
                // Calendar with date grid
                path = "M 1 3 a 2 2 0 0 1 2 -2 h 8 a 2 2 0 0 1 2 2 v 9 a 2 2 0 0 1 -2 2 h -8 a 2 2 0 0 1 -2 -2 Z M 2.5 5 h 9 v 7 h -9 Z M 3 0.5 h 1.5 v 2 h -1.5 Z M 9.5 0.5 h 1.5 v 2 h -1.5 Z M 4 7 h 2 v 1.8 h -2 Z M 7.5 7 h 2 v 1.8 h -2 Z M 4 9.5 h 2 v 1.8 h -2 Z M 7.5 9.5 h 2 v 1.8 h -2 Z";
                break;
            case "services":
                // Service catalog / Gear
                path = "M 6 0 h 2 v 2 h -2 Z M 6 12 h 2 v 2 h -2 Z M 0 6 h 2 v 2 h -2 Z M 12 6 h 2 v 2 h -2 Z M 2 2 h 1.8 v 1.8 h -1.8 Z M 10.2 10.2 h 1.8 v 1.8 h -1.8 Z M 2 10.2 h 1.8 v 1.8 h -1.8 Z M 10.2 2 h 1.8 v 1.8 h -1.8 Z M 7 3 a 4 4 0 1 0 0 8 a 4 4 0 0 0 0 -8 Z M 7 5.5 a 1.5 1.5 0 1 1 0 3 a 1.5 1.5 0 0 1 0 -3 Z";
                break;
            case "mechanics":
                // Mechanic / Tool wrench
                path = "M 11.5 0.5 a 3.5 3.5 0 0 0 -3.2 2.1 l -6.5 6.5 a 1.5 1.5 0 0 0 2.1 2.1 l 6.5 -6.5 a 3.5 3.5 0 0 0 2.1 -3.2 l -1.8 1.8 l -1.2 -0.4 l -0.4 -1.2 Z";
                break;
            case "workorders":
                // Work order clipboard checklist
                path = "M 3 2 a 1 1 0 0 1 1 -1 h 6 a 1 1 0 0 1 1 1 v 11 a 1 1 0 0 1 -1 1 h -6 a 1 1 0 0 1 -1 -1 Z M 4.5 4 h 5 v 1.2 h -5 Z M 4.5 6.5 h 5 v 1.2 h -5 Z M 4.5 9 h 3.5 v 1.2 h -3.5 Z M 5 0 h 4 v 1.5 h -4 Z";
                break;
            case "invoices":
                // Invoice / Billing receipt
                path = "M 2 0.5 h 10 v 13 l -1.5 -1 l -1.5 1 l -1.5 -1 l -1.5 1 l -1.5 -1 l -1.5 1 l -1 -0.7 v -12.3 Z M 4 3 h 6 v 1.2 h -6 Z M 4 5.5 h 6 v 1.2 h -6 Z M 4 8 h 4 v 1.2 h -4 Z";
                break;
            case "payments":
                // Payment / Credit card
                path = "M 1 2 a 1.5 1.5 0 0 1 1.5 -1.5 h 9 a 1.5 1.5 0 0 1 1.5 1.5 v 8 a 1.5 1.5 0 0 1 -1.5 1.5 h -9 a 1.5 1.5 0 0 1 -1.5 -1.5 Z M 2 4.5 h 10 v 2 h -10 Z M 3 8 h 2.5 v 1.5 h -2.5 Z M 7 8 h 2 v 1.5 h -2 Z";
                break;
            default:
                path = "";
                break;
        }
        icon.setContent(path);
        icon.getStyleClass().add("nav-icon");
        return icon;
    }
}
