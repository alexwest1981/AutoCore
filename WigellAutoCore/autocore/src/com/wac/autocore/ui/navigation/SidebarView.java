package com.wac.autocore.ui.navigation;

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
import java.util.List;
import java.util.function.Consumer;

/**
 * Sidomeny med varumärkesikon och kollapsbara sektioner för navigation.
 */
public class SidebarView {

    private final VBox container;
    private final List<Button> navButtons = new ArrayList<Button>();
    private final Consumer<String> onNavigate;

    public SidebarView(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
        this.container = buildSidebar();
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

    private VBox buildSidebar() {
        StackPane mark = new StackPane();
        mark.getStyleClass().add("brand-mark");
        mark.setPrefSize(38, 38);
        Label letter = new Label("AC");
        letter.getStyleClass().add("letter");
        mark.getChildren().add(letter);

        VBox brandTitles = new VBox(2);
        Label brand = new Label("AutoCore");
        brand.getStyleClass().add("brand-title");
        Label brandSub = new Label("Workshop System");
        brandSub.getStyleClass().add("brand-sub");
        brandTitles.getChildren().addAll(brand, brandSub);

        HBox brandRow = new HBox(12, mark, brandTitles);
        brandRow.getStyleClass().add("brand-row");
        brandRow.setAlignment(Pos.CENTER_LEFT);

        VBox nav = new VBox(3);
        nav.setPadding(new Insets(14, 0, 0, 0));
        addNav(nav, "overview", "Overview");

        VBox groups = new VBox(2);
        addGroup(groups, "Customers", navItem("customers", "Show customers"));
        addGroup(groups, "Vehicles", navItem("vehicles", "Show vehicles"));
        addGroup(groups, "Bookings", navItem("bookings", "Show bookings"));
        addGroup(groups, "Workshop",
                navItem("services", "Show services"),
                navItem("mechanics", "Show mechanics"),
                navItem("workorders", "Show work orders"));
        addGroup(groups, "Finance",
                navItem("invoices", "Show invoices"),
                navItem("payments", "Show payments"));
        nav.getChildren().add(groups);

        ScrollPane navScroll = new ScrollPane(nav);
        navScroll.setFitToWidth(true);
        navScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        navScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        navScroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(navScroll, Priority.ALWAYS);

        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(236);
        sidebar.setMinWidth(200);
        sidebar.getChildren().addAll(brandRow, navScroll);
        return sidebar;
    }

    private static class NavSpec {
        final String key;
        final String label;
        NavSpec(String key, String label) {
            this.key = key;
            this.label = label;
        }
    }

    private static NavSpec navItem(String key, String label) {
        return new NavSpec(key, label);
    }

    private void addGroup(VBox parent, String title, NavSpec... items) {
        Label t = new Label(title.toUpperCase());
        t.getStyleClass().add("side-label");
        Label chev = new Label("\u25BE");
        chev.getStyleClass().add("side-label");

        Region spr = new Region();
        HBox.setHgrow(spr, Priority.ALWAYS);

        HBox head = new HBox(6, t, spr, chev);
        head.setPadding(new Insets(10, 12, 4, 14));
        head.setCursor(Cursor.HAND);

        VBox list = new VBox(2);
        for (NavSpec s : items) {
            addNav(list, s.key, s.label);
        }

        head.setOnMouseClicked(e -> {
            boolean show = !list.isVisible();
            list.setVisible(show);
            list.setManaged(show);
            chev.setText(show ? "\u25BE" : "\u25B8");
        });

        parent.getChildren().add(new VBox(1, head, list));
    }

    private void addNav(VBox nav, String key, String label) {
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
        nav.getChildren().add(b);
    }
}
