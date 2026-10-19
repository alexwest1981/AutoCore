package com.wac.autocore.model;

import java.util.Locale;

public enum BookingStatus {
    BOOKED,
    CONFIRMED,
    WORK_ORDER_CREATED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    /** Turns the text stored in the database into a status. */
    public static BookingStatus fromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return BOOKED;
        }
        return valueOf(text.trim().toUpperCase(Locale.ROOT));
    }
}