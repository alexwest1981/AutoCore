package com.wac.autocore.ui.views;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.components.TableFactory;
import com.wac.autocore.ui.components.UiComponents;
import com.wac.autocore.ui.navigation.PageRouter;
import com.wac.autocore.ui.util.EntityLookup;
import com.wac.autocore.ui.util.GlobalSearch;
import com.wac.autocore.ui.util.GlobalSearch.SearchResults;
import com.wac.autocore.ui.util.UiFormatters;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Samlad global sökresultatsvy som delar in träffar i tydliga sektioner
 * (Kunder, Fordon, Arbetsordrar, Bokningar, Mekaniker, Fakturor, Tjänster)
 * med direktlänkar och interaktiva tabeller.
 */
public final class SearchResultsView {

    private SearchResultsView() {}

    public static Node build(GarageSystem garage, PageRouter router, String query) {
        SearchResults results = GlobalSearch.search(garage, query);

        VBox content = new VBox(18);

        // Sidhuvud
        String qDisplay = (query == null || query.trim().isEmpty()) ? "" : query.trim();
        String title = qDisplay.isEmpty() ? "Global Search" : "Search results for \"" + qDisplay + "\"";
        String sub = results.isEmpty()
                ? (qDisplay.isEmpty() ? "Type in the top search bar to search across all system records"
                                      : "No matches found across any section")
                : results.getTotalMatches() + " matches found across "
                  + results.getSectionsWithMatchesCount() + " "
                  + (results.getSectionsWithMatchesCount() == 1 ? "section" : "sections");

        VBox head = UiComponents.pageHead(title, sub, "GLOBAL SEARCH");
        content.getChildren().add(head);

        if (results.isEmpty() && !qDisplay.isEmpty()) {
            content.getChildren().add(buildEmptyState(qDisplay));
            return content;
        }

        // Sektion 1: Kunder
        if (!results.getCustomers().isEmpty()) {
            content.getChildren().add(buildCustomersSection(results.getCustomers(), router));
        }

        // Sektion 2: Fordon
        if (!results.getVehicles().isEmpty()) {
            content.getChildren().add(buildVehiclesSection(results.getVehicles(), garage, router));
        }

        // Sektion 3: Arbetsordrar
        if (!results.getWorkOrders().isEmpty()) {
            content.getChildren().add(buildWorkOrdersSection(results.getWorkOrders(), garage, router));
        }

        // Sektion 4: Bokningar
        if (!results.getBookings().isEmpty()) {
            content.getChildren().add(buildBookingsSection(results.getBookings(), garage, router));
        }

        // Sektion 5: Mekaniker
        if (!results.getMechanics().isEmpty()) {
            content.getChildren().add(buildMechanicsSection(results.getMechanics(), router));
        }

        // Sektion 6: Fakturor
        if (!results.getInvoices().isEmpty()) {
            content.getChildren().add(buildInvoicesSection(results.getInvoices(), garage, router));
        }

        // Sektion 7: Tjänster
        if (!results.getServices().isEmpty()) {
            content.getChildren().add(buildServicesSection(results.getServices(), router));
        }

        return content;
    }

    private static Node buildEmptyState(String query) {
        VBox emptyCard = new VBox(10);
        emptyCard.getStyleClass().add("panel");
        emptyCard.setPadding(new Insets(28, 24, 28, 24));
        emptyCard.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("No matches found for \"" + query + "\"");
        title.getStyleClass().add("panel-title");

        Label sub = new Label("No customers, vehicles, work orders, bookings or mechanics matched your query.");
        sub.getStyleClass().add("page-sub");

        Label tip = new Label("Tips: Try searching for a customer name (e.g. Anna), vehicle registration number (e.g. ABC 123), car brand (e.g. Volvo), or mechanic name.");
        tip.getStyleClass().addAll("srow-sub", "small");

        emptyCard.getChildren().addAll(title, sub, tip);
        return emptyCard;
    }

    private static VBox createSectionContainer(String titleText, int count, String actionText, Runnable onAction, TableView<?> table) {
        Label title = new Label(titleText);
        title.getStyleClass().add("panel-title");

        Label countBadge = new Label(String.valueOf(count));
        countBadge.getStyleClass().addAll("badge", "info");

        HBox left = new HBox(8, title, countBadge);
        left.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button navBtn = UiComponents.secondaryButton(actionText);
        navBtn.setOnAction(e -> onAction.run());

        HBox header = new HBox(12, left, spacer, navBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        // Anpassa höjd efter antal rader så vyn blir kompakt och överskådlig
        double targetHeight = Math.min(240, 42 + count * 36);
        table.setPrefHeight(targetHeight);
        table.setMinHeight(targetHeight);
        table.setMaxHeight(260);

        HBox.setHgrow(table, Priority.ALWAYS);
        VBox.setVgrow(table, Priority.NEVER);

        VBox panel = new VBox(12, header, table);
        panel.getStyleClass().add("panel");
        return panel;
    }

    private static Node buildCustomersSection(List<Customer> list, PageRouter router) {
        TableView<Customer> table = TableFactory.create(list).getTableView();
        table.getColumns().add(TableFactory.col("ID", 60, c -> String.valueOf(c.getId())));
        table.getColumns().add(TableFactory.col("Name", 200, Customer::getName));
        table.getColumns().add(TableFactory.col("Phone", 150, Customer::getPhone));
        table.getColumns().add(TableFactory.col("Email", 260, Customer::getEmail));
        table.getColumns().add(TableFactory.badgeCol("VIP", 80, c -> c.isVip() ? "Yes" : "No"));

        table.setRowFactory(tv -> {
            TableRow<Customer> row = new TableRow<Customer>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    router.navigate("customers");
                }
            });
            return row;
        });

        return createSectionContainer("Customers", list.size(), "Open in Customers →",
                () -> router.navigate("customers"), table);
    }

    private static Node buildVehiclesSection(List<Vehicle> list, GarageSystem garage, PageRouter router) {
        TableView<Vehicle> table = TableFactory.create(list).getTableView();
        table.getColumns().add(TableFactory.col("ID", 60, v -> String.valueOf(v.getId())));
        table.getColumns().add(TableFactory.col("Reg. no.", 120, Vehicle::getRegistrationNumber));
        table.getColumns().add(TableFactory.col("Make", 140, Vehicle::getBrand));
        table.getColumns().add(TableFactory.col("Model", 160, Vehicle::getModel));
        table.getColumns().add(TableFactory.col("Year", 90, v -> String.valueOf(v.getYear())));
        table.getColumns().add(TableFactory.col("Owner", 200, v -> EntityLookup.customerName(garage, v.getCustomerId())));

        table.setRowFactory(tv -> {
            TableRow<Vehicle> row = new TableRow<Vehicle>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    router.navigate("vehicles");
                }
            });
            return row;
        });

        return createSectionContainer("Vehicles", list.size(), "Open in Vehicles →",
                () -> router.navigate("vehicles"), table);
    }

    private static Node buildWorkOrdersSection(List<WorkOrder> list, GarageSystem garage, PageRouter router) {
        TableView<WorkOrder> table = TableFactory.create(list).getTableView();
        table.getColumns().add(TableFactory.col("Order ID", 80, wo -> "#" + wo.getId()));
        table.getColumns().add(TableFactory.col("Vehicle", 120, wo -> EntityLookup.workOrderVehicleReg(garage, wo)));
        table.getColumns().add(TableFactory.col("Customer", 180, wo -> EntityLookup.workOrderCustomerName(garage, wo)));
        table.getColumns().add(TableFactory.col("Mechanic", 160, wo -> EntityLookup.mechanicName(garage, wo.getMechanicId())));
        table.getColumns().add(TableFactory.col("Services", 240, wo -> EntityLookup.serviceNames(garage, wo.getServiceItemIds())));
        table.getColumns().add(TableFactory.badgeCol("Status", 120, wo -> UiFormatters.statusWord(wo.getStatus())));

        table.setRowFactory(tv -> {
            TableRow<WorkOrder> row = new TableRow<WorkOrder>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    router.navigate("workorders");
                }
            });
            return row;
        });

        return createSectionContainer("Work Orders", list.size(), "Open in Work Orders →",
                () -> router.navigate("workorders"), table);
    }

    private static Node buildBookingsSection(List<Booking> list, GarageSystem garage, PageRouter router) {
        TableView<Booking> table = TableFactory.create(list).getTableView();
        table.getColumns().add(TableFactory.col("Booking ID", 90, b -> "#" + b.getId()));
        table.getColumns().add(TableFactory.col("Date", 120, b -> UiFormatters.formatDate(b.getDate())));
        table.getColumns().add(TableFactory.col("Customer", 180, b -> EntityLookup.bookingCustomerName(garage, b.getId())));
        table.getColumns().add(TableFactory.col("Vehicle", 120, b -> EntityLookup.vehicleReg(garage, b.getVehicleId())));
        table.getColumns().add(TableFactory.col("Description", 240, Booking::getDescription));
        table.getColumns().add(TableFactory.badgeCol("Status", 120, b -> UiFormatters.statusWord(b.getStatus())));

        table.setRowFactory(tv -> {
            TableRow<Booking> row = new TableRow<Booking>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    router.navigate("bookings");
                }
            });
            return row;
        });

        return createSectionContainer("Bookings", list.size(), "Open in Bookings →",
                () -> router.navigate("bookings"), table);
    }

    private static Node buildMechanicsSection(List<Mechanic> list, PageRouter router) {
        TableView<Mechanic> table = TableFactory.create(list).getTableView();
        table.getColumns().add(TableFactory.col("ID", 60, m -> String.valueOf(m.getId())));
        table.getColumns().add(TableFactory.col("Name", 180, Mechanic::getName));
        table.getColumns().add(TableFactory.col("Phone", 140, Mechanic::getPhone));
        table.getColumns().add(TableFactory.col("Specialisation", 220, Mechanic::getSpecialization));
        table.getColumns().add(TableFactory.badgeCol("Available", 110, m -> m.isAvailable() ? "Yes" : "No"));

        table.setRowFactory(tv -> {
            TableRow<Mechanic> row = new TableRow<Mechanic>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    router.navigate("mechanics");
                }
            });
            return row;
        });

        return createSectionContainer("Mechanics", list.size(), "Open in Mechanics →",
                () -> router.navigate("mechanics"), table);
    }

    private static Node buildInvoicesSection(List<Invoice> list, GarageSystem garage, PageRouter router) {
        TableView<Invoice> table = TableFactory.create(list).getTableView();
        table.getColumns().add(TableFactory.col("Invoice ID", 90, inv -> "#" + inv.getId()));
        table.getColumns().add(TableFactory.col("Customer", 180, inv -> EntityLookup.invoiceCustomerName(garage, inv)));
        table.getColumns().add(TableFactory.col("Work Order", 100, inv -> "#" + inv.getWorkOrderId()));
        table.getColumns().add(TableFactory.col("Date", 120, inv -> String.valueOf(inv.getInvoiceDate())));
        table.getColumns().add(TableFactory.col("Total", 120, inv -> UiFormatters.formatMoney(inv.getTotalAmount())));
        table.getColumns().add(TableFactory.badgeCol("Paid", 100, inv -> inv.isPaid() ? "Yes" : "No"));

        table.setRowFactory(tv -> {
            TableRow<Invoice> row = new TableRow<Invoice>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    router.navigate("invoices");
                }
            });
            return row;
        });

        return createSectionContainer("Invoices", list.size(), "Open in Invoices →",
                () -> router.navigate("invoices"), table);
    }

    private static Node buildServicesSection(List<ServiceItem> list, PageRouter router) {
        TableView<ServiceItem> table = TableFactory.create(list).getTableView();
        table.getColumns().add(TableFactory.col("ID", 60, s -> String.valueOf(s.getId())));
        table.getColumns().add(TableFactory.col("Name", 200, ServiceItem::getName));
        table.getColumns().add(TableFactory.col("Description", 300, ServiceItem::getDescription));
        table.getColumns().add(TableFactory.col("Price", 120, s -> UiFormatters.formatMoney(s.getPrice())));
        table.getColumns().add(TableFactory.col("Time", 100, s -> s.getEstimatedMinutes() + " min"));

        table.setRowFactory(tv -> {
            TableRow<ServiceItem> row = new TableRow<ServiceItem>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    router.navigate("services");
                }
            });
            return row;
        });

        return createSectionContainer("Services", list.size(), "Open in Services →",
                () -> router.navigate("services"), table);
    }
}
