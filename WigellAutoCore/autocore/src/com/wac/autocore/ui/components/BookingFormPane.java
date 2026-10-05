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

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import com.wac.autocore.seed.SeedText;

/** Formuläret för att skapa och ändra en bokning. */
public class BookingFormPane extends GridPane {

    private final ComboBox<Vehicle> vehicleBox;
    private final DatePicker datePicker;
    private final BookingScheduleField scheduleField;
    private final ComboBox<ServiceItem> serviceBox;
    private MultiSelectComboBox<ServiceItem> serviceMulti;
    private final ObservableList<ServiceItem> selectedServices = FXCollections.observableArrayList();
    private final VBox selectedServicesContainer;
    private final BookingServicesField servicesField;
    private final ScrollPane servicesScroll;
    private final Label totalSummaryLabel;
    private final ComboBox<Mechanic> mechanicBox;
    private MultiSelectComboBox<Mechanic> mechanicMulti;
    private final Label mechanicFilterHint;
    private final ComboBox<LocalTime> startTimeBox;
    private final Label durationLabel;
    private final TextField descField;
    private final ComboBox<String> statusBox;

    private final int excludeId;
    private final GarageSystem garage;

/** Riktig post i stället för null, som får JavaFX att kasta när den väljs. */
    static final Mechanic NO_MECHANIC = new Mechanic(0, "", "", "");

    private Runnable onContentGrown;

    public BookingFormPane(GarageSystem garage, Booking existingBooking,
                           LocalDate initialDate, Mechanic defaultMechanic, Integer defaultHour) {
        this.garage = garage;
        setHgap(18);
        setVgap(16);
        setPadding(new Insets(18, 22, 18, 22));
        setPrefWidth(820);
        setMinWidth(700);

        javafx.scene.layout.ColumnConstraints col0 = new javafx.scene.layout.ColumnConstraints();
        col0.setMinWidth(120);
        col0.setPrefWidth(130);
        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(col0, col1);



        this.vehicleBox = new ComboBox<Vehicle>();
        this.vehicleBox.getItems().addAll(garage.getVehicles());
        this.vehicleBox.setMaxWidth(Double.MAX_VALUE);
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
        this.serviceBox.setCellFactory(lv -> serviceChoiceCell());

        Button addServiceBtn = new Button("+ " + I18n.get("dialog.booking.add_service"));
        addServiceBtn.setMaxHeight(Double.MAX_VALUE);
        addServiceBtn.getStyleClass().addAll("primary", "primary-button", "add-service-btn");
        final String baseStyle = "-fx-cursor: hand; -fx-background-color: -wac-accent; -fx-text-fill: -wac-on-accent; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-padding: 7px 14px; -fx-alignment: center;";
        final String hoverStyle = "-fx-cursor: hand; -fx-background-color: -wac-accent-hover; -fx-text-fill: -wac-on-accent; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-padding: 7px 14px; -fx-alignment: center;";
        addServiceBtn.setStyle(baseStyle);
        addServiceBtn.setOnMouseEntered(ev -> {
            if (!addServiceBtn.isDisabled()) addServiceBtn.setStyle(hoverStyle);
        });
        addServiceBtn.setOnMouseExited(ev -> {
            if (!addServiceBtn.isDisabled()) addServiceBtn.setStyle(baseStyle);
        });

        final boolean isServicesLocked = existingBooking != null && existingBooking.isWorkStarted();
        if (isServicesLocked) {
            this.serviceBox.setDisable(true);
            addServiceBtn.setDisable(true);
            addServiceBtn.setStyle(baseStyle + " -fx-opacity: 0.5;");
        }

        this.servicesField = new BookingServicesField(this, isServicesLocked);
        this.selectedServicesContainer = servicesField.getContainer();
        this.servicesScroll = servicesField.getScroll();
        this.totalSummaryLabel = servicesField.getSummary();

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


        addServiceBtn.setOnAction(e -> {
            if (isServicesLocked) return;
            ServiceItem sel = serviceBox.getValue();
            if (sel != null && selectedServices.stream().noneMatch(s -> s.getId() == sel.getId())) {
                selectedServices.add(sel);
            }
        });

        // 3. Mekaniker med dynamiskt kvalifikationsfilter för samtliga valda tjänster

// 4. Mekaniker med dynamiskt kvalifikationsfilter
        // Fältets val speglas mot formulärets selectedServices, som summor och sparande läser.
        this.serviceMulti = new MultiSelectComboBox<ServiceItem>(
                I18n.get("dialog.booking.service_select"),
                s -> SeedText.resolve(s.getName()),
                s -> UiFormatters.formatMoney(s.getPrice()) + " · " + s.getEstimatedMinutes() + " min");
        this.serviceMulti.setItems(garage.getServiceItems());
        this.serviceMulti.setKeyProvider(s -> s.getId());
        // En bokning som redan har tjänster visar dem som valda chips, även i redigeringsläge.
        this.serviceMulti.setSelectedItems(new java.util.ArrayList<ServiceItem>(this.selectedServices));
        this.serviceMulti.getSelectedItems().addListener(
                (javafx.collections.ListChangeListener<ServiceItem>) c -> {
                    this.selectedServices.setAll(this.serviceMulti.getSelectedItems());
                    servicesField.render();
                });
        if (isServicesLocked) {
            this.serviceMulti.setDisable(true);
        }
        BookingMechanicsField mechanics = new BookingMechanicsField(garage, this);
        this.mechanicFilterHint = mechanics.getHint();
        this.mechanicBox = mechanics.getBox();
        this.mechanicMulti = mechanics.getMulti();

        this.excludeId = existingBooking != null ? existingBooking.getId() : 0;

        this.scheduleField = new BookingScheduleField(garage, this, mechanics.getBox(), excludeId,
                existingBooking, initialDate);
        this.datePicker = scheduleField.getDatePicker();
        this.startTimeBox = scheduleField.getStartTimeBox();
        this.durationLabel = scheduleField.getDurationLabel();

        // 7. Beskrivning
        String initialDesc = existingBooking != null && existingBooking.getDescription() != null
                ? SeedText.resolve(existingBooking.getDescription()) : "";
        this.descField = new TextField(initialDesc);
        this.descField.setPromptText(I18n.get("dialog.booking.desc_prompt"));

        selectedServices.addListener((javafx.collections.ListChangeListener<ServiceItem>) c -> {
            servicesField.render();
            // Gråmarkeringen i tjänstelistan följer bokningens innehåll: cellerna byggs om när
            // listan ändras, annars står en redan tillagd tjänst kvar som valbar.
            serviceBox.setCellFactory(lv -> serviceChoiceCell());
            mechanics.update();
            scheduleField.refresh();
            scheduleField.refresh();
            if (existingBooking == null) {
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
        if (existingBooking != null && existingBooking.getStartTime() != null) {
            this.startTimeBox.getSelectionModel().select(existingBooking.getStartTime());
        } else if (defaultHour != null && defaultHour >= 7 && defaultHour <= 16) {
            this.startTimeBox.getSelectionModel().select(LocalTime.of(defaultHour, 0));
        }
        scheduleField.refreshTimes();

        // 8. Status (endast vid redigering)
        if (existingBooking != null) {
            this.statusBox = new ComboBox<String>();
            if (existingBooking.isWorkStarted()) {
                // Arbetet är påbörjat eller en arbetsorder finns. Då får statusen inte gå
                // tillbaka till Bokad eller Bekräftad, och bokningen får inte avbokas,
                // för då öppnas låset på tjänsterna igen.
                this.statusBox.getItems().addAll("IN_PROGRESS", "COMPLETED");
                if (existingBooking.getStatus() != null
                        && !this.statusBox.getItems().contains(existingBooking.getStatus())) {
                    this.statusBox.getItems().add(existingBooking.getStatus());
                }
            } else {
                this.statusBox.getItems().addAll("BOOKED", "CONFIRMED", "IN_PROGRESS", "COMPLETED", "CANCELLED");
            }
            this.statusBox.setMaxWidth(Double.MAX_VALUE);
            setupComboBoxDisplay(this.statusBox, new StringConverter<String>() {
                @Override
                public String toString(String st) {
                    return st != null ? UiFormatters.statusWord(st) : "";
                }
                @Override
                public String fromString(String string) { return null; }
            });
            this.statusBox.getSelectionModel().select(existingBooking.getStatus() != null ? existingBooking.getStatus() : "BOOKED");
        } else {
            this.statusBox = null;
        }

        // Layout i Grid: Nytt flöde (Fordon -> Tjänster -> Mekaniker -> Datum -> Tid -> Beskrivning)
        this.datePicker.setMaxWidth(Double.MAX_VALUE);
        this.startTimeBox.setMaxWidth(Double.MAX_VALUE);
        this.descField.setMaxWidth(Double.MAX_VALUE);
        if (this.statusBox != null) {
            this.statusBox.setMaxWidth(Double.MAX_VALUE);
        }

        int rowIdx = 0;
        // 1. Fordon
        add(new Label(I18n.get("dialog.booking.vehicle_select") + ":"), 0, rowIdx);
        GridPane.setHgrow(this.vehicleBox, Priority.ALWAYS);
        add(this.vehicleBox, 1, rowIdx++);

        // 2. Tjänster
        Label serviceLbl = new Label(I18n.get("dialog.booking.service_select") + ":");
        GridPane.setValignment(serviceLbl, VPos.TOP);
        serviceLbl.setPadding(new Insets(6, 0, 0, 0));
        add(serviceLbl, 0, rowIdx);
        VBox serviceCol = new VBox(6);
        GridPane.setHgrow(serviceCol, Priority.ALWAYS);
        // Fältet ska fylla kolumnen som combon gjorde, annars stannar pilen långt in från kanten.
        this.serviceMulti.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(this.serviceMulti, Priority.ALWAYS);
        // Ersätter combon, plusknappen och den separata listan, som togs bort.
        // chips i fältet, och listan i fältet visar pris och tid per tjänst.
        serviceCol.getChildren().add(this.serviceMulti);
        if (isServicesLocked) {
            Label lockNotice = new Label("🔒 " + I18n.get("dialog.booking.services_locked_work_started"));
            lockNotice.setStyle("-fx-font-size: 11px; -fx-text-fill: #f87171; -fx-font-weight: bold;");
            serviceCol.getChildren().add(lockNotice);
        }
        serviceCol.getChildren().add(this.totalSummaryLabel);
        add(serviceCol, 1, rowIdx++);

        // 3. Mekaniker
        Label mechLbl = new Label(I18n.get("dialog.booking.mechanic_select") + ":");
        GridPane.setValignment(mechLbl, VPos.TOP);
        mechLbl.setPadding(new Insets(6, 0, 0, 0));
        add(mechLbl, 0, rowIdx);
        VBox mechCol = new VBox(4, this.mechanicMulti, this.mechanicFilterHint);
        GridPane.setHgrow(mechCol, Priority.ALWAYS);
        this.mechanicMulti.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(this.mechanicMulti, Priority.ALWAYS);
        add(mechCol, 1, rowIdx++);

        // 4. Datum & Tid (Kalender synlig med starttid bredvid)
        Label dateTimeLbl = new Label(I18n.get("dialog.booking.date_and_time") + ":");
        GridPane.setValignment(dateTimeLbl, VPos.TOP);
        dateTimeLbl.setPadding(new Insets(6, 0, 0, 0));
        add(dateTimeLbl, 0, rowIdx);

        VBox calCol = new VBox(10, scheduleField.getDateHeader(), scheduleField.getCalendarHint(), scheduleField.getCalendarNode());
        calCol.setAlignment(Pos.TOP_LEFT);
        calCol.getStyleClass().add("booking-card");

        VBox timeCol = new VBox(8);
        Label timeTitle = new Label(I18n.get("dialog.booking.time_select") + ":");
        timeTitle.getStyleClass().add("booking-card-title");
        // Arbetspasset hamnar längst ned i kortet, så att innehållet fördelas över hela höjden
        // i stället för att lämna en tom yta under texten.
        Label timeHint = new Label(I18n.get("dialog.booking.only_free_times"));
        timeHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -wac-muted;");
        timeHint.setWrapText(true);
        timeCol.getChildren().addAll(timeTitle, this.startTimeBox, timeHint, this.durationLabel);
        // Innehållet centreras i kortet, så att luften fördelas jämnt över och under i stället för
        // att samlas i en tom yta.
        timeCol.setAlignment(Pos.CENTER_LEFT);
        timeCol.setMinWidth(220);
        timeCol.setPrefWidth(240);
        HBox.setHgrow(timeCol, Priority.ALWAYS);
        this.startTimeBox.setMaxWidth(Double.MAX_VALUE);
        timeCol.getStyleClass().add("booking-card");

        HBox dateTimeRow = new HBox(16, calCol, timeCol);
        dateTimeRow.setAlignment(Pos.TOP_LEFT);

        // Datumväljaren själv läggs inte i layouten: kalendern ovan är byggd från ett eget skinn,
        // och lägger man ändå kontrollen i scenen skapar JavaFX ett andra skinn — då kastar
        // DatePickerSkin "duplicate children added" och hela bokningsformuläret dör vid klick.
        VBox dateTimeContainer = new VBox(4, dateTimeRow);
        GridPane.setHgrow(dateTimeContainer, Priority.ALWAYS);
        add(dateTimeContainer, 1, rowIdx++);

        // 5. Beskrivning
        add(new Label(I18n.get("table.col.description") + ":"), 0, rowIdx);
        GridPane.setHgrow(this.descField, Priority.ALWAYS);
        add(this.descField, 1, rowIdx++);

        // 6. Status (om redigering)
        if (this.statusBox != null) {
            add(new Label(I18n.get("table.col.status") + ":"), 0, rowIdx);
            GridPane.setHgrow(this.statusBox, Priority.ALWAYS);
            add(this.statusBox, 1, rowIdx++);
        }
    }

/** Fordon, datum och antingen en beskrivning eller en tjänst. Ny bokning kräver en tjänst. */
    private boolean requiredFieldsFilled(int excludeBookingId) {
        boolean hasServices = !selectedServices.isEmpty();
        if (getSelectedVehicle() == null || getSelectedDate() == null) {
            return false;
        }
        if (!hasServices && getDescription().isEmpty()) {
            return false;
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

/** Låser OK-knappen tills fälten är ifyllda, samma regel som validate(). */
    public BooleanBinding requiredFieldsFilledBinding(final int excludeBookingId) {
        return Bindings.createBooleanBinding(
                () -> requiredFieldsFilled(excludeBookingId),
                this.vehicleBox.valueProperty(), this.datePicker.valueProperty(),
                this.startTimeBox.valueProperty(),
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

        // 1. Spärr mot stängningstid (17:00): ett jobb som slutar efter stängning får inte bokas
        if (startTime != null) {
            LocalTime endTime = startTime.plusMinutes(minutes);
            if (endTime.isAfter(BookingAvailability.CLOSING_TIME)) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.booking.closing_time_exceeded"));
                return false;
            }
        }

        // 2. Behörighetskontroll: kontrollera att valda tjänster kan bemannas av behöriga mekaniker
        List<Mechanic> team = getSelectedMechanics();   // de mekaniker som är valda i fältet
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
        if (startTime != null) {
            if (BookingAvailability.isTeamBooked(garage, team, date, startTime,
                    startTime.plusMinutes(minutes), excludeBookingId)) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.booking.slot_busy_error"));
                return false;
            }
        }

        return true;
    }

    public Vehicle getSelectedVehicle() { return vehicleBox.getValue(); }
    public LocalDate getSelectedDate() { return datePicker.getValue(); }
    public List<ServiceItem> getSelectedServices() { return new ArrayList<ServiceItem>(selectedServices); }
/** Första valda tjänsten, eller null. Rullistans värde räknas inte, det är bara ett förslag. */
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

/** Mekanikerna i den ordning de valdes. */
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

    /** Kör lyssnaren som låter dialogen växa med innehållet. */
    void contentGrown() {
        if (onContentGrown != null) {
            onContentGrown.run();
        }
    }

    /** Låter dialogen veta när formuläret behöver mer plats, så att fönstret kan växa med det. */
    public void setOnContentGrown(Runnable listener) {
        this.onContentGrown = listener;
    }
    public LocalTime getSelectedStartTime() { return startTimeBox.getValue(); }
    public String getDescription() { return descField.getText().trim(); }
    public String getStatus() { return statusBox != null ? statusBox.getValue() : "BOOKED"; }

/** Gråmarkerar tjänster som redan ligger i bokningen. */
    private ListCell<ServiceItem> serviceChoiceCell() {
        return new ListCell<ServiceItem>() {
            @Override
            protected void updateItem(ServiceItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                    setOpacity(1);
                    return;
                }
                setText(SeedText.resolve(item.getName()) + " · " + UiFormatters.formatMoney(item.getPrice())
                        + " (" + item.getEstimatedMinutes() + " min)");
                boolean alreadyInBooking = selectedServices.stream().anyMatch(s -> s.getId() == item.getId());
                setDisable(alreadyInBooking);
                setOpacity(alreadyInBooking ? 0.45 : 1);
            }
        };
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
