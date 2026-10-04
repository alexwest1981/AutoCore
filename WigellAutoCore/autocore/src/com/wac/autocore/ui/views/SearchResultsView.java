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
import com.wac.autocore.ui.i18n.I18n;
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
import com.wac.autocore.seed.SeedText;

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
        String title = qDisplay.isEmpty()
                ? I18n.get("search.global.title")
                : I18n.get("search.global.query_title", qDisplay);
        String sub = results.isEmpty()
                ? (qDisplay.isEmpty() ? I18n.get("search.global.hint")
                                      : I18n.get("search.global.no_matches"))
                : I18n.get("search.global.matches_found",
                        results.getTotalMatches(),
                        results.getSectionsWithMatchesCount(),
                        (results.getSectionsWithMatchesCount() == 1 ? I18n.get("search.global.section") : I18n.get("search.global.sections")));

        VBox head = UiComponents.pageHead(title, sub, I18n.get("search.global.title").toUpperCase());
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

        Label title = new Label(I18n.get("search.results.empty_title", query));
        title.getStyleClass().add("panel-title");

        Label sub = new Label(I18n.get("search.results.empty_desc"));
        sub.getStyleClass().add("page-sub");

        Label tip = new Label(I18n.get("search.results.empty_tips"));
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
        table.getColumns().add(TableFactory.idCol(c -> String.valueOf(c.getId())));
        table.getColumns().add(TableFactory.textCol(I18n.get("table.col.name"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, Customer::getName));
        table.getColumns().add(TableFactory.sizeCol(I18n.get("table.col.phone"), TableFactory.W_PHONE, Customer::getPhone));
        table.getColumns().add(TableFactory.textCol(I18n.get("table.col.email"), TableFactory.W_EMAIL_MIN, TableFactory.W_EMAIL_MAX, Customer::getEmail));
        table.getColumns().add(TableFactory.sizeBadge(I18n.get("table.col.vip"), TableFactory.W_FLAG, c -> c.isVip() ? I18n.get("common.yes") : I18n.get("common.no")));

        table.setRowFactory(tv -> {
            TableRow<Customer> row = new TableRow<Customer>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    router.navigate("customers");
                }
            });
            return row;
        });

        return createSectionContainer(I18n.get("search.category.customers"), list.size(), I18n.get("entity.customers.title") + " →",
                () -> router.navigate("customers"), table);
    }

    private static Node buildVehiclesSection(List<Vehicle> list, GarageSystem garage, PageRouter router) {
        TableView<Vehicle> table = TableFactory.create(list).getTableView();
        table.getColumns().add(TableFactory.idCol(v -> String.valueOf(v.getId())));
        table.getColumns().add(TableFactory.sizeCol(I18n.get("table.col.reg_nr"), TableFactory.W_REG_NR, Vehicle::getRegistrationNumber));
        table.getColumns().add(TableFactory.sizeCol(I18n.get("table.col.brand"), TableFactory.W_BRAND, Vehicle::getBrand));
        table.getColumns().add(TableFactory.sizeCol(I18n.get("table.col.model"), TableFactory.W_MODEL, Vehicle::getModel));
        table.getColumns().add(TableFactory.sizeCol(I18n.get("table.col.year"), TableFactory.W_YEAR, v -> String.valueOf(v.getYear())));
        table.getColumns().add(TableFactory.textCol(I18n.get("table.col.customer"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, v -> EntityLookup.customerName(garage, v.getCustomerId())));

        table.setRowFactory(tv -> {
            TableRow<Vehicle> row = new TableRow<Vehicle>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    router.navigate("vehicles");
                }
            });
            return row;
        });

        return createSectionContainer(I18n.get("search.category.vehicles"), list.size(), I18n.get("entity.vehicles.title") + " →",
                () -> router.navigate("vehicles"), table);
    }

    private static Node buildWorkOrdersSection(List<WorkOrder> list, GarageSystem garage, PageRouter router) {
        TableView<WorkOrder> table = TableFactory.create(list).getTableView();
        table.getColumns().add(TableFactory.sizeCol(I18n.get("table.col.workorder"), TableFactory.W_REF, wo -> "#" + wo.getId()));
        table.getColumns().add(TableFactory.sizeCol(I18n.get("table.col.vehicle"), TableFactory.W_REG_NR, wo -> EntityLookup.workOrderVehicleReg(garage, wo)));
        table.getColumns().add(TableFactory.textCol(I18n.get("table.col.customer"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, wo -> EntityLookup.workOrderCustomerName(garage, wo)));
        table.getColumns().add(TableFactory.textCol(I18n.get("table.col.mechanic"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, wo -> EntityLookup.mechanicName(garage, wo.getMechanicId())));
        table.getColumns().add(TableFactory.textCol(I18n.get("table.col.services"), TableFactory.W_SERVICES_MIN, TableFactory.W_SERVICES_MAX, wo -> EntityLookup.workOrderServicesWithPrices(garage, wo)));
        table.getColumns().add(TableFactory.sizeBadge(I18n.get("table.col.status"), TableFactory.W_STATUS, wo -> UiFormatters.statusWord(wo.getStatus())));

        table.setRowFactory(tv -> {
            TableRow<WorkOrder> row = new TableRow<WorkOrder>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    router.navigate("workorders");
                }
            });
            return row;
        });

        return createSectionContainer(I18n.get("search.category.workorders"), list.size(), I18n.get("entity.workorders.title") + " →",
                () -> router.navigate("workorders"), table);
    }

    private static Node buildBookingsSection(List<Booking> list, GarageSystem garage, PageRouter router) {
        TableView<Booking> table = TableFactory.create(list).getTableView();
        table.getColumns().add(TableFactory.sizeCol(I18n.get("table.col.booking"), TableFactory.W_REF, b -> "#" + b.getId()));
        table.getColumns().add(TableFactory.sizeCol(I18n.get("table.col.date"), TableFactory.W_DATE, b -> UiFormatters.formatDate(b.getDate())));
        table.getColumns().add(TableFactory.textCol(I18n.get("table.col.customer"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, b -> EntityLookup.bookingCustomerName(garage, b.getId())));
        table.getColumns().add(TableFactory.sizeCol(I18n.get("table.col.vehicle"), TableFactory.W_REG_NR, b -> EntityLookup.vehicleReg(garage, b.getVehicleId())));
        table.getColumns().add(TableFactory.textCol(I18n.get("table.col.description"), TableFactory.W_TEXT_MIN, TableFactory.W_TEXT_MAX, b -> SeedText.resolve(b.getDescription())));
        table.getColumns().add(TableFactory.sizeBadge(I18n.get("table.col.status"), TableFactory.W_STATUS, b -> UiFormatters.statusWord(b.getStatus())));

        table.setRowFactory(tv -> {
            TableRow<Booking> row = new TableRow<Booking>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    router.navigate("bookings");
                }
            });
            return row;
        });

        return createSectionContainer(I18n.get("search.category.bookings"), list.size(), I18n.get("entity.bookings.title") + " →",
                () -> router.navigate("bookings"), table);
    }

    private static Node buildMechanicsSection(List<Mechanic> list, PageRouter router) {
        TableView<Mechanic> table = TableFactory.create(list).getTableView();
        table.getColumns().add(TableFactory.idCol(m -> String.valueOf(m.getId())));
        table.getColumns().add(TableFactory.textCol(I18n.get("table.col.name"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, Mechanic::getName));
        table.getColumns().add(TableFactory.sizeCol(I18n.get("table.col.phone"), TableFactory.W_PHONE, Mechanic::getPhone));
        table.getColumns().add(TableFactory.textCol(I18n.get("table.col.specialisation"), TableFactory.W_SPEC_MIN, TableFactory.W_SPEC_MAX, m -> SeedText.resolve(m.getSpecialization())));
        table.getColumns().add(TableFactory.sizeBadge(I18n.get("table.col.available"), TableFactory.W_FLAG, m -> m.isAvailable() ? I18n.get("common.yes") : I18n.get("common.no")));

        table.setRowFactory(tv -> {
            TableRow<Mechanic> row = new TableRow<Mechanic>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    router.navigate("mechanics");
                }
            });
            return row;
        });

        return createSectionContainer(I18n.get("search.category.mechanics"), list.size(), I18n.get("entity.mechanics.title") + " →",
                () -> router.navigate("mechanics"), table);
    }

    private static Node buildInvoicesSection(List<Invoice> list, GarageSystem garage, PageRouter router) {
        TableView<Invoice> table = TableFactory.create(list).getTableView();
        table.getColumns().add(TableFactory.sizeCol(I18n.get("table.col.invoice"), TableFactory.W_REF, inv -> "#" + inv.getId()));
        table.getColumns().add(TableFactory.textCol(I18n.get("table.col.customer"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, inv -> EntityLookup.invoiceCustomerName(garage, inv)));
        table.getColumns().add(TableFactory.sizeCol(I18n.get("table.col.workorder"), TableFactory.W_REF, inv -> "#" + inv.getWorkOrderId()));
        table.getColumns().add(TableFactory.sizeCol(I18n.get("table.col.date"), TableFactory.W_DATE, inv -> String.valueOf(inv.getInvoiceDate())));
        table.getColumns().add(TableFactory.sizeCol(I18n.get("table.col.total"), TableFactory.W_MONEY, inv -> UiFormatters.formatMoney(inv.getTotalAmount())));
        table.getColumns().add(TableFactory.sizeBadge(I18n.get("table.col.paid"), TableFactory.W_FLAG, inv -> inv.isPaid() ? I18n.get("common.yes") : I18n.get("common.no")));

        table.setRowFactory(tv -> {
            TableRow<Invoice> row = new TableRow<Invoice>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    router.navigate("invoices");
                }
            });
            return row;
        });

        return createSectionContainer(I18n.get("search.category.invoices"), list.size(), I18n.get("entity.invoices.title") + " →",
                () -> router.navigate("invoices"), table);
    }

    private static Node buildServicesSection(List<ServiceItem> list, PageRouter router) {
        TableView<ServiceItem> table = TableFactory.create(list).getTableView();
        table.getColumns().add(TableFactory.idCol(s -> String.valueOf(s.getId())));
        table.getColumns().add(TableFactory.textCol(I18n.get("table.col.name"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, s -> SeedText.resolve(s.getName())));
        table.getColumns().add(TableFactory.textCol(I18n.get("table.col.description"), TableFactory.W_TEXT_MIN, TableFactory.W_TEXT_MAX, s -> SeedText.resolve(s.getDescription())));
        table.getColumns().add(TableFactory.sizeCol(I18n.get("table.col.price"), TableFactory.W_MONEY, s -> UiFormatters.formatMoney(s.getPrice())));
        table.getColumns().add(TableFactory.sizeCol(I18n.get("table.col.time"), TableFactory.W_MINUTES, s -> s.getEstimatedMinutes() + " min"));

        table.setRowFactory(tv -> {
            TableRow<ServiceItem> row = new TableRow<ServiceItem>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    router.navigate("services");
                }
            });
            return row;
        });

        return createSectionContainer(I18n.get("search.category.services"), list.size(), I18n.get("entity.services.title") + " →",
                () -> router.navigate("services"), table);
    }
}
