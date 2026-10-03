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
    private final ScrollPane servicesScroll;
    private final Label totalSummaryLabel = new Label();
    private final ComboBox<Mechanic> mechanicBox;
    private final Label mechanicFilterHint;
    private final ComboBox<LocalTime> startTimeBox;
    private final Label durationLabel;
    private final TextField descField;
    private final ComboBox<String> statusBox;

    /** Id för bokningen som redigeras, så dess egen tid inte räknas som upptagen. 0 = ny bokning. */
    private final int excludeId;

    /** Verkstaden som bokningen gäller — kalendern behöver den för att kunna sålla dagar. */
    private final GarageSystem garage;

    /** Kalenderns dagceller, så de kan ritas om när tjänster eller mekaniker ändras. */
    private final List<BookingDayCell> dayCells = new ArrayList<BookingDayCell>();

    /**
     * Posten "Välj ingen mekaniker" i rullistan. Ett riktigt objekt i stället för null: en null-post
     * i listan får JavaFX att kasta IndexOutOfBoundsException när man väljer den, för väljarens
     * markering nollas samtidigt som posten markeras (ListViewBehavior läser en ändring som redan
     * hunnit bli inaktuell). Värdet utåt är fortfarande null — se {@link #getSelectedMechanic()}.
     */
    private static final Mechanic NO_MECHANIC = new Mechanic(0, "", "", "");

    /** Höjden på en tjänsterad i listan (radhöjd + mellanrum) och taket för hur hög rutan får bli. */
    private static final double SERVICE_ROW_HEIGHT = 30;
    private static final double SERVICES_MAX_HEIGHT = 330;

    /** Anropas när formuläret behöver mer plats, så att dialogen kan växa med innehållet. */
    private Runnable onContentGrown;

    public BookingFormPane(GarageSystem garage, Booking existingBooking,
                           LocalDate initialDate, Mechanic defaultMechanic, Integer defaultHour) {
        this.garage = garage;
        setHgap(14);
        setVgap(14);
        setPadding(new Insets(18, 22, 18, 22));
        setPrefWidth(640);

        javafx.scene.layout.ColumnConstraints col0 = new javafx.scene.layout.ColumnConstraints();
        col0.setMinWidth(120);
        col0.setPrefWidth(130);
        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(col0, col1);

        this.totalSummaryLabel.setWrapText(true);
        this.totalSummaryLabel.setMaxWidth(Double.MAX_VALUE);

        this.servicesScroll = new ScrollPane(this.selectedServicesContainer);
        this.servicesScroll.setFitToWidth(true);
        this.servicesScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.servicesScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        // Höjden sätts i renderServices, efter hur många tjänster bokningen innehåller.
        this.servicesScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");

        // 1. Fordon
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

        // 2. Tjänster (stöd för flera val i samma bokning)
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
        // Tjänster som redan ligger i bokningen visas gråmarkerade och går inte att välja igen.
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
            // Rutan växer med antalet tjänster i stället för att scrolla i en liten yta. Taket gör
            // att en lång lista fortfarande scrollar, men först när dialogen är så hög den får bli.
            double wanted = 12 + Math.max(1, selectedServices.size()) * SERVICE_ROW_HEIGHT;
            double height = Math.min(SERVICES_MAX_HEIGHT, wanted);
            servicesScroll.setPrefHeight(height);
            servicesScroll.setMaxHeight(height);
            if (onContentGrown != null) {
                onContentGrown.run();
            }
        };

        addServiceBtn.setOnAction(e -> {
            if (isServicesLocked) return;
            ServiceItem sel = serviceBox.getValue();
            if (sel != null && selectedServices.stream().noneMatch(s -> s.getId() == sel.getId())) {
                selectedServices.add(sel);
            }
        });

        // 3. Mekaniker med dynamiskt kvalifikationsfilter för samtliga valda tjänster
        this.mechanicFilterHint = new Label();
        this.mechanicFilterHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -wac-muted;");

        this.mechanicBox = new ComboBox<Mechanic>();
        this.mechanicBox.setMaxWidth(Double.MAX_VALUE);
        setupComboBoxDisplay(this.mechanicBox, new StringConverter<Mechanic>() {
            @Override
            public String toString(Mechanic m) {
                if (m == null || m == NO_MECHANIC) return I18n.get("dialog.booking.no_mechanic");
                return m.getName() + " (" + SeedText.resolve(m.getSpecialization()) + ")";
            }
            @Override
            public Mechanic fromString(String string) { return null; }
        });

        this.excludeId = existingBooking != null ? existingBooking.getId() : 0;

        Runnable updateMechanics = () -> {
            List<Mechanic> team = garage.getRequiredMechanics(selectedServices);
            List<Mechanic> qualified = garage.getQualifiedMechanics(selectedServices);
            Mechanic currentSel = getSelectedMechanic();

            mechanicBox.getItems().clear();
            mechanicBox.getItems().add(NO_MECHANIC);
            if (!team.isEmpty()) {
                mechanicBox.getItems().addAll(team);
                for (Mechanic m : qualified) {
                    if (!mechanicBox.getItems().contains(m)) {
                        mechanicBox.getItems().add(m);
                    }
                }
            } else {
                mechanicBox.getItems().addAll(garage.getMechanics());
            }

            if (selectedServices.isEmpty()) {
                mechanicFilterHint.setText("");
            } else if (!team.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Mechanic m : team) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(m.getName());
                }
                mechanicFilterHint.setText("🔧 " + I18n.get("dialog.booking.team_assigned", sb.toString()));
                mechanicFilterHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -wac-accent; -fx-font-weight: bold;");
            } else {
                mechanicFilterHint.setText(I18n.get("dialog.booking.no_mechanic_for_selected_services"));
                mechanicFilterHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #f87171;");
            }

            int targetId = currentSel != null ? currentSel.getId()
                    : (existingBooking != null ? existingBooking.getMechanicId()
                    : (defaultMechanic != null ? defaultMechanic.getId() : 0));

            if (targetId > 0) {
                boolean found = false;
                for (Mechanic m : mechanicBox.getItems()) {
                    if (m.getId() == targetId) {
                        mechanicBox.getSelectionModel().select(m);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    if (!team.isEmpty()) {
                        mechanicBox.getSelectionModel().select(team.get(0));
                    } else {
                        mechanicBox.getSelectionModel().selectFirst();
                    }
                }
            } else {
                if (!team.isEmpty()) {
                    mechanicBox.getSelectionModel().select(team.get(0));
                } else {
                    mechanicBox.getSelectionModel().selectFirst();
                }
            }
        };

        // 4. Bokningsdatum (visas som kalender där otillgängliga datum gråmarkeras)
        LocalDate initialDateVal = existingBooking != null && existingBooking.getDate() != null
                ? existingBooking.getDate()
                : (initialDate != null ? initialDate : LocalDate.now().plusDays(1));
        this.datePicker = new DatePicker(initialDateVal);
        this.datePicker.setVisible(false);
        this.datePicker.setManaged(false);

        // Kalenderns dagar. Fabriken sätts en gång och cellerna läser aktuella tjänster och tider
        // varje gång de ritas, så de kan uppdateras med refresh() när valet ändras.
        this.datePicker.setDayCellFactory(picker -> {
            BookingDayCell cell = new BookingDayCell();
            dayCells.add(cell);
            return cell;
        });

        com.sun.javafx.scene.control.skin.DatePickerSkin dateSkin =
                new com.sun.javafx.scene.control.skin.DatePickerSkin(this.datePicker);
        Node calendarNode = dateSkin.getPopupContent();
        // Temafärgerna sätts i stilmallen (.booking-calendar): en inline-style kan inte slå upp
        // -wac-card/-wac-line, så de föll tyst bort och kalendern blev genomskinlig.
        calendarNode.getStyleClass().add("booking-calendar");

        Label dateHeaderLabel = new Label();
        dateHeaderLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -wac-text;");
        Label calendarHintLabel = new Label(I18n.get("dialog.booking.calendar_hint"));
        calendarHintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -wac-muted;");
        calendarHintLabel.setWrapText(true);
        calendarHintLabel.setMaxWidth(230);

        Runnable updateDateHeader = () -> {
            LocalDate d = datePicker.getValue();
            if (d != null) {
                dateHeaderLabel.setText("📅 " + I18n.get("dialog.booking.selected_date", UiFormatters.formatDate(d)));
            } else {
                dateHeaderLabel.setText("");
            }
        };
        this.datePicker.valueProperty().addListener((obs, o, n) -> updateDateHeader.run());
        updateDateHeader.run();

        // Ritar om kalenderns dagar när tjänster, mekaniker eller datum ändrats. Cellerna behåller
        // annars den bedömning de gjorde när de skapades, och en dag som inte längre rymmer hela
        // jobbet stod kvar som bokningsbar.
        Runnable setupDatePickerCells = () -> {
            for (BookingDayCell cell : dayCells) {
                cell.refresh();
            }
        };

        Runnable ensureValidDate = () -> {
            int duration = getTotalEstimatedMinutes() > 0 ? getTotalEstimatedMinutes() : 60;
            List<Mechanic> team = garage.getRequiredMechanics(selectedServices);
            Mechanic m = getSelectedMechanic();
            LocalDate current = datePicker.getValue();
            if (current == null || !BookingAvailability.hasAvailableSlotOnDate(garage, m, team, current, duration, excludeId)) {
                LocalDate check = LocalDate.now().plusDays(1);
                for (int i = 0; i < 60; i++) {
                    if (BookingAvailability.hasAvailableSlotOnDate(garage, m, team, check, duration, excludeId)) {
                        datePicker.setValue(check);
                        break;
                    }
                    check = check.plusDays(1);
                }
            }
        };

        // 5. Starttid med stängningsspärr (17:00) och tillgänglighetsindikering
        this.startTimeBox = new ComboBox<LocalTime>();

        Function<LocalTime, Boolean> isBusyFunc = time -> {
            if (time == null) return false;
            int duration = getTotalEstimatedMinutes() > 0 ? getTotalEstimatedMinutes() : 60;
            LocalTime end = time.plusMinutes(duration);
            if (end.isAfter(BookingAvailability.CLOSING_TIME)) {
                return true;
            }
            LocalDate d = datePicker.getValue();
            List<Mechanic> team = garage.getRequiredMechanics(selectedServices);
            if (team != null && !team.isEmpty()) {
                return BookingAvailability.isTeamBooked(garage, team, d, time, end, excludeId);
            }
            Mechanic m = getSelectedMechanic();
            return BookingAvailability.isRangeBooked(garage, m, d, time, end, excludeId);
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

        // 6. Dynamisk sluttidsberäkning
        this.durationLabel = new Label();
        this.durationLabel.setStyle("-fx-text-fill: -wac-accent; -fx-font-weight: bold;");

        Runnable updateDuration = () -> {
            LocalTime start = startTimeBox.getValue();
            int totalMin = getTotalEstimatedMinutes();
            if (start != null && totalMin > 0) {
                LocalTime end = start.plusMinutes(totalMin);
                if (end.isAfter(BookingAvailability.CLOSING_TIME)) {
                    durationLabel.setText("⚠️ " + I18n.get("dialog.booking.time_window",
                            start.format(TimeSlotCell.TIME_FMT), end.format(TimeSlotCell.TIME_FMT))
                            + " (" + totalMin + " min) – " + I18n.get("dialog.booking.closing_time_exceeded"));
                    durationLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
                } else {
                    durationLabel.setText(I18n.get("dialog.booking.time_window",
                            start.format(TimeSlotCell.TIME_FMT), end.format(TimeSlotCell.TIME_FMT))
                            + " (" + totalMin + " min)");
                    durationLabel.setStyle("-fx-text-fill: -wac-accent; -fx-font-weight: bold;");
                }
            } else if (start != null) {
                LocalTime end = start.plusHours(1);
                durationLabel.setText(I18n.get("dialog.booking.time_window",
                        start.format(TimeSlotCell.TIME_FMT), end.format(TimeSlotCell.TIME_FMT)) + " (60 min)");
                durationLabel.setStyle("-fx-text-fill: -wac-accent; -fx-font-weight: bold;");
            } else {
                durationLabel.setText("");
            }
        };

        Runnable refreshTimeBox = () -> {
            int duration = getTotalEstimatedMinutes() > 0 ? getTotalEstimatedMinutes() : 60;
            List<LocalTime> validTimes = new ArrayList<LocalTime>();
            for (int h = 7; h <= 16; h++) {
                LocalTime t = LocalTime.of(h, 0);
                if (!t.plusMinutes(duration).isAfter(BookingAvailability.CLOSING_TIME)) {
                    validTimes.add(t);
                }
            }
            LocalTime currentSel = startTimeBox.getValue();
            startTimeBox.setItems(FXCollections.observableArrayList(validTimes));
            if (currentSel != null && validTimes.contains(currentSel) && !Boolean.TRUE.equals(isBusyFunc.apply(currentSel))) {
                startTimeBox.getSelectionModel().select(currentSel);
            } else {
                selectFirstAvailableTime(validTimes, isBusyFunc);
            }
            if (startTimeBox.getButtonCell() != null) {
                startTimeBox.getButtonCell().updateIndex(-1);
            }
            updateDuration.run();
        };

        // Koppla lyssnare
        this.datePicker.valueProperty().addListener((obs, o, n) -> refreshTimeBox.run());
        this.mechanicBox.valueProperty().addListener((obs, o, n) -> {
            setupDatePickerCells.run();
            refreshTimeBox.run();
        });
        this.startTimeBox.valueProperty().addListener((obs, o, n) -> updateDuration.run());

        // 7. Beskrivning
        String initialDesc = existingBooking != null && existingBooking.getDescription() != null
                ? SeedText.resolve(existingBooking.getDescription()) : "";
        this.descField = new TextField(initialDesc);
        this.descField.setPromptText(I18n.get("dialog.booking.desc_prompt"));

        selectedServices.addListener((javafx.collections.ListChangeListener<ServiceItem>) c -> {
            renderServices.run();
            // Gråmarkeringen i tjänstelistan följer bokningens innehåll: cellerna byggs om när
            // listan ändras, annars står en redan tillagd tjänst kvar som valbar.
            serviceBox.setCellFactory(lv -> serviceChoiceCell());
            updateMechanics.run();
            setupDatePickerCells.run();
            ensureValidDate.run();
            refreshTimeBox.run();
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

        // Initial körning
        renderServices.run();
        updateMechanics.run();
        setupDatePickerCells.run();
        ensureValidDate.run();
        if (existingBooking != null && existingBooking.getStartTime() != null) {
            this.startTimeBox.getSelectionModel().select(existingBooking.getStartTime());
        } else if (defaultHour != null && defaultHour >= 7 && defaultHour <= 16) {
            this.startTimeBox.getSelectionModel().select(LocalTime.of(defaultHour, 0));
        }
        refreshTimeBox.run();
        updateDuration.run();

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
        HBox servicePickerRow = new HBox(8, this.serviceBox, addServiceBtn);
        servicePickerRow.setAlignment(Pos.CENTER_LEFT);
        servicePickerRow.setFillHeight(true);
        HBox.setHgrow(this.serviceBox, Priority.ALWAYS);
        VBox serviceCol = new VBox(6);
        GridPane.setHgrow(serviceCol, Priority.ALWAYS);
        serviceCol.getChildren().add(servicePickerRow);
        if (isServicesLocked) {
            Label lockNotice = new Label("🔒 " + I18n.get("dialog.booking.services_locked_work_started"));
            lockNotice.setStyle("-fx-font-size: 11px; -fx-text-fill: #f87171; -fx-font-weight: bold;");
            serviceCol.getChildren().add(lockNotice);
        }
        serviceCol.getChildren().addAll(this.servicesScroll, this.totalSummaryLabel);
        add(serviceCol, 1, rowIdx++);

        // 3. Mekaniker
        Label mechLbl = new Label(I18n.get("dialog.booking.mechanic_select") + ":");
        GridPane.setValignment(mechLbl, VPos.TOP);
        mechLbl.setPadding(new Insets(6, 0, 0, 0));
        add(mechLbl, 0, rowIdx);
        VBox mechCol = new VBox(4, this.mechanicBox, this.mechanicFilterHint);
        GridPane.setHgrow(mechCol, Priority.ALWAYS);
        add(mechCol, 1, rowIdx++);

        // 4. Datum & Tid (Kalender synlig med starttid bredvid)
        Label dateTimeLbl = new Label(I18n.get("dialog.booking.date_and_time") + ":");
        GridPane.setValignment(dateTimeLbl, VPos.TOP);
        dateTimeLbl.setPadding(new Insets(6, 0, 0, 0));
        add(dateTimeLbl, 0, rowIdx);

        VBox calCol = new VBox(6, dateHeaderLabel, calendarHintLabel, calendarNode);
        calCol.setAlignment(Pos.TOP_LEFT);

        VBox timeCol = new VBox(8);
        Label timeTitle = new Label(I18n.get("dialog.booking.time_select") + ":");
        timeTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -wac-text;");
        timeCol.getChildren().addAll(timeTitle, this.startTimeBox, this.durationLabel);
        timeCol.setMinWidth(170);
        timeCol.setPrefWidth(190);
        HBox.setHgrow(timeCol, Priority.ALWAYS);

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

    /**
     * Sant när de obligatoriska fälten är ifyllda: fordon, datum och antingen en beskrivning eller
     * minst en tjänst. En ny bokning (excludeBookingId == 0) måste dessutom innehålla en tjänst —
     * en bokning som redan finns får behålla sina.
     *
     * Delas av låsningen av OK-knappen och av validate(), så de två inte kan glida ifrån varandra.
     */
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

    /**
     * Låsningen av OK-knappen: den är låst så länge ett obligatoriskt fält saknas och öppnas när
     * fältet fylls i. Följer samma regel som validate(), via {@link #requiredFieldsFilled(int)}.
     */
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
        List<Mechanic> team = garage.getRequiredMechanics(selectedServices);
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

        // 3. Kontroll mot upptagna tider: hela bokningens intervall kontrolleras för de behövliga mekanikerna
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
    /**
     * Den första tjänst som lagts till i bokningen, eller null när ingen är vald.
     * Rullistans värde räknas inte, för det är bara ett förslag som står förvalt.
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
    public double getTotalEstimatedPrice() {
        double total = 0.0;
        for (ServiceItem s : selectedServices) {
            if (s != null) total += s.getPrice();
        }
        return total;
    }
    public Mechanic getSelectedMechanic() {
        Mechanic m = mechanicBox.getValue();
        return m == NO_MECHANIC ? null : m;
    }

    /** Låter dialogen veta när formuläret behöver mer plats, så att fönstret kan växa med det. */
    public void setOnContentGrown(Runnable listener) {
        this.onContentGrown = listener;
    }
    public LocalTime getSelectedStartTime() { return startTimeBox.getValue(); }
    public String getDescription() { return descField.getText().trim(); }
    public String getStatus() { return statusBox != null ? statusBox.getValue() : "BOOKED"; }

    /**
     * Rad i tjänstelistan. En tjänst som redan ligger i bokningen är gråmarkerad och går inte att
     * välja, så samma arbete inte kan bokas två gånger av misstag.
     */
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

    private static <T> void setupComboBoxDisplay(ComboBox<T> box, StringConverter<T> converter) {
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

    /**
     * En dag i kalendern. Reglerna läses vid varje ritning i stället för att låsas när cellen
     * skapades: annars står en dag kvar som bokningsbar sedan tjänsterna ändrats till ett jobb som
     * inte längre får plats inom öppettiderna (t.ex. 315 minuter mot en dag med bara en sen timme kvar).
     */
    private final class BookingDayCell extends DateCell {

        @Override
        public void updateItem(LocalDate date, boolean empty) {
            super.updateItem(date, empty);
            if (empty || date == null) {
                return;
            }
            if (date.isBefore(LocalDate.now())) {
                unbookable("-fx-background-color: #f1f5f9; -fx-text-fill: #94a3b8; -fx-opacity: 0.45;",
                        "dialog.booking.date_past");
                return;
            }
            if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                unbookable("-fx-background-color: #f1f5f9; -fx-text-fill: #94a3b8; -fx-opacity: 0.45;",
                        "dialog.booking.date_weekend");
                return;
            }
            int duration = getTotalEstimatedMinutes() > 0 ? getTotalEstimatedMinutes() : 60;
            if (duration > BookingAvailability.MAX_WORK_MINUTES_PER_DAY) {
                unbookable("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-opacity: 0.50;",
                        "dialog.booking.duration_too_long");
                return;
            }
            List<Mechanic> team = garage.getRequiredMechanics(selectedServices);
            if (!BookingAvailability.hasAvailableSlotOnDate(garage, getSelectedMechanic(), team, date, duration, excludeId)) {
                unbookable("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-opacity: 0.50;",
                        "dialog.booking.date_fully_booked");
                return;
            }
            setDisable(false);
            getStyleClass().remove("unbookable-day");
            setStyle("");
            setTooltip(null);
        }

        /** Ritar om cellen med de val som gäller nu. */
        void refresh() {
            updateItem(getItem(), isEmpty());
        }

        private void unbookable(String style, String messageKey) {
            setDisable(true);
            if (!getStyleClass().contains("unbookable-day")) {
                getStyleClass().add("unbookable-day");
            }
            setStyle(style);
            setTooltip(new Tooltip(I18n.get(messageKey)));
        }
    }
}
