package com.wac.autocore.ui;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.theme.ThemeCatalog;
import com.wac.autocore.theme.ThemeCatalog.Theme;
import com.wac.autocore.theme.ThemeManager;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * JavaFX GUI for Wigell AutoCore (sprint 1: read-only overview of the data
 * that already exists in the system). Rebuilt to mirror the web dashboards:
 * page canvas -> rounded shell card -> sidebar (brand + nav) -> topbar ->
 * content pages, all styled through the generated per-theme CSS.
 *
 * Run from {@code Main}; a theme can be picked live from the top bar.
 */
public class AutoCoreApp extends Application {

    private final GarageSystem garage = new GarageSystem();
    private final DecimalFormat money = new DecimalFormat("#,##0");

    private final List<Button> navButtons = new ArrayList<Button>();

    private VBox pageBox;
    private TableView currentTable;
    private FilteredList currentFiltered;
    private ObservableList currentBase;
    private TextField searchField;

    @Override
    public void start(Stage primaryStage) {
        BorderPane stage = new BorderPane();
        stage.setPadding(new Insets(14));
        stage.getStyleClass().add("stage");

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("shell");

        shell.setLeft(buildSidebar());
        shell.setCenter(buildMainArea());

        stage.setCenter(shell);

        Scene scene = new Scene(stage, 1280, 800);
        ThemeManager.applyDefault(scene);

        primaryStage.setTitle("Wigell AutoCore");
        primaryStage.setScene(scene);
        primaryStage.show();

        selectPage("overview");
    }

    // ---------------------------------------------------------------- shell

    private VBox buildSidebar() {
        StackPane mark = new StackPane();
        mark.getStyleClass().add("brand-mark");
        mark.setPrefSize(38, 38);
        Label letter = new Label("AC");
        letter.getStyleClass().add("letter");
        mark.getChildren().add(letter);

        VBox brandTitles = new VBox(0);
        Label brand = new Label("AutoCore");
        brand.getStyleClass().add("brand-title");
        Label brandSub = new Label("Workshop System");
        brandSub.getStyleClass().add("brand-sub");
        brandTitles.getChildren().addAll(brand, brandSub);

        HBox brandRow = new HBox(10, mark, brandTitles);
        brandRow.getStyleClass().add("brand-row");
        brandRow.setAlignment(Pos.CENTER_LEFT);

        // The menu follows the design's groups and can be collapsed so it always
        // fits even when the window is short.
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
        // No spacer needed: navScroll grows and the status box stays at the
        // bottom, so the sidebar and content always share the same height.
        sidebar.getChildren().addAll(brandRow, navScroll/*, buildDrift()*/);
        return sidebar;
    }

    /** A navigation entry inside a group. */
    private static final class NavSpec {
        final String key;
        final String label;
        NavSpec(String key, String label) { this.key = key; this.label = label; }
    }

    private static NavSpec navItem(String key, String label) {
        return new NavSpec(key, label);
    }

    /**
     * A collapsible nav group. The header is clickable and collapses the entries
     * (managed=false so they take no space when hidden).
     */
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

    /*
     * buildDrift() – System Status-rutan är en placeholder.
     * "All systems operational" och "Last backup today 06:00" har ingen
     * koppling till riktig logik i systemet. Kommenteras ut tills
     * funktionaliteten finns på plats.
     *
     * private VBox buildDrift() {
     *     Label title = new Label("System Status");
     *     title.getStyleClass().add("drift-title");
     *     Label text = new Label("All systems operational");
     *     text.getStyleClass().add("drift-text");
     *
     *     Region fill = new Region();
     *     fill.getStyleClass().add("drift-fill");
     *     StackPane track = new StackPane(fill);
     *     track.getStyleClass().add("drift-track");
     *     track.setPrefHeight(8);
     *     track.setAlignment(Pos.CENTER_LEFT);
     *     fill.prefWidthProperty().bind(track.widthProperty().multiply(0.92));
     *     fill.prefHeightProperty().bind(track.heightProperty().subtract(2));
     *     fill.maxWidthProperty().bind(track.widthProperty().multiply(0.92));
     *
     *     Label foot = new Label("Last backup today 06:00");
     *     foot.getStyleClass().addAll("drift-text", "small");
     *
     *     VBox box = new VBox(7, title, text, track, foot);
     *     box.getStyleClass().add("drift");
     *     box.setPadding(new Insets(14, 16, 14, 16));
     *     return box;
     * }
     */

    private void addNav(VBox nav, String key, String label) {
        Button b = new Button(label);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setUserData(key);
        b.getStyleClass().addAll("ghost", "nav-item");
        b.setOnAction(e -> selectPage(key));
        navButtons.add(b);
        nav.getChildren().add(b);
    }

    private BorderPane buildMainArea() {
        searchField = new TextField();
        searchField.getStyleClass().add("search");
        searchField.setPromptText("Search work orders, customers or vehicles…");
        searchField.setPrefWidth(320);
        searchField.textProperty().addListener((obs, old, now) -> applySearch(now));

        ComboBox<Theme> themeBox = new ComboBox<Theme>();
        themeBox.getStyleClass().add("theme-pick");
        themeBox.setCellFactory(param -> themeCell());
        themeBox.setButtonCell(themeCell());
        themeBox.setItems(FXCollections.observableArrayList(ThemeCatalog.all()));
        themeBox.getSelectionModel().select(ThemeCatalog.bySlug(ThemeCatalog.DEFAULT_SLUG));
        // Inline-style vinner alltid över stylesheet-regler (oavsett CSS-ordning).
        // Sätter fast vit/svart stil så att temaväljaren alltid är läsbar.
        themeBox.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #d1d5db;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-border-width: 1;" +
            "-fx-padding: 4px 10px;");
        themeBox.valueProperty().addListener((obs, oldT, newT) -> {
            Scene scene = themeBox.getScene();
            if (scene != null && newT != null) {
                ThemeManager.apply(scene, newT.slug);
                // Återställ inline-style (ThemeManager.apply rensar stylesheets men
                // inline-style är separat och bevaras ändå – detta är bara för tydlighet)
                themeBox.setStyle(
                    "-fx-background-color: white;" +
                    "-fx-border-color: #d1d5db;" +
                    "-fx-border-radius: 8;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-width: 1;" +
                    "-fx-padding: 4px 10px;");
                // Synka popup-scenen om den är öppen
                javafx.application.Platform.runLater(() -> syncComboPopup(themeBox));
            }
        });
        // När användaren klickar öppnar sig popupen – kopiera stylesheets dit
        themeBox.setOnShowing(e -> syncComboPopup(themeBox));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Avatar ("AK") är en placeholder – ingen inloggningsfunktion finns ännu.
        // Återaktivera när ett användarsystem är på plats.
        //
        // StackPane avatar = new StackPane();
        // avatar.getStyleClass().add("avatar");
        // avatar.setPrefSize(36, 36);
        // Label avLetter = new Label("AK");
        // avLetter.getStyleClass().add("letter");
        // avatar.getChildren().add(avLetter);

        HBox top = new HBox(14, searchField, spacer, themeBox/*, avatar*/);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("topbar");

        pageBox = new VBox(18);
        pageBox.getStyleClass().add("pages");

        ScrollPane scroll = new ScrollPane(pageBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        BorderPane col = new BorderPane();
        col.getStyleClass().add("col");
        col.setTop(top);
        col.setCenter(scroll);
        return col;
    }

    private void selectPage(String key) {
        for (Button b : navButtons) {
            b.getStyleClass().remove("selected");
            if (key.equals(b.getUserData())) {
                b.getStyleClass().add("selected");
            }
        }

        pageBox.getChildren().clear();
        if (key.equals("overview")) {
            pageBox.getChildren().add(buildOverview());
        } else if (key.equals("customers")) {
            Button addBtn = primaryButton("+ New customer");
            addBtn.setOnAction(e -> ActionDialogs.showCreateCustomerDialog(garage, () -> selectPage("customers")));
            pageBox.getChildren().add(buildEntityPage(
                    "Customers", garage.getCustomers().size() + " registered",
                    "Names, contact details and VIP status",
                    buildCustomersTable(), addBtn));
        } else if (key.equals("vehicles")) {
            Button addBtn = primaryButton("+ Register vehicle");
            addBtn.setOnAction(e -> ActionDialogs.showCreateVehicleDialog(garage, () -> selectPage("vehicles")));
            pageBox.getChildren().add(buildEntityPage(
                    "Vehicles", garage.getVehicles().size() + " registered",
                    "Vehicles registered at the workshop",
                    buildVehiclesTable(), addBtn));
        } else if (key.equals("bookings")) {
            Button addBtn = primaryButton("+ New booking");
            addBtn.setOnAction(e -> ActionDialogs.showCreateBookingDialog(garage, () -> selectPage("bookings")));
            pageBox.getChildren().add(buildEntityPage(
                    "Bookings", garage.getBookings().size() + " bookings",
                    "Scheduled jobs and their status",
                    buildBookingsTable(), addBtn));
        } else if (key.equals("workorders")) {
            TableView<WorkOrder> table = buildWorkOrdersTable();
            Button addBtn = primaryButton("+ New work order");
            addBtn.setOnAction(e -> ActionDialogs.showCreateWorkOrderDialog(garage, () -> selectPage("workorders")));
            Button startBtn = secondaryButton("▶ Start order");
            Button completeBtn = secondaryButton("✔ Complete order");
            startBtn.setDisable(true);
            completeBtn.setDisable(true);

            table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> {
                startBtn.setDisable(sel == null || !"CREATED".equals(sel.getStatus()));
                completeBtn.setDisable(sel == null || !"IN_PROGRESS".equals(sel.getStatus()));
            });

            startBtn.setOnAction(e -> {
                WorkOrder sel = table.getSelectionModel().getSelectedItem();
                if (sel != null && "CREATED".equals(sel.getStatus())) {
                    garage.startWorkOrder(sel.getId());
                    selectPage("workorders");
                }
            });

            completeBtn.setOnAction(e -> {
                WorkOrder sel = table.getSelectionModel().getSelectedItem();
                if (sel != null && "IN_PROGRESS".equals(sel.getStatus())) {
                    garage.completeWorkOrder(sel.getId());
                    selectPage("workorders");
                }
            });

            pageBox.getChildren().add(buildEntityPage(
                    "Work orders", garage.getWorkOrders().size() + " work orders",
                    "Active and completed jobs (select a row to start / complete)",
                    table, startBtn, completeBtn, addBtn));
        } else if (key.equals("services")) {
            pageBox.getChildren().add(buildEntityPage(
                    "Services", garage.getServiceItems().size() + " services",
                    "Price list for workshop services",
                    buildServicesTable()));
        } else if (key.equals("mechanics")) {
            pageBox.getChildren().add(buildEntityPage(
                    "Mechanics", garage.getMechanics().size() + " employees",
                    "Team, specialisation and availability",
                    buildMechanicsTable()));
        } else if (key.equals("invoices")) {
            TableView<Invoice> table = buildInvoicesTable();
            Button addBtn = primaryButton("+ Create invoice");
            addBtn.setOnAction(e -> ActionDialogs.showCreateInvoiceDialog(garage, () -> selectPage("invoices")));
            Button payBtn = secondaryButton("💳 Pay selected invoice");
            payBtn.setDisable(true);

            table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> {
                payBtn.setDisable(sel == null || sel.isPaid());
            });

            payBtn.setOnAction(e -> {
                Invoice sel = table.getSelectionModel().getSelectedItem();
                if (sel != null && !sel.isPaid()) {
                    ActionDialogs.showProcessPaymentDialog(garage, sel, () -> selectPage("invoices"));
                }
            });

            pageBox.getChildren().add(buildEntityPage(
                    "Invoices", garage.getInvoices().size() + " invoices",
                    "Issued invoices and payment status",
                    table, payBtn, addBtn));
        } else if (key.equals("payments")) {
            Button addBtn = primaryButton("+ Register payment");
            addBtn.setOnAction(e -> ActionDialogs.showProcessPaymentDialog(garage, null, () -> selectPage("payments")));
            pageBox.getChildren().add(buildEntityPage(
                    "Payments", garage.getPayments().size() + " payments",
                    "Received payments and their status",
                    buildPaymentsTable(), addBtn));
        }
    }

    // ---------------------------------------------------------------- pages

    private VBox pageHead(String title, String sub, String eyebrow) {
        Label eyebrowLabel = null;
        if (eyebrow != null) {
            eyebrowLabel = new Label(eyebrow);
            eyebrowLabel.getStyleClass().add("eyebrow");
        }
        Label t = new Label(title);
        t.getStyleClass().add("page-title");
        Label s = new Label(sub);
        s.getStyleClass().add("page-sub");
        VBox box = eyebrowLabel == null
                ? new VBox(2, t, s)
                : new VBox(1, eyebrowLabel, t, s);
        return box;
    }

    private Button primaryButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("button", "primary");
        return b;
    }

    private Button secondaryButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("button", "secondary-button");
        return b;
    }

    private VBox buildEntityPage(String title, String sub, String eyebrow,
                                 TableView<?> table, Node... actions) {
        VBox titles = pageHead(title, sub, eyebrow);
        HBox.setHgrow(titles, Priority.ALWAYS);

        HBox topRow = new HBox(12, titles);
        topRow.setAlignment(Pos.CENTER_LEFT);

        if (actions != null && actions.length > 0) {
            HBox actionBox = new HBox(8, actions);
            actionBox.setAlignment(Pos.CENTER_RIGHT);
            topRow.getChildren().add(actionBox);
        }

        table.setPlaceholder(new Label("No rows"));
        HBox.setHgrow(table, Priority.ALWAYS);

        VBox inner = new VBox();
        inner.getStyleClass().add("panel");
        inner.getChildren().add(table);
        inner.setPadding(new Insets(4, 6, 6, 6));

        VBox.setVgrow(table, Priority.ALWAYS);
        return new VBox(18, topRow, inner);
    }

    // ------------------------------------------------------------- overview

    private VBox buildOverview() {
        List<Customer> customers = garage.getCustomers();
        List<Vehicle> vehicles = garage.getVehicles();
        List<Booking> bookings = garage.getBookings();
        List<WorkOrder> workOrders = garage.getWorkOrders();
        List<Invoice> invoices = garage.getInvoices();
        List<Payment> payments = garage.getPayments();

        long active = 0;
        for (WorkOrder wo : workOrders) {
            if (!"COMPLETED".equals(wo.getStatus())) {
                active++;
            }
        }
        long revenue = 0;
        for (Payment p : payments) {
            if (p.isSuccessful()) {
                revenue += p.getAmount();
            }
        }
        long paid = 0;
        for (Invoice inv : invoices) {
            if (inv.isPaid()) {
                paid++;
            }
        }
        int avail = 0;
        for (Mechanic m : garage.getMechanics()) {
            if (m.isAvailable()) {
                avail++;
            }
        }

        VBox head = pageHead("Overview", "Current status of the workshop",
                "AutoCore \u00b7 " + todayFormatted());

        Button quickBooking = primaryButton("+ New booking");
        quickBooking.setOnAction(e -> ActionDialogs.showCreateBookingDialog(garage, () -> selectPage("overview")));
        Button quickOrder = secondaryButton("+ New work order");
        quickOrder.setOnAction(e -> ActionDialogs.showCreateWorkOrderDialog(garage, () -> selectPage("overview")));
        Button quickInvoice = secondaryButton("+ Create invoice");
        quickInvoice.setOnAction(e -> ActionDialogs.showCreateInvoiceDialog(garage, () -> selectPage("overview")));
        Button quickPay = secondaryButton("💳 Payment");
        quickPay.setOnAction(e -> ActionDialogs.showProcessPaymentDialog(garage, null, () -> selectPage("overview")));

        HBox quickBar = new HBox(10, quickBooking, quickOrder, quickInvoice, quickPay);
        quickBar.setAlignment(Pos.CENTER_LEFT);

        HBox kpis = new HBox(14);
        kpis.setAlignment(Pos.CENTER_LEFT);
        kpis.getChildren().addAll(
                kpi("Active work orders", String.valueOf(active)),
                kpi("Total revenue", money.format(revenue) + " kr"),
                kpi("Bookings", String.valueOf(bookings.size())),
                kpi("Mechanics on duty", avail + "/" + garage.getMechanics().size()));

        HBox panels = new HBox(14);
        panels.setAlignment(Pos.CENTER_LEFT);
        panels.getChildren().addAll(
                statusPanel(workOrders),
                bookingsPanel(bookings));

        TableView<WorkOrder> recent = buildRecentOrders(workOrders);
        VBox recentPanel = panel("Recent work orders",
                "The latest jobs registered in the system", recent);

        return new VBox(18, head, quickBar, kpis, panels, recentPanel);
    }

    private VBox kpi(String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("kpi-label");
        Label v = new Label(value);
        v.getStyleClass().add("kpi-value");
        VBox box = new VBox(6, v, l);
        box.getStyleClass().add("kpi");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private VBox statusPanel(List<WorkOrder> workOrders) {
        Label title = new Label("Work orders by status");
        title.getStyleClass().add("panel-title");
        Label sub = new Label("Distribution across all work orders");
        sub.getStyleClass().add("panel-sub");

        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        counts.put("Completed", 0);
        counts.put("In progress", 0);
        counts.put("Work order created", 0);
        counts.put("Created", 0);
        counts.put("Booked", 0);
        for (WorkOrder wo : workOrders) {
            String w = statusWord(wo.getStatus());
            counts.put(w, counts.containsKey(w) ? counts.get(w) + 1 : 1);
        }

        VBox list = new VBox(9);
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() == 0) {
                continue;
            }
            Label dot = new Label();
            dot.getStyleClass().addAll("sdot", dotClass(e.getKey()));
            Label name = new Label(e.getKey());
            name.getStyleClass().add("srow-title");
            Label n = new Label(String.valueOf(e.getValue()));
            n.getStyleClass().add("srow-sub");
            Region spr = new Region();
            HBox.setHgrow(spr, Priority.ALWAYS);
            HBox row = new HBox(10, dot, name, spr, n);
            row.getStyleClass().add("srow");
            list.getChildren().add(row);
        }
        if (list.getChildren().isEmpty()) {
            list.getChildren().add(mutedNote("No work orders yet"));
        }

        VBox box = new VBox(12, title, sub, list);
        box.getStyleClass().add("panel");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private VBox bookingsPanel(List<Booking> bookings) {
        Label title = new Label("Upcoming bookings");
        title.getStyleClass().add("panel-title");
        Label sub = new Label("Next scheduled jobs");
        sub.getStyleClass().add("panel-sub");

        VBox list = new VBox(9);
        int shown = 0;
        for (Booking b : bookings) {
            if (shown >= 5) {
                break;
            }
            Label dot = new Label();
            dot.getStyleClass().addAll("sdot", dotClass(b.getStatus()));
            Label vehicle = new Label(vehicleReg(b.getVehicleId()) + " · " + b.getDate());
            vehicle.getStyleClass().add("srow-title");
            Label desc = new Label(truncate(b.getDescription(), 42));
            desc.getStyleClass().addAll("srow-sub", "small");
            Region spr = new Region();
            HBox.setHgrow(spr, Priority.ALWAYS);
            Label st = new Label(statusWord(b.getStatus()));
            st.getStyleClass().addAll("badge", badgeClass(st.getText()));
            VBox text = new VBox(0, vehicle, desc);
            HBox row = new HBox(10, dot, text, spr, st);
            row.getStyleClass().add("srow");
            list.getChildren().add(row);
            shown++;
        }
        if (list.getChildren().isEmpty()) {
            list.getChildren().add(mutedNote("No bookings yet"));
        }

        VBox box = new VBox(12, title, sub, list);
        box.getStyleClass().add("panel");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private VBox panel(String title, String sub, javafx.scene.Node body) {
        Label t = new Label(title);
        t.getStyleClass().add("panel-title");
        Label s = new Label(sub);
        s.getStyleClass().add("panel-sub");
        VBox box = new VBox(10, t, s, body);
        box.getStyleClass().add("panel");
        return box;
    }

    private Label mutedNote(String text) {
        Label l = new Label(text);
        l.getStyleClass().addAll("srow-sub", "small");
        return l;
    }

    // ------------------------------------------------------------ customers

    private TableView<Customer> buildCustomersTable() {
        TableView<Customer> t = make(garage.getCustomers());
        t.getColumns().addAll(
                col("ID", 60, c -> String.valueOf(c.getId())),
                col("Name", 200, c -> c.getName()),
                col("Phone", 150, c -> c.getPhone()),
                col("Email", 280, c -> c.getEmail()),
                badge("VIP", 100, c -> c.isVip() ? "Yes" : "No"));
        return t;
    }

    private TableView<Vehicle> buildVehiclesTable() {
        TableView<Vehicle> t = make(garage.getVehicles());
        t.getColumns().addAll(
                col("ID", 60, c -> String.valueOf(c.getId())),
                col("Reg. no.", 120, c -> c.getRegistrationNumber()),
                col("Make", 140, c -> c.getBrand()),
                col("Model", 160, c -> c.getModel()),
                col("Year", 100, c -> String.valueOf(c.getYear())),
                col("Customer", 220, c -> customerName(c.getCustomerId())));
        return t;
    }

    private TableView<Booking> buildBookingsTable() {
        TableView<Booking> t = make(garage.getBookings());
        t.getColumns().addAll(
                col("ID", 60, c -> String.valueOf(c.getId())),
                col("Vehicle", 140, c -> vehicleReg(c.getVehicleId())),
                col("Date", 130, c -> String.valueOf(c.getDate())),
                col("Description", 320, c -> c.getDescription()),
                badge("Status", 140, c -> statusWord(c.getStatus())));
        return t;
    }

    private TableView<WorkOrder> buildRecentOrders(List<WorkOrder> orders) {
        TableView<WorkOrder> t = make(orders);
        t.getColumns().addAll(
                col("ID", 70, c -> String.valueOf(c.getId())),
                col("Booking", 90, c -> String.valueOf(c.getBookingId())),
                col("Mechanic", 180, c -> mechanicName(c.getMechanicId())),
                col("Services", 300, c -> serviceNames(c.getServiceItemIds())),
                badge("Status", 140, c -> statusWord(c.getStatus())));
        return t;
    }

    private TableView<WorkOrder> buildWorkOrdersTable() {
        return buildRecentOrders(garage.getWorkOrders());
    }

    private TableView<ServiceItem> buildServicesTable() {
        TableView<ServiceItem> t = make(garage.getServiceItems());
        t.getColumns().addAll(
                col("ID", 70, c -> String.valueOf(c.getId())),
                col("Name", 220, c -> c.getName()),
                col("Description", 360, c -> c.getDescription()),
                col("Price", 120, c -> money.format(c.getPrice()) + " kr"),
                col("Time", 100, c -> c.getEstimatedMinutes() + " min"));
        return t;
    }

    private TableView<Mechanic> buildMechanicsTable() {
        TableView<Mechanic> t = make(garage.getMechanics());
        t.getColumns().addAll(
                col("ID", 70, c -> String.valueOf(c.getId())),
                col("Name", 220, c -> c.getName()),
                col("Phone", 160, c -> c.getPhone()),
                col("Specialisation", 260, c -> c.getSpecialization()),
                badge("Available", 130, c -> c.isAvailable() ? "Yes" : "No"));
        return t;
    }

    private TableView<Invoice> buildInvoicesTable() {
        TableView<Invoice> t = make(garage.getInvoices());
        t.getColumns().addAll(
                col("ID", 70, c -> String.valueOf(c.getId())),
                col("Work order", 110, c -> String.valueOf(c.getWorkOrderId())),
                col("Date", 130, c -> String.valueOf(c.getInvoiceDate())),
                col("Amount", 110, c -> money.format(c.getAmount()) + " kr"),
                col("Discount", 100, c -> money.format(c.getDiscount()) + " kr"),
                col("Total", 110, c -> money.format(c.getTotalAmount()) + " kr"),
                badge("Paid", 110, c -> c.isPaid() ? "Yes" : "No"));
        return t;
    }

    private TableView<Payment> buildPaymentsTable() {
        TableView<Payment> t = make(garage.getPayments());
        t.getColumns().addAll(
                col("ID", 70, c -> String.valueOf(c.getId())),
                col("Invoice", 100, c -> String.valueOf(c.getInvoiceId())),
                col("Amount", 120, c -> money.format(c.getAmount()) + " kr"),
                col("Type", 130, c -> c.getPaymentType()),
                col("Date/time", 220, c -> String.valueOf(c.getPaymentDate())),
                badge("Status", 110, c -> c.isSuccessful() ? "Successful" : "Failed"));
        return t;
    }

    // -------------------------------------------------------------- helpers

    private <S> TableView<S> make(List<S> data) {
        currentBase = FXCollections.observableArrayList(data);
        currentFiltered = new FilteredList(currentBase);
        currentTable = new TableView(currentFiltered);
        return currentTable;
    }

    private void applySearch(String query) {
        if (currentFiltered == null) {
            return;
        }
        final String q = query == null ? "" : query.trim().toLowerCase();
        currentFiltered.setPredicate(row -> {
            if (q.isEmpty()) {
                return true;
            }
            if (currentTable == null) {
                return false;
            }
            int idx = currentBase == null ? -1 : currentBase.indexOf(row);
            if (idx < 0) {
                return false;
            }
            for (Object ocol : currentTable.getColumns()) {
                if (ocol == null) {
                    continue;
                }
                Object val = ((TableColumn) ocol).getCellData(idx);
                if (val != null && val.toString().toLowerCase().contains(q)) {
                    return true;
                }
            }
            return false;
        });
    }

    private javafx.scene.control.ListCell<Theme> themeCell() {
        return new javafx.scene.control.ListCell<Theme>() {
            @Override
            protected void updateItem(Theme t, boolean empty) {
                super.updateItem(t, empty);
                setText(t == null || empty ? null : t.name);
                // Inline-style – svart text, vit bakgrund, alltid läsbar oavsett tema
                setStyle("-fx-text-fill: #111827; -fx-background-color: " +
                         (isSelected() ? "#e5e7eb" : "transparent") + "; -fx-padding: 5 10;");
            }
            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                setStyle("-fx-text-fill: #111827; -fx-background-color: " +
                         (selected ? "#e5e7eb" : "transparent") + "; -fx-padding: 5 10;");
            }
        };
    }

    private <S> TableColumn<S, String> col(String title, double width,
                                           Function<S, String> mapper) {
        TableColumn<S, String> c = new TableColumn<S, String>(title);
        c.setPrefWidth(width);
        c.setCellValueFactory(cd -> new ReadOnlyStringWrapper(mapper.apply(cd.getValue())));
        return c;
    }

    private <S> TableColumn<S, String> badge(String title, double width,
                                             Function<S, String> mapper) {
        TableColumn<S, String> c = new TableColumn<S, String>(title);
        c.setPrefWidth(width);
        c.setCellValueFactory(cd -> new ReadOnlyStringWrapper(mapper.apply(cd.getValue())));
        c.setCellFactory(column -> new TableCell<S, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label chip = new Label(item);
                chip.getStyleClass().add("badge");
                String extra = badgeClass(item);
                if (!extra.isEmpty()) {
                    chip.getStyleClass().add(extra);
                }
                setGraphic(chip);
                setText(null);
            }
        });
        return c;
    }

    private static boolean isGood(String s) {
        return s.equals("Yes") || s.equals("Successful")
                || s.equals("Completed") || s.equals("Paid");
    }

    private static String badgeClass(String s) {
        if (s == null) {
            return "";
        }
        if (isGood(s)) {
            return "success";
        }
        if (s.equals("No") || s.equals("Failed")) {
            return "danger";
        }
        if (s.equals("In progress")) {
            return "warn";
        }
        if (s.equals("Booked") || s.equals("Created") || s.equals("Work order created")) {
            return "info";
        }
        return "";
    }

    private static String dotClass(String s) {
        String b = badgeClass(s);
        if (b.isEmpty()) {
            return "info";
        }
        return b;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static String todayFormatted() {
        String[] week = {"", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        String[] months = {"", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        LocalDate d = LocalDate.now();
        return week[d.getDayOfWeek().getValue()] + " " + d.getDayOfMonth()
                + " " + months[d.getMonthValue()] + " " + d.getYear();
    }

    private String statusWord(String status) {
        if (status == null) {
            return "";
        }
        if (status.equals("BOOKED")) {
            return "Booked";
        }
        if (status.equals("CREATED")) {
            return "Created";
        }
        if (status.equals("WORK_ORDER_CREATED")) {
            return "Work order created";
        }
        if (status.equals("IN_PROGRESS")) {
            return "In progress";
        }
        if (status.equals("COMPLETED")) {
            return "Completed";
        }
        return status;
    }

    private String customerName(int id) {
        for (Customer c : garage.getCustomers()) {
            if (c.getId() == id) {
                return c.getName();
            }
        }
        return "Customer #" + id;
    }

    private String vehicleReg(int id) {
        for (Vehicle v : garage.getVehicles()) {
            if (v.getId() == id) {
                return v.getRegistrationNumber();
            }
        }
        return "Vehicle #" + id;
    }

    private String mechanicName(int id) {
        for (Mechanic m : garage.getMechanics()) {
            if (m.getId() == id) {
                return m.getName();
            }
        }
        return "Mechanic #" + id;
    }

    private String serviceNames(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Integer sid : ids) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            String found = null;
            for (ServiceItem s : garage.getServiceItems()) {
                if (s.getId() == sid) {
                    found = s.getName();
                    break;
                }
            }
            sb.append(found != null ? found : "Service #" + sid);
        }
        return sb.toString();
    }

    /**
     * JavaFX ComboBox-popupen är ett separat PopupWindow med sin egna Scene.
     * Scenens stylesheets ärver INTE automatiskt appens – vi måste kopiera dem
     * manuellt varje gång popupen visas eller temat byts.
     *
     * Popup-referensen finns som privat fält "popup" i ComboBoxListViewSkin (JFX 8).
     * Vi hämtar den via reflektion och applicerar aktuella stylesheets på pop-scene.
     */
    private void syncComboPopup(javafx.scene.control.ComboBox<?> box) {
        if (box == null || box.getSkin() == null || box.getScene() == null) {
            return;
        }
        javafx.scene.Scene appScene = box.getScene();
        // Traversera skin-hierarkin och leta efter fältet "popup"
        for (Class<?> c = box.getSkin().getClass(); c != null; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField("popup");
                f.setAccessible(true);
                Object popupObj = f.get(box.getSkin());
                if (popupObj instanceof javafx.stage.Window) {
                    javafx.scene.Scene popScene = ((javafx.stage.Window) popupObj).getScene();
                    if (popScene != null) {
                        popScene.getStylesheets().setAll(appScene.getStylesheets());
                    }
                }
                return;
            } catch (NoSuchFieldException ignored) {
                // Fortsätt till superklassen
            } catch (Exception e) {
                System.err.println("[AutoCoreApp] syncComboPopup: " + e);
                return;
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
