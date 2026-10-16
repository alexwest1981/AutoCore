package com.wac.autocore.model;

import java.util.Locale;

public enum WorkOrderStatus {
    CREATED,
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;


    /** Turns the text stored in the database into a status. */
    public static WorkOrderStatus fromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return CREATED;
        }
        return valueOf(text.trim().toUpperCase(Locale.ROOT));
    }

    /** True when this status may become the given one. Anything not listed is refused. */
    public boolean canChangeTo(WorkOrderStatus next) {

        if (this == CREATED && next == CONFIRMED) {
            return true;
        }
        if (this == CREATED && next == CANCELLED) {
            return true;
        }
        if (this == CONFIRMED && next == IN_PROGRESS) {
            return true;
        }
        if (this == CONFIRMED && next == CANCELLED) {
            return true;
        }
        if (this == IN_PROGRESS && next == COMPLETED) {
            return true;
        }
        if (this == IN_PROGRESS && next == CANCELLED) {
            return true;
        }
        if (this == CANCELLED && next == IN_PROGRESS) {
            return true;
        }
        return false;
    }
}
