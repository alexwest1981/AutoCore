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
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import com.wac.autocore.seed.SeedText;

/**
 * Återanvändbar formulärpanel för bokningar (används i både skapa- och redigeringsdialoger).
 * Ansvarar för UI-komponenter, händelselyssnare och validering.
 */
public class BookingFormPane extends GridPane {

    private final ComboBox<Vehicle> vehicleBox;
    private final DatePicker datePicker;
    private final ComboBox<ServiceItem> serviceBox;
    private final ObservableList<ServiceItem> selectedServices = FXCollections.observableArrayList();
    private final VBox selectedServicesContainer = new VBox(4);
    private final Label totalSummaryLabel = new Label();
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

        // 3. Tjänster (stöd för flera val i samma bokning)
        this.serviceBox = new ComboBox<ServiceItem>();
        this.serviceBox.getItems().addAll(garage.getServiceItems());
        this.serviceBox.setMaxWidth(Double.MAX_VALUE);
        setupComboBoxDisplay(this.serviceBox, new StringConverter<ServiceItem>() {
            @Override
            public String toString(ServiceItem s) {
                if (s == null) return I18n.get("dialog.booking.no_service");
                return SeedText.resolve(s.getName()) + " · " + UiFormatters.formatMoney(s.getPrice()) + " (" + s.getEstimatedMinutes() + " min)";
            }
            @Override
            public ServiceItem fromString(String string) { return null; }
        });
        if (!this.serviceBox.getItems().isEmpty()) {
            this.serviceBox.getSelectionModel().selectFirst();
        }

        Button addServiceBtn = new Button("+ " + I18n.get("dialog.booking.add_service"));
        addServiceBtn.setStyle("-fx-cursor: hand; -fx-padding: 4 10; -fx-font-weight: bold;");

        final boolean isServicesLocked = existingBooking != null && existingBooking.isWorkStarted();
        if (isServicesLocked) {
            this.serviceBox.setDisable(true);
            addServiceBtn.setDisable(true);
        }

        if (existingBooking != null) {
            if (existingBooking.getServiceItems() != null && !existingBooking.getServiceItems().isEmpty()) {
                this.selectedServices.addAll(existingBooking.getServiceItems());
            } else if (existingBooking.getServiceItemIds() != null && !existingBooking.getServiceItemIds().isEmpty()) {
                for (int sid : existingBooking.getServiceItemIds()) {
                    for (ServiceItem s : garage.getServiceItems()) {
                        if (s.getId() == sid) {
                            this.selectedServices.add(s);
                            break;
                        }
                    }
                }
            } else if (existingBooking.getServiceItemId() > 0) {
                for (ServiceItem s : garage.getServiceItems()) {
                    if (s.getId() == existingBooking.getServiceItemId()) {
                        this.selectedServices.add(s);
                        break;
                    }
                }
            }
        }

        Runnable renderServices = () -> {
            selectedServicesContainer.getChildren().clear();
            if (selectedServices.isEmpty()) {
                Label emptyLbl = new Label(I18n.get("dialog.booking.no_services_selected"));
                emptyLbl.setStyle("-fx-text-fill: -wac-muted; -fx-font-size: 11px; -fx-font-style: italic; -fx-padding: 2 0;");
                selectedServicesContainer.getChildren().add(emptyLbl);
                totalSummaryLabel.setText("");
            } else {
                int totalMin = 0;
                double totalCost = 0.0;
                for (ServiceItem item : selectedServices) {
                    totalMin += item.getEstimatedMinutes();
                    totalCost += item.getPrice();

                    HBox row = new HBox(8);
                    row.setStyle("-fx-background-color: -wac-card; -fx-border-color: -wac-line; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 4 8; -fx-alignment: center-left;");

                    Label nameLbl = new Label(SeedText.resolve(item.getName()));
                    nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: -wac-text; -fx-font-size: 12px;");

                    Label detailLbl = new Label(UiFormatters.formatMoney(item.getPrice()) + " · " + item.getEstimatedMinutes() + " min");
                    detailLbl.setStyle("-fx-text-fill: -wac-muted; -fx-font-size: 11px;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    if (!isServicesLocked) {
                        Button removeBtn = new Button("✕");
                        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f87171; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 0 4; -fx-font-weight: bold;");
                        removeBtn.setOnAction(ev -> selectedServices.remove(item));
                        row.getChildren().addAll(nameLbl, detailLbl, spacer, removeBtn);
                    } else {
                        row.getChildren().addAll(nameLbl, detailLbl, spacer);
                    }
                    selectedServicesContainer.getChildren().add(row);
                }
                totalSummaryLabel.setText(I18n.get("dialog.booking.total_time", totalMin) + "  |  " + I18n.get("dialog.booking.total_price", UiFormatters.formatMoney(totalCost)));
                totalSummaryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -wac-accent; -fx-font-size: 12px; -fx-padding: 2 0 0 2;");
            }
        };

        addServiceBtn.setOnAction(e -> {
            if (isServicesLocked) return;
            ServiceItem sel = serviceBox.getValue();
            if (sel != null && !selectedServices.contains(sel)) {
                selectedServices.add(sel);
            }
        });

        // 4. Mekaniker med dynamiskt kvalifikationsfilter
        this.mechanicFilterHint = new Label();
        this.mechanicFilterHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -wac-muted;");

        this.mechanicBox = new ComboBox<Mechanic>();
        setupComboBoxDisplay(this.mechanicBox, new StringConverter<Mechanic>() {
            @Override
            public String toString(Mechanic m) {
                if (m == null) return I18n.get("dialog.booking.no_mechanic");
                return m.getName() + " (" + SeedText.resolve(m.getSpecialization()) + ")";
            }
            @Override
            public Mechanic fromString(String string) { return null; }
        });

        Consumer<ServiceItem> updateMechanics = selService -> {
            List<Mechanic> qualified = selService != null ? garage.getQualifiedMechanics(selService) : garage.getMechanics();
            Mechanic currentSel = mechanicBox.getValue();

            mechanicBox.getItems().clear();
            mechanicBox.getItems().add(null);
            mechanicBox.getItems().addAll(qualified);

            if (selService == null && selectedServices.isEmpty()) {
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
        updateMechanics.accept(getSelectedService());

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
            int totalMin = getTotalEstimatedMinutes();
            if (start != null && totalMin > 0) {
                LocalTime end = start.plusMinutes(totalMin);
                durationLabel.setText(I18n.get("dialog.booking.time_window", start.format(TimeSlotCell.TIME_FMT), end.format(TimeSlotCell.TIME_FMT))
                        + " (" + totalMin + " min)");
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

        // 7. Beskrivning
        String initialDesc = existingBooking != null && existingBooking.getDescription() != null
                ? SeedText.resolve(existingBooking.getDescription()) : "";
        this.descField = new TextField(initialDesc);
        this.descField.setPromptText(I18n.get("dialog.booking.desc_prompt"));

        selectedServices.addListener((javafx.collections.ListChangeListener<ServiceItem>) c -> {
            renderServices.run();
            updateMechanics.accept(getSelectedService());
            updateDuration.run();
            if (existingBooking == null) {
                StringBuilder sb = new StringBuilder();
                for (ServiceItem s : selectedServices) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(SeedText.resolve(s.getName()));
                }
                descField.setText(sb.toString());
            }
        });
        renderServices.run();
        updateDuration.run();

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

        Label serviceLbl = new Label(I18n.get("dialog.booking.service_select") + ":");
        GridPane.setValignment(serviceLbl, VPos.TOP);
        serviceLbl.setPadding(new Insets(6, 0, 0, 0));
        add(serviceLbl, 0, rowIdx);
        HBox servicePickerRow = new HBox(8, this.serviceBox, addServiceBtn);
        HBox.setHgrow(this.serviceBox, Priority.ALWAYS);
        VBox serviceCol = new VBox(6);
        serviceCol.getChildren().add(servicePickerRow);
        if (isServicesLocked) {
            Label lockNotice = new Label("🔒 " + I18n.get("dialog.booking.services_locked_work_started"));
            lockNotice.setStyle("-fx-font-size: 11px; -fx-text-fill: #f87171; -fx-font-weight: bold;");
            serviceCol.getChildren().add(lockNotice);
        }
        serviceCol.getChildren().addAll(this.selectedServicesContainer, this.totalSummaryLabel);
        add(serviceCol, 1, rowIdx++);

        Label mechLbl = new Label(I18n.get("dialog.booking.mechanic_select") + ":");
        GridPane.setValignment(mechLbl, VPos.TOP);
        mechLbl.setPadding(new Insets(6, 0, 0, 0));
        add(mechLbl, 0, rowIdx);
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
        List<ServiceItem> chosenServices = getSelectedServices();
        if (desc.isEmpty() && !chosenServices.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ServiceItem s : chosenServices) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(SeedText.resolve(s.getName()));
            }
            desc = sb.toString();
        } else if (desc.isEmpty() && getSelectedService() != null) {
            desc = SeedText.resolve(getSelectedService().getName());
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
    public List<ServiceItem> getSelectedServices() { return new ArrayList<ServiceItem>(selectedServices); }
    public ServiceItem getSelectedService() { return selectedServices.isEmpty() ? serviceBox.getValue() : selectedServices.get(0); }
    public int getTotalEstimatedMinutes() {
        int total = 0;
        for (ServiceItem s : selectedServices) {
            if (s != null) total += s.getEstimatedMinutes();
        }
        return total;
    }
    public double getTotalEstimatedPrice() {
        double total = 0.0;
        for (ServiceItem s : selectedServices) {
            if (s != null) total += s.getPrice();
        }
        return total;
    }
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
