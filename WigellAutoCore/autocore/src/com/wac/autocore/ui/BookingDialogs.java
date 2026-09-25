package com.wac.autocore.ui;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.MechanicSchedule;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.EntityLookup;
import com.wac.autocore.ui.util.UiFormatters;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Modala dialoger för att skapa, redigera, avboka och ta bort bokningar.
 * Inkluderar fullt stöd för val av fordon, datum, tjänst, mekaniker och tidspass.
 */
public final class BookingDialogs {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private BookingDialogs() {}

    public static void showCreateBookingDialog(GarageSystem garage, Runnable onSuccess) {
        showCreateBookingDialog(garage, null, null, null, onSuccess);
    }

    public static void showCreateBookingDialog(GarageSystem garage, LocalDate defaultDate,
                                               Mechanic defaultMechanic, Integer defaultHour, Runnable onSuccess) {
        List<Vehicle> vehicles = garage.getVehicles();
        if (vehicles.isEmpty()) {
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.booking.create.title"));
        dialog.setHeaderText(I18n.get("dialog.booking.create.header"));
        ActionDialogs.styleDialog(dialog);

        GridPane grid = ActionDialogs.createGrid();

        // 1. Fordon
        ComboBox<Vehicle> vehicleBox = new ComboBox<Vehicle>();
        vehicleBox.getItems().addAll(vehicles);
        vehicleBox.getSelectionModel().selectFirst();
        vehicleBox.setConverter(new StringConverter<Vehicle>() {
            @Override
            public String toString(Vehicle v) {
                if (v == null) return "";
                String owner = EntityLookup.customerName(garage, v.getCustomerId());
                return v.getId() + " - " + v.getRegistrationNumber() + " (" + v.getBrand() + " " + v.getModel() + ") · " + owner;
            }
            @Override
            public Vehicle fromString(String string) { return null; }
        });

        // 2. Datum
        LocalDate initialDate = defaultDate != null ? defaultDate : LocalDate.now().plusDays(1);
        DatePicker datePicker = new DatePicker(initialDate);

        // 3. Tjänst
        List<ServiceItem> services = garage.getServiceItems();
        ComboBox<ServiceItem> serviceBox = new ComboBox<ServiceItem>();
        serviceBox.getItems().add(null); // Tillåt att ingen specifik tjänst väljs
        serviceBox.getItems().addAll(services);
        serviceBox.setConverter(new StringConverter<ServiceItem>() {
            @Override
            public String toString(ServiceItem s) {
                if (s == null) return I18n.get("dialog.booking.no_service");
                return s.getName() + " · " + UiFormatters.formatMoney(s.getPrice()) + " (" + s.getEstimatedMinutes() + " min)";
            }
            @Override
            public ServiceItem fromString(String string) { return null; }
        });
        serviceBox.getSelectionModel().selectFirst();

        // 4. Mekaniker
        List<Mechanic> mechanics = garage.getMechanics();
        ComboBox<Mechanic> mechanicBox = new ComboBox<Mechanic>();
        mechanicBox.getItems().add(null); // Tillåt "ingen tilldelad ännu"
        mechanicBox.getItems().addAll(mechanics);
        mechanicBox.setConverter(new StringConverter<Mechanic>() {
            @Override
            public String toString(Mechanic m) {
                if (m == null) return I18n.get("dialog.booking.no_mechanic");
                return m.getName() + " (" + m.getSpecialization() + ")";
            }
            @Override
            public Mechanic fromString(String string) { return null; }
        });
        if (defaultMechanic != null) {
            for (Mechanic m : mechanics) {
                if (m.getId() == defaultMechanic.getId()) {
                    mechanicBox.getSelectionModel().select(m);
                    break;
                }
            }
        } else {
            mechanicBox.getSelectionModel().selectFirst();
        }

        // 5. Starttid
        List<LocalTime> timeOptions = new ArrayList<LocalTime>();
        for (int h = 7; h <= 16; h++) {
            timeOptions.add(LocalTime.of(h, 0));
        }
        ComboBox<LocalTime> startTimeBox = new ComboBox<LocalTime>();
        startTimeBox.setItems(FXCollections.observableArrayList(timeOptions));

        java.util.function.Function<LocalTime, Boolean> isBusyFunc = time -> {
            if (time == null) return false;
            Mechanic m = mechanicBox.getValue();
            LocalDate d = datePicker.getValue();
            return isHourBooked(garage, m, d, time.getHour(), 0);
        };

        startTimeBox.setCellFactory(lv -> new TimeSlotCell(isBusyFunc, true));
        startTimeBox.setButtonCell(new TimeSlotCell(isBusyFunc, false));

        if (defaultHour != null && defaultHour >= 7 && defaultHour <= 16) {
            startTimeBox.getSelectionModel().select(LocalTime.of(defaultHour, 0));
        } else {
            LocalTime firstFree = null;
            for (LocalTime t : timeOptions) {
                if (!Boolean.TRUE.equals(isBusyFunc.apply(t))) {
                    firstFree = t;
                    break;
                }
            }
            startTimeBox.getSelectionModel().select(firstFree != null ? firstFree : LocalTime.of(8, 0));
        }
        startTimeBox.setConverter(new StringConverter<LocalTime>() {
            @Override
            public String toString(LocalTime t) {
                return t == null ? "" : t.format(TIME_FMT);
            }
            @Override
            public LocalTime fromString(String string) { return null; }
        });

        // 6. Dynamisk sluttid
        Label durationLabel = new Label();
        durationLabel.setStyle("-fx-text-fill: -wac-accent; -fx-font-weight: bold;");

        Runnable updateDuration = () -> {
            LocalTime start = startTimeBox.getValue();
            ServiceItem selService = serviceBox.getValue();
            if (start != null && selService != null) {
                LocalTime end = start.plusMinutes(selService.getEstimatedMinutes());
                durationLabel.setText(I18n.get("dialog.booking.time_window", start.format(TIME_FMT), end.format(TIME_FMT))
                        + " (" + selService.getEstimatedMinutes() + " min)");
            } else if (start != null) {
                LocalTime end = start.plusHours(1);
                durationLabel.setText(I18n.get("dialog.booking.time_window", start.format(TIME_FMT), end.format(TIME_FMT)) + " (60 min)");
            } else {
                durationLabel.setText("");
            }
        };

        Runnable refreshTimeBox = () -> {
            LocalTime currentSel = startTimeBox.getValue();
            startTimeBox.setItems(FXCollections.observableArrayList(timeOptions));
            if (currentSel != null && !Boolean.TRUE.equals(isBusyFunc.apply(currentSel))) {
                startTimeBox.getSelectionModel().select(currentSel);
            } else {
                LocalTime firstFree = null;
                for (LocalTime t : timeOptions) {
                    if (!Boolean.TRUE.equals(isBusyFunc.apply(t))) {
                        firstFree = t;
                        break;
                    }
                }
                if (firstFree != null) {
                    startTimeBox.getSelectionModel().select(firstFree);
                } else if (currentSel != null) {
                    startTimeBox.getSelectionModel().select(currentSel);
                }
            }
            if (startTimeBox.getButtonCell() != null) {
                startTimeBox.getButtonCell().updateIndex(-1);
            }
            updateDuration.run();
        };

        datePicker.valueProperty().addListener((obs, o, n) -> refreshTimeBox.run());
        mechanicBox.valueProperty().addListener((obs, o, n) -> refreshTimeBox.run());
        startTimeBox.valueProperty().addListener((obs, o, n) -> updateDuration.run());
        serviceBox.valueProperty().addListener((obs, o, n) -> updateDuration.run());
        updateDuration.run();

        // 7. Beskrivning
        TextField descField = new TextField();
        descField.setPromptText(I18n.get("dialog.booking.desc_prompt"));
        serviceBox.valueProperty().addListener((obs, o, n) -> {
            if (n != null && descField.getText().trim().isEmpty()) {
                descField.setText(n.getName() + (n.getDescription() != null && !n.getDescription().isEmpty() ? " - " + n.getDescription() : ""));
            }
        });

        // Layout i Grid
        int rowIdx = 0;
        grid.add(new Label(I18n.get("dialog.booking.vehicle_select") + ":"), 0, rowIdx);
        grid.add(vehicleBox, 1, rowIdx++);

        grid.add(new Label(I18n.get("dialog.booking.date") + ":"), 0, rowIdx);
        grid.add(datePicker, 1, rowIdx++);

        grid.add(new Label(I18n.get("dialog.booking.service_select") + ":"), 0, rowIdx);
        grid.add(serviceBox, 1, rowIdx++);

        grid.add(new Label(I18n.get("dialog.booking.mechanic_select") + ":"), 0, rowIdx);
        grid.add(mechanicBox, 1, rowIdx++);

        grid.add(new Label(I18n.get("dialog.booking.time_select") + ":"), 0, rowIdx);
        grid.add(startTimeBox, 1, rowIdx++);

        grid.add(new Label(""), 0, rowIdx);
        grid.add(durationLabel, 1, rowIdx++);

        grid.add(new Label(I18n.get("table.col.description") + ":"), 0, rowIdx);
        grid.add(descField, 1, rowIdx++);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Vehicle v = vehicleBox.getValue();
                LocalDate date = datePicker.getValue();
                ServiceItem chosenService = serviceBox.getValue();
                Mechanic chosenMech = mechanicBox.getValue();
                LocalTime startTime = startTimeBox.getValue();
                String desc = descField.getText().trim();

                if (desc.isEmpty() && chosenService != null) {
                    desc = chosenService.getName();
                }

                if (date == null || desc.isEmpty() || v == null) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }

                if (chosenMech != null && startTime != null && isHourBooked(garage, chosenMech, date, startTime.getHour(), 0)) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.booking.slot_busy_error"));
                    return;
                }

                Booking b = garage.createBooking(v.getId(), date, desc);
                if (b != null) {
                    if (chosenService != null) {
                        b.setServiceItemId(chosenService.getId());
                    }
                    if (chosenMech != null) {
                        b.setMechanicId(chosenMech.getId());
                    }
                    if (startTime != null) {
                        b.setStartTime(startTime);
                        int estMin = chosenService != null ? chosenService.getEstimatedMinutes() : 60;
                        b.setEndTime(startTime.plusMinutes(estMin));
                    }

                    try {
                        garage.updateBooking(b);
                    } catch (Exception ignored) {}

                    // Koppla kanban-schema om mekaniker valts
                    if (chosenMech != null) {
                        int hour = startTime != null ? startTime.getHour() : (defaultHour != null ? defaultHour : 8);
                        String custName = EntityLookup.customerName(garage, v.getCustomerId());
                        MechanicSchedule.getInstance().bookSlot(
                                chosenMech.getId(), date, hour, b.getId(), 0,
                                custName, v.getRegistrationNumber(), desc
                        );
                    }
                }
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showEditBookingDialog(GarageSystem garage, Booking booking, Runnable onSuccess) {
        if (booking == null) return;

        List<Vehicle> vehicles = garage.getVehicles();
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.booking.edit.title"));
        dialog.setHeaderText(I18n.get("dialog.booking.edit.header"));
        ActionDialogs.styleDialog(dialog);

        GridPane grid = ActionDialogs.createGrid();

        // 1. Fordon
        ComboBox<Vehicle> vehicleBox = new ComboBox<Vehicle>();
        vehicleBox.getItems().addAll(vehicles);
        for (Vehicle v : vehicles) {
            if (v.getId() == booking.getVehicleId()) {
                vehicleBox.getSelectionModel().select(v);
                break;
            }
        }
        vehicleBox.setConverter(new StringConverter<Vehicle>() {
            @Override
            public String toString(Vehicle v) {
                if (v == null) return "";
                String owner = EntityLookup.customerName(garage, v.getCustomerId());
                return v.getId() + " - " + v.getRegistrationNumber() + " (" + v.getBrand() + " " + v.getModel() + ") · " + owner;
            }
            @Override
            public Vehicle fromString(String string) { return null; }
        });

        // 2. Datum
        DatePicker datePicker = new DatePicker(booking.getDate() != null ? booking.getDate() : LocalDate.now());

        // 3. Tjänst
        List<ServiceItem> services = garage.getServiceItems();
        ComboBox<ServiceItem> serviceBox = new ComboBox<ServiceItem>();
        serviceBox.getItems().add(null);
        serviceBox.getItems().addAll(services);
        serviceBox.setConverter(new StringConverter<ServiceItem>() {
            @Override
            public String toString(ServiceItem s) {
                if (s == null) return I18n.get("dialog.booking.no_service");
                return s.getName() + " · " + UiFormatters.formatMoney(s.getPrice()) + " (" + s.getEstimatedMinutes() + " min)";
            }
            @Override
            public ServiceItem fromString(String string) { return null; }
        });
        if (booking.getServiceItemId() > 0) {
            for (ServiceItem s : services) {
                if (s.getId() == booking.getServiceItemId()) {
                    serviceBox.getSelectionModel().select(s);
                    break;
                }
            }
        } else {
            serviceBox.getSelectionModel().selectFirst();
        }

        // 4. Mekaniker
        List<Mechanic> mechanics = garage.getMechanics();
        ComboBox<Mechanic> mechanicBox = new ComboBox<Mechanic>();
        mechanicBox.getItems().add(null);
        mechanicBox.getItems().addAll(mechanics);
        mechanicBox.setConverter(new StringConverter<Mechanic>() {
            @Override
            public String toString(Mechanic m) {
                if (m == null) return I18n.get("dialog.booking.no_mechanic");
                return m.getName() + " (" + m.getSpecialization() + ")";
            }
            @Override
            public Mechanic fromString(String string) { return null; }
        });
        if (booking.getMechanicId() > 0) {
            for (Mechanic m : mechanics) {
                if (m.getId() == booking.getMechanicId()) {
                    mechanicBox.getSelectionModel().select(m);
                    break;
                }
            }
        } else {
            mechanicBox.getSelectionModel().selectFirst();
        }

        // 5. Starttid
        List<LocalTime> timeOptions = new ArrayList<LocalTime>();
        for (int h = 7; h <= 16; h++) {
            timeOptions.add(LocalTime.of(h, 0));
        }
        ComboBox<LocalTime> startTimeBox = new ComboBox<LocalTime>();
        startTimeBox.setItems(FXCollections.observableArrayList(timeOptions));

        java.util.function.Function<LocalTime, Boolean> isBusyFunc = time -> {
            if (time == null) return false;
            Mechanic m = mechanicBox.getValue();
            LocalDate d = datePicker.getValue();
            return isHourBooked(garage, m, d, time.getHour(), booking.getId());
        };

        startTimeBox.setCellFactory(lv -> new TimeSlotCell(isBusyFunc, true));
        startTimeBox.setButtonCell(new TimeSlotCell(isBusyFunc, false));

        if (booking.getStartTime() != null) {
            startTimeBox.getSelectionModel().select(booking.getStartTime());
        } else {
            LocalTime firstFree = null;
            for (LocalTime t : timeOptions) {
                if (!Boolean.TRUE.equals(isBusyFunc.apply(t))) {
                    firstFree = t;
                    break;
                }
            }
            startTimeBox.getSelectionModel().select(firstFree != null ? firstFree : LocalTime.of(8, 0));
        }
        startTimeBox.setConverter(new StringConverter<LocalTime>() {
            @Override
            public String toString(LocalTime t) {
                return t == null ? "" : t.format(TIME_FMT);
            }
            @Override
            public LocalTime fromString(String string) { return null; }
        });

        // 6. Dynamisk sluttid
        Label durationLabel = new Label();
        durationLabel.setStyle("-fx-text-fill: -wac-accent; -fx-font-weight: bold;");

        Runnable updateDuration = () -> {
            LocalTime start = startTimeBox.getValue();
            ServiceItem selService = serviceBox.getValue();
            if (start != null && selService != null) {
                LocalTime end = start.plusMinutes(selService.getEstimatedMinutes());
                durationLabel.setText(I18n.get("dialog.booking.time_window", start.format(TIME_FMT), end.format(TIME_FMT))
                        + " (" + selService.getEstimatedMinutes() + " min)");
            } else if (start != null) {
                LocalTime end = start.plusHours(1);
                durationLabel.setText(I18n.get("dialog.booking.time_window", start.format(TIME_FMT), end.format(TIME_FMT)) + " (60 min)");
            } else {
                durationLabel.setText("");
            }
        };

        Runnable refreshTimeBox = () -> {
            LocalTime currentSel = startTimeBox.getValue();
            startTimeBox.setItems(FXCollections.observableArrayList(timeOptions));
            if (currentSel != null && !Boolean.TRUE.equals(isBusyFunc.apply(currentSel))) {
                startTimeBox.getSelectionModel().select(currentSel);
            } else {
                LocalTime firstFree = null;
                for (LocalTime t : timeOptions) {
                    if (!Boolean.TRUE.equals(isBusyFunc.apply(t))) {
                        firstFree = t;
                        break;
                    }
                }
                if (firstFree != null) {
                    startTimeBox.getSelectionModel().select(firstFree);
                } else if (currentSel != null) {
                    startTimeBox.getSelectionModel().select(currentSel);
                }
            }
            if (startTimeBox.getButtonCell() != null) {
                startTimeBox.getButtonCell().updateIndex(-1);
            }
            updateDuration.run();
        };

        datePicker.valueProperty().addListener((obs, o, n) -> refreshTimeBox.run());
        mechanicBox.valueProperty().addListener((obs, o, n) -> refreshTimeBox.run());
        startTimeBox.valueProperty().addListener((obs, o, n) -> updateDuration.run());
        serviceBox.valueProperty().addListener((obs, o, n) -> updateDuration.run());
        updateDuration.run();

        // 7. Beskrivning & Status
        TextField descField = new TextField(booking.getDescription() != null ? booking.getDescription() : "");
        descField.setPromptText(I18n.get("dialog.booking.desc_prompt"));

        ComboBox<String> statusBox = new ComboBox<String>();
        statusBox.getItems().addAll("BOOKED", "CONFIRMED", "IN_PROGRESS", "COMPLETED", "CANCELLED");
        statusBox.getSelectionModel().select(booking.getStatus() != null ? booking.getStatus() : "BOOKED");

        int rowIdx = 0;
        grid.add(new Label(I18n.get("dialog.booking.vehicle_select") + ":"), 0, rowIdx);
        grid.add(vehicleBox, 1, rowIdx++);

        grid.add(new Label(I18n.get("dialog.booking.date") + ":"), 0, rowIdx);
        grid.add(datePicker, 1, rowIdx++);

        grid.add(new Label(I18n.get("dialog.booking.service_select") + ":"), 0, rowIdx);
        grid.add(serviceBox, 1, rowIdx++);

        grid.add(new Label(I18n.get("dialog.booking.mechanic_select") + ":"), 0, rowIdx);
        grid.add(mechanicBox, 1, rowIdx++);

        grid.add(new Label(I18n.get("dialog.booking.time_select") + ":"), 0, rowIdx);
        grid.add(startTimeBox, 1, rowIdx++);

        grid.add(new Label(""), 0, rowIdx);
        grid.add(durationLabel, 1, rowIdx++);

        grid.add(new Label(I18n.get("table.col.description") + ":"), 0, rowIdx);
        grid.add(descField, 1, rowIdx++);

        grid.add(new Label(I18n.get("table.col.status") + ":"), 0, rowIdx);
        grid.add(statusBox, 1, rowIdx++);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Vehicle v = vehicleBox.getValue();
                LocalDate date = datePicker.getValue();
                ServiceItem chosenService = serviceBox.getValue();
                Mechanic chosenMech = mechanicBox.getValue();
                LocalTime startTime = startTimeBox.getValue();
                String desc = descField.getText().trim();
                String status = statusBox.getValue();

                if (v == null || date == null || desc.isEmpty()) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }

                if (chosenMech != null && startTime != null && isHourBooked(garage, chosenMech, date, startTime.getHour(), booking.getId())) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.booking.slot_busy_error"));
                    return;
                }

                booking.setVehicleId(v.getId());
                booking.setDate(date);
                booking.setDescription(desc);
                booking.setStatus(status);
                booking.setServiceItemId(chosenService != null ? chosenService.getId() : 0);
                booking.setMechanicId(chosenMech != null ? chosenMech.getId() : 0);
                if (startTime != null) {
                    booking.setStartTime(startTime);
                    int estMin = chosenService != null ? chosenService.getEstimatedMinutes() : 60;
                    booking.setEndTime(startTime.plusMinutes(estMin));
                }

                try {
                    garage.updateBooking(booking);
                } catch (Exception ignored) {}

                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showCancelBookingConfirmation(GarageSystem garage, Booking booking, Runnable onSuccess) {
        if (booking == null) return;

        if (!garage.canCancelOrDeleteBooking(booking.getId())) {
            ActionDialogs.showError(I18n.get("dialog.booking.cancel.title"),
                    I18n.get("dialog.booking.cancel.has_active_orders", booking.getId()));
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(I18n.get("dialog.booking.cancel.title"));
        confirm.setHeaderText(I18n.get("dialog.booking.cancel.header"));
        confirm.setContentText(I18n.get("dialog.booking.cancel.confirm", booking.getId()));
        ActionDialogs.styleDialog(confirm);

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                try {
                    garage.cancelBooking(booking.getId());
                } catch (Exception e) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showDeleteBookingConfirmation(GarageSystem garage, Booking booking, Runnable onSuccess) {
        if (booking == null) return;

        if (!garage.canCancelOrDeleteBooking(booking.getId())) {
            ActionDialogs.showError(I18n.get("dialog.booking.delete.title"),
                    I18n.get("dialog.booking.delete.has_active_orders", booking.getId()));
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(I18n.get("dialog.booking.delete.title"));
        confirm.setHeaderText(I18n.get("dialog.booking.delete.header"));
        confirm.setContentText(I18n.get("dialog.booking.delete.confirm", booking.getId()));
        ActionDialogs.styleDialog(confirm);

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                try {
                    garage.deleteBooking(booking.getId());
                } catch (Exception e) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    /**
     * Kontrollerar om en specifik timme är upptagen för en mekaniker ett visst datum.
     */
    public static boolean isHourBooked(GarageSystem garage, Mechanic mechanic, LocalDate date, int hour, int excludeBookingId) {
        if (mechanic == null || date == null) {
            return false;
        }

        LocalTime slotStart = LocalTime.of(hour, 0);
        LocalTime slotEnd = slotStart.plusHours(1);

        // 1. Kontrollera mot schemat (MechanicSchedule)
        MechanicSchedule schedule = MechanicSchedule.getInstance();
        List<MechanicSchedule.TimeSlot> slots = schedule.getSlotsForDay(mechanic.getId(), date);
        for (MechanicSchedule.TimeSlot s : slots) {
            if (s.getHour() == hour && s.isBooked()) {
                if (excludeBookingId > 0 && s.getBookingId() == excludeBookingId) {
                    continue;
                }
                return true;
            }
        }

        // 2. Kontrollera mot sparade bokningar i GarageSystem
        if (garage != null) {
            for (Booking b : garage.getBookings()) {
                if (excludeBookingId > 0 && b.getId() == excludeBookingId) {
                    continue;
                }
                if ("CANCELLED".equalsIgnoreCase(b.getStatus())) {
                    continue;
                }
                if (b.getMechanicId() == mechanic.getId() && date.equals(b.getDate())) {
                    if (b.getStartTime() != null) {
                        LocalTime bStart = b.getStartTime();
                        LocalTime bEnd = b.getEndTime() != null ? b.getEndTime() : bStart.plusHours(1);
                        if (bEnd.isBefore(bStart) || bEnd.equals(bStart)) {
                            bEnd = bStart.plusHours(1);
                        }
                        if (slotStart.isBefore(bEnd) && bStart.isBefore(slotEnd)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Anpassad ListCell för starttider: grön för ledig tid, röd för upptagen tid.
     * Inaktiverar automatiskt upptagna tider i popup-listan så att dubbelbokning förhindras.
     */
    private static class TimeSlotCell extends ListCell<LocalTime> {
        private final java.util.function.Function<LocalTime, Boolean> isBusyFunc;
        private final boolean isDropdownItem;

        public TimeSlotCell(java.util.function.Function<LocalTime, Boolean> isBusyFunc, boolean isDropdownItem) {
            this.isBusyFunc = isBusyFunc;
            this.isDropdownItem = isDropdownItem;
        }

        @Override
        protected void updateItem(LocalTime time, boolean empty) {
            super.updateItem(time, empty);
            if (empty || time == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
                setDisable(false);
            } else {
                boolean busy = isBusyFunc != null && Boolean.TRUE.equals(isBusyFunc.apply(time));
                String timeStr = time.format(TIME_FMT);
                String statusText = busy ? I18n.get("dialog.booking.time_busy") : I18n.get("dialog.booking.time_available");

                Circle dot = new Circle(4);
                dot.setFill(Color.web(busy ? "#f87171" : "#22c55e"));

                Label textLabel = new Label(timeStr + "  (" + statusText + ")");
                textLabel.setStyle(busy
                        ? "-fx-text-fill: #f87171; -fx-font-weight: bold;"
                        : "-fx-text-fill: #22c55e; -fx-font-weight: bold;");

                HBox box = new HBox(8, dot, textLabel);
                box.setAlignment(Pos.CENTER_LEFT);

                setGraphic(box);
                setText(null);

                if (isDropdownItem) {
                    setDisable(busy);
                    if (busy) {
                        setStyle("-fx-opacity: 0.60; -fx-background-color: rgba(248, 113, 113, 0.12);");
                    } else {
                        setStyle("-fx-opacity: 1.0; -fx-background-color: rgba(34, 197, 94, 0.08);");
                    }
                } else {
                    setDisable(false);
                    setStyle("");
                }
            }
        }
    }
}
