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

/** Dialoger för fakturering och betalningar. */
public final class BillingDialogs {

    private BillingDialogs() {}

    public static void showCreateInvoiceDialog(GarageSystem garage, Runnable onSuccess) {
        showCreateInvoiceDialog(garage, null, onSuccess);
    }

    public static void showCreateInvoiceDialog(GarageSystem garage, WorkOrder preselected, Runnable onSuccess) {
        // Bokningar med utfört arbete kvar att fakturera. En bokning kan ha flera arbetsordrar — en
/** Kunden ska ha en faktura med allt som är gjort. */
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

        // En reklamation kan föra med sig en ny kostnad som kunden ska betala. Fälten visas bara
        // när bokningen innehåller ett garantiarbete, annars står de bara i vägen.
        Label extraNameLabel = new Label(I18n.get("dialog.invoice.extra_cost_label") + ":");
        TextField extraNameField = new TextField();
        extraNameField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(extraNameField, Priority.ALWAYS);
        Label extraAmountLabel = new Label(I18n.get("dialog.invoice.extra_cost_amount") + ":");
        TextField extraAmountField = new TextField();
        extraAmountField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(extraAmountField, Priority.ALWAYS);

        grid.add(new Label(I18n.get("dialog.invoice.booking_select") + ":"), 0, 0);
        grid.add(bookingBox, 1, 0);
        grid.add(new Label(I18n.get("dialog.invoice.discount") + ":"), 0, 1);
        grid.add(discountField, 1, 1);
        grid.add(extraNameLabel, 0, 2);
        grid.add(extraNameField, 1, 2);
        grid.add(extraAmountLabel, 0, 3);
        grid.add(extraAmountField, 1, 3);

        Runnable updateExtraFields = () -> {
            boolean show = hasWarrantyOrder(garage, bookingBox.getValue());
            extraNameLabel.setVisible(show);
            extraNameLabel.setManaged(show);
            extraNameField.setVisible(show);
            extraNameField.setManaged(show);
            extraAmountLabel.setVisible(show);
            extraAmountLabel.setManaged(show);
            extraAmountField.setVisible(show);
            extraAmountField.setManaged(show);
        };
        bookingBox.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, now) -> updateExtraFields.run());
        updateExtraFields.run();

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
                Double extraAmount = null;
                if (extraNameField.isVisible() && !extraAmountField.getText().trim().isEmpty()) {
                    // Samma tolkning av belopp som tjänstedialogen gör, så att 250,50 går att skriva
                    // på båda ställena.
                    extraAmount = ServiceItemDialogs.parsePrice(extraAmountField.getText());
                    if (extraAmount == null) {
                        ActionDialogs.showError(I18n.get("dialog.confirm.title"),
                                I18n.get("dialog.invoice.extra_cost_invalid"));
                        return;
                    }
                }
                Invoice invoice = garage.createInvoiceForBooking(booking.getId(), code,
                        extraNameField.getText().trim(),
                        extraAmount == null ? 0.0 : extraAmount.doubleValue());
                if (invoice == null) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.invoice.create_failed"));
                    return;
                }
                showInvoiceLinesDialog(invoice);
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    // Kostnader utöver arbetet hör till en reklamation, så utan ett garantiarbete finns inget att
    // fylla i. Letar i bokningens arbetsordrar, inte bara den förvalda.
    private static boolean hasWarrantyOrder(GarageSystem garage, Booking booking) {
        if (booking == null) {
            return false;
        }
        for (WorkOrder order : garage.getWorkOrders()) {
            if (order.getBookingId() == booking.getId() && order.isWarranty()) {
                return true;
            }
        }
        return false;
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
        content.setPrefWidth(620);

        Label notice = new Label(I18n.get("dialog.workorder.historical_notice"));
        notice.getStyleClass().addAll("srow-sub", "small");

        GridPane grid = ActionDialogs.createGrid();
        // Den här dialogen är en tabell med fyra kolumner, så den byter ut de två kolumnreglerna
        // mot fyra egna där alla får växa jämnt.
        grid.getColumnConstraints().clear();
        for (int i = 0; i < 4; i++) {
            javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
            column.setMinWidth(110);
            column.setPrefWidth(150);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }
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

/** Förhandsgranskar fakturan och skickar den till skrivaren. Rutan stängs inte. */
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

/** Skickar dokumentet till skrivaren. Utan skrivare blir det ett besked. */
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