package com.wac.autocore.ui.components;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.seed.SeedText;
import com.wac.autocore.ui.ActionDialogs;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.BookingAvailability;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The form for creating and changing a booking.
 */
public class BookingFormPane extends GridPane {

    private final GarageSystem garage;
    private final Booking existingBooking;
    private final ComboBox<Vehicle> vehicleBox;
    private final DatePicker datePicker;
    private final BookingScheduleField scheduleField;
    private MultiSelectComboBox<ServiceItem> serviceMulti;
    private final ObservableList<ServiceItem> selectedServices = FXCollections.observableArrayList();
    private final BookingServicesField servicesField;
    private final BookingPackagePicker packagePicker;
    private final BookingMechanicsField mechanics;
    private final ComboBox<Mechanic> mechanicBox;
    private MultiSelectComboBox<Mechanic> mechanicMulti;
    private final Label mechanicFilterHint;
    private final ComboBox<LocalTime> startTimeBox;
    private final Label durationLabel;
    private final TextField descField;
    private final ComboBox<String> statusBox;

    /**
     * Drop-in: the time is not picked, it is worked out and shown before the booking is approved.
     */
    private final Label dropInTimeLabel = new Label();
    private final ObjectProperty<LocalTime> dropInStart = new SimpleObjectProperty<LocalTime>();
    private final boolean dropIn;

    private final int excludeId;

    /**
     * A real entry instead of null, which makes JavaFX throw when it is picked.
     */
    static final Mechanic NO_MECHANIC = new Mechanic(0, "", "", "");

    private Runnable onContentGrown;

    public BookingFormPane(GarageSystem garage, Booking existingBooking,
                           LocalDate initialDate, Mechanic defaultMechanic, Integer defaultHour) {
        this(garage, existingBooking, initialDate, defaultMechanic, defaultHour, false);
    }

    /**
     * dropIn: the same form without Date & Time. The booking happens as the customer walks in, so
     * the time is set by the service instead, and the description keeps the drop-in text rather
     * than the service names — otherwise a drop-in cannot be told from an ordinary booking in the list.
     */
    public BookingFormPane(GarageSystem garage, Booking existingBooking,
                           LocalDate initialDate, Mechanic defaultMechanic, Integer defaultHour,
                           boolean dropIn) {
        this(garage, existingBooking, initialDate, defaultMechanic, defaultHour, dropIn, false);
    }

    /**
     * forCopy: a new booking that inherits details from an old one. It should neither lock the
     * services nor count away the old one's time, because the new booking is a booking of its own.
     */
    public BookingFormPane(GarageSystem garage, Booking existingBooking,
                           LocalDate initialDate, Mechanic defaultMechanic, Integer defaultHour,
                           boolean dropIn, boolean forCopy) {
        this.garage = garage;
        this.existingBooking = existingBooking;
        this.excludeId = existingBooking != null && !forCopy ? existingBooking.getId() : 0;
        this.dropIn = dropIn;
        this.dropInTimeLabel.setWrapText(true);
        final boolean isServicesLocked = existingBooking != null && existingBooking.isWorkStarted() && !forCopy;

        setHgap(18);
        setVgap(16);
        setPadding(new Insets(18, 22, 18, 22));
        setPrefWidth(820);
        setMinWidth(700);

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setMinWidth(120);
        col0.setPrefWidth(130);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(col0, col1);

        this.vehicleBox = new BookingVehicleField(garage, existingBooking).getBox();
        this.servicesField = new BookingServicesField(this, isServicesLocked);
        this.serviceMulti = new BookingServicePicker(garage, existingBooking, selectedServices, isServicesLocked).getMulti();
        this.packagePicker = new BookingPackagePicker(garage, isServicesLocked);
        this.packagePicker.setOnChanged(this::packagePicked);

        this.mechanics = new BookingMechanicsField(garage, this);
        this.mechanicFilterHint = mechanics.getHint();
        this.mechanicBox = mechanics.getBox();
        this.mechanicMulti = mechanics.getMulti();

        // The team decides the drop-in time, so a change in the team works it out again.
        this.mechanicMulti.getSelectedItems().addListener(
                (ListChangeListener<Mechanic>) c -> refreshDropInTime(garage));

        this.scheduleField = new BookingScheduleField(garage, this, mechanics.getBox(), excludeId,
                existingBooking, initialDate);
        this.datePicker = scheduleField.getDatePicker();
        this.startTimeBox = scheduleField.getStartTimeBox();
        this.durationLabel = scheduleField.getDurationLabel();

        BookingStatusField statusField = new BookingStatusField(existingBooking);
        this.descField = statusField.getDesc();
        this.statusBox = statusField.getStatus();

        // The list of picked services drives the summary, the mechanic filter and the calendar.
        selectedServices.addListener((ListChangeListener<ServiceItem>) c -> servicesChanged());

        // First run
        servicesField.render();
        mechanics.update();
        scheduleField.refreshDate();

        // A drop-in is recognized by its description, so it stays and cannot be written over.
        // The field shows the text, but the key is what is stored, so it switches language with the rest.
        if (dropIn) {
            descField.setText(SeedText.resolve("seed.booking.drop_in.description"));
            descField.setEditable(false);
        }
        if (existingBooking != null && existingBooking.getStartTime() != null) {
            this.startTimeBox.getSelectionModel().select(existingBooking.getStartTime());
        } else if (defaultHour != null && defaultHour >= 7 && defaultHour <= 16) {
            this.startTimeBox.getSelectionModel().select(LocalTime.of(defaultHour, 0));
        }
        scheduleField.refreshTimes();
        refreshDropInTime(garage);

        new BookingFormLayout(this, dropIn).layout(
                vehicleBox,
                packagePicker.getMulti(),
                serviceMulti,
                mechanicMulti, mechanicFilterHint,
                scheduleField,
                datePicker, startTimeBox, durationLabel,
                descField, statusBox, dropInTimeLabel,
                servicesField,
                isServicesLocked);
    }

    // The time a drop-in gets is worked out from the team's free hours, not from anything the user
    // picks. It is shown before the booking is approved, and if it says there is no time, OK is locked.
    private void refreshDropInTime(GarageSystem garage) {
        if (!dropIn) {
            return;
        }
        List<ServiceItem> services = getSelectedServices();
        List<Mechanic> team = getSelectedMechanics();
        LocalTime start = services.isEmpty() || team.isEmpty()
                ? null : garage.dropInStartTime(services, team);

        dropInStart.set(start);
        if (start == null) {
            dropInTimeLabel.setText(I18n.get("dialog.booking.dropin_no_time"));
            dropInTimeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #f87171; -fx-font-weight: bold;");
            return;
        }
        LocalTime end = start.plusMinutes(garage.busyMinutes(services, team));
        dropInTimeLabel.setText(start + " – " + end);
        dropInTimeLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
    }

    /**
     * Vehicle, date and either a description or a service. A new booking requires a service.
     */
    private boolean requiredFieldsFilled(int excludeBookingId) {
        boolean hasServices = !getSelectedServices().isEmpty();
        if (getSelectedVehicle() == null || getSelectedDate() == null) {
            return false;
        }
        if (!hasServices && getDescription().isEmpty()) {
            return false;
        }
        if (dropIn) {
            // A drop-in has no time to fill in. With no free hour there is nothing to approve,
            // and then the row in the form says why OK is locked.
            return hasServices && dropInStart.get() != null;
        }
        LocalTime start = getSelectedStartTime();
        if (start != null) {
            int duration = getTotalEstimatedMinutes() > 0 ? getTotalEstimatedMinutes() : 60;
            if (start.plusMinutes(duration).isAfter(BookingAvailability.CLOSING_TIME)) {
                return false;
            }
        }
        return excludeBookingId != 0 || hasServices;
    }

    /**
     * Locks the OK button until the fields are filled in, the same rule as validate().
     */
    public BooleanBinding requiredFieldsFilledBinding(final int excludeBookingId) {
        return Bindings.createBooleanBinding(
                () -> requiredFieldsFilled(excludeBookingId),
                this.vehicleBox.valueProperty(), this.datePicker.valueProperty(),
                this.startTimeBox.valueProperty(), this.dropInStart,
                this.descField.textProperty(), this.selectedServices);
    }

    public boolean validate(GarageSystem garage, int excludeBookingId) {
        // A new booking must hold at least one service. In edit mode the requirement does not
        // apply, because a booking that already exists may keep its services.
        if (!requiredFieldsFilled(excludeBookingId)) {
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
            return false;
        }

        LocalDate date = getSelectedDate();
        Mechanic chosenMech = getSelectedMechanic();
        LocalTime startTime = getSelectedStartTime();
        int minutes = getTotalEstimatedMinutes() > 0 ? getTotalEstimatedMinutes() : 60;

        // Blocked against closing time (17:00): a job that ends after closing cannot be booked.
        // A drop-in has no picked time — it is worked out by the service.
        if (startTime != null && !dropIn) {
            LocalTime endTime = startTime.plusMinutes(minutes);
            if (endTime.isAfter(BookingAvailability.CLOSING_TIME)) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.booking.closing_time_exceeded"));
                return false;
            }
        }

        // Qualification check: every picked service needs at least one qualified mechanic in the team
        List<Mechanic> team = getSelectedMechanics();
        for (ServiceItem s : getSelectedServices()) {
            boolean hasQualified = false;
            for (Mechanic m : team) {
                if (garage.isMechanicQualified(m, s)) {
                    hasQualified = true;
                    break;
                }
            }
            if (!hasQualified && chosenMech != null && garage.isMechanicQualified(chosenMech, s)) {
                hasQualified = true;
            }
            if (!hasQualified) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"),
                        I18n.get("dialog.booking.no_mechanic_for_spec"));
                return false;
            }
        }

        // The whole booking's range is checked against the busy times of the required mechanics.
        // Drop-in is exempt: its time was already picked from the team's free hours.
        if (startTime != null && !dropIn) {
            if (BookingAvailability.isTeamBooked(garage, team, date, startTime,
                    startTime.plusMinutes(minutes), excludeBookingId)) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.booking.slot_busy_error"));
                return false;
            }
        }

        return true;
    }

    public Vehicle getSelectedVehicle() {
        return vehicleBox.getValue();
    }

    public LocalDate getSelectedDate() {
        return datePicker.getValue();
    }

    /**
     * Everything on the booking: the services the packages bring, then the ones picked on their own.
     * This is what gets saved and priced. The picker's own list holds only the latter.
     */
    public List<ServiceItem> getSelectedServices() {
        List<ServiceItem> all = new ArrayList<ServiceItem>();
        Set<Integer> seen = new HashSet<Integer>();
        for (ServiceItem item : servicesFromPackages()) {
            if (seen.add(item.getId())) {
                all.add(item);
            }
        }
        for (ServiceItem item : selectedServices) {
            if (seen.add(item.getId())) {
                all.add(item);
            }
        }
        return all;
    }

    /** Every service a picked package brings, without duplicates. */
    private List<ServiceItem> servicesFromPackages() {
        List<ServiceItem> items = new ArrayList<ServiceItem>();
        Set<Integer> seen = new HashSet<Integer>();
        for (ServicePackage pkg : packagePicker.getChosen()) {
            for (ServiceItem item : pkg.getServiceItems()) {
                if (seen.add(item.getId())) {
                    items.add(item);
                }
            }
        }
        return items;
    }

    /** A picked package puts its services on the booking, so they are blocked in the picker instead
     *  of sitting there as chips of their own. */
    private void packagePicked() {
        serviceMulti.setBlockedItems(servicesFromPackages());
        servicesChanged();
    }

    /** Redraws everything that follows from what is on the booking. */
    private void servicesChanged() {
        packagePicker.setServicesOnBooking(getSelectedServices());
        servicesField.render();
        mechanics.update();
        scheduleField.refresh();
        refreshDropInTime(garage);
        // A new booking gets the package and service names as its description; in edit mode the
        // text already there is kept.
        if (existingBooking == null && !dropIn) {
            StringBuilder sb = new StringBuilder();
            for (ServicePackage pkg : packagePicker.getChosen()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(SeedText.resolve(pkg.getName()));
            }
            for (ServiceItem s : getSelectedServices()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(SeedText.resolve(s.getName()));
            }
            descField.setText(sb.toString());
        }
    }

    /** The packages picked so far. Their services are in the list above; this is only used to group
     *  them, so the basket can tell a service from a package from one picked on its own. */
    List<ServicePackage> getChosenPackages() {
        return new ArrayList<ServicePackage>(packagePicker.getChosen());
    }

    /** The package name for each service that came from a package, by service id. The first package
     *  that brings a service owns it, so a service shared by two packages is named once. */
    public Map<Integer, String> getServicePackageNames() {
        Map<Integer, String> names = new HashMap<Integer, String>();
        for (ServicePackage pkg : packagePicker.getChosen()) {
            for (ServiceItem item : pkg.getServiceItems()) {
                if (!names.containsKey(Integer.valueOf(item.getId()))) {
                    names.put(Integer.valueOf(item.getId()), SeedText.resolve(pkg.getName()));
                }
            }
        }
        return names;
    }

    // The removal has to go through the field, getSelectedServices hands out a copy of the list.
    void removeService(ServiceItem item) {
        if (serviceMulti != null) {
            serviceMulti.removeSelectedItem(item);
        } else {
            selectedServices.remove(item);
        }
    }

    /** Drops a picked package. Its services are freed in the service picker again, and the list of
     *  what is on the booking is redrawn. */
    void removePackage(ServicePackage pkg) {
        packagePicker.remove(pkg);
    }

    /**
     * The first picked service, or null.
     */
    public ServiceItem getSelectedService() {
        List<ServiceItem> all = getSelectedServices();
        return all.isEmpty() ? null : all.get(0);
    }

    public int getTotalEstimatedMinutes() {
        int total = 0;
        for (ServiceItem s : getSelectedServices()) {
            if (s != null) total += s.getEstimatedMinutes();
        }
        return total;
    }

    public Mechanic getSelectedMechanic() {
        List<Mechanic> chosen = getSelectedMechanics();
        return chosen.isEmpty() ? null : chosen.get(0);
    }

    /**
     * The mechanics in the order they were picked.
     */
    public List<Mechanic> getSelectedMechanics() {
        List<Mechanic> ut = new ArrayList<Mechanic>();
        if (mechanicMulti != null) {
            for (Mechanic m : mechanicMulti.getSelectedItems()) {
                ut.add(m);
            }
        }
        if (ut.isEmpty() && mechanicBox.getValue() != null && mechanicBox.getValue() != NO_MECHANIC) {
            ut.add(mechanicBox.getValue());
        }
        return ut;
    }

    /**
     * Runs the listener that lets the dialog grow with the content.
     */
    void contentGrown() {
        if (onContentGrown != null) {
            onContentGrown.run();
        }
    }

    /**
     * Lets the dialog know when the form needs more room, so the window can grow with it.
     */
    public void setOnContentGrown(Runnable listener) {
        this.onContentGrown = listener;
    }

    public LocalTime getSelectedStartTime() {
        return startTimeBox.getValue();
    }

    public String getDescription() {
        return descField.getText().trim();
    }

    public String getStatus() {
        return statusBox != null ? statusBox.getValue() : "BOOKED";
    }

    static <T> void setupComboBoxDisplay(ComboBox<T> box, StringConverter<T> converter) {
        box.setConverter(converter);
        box.setCellFactory(lv -> new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_LEFT);
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
                setAlignment(Pos.CENTER_LEFT);
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
