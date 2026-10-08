package com.wac.autocore.ui.components;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.TimeSlot;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/**
 * A mechanic card on the Kanban board: the header with the view switcher and the body
 * with the day, week or month view. The board itself is built by KanbanBoard.
 */
public class MechanicKanbanCard {

    public enum KanbanViewMode {
        DAY, WEEK, MONTH
    }

    final GarageSystem garage;
    final com.wac.autocore.ui.navigation.PageRouter router;
    final Runnable onRefresh;
    private final VBox cardContainer;

    private int activeMechanicIndex;
    KanbanViewMode currentMode = KanbanViewMode.DAY;
    LocalDate selectedDate = LocalDate.now();
    TimeSlot expandedSlot;

    void toggleExpandSlot(TimeSlot slot) {
        if (expandedSlot != null && expandedSlot.getDate().equals(slot.getDate()) && expandedSlot.getHour() == slot.getHour()) {
            expandedSlot = null;
        } else {
            expandedSlot = slot;
        }
        renderBody();
    }

    boolean isExpanded(TimeSlot slot) {
        return expandedSlot != null && expandedSlot.getDate().equals(slot.getDate()) && expandedSlot.getHour() == slot.getHour();
    }

    final KanbanSlotDrawer slotDrawer = new KanbanSlotDrawer(this);
    final KanbanDayView dayView = new KanbanDayView(this);
    final KanbanWeekView weekView = new KanbanWeekView(this);
    final KanbanMonthView monthView = new KanbanMonthView(this);

    private final KanbanCardHeader header;
    private final VBox bodyContent;

    public MechanicKanbanCard(GarageSystem garage, Runnable onRefresh) {
        this(garage, 0, null, onRefresh);
    }

    public MechanicKanbanCard(GarageSystem garage, int initialMechanicIndex, Runnable onRefresh) {
        this(garage, initialMechanicIndex, null, onRefresh);
    }

    public MechanicKanbanCard(GarageSystem garage, int initialMechanicIndex,
                              com.wac.autocore.ui.navigation.PageRouter router, Runnable onRefresh) {
        this.garage = garage;
        this.activeMechanicIndex = initialMechanicIndex;
        this.router = router;
        this.onRefresh = onRefresh;

        this.cardContainer = new VBox(8);
        this.cardContainer.getStyleClass().addAll("kanban-card", "kanban-card-compact");
        this.cardContainer.setMinWidth(360);
        this.cardContainer.setPrefWidth(400);

        this.header = new KanbanCardHeader(this);
        this.bodyContent = new VBox(6);

        this.cardContainer.getChildren().addAll(header.getBox(), bodyContent);

        I18n.addListener(lang -> render());
        render();
    }

    public VBox getView() {
        return cardContainer;
    }

    public void render() {
        header.render();
        renderBody();
    }

    Mechanic getActiveMechanic() {
        List<Mechanic> mechanics = garage.getMechanics();
        if (mechanics.isEmpty()) return null;
        if (activeMechanicIndex < 0) activeMechanicIndex = 0;
        if (activeMechanicIndex >= mechanics.size()) activeMechanicIndex = mechanics.size() - 1;
        return mechanics.get(activeMechanicIndex);
    }

    static String formatSpecialization(String spec) {
        if (spec == null) return "";
        if (spec.equalsIgnoreCase("General service") || spec.equalsIgnoreCase("Allmän service")) {
            return I18n.get("kanban.specialization.general_service");
        }
        if (spec.equalsIgnoreCase("Brakes") || spec.equalsIgnoreCase("Bromsar")) {
            return I18n.get("kanban.specialization.brakes");
        }
        if (spec.equalsIgnoreCase("Diagnostics") || spec.equalsIgnoreCase("Diagnostik") || spec.equalsIgnoreCase("Diagnostik & Felsökning")) {
            return I18n.get("kanban.specialization.diagnostics");
        }
        return spec;
    }

    void renderBody() {
        bodyContent.getChildren().clear();
        Mechanic mech = getActiveMechanic();
        if (mech == null) return;

        switch (currentMode) {
            case DAY:
                bodyContent.getChildren().add(dayView.build(mech));
                break;
            case WEEK:
                bodyContent.getChildren().add(weekView.build(mech));
                break;
            case MONTH:
                bodyContent.getChildren().add(monthView.build(mech));
                break;
        }
    }
}
