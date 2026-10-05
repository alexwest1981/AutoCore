package com.wac.autocore.ui.components;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javafx.collections.ListChangeListener;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.seed.SeedText;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;

/**
 * Mekanikerfältet i bokningsformuläret: vem som ska utföra arbetet.
 * Teamet följer tjänsterna, och den som tagits bort ur teamet läggs inte tillbaka.
 */
class BookingMechanicsField {

    private final GarageSystem garage;
    private final BookingFormPane form;
    private final ComboBox<Mechanic> box = new ComboBox<Mechanic>();
    private final MultiSelectComboBox<Mechanic> multi;
    private final Label hint = new Label();
    private final Set<Integer> removed = new HashSet<Integer>();
    private final Set<Integer> added = new HashSet<Integer>();
    private boolean updating;

    BookingMechanicsField(GarageSystem garage, BookingFormPane form) {
        this.garage = garage;
        this.form = form;
        this.multi = new MultiSelectComboBox<Mechanic>(
                I18n.get("dialog.booking.mechanic_select"),
                m -> m.getName() + " (" + SeedText.resolve(m.getSpecialization()) + ")");
        this.multi.setChipTextProvider(m -> m.getName());
        this.multi.setItems(garage.getMechanics());
        this.multi.setKeyProvider(m -> m.getId());
        this.multi.getSelectedItems().addListener((ListChangeListener<Mechanic>) c -> onSelectionChanged());
        this.hint.setStyle("-fx-font-size: 11px; -fx-text-fill: -wac-muted;");
        this.box.setMaxWidth(Double.MAX_VALUE);
        BookingFormPane.setupComboBoxDisplay(this.box, new javafx.util.StringConverter<Mechanic>() {
            @Override
            public String toString(Mechanic m) {
                if (m == null || m == BookingFormPane.NO_MECHANIC) return I18n.get("dialog.booking.no_mechanic");
                return m.getName() + " (" + SeedText.resolve(m.getSpecialization()) + ")";
            }

            @Override
            public Mechanic fromString(String string) { return null; }
        });
    }

    ComboBox<Mechanic> getBox() { return box; }

    MultiSelectComboBox<Mechanic> getMulti() { return multi; }

    Label getHint() { return hint; }

    /** Teamets mekaniker som användaren tagit bort kommer ihåg som borttagna. */
    private void onSelectionChanged() {
        if (updating) {
            return;
        }
        List<Integer> remaining = new ArrayList<Integer>();
        for (Mechanic m : multi.getSelectedItems()) {
            remaining.add(m.getId());
        }
        List<Integer> inTeam = new ArrayList<Integer>();
        for (Mechanic m : garage.getRequiredMechanics(form.getSelectedServices())) {
            inTeam.add(m.getId());
            if (remaining.contains(m.getId())) {
                removed.remove(m.getId());
            } else {
                removed.add(m.getId());
            }
        }
        // Egna tillägg utanför teamet hålls isär: de ligger kvar när tjänsterna ändras.
        for (Mechanic m : garage.getMechanics()) {
            if (inTeam.contains(m.getId())) {
                continue;
            }
            if (remaining.contains(m.getId())) {
                added.add(m.getId());
            } else {
                added.remove(m.getId());
            }
        }
    }

    /** Fyller fältet utifrån tjänsterna som är valda just nu. */
    /** Fyller fältet utifrån tjänsterna som är valda just nu. */
    void update() {
        List<Mechanic> team = garage.getRequiredMechanics(form.getSelectedServices());
        List<Mechanic> qualified = garage.getQualifiedMechanics(form.getSelectedServices());

        box.getItems().clear();
        box.getItems().add(BookingFormPane.NO_MECHANIC);
        if (!team.isEmpty()) {
        box.getItems().addAll(team);
        for (Mechanic m : qualified) {
        if (!box.getItems().contains(m)) {
        box.getItems().add(m);
        }
        }
        } else {
        box.getItems().addAll(garage.getMechanics());
        }

        // Teamet följer tjänsterna. Egna tillägg ligger kvar, bortvalda läggs inte tillbaka.
        java.util.List<Mechanic> chosen = new java.util.ArrayList<Mechanic>();
        java.util.List<Integer> inTeam = new java.util.ArrayList<Integer>();
        for (Mechanic m : team) {
        inTeam.add(m.getId());
        if (!removed.contains(m.getId())) {
        chosen.add(m);
        }
        }
        // Bortval utanför teamet glöms, så att tjänsten kan komma tillbaka.
        java.util.Iterator<Integer> removedIterator = removed.iterator();
        while (removedIterator.hasNext()) {
        if (!inTeam.contains(removedIterator.next())) {
        removedIterator.remove();
        }
        }
        for (Mechanic m : garage.getMechanics()) {
        if (!added.contains(m.getId())) {
        continue;
        }
        boolean redan = false;
        for (Mechanic v : chosen) {
        if (v.getId() == m.getId()) {
        redan = true;
        break;
        }
        }
        if (!redan) {
        chosen.add(m);
        }
        }
        updating = true;
        multi.setSelectedItems(chosen);
        updating = false;

        // Tipset behövs bara när tjänsterna inte går att bemanna.
        if (!form.getSelectedServices().isEmpty() && team.isEmpty()) {
        hint.setText(I18n.get("dialog.booking.no_mechanic_for_selected_services"));
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #f87171;");
        } else {
        hint.setText("");
        }

        // Combon hålls i takt med fältet. Den är den som sparandet och tidskontrollerna läser.
        java.util.List<Mechanic> chosenNow = form.getSelectedMechanics();
        if (!chosenNow.isEmpty()) {
        int firstId = chosenNow.get(0).getId();
        boolean found = false;
        for (Mechanic m : box.getItems()) {
        if (m.getId() == firstId) {
        box.getSelectionModel().select(m);
        found = true;
        break;
        }
        }
        if (!found && !team.isEmpty()) {
        box.getSelectionModel().select(team.get(0));
        }
        } else if (!team.isEmpty()) {
        box.getSelectionModel().select(team.get(0));
        } else if (!box.getItems().isEmpty()) {
        box.getSelectionModel().selectFirst();
        }
    }
}
