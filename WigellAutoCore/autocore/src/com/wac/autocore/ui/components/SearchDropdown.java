package com.wac.autocore.ui.components;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.navigation.PageRouter;
import com.wac.autocore.ui.util.EntityLookup;
import com.wac.autocore.ui.util.GlobalSearch;
import com.wac.autocore.ui.util.GlobalSearch.SearchResults;
import com.wac.autocore.ui.util.UiFormatters;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.util.List;

/**
 * Interaktiv sök-dropdown som fälls ut direkt under sökrutan i TopBar.
 * Visar kategoriserade träffar i realtid (kunder, fordon, mekaniker etc.)
 * när användaren skriver in tecken (t.ex. "A").
 */
public final class SearchDropdown {

    private static final int MAX_ITEMS_PER_SECTION = 4;
    private static final double DROPDOWN_MIN_WIDTH = 380.0;
    private static final double DROPDOWN_MAX_HEIGHT = 420.0;

    private final TextField searchField;
    private final GarageSystem garage;
    private final PageRouter router;
    private final Popup popup;
    private final VBox container;
    private final ScrollPane scrollPane;

    public static SearchDropdown attach(TextField searchField, GarageSystem garage, PageRouter router) {
        return new SearchDropdown(searchField, garage, router);
    }

    private SearchDropdown(TextField searchField, GarageSystem garage, PageRouter router) {
        this.searchField = searchField;
        this.garage = garage;
        this.router = router;

        this.popup = new Popup();
        this.popup.setAutoHide(true);
        this.popup.setHideOnEscape(true);

        this.container = new VBox(0);
        this.container.getStyleClass().addAll("root", "search-dropdown");

        this.scrollPane = new ScrollPane(container);
        this.scrollPane.setFitToWidth(true);
        this.scrollPane.setMaxHeight(DROPDOWN_MAX_HEIGHT);
        this.scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        this.popup.getContent().add(scrollPane);

        setupListeners();
    }

    private void setupListeners() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                hide();
            } else {
                updateAndShow(newVal.trim());
            }
        });

        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                hide();
            } else if (event.getCode() == KeyCode.ENTER) {
                hide();
                if (router != null && searchField.getText() != null && !searchField.getText().trim().isEmpty()) {
                    router.navigate("search");
                }
            }
        });

        searchField.focusedProperty().addListener((obs, oldF, newF) -> {
            if (!newF && !popup.isFocused()) {
                hide();
            }
        });
    }

    public void hide() {
        if (popup.isShowing()) {
            popup.hide();
        }
    }

    private void updateAndShow(String query) {
        if (garage == null || searchField.getScene() == null) {
            return;
        }

        Window window = searchField.getScene().getWindow();
        if (window == null || !window.isShowing()) {
            return;
        }

        boolean dark = com.wac.autocore.theme.ThemeManager.isCurrentDark();
        String solidBg = dark ? "#1f1f23" : "#ffffff";
        String solidBorder = dark ? "#2e2e32" : "#cbd5e1";

        scrollPane.setStyle(
                "-fx-background-color: " + solidBg + "; " +
                "-fx-background: " + solidBg + "; " +
                "-fx-border-color: " + solidBorder + "; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.30), 18, 0.2, 0, 5); " +
                "-fx-padding: 0;"
        );
        container.setStyle("-fx-background-color: " + solidBg + "; -fx-background-radius: 8px;");

        SearchResults results = GlobalSearch.search(garage, query);
        populateResults(results, query, dark, solidBg, solidBorder);

        double width = Math.max(searchField.getWidth(), DROPDOWN_MIN_WIDTH);
        scrollPane.setPrefWidth(width);
        container.setPrefWidth(width);

        Point2D screenCoords = searchField.localToScreen(0, searchField.getHeight());
        if (screenCoords == null) {
            return;
        }

        double x = screenCoords.getX();
        double y = screenCoords.getY() + 4;

        if (!popup.isShowing()) {
            popup.show(window, x, y);
        } else {
            popup.setX(x);
            popup.setY(y);
        }

        // Synka stylesheets med appen
        if (popup.getScene() != null && searchField.getScene() != null) {
            popup.getScene().getStylesheets().setAll(searchField.getScene().getStylesheets());
            if (!popup.getScene().getRoot().getStyleClass().contains("root")) {
                popup.getScene().getRoot().getStyleClass().add("root");
            }
        }
    }

    private void populateResults(SearchResults results, String query, boolean dark, String solidBg, String solidBorder) {
        container.getChildren().clear();

        String headerBg = dark ? "#18181b" : "#f1f5f9";
        String hoverBg = dark ? "#27272a" : "#f1f5f9";
        String textColor = dark ? "#e4e4e7" : "#0f172a";
        String mutedColor = dark ? "#71717a" : "#64748b";
        String accentColor = dark ? "#a1a1aa" : "#2563eb";

        if (results.isEmpty()) {
            VBox emptyBox = new VBox(6);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(20, 16, 20, 16));
            emptyBox.setStyle("-fx-background-color: " + solidBg + ";");
            Label l = new Label(I18n.get("search.category.empty_query", query));
            l.setStyle("-fx-text-fill: " + mutedColor + "; -fx-font-size: 13px;");
            emptyBox.getChildren().add(l);
            container.getChildren().add(emptyBox);
            return;
        }

        // 1. KUNDER
        if (!results.getCustomers().isEmpty()) {
            container.getChildren().add(createCategoryHeader(I18n.get("search.category.customers"), results.getCustomers().size(), headerBg, solidBorder, mutedColor));
            List<Customer> list = results.getCustomers();
            int limit = Math.min(list.size(), MAX_ITEMS_PER_SECTION);
            for (int i = 0; i < limit; i++) {
                Customer c = list.get(i);
                container.getChildren().add(createItemRow(
                        I18n.get("search.badge.customer"), "info",
                        c.getName() + (c.isVip() ? " ★ VIP" : ""),
                        c.getPhone() + " • " + c.getEmail(),
                        solidBg, hoverBg, solidBorder, textColor, mutedColor,
                        () -> {
                            hide();
                            if (router != null) {
                                router.navigate("customers");
                                router.applySearch(c.getName());
                            }
                        }
                ));
            }
        }

        // 2. FORDON
        if (!results.getVehicles().isEmpty()) {
            container.getChildren().add(createCategoryHeader(I18n.get("search.category.vehicles"), results.getVehicles().size(), headerBg, solidBorder, mutedColor));
            List<Vehicle> list = results.getVehicles();
            int limit = Math.min(list.size(), MAX_ITEMS_PER_SECTION);
            for (int i = 0; i < limit; i++) {
                Vehicle v = list.get(i);
                String owner = EntityLookup.customerName(garage, v.getCustomerId());
                container.getChildren().add(createItemRow(
                        I18n.get("search.badge.vehicle"), "success",
                        v.getBrand() + " " + v.getModel() + " (" + v.getYear() + ")",
                        "Reg: " + v.getRegistrationNumber() + " • " + I18n.get("search.category.owner") + " " + owner,
                        solidBg, hoverBg, solidBorder, textColor, mutedColor,
                        () -> {
                            hide();
                            if (router != null) {
                                router.navigate("vehicles");
                                router.applySearch(v.getRegistrationNumber());
                            }
                        }
                ));
            }
        }

        // 3. MEKANIKER
        if (!results.getMechanics().isEmpty()) {
            container.getChildren().add(createCategoryHeader(I18n.get("search.category.mechanics"), results.getMechanics().size(), headerBg, solidBorder, mutedColor));
            List<Mechanic> list = results.getMechanics();
            int limit = Math.min(list.size(), MAX_ITEMS_PER_SECTION);
            for (int i = 0; i < limit; i++) {
                Mechanic m = list.get(i);
                container.getChildren().add(createItemRow(
                        I18n.get("search.badge.mechanic"), "warn",
                        m.getName(),
                        m.getSpecialization() + " • " + (m.isAvailable() ? I18n.get("table.col.available") : I18n.get("table.col.unavailable")),
                        solidBg, hoverBg, solidBorder, textColor, mutedColor,
                        () -> {
                            hide();
                            if (router != null) {
                                router.navigate("mechanics");
                                router.applySearch(m.getName());
                            }
                        }
                ));
            }
        }

        // 4. BOKNINGAR
        if (!results.getBookings().isEmpty()) {
            container.getChildren().add(createCategoryHeader(I18n.get("search.category.bookings"), results.getBookings().size(), headerBg, solidBorder, mutedColor));
            List<Booking> list = results.getBookings();
            int limit = Math.min(list.size(), MAX_ITEMS_PER_SECTION);
            for (int i = 0; i < limit; i++) {
                Booking b = list.get(i);
                String veh = EntityLookup.bookingVehicleReg(garage, b.getId());
                container.getChildren().add(createItemRow(
                        I18n.get("search.badge.booking"), "info",
                        I18n.get("table.col.booking") + " #" + b.getId() + " - " + b.getDescription(),
                        b.getDate() + " • " + veh,
                        solidBg, hoverBg, solidBorder, textColor, mutedColor,
                        () -> {
                            hide();
                            if (router != null) {
                                router.navigate("bookings");
                                router.applySearch(String.valueOf(b.getId()));
                            }
                        }
                ));
            }
        }

        // 5. ARBETSORDRAR
        if (!results.getWorkOrders().isEmpty()) {
            container.getChildren().add(createCategoryHeader(I18n.get("search.category.workorders"), results.getWorkOrders().size(), headerBg, solidBorder, mutedColor));
            List<WorkOrder> list = results.getWorkOrders();
            int limit = Math.min(list.size(), MAX_ITEMS_PER_SECTION);
            for (int i = 0; i < limit; i++) {
                WorkOrder wo = list.get(i);
                String cust = EntityLookup.workOrderCustomerName(garage, wo);
                String veh = EntityLookup.workOrderVehicleReg(garage, wo);
                container.getChildren().add(createItemRow(
                        I18n.get("search.badge.workorder"), "accent",
                        I18n.get("table.col.workorder") + " #" + wo.getId() + " (" + UiFormatters.statusWord(wo.getStatus()) + ")",
                        cust + " • " + veh,
                        solidBg, hoverBg, solidBorder, textColor, mutedColor,
                        () -> {
                            hide();
                            if (router != null) {
                                router.navigate("workorders");
                                router.applySearch(String.valueOf(wo.getId()));
                            }
                        }
                ));
            }
        }

        // 6. TJÄNSTER
        if (!results.getServices().isEmpty()) {
            container.getChildren().add(createCategoryHeader(I18n.get("search.category.services"), results.getServices().size(), headerBg, solidBorder, mutedColor));
            List<ServiceItem> list = results.getServices();
            int limit = Math.min(list.size(), MAX_ITEMS_PER_SECTION);
            for (int i = 0; i < limit; i++) {
                ServiceItem s = list.get(i);
                container.getChildren().add(createItemRow(
                        I18n.get("search.badge.service"), "muted",
                        s.getName() + " (" + UiFormatters.formatMoney(s.getPrice()) + ")",
                        s.getEstimatedMinutes() + " min • " + s.getDescription(),
                        solidBg, hoverBg, solidBorder, textColor, mutedColor,
                        () -> {
                            hide();
                            if (router != null) {
                                router.navigate("services");
                                router.applySearch(s.getName());
                            }
                        }
                ));
            }
        }

        // 7. FAKTUROR
        if (!results.getInvoices().isEmpty()) {
            container.getChildren().add(createCategoryHeader(I18n.get("search.category.invoices"), results.getInvoices().size(), headerBg, solidBorder, mutedColor));
            List<Invoice> list = results.getInvoices();
            int limit = Math.min(list.size(), MAX_ITEMS_PER_SECTION);
            for (int i = 0; i < limit; i++) {
                Invoice inv = list.get(i);
                String cust = EntityLookup.invoiceCustomerName(garage, inv);
                container.getChildren().add(createItemRow(
                        I18n.get("search.badge.invoice"), "warn",
                        I18n.get("table.col.invoice") + " #" + inv.getId() + " (" + UiFormatters.formatMoney(inv.getTotalAmount()) + ")",
                        cust + " • " + (inv.isPaid() ? I18n.get("status.paid") : I18n.get("status.unpaid")),
                        solidBg, hoverBg, solidBorder, textColor, mutedColor,
                        () -> {
                            hide();
                            if (router != null) {
                                router.navigate("invoices");
                                router.applySearch(String.valueOf(inv.getId()));
                            }
                        }
                ));
            }
        }

        // Footer: Full sökvy
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 14, 10, 14));
        footer.setStyle("-fx-background-color: " + headerBg + "; -fx-border-color: " + solidBorder + "; -fx-border-width: 1 0 0 0; -fx-cursor: hand;");
        Label footerLbl = new Label(I18n.get("search.category.view_all_matches", results.getTotalMatches()));
        footerLbl.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-weight: bold; -fx-font-size: 12px;");
        footer.getChildren().add(footerLbl);
        footer.setOnMouseEntered(e -> footer.setStyle("-fx-background-color: " + hoverBg + "; -fx-border-color: " + solidBorder + "; -fx-border-width: 1 0 0 0; -fx-cursor: hand;"));
        footer.setOnMouseExited(e -> footer.setStyle("-fx-background-color: " + headerBg + "; -fx-border-color: " + solidBorder + "; -fx-border-width: 1 0 0 0; -fx-cursor: hand;"));
        footer.setOnMouseClicked(e -> {
            hide();
            if (router != null) {
                router.navigate("search");
            }
        });
        container.getChildren().add(footer);
    }

    private Node createCategoryHeader(String title, int count, String headerBg, String solidBorder, String mutedColor) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(7, 14, 7, 14));
        box.setStyle("-fx-background-color: " + headerBg + "; -fx-border-color: " + solidBorder + "; -fx-border-width: 0 0 1 0;");

        Label lbl = new Label(title.toUpperCase() + " (" + count + ")");
        lbl.setStyle("-fx-text-fill: " + mutedColor + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 0.5px;");
        box.getChildren().add(lbl);
        return box;
    }

    private Node createItemRow(String badgeText, String badgeType, String primaryText, String secondaryText,
                              String solidBg, String hoverBg, String solidBorder, String textColor, String mutedColor,
                              Runnable onClick) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 14, 8, 14));
        row.setStyle("-fx-cursor: hand; -fx-background-color: " + solidBg + "; -fx-border-color: " + solidBorder + "; -fx-border-width: 0 0 1 0;");

        Label badge = new Label(badgeText);
        badge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 4; -fx-border-radius: 4;");
        if ("success".equals(badgeType)) {
            badge.setStyle(badge.getStyle() + "-fx-background-color: #dcfce7; -fx-text-fill: #15803d;");
        } else if ("warn".equals(badgeType)) {
            badge.setStyle(badge.getStyle() + "-fx-background-color: #fef3c7; -fx-text-fill: #b45309;");
        } else if ("accent".equals(badgeType)) {
            badge.setStyle(badge.getStyle() + "-fx-background-color: #e0e7ff; -fx-text-fill: #4338ca;");
        } else {
            badge.setStyle(badge.getStyle() + "-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1;");
        }

        VBox textCol = new VBox(2);
        Label prim = new Label(primaryText);
        prim.setStyle("-fx-text-fill: " + textColor + "; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label sec = new Label(secondaryText);
        sec.setStyle("-fx-text-fill: " + mutedColor + "; -fx-font-size: 11px;");
        textCol.getChildren().addAll(prim, sec);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        Label arrow = new Label("→");
        arrow.setStyle("-fx-text-fill: " + mutedColor + "; -fx-font-size: 12px;");

        row.getChildren().addAll(badge, textCol, arrow);

        row.setOnMouseEntered(e -> row.setStyle("-fx-cursor: hand; -fx-background-color: " + hoverBg + "; -fx-border-color: " + solidBorder + "; -fx-border-width: 0 0 1 0;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-cursor: hand; -fx-background-color: " + solidBg + "; -fx-border-color: " + solidBorder + "; -fx-border-width: 0 0 1 0;"));
        row.setOnMouseClicked(e -> {
            if (onClick != null) {
                onClick.run();
            }
        });

        return row;
    }
}
