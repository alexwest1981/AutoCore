package com.wac.autocore.ui.components;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.ActionDialogs;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.BookingAvailability;
import com.wac.autocore.ui.util.EntityLookup;
import com.wac.autocore.ui.util.UiFormatters;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Återanvändbar formulärpanel för bokningar (används i både skapa- och redigeringsdialoger).
 * Ansvarar för UI-komponenter, händelselyssnare och validering.
 */
public class BookingFormPane extends GridPane {

    private final ComboBox<Vehicle> vehicleBox;
    private final DatePicker datePicker;
    private final ComboBox<ServiceItem> serviceBox;
    private final ComboBox<Mechanic> mechanicBox;
    private final Label mechanicFilterHint;
    private final ComboBox<LocalTime> startTimeBox;
    private final Label durationLabel;
    private final TextField descField;
    private final ComboBox<String> statusBox;

    public BookingFormPane(GarageSystem garage, Booking existingBooking,
                           LocalDate initialDate, Mechanic defaultMechanic, Integer defaultHour) {
        setHgap(10);
        setVgap(10);
        setPadding(new Insets(14, 14, 14, 14));

        // 1. Fordon
        this.vehicleBox = new ComboBox<Vehicle>();
        this.vehicleBox.getItems().addAll(garage.getVehicles());
        setupComboBoxDisplay(this.vehicleBox, new StringConverter<Vehicle>() {
            @Override
            public String toString(Vehicle v) {
                if (v == null) return "";
                String owner = EntityLookup.customerName(garage, v.getCustomerId());
                return v.getId() + " - " + v.getRegistrationNumber() + " (" + v.getBrand() + " " + v.getModel() + ") · " + owner;
            }
            @Override
            public Vehicle fromString(String string) { return null; }
        });
        if (existingBooking != null) {
            for (Vehicle v : this.vehicleBox.getItems()) {
                if (v.getId() == existingBooking.getVehicleId()) {
                    this.vehicleBox.getSelectionModel().select(v);
                    break;
                }
            }
        } else {
            this.vehicleBox.getSelectionModel().selectFirst();
        }

        // 2. Datum
        LocalDate dateVal = initialDate != null ? initialDate : LocalDate.now().plusDays(1);
        this.datePicker = new DatePicker(dateVal);

        // 3. Tjänst
        this.serviceBox = new ComboBox<ServiceItem>();
        this.serviceBox.getItems().add(null);
        this.serviceBox.getItems().addAll(garage.getServiceItems());
        setupComboBoxDisplay(this.serviceBox, new StringConverter<ServiceItem>() {
            @Override
            public String toString(ServiceItem s) {
                if (s == null) return I18n.get("dialog.booking.no_service");
                return s.getName() + " · " + UiFormatters.formatMoney(s.getPrice()) + " (" + s.getEstimatedMinutes() + " min)";
            }
            @Override
            public ServiceItem fromString(String string) { return null; }
        });
        if (existingBooking != null && existingBooking.getServiceItemId() > 0) {
            for (ServiceItem s : this.serviceBox.getItems()) {
                if (s != null && s.getId() == existingBooking.getServiceItemId()) {
                    this.serviceBox.getSelectionModel().select(s);
                    break;
                }
            }
        } else {
            this.serviceBox.getSelectionModel().selectFirst();
        }

        // 4. Mekaniker med dynamiskt kvalifikationsfilter
        this.mechanicFilterHint = new Label();
        this.mechanicFilterHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -wac-muted;");

        this.mechanicBox = new ComboBox<Mechanic>();
        setupComboBoxDisplay(this.mechanicBox, new StringConverter<Mechanic>() {
            @Override
            public String toString(Mechanic m) {
                if (m == null) return I18n.get("dialog.booking.no_mechanic");
                return m.getName() + " (" + m.getSpecialization() + ")";
            }
            @Override
            public Mechanic fromString(String string) { return null; }
        });

        Consumer<ServiceItem> updateMechanics = selService -> {
            List<Mechanic> qualified = garage.getQualifiedMechanics(selService);
            Mechanic currentSel = mechanicBox.getValue();

            mechanicBox.getItems().clear();
            mechanicBox.getItems().add(null);
            mechanicBox.getItems().addAll(qualified);

            if (selService == null) {
                mechanicFilterHint.setText("");
            } else if (qualified.isEmpty()) {
                mechanicFilterHint.setText(I18n.get("dialog.booking.no_mechanic_for_spec"));
                mechanicFilterHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #f87171;");
            } else {
                mechanicFilterHint.setText(I18n.get("dialog.booking.mechanics_filtered_for_spec", qualified.size()));
                mechanicFilterHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -wac-accent;");
            }

            int targetId = currentSel != null ? currentSel.getId()
                    : (existingBooking != null ? existingBooking.getMechanicId()
                    : (defaultMechanic != null ? defaultMechanic.getId() : 0));

            if (targetId > 0) {
                boolean found = false;
                for (Mechanic m : qualified) {
                    if (m.getId() == targetId) {
                        mechanicBox.getSelectionModel().select(m);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    mechanicBox.getSelectionModel().selectFirst();
                }
            } else {
                mechanicBox.getSelectionModel().selectFirst();
            }
        };
        updateMechanics.accept(this.serviceBox.getValue());

        // 5. Starttid med tillgänglighetsindikering (grön/röd)
        List<LocalTime> timeOptions = new ArrayList<LocalTime>();
        for (int h = 7; h <= 16; h++) {
            timeOptions.add(LocalTime.of(h, 0));
        }
        this.startTimeBox = new ComboBox<LocalTime>();
        this.startTimeBox.setItems(FXCollections.observableArrayList(timeOptions));

        int excludeId = existingBooking != null ? existingBooking.getId() : 0;
        Function<LocalTime, Boolean> isBusyFunc = time -> {
            if (time == null) return false;
            Mechanic m = mechanicBox.getValue();
            LocalDate d = datePicker.getValue();
            return BookingAvailability.isHourBooked(garage, m, d, time.getHour(), excludeId);
        };

        this.startTimeBox.setCellFactory(lv -> new TimeSlotCell(isBusyFunc, true));
        this.startTimeBox.setButtonCell(new TimeSlotCell(isBusyFunc, false));
        this.startTimeBox.setConverter(new StringConverter<LocalTime>() {
            @Override
            public String toString(LocalTime t) {
                return t == null ? "" : t.format(TimeSlotCell.TIME_FMT);
            }
            @Override
            public LocalTime fromString(String string) { return null; }
        });

        if (existingBooking != null && existingBooking.getStartTime() != null) {
            this.startTimeBox.getSelectionModel().select(existingBooking.getStartTime());
        } else if (defaultHour != null && defaultHour >= 7 && defaultHour <= 16) {
            this.startTimeBox.getSelectionModel().select(LocalTime.of(defaultHour, 0));
        } else {
            selectFirstAvailableTime(timeOptions, isBusyFunc);
        }

        // 6. Dynamisk sluttidsberäkning
        this.durationLabel = new Label();
        this.durationLabel.setStyle("-fx-text-fill: -wac-accent; -fx-font-weight: bold;");

        Runnable updateDuration = () -> {
            LocalTime start = startTimeBox.getValue();
            ServiceItem selService = serviceBox.getValue();
            if (start != null && selService != null) {
                LocalTime end = start.plusMinutes(selService.getEstimatedMinutes());
                durationLabel.setText(I18n.get("dialog.booking.time_window", start.format(TimeSlotCell.TIME_FMT), end.format(TimeSlotCell.TIME_FMT))
                        + " (" + selService.getEstimatedMinutes() + " min)");
            } else if (start != null) {
                LocalTime end = start.plusHours(1);
                durationLabel.setText(I18n.get("dialog.booking.time_window", start.format(TimeSlotCell.TIME_FMT), end.format(TimeSlotCell.TIME_FMT)) + " (60 min)");
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
                selectFirstAvailableTime(timeOptions, isBusyFunc);
            }
            if (startTimeBox.getButtonCell() != null) {
                startTimeBox.getButtonCell().updateIndex(-1);
            }
            updateDuration.run();
        };

        this.datePicker.valueProperty().addListener((obs, o, n) -> refreshTimeBox.run());
        this.mechanicBox.valueProperty().addListener((obs, o, n) -> refreshTimeBox.run());
        this.startTimeBox.valueProperty().addListener((obs, o, n) -> updateDuration.run());
        this.serviceBox.valueProperty().addListener((obs, o, n) -> {
            updateMechanics.accept(n);
            updateDuration.run();
        });
        updateDuration.run();

        // 7. Beskrivning
        String initialDesc = existingBooking != null && existingBooking.getDescription() != null
                ? existingBooking.getDescription() : "";
        this.descField = new TextField(initialDesc);
        this.descField.setPromptText(I18n.get("dialog.booking.desc_prompt"));

        if (existingBooking == null) {
            this.serviceBox.valueProperty().addListener((obs, o, n) -> {
                if (n != null && descField.getText().trim().isEmpty()) {
                    descField.setText(n.getName() + (n.getDescription() != null && !n.getDescription().isEmpty() ? " - " + n.getDescription() : ""));
                }
            });
        }

        // 8. Status (endast vid redigering)
        if (existingBooking != null) {
            this.statusBox = new ComboBox<String>();
            this.statusBox.getItems().addAll("BOOKED", "CONFIRMED", "IN_PROGRESS", "COMPLETED", "CANCELLED");
            this.statusBox.getSelectionModel().select(existingBooking.getStatus() != null ? existingBooking.getStatus() : "BOOKED");
        } else {
            this.statusBox = null;
        }

        // Layout i Grid
        int rowIdx = 0;
        add(new Label(I18n.get("dialog.booking.vehicle_select") + ":"), 0, rowIdx);
        add(this.vehicleBox, 1, rowIdx++);

        add(new Label(I18n.get("dialog.booking.date") + ":"), 0, rowIdx);
        add(this.datePicker, 1, rowIdx++);

        add(new Label(I18n.get("dialog.booking.service_select") + ":"), 0, rowIdx);
        add(this.serviceBox, 1, rowIdx++);

        add(new Label(I18n.get("dialog.booking.mechanic_select") + ":"), 0, rowIdx);
        VBox mechCol = new VBox(4, this.mechanicBox, this.mechanicFilterHint);
        add(mechCol, 1, rowIdx++);

        add(new Label(I18n.get("dialog.booking.time_select") + ":"), 0, rowIdx);
        add(this.startTimeBox, 1, rowIdx++);

        add(new Label(""), 0, rowIdx);
        add(this.durationLabel, 1, rowIdx++);

        add(new Label(I18n.get("table.col.description") + ":"), 0, rowIdx);
        add(this.descField, 1, rowIdx++);

        if (this.statusBox != null) {
            add(new Label(I18n.get("table.col.status") + ":"), 0, rowIdx);
            add(this.statusBox, 1, rowIdx++);
        }
    }

    private void selectFirstAvailableTime(List<LocalTime> timeOptions, Function<LocalTime, Boolean> isBusyFunc) {
        LocalTime firstFree = null;
        for (LocalTime t : timeOptions) {
            if (!Boolean.TRUE.equals(isBusyFunc.apply(t))) {
                firstFree = t;
                break;
            }
        }
        startTimeBox.getSelectionModel().select(firstFree != null ? firstFree : LocalTime.of(8, 0));
    }

    public boolean validate(GarageSystem garage, int excludeBookingId) {
        Vehicle v = getSelectedVehicle();
        LocalDate date = getSelectedDate();
        String desc = getDescription();
        ServiceItem chosenService = getSelectedService();
        if (desc.isEmpty() && chosenService != null) {
            desc = chosenService.getName();
        }

        if (v == null || date == null || desc.isEmpty()) {
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
            return false;
        }

        Mechanic chosenMech = getSelectedMechanic();
        LocalTime startTime = getSelectedStartTime();
        if (chosenMech != null && startTime != null && BookingAvailability.isHourBooked(garage, chosenMech, date, startTime.getHour(), excludeBookingId)) {
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.booking.slot_busy_error"));
            return false;
        }

        return true;
    }

    public Vehicle getSelectedVehicle() { return vehicleBox.getValue(); }
    public LocalDate getSelectedDate() { return datePicker.getValue(); }
    public ServiceItem getSelectedService() { return serviceBox.getValue(); }
    public Mechanic getSelectedMechanic() { return mechanicBox.getValue(); }
    public LocalTime getSelectedStartTime() { return startTimeBox.getValue(); }
    public String getDescription() { return descField.getText().trim(); }
    public String getStatus() { return statusBox != null ? statusBox.getValue() : "BOOKED"; }

    private static <T> void setupComboBoxDisplay(ComboBox<T> box, StringConverter<T> converter) {
        box.setConverter(converter);
        box.setCellFactory(lv -> new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(converter.toString(item));
                    setGraphic(null);
                }
            }
        });
        box.setButtonCell(new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(converter.toString(item));
                    setGraphic(null);
                }
            }
        });
    }
}
