package com.wac.autocore.ui.components;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
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
import javafx.scene.layout.Region;
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
        this.container.getStyleClass().add("root");
        this.container.setStyle(
                "-fx-background-color: -wac-card; " +
                "-fx-border-color: -wac-line; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.30), 16, 0.2, 0, 4);"
        );

        this.scrollPane = new ScrollPane(container);
        this.scrollPane.setFitToWidth(true);
        this.scrollPane.setMaxHeight(DROPDOWN_MAX_HEIGHT);
        this.scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        this.scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");

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

        SearchResults results = GlobalSearch.search(garage, query);
        populateResults(results, query);

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

    private void populateResults(SearchResults results, String query) {
        container.getChildren().clear();

        if (results.isEmpty()) {
            VBox emptyBox = new VBox(6);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(20, 16, 20, 16));
            Label l = new Label("Inga träffar för \"" + query + "\"");
            l.setStyle("-fx-text-fill: -wac-muted; -fx-font-size: 13px;");
            emptyBox.getChildren().add(l);
            container.getChildren().add(emptyBox);
            return;
        }

        // 1. KUNDER
        if (!results.getCustomers().isEmpty()) {
            container.getChildren().add(createCategoryHeader("Kunder", results.getCustomers().size()));
            List<Customer> list = results.getCustomers();
            int limit = Math.min(list.size(), MAX_ITEMS_PER_SECTION);
            for (int i = 0; i < limit; i++) {
                Customer c = list.get(i);
                container.getChildren().add(createItemRow(
                        "KUND", "info",
                        c.getName() + (c.isVip() ? " ★ VIP" : ""),
                        c.getPhone() + " • " + c.getEmail(),
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
            container.getChildren().add(createCategoryHeader("Fordon", results.getVehicles().size()));
            List<Vehicle> list = results.getVehicles();
            int limit = Math.min(list.size(), MAX_ITEMS_PER_SECTION);
            for (int i = 0; i < limit; i++) {
                Vehicle v = list.get(i);
                String owner = EntityLookup.customerName(garage, v.getCustomerId());
                container.getChildren().add(createItemRow(
                        "FORDON", "success",
                        v.getBrand() + " " + v.getModel() + " (" + v.getYear() + ")",
                        "Reg: " + v.getRegistrationNumber() + " • Ägare: " + owner,
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
            container.getChildren().add(createCategoryHeader("Mekaniker", results.getMechanics().size()));
            List<Mechanic> list = results.getMechanics();
            int limit = Math.min(list.size(), MAX_ITEMS_PER_SECTION);
            for (int i = 0; i < limit; i++) {
                Mechanic m = list.get(i);
                container.getChildren().add(createItemRow(
                        "MEK", "warn",
                        m.getName(),
                        m.getSpecialization() + " • " + (m.isAvailable() ? "Tillgänglig" : "Upptagen"),
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
            container.getChildren().add(createCategoryHeader("Bokningar", results.getBookings().size()));
            List<Booking> list = results.getBookings();
            int limit = Math.min(list.size(), MAX_ITEMS_PER_SECTION);
            for (int i = 0; i < limit; i++) {
                Booking b = list.get(i);
                String veh = EntityLookup.bookingVehicleReg(garage, b.getId());
                container.getChildren().add(createItemRow(
                        "BOKNING", "info",
                        "Bokning #" + b.getId() + " - " + b.getDescription(),
                        b.getDate() + " • " + veh,
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
            container.getChildren().add(createCategoryHeader("Arbetsordrar", results.getWorkOrders().size()));
            List<WorkOrder> list = results.getWorkOrders();
            int limit = Math.min(list.size(), MAX_ITEMS_PER_SECTION);
            for (int i = 0; i < limit; i++) {
                WorkOrder wo = list.get(i);
                String cust = EntityLookup.workOrderCustomerName(garage, wo);
                String veh = EntityLookup.workOrderVehicleReg(garage, wo);
                container.getChildren().add(createItemRow(
                        "ORDER", "accent",
                        "Arbetsorder #" + wo.getId() + " (" + UiFormatters.statusWord(wo.getStatus()) + ")",
                        cust + " • " + veh,
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
            container.getChildren().add(createCategoryHeader("Tjänster", results.getServices().size()));
            List<ServiceItem> list = results.getServices();
            int limit = Math.min(list.size(), MAX_ITEMS_PER_SECTION);
            for (int i = 0; i < limit; i++) {
                ServiceItem s = list.get(i);
                container.getChildren().add(createItemRow(
                        "TJÄNST", "muted",
                        s.getName() + " (" + UiFormatters.formatMoney(s.getPrice()) + ")",
                        s.getEstimatedMinutes() + " min • " + s.getDescription(),
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
            container.getChildren().add(createCategoryHeader("Fakturor", results.getInvoices().size()));
            List<Invoice> list = results.getInvoices();
            int limit = Math.min(list.size(), MAX_ITEMS_PER_SECTION);
            for (int i = 0; i < limit; i++) {
                Invoice inv = list.get(i);
                String cust = EntityLookup.invoiceCustomerName(garage, inv);
                container.getChildren().add(createItemRow(
                        "FAKTURA", "warn",
                        "Faktura #" + inv.getId() + " (" + UiFormatters.formatMoney(inv.getTotalAmount()) + ")",
                        cust + " • " + (inv.isPaid() ? "Betald" : "Obetald"),
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
        footer.setStyle("-fx-background-color: -wac-shell; -fx-border-color: -wac-line; -fx-border-width: 1 0 0 0; -fx-cursor: hand;");
        Label footerLbl = new Label("🔍 Visa alla " + results.getTotalMatches() + " träffar i fullständig översikt →");
        footerLbl.setStyle("-fx-text-fill: -wac-accent; -fx-font-weight: bold; -fx-font-size: 12px;");
        footer.getChildren().add(footerLbl);
        footer.setOnMouseEntered(e -> footer.setStyle("-fx-background-color: -wac-accent-soft; -fx-border-color: -wac-line; -fx-border-width: 1 0 0 0; -fx-cursor: hand;"));
        footer.setOnMouseExited(e -> footer.setStyle("-fx-background-color: -wac-shell; -fx-border-color: -wac-line; -fx-border-width: 1 0 0 0; -fx-cursor: hand;"));
        footer.setOnMouseClicked(e -> {
            hide();
            if (router != null) {
                router.navigate("search");
            }
        });
        container.getChildren().add(footer);
    }

    private Node createCategoryHeader(String title, int count) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(7, 14, 7, 14));
        box.setStyle("-fx-background-color: -wac-shell; -fx-border-color: -wac-line; -fx-border-width: 0 0 1 0;");

        Label lbl = new Label(title.toUpperCase() + " (" + count + ")");
        lbl.setStyle("-fx-text-fill: -wac-muted; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 0.5px;");
        box.getChildren().add(lbl);
        return box;
    }

    private Node createItemRow(String badgeText, String badgeType, String primaryText, String secondaryText, Runnable onClick) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 14, 8, 14));
        row.setStyle("-fx-cursor: hand; -fx-background-color: transparent; -fx-border-color: -wac-line; -fx-border-width: 0 0 1 0;");

        Label badge = new Label(badgeText);
        badge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 4; -fx-border-radius: 4;");
        if ("success".equals(badgeType)) {
            badge.setStyle(badge.getStyle() + "-fx-background-color: -wac-success-soft; -fx-text-fill: -wac-success;");
        } else if ("warn".equals(badgeType)) {
            badge.setStyle(badge.getStyle() + "-fx-background-color: -wac-warn-soft; -fx-text-fill: -wac-warn;");
        } else if ("accent".equals(badgeType)) {
            badge.setStyle(badge.getStyle() + "-fx-background-color: -wac-accent-soft; -fx-text-fill: -wac-accent;");
        } else {
            badge.setStyle(badge.getStyle() + "-fx-background-color: -wac-info-soft; -fx-text-fill: -wac-info;");
        }

        VBox textCol = new VBox(2);
        Label prim = new Label(primaryText);
        prim.setStyle("-fx-text-fill: -wac-text; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label sec = new Label(secondaryText);
        sec.setStyle("-fx-text-fill: -wac-muted; -fx-font-size: 11px;");
        textCol.getChildren().addAll(prim, sec);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        Label arrow = new Label("→");
        arrow.setStyle("-fx-text-fill: -wac-muted; -fx-font-size: 12px;");

        row.getChildren().addAll(badge, textCol, arrow);

        row.setOnMouseEntered(e -> row.setStyle("-fx-cursor: hand; -fx-background-color: -wac-accent-soft; -fx-border-color: -wac-line; -fx-border-width: 0 0 1 0;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-cursor: hand; -fx-background-color: transparent; -fx-border-color: -wac-line; -fx-border-width: 0 0 1 0;"));
        row.setOnMouseClicked(e -> {
            if (onClick != null) {
                onClick.run();
            }
        });

        return row;
    }
}
