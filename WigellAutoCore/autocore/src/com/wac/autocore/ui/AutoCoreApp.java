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
        Label brandSub = new Label("Verkstadssystem");
        brandSub.getStyleClass().add("brand-sub");
        brandTitles.getChildren().addAll(brand, brandSub);

        HBox brandRow = new HBox(10, mark, brandTitles);
        brandRow.getStyleClass().add("brand-row");
        brandRow.setAlignment(Pos.CENTER_LEFT);

        // Menyn följer designens grupper och går att fälla ihop, så den alltid
        // får plats även när fönstret är lågt.
        VBox nav = new VBox(3);
        nav.setPadding(new Insets(14, 0, 0, 0));
        addNav(nav, "overview", "Översikt");

        VBox groups = new VBox(2);
        addGroup(groups, "Kunder", navItem("customers", "Visa kunder"));
        addGroup(groups, "Fordon", navItem("vehicles", "Visa fordon"));
        addGroup(groups, "Bokningar", navItem("bookings", "Visa bokningar"));
        addGroup(groups, "Verkstad",
                navItem("services", "Visa tjänster"),
                navItem("mechanics", "Visa mekaniker"),
                navItem("workorders", "Visa arbetsorder"));
        addGroup(groups, "Ekonomi",
                navItem("invoices", "Visa fakturor"),
                navItem("payments", "Visa betalningar"));
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
        // Ingen spacer behövs: navScroll växer och drift-rutan ligger kvar i botten,
        // så sidofältet och innehållet alltid har samma höjd.
        sidebar.getChildren().addAll(brandRow, navScroll, buildDrift());
        return sidebar;
    }

    /** En navigeringspost i en grupp. */
    private static final class NavSpec {
        final String key;
        final String label;
        NavSpec(String key, String label) { this.key = key; this.label = label; }
    }

    private static NavSpec navItem(String key, String label) {
        return new NavSpec(key, label);
    }

    /**
     * En kollapsbar meny grupp. Rubriken är klickbar och fäller ihop posterna
     * (managed=false så de inte tar plats när de är dolda).
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

    private VBox buildDrift() {
        Label title = new Label("Driftstatus");
        title.getStyleClass().add("drift-title");
        Label text = new Label("Alla system i drift");
        text.getStyleClass().add("drift-text");

        Region fill = new Region();
        fill.getStyleClass().add("drift-fill");
        StackPane track = new StackPane(fill);
        track.getStyleClass().add("drift-track");
        track.setPrefHeight(8);
        track.setAlignment(Pos.CENTER_LEFT);
        fill.prefWidthProperty().bind(track.widthProperty().multiply(0.92));
        fill.prefHeightProperty().bind(track.heightProperty().subtract(2));
        fill.maxWidthProperty().bind(track.widthProperty().multiply(0.92));

        Label foot = new Label("Senaste backup idag 06:00");
        foot.getStyleClass().addAll("drift-text", "small");

        VBox box = new VBox(7, title, text, track, foot);
        box.getStyleClass().add("drift");
        box.setPadding(new Insets(14, 16, 14, 16));
        return box;
    }

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
        searchField.setPromptText("Sök arbetsorder, kund eller fordon…");
        searchField.setPrefWidth(320);
        searchField.textProperty().addListener((obs, old, now) -> applySearch(now));

        ComboBox<Theme> themeBox = new ComboBox<Theme>();
        themeBox.getStyleClass().add("theme-pick");
        themeBox.setCellFactory(param -> themeCell());
        themeBox.setButtonCell(themeCell());
        themeBox.setItems(FXCollections.observableArrayList(ThemeCatalog.all()));
        themeBox.getSelectionModel().select(ThemeCatalog.bySlug(ThemeCatalog.DEFAULT_SLUG));
        themeBox.valueProperty().addListener((obs, oldT, newT) -> {
            Scene scene = themeBox.getScene();
            if (scene != null && newT != null) {
                ThemeManager.apply(scene, newT.slug);
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("avatar");
        avatar.setPrefSize(36, 36);
        Label avLetter = new Label("AK");
        avLetter.getStyleClass().add("letter");
        avatar.getChildren().add(avLetter);

        HBox top = new HBox(14, searchField, spacer, themeBox, avatar);
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
            Button addBtn = primaryButton("+ Ny kund");
            addBtn.setOnAction(e -> ActionDialogs.showCreateCustomerDialog(garage, () -> selectPage("customers")));
            pageBox.getChildren().add(buildEntityPage(
                    "Kunder", garage.getCustomers().size() + " registrerade",
                    "Namn, kontaktuppgifter och VIP-status",
                    buildCustomersTable(), addBtn));
        } else if (key.equals("vehicles")) {
            Button addBtn = primaryButton("+ Registrera fordon");
            addBtn.setOnAction(e -> ActionDialogs.showCreateVehicleDialog(garage, () -> selectPage("vehicles")));
            pageBox.getChildren().add(buildEntityPage(
                    "Fordon", garage.getVehicles().size() + " registrerade",
                    "Registrerade fordon i verkstaden",
                    buildVehiclesTable(), addBtn));
        } else if (key.equals("bookings")) {
            Button addBtn = primaryButton("+ Ny bokning");
            addBtn.setOnAction(e -> ActionDialogs.showCreateBookingDialog(garage, () -> selectPage("bookings")));
            pageBox.getChildren().add(buildEntityPage(
                    "Bokningar", garage.getBookings().size() + " bokningar",
                    "Inbokade jobb och dess status",
                    buildBookingsTable(), addBtn));
        } else if (key.equals("workorders")) {
            TableView<WorkOrder> table = buildWorkOrdersTable();
            Button addBtn = primaryButton("+ Ny arbetsorder");
            addBtn.setOnAction(e -> ActionDialogs.showCreateWorkOrderDialog(garage, () -> selectPage("workorders")));
            Button startBtn = secondaryButton("▶ Starta order");
            Button completeBtn = secondaryButton("✔ Slutför order");
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
                    "Arbetsorder", garage.getWorkOrders().size() + " arbetsorder",
                    "Pågående och slutförda jobb (markera rad för att starta/slutföra)",
                    table, startBtn, completeBtn, addBtn));
        } else if (key.equals("services")) {
            pageBox.getChildren().add(buildEntityPage(
                    "Tjänster", garage.getServiceItems().size() + " tjänster",
                    "Prislista för verkstadens tjänster",
                    buildServicesTable()));
        } else if (key.equals("mechanics")) {
            pageBox.getChildren().add(buildEntityPage(
                    "Mekaniker", garage.getMechanics().size() + " anställda",
                    "Team, specialisering och tillgänglighet",
                    buildMechanicsTable()));
        } else if (key.equals("invoices")) {
            TableView<Invoice> table = buildInvoicesTable();
            Button addBtn = primaryButton("+ Skapa faktura");
            addBtn.setOnAction(e -> ActionDialogs.showCreateInvoiceDialog(garage, () -> selectPage("invoices")));
            Button payBtn = secondaryButton("💳 Betala vald faktura");
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
                    "Fakturor", garage.getInvoices().size() + " fakturor",
                    "Utfärdade fakturor och betalstatus",
                    table, payBtn, addBtn));
        } else if (key.equals("payments")) {
            Button addBtn = primaryButton("+ Registrera betalning");
            addBtn.setOnAction(e -> ActionDialogs.showProcessPaymentDialog(garage, null, () -> selectPage("payments")));
            pageBox.getChildren().add(buildEntityPage(
                    "Betalningar", garage.getPayments().size() + " betalningar",
                    "Inkomna betalningar och deras status",
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

        table.setPlaceholder(new Label("Inga rader"));
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

        VBox head = pageHead("Översikt", "Så ser läget ut i verkstaden just nu",
                "AutoCore · " + todaySwedish());

        Button quickBooking = primaryButton("+ Ny bokning");
        quickBooking.setOnAction(e -> ActionDialogs.showCreateBookingDialog(garage, () -> selectPage("overview")));
        Button quickOrder = secondaryButton("+ Ny arbetsorder");
        quickOrder.setOnAction(e -> ActionDialogs.showCreateWorkOrderDialog(garage, () -> selectPage("overview")));
        Button quickInvoice = secondaryButton("+ Skapa faktura");
        quickInvoice.setOnAction(e -> ActionDialogs.showCreateInvoiceDialog(garage, () -> selectPage("overview")));
        Button quickPay = secondaryButton("💳 Betalning");
        quickPay.setOnAction(e -> ActionDialogs.showProcessPaymentDialog(garage, null, () -> selectPage("overview")));

        HBox quickBar = new HBox(10, quickBooking, quickOrder, quickInvoice, quickPay);
        quickBar.setAlignment(Pos.CENTER_LEFT);

        HBox kpis = new HBox(14);
        kpis.setAlignment(Pos.CENTER_LEFT);
        kpis.getChildren().addAll(
                kpi("Aktiva arbetsorder", String.valueOf(active)),
                kpi("Betalt totalt", money.format(revenue) + " kr"),
                kpi("Bokningar", String.valueOf(bookings.size())),
                kpi("Mekaniker i tjänst", avail + "/" + garage.getMechanics().size()));

        HBox panels = new HBox(14);
        panels.setAlignment(Pos.CENTER_LEFT);
        panels.getChildren().addAll(
                statusPanel(workOrders),
                bookingsPanel(bookings));

        TableView<WorkOrder> recent = buildRecentOrders(workOrders);
        VBox recentPanel = panel("Senaste arbetsorder",
                "De senaste registrerade jobben i systemet", recent);

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
        Label title = new Label("Arbetsorder per status");
        title.getStyleClass().add("panel-title");
        Label sub = new Label("Fördelning över alla arbetsorder");
        sub.getStyleClass().add("panel-sub");

        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        counts.put("Slutförd", 0);
        counts.put("Pågår", 0);
        counts.put("Arbetsorder skapad", 0);
        counts.put("Skapad", 0);
        counts.put("Bokad", 0);
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
            list.getChildren().add(mutedNote("Inga arbetsorder ännu"));
        }

        VBox box = new VBox(12, title, sub, list);
        box.getStyleClass().add("panel");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private VBox bookingsPanel(List<Booking> bookings) {
        Label title = new Label("Kommande bokningar");
        title.getStyleClass().add("panel-title");
        Label sub = new Label("Nästa inbokade jobb");
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
            list.getChildren().add(mutedNote("Inga bokningar ännu"));
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
                col("Namn", 200, c -> c.getName()),
                col("Telefon", 150, c -> c.getPhone()),
                col("E-post", 280, c -> c.getEmail()),
                badge("VIP", 100, c -> c.isVip() ? "Ja" : "Nej"));
        return t;
    }

    private TableView<Vehicle> buildVehiclesTable() {
        TableView<Vehicle> t = make(garage.getVehicles());
        t.getColumns().addAll(
                col("ID", 60, c -> String.valueOf(c.getId())),
                col("Reg.nr", 120, c -> c.getRegistrationNumber()),
                col("Märke", 140, c -> c.getBrand()),
                col("Modell", 160, c -> c.getModel()),
                col("Årsmodell", 100, c -> String.valueOf(c.getYear())),
                col("Kund", 220, c -> customerName(c.getCustomerId())));
        return t;
    }

    private TableView<Booking> buildBookingsTable() {
        TableView<Booking> t = make(garage.getBookings());
        t.getColumns().addAll(
                col("ID", 60, c -> String.valueOf(c.getId())),
                col("Fordon", 140, c -> vehicleReg(c.getVehicleId())),
                col("Datum", 130, c -> String.valueOf(c.getDate())),
                col("Beskrivning", 320, c -> c.getDescription()),
                badge("Status", 140, c -> statusWord(c.getStatus())));
        return t;
    }

    private TableView<WorkOrder> buildRecentOrders(List<WorkOrder> orders) {
        TableView<WorkOrder> t = make(orders);
        t.getColumns().addAll(
                col("ID", 70, c -> String.valueOf(c.getId())),
                col("Bokning", 90, c -> String.valueOf(c.getBookingId())),
                col("Mekaniker", 180, c -> mechanicName(c.getMechanicId())),
                col("Tjänster", 300, c -> serviceNames(c.getServiceItemIds())),
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
                col("Namn", 220, c -> c.getName()),
                col("Beskrivning", 360, c -> c.getDescription()),
                col("Pris", 120, c -> money.format(c.getPrice()) + " kr"),
                col("Tid", 100, c -> c.getEstimatedMinutes() + " min"));
        return t;
    }

    private TableView<Mechanic> buildMechanicsTable() {
        TableView<Mechanic> t = make(garage.getMechanics());
        t.getColumns().addAll(
                col("ID", 70, c -> String.valueOf(c.getId())),
                col("Namn", 220, c -> c.getName()),
                col("Telefon", 160, c -> c.getPhone()),
                col("Specialisering", 260, c -> c.getSpecialization()),
                badge("Tillgänglig", 130, c -> c.isAvailable() ? "Ja" : "Nej"));
        return t;
    }

    private TableView<Invoice> buildInvoicesTable() {
        TableView<Invoice> t = make(garage.getInvoices());
        t.getColumns().addAll(
                col("ID", 70, c -> String.valueOf(c.getId())),
                col("Arbetsorder", 110, c -> String.valueOf(c.getWorkOrderId())),
                col("Datum", 130, c -> String.valueOf(c.getInvoiceDate())),
                col("Belopp", 110, c -> money.format(c.getAmount()) + " kr"),
                col("Rabatt", 100, c -> money.format(c.getDiscount()) + " kr"),
                col("Totalt", 110, c -> money.format(c.getTotalAmount()) + " kr"),
                badge("Betald", 110, c -> c.isPaid() ? "Ja" : "Nej"));
        return t;
    }

    private TableView<Payment> buildPaymentsTable() {
        TableView<Payment> t = make(garage.getPayments());
        t.getColumns().addAll(
                col("ID", 70, c -> String.valueOf(c.getId())),
                col("Faktura", 100, c -> String.valueOf(c.getInvoiceId())),
                col("Belopp", 120, c -> money.format(c.getAmount()) + " kr"),
                col("Typ", 130, c -> c.getPaymentType()),
                col("Datum/tid", 220, c -> String.valueOf(c.getPaymentDate())),
                badge("Status", 110, c -> c.isSuccessful() ? "Lyckad" : "Misslyckad"));
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
        return s.equals("Ja") || s.equals("Lyckad")
                || s.equals("Slutförd") || s.equals("Betald");
    }

    private static String badgeClass(String s) {
        if (s == null) {
            return "";
        }
        if (isGood(s)) {
            return "success";
        }
        if (s.equals("Nej") || s.equals("Misslyckad")) {
            return "danger";
        }
        if (s.equals("Pågår")) {
            return "warn";
        }
        if (s.equals("Bokad") || s.equals("Skapad") || s.equals("Arbetsorder skapad")) {
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

    private static String todaySwedish() {
        String[] week = {"", "Måndag", "Tisdag", "Onsdag", "Torsdag", "Fredag", "Lördag", "Söndag"};
        String[] months = {"", "januari", "februari", "mars", "april", "maj", "juni",
                "juli", "augusti", "september", "oktober", "november", "december"};
        LocalDate d = LocalDate.now();
        return week[d.getDayOfWeek().getValue()] + " " + d.getDayOfMonth()
                + " " + months[d.getMonthValue()] + " " + d.getYear();
    }

    private String statusWord(String status) {
        if (status == null) {
            return "";
        }
        if (status.equals("BOOKED")) {
            return "Bokad";
        }
        if (status.equals("CREATED")) {
            return "Skapad";
        }
        if (status.equals("WORK_ORDER_CREATED")) {
            return "Arbetsorder skapad";
        }
        if (status.equals("IN_PROGRESS")) {
            return "Pågår";
        }
        if (status.equals("COMPLETED")) {
            return "Slutförd";
        }
        return status;
    }

    private String customerName(int id) {
        for (Customer c : garage.getCustomers()) {
            if (c.getId() == id) {
                return c.getName();
            }
        }
        return "Kund #" + id;
    }

    private String vehicleReg(int id) {
        for (Vehicle v : garage.getVehicles()) {
            if (v.getId() == id) {
                return v.getRegistrationNumber();
            }
        }
        return "Fordon #" + id;
    }

    private String mechanicName(int id) {
        for (Mechanic m : garage.getMechanics()) {
            if (m.getId() == id) {
                return m.getName();
            }
        }
        return "Mekaniker #" + id;
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
            sb.append(found != null ? found : "Tjänst #" + sid);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
