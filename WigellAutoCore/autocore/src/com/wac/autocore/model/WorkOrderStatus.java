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
}
