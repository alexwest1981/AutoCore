package com.wac.autocore.ui;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Modal form dialogs for performing all system business actions directly in the JavaFX GUI with i18n support.
 */
public final class ActionDialogs {

    private ActionDialogs() {}

    private static void styleDialog(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        if (!pane.getStyleClass().contains("root")) {
            pane.getStyleClass().add("root");
        }
        dialog.setOnShowing(evt -> {
            javafx.scene.Scene appScene = com.wac.autocore.theme.ThemeManager.getCurrentScene();
            if (appScene != null) {
                javafx.scene.Scene dScene = pane.getScene();
                if (dScene != null) {
                    dScene.getStylesheets().setAll(appScene.getStylesheets());
                    if (dScene.getRoot() != null && !dScene.getRoot().getStyleClass().contains("root")) {
                        dScene.getRoot().getStyleClass().add("root");
                    }
                }
            }
        });
    }

    // ----------------------------------------------------- 1. Customer
    public static void showCreateCustomerDialog(GarageSystem garage, Runnable onSuccess) {
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.customer.create.title"));
        dialog.setHeaderText(I18n.get("dialog.customer.create.header"));
        styleDialog(dialog);

        GridPane grid = createGrid();

        TextField nameField = new TextField();
        nameField.setPromptText("First and last name");
        TextField phoneField = new TextField();
        phoneField.setPromptText("555-1234567");
        TextField emailField = new TextField();
        emailField.setPromptText("name@example.com");

        grid.add(new Label(I18n.get("dialog.customer.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("dialog.customer.phone") + ":"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label(I18n.get("dialog.customer.email") + ":"), 0, 2);
        grid.add(emailField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String name = nameField.getText().trim();
                String phone = phoneField.getText().trim();
                String email = emailField.getText().trim();

                if (name.isEmpty() || phone.isEmpty()) {
                    showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }

                garage.createCustomer(name, phone, email);
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    // --------------------------------------------------------- 2. Vehicle
    public static void showCreateVehicleDialog(GarageSystem garage, Runnable onSuccess) {
        List<Customer> customers = garage.getCustomers();
        if (customers.isEmpty()) {
            showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.vehicle.create.title"));
        dialog.setHeaderText(I18n.get("dialog.vehicle.create.header"));
        styleDialog(dialog);

        GridPane grid = createGrid();

        ComboBox<Customer> customerBox = new ComboBox<Customer>();
        customerBox.getItems().addAll(customers);
        customerBox.getSelectionModel().selectFirst();
        customerBox.setConverter(new StringConverter<Customer>() {
            @Override
            public String toString(Customer c) {
                return c == null ? "" : c.getId() + " - " + c.getName() + " (" + c.getPhone() + ")";
            }
            @Override
            public Customer fromString(String string) { return null; }
        });

        TextField regField = new TextField();
        regField.setPromptText("ABC123");
        TextField brandField = new TextField();
        brandField.setPromptText("Volvo");
        TextField modelField = new TextField();
        modelField.setPromptText("V60");
        TextField yearField = new TextField();
        yearField.setPromptText("2022");

        grid.add(new Label(I18n.get("dialog.vehicle.customer_select") + ":"), 0, 0);
        grid.add(customerBox, 1, 0);
        grid.add(new Label(I18n.get("dialog.vehicle.reg_nr") + ":"), 0, 1);
        grid.add(regField, 1, 1);
        grid.add(new Label(I18n.get("dialog.vehicle.brand") + ":"), 0, 2);
        grid.add(brandField, 1, 2);
        grid.add(new Label(I18n.get("dialog.vehicle.model") + ":"), 0, 3);
        grid.add(modelField, 1, 3);
        grid.add(new Label(I18n.get("dialog.vehicle.year") + ":"), 0, 4);
        grid.add(yearField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Customer owner = customerBox.getValue();
                String reg = regField.getText().trim().toUpperCase();
                String brand = brandField.getText().trim();
                String model = modelField.getText().trim();
                int year;
                try {
                    year = Integer.parseInt(yearField.getText().trim());
                } catch (NumberFormatException ex) {
                    showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.invalid_number"));
                    return;
                }

                if (reg.isEmpty() || brand.isEmpty()) {
                    showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }

                garage.createVehicle(reg, brand, model, year, owner.getId());
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    // ---------------------------------------------------------- 3. Booking
    public static void showCreateBookingDialog(GarageSystem garage, Runnable onSuccess) {
        List<Vehicle> vehicles = garage.getVehicles();
        if (vehicles.isEmpty()) {
            showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.booking.create.title"));
        dialog.setHeaderText(I18n.get("dialog.booking.create.header"));
        styleDialog(dialog);

        GridPane grid = createGrid();

        ComboBox<Vehicle> vehicleBox = new ComboBox<Vehicle>();
        vehicleBox.getItems().addAll(vehicles);
        vehicleBox.getSelectionModel().selectFirst();
        vehicleBox.setConverter(new StringConverter<Vehicle>() {
            @Override
            public String toString(Vehicle v) {
                return v == null ? "" : v.getId() + " - " + v.getRegistrationNumber() + " (" + v.getBrand() + " " + v.getModel() + ")";
            }
            @Override
            public Vehicle fromString(String string) { return null; }
        });

        DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(1));
        TextField descField = new TextField();
        descField.setPromptText("E.g. Annual service and brake replacement");

        grid.add(new Label(I18n.get("dialog.booking.vehicle_select") + ":"), 0, 0);
        grid.add(vehicleBox, 1, 0);
        grid.add(new Label(I18n.get("dialog.booking.date") + ":"), 0, 1);
        grid.add(datePicker, 1, 1);
        grid.add(new Label(I18n.get("table.col.description") + ":"), 0, 2);
        grid.add(descField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Vehicle v = vehicleBox.getValue();
                LocalDate date = datePicker.getValue();
                String desc = descField.getText().trim();

                if (date == null || desc.isEmpty()) {
                    showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }

                garage.createBooking(v.getId(), date, desc);
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    // --------------------------------------------------- 4. Work order
    public static void showCreateWorkOrderDialog(GarageSystem garage, Runnable onSuccess) {
        List<Booking> bookings = new ArrayList<Booking>();
        for (Booking b : garage.getBookings()) {
            if ("BOOKED".equalsIgnoreCase(b.getStatus())) {
                bookings.add(b);
            }
        }

        if (bookings.isEmpty()) {
            showError(I18n.get("dialog.confirm.title"), I18n.get("overview.empty.bookings"));
            return;
        }

        List<Mechanic> mechanics = garage.getMechanics();
        if (mechanics.isEmpty()) {
            showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.workorder.create.title"));
        dialog.setHeaderText(I18n.get("dialog.workorder.create.header"));
        styleDialog(dialog);

        VBox content = new VBox(12);
        content.setPadding(new Insets(10));

        GridPane grid = createGrid();

        ComboBox<Booking> bookingBox = new ComboBox<Booking>();
        bookingBox.getItems().addAll(bookings);
        bookingBox.getSelectionModel().selectFirst();
        bookingBox.setConverter(new StringConverter<Booking>() {
            @Override
            public String toString(Booking b) {
                return b == null ? "" : "Booking #" + b.getId() + " - " + b.getDescription() + " (" + b.getDate() + ")";
            }
            @Override
            public Booking fromString(String string) { return null; }
        });

        ComboBox<Mechanic> mechanicBox = new ComboBox<Mechanic>();
        mechanicBox.getItems().addAll(mechanics);
        mechanicBox.getSelectionModel().selectFirst();
        mechanicBox.setConverter(new StringConverter<Mechanic>() {
            @Override
            public String toString(Mechanic m) {
                return m == null ? "" : m.getName() + " (" + m.getSpecialization() + ") - " + (m.isAvailable() ? I18n.get("table.col.available") : "Busy");
            }
            @Override
            public Mechanic fromString(String string) { return null; }
        });

        grid.add(new Label(I18n.get("dialog.workorder.booking_select") + ":"), 0, 0);
        grid.add(bookingBox, 1, 0);
        grid.add(new Label(I18n.get("dialog.workorder.mechanic_select") + ":"), 0, 1);
        grid.add(mechanicBox, 1, 1);

        Label servicesTitle = new Label(I18n.get("dialog.workorder.services_select"));
        servicesTitle.setStyle("-fx-font-weight: bold;");

        VBox serviceChecks = new VBox(6);
        List<CheckBox> checkList = new ArrayList<CheckBox>();
        for (ServiceItem s : garage.getServiceItems()) {
            CheckBox cb = new CheckBox(s.getName() + " (" + s.getPrice() + " " + I18n.get("common.currency") + ", " + s.getEstimatedMinutes() + " min)");
            cb.setUserData(s.getId());
            checkList.add(cb);
            serviceChecks.getChildren().add(cb);
        }
        if (!checkList.isEmpty()) {
            checkList.get(0).setSelected(true);
        }

        ScrollPane scroll = new ScrollPane(serviceChecks);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(130);

        content.getChildren().addAll(grid, servicesTitle, scroll);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Booking b = bookingBox.getValue();
                Mechanic m = mechanicBox.getValue();

                List<Integer> selectedServiceIds = new ArrayList<Integer>();
                for (CheckBox cb : checkList) {
                    if (cb.isSelected()) {
                        selectedServiceIds.add((Integer) cb.getUserData());
                    }
                }

                if (selectedServiceIds.isEmpty()) {
                    showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }

                int[] ids = new int[selectedServiceIds.size()];
                for (int i = 0; i < ids.length; i++) ids[i] = selectedServiceIds.get(i);

                garage.createWorkOrder(b.getId(), m.getId(), ids);
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    // ------------------------------------------------------- 5. Invoice
    public static void showCreateInvoiceDialog(GarageSystem garage, Runnable onSuccess) {
        List<WorkOrder> completedOrders = new ArrayList<WorkOrder>();
        for (WorkOrder wo : garage.getWorkOrders()) {
            if ("COMPLETED".equalsIgnoreCase(wo.getStatus())) {
                boolean alreadyInvoiced = false;
                for (Invoice inv : garage.getInvoices()) {
                    if (inv.getWorkOrderId() == wo.getId()) {
                        alreadyInvoiced = true;
                        break;
                    }
                }
                if (!alreadyInvoiced) {
                    completedOrders.add(wo);
                }
            }
        }

        if (completedOrders.isEmpty()) {
            showError(I18n.get("dialog.confirm.title"), I18n.get("overview.empty.workorders"));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.invoice.create.title"));
        dialog.setHeaderText(I18n.get("dialog.invoice.create.header"));
        styleDialog(dialog);

        GridPane grid = createGrid();

        ComboBox<WorkOrder> orderBox = new ComboBox<WorkOrder>();
        orderBox.getItems().addAll(completedOrders);
        orderBox.getSelectionModel().selectFirst();
        orderBox.setConverter(new StringConverter<WorkOrder>() {
            @Override
            public String toString(WorkOrder wo) {
                return wo == null ? "" : "Work order #" + wo.getId() + " (Booking #" + wo.getBookingId() + ")";
            }
            @Override
            public WorkOrder fromString(String string) { return null; }
        });

        TextField discountField = new TextField();
        discountField.setPromptText("E.g. WELCOME10 or SERVICE200 (optional)");

        grid.add(new Label(I18n.get("dialog.invoice.workorder_select") + ":"), 0, 0);
        grid.add(orderBox, 1, 0);
        grid.add(new Label(I18n.get("dialog.invoice.discount") + ":"), 0, 1);
        grid.add(discountField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                WorkOrder wo = orderBox.getValue();
                String code = discountField.getText().trim();
                garage.createInvoice(wo.getId(), code);
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    // --------------------------------------------------------- 6. Payment
    public static void showProcessPaymentDialog(GarageSystem garage, Invoice preselected, Runnable onSuccess) {
        List<Invoice> unpaid = new ArrayList<Invoice>();
        for (Invoice inv : garage.getInvoices()) {
            if (!inv.isPaid()) {
                unpaid.add(inv);
            }
        }

        if (unpaid.isEmpty()) {
            showError(I18n.get("dialog.confirm.title"), I18n.get("common.close"));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.payment.create.title"));
        dialog.setHeaderText(I18n.get("dialog.payment.create.header"));
        styleDialog(dialog);

        GridPane grid = createGrid();

        ComboBox<Invoice> invoiceBox = new ComboBox<Invoice>();
        invoiceBox.getItems().addAll(unpaid);
        if (preselected != null && unpaid.contains(preselected)) {
            invoiceBox.getSelectionModel().select(preselected);
        } else {
            invoiceBox.getSelectionModel().selectFirst();
        }
        invoiceBox.setConverter(new StringConverter<Invoice>() {
            @Override
            public String toString(Invoice inv) {
                return inv == null ? "" : "Invoice #" + inv.getId() + " - " + inv.getTotalAmount() + " " + I18n.get("common.currency") + " (Order #" + inv.getWorkOrderId() + ")";
            }
            @Override
            public Invoice fromString(String string) { return null; }
        });

        ComboBox<String> typeBox = new ComboBox<String>();
        typeBox.getItems().addAll("SWISH", "CARD", "CASH");
        typeBox.getSelectionModel().select("SWISH");

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

    // ----------------------------------------------------------- Helpers
    private static GridPane createGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(14, 14, 14, 14));
        return grid;
    }

    private static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.setOnShowing(evt -> {
            javafx.scene.Scene appScene = com.wac.autocore.theme.ThemeManager.getCurrentScene();
            if (appScene != null) {
                javafx.scene.Scene aScene = alert.getDialogPane().getScene();
                if (aScene != null) {
                    aScene.getStylesheets().setAll(appScene.getStylesheets());
                }
            }
        });
        alert.showAndWait();
    }
}
