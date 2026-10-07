package com.wac.autocore.ui.components;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.seed.SeedText;
import com.wac.autocore.ui.ActionDialogs;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.BookingAvailability;

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
import java.util.List;

/**
 * Formuläret för att skapa och ändra en bokning.
 */
public class BookingFormPane extends GridPane {

    private final ComboBox<Vehicle> vehicleBox;
    private final DatePicker datePicker;
    private final BookingScheduleField scheduleField;
    private MultiSelectComboBox<ServiceItem> serviceMulti;
    private final ObservableList<ServiceItem> selectedServices = FXCollections.observableArrayList();
    private final BookingServicesField servicesField;
    private final BookingPackagePicker packagePicker;
    private final Label totalSummaryLabel;
    private final ComboBox<Mechanic> mechanicBox;
    private MultiSelectComboBox<Mechanic> mechanicMulti;
    private final Label mechanicFilterHint;
    private final ComboBox<LocalTime> startTimeBox;
    private final Label durationLabel;
    private final TextField descField;
    private final ComboBox<String> statusBox;

    /**
     * Drop-in: tiden väljs inte, den räknas fram och visas innan bokningen godkänns.
     */
    private final Label dropInTimeLabel = new Label();
    private final ObjectProperty<LocalTime> dropInStart = new SimpleObjectProperty<LocalTime>();
    private final boolean dropIn;

    private final int excludeId;

    /**
     * Riktig post i stället för null, som får JavaFX att kasta när den väljs.
     */
    static final Mechanic NO_MECHANIC = new Mechanic(0, "", "", "");

    private Runnable onContentGrown;

    public BookingFormPane(GarageSystem garage, Booking existingBooking,
                           LocalDate initialDate, Mechanic defaultMechanic, Integer defaultHour) {
        this(garage, existingBooking, initialDate, defaultMechanic, defaultHour, false);
    }

    /**
     * dropIn: samma formulär utan Datum & Tid. Bokningen sker när kunden kommer in, så tiden
     * sätts av tjänsten i stället, och beskrivningen behåller drop-in-texten i stället för
     * tjänstenamnen — annars går en drop-in inte att skilja från en vanlig bokning i listan.
     */
    public BookingFormPane(GarageSystem garage, Booking existingBooking,
                           LocalDate initialDate, Mechanic defaultMechanic, Integer defaultHour,
                           boolean dropIn) {
        this.excludeId = existingBooking != null ? existingBooking.getId() : 0;
        this.dropIn = dropIn;
        this.dropInTimeLabel.setWrapText(true);
        final boolean isServicesLocked = existingBooking != null && existingBooking.isWorkStarted();

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
        this.totalSummaryLabel = servicesField.getSummary();
        this.serviceMulti = new BookingServicePicker(garage, existingBooking, selectedServices, isServicesLocked).getMulti();
        this.packagePicker = new BookingPackagePicker(garage, serviceMulti, isServicesLocked);

        BookingMechanicsField mechanics = new BookingMechanicsField(garage, this);
        this.mechanicFilterHint = mechanics.getHint();
        this.mechanicBox = mechanics.getBox();
        this.mechanicMulti = mechanics.getMulti();

        // Teamet bestämmer drop-in-tiden, så en ändring i teamet räknar om den.
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

        // Listan av valda tjänster styr sammanfattningen, mekanikerfiltret och schemat.
        selectedServices.addListener((ListChangeListener<ServiceItem>) c -> {
            servicesField.render();
            mechanics.update();
            scheduleField.refresh();
            refreshDropInTime(garage);
            // En ny bokning får tjänsternas namn som beskrivning; i redigeringsläge
            // behålls den text som redan står där.
            if (existingBooking == null && !dropIn) {
                StringBuilder sb = new StringBuilder();
                for (ServiceItem s : selectedServices) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(SeedText.resolve(s.getName()));
                }
                descField.setText(sb.toString());
            }
        });

        // Initial körning
        servicesField.render();
        mechanics.update();
        scheduleField.refreshDate();

        // En drop-in känns igen på beskrivningen, så den står kvar och går inte att skriva över.
        // Fältet visar texten, men det som sparas är nyckeln, så den byter språk med resten.
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
                packagePicker.getBox(),
                serviceMulti, totalSummaryLabel,
                mechanicMulti, mechanicFilterHint,
                scheduleField,
                datePicker, startTimeBox, durationLabel,
                descField, statusBox, dropInTimeLabel,
                isServicesLocked);
    }

    // Tiden en drop-in får räknas fram ur teamets lediga timmar, inte ur något användaren väljer.
    // Den visas innan bokningen godkänns, och står det att det inte finns någon tid är OK stängt.
    private void refreshDropInTime(GarageSystem garage) {
        if (!dropIn) {
            return;
        }
        List<ServiceItem> services = new ArrayList<ServiceItem>(selectedServices);
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
     * Fordon, datum och antingen en beskrivning eller en tjänst. Ny bokning kräver en tjänst.
     */
    private boolean requiredFieldsFilled(int excludeBookingId) {
        boolean hasServices = !selectedServices.isEmpty();
        if (getSelectedVehicle() == null || getSelectedDate() == null) {
            return false;
        }
        if (!hasServices && getDescription().isEmpty()) {
            return false;
        }
        if (dropIn) {
            // En drop-in har ingen tid att fylla i. Finns ingen ledig timme finns inget att
            // godkänna, och då säger raden i formuläret varför OK är stängt.
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
     * Låser OK-knappen tills fälten är ifyllda, samma regel som validate().
     */
    public BooleanBinding requiredFieldsFilledBinding(final int excludeBookingId) {
        return Bindings.createBooleanBinding(
                () -> requiredFieldsFilled(excludeBookingId),
                this.vehicleBox.valueProperty(), this.datePicker.valueProperty(),
                this.startTimeBox.valueProperty(), this.dropInStart,
                this.descField.textProperty(), this.selectedServices);
    }

    public boolean validate(GarageSystem garage, int excludeBookingId) {
        // En ny bokning måste innehålla minst en tjänst. Vid en redigering gäller inte
        // kravet, eftersom en bokning som redan finns får behålla sina tjänster.
        if (!requiredFieldsFilled(excludeBookingId)) {
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
            return false;
        }

        LocalDate date = getSelectedDate();
        Mechanic chosenMech = getSelectedMechanic();
        LocalTime startTime = getSelectedStartTime();
        int minutes = getTotalEstimatedMinutes() > 0 ? getTotalEstimatedMinutes() : 60;

        // Spärr mot stängningstid (17:00): ett jobb som slutar efter stängning får inte bokas.
        // En drop-in har ingen vald tid — den räknas fram av tjänsten.
        if (startTime != null && !dropIn) {
            LocalTime endTime = startTime.plusMinutes(minutes);
            if (endTime.isAfter(BookingAvailability.CLOSING_TIME)) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.booking.closing_time_exceeded"));
                return false;
            }
        }

        // Behörighetskontroll: varje vald tjänst måste ha minst en behörig mekaniker i teamet
        List<Mechanic> team = getSelectedMechanics();
        for (ServiceItem s : selectedServices) {
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

        // Hela bokningens intervall kontrolleras mot de behövliga mekanikernas upptagna tider.
        // Drop-in undantas: dess tid är redan vald ur teamets lediga timmar.
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

    public List<ServiceItem> getSelectedServices() {
        return new ArrayList<ServiceItem>(selectedServices);
    }

    // Borttagningen måste gå via fältet, getSelectedServices lämnar en kopia av listan.
    void removeService(ServiceItem item) {
        if (serviceMulti != null) {
            serviceMulti.removeSelectedItem(item);
        } else {
            selectedServices.remove(item);
        }
    }

    /**
     * Första valda tjänsten, eller null.
     */
    public ServiceItem getSelectedService() {
        return selectedServices.isEmpty() ? null : selectedServices.get(0);
    }

    public int getTotalEstimatedMinutes() {
        int total = 0;
        for (ServiceItem s : selectedServices) {
            if (s != null) total += s.getEstimatedMinutes();
        }
        return total;
    }

    public Mechanic getSelectedMechanic() {
        List<Mechanic> chosen = getSelectedMechanics();
        return chosen.isEmpty() ? null : chosen.get(0);
    }

    /**
     * Mekanikerna i den ordning de valdes.
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
     * Kör lyssnaren som låter dialogen växa med innehållet.
     */
    void contentGrown() {
        if (onContentGrown != null) {
            onContentGrown.run();
        }
    }

    /**
     * Låter dialogen veta när formuläret behöver mer plats, så att fönstret kan växa med det.
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
