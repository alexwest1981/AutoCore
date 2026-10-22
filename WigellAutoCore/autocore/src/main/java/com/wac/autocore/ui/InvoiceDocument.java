package com.wac.autocore.ui;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.Seller;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
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
 * The invoice as it is printed, with the company, the customer, the vehicle and the work.
 *
 * Everything an invoice must carry stands here and nowhere else: who sold the work and under which
 * organisation and VAT numbers, who buys it and where, the number from the series, the date, the
 * quantity and the price of each line, the VAT base and the VAT at its rate. A correction is a
 * credit invoice, which points back at the invoice it takes back.
 *
 * The company details come from the settings each time the document is built, so a changed address
 * reaches the next invoice. shortcoming: an invoice that has already been sent is reprinted with the
 * details of today, not the ones it was sent with.
 */
public final class InvoiceDocument {

    /** Paper width in points (A4 with margins), so the preview resembles the printout. */
    private static final double PAPER_WIDTH = 520;

    private static final String INK = "-fx-text-fill: #111827;";
    private static final String MUTED = "-fx-text-fill: #6b7280;";

    private InvoiceDocument() {}

    public static Node build(GarageSystem garage, Invoice invoice) {
        Seller seller = garage.seller();

        VBox paper = new VBox(0);
        paper.setPrefWidth(PAPER_WIDTH);
        paper.setMinWidth(PAPER_WIDTH);
        paper.setMaxWidth(PAPER_WIDTH);
        paper.setPadding(new Insets(28, 32, 32, 32));
        paper.setStyle("-fx-background-color: #ffffff;");

        paper.getChildren().addAll(
                header(seller),
                rule(),
                title(garage, invoice, seller),
                rule(),
                parties(garage, invoice),
                new Spacer(18),
                lines(invoice),
                new Spacer(14),
                totals(invoice),
                new Spacer(24),
                footer(seller));
        return paper;
    }

    private static Node header(Seller seller) {
        Image logo = com.wac.autocore.ui.components.UiComponents.loadLogoImage();
        Node left;
        if (logo != null && !logo.isError()) {
            ImageView view = new ImageView(logo);
            view.setPreserveRatio(true);
            view.setFitWidth(150);
            view.setSmooth(true);
            left = view;
        } else {
            left = label(seller.name(), 22, true, INK);
        }

        VBox company = new VBox(2,
                right(label(seller.name(), 13, true, INK)),
                right(label(seller.street(), 11, false, MUTED)),
                right(label(seller.postalAndCity(), 11, false, MUTED)),
                right(label(seller.phone() + "  ·  " + seller.email(), 11, false, MUTED)),
                right(label(I18n.get("invoice.org_number") + " " + seller.orgNumber()
                        + "  ·  " + I18n.get("invoice.vat_number") + " " + seller.vatNumber(), 11, false, MUTED)));

        HBox head = new HBox(16, left, growing(), company);
        head.setAlignment(Pos.TOP_LEFT);
        head.setPadding(new Insets(0, 0, 20, 0));
        return head;
    }

    private static Node title(GarageSystem garage, Invoice invoice, Seller seller) {
        VBox titleBox = new VBox(4,
                label(I18n.get(invoice.isCredit() ? "invoice.credit_title" : "invoice.title"), 20, true, INK));
        if (!invoice.isCredit()) {
            titleBox.getChildren().add(label(I18n.get("invoice.thanks_line"), 11, false, MUTED));
        }

        GridPane facts = new GridPane();
        facts.setHgap(14);
        facts.setVgap(3);
        int row = 0;
        facts.add(right(label(I18n.get("invoice.number"), 11, false, MUTED)), 0, row);
        facts.add(right(label(invoice.getNumber(), 11, true, INK)), 1, row++);
        facts.add(right(label(I18n.get("invoice.date"), 11, false, MUTED)), 0, row);
        facts.add(right(label(String.valueOf(invoice.getInvoiceDate()), 11, false, INK)), 1, row++);
        // A credit invoice has no due date of its own: it is a claim the customer pays nothing for.
        if (!invoice.isCredit()) {
            facts.add(right(label(I18n.get("invoice.due_date"), 11, false, MUTED)), 0, row);
            facts.add(right(label(String.valueOf(dueDate(invoice, seller.paymentTermsDays())), 11, false, INK)),
                    1, row++);
        }
        // A credit invoice has no due date and nothing to pay, so its status is not the invoice's.
        if (!invoice.isCredit()) {
            facts.add(right(label(I18n.get("invoice.status"), 11, false, MUTED)), 0, row);
            facts.add(right(label(I18n.get(invoice.isPaid() ? "status.paid" : "status.unpaid"), 11, true,
                    invoice.isPaid() ? "-fx-text-fill: #047857;" : "-fx-text-fill: #b91c1c;")), 1, row);
        }

        VBox box = new VBox(6, titleBox);
        if (invoice.isCredit()) {
            box.getChildren().add(label(I18n.get("invoice.credits") + " " + originalNumber(garage, invoice)
                    + (invoice.getCreditReason().isEmpty() ? ""
                    : "  ·  " + I18n.get("invoice.credit_reason") + ": " + invoice.getCreditReason()), 11, true, INK));
        }

        HBox line = new HBox(20, box, growing(), facts);
        line.setPadding(new Insets(18, 0, 18, 0));
        return line;
    }

    private static Node parties(GarageSystem garage, Invoice invoice) {
        Customer customer = customerFor(garage, invoice);
        Vehicle vehicle = vehicleFor(garage, invoice);
        Booking booking = bookingFor(garage, invoice);

        VBox customerBox = block(I18n.get("invoice.customer"),
                customer == null ? "-" : customer.getName(),
                customer == null ? "" : customer.getAddress(),
                customer == null ? "" : customer.getPostalAndCity(),
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
                booking == null ? "" : I18n.get("invoice.performed") + ": " + booking.getDate(),
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

    private static Node lines(Invoice invoice) {
        GridPane grid = new GridPane();
        grid.setHgap(0);
        grid.setVgap(6);
        grid.getColumnConstraints().addAll(
                column(Priority.ALWAYS, HPos.LEFT),
                column(45, HPos.RIGHT),
                column(70, HPos.RIGHT),
                column(70, HPos.RIGHT),
                column(80, HPos.RIGHT));

        addRow(grid, 0,
                label(I18n.get("invoice.col.description"), 10, true, MUTED),
                right(label(I18n.get("invoice.col.quantity"), 10, true, MUTED)),
                right(label(I18n.get("invoice.col.price"), 10, true, MUTED)),
                right(label(I18n.get("invoice.col.discount"), 10, true, MUTED)),
                right(label(I18n.get("invoice.col.sum"), 10, true, MUTED)));

        int row = 1;
        java.util.LinkedHashMap<String, java.util.List<InvoiceLine>> groups =
                com.wac.autocore.ui.components.UiComponents.groupLinesByPackage(invoice);
        // The heading is only worth showing when at least one line came from a package.
        boolean anyPackage = false;
        for (String key : groups.keySet()) {
            if (!key.isEmpty()) {
                anyPackage = true;
            }
        }
        for (java.util.Map.Entry<String, java.util.List<InvoiceLine>> group : groups.entrySet()) {
            if (anyPackage) {
                String heading = group.getKey().isEmpty() ? I18n.get("invoice.other_services") : group.getKey();
                addRow(grid, row++, label(heading, 11, true, INK));
            }
            for (InvoiceLine line : group.getValue()) {
                // Every line is one performed job, at one price. The quantity is what the invoice
                // must state and it is one piece of work, not a number the workshop has to fill in.
                addRow(grid, row++,
                        label(com.wac.autocore.seed.SeedText.resolve(line.getServiceName()), 11, false, INK),
                        right(label(I18n.get("invoice.quantity_piece", 1), 11, false, INK)),
                        right(label(UiFormatters.formatMoney(line.getPrice()), 11, false, INK)),
                        right(label(UiFormatters.formatMoney(line.getDiscount()), 11, false, MUTED)),
                        right(label(UiFormatters.formatMoney(line.getFinalPrice()), 11, true, INK)));
            }
        }
        return grid;
    }

    private static Node totals(Invoice invoice) {
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(4);
        grid.getColumnConstraints().addAll(column(Priority.ALWAYS, HPos.RIGHT), column(110, HPos.RIGHT));

        int row = 0;
        grid.add(right(label(I18n.get("invoice.subtotal"), 11, false, MUTED)), 0, row);
        grid.add(right(label(UiFormatters.formatMoney(invoice.getAmount()), 11, false, INK)), 1, row++);
        if (invoice.getDiscount() != 0) {
            grid.add(right(label(I18n.get("invoice.discount_total"), 11, false, MUTED)), 0, row);
            // The row shows the deduction with its own sign: a credit invoice gives the discount back.
            grid.add(right(label(UiFormatters.formatMoney(-invoice.getDiscount()), 11, false, INK)), 1, row++);
        }
        // The base the VAT is counted on, stated on its own: the invoice must show it.
        grid.add(right(label(I18n.get("invoice.vat_base"), 11, false, MUTED)), 0, row);
        grid.add(right(label(UiFormatters.formatMoney(invoice.getTotalAmount()), 11, false, INK)), 1, row++);
        grid.add(right(label(I18n.get("invoice.vat"), 11, false, MUTED)), 0, row);
        grid.add(right(label(UiFormatters.formatMoney(invoice.getVatAmount()), 11, false, INK)), 1, row++);

        // To pay is the amount the customer owes, that is with VAT added.
        Label totalLabel = label(I18n.get("invoice.total"), 13, true, INK);
        Label totalValue = label(UiFormatters.formatMoney(invoice.getTotalIncludingVat()), 13, true, INK);
        grid.add(right(totalLabel), 0, row);
        grid.add(right(totalValue), 1, row);

        VBox box = new VBox(8, rule(), grid);
        box.setPadding(new Insets(4, 0, 0, 0));
        return box;
    }

    private static Node footer(Seller seller) {
        VBox footer = new VBox(3,
                label(I18n.get("invoice.footer"), 10, false, MUTED),
                label(I18n.get("invoice.footer_terms", seller.paymentTermsDays()), 10, false, MUTED),
                label(seller.name() + " · " + I18n.get("invoice.org_number") + " " + seller.orgNumber()
                        + " · " + I18n.get("invoice.vat_number") + " " + seller.vatNumber(), 10, false, MUTED),
                label(seller.phone() + "  ·  " + seller.email(), 10, false, MUTED));
        footer.setPadding(new Insets(12, 0, 0, 0));
        return footer;
    }

    static LocalDate dueDate(Invoice invoice, int paymentTermsDays) {
        LocalDate date = invoice.getInvoiceDate() == null ? LocalDate.now() : invoice.getInvoiceDate();
        return date.plusDays(paymentTermsDays);
    }

    /** The number of the invoice this one takes back, or its id when the row has no number. */
    private static String originalNumber(GarageSystem garage, Invoice invoice) {
        for (Invoice row : garage.getInvoices()) {
            if (row.getId() == invoice.getCreditOf() && !row.getNumber().isEmpty()) {
                return row.getNumber();
            }
        }
        return String.valueOf(invoice.getCreditOf());
    }

    private static Customer customerFor(GarageSystem garage, Invoice invoice) {
        Vehicle vehicle = vehicleFor(garage, invoice);
        return vehicle == null ? null : garage.findCustomer(vehicle.getCustomerId());
    }

    private static Booking bookingFor(GarageSystem garage, Invoice invoice) {
        return garage.findBooking(bookingIdFor(garage, invoice));
    }

    private static Vehicle vehicleFor(GarageSystem garage, Invoice invoice) {
        Booking booking = bookingFor(garage, invoice);
        return booking == null ? null : garage.findVehicle(booking.getVehicleId());
    }

    private static int bookingIdFor(GarageSystem garage, Invoice invoice) {
        for (WorkOrder order : garage.getWorkOrders()) {
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

    /** Empty space between the sections, so the invoice breathes. */
    private static final class Spacer extends Region {
        private Spacer(double height) {
            setMinHeight(height);
            setPrefHeight(height);
        }
    }
}
