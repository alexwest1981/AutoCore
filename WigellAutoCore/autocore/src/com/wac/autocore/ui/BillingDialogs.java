package com.wac.autocore.ui;

import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.EntityLookup;
import com.wac.autocore.ui.util.UiFormatters;
import com.wac.autocore.seed.SeedText;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Modala dialoger för fakturering och betalningar (skapa faktura, genomföra betalning).
 */
public final class BillingDialogs {

    private BillingDialogs() {}

    public static void showCreateInvoiceDialog(GarageSystem garage, Runnable onSuccess) {
        showCreateInvoiceDialog(garage, null, onSuccess);
    }

    public static void showCreateInvoiceDialog(GarageSystem garage, WorkOrder preselected, Runnable onSuccess) {
        // Bokningar med utfört arbete kvar att fakturera. En bokning kan ha flera arbetsordrar — en
        // per mekaniker när tjänsterna delas upp — men kunden ska ha en faktura med allt som är gjort.
        List<Booking> invoiceable = garage.getInvoiceableBookings();

        if (invoiceable.isEmpty()) {
            // Är det arbete kvar på bokningarna är det därför ingen faktura kan skapas, och då
            // säger vi det i stället för att bara visa att listan är tom.
            String message = garage.hasBookingWithUnfinishedWork()
                    ? I18n.get("dialog.invoice.not_all_completed")
                    : I18n.get("overview.empty.workorders");
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), message);
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.invoice.create.title"));
        dialog.setHeaderText(I18n.get("dialog.invoice.create.header"));
        ActionDialogs.styleDialog(dialog);

        GridPane grid = ActionDialogs.createGrid();
        grid.setPrefWidth(600);
        javafx.scene.layout.ColumnConstraints col0 = new javafx.scene.layout.ColumnConstraints();
        col0.setMinWidth(140);
        col0.setPrefWidth(150);
        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);

        ComboBox<Booking> bookingBox = new ComboBox<Booking>();
        bookingBox.getItems().addAll(invoiceable);
        bookingBox.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(bookingBox, Priority.ALWAYS);
        if (preselected != null) {
            for (Booking b : invoiceable) {
                if (b.getId() == preselected.getBookingId()) {
                    bookingBox.getSelectionModel().select(b);
                    break;
                }
            }
        }
        if (bookingBox.getSelectionModel().getSelectedItem() == null) {
            bookingBox.getSelectionModel().selectFirst();
        }
        bookingBox.setConverter(new StringConverter<Booking>() {
            @Override
            public String toString(Booking b) {
                return b == null ? "" : I18n.get("table.col.booking") + " #" + b.getId() + " - "
                        + EntityLookup.bookingVehicleReg(garage, b.getId())
                        + " (" + b.getDate() + ")";
            }
            @Override
            public Booking fromString(String string) { return null; }
        });

        TextField discountField = new TextField();
        discountField.setPromptText(I18n.get("dialog.invoice.discount_prompt"));
        discountField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(discountField, Priority.ALWAYS);

        grid.add(new Label(I18n.get("dialog.invoice.booking_select") + ":"), 0, 0);
        grid.add(bookingBox, 1, 0);
        grid.add(new Label(I18n.get("dialog.invoice.discount") + ":"), 0, 1);
        grid.add(discountField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Booking booking = bookingBox.getValue();
                if (booking == null) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }
                String code = discountField.getText().trim();
                Invoice invoice = garage.createInvoiceForBooking(booking.getId(), code);
                if (invoice == null) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.invoice.create_failed"));
                    return;
                }
                showInvoiceLinesDialog(invoice);
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showProcessPaymentDialog(GarageSystem garage, Invoice preselected, Runnable onSuccess) {
        List<Invoice> unpaid = new ArrayList<Invoice>();
        for (Invoice inv : garage.getInvoices()) {
            if (!inv.isPaid()) {
                unpaid.add(inv);
            }
        }

        if (unpaid.isEmpty()) {
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.payment.no_unpaid_invoices"));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.payment.create.title"));
        dialog.setHeaderText(I18n.get("dialog.payment.create.header"));
        ActionDialogs.styleDialog(dialog);

        GridPane grid = ActionDialogs.createGrid();
        grid.setPrefWidth(600);
        javafx.scene.layout.ColumnConstraints col0 = new javafx.scene.layout.ColumnConstraints();
        col0.setMinWidth(140);
        col0.setPrefWidth(150);
        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);

        ComboBox<Invoice> invoiceBox = new ComboBox<Invoice>();
        invoiceBox.getItems().addAll(unpaid);
        invoiceBox.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(invoiceBox, Priority.ALWAYS);
        if (preselected != null && unpaid.contains(preselected)) {
            invoiceBox.getSelectionModel().select(preselected);
        } else {
            invoiceBox.getSelectionModel().selectFirst();
        }
        invoiceBox.setConverter(new StringConverter<Invoice>() {
            @Override
            public String toString(Invoice inv) {
                return inv == null ? "" : I18n.get("table.col.invoice") + " #" + inv.getId() + " - " + UiFormatters.formatMoney(inv.getTotalAmount()) + " (" + I18n.get("table.col.workorder") + " #" + inv.getWorkOrderId() + ")";
            }
            @Override
            public Invoice fromString(String string) { return null; }
        });

        ComboBox<String> typeBox = new ComboBox<String>();
        typeBox.getItems().addAll("SWISH", "CARD", "CASH");
        typeBox.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(typeBox, Priority.ALWAYS);
        typeBox.getSelectionModel().select("SWISH");
        // Det sparade värdet står kvar som SWISH i databasen, men listan visar "Swish".
        typeBox.setConverter(new StringConverter<String>() {
            @Override
            public String toString(String stored) {
                return UiFormatters.paymentTypeWord(stored);
            }
            @Override
            public String fromString(String text) {
                return null;
            }
        });

        grid.add(new Label(I18n.get("dialog.payment.invoice_select") + ":"), 0, 0);
        grid.add(invoiceBox, 1, 0);
        grid.add(new Label(I18n.get("dialog.payment.method") + ":"), 0, 1);
        grid.add(typeBox, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Invoice inv = invoiceBox.getValue();
                String paymentType = typeBox.getValue();

                garage.processPayment(inv.getId(), paymentType);
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    static void showInvoiceLinesDialog(Invoice invoice) {
        if (invoice == null) return;
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("table.col.invoice") + " #" + invoice.getId());
        dialog.setHeaderText(I18n.get("table.col.invoice") + " #" + invoice.getId()
                + " (" + I18n.get("table.col.workorder") + " #" + invoice.getWorkOrderId()
                + ", " + invoice.getInvoiceDate() + ")");
        ActionDialogs.styleDialog(dialog);

        VBox content = new VBox(14);
        content.setPadding(new Insets(18, 22, 18, 22));
        content.setPrefWidth(600);

        Label notice = new Label(I18n.get("dialog.workorder.historical_notice"));
        notice.getStyleClass().addAll("srow-sub", "small");

        GridPane grid = ActionDialogs.createGrid();
        Label h1 = new Label(I18n.get("table.col.service"));
        h1.setStyle("-fx-font-weight: bold;");
        Label h2 = new Label(I18n.get("table.col.price"));
        h2.setStyle("-fx-font-weight: bold;");
        Label h3 = new Label(I18n.get("table.col.discount"));
        h3.setStyle("-fx-font-weight: bold;");
        Label h4 = new Label(I18n.get("table.col.final_price"));
        h4.setStyle("-fx-font-weight: bold;");

        grid.add(h1, 0, 0);
        grid.add(h2, 1, 0);
        grid.add(h3, 2, 0);
        grid.add(h4, 3, 0);

        int row = 1;
        if (invoice.getLines() != null) {
            for (InvoiceLine line : invoice.getLines()) {
                grid.add(new Label(SeedText.resolve(line.getServiceName())), 0, row);
                grid.add(new Label(UiFormatters.formatMoney(line.getPrice())), 1, row);
                grid.add(new Label(UiFormatters.formatMoney(line.getDiscount())), 2, row);
                grid.add(new Label(UiFormatters.formatMoney(line.getFinalPrice())), 3, row);
                row++;
            }
        }

        // Summary row
        Label totalLabel = new Label(I18n.get("table.col.total") + ":");
        totalLabel.setStyle("-fx-font-weight: bold;");
        Label totalVal = new Label(UiFormatters.formatMoney(invoice.getTotalAmount()));
        totalVal.setStyle("-fx-font-weight: bold;");
        grid.add(totalLabel, 2, row);
        grid.add(totalVal, 3, row);

        content.getChildren().addAll(notice, grid);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
    }

    /**
     * Förhandsgranskar fakturan som den skrivs ut, och skickar den till skrivaren.
     *
     * Rutan stängs inte när man skriver ut, så samma faktura kan skrivas ut igen eller rättas först.
     */
    static void showInvoiceDocumentDialog(GarageSystem garage, Invoice invoice) {
        if (invoice == null) return;

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.invoice.print_title") + " #" + invoice.getId());
        dialog.setHeaderText(I18n.get("dialog.invoice.print_header"));
        ActionDialogs.styleDialog(dialog);
        dialog.setResizable(true);

        final javafx.scene.Node document = InvoiceDocument.build(garage, invoice);
        javafx.scene.layout.StackPane paper = new javafx.scene.layout.StackPane(document);
        paper.setStyle("-fx-background-color: #e5e7eb; -fx-padding: 18;");
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(paper);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportWidth(document.prefWidth(-1) + 60);
        scroll.setPrefViewportHeight(640);

        ButtonType printType = new ButtonType(I18n.get("dialog.invoice.print"),
                javafx.scene.control.ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().addAll(printType, ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(printType).addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            evt.consume();
            sendToPrinter(document, dialog);
        });
        dialog.showAndWait();
    }

    /** Skickar dokumentet till skrivaren. Utan skrivare blir det ett besked i stället för tystnad. */
    private static void sendToPrinter(final javafx.scene.Node document, Dialog<?> dialog) {
        javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();
        if (job == null) {
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.invoice.no_printer"));
            return;
        }

        try {
            javafx.print.PageLayout layout = job.getPrinter().createPageLayout(
                    javafx.print.Paper.A4, javafx.print.PageOrientation.PORTRAIT,
                    javafx.print.Printer.MarginType.DEFAULT);
            job.getJobSettings().setPageLayout(layout);
        } catch (Exception ignored) {
            // Skrivaren utan A4: skriv ut på den layout den erbjuder i stället.
        }

        javafx.stage.Window owner = dialog.getDialogPane().getScene() == null
                ? null : dialog.getDialogPane().getScene().getWindow();
        if (!job.showPrintDialog(owner)) {
            job.endJob();
            return;
        }

        boolean printed = job.printPage(document);
        job.endJob();
        if (!printed) {
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.invoice.print_failed"));
        }
    }
}