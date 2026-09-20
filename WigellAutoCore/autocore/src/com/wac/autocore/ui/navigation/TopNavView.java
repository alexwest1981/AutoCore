package com.wac.autocore.ui.navigation;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Horisontell navigeringsmeny i toppbaren för Wigell AutoCore.
 * Används när applikationen körs i "Top bar"-läge.
 */
public class TopNavView {

    private final HBox container;
    private final List<Button> navButtons = new ArrayList<Button>();
    private final Consumer<String> onNavigate;

    public TopNavView(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
        this.container = buildNav();
    }

    public HBox getView() {
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

    private HBox buildNav() {
        HBox nav = new HBox(6);
        nav.getStyleClass().add("top-nav-bar");
        nav.setAlignment(Pos.CENTER_LEFT);

        // Översikt
        addNav(nav, "overview", "Overview");
        addSeparator(nav);

        // Kunder, Fordon & Bokningar
        addNav(nav, "customers", "Customers");
        addNav(nav, "vehicles", "Vehicles");
        addNav(nav, "bookings", "Bookings");
        addSeparator(nav);

        // Verkstad (Tjänster, Mekaniker, Arbetsordrar)
        addNav(nav, "services", "Services");
        addNav(nav, "mechanics", "Mechanics");
        addNav(nav, "workorders", "Work Orders");
        addSeparator(nav);

        // Ekonomi (Fakturor, Betalningar)
        addNav(nav, "invoices", "Invoices");
        addNav(nav, "payments", "Payments");

        return nav;
    }

    private void addSeparator(HBox nav) {
        Region sep = new Region();
        sep.getStyleClass().add("top-nav-separator");
        nav.getChildren().add(sep);
    }

    private void addNav(HBox nav, String key, String label) {
        Button b = new Button(label);
        b.setUserData(key);
        b.getStyleClass().add("top-nav-item");
        b.setOnAction(e -> {
            if (onNavigate != null) {
                onNavigate.accept(key);
            }
        });
        navButtons.add(b);
        nav.getChildren().add(b);
    }
}
