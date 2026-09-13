package com.wac.autocore.ui;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Modala formulärdialoger för att utföra alla systemets affärsåtgärder direkt i JavaFX GUI.
 *
 * Stödjer:
 * - Skapa ny kund
 * - Registrera fordon på befintlig kund
 * - Skapa ny tidsbokning
 * - Skapa arbetsorder med mekanikertilldelning och multival av tjänster
 * - Skapa faktura med valfri kampanjkod
 * - Genomföra betalning (Swish, Kort, Kontant)
 */
public final class ActionDialogs {

    private ActionDialogs() {}

    private static void styleDialog(Dialog<?> dialog) {
        dialog.getDialogPane().getStyleClass().add("panel");
    }

    // ------------------------------------------------------------- 1. Kund
    public static void showCreateCustomerDialog(GarageSystem garage, Runnable onSuccess) {
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle("Ny kund");
        dialog.setHeaderText("Registrera en ny kund i AutoCore");
        styleDialog(dialog);

        GridPane grid = createGrid();

        TextField nameField = new TextField();
        nameField.setPromptText("För- och efternamn");
        TextField phoneField = new TextField();
        phoneField.setPromptText("070-1234567");
        TextField emailField = new TextField();
        emailField.setPromptText("namn@example.se");

        grid.add(new Label("Namn:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Telefon:"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label("E-post:"), 0, 2);
        grid.add(emailField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String name = nameField.getText().trim();
                String phone = phoneField.getText().trim();
                String email = emailField.getText().trim();

                if (name.isEmpty() || phone.isEmpty()) {
                    showError("Felaktig inmatning", "Namn och telefonnummer måste fyllas i.");
                    return;
                }

                garage.createCustomer(name, phone, email);
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    // ----------------------------------------------------------- 2. Fordon
    public static void showCreateVehicleDialog(GarageSystem garage, Runnable onSuccess) {
        List<Customer> customers = garage.getCustomers();
        if (customers.isEmpty()) {
            showError("Inga kunder finns", "Skapa en kund först innan du registrerar ett fordon.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle("Nytt fordon");
        dialog.setHeaderText("Registrera ett nytt fordon i verkstaden");
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

        grid.add(new Label("Ägare:"), 0, 0);
        grid.add(customerBox, 1, 0);
        grid.add(new Label("Reg.nr:"), 0, 1);
        grid.add(regField, 1, 1);
        grid.add(new Label("Märke:"), 0, 2);
        grid.add(brandField, 1, 2);
        grid.add(new Label("Modell:"), 0, 3);
        grid.add(modelField, 1, 3);
        grid.add(new Label("Årsmodell:"), 0, 4);
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
                    showError("Ogiltigt årtal", "Ange ett giltigt fyrsiffrigt årtal.");
                    return;
                }

                if (reg.isEmpty() || brand.isEmpty()) {
                    showError("Felaktig inmatning", "Registreringsnummer och märke måste anges.");
                    return;
                }

                garage.createVehicle(reg, brand, model, year, owner.getId());
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    // ---------------------------------------------------------- 3. Bokning
    public static void showCreateBookingDialog(GarageSystem garage, Runnable onSuccess) {
        List<Vehicle> vehicles = garage.getVehicles();
        if (vehicles.isEmpty()) {
            showError("Inga fordon finns", "Registrera ett fordon innan en bokning kan skapas.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle("Ny bokning");
        dialog.setHeaderText("Boka in ett fordon för service eller reparation");
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
        descField.setPromptText("T.ex. Årlig service och bromsbyte");

        grid.add(new Label("Fordon:"), 0, 0);
        grid.add(vehicleBox, 1, 0);
        grid.add(new Label("Datum:"), 0, 1);
        grid.add(datePicker, 1, 1);
        grid.add(new Label("Beskrivning:"), 0, 2);
        grid.add(descField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Vehicle v = vehicleBox.getValue();
                LocalDate date = datePicker.getValue();
                String desc = descField.getText().trim();

                if (date == null || desc.isEmpty()) {
                    showError("Felaktig inmatning", "Datum och beskrivning måste anges.");
                    return;
                }

                garage.createBooking(v.getId(), date, desc);
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    // ----------------------------------------------------- 4. Arbetsorder
    public static void showCreateWorkOrderDialog(GarageSystem garage, Runnable onSuccess) {
        List<Booking> bookings = new ArrayList<Booking>();
        for (Booking b : garage.getBookings()) {
            if ("BOOKED".equalsIgnoreCase(b.getStatus())) {
                bookings.add(b);
            }
        }

        if (bookings.isEmpty()) {
            showError("Inga bokade jobb", "Det finns inga aktiva bokningar med status 'BOOKED' att skapa arbetsorder på.");
            return;
        }

        List<Mechanic> mechanics = garage.getMechanics();
        if (mechanics.isEmpty()) {
            showError("Inga mekaniker", "Det finns inga registrerade mekaniker i systemet.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle("Ny arbetsorder");
        dialog.setHeaderText("Tilldela mekaniker och välj tjänster för arbetsordern");
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
                return b == null ? "" : "Bokning #" + b.getId() + " - " + b.getDescription() + " (" + b.getDate() + ")";
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
                return m == null ? "" : m.getName() + " (" + m.getSpecialization() + ") - " + (m.isAvailable() ? "Ledig" : "Upptagen");
            }
            @Override
            public Mechanic fromString(String string) { return null; }
        });

        grid.add(new Label("Bokning:"), 0, 0);
        grid.add(bookingBox, 1, 0);
        grid.add(new Label("Mekaniker:"), 0, 1);
        grid.add(mechanicBox, 1, 1);

        Label servicesTitle = new Label("Välj tjänster som ska ingå:");
        servicesTitle.setStyle("-fx-font-weight: bold;");

        VBox serviceChecks = new VBox(6);
        List<CheckBox> checkList = new ArrayList<CheckBox>();
        for (ServiceItem s : garage.getServiceItems()) {
            CheckBox cb = new CheckBox(s.getName() + " (" + s.getPrice() + " kr, " + s.getEstimatedMinutes() + " min)");
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
                    showError("Inga tjänster valda", "Du måste välja minst en tjänst.");
                    return;
                }

                int[] ids = new int[selectedServiceIds.size()];
                for (int i = 0; i < ids.length; i++) ids[i] = selectedServiceIds.get(i);

                garage.createWorkOrder(b.getId(), m.getId(), ids);
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    // --------------------------------------------------------- 5. Faktura
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
            showError("Inga slutförda ordrar", "Det finns inga slutförda arbetsordrar ('COMPLETED') som saknar faktura.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle("Skapa faktura");
        dialog.setHeaderText("Generera faktura för en slutförd arbetsorder");
        styleDialog(dialog);

        GridPane grid = createGrid();

        ComboBox<WorkOrder> orderBox = new ComboBox<WorkOrder>();
        orderBox.getItems().addAll(completedOrders);
        orderBox.getSelectionModel().selectFirst();
        orderBox.setConverter(new StringConverter<WorkOrder>() {
            @Override
            public String toString(WorkOrder wo) {
                return wo == null ? "" : "Arbetsorder #" + wo.getId() + " (Bokning #" + wo.getBookingId() + ")";
            }
            @Override
            public WorkOrder fromString(String string) { return null; }
        });

        TextField discountField = new TextField();
        discountField.setPromptText("T.ex. WELCOME10 eller SERVICE200 (valfri)");

        grid.add(new Label("Arbetsorder:"), 0, 0);
        grid.add(orderBox, 1, 0);
        grid.add(new Label("Rabattkod:"), 0, 1);
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

    // ------------------------------------------------------- 6. Betalning
    public static void showProcessPaymentDialog(GarageSystem garage, Invoice preselected, Runnable onSuccess) {
        List<Invoice> unpaid = new ArrayList<Invoice>();
        for (Invoice inv : garage.getInvoices()) {
            if (!inv.isPaid()) {
                unpaid.add(inv);
            }
        }

        if (unpaid.isEmpty()) {
            showError("Inga obetalda fakturor", "Alla utfärdade fakturor är redan betalda!");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle("Registrera betalning");
        dialog.setHeaderText("Genomför och bokför kundens betalning");
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
                return inv == null ? "" : "Faktura #" + inv.getId() + " - " + inv.getTotalAmount() + " kr (Order #" + inv.getWorkOrderId() + ")";
            }
            @Override
            public Invoice fromString(String string) { return null; }
        });

        ComboBox<String> typeBox = new ComboBox<String>();
        typeBox.getItems().addAll("SWISH", "CARD", "CASH");
        typeBox.getSelectionModel().select("SWISH");

        grid.add(new Label("Faktura:"), 0, 0);
        grid.add(invoiceBox, 1, 0);
        grid.add(new Label("Betalsätt:"), 0, 1);
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

    // ------------------------------------------------------------- Helpers
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
        alert.showAndWait();
    }
}
