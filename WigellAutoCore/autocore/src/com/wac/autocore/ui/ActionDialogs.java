package com.wac.autocore.ui;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.EntityLookup;

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

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Modal form dialogs for performing all system business actions directly in the JavaFX GUI with i18n support.
 */
public final class ActionDialogs {

    private ActionDialogs() {}

    private static final MechanicRepository mechanicRepository = new MechanicRepository();

    static void styleDialog(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        if (!pane.getStyleClass().contains("root")) {
            pane.getStyleClass().add("root");
        }
        javafx.scene.Scene appScene = com.wac.autocore.theme.ThemeManager.getCurrentScene();
        if (appScene != null && appScene.getWindow() != null) {
            try {
                dialog.initOwner(appScene.getWindow());
                dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            } catch (Exception ignored) {}
        }
        dialog.setOnShowing(evt -> {
            javafx.scene.Scene currentAppScene = com.wac.autocore.theme.ThemeManager.getCurrentScene();
            if (currentAppScene != null) {
                javafx.scene.Scene dScene = pane.getScene();
                if (dScene != null) {
                    dScene.getStylesheets().setAll(currentAppScene.getStylesheets());
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
        nameField.setPromptText(I18n.get("dialog.customer.name_prompt"));
        TextField phoneField = new TextField();
        phoneField.setPromptText(I18n.get("dialog.customer.phone_prompt"));
        TextField emailField = new TextField();
        emailField.setPromptText(I18n.get("dialog.customer.email_prompt"));

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

    public static void showEditCustomerDialog(GarageSystem garage, Customer customer, Runnable onSuccess) {
        if (customer == null) return;

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.customer.edit.title"));
        dialog.setHeaderText(I18n.get("dialog.customer.edit.header"));
        styleDialog(dialog);

        GridPane grid = createGrid();

        TextField nameField = new TextField(customer.getName());
        nameField.setPromptText(I18n.get("dialog.customer.name_prompt"));
        TextField phoneField = new TextField(customer.getPhone() != null ? customer.getPhone() : "");
        phoneField.setPromptText(I18n.get("dialog.customer.phone_prompt"));
        TextField emailField = new TextField(customer.getEmail() != null ? customer.getEmail() : "");
        emailField.setPromptText(I18n.get("dialog.customer.email_prompt"));
        CheckBox vipBox = new CheckBox(I18n.get("table.col.vip"));
        vipBox.setSelected(customer.isVip());

        grid.add(new Label(I18n.get("dialog.customer.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("dialog.customer.phone") + ":"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label(I18n.get("dialog.customer.email") + ":"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label(I18n.get("table.col.vip") + ":"), 0, 3);
        grid.add(vipBox, 1, 3);

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

                customer.setName(name);
                customer.setPhone(phone);
                customer.setEmail(email);
                customer.setVip(vipBox.isSelected());

                try {
                    garage.updateCustomer(customer);
                } catch (SQLException e) {
                    showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }

                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showDeleteCustomerConfirmation(GarageSystem garage, Customer customer, Runnable onSuccess) {
        if (customer == null) return;

        if (!garage.canDeleteCustomer(customer.getId())) {
            showError(I18n.get("dialog.customer.delete.title"),
                    I18n.get("dialog.customer.delete.has_active_orders", customer.getName()));
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18n.get("dialog.customer.delete.title"));
        alert.setHeaderText(I18n.get("dialog.customer.delete.header"));
        alert.setContentText(I18n.get("dialog.customer.delete.confirm", customer.getName()));
        styleDialog(alert);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    garage.deleteCustomer(customer.getId());
                } catch (SQLException e) {
                    showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }
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
        regField.setPromptText(I18n.get("dialog.vehicle.reg_prompt"));
        TextField brandField = new TextField();
        brandField.setPromptText(I18n.get("dialog.vehicle.brand_prompt"));
        TextField modelField = new TextField();
        modelField.setPromptText(I18n.get("dialog.vehicle.model_prompt"));
        TextField yearField = new TextField();
        yearField.setPromptText(I18n.get("dialog.vehicle.year_prompt"));

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

    public static void showEditVehicleDialog(GarageSystem garage, Vehicle vehicle, Runnable onSuccess) {
        if (vehicle == null) return;

        List<Customer> customers = garage.getCustomers();
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.vehicle.edit.title"));
        dialog.setHeaderText(I18n.get("dialog.vehicle.edit.header"));
        styleDialog(dialog);

        GridPane grid = createGrid();

        ComboBox<Customer> customerBox = new ComboBox<Customer>();
        customerBox.getItems().addAll(customers);
        for (Customer c : customers) {
            if (c.getId() == vehicle.getCustomerId()) {
                customerBox.getSelectionModel().select(c);
                break;
            }
        }
        customerBox.setConverter(new StringConverter<Customer>() {
            @Override
            public String toString(Customer c) {
                return c == null ? "" : c.getId() + " - " + c.getName() + " (" + c.getPhone() + ")";
            }
            @Override
            public Customer fromString(String string) { return null; }
        });

        TextField regField = new TextField(vehicle.getRegistrationNumber());
        regField.setPromptText(I18n.get("dialog.vehicle.reg_prompt"));
        TextField brandField = new TextField(vehicle.getBrand() != null ? vehicle.getBrand() : "");
        brandField.setPromptText(I18n.get("dialog.vehicle.brand_prompt"));
        TextField modelField = new TextField(vehicle.getModel() != null ? vehicle.getModel() : "");
        modelField.setPromptText(I18n.get("dialog.vehicle.model_prompt"));
        TextField yearField = new TextField(String.valueOf(vehicle.getYear()));
        yearField.setPromptText(I18n.get("dialog.vehicle.year_prompt"));

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
                Customer cust = customerBox.getValue();
                String reg = regField.getText().trim().toUpperCase();
                String brand = brandField.getText().trim();
                String model = modelField.getText().trim();
                String yearStr = yearField.getText().trim();

                if (cust == null || reg.isEmpty() || brand.isEmpty() || model.isEmpty() || yearStr.isEmpty()) {
                    showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }

                int year;
                try {
                    year = Integer.parseInt(yearStr);
                } catch (NumberFormatException e) {
                    showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.invalid_number"));
                    return;
                }

                vehicle.setCustomerId(cust.getId());
                vehicle.setRegistrationNumber(reg);
                vehicle.setBrand(brand);
                vehicle.setModel(model);
                vehicle.setYear(year);

                try {
                    garage.updateVehicle(vehicle);
                } catch (SQLException e) {
                    showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }

                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showDeleteVehicleConfirmation(GarageSystem garage, Vehicle vehicle, Runnable onSuccess) {
        if (vehicle == null) return;

        if (!garage.canDeleteVehicle(vehicle.getId())) {
            showError(I18n.get("dialog.vehicle.delete.title"),
                    I18n.get("dialog.vehicle.delete.has_active_orders", vehicle.getRegistrationNumber()));
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18n.get("dialog.vehicle.delete.title"));
        alert.setHeaderText(I18n.get("dialog.vehicle.delete.header"));
        alert.setContentText(I18n.get("dialog.vehicle.delete.confirm", vehicle.getRegistrationNumber()));
        styleDialog(alert);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    garage.deleteVehicle(vehicle.getId());
                } catch (SQLException e) {
                    showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    // ---------------------------------------------------------- 3. Booking
    public static void showCreateBookingDialog(GarageSystem garage, Runnable onSuccess) {
        BookingDialogs.showCreateBookingDialog(garage, onSuccess);
    }

    public static void showCreateBookingDialog(GarageSystem garage, LocalDate defaultDate,
                                               Mechanic defaultMechanic, Integer defaultHour, Runnable onSuccess) {
        BookingDialogs.showCreateBookingDialog(garage, defaultDate, defaultMechanic, defaultHour, onSuccess);
    }

    public static void showEditBookingDialog(GarageSystem garage, Booking booking, Runnable onSuccess) {
        BookingDialogs.showEditBookingDialog(garage, booking, onSuccess);
    }

    public static void showCancelBookingConfirmation(GarageSystem garage, Booking booking, Runnable onSuccess) {
        BookingDialogs.showCancelBookingConfirmation(garage, booking, onSuccess);
    }

    public static void showDeleteBookingConfirmation(GarageSystem garage, Booking booking, Runnable onSuccess) {
        BookingDialogs.showDeleteBookingConfirmation(garage, booking, onSuccess);
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
                return b == null ? "" : I18n.get("table.col.booking") + " #" + b.getId() + " - " + b.getDescription() + " (" + b.getDate() + ")";
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
                return m == null ? "" : m.getName() + " (" + m.getSpecialization() + ") - " + (m.isAvailable() ? I18n.get("table.col.available") : I18n.get("table.col.unavailable"));
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
                com.wac.autocore.service.MechanicSchedule.getInstance().syncFromDatabase();
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
                return wo == null ? "" : I18n.get("table.col.workorder") + " #" + wo.getId() + " (" + I18n.get("table.col.booking") + " #" + wo.getBookingId() + ")";
            }
            @Override
            public WorkOrder fromString(String string) { return null; }
        });

        TextField discountField = new TextField();
        discountField.setPromptText(I18n.get("dialog.invoice.discount_prompt"));

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
                return inv == null ? "" : I18n.get("table.col.invoice") + " #" + inv.getId() + " - " + inv.getTotalAmount() + " " + I18n.get("common.currency") + " (" + I18n.get("table.col.workorder") + " #" + inv.getWorkOrderId() + ")";
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

    // -------------------------------------------------------- 7. Mechanic
    public static void showCreateMechanicDialog(GarageSystem garage, Runnable onSuccess) {
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.mechanic.create.title"));
        dialog.setHeaderText(I18n.get("dialog.mechanic.create.header"));
        styleDialog(dialog);

        GridPane grid = createGrid();

        TextField nameField = new TextField();
        nameField.setPromptText(I18n.get("dialog.mechanic.name_prompt"));
        TextField phoneField = new TextField();
        phoneField.setPromptText(I18n.get("dialog.mechanic.phone_prompt"));
        TextField specField = new TextField();
        specField.setPromptText(I18n.get("dialog.mechanic.spec_prompt"));

        grid.add(new Label(I18n.get("dialog.customer.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("dialog.customer.phone") + ":"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label(I18n.get("table.col.specialisation") + ":"), 0, 2);
        grid.add(specField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String name = nameField.getText().trim();
                String phone = phoneField.getText().trim();
                String spec = specField.getText().trim();

                if (name.isEmpty() || phone.isEmpty()) {
                    showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }

                Mechanic mechanic = new Mechanic(0, name, phone, spec.isEmpty() ? I18n.get("dialog.mechanic.default_spec") : spec);

                try {
                    mechanicRepository.save(mechanic);
                } catch (SQLException e) {
                    showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }

                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showEditMechanicDialog(GarageSystem garage, Mechanic mechanic, Runnable onSuccess) {
        if (mechanic == null) return;

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.mechanic.edit.title"));
        dialog.setHeaderText(I18n.get("dialog.mechanic.edit.header"));
        styleDialog(dialog);

        GridPane grid = createGrid();

        TextField nameField = new TextField(mechanic.getName());
        nameField.setPromptText(I18n.get("dialog.mechanic.name_prompt"));
        TextField phoneField = new TextField(mechanic.getPhone() != null ? mechanic.getPhone() : "");
        phoneField.setPromptText(I18n.get("dialog.mechanic.phone_prompt"));
        TextField specField = new TextField(mechanic.getSpecialization() != null ? mechanic.getSpecialization() : "");
        specField.setPromptText(I18n.get("dialog.mechanic.spec_prompt"));
        CheckBox availBox = new CheckBox(I18n.get("dialog.mechanic.available"));
        availBox.setSelected(mechanic.isAvailable());

        grid.add(new Label(I18n.get("dialog.customer.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("dialog.customer.phone") + ":"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label(I18n.get("table.col.specialisation") + ":"), 0, 2);
        grid.add(specField, 1, 2);
        grid.add(new Label(I18n.get("table.col.status") + ":"), 0, 3);
        grid.add(availBox, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String name = nameField.getText().trim();
                String phone = phoneField.getText().trim();
                String spec = specField.getText().trim();

                if (name.isEmpty() || phone.isEmpty()) {
                    showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }

                mechanic.setName(name);
                mechanic.setPhone(phone);
                mechanic.setSpecialization(spec.isEmpty() ? I18n.get("dialog.mechanic.default_spec") : spec);
                mechanic.setAvailable(availBox.isSelected());

                try {
                    garage.updateMechanic(mechanic);
                } catch (SQLException e) {
                    showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }

                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showDeleteMechanicConfirmation(GarageSystem garage, Mechanic mechanic, Runnable onSuccess) {
        if (mechanic == null) return;

        if (!garage.canDeleteMechanic(mechanic.getId())) {
            showError(I18n.get("dialog.mechanic.delete.title"),
                    I18n.get("dialog.mechanic.delete.has_active_orders", mechanic.getName()));
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18n.get("dialog.mechanic.delete.title"));
        alert.setHeaderText(I18n.get("dialog.mechanic.delete.header"));
        alert.setContentText(I18n.get("dialog.mechanic.delete.confirm", mechanic.getName()));
        styleDialog(alert);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    garage.deleteMechanic(mechanic.getId());
                } catch (SQLException e) {
                    showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    // --------------------------------------------------------- 8. ServiceItem
    public static void showCreateServiceItemDialog(GarageSystem garage, Runnable onSuccess) {
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.service.create.title"));
        dialog.setHeaderText(I18n.get("dialog.service.create.header"));
        styleDialog(dialog);

        GridPane grid = createGrid();

        TextField nameField = new TextField();
        nameField.setPromptText(I18n.get("dialog.service.name_prompt"));
        TextField descField = new TextField();
        descField.setPromptText(I18n.get("dialog.service.desc_prompt"));
        TextField priceField = new TextField();
        priceField.setPromptText(I18n.get("dialog.service.price_prompt"));
        TextField timeField = new TextField();
        timeField.setPromptText(I18n.get("dialog.service.time_prompt"));

        grid.add(new Label(I18n.get("table.col.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("table.col.description") + ":"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label(I18n.get("table.col.price") + ":"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label(I18n.get("table.col.time") + ":"), 0, 3);
        grid.add(timeField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String name = nameField.getText().trim();
                String desc = descField.getText().trim();
                String priceStr = priceField.getText().trim();
                String timeStr = timeField.getText().trim();

                if (name.isEmpty() || priceStr.isEmpty() || timeStr.isEmpty()) {
                    showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }

                double price;
                int time;
                try {
                    price = Double.parseDouble(priceStr.replace(",", "."));
                    time = Integer.parseInt(timeStr);
                } catch (NumberFormatException e) {
                    showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.invalid_number"));
                    return;
                }

                try {
                    garage.createServiceItem(name, desc, price, time);
                } catch (SQLException e) {
                    showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }

                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showEditServiceItemDialog(GarageSystem garage, ServiceItem serviceItem, Runnable onSuccess) {
        if (serviceItem == null) return;

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.service.edit.title"));
        dialog.setHeaderText(I18n.get("dialog.service.edit.header"));
        styleDialog(dialog);

        GridPane grid = createGrid();

        TextField nameField = new TextField(serviceItem.getName() != null ? serviceItem.getName() : "");
        nameField.setPromptText(I18n.get("dialog.service.name_prompt"));
        TextField descField = new TextField(serviceItem.getDescription() != null ? serviceItem.getDescription() : "");
        descField.setPromptText(I18n.get("dialog.service.desc_prompt"));
        TextField priceField = new TextField(String.valueOf(serviceItem.getPrice()));
        priceField.setPromptText(I18n.get("dialog.service.price_prompt"));
        TextField timeField = new TextField(String.valueOf(serviceItem.getEstimatedMinutes()));
        timeField.setPromptText(I18n.get("dialog.service.time_prompt"));

        grid.add(new Label(I18n.get("table.col.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("table.col.description") + ":"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label(I18n.get("table.col.price") + ":"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label(I18n.get("table.col.time") + ":"), 0, 3);
        grid.add(timeField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String name = nameField.getText().trim();
                String desc = descField.getText().trim();
                String priceStr = priceField.getText().trim();
                String timeStr = timeField.getText().trim();

                if (name.isEmpty() || priceStr.isEmpty() || timeStr.isEmpty()) {
                    showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }

                double price;
                int time;
                try {
                    price = Double.parseDouble(priceStr.replace(",", "."));
                    time = Integer.parseInt(timeStr);
                } catch (NumberFormatException e) {
                    showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.invalid_number"));
                    return;
                }

                serviceItem.setName(name);
                serviceItem.setDescription(desc);
                serviceItem.setPrice(price);
                serviceItem.setEstimatedMinutes(time);

                try {
                    garage.updateServiceItem(serviceItem);
                } catch (SQLException e) {
                    showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }

                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showDeleteServiceItemConfirmation(GarageSystem garage, ServiceItem serviceItem, Runnable onSuccess) {
        if (serviceItem == null) return;

        if (!garage.canDeleteServiceItem(serviceItem.getId())) {
            showError(I18n.get("dialog.service.delete.title"),
                    I18n.get("dialog.service.delete.has_active_orders", serviceItem.getName()));
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18n.get("dialog.service.delete.title"));
        alert.setHeaderText(I18n.get("dialog.service.delete.header"));
        alert.setContentText(I18n.get("dialog.service.delete.confirm", serviceItem.getName()));
        styleDialog(alert);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    garage.deleteServiceItem(serviceItem.getId());
                } catch (SQLException e) {
                    showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    // ------------------------------------------------- 9. Slot / Work Order Details
    public static void showSlotDetailsDialog(GarageSystem garage,
                                             com.wac.autocore.service.MechanicSchedule.TimeSlot slot,
                                             com.wac.autocore.ui.navigation.PageRouter router,
                                             Runnable onRefresh) {
        SlotDetailsDialog.showSlotDetailsDialog(garage, slot, router, onRefresh);
    }

    public static void showWorkOrderDetailsDialog(GarageSystem garage, int workOrderId,
                                                  com.wac.autocore.ui.navigation.PageRouter router, Runnable onRefresh) {
        SlotDetailsDialog.showWorkOrderDetailsDialog(garage, workOrderId, router, onRefresh);
    }

    // ----------------------------------------------------------- Helpers
    static GridPane createGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(14, 14, 14, 14));
        return grid;
    }

    static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        javafx.scene.Scene appScene = com.wac.autocore.theme.ThemeManager.getCurrentScene();
        if (appScene != null && appScene.getWindow() != null) {
            try {
                alert.initOwner(appScene.getWindow());
                alert.initModality(javafx.stage.Modality.WINDOW_MODAL);
            } catch (Exception ignored) {}
        }
        alert.setOnShowing(evt -> {
            javafx.scene.Scene currentAppScene = com.wac.autocore.theme.ThemeManager.getCurrentScene();
            if (currentAppScene != null) {
                javafx.scene.Scene aScene = alert.getDialogPane().getScene();
                if (aScene != null) {
                    aScene.getStylesheets().setAll(currentAppScene.getStylesheets());
                }
            }
        });
        alert.showAndWait();
    }
}
