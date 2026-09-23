package com.wac.autocore.ui.navigation;

import com.wac.autocore.ui.i18n.I18n;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Horisontell navigeringsmeny i toppbaren för Wigell AutoCore.
 * Används när applikationen körs i "Top bar"-läge.
 */
public class TopNavView {

    private final HBox container;
    private final List<Button> navButtons = new ArrayList<Button>();
    private final Map<Button, String> buttonKeys = new LinkedHashMap<Button, String>();
    private final Consumer<String> onNavigate;

    public TopNavView(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
        this.container = buildNav();
        I18n.addListener(lang -> refreshTexts());
    }

    public HBox getView() {
        return container;
    }

    public void refreshTexts() {
        for (Map.Entry<Button, String> entry : buttonKeys.entrySet()) {
            Button b = entry.getKey();
            String key = entry.getValue();
            if ("overview".equals(key)) {
                b.setText(I18n.get("nav.section.overview"));
            } else {
                b.setText(I18n.get("nav.item." + key));
            }
        }
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
        addNav(nav, "overview", I18n.get("nav.section.overview"));
        addSeparator(nav);

        // Kunder, Fordon & Bokningar
        addNav(nav, "customers", I18n.get("nav.item.customers"));
        addNav(nav, "vehicles", I18n.get("nav.item.vehicles"));
        addNav(nav, "bookings", I18n.get("nav.item.bookings"));
        addSeparator(nav);

        // Verkstad (Tjänster, Mekaniker, Arbetsordrar)
        addNav(nav, "services", I18n.get("nav.item.services"));
        addNav(nav, "mechanics", I18n.get("nav.item.mechanics"));
        addNav(nav, "workorders", I18n.get("nav.item.workorders"));
        addSeparator(nav);

        // Ekonomi (Fakturor, Betalningar)
        addNav(nav, "invoices", I18n.get("nav.item.invoices"));
        addNav(nav, "payments", I18n.get("nav.item.payments"));

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
        buttonKeys.put(b, key);
        nav.getChildren().add(b);
    }
}
