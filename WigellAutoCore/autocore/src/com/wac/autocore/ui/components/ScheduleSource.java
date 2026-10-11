package com.wac.autocore.ui.components;

import java.util.List;

import com.wac.autocore.model.Mechanic;

/**
 * What the schedule field needs to know about the job being planned: how long it takes and who
 * does it. The booking form and the draft dialog both answer these, so both get the same calendar.
 */
public interface ScheduleSource {

    /** The whole job in minutes. */
    int getTotalEstimatedMinutes();

    /** The mechanic who holds the time, or null when none is picked yet. */
    Mechanic getSelectedMechanic();

    /** The mechanics picked for the job. */
    List<Mechanic> getSelectedMechanics();
}
