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

    private static void styleDialog(Dialog<?> dialog) {
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

    // ---------------------------------------------------------- 3. Booking
    public static void showCreateBookingDialog(GarageSystem garage, Runnable onSuccess) {
        showCreateBookingDialog(garage, null, null, null, onSuccess);
    }

    public static void showCreateBookingDialog(GarageSystem garage, LocalDate defaultDate,
                                               Mechanic defaultMechanic, Integer defaultHour, Runnable onSuccess) {
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

        LocalDate initialDate = defaultDate != null ? defaultDate : LocalDate.now().plusDays(1);
        DatePicker datePicker = new DatePicker(initialDate);
        TextField descField = new TextField();
        descField.setPromptText(I18n.get("dialog.booking.desc_prompt"));

        int rowIdx = 0;
        grid.add(new Label(I18n.get("dialog.booking.vehicle_select") + ":"), 0, rowIdx);
        grid.add(vehicleBox, 1, rowIdx++);

        grid.add(new Label(I18n.get("dialog.booking.date") + ":"), 0, rowIdx);
        grid.add(datePicker, 1, rowIdx++);

        if (defaultMechanic != null && defaultHour != null) {
            Label mechInfo = new Label(defaultMechanic.getName() + " (" + String.format("%02d:00 - %02d:00", defaultHour, defaultHour + 1) + ")");
            mechInfo.setStyle("-fx-font-weight: bold; -fx-text-fill: -wac-accent;");
            grid.add(new Label(I18n.get("table.col.mechanic") + ":"), 0, rowIdx);
            grid.add(mechInfo, 1, rowIdx++);
        }

        grid.add(new Label(I18n.get("table.col.description") + ":"), 0, rowIdx);
        grid.add(descField, 1, rowIdx++);

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

                Booking b = null;

                    b = garage.createBooking(v.getId(), date, desc);

                if (defaultMechanic != null && defaultHour != null && b != null) {
                    WorkOrder wo = garage.createWorkOrder(b.getId(), defaultMechanic.getId(), 1);
                    int woId = wo != null ? wo.getId() : 0;
                    com.wac.autocore.service.MechanicSchedule.getInstance().bookSlot(
                            defaultMechanic.getId(), date, defaultHour, b.getId(), woId,
                            EntityLookup.customerName(garage, v.getCustomerId()),
                            v.getRegistrationNumber(), desc
                    );
                }
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

    // ------------------------------------------------- 8. Slot / Work Order Details
    public static void showSlotDetailsDialog(GarageSystem garage,
                                             com.wac.autocore.service.MechanicSchedule.TimeSlot slot,
                                             com.wac.autocore.ui.navigation.PageRouter router,
                                             Runnable onRefresh) {
        if (slot == null || !slot.isBooked()) {
            return;
        }

        // Hitta arbetsorder och/eller bokning för detta slot
        WorkOrder targetOrder = null;
        if (slot.getWorkOrderId() > 0) {
            for (WorkOrder wo : garage.getWorkOrders()) {
                if (wo.getId() == slot.getWorkOrderId()) {
                    targetOrder = wo;
                    break;
                }
            }
        }
        if (targetOrder == null && slot.getBookingId() > 0) {
            for (WorkOrder wo : garage.getWorkOrders()) {
                if (wo.getBookingId() == slot.getBookingId()) {
                    targetOrder = wo;
                    slot.setWorkOrderId(wo.getId());
                    break;
                }
            }
        }

        Booking booking = null;
        if (targetOrder != null) {
            for (Booking bk : garage.getBookings()) {
                if (bk.getId() == targetOrder.getBookingId()) {
                    booking = bk;
                    break;
                }
            }
        }
        if (booking == null && slot.getBookingId() > 0) {
            for (Booking bk : garage.getBookings()) {
                if (bk.getId() == slot.getBookingId()) {
                    booking = bk;
                    break;
                }
            }
        }

        final WorkOrder wo = targetOrder;
        final Booking b = booking;

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        boolean hasWorkOrder = (wo != null);
        String title = hasWorkOrder
                ? I18n.get("kanban.drawer.work_order", wo.getId())
                : I18n.get("kanban.drawer.booked");
        dialog.setTitle(title);
        dialog.setHeaderText(I18n.get("dialog.slot.header", slot.getDate().toString() + " (" + slot.getTimeRange() + ")"));
        styleDialog(dialog);

        GridPane grid = createGrid();
        int rowIdx = 0;

        // Datum & Tid
        grid.add(new Label(I18n.get("dialog.slot.time_date")), 0, rowIdx);
        Label timeLabel = new Label(slot.getDate() + "  |  " + slot.getTimeRange());
        timeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -wac-accent;");
        grid.add(timeLabel, 1, rowIdx++);

        // Mekaniker
        grid.add(new Label(I18n.get("table.col.mechanic") + ":"), 0, rowIdx);
        grid.add(new Label(EntityLookup.mechanicName(garage, slot.getMechanicId())), 1, rowIdx++);

        // Fordon
        String reg = slot.getVehicleReg() != null && !slot.getVehicleReg().isEmpty() ? slot.getVehicleReg() : "-";
        if (b != null) {
            reg = EntityLookup.vehicleReg(garage, b.getVehicleId());
        }
        grid.add(new Label(I18n.get("table.col.vehicle") + ":"), 0, rowIdx);
        grid.add(new Label(reg), 1, rowIdx++);

        // Kund
        String cust = slot.getCustomerName() != null && !slot.getCustomerName().isEmpty() ? slot.getCustomerName() : "-";
        grid.add(new Label(I18n.get("table.col.customer") + ":"), 0, rowIdx);
        grid.add(new Label(cust), 1, rowIdx++);

        // Beskrivning
        String desc = slot.getDescription() != null && !slot.getDescription().isEmpty() ? slot.getDescription() : "-";
        if (b != null && b.getDescription() != null && !b.getDescription().isEmpty()) {
            desc = b.getDescription();
        }
        grid.add(new Label(I18n.get("table.col.description") + ":"), 0, rowIdx);
        grid.add(new Label(desc), 1, rowIdx++);

        // Status och Tjänster
        if (hasWorkOrder) {
            grid.add(new Label(I18n.get("table.col.services") + ":"), 0, rowIdx);
            grid.add(new Label(EntityLookup.serviceNames(garage, wo.getServiceItemIds())), 1, rowIdx++);

            grid.add(new Label(I18n.get("table.col.status") + ":"), 0, rowIdx);
            Label stLabel = new Label(com.wac.autocore.ui.util.UiFormatters.statusWord(wo.getStatus()));
            stLabel.getStyleClass().addAll("badge", com.wac.autocore.ui.util.UiFormatters.badgeClass(stLabel.getText()));
            grid.add(stLabel, 1, rowIdx++);
        } else {
            grid.add(new Label(I18n.get("table.col.status") + ":"), 0, rowIdx);
            Label stLabel = new Label(I18n.get("dialog.slot.booked_status"));
            stLabel.getStyleClass().addAll("badge", "yellow");
            grid.add(stLabel, 1, rowIdx++);
        }

        ButtonType actionBtnType = new ButtonType(
                hasWorkOrder ? I18n.get("dialog.slot.open_order")
                             : I18n.get("dialog.slot.create_order"),
                javafx.scene.control.ButtonBar.ButtonData.OTHER
        );
        ButtonType closeType = ButtonType.CLOSE;

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(actionBtnType, closeType);

        dialog.showAndWait().ifPresent(response -> {
            if (response == actionBtnType && router != null) {
                if (wo != null) {
                    router.navigateToWorkOrder(wo.getId());
                } else {
                    // Skapa workorder för denna bokade timme om ingen finns
                    Booking targetBooking = b;
                    if (targetBooking == null) {
                        int vehicleId = 1;
                        for (Vehicle v : garage.getVehicles()) {
                            if (v.getRegistrationNumber().equalsIgnoreCase(slot.getVehicleReg())) {
                                vehicleId = v.getId();
                                break;
                            }
                        }
                            targetBooking = garage.createBooking(vehicleId, slot.getDate(), slot.getDescription());

                        slot.setBookingId(targetBooking.getId());
                    }
                    WorkOrder createdWo = garage.createWorkOrder(targetBooking.getId(), slot.getMechanicId(), 1);
                    if (createdWo != null) {
                        slot.setWorkOrderId(createdWo.getId());
                        if (onRefresh != null) onRefresh.run();
                        router.navigateToWorkOrder(createdWo.getId());
                    }
                }
            }
        });
    }

    public static void showWorkOrderDetailsDialog(GarageSystem garage, int workOrderId,
                                                 com.wac.autocore.ui.navigation.PageRouter router, Runnable onRefresh) {
        WorkOrder targetOrder = null;
        for (WorkOrder wo : garage.getWorkOrders()) {
            if (wo.getId() == workOrderId) {
                targetOrder = wo;
                break;
            }
        }

        if (targetOrder == null && !garage.getWorkOrders().isEmpty()) {
            targetOrder = garage.getWorkOrders().get(0);
        }

        if (targetOrder == null) {
            showError(I18n.get("dialog.confirm.title"), I18n.get("overview.empty.workorders"));
            return;
        }

        final WorkOrder wo = targetOrder;
        Booking b = null;
        for (Booking bk : garage.getBookings()) {
            if (bk.getId() == wo.getBookingId()) {
                b = bk;
                break;
            }
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("kanban.drawer.work_order", wo.getId()));
        dialog.setHeaderText(I18n.get("dialog.workorder.details_header", wo.getId()));
        styleDialog(dialog);

        GridPane grid = createGrid();

        int rowIdx = 0;
        grid.add(new Label(I18n.get("table.col.id") + ":"), 0, rowIdx);
        Label idLbl = new Label("#" + wo.getId());
        idLbl.setStyle("-fx-font-weight: bold;");
        grid.add(idLbl, 1, rowIdx++);

        grid.add(new Label(I18n.get("table.col.mechanic") + ":"), 0, rowIdx);
        grid.add(new Label(EntityLookup.mechanicName(garage, wo.getMechanicId())), 1, rowIdx++);

        if (b != null) {
            grid.add(new Label(I18n.get("table.col.vehicle") + ":"), 0, rowIdx);
            grid.add(new Label(EntityLookup.vehicleReg(garage, b.getVehicleId())), 1, rowIdx++);

            grid.add(new Label(I18n.get("table.col.date") + ":"), 0, rowIdx);
            grid.add(new Label(b.getDate().toString()), 1, rowIdx++);

            grid.add(new Label(I18n.get("table.col.description") + ":"), 0, rowIdx);
            grid.add(new Label(b.getDescription()), 1, rowIdx++);
        }

        grid.add(new Label(I18n.get("table.col.services") + ":"), 0, rowIdx);
        grid.add(new Label(EntityLookup.serviceNames(garage, wo.getServiceItemIds())), 1, rowIdx++);

        grid.add(new Label(I18n.get("table.col.status") + ":"), 0, rowIdx);
        Label stLabel = new Label(com.wac.autocore.ui.util.UiFormatters.statusWord(wo.getStatus()));
        stLabel.getStyleClass().addAll("badge", com.wac.autocore.ui.util.UiFormatters.badgeClass(stLabel.getText()));
        grid.add(stLabel, 1, rowIdx++);

        ButtonType gotoType = new ButtonType(I18n.get("dialog.workorder.open_in_orders"), javafx.scene.control.ButtonBar.ButtonData.OTHER);
        ButtonType closeType = ButtonType.CLOSE;

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(gotoType, closeType);

        dialog.showAndWait().ifPresent(response -> {
            if (response == gotoType && router != null) {
                router.navigateToWorkOrder(wo.getId());
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
