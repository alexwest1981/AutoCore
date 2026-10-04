package com.wac.autocore.ui;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.UiFormatters;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;

/**
 * Fakturan som den skrivs ut: ett papper att förhandsgranska och skriva ut, med företagets namn och
 * logotyp, kundens och fordonets uppgifter, alla utförda arbeten med priser och fakturans summa.
 *
 * Färgerna sätts här och inte av temat — pappret är vitt även när programmet körs i mörkt tema, och
 * det som skrivs ut ska se likadant ut som förhandsgranskningen.
 */
public final class InvoiceDocument {

    /** Företagsuppgifterna i fakturahuvudet. Ändra här när verkstadens adress ändras. */
    private static final String COMPANY_NAME = "Wigell AutoCore";
    private static final String COMPANY_STREET = "Verkstadsvägen 1";
    private static final String COMPANY_POSTAL = "512 00 Svenljunga";
    private static final String COMPANY_PHONE = "070-000 00 00";
    private static final String COMPANY_EMAIL = "info@wigellautocore.se";

    /** Betalningsvillkor i dagar. Räknas från fakturadatumet och visas som förfallodatum. */
    private static final int PAYMENT_TERMS_DAYS = 30;

    /** Papprets bredd i punkter (A4 med marginal), så förhandsgranskningen liknar det utskrivna. */
    private static final double PAPER_WIDTH = 520;

    private static final String INK = "-fx-text-fill: #111827;";
    private static final String MUTED = "-fx-text-fill: #6b7280;";

    private InvoiceDocument() {}

    public static Node build(GarageSystem garage, Invoice invoice) {
        VBox paper = new VBox(0);
        paper.setPrefWidth(PAPER_WIDTH);
        paper.setMinWidth(PAPER_WIDTH);
        paper.setMaxWidth(PAPER_WIDTH);
        paper.setPadding(new Insets(28, 32, 32, 32));
        paper.setStyle("-fx-background-color: #ffffff;");

        paper.getChildren().addAll(
                header(),
                rule(),
                title(invoice),
                rule(),
                parties(garage, invoice),
                new Spacer(18),
                lines(invoice),
                new Spacer(14),
                totals(invoice),
                new Spacer(24),
                footer());
        return paper;
    }

    // ─────────────────────────────────────────────────────────────────── huvudet

    private static Node header() {
        Image logo = com.wac.autocore.ui.components.UiComponents.loadLogoImage();
        Node left;
        if (logo != null && !logo.isError()) {
            ImageView view = new ImageView(logo);
            view.setPreserveRatio(true);
            view.setFitWidth(150);
            view.setSmooth(true);
            left = view;
        } else {
            left = label(COMPANY_NAME, 22, true, INK);
        }

        VBox company = new VBox(2,
                right(label(COMPANY_NAME, 13, true, INK)),
                right(label(COMPANY_STREET, 11, false, MUTED)),
                right(label(COMPANY_POSTAL, 11, false, MUTED)),
                right(label(COMPANY_PHONE + "  ·  " + COMPANY_EMAIL, 11, false, MUTED)));

        HBox head = new HBox(16, left, growing(), company);
        head.setAlignment(Pos.TOP_LEFT);
        head.setPadding(new Insets(0, 0, 20, 0));
        return head;
    }

    private static Node title(Invoice invoice) {
        VBox titleBox = new VBox(4, label(I18n.get("invoice.title"), 20, true, INK),
                label(I18n.get("invoice.thanks_line"), 11, false, MUTED));

        GridPane facts = new GridPane();
        facts.setHgap(14);
        facts.setVgap(3);
        facts.add(right(label(I18n.get("invoice.number"), 11, false, MUTED)), 0, 0);
        facts.add(right(label("#" + invoice.getId(), 11, true, INK)), 1, 0);
        facts.add(right(label(I18n.get("invoice.date"), 11, false, MUTED)), 0, 1);
        facts.add(right(label(String.valueOf(invoice.getInvoiceDate()), 11, false, INK)), 1, 1);
        facts.add(right(label(I18n.get("invoice.due_date"), 11, false, MUTED)), 0, 2);
        facts.add(right(label(String.valueOf(dueDate(invoice)), 11, false, INK)), 1, 2);
        facts.add(right(label(I18n.get("invoice.status"), 11, false, MUTED)), 0, 3);
        facts.add(right(label(I18n.get(invoice.isPaid() ? "status.paid" : "status.unpaid"), 11, true,
                invoice.isPaid() ? "-fx-text-fill: #047857;" : "-fx-text-fill: #b91c1c;")), 1, 3);

        HBox row = new HBox(20, titleBox, growing(), facts);
        row.setPadding(new Insets(18, 0, 18, 0));
        return row;
    }

    // ─────────────────────────────────────────────────────────────────── parterna

    private static Node parties(GarageSystem garage, Invoice invoice) {
        Customer customer = customerFor(garage, invoice);
        Vehicle vehicle = vehicleFor(garage, invoice);

        VBox customerBox = block(I18n.get("invoice.customer"),
                customer == null ? "-" : customer.getName(),
                customer == null || customer.getPhone() == null ? "" : customer.getPhone(),
                customer == null || customer.getEmail() == null ? "" : customer.getEmail());

        StringBuilder car = new StringBuilder();
        StringBuilder extra = new StringBuilder();
        if (vehicle != null) {
            car.append(vehicle.getRegistrationNumber());
            extra.append(vehicle.getBrand()).append(" ").append(vehicle.getModel())
                    .append(" · ").append(vehicle.getYear());
        }
        VBox vehicleBox = block(I18n.get("invoice.vehicle"),
                car.length() == 0 ? "-" : car.toString(),
                extra.toString(),
                I18n.get("invoice.booking") + " #" + bookingIdFor(garage, invoice));

        HBox row = new HBox(40, customerBox, vehicleBox);
        row.setPadding(new Insets(18, 0, 4, 0));
        return row;
    }

    private static VBox block(String heading, String... lines) {
        VBox box = new VBox(2, label(heading.toUpperCase(), 10, true, MUTED));
        box.setPrefWidth(230);
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            box.getChildren().add(label(com.wac.autocore.seed.SeedText.resolve(line), 11, false, INK));
        }
        return box;
    }

    // ─────────────────────────────────────────────────────────────────── raderna

    private static Node lines(Invoice invoice) {
        GridPane grid = new GridPane();
        grid.setHgap(0);
        grid.setVgap(6);
        grid.getColumnConstraints().addAll(
                column(Priority.ALWAYS, HPos.LEFT),
                column(70, HPos.RIGHT),
                column(70, HPos.RIGHT),
                column(80, HPos.RIGHT));

        addRow(grid, 0,
                label(I18n.get("invoice.col.description"), 10, true, MUTED),
                right(label(I18n.get("invoice.col.price"), 10, true, MUTED)),
                right(label(I18n.get("invoice.col.discount"), 10, true, MUTED)),
                right(label(I18n.get("invoice.col.sum"), 10, true, MUTED)));

        int row = 1;
        if (invoice.getLines() != null) {
            for (InvoiceLine line : invoice.getLines()) {
                addRow(grid, row++,
                        label(com.wac.autocore.seed.SeedText.resolve(line.getServiceName()), 11, false, INK),
                        right(label(UiFormatters.formatMoney(line.getPrice()), 11, false, INK)),
                        right(label(UiFormatters.formatMoney(line.getDiscount()), 11, false, MUTED)),
                        right(label(UiFormatters.formatMoney(line.getFinalPrice()), 11, true, INK)));
            }
        }
        return grid;
    }

    // ─────────────────────────────────────────────────────────────────── summan

    private static Node totals(Invoice invoice) {
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(4);
        grid.getColumnConstraints().addAll(column(Priority.ALWAYS, HPos.RIGHT), column(110, HPos.RIGHT));

        grid.add(right(label(I18n.get("invoice.subtotal"), 11, false, MUTED)), 0, 0);
        grid.add(right(label(UiFormatters.formatMoney(invoice.getAmount()), 11, false, INK)), 1, 0);
        if (invoice.getDiscount() > 0) {
            grid.add(right(label(I18n.get("invoice.discount_total"), 11, false, MUTED)), 0, 1);
            grid.add(right(label("- " + UiFormatters.formatMoney(invoice.getDiscount()), 11, false, INK)), 1, 1);
        }
        grid.add(right(label(I18n.get("invoice.vat"), 11, false, MUTED)), 0, 2);
        grid.add(right(label(UiFormatters.formatMoney(invoice.getVatAmount()), 11, false, INK)), 1, 2);

        // Att betala är beloppet kunden ska betala, alltså med moms på.
        Label totalLabel = label(I18n.get("invoice.total"), 13, true, INK);
        Label totalValue = label(UiFormatters.formatMoney(invoice.getTotalIncludingVat()), 13, true, INK);
        grid.add(right(totalLabel), 0, 3);
        grid.add(right(totalValue), 1, 3);

        VBox box = new VBox(8, rule(), grid);
        box.setPadding(new Insets(4, 0, 0, 0));
        return box;
    }

    private static Node footer() {
        VBox footer = new VBox(3,
                label(I18n.get("invoice.footer"), 10, false, MUTED),
                label(I18n.get("invoice.footer_terms", PAYMENT_TERMS_DAYS), 10, false, MUTED),
                label(COMPANY_NAME + " · " + COMPANY_PHONE + " · " + COMPANY_EMAIL, 10, false, MUTED));
        footer.setPadding(new Insets(12, 0, 0, 0));
        return footer;
    }

    // ─────────────────────────────────────────────────────────────────── smådelar

    static LocalDate dueDate(Invoice invoice) {
        LocalDate date = invoice.getInvoiceDate() == null ? LocalDate.now() : invoice.getInvoiceDate();
        return date.plusDays(PAYMENT_TERMS_DAYS);
    }

    private static Customer customerFor(GarageSystem garage, Invoice invoice) {
        for (Booking booking : garage.getBookings()) {
            if (booking.getId() != bookingIdFor(garage, invoice)) {
                continue;
            }
            for (Vehicle vehicle : garage.getVehicles()) {
                if (vehicle.getId() != booking.getVehicleId()) {
                    continue;
                }
                for (Customer customer : garage.getCustomers()) {
                    if (customer.getId() == vehicle.getCustomerId()) {
                        return customer;
                    }
                }
            }
        }
        return null;
    }

    private static Vehicle vehicleFor(GarageSystem garage, Invoice invoice) {
        int bookingId = bookingIdFor(garage, invoice);
        for (Booking booking : garage.getBookings()) {
            if (booking.getId() == bookingId) {
                for (Vehicle vehicle : garage.getVehicles()) {
                    if (vehicle.getId() == booking.getVehicleId()) {
                        return vehicle;
                    }
                }
            }
        }
        return null;
    }

    private static int bookingIdFor(GarageSystem garage, Invoice invoice) {
        for (com.wac.autocore.model.WorkOrder order : garage.getWorkOrders()) {
            if (order.getId() == invoice.getWorkOrderId()) {
                return order.getBookingId();
            }
        }
        return 0;
    }

    private static ColumnConstraints column(Priority grow, HPos alignment) {
        ColumnConstraints c = new ColumnConstraints();
        c.setHgrow(grow);
        c.setHalignment(alignment);
        return c;
    }

    private static ColumnConstraints column(double width, HPos alignment) {
        ColumnConstraints c = new ColumnConstraints(width);
        c.setHalignment(alignment);
        return c;
    }

    private static void addRow(GridPane grid, int row, Node... cells) {
        for (int i = 0; i < cells.length; i++) {
            grid.add(cells[i], i, row);
        }
    }

    private static Label label(String text, double size, boolean bold, String style) {
        Label l = new Label(text == null ? "" : text);
        l.setFont(Font.font("System", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        l.setStyle(style);
        return l;
    }

    private static Label right(Label l) {
        l.setAlignment(Pos.CENTER_RIGHT);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private static Node rule() {
        Region line = new Region();
        line.setPrefHeight(1);
        line.setMinHeight(1);
        line.setStyle("-fx-background-color: #d1d5db;");
        return line;
    }

    private static Node growing() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    /** Tom yta mellan avsnitten, så fakturan får luft. */
    private static final class Spacer extends Region {
        private Spacer(double height) {
            setMinHeight(height);
            setPrefHeight(height);
        }
    }
}
