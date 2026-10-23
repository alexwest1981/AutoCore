package com.wac.autocore.model;

import java.util.Locale;

/**
 * What kind of work order it is.
 *
 * The constant's name is the text stored in {@code work_orders.type}, so the database keeps
 * readable values and a row written before this enum existed still reads. Nothing outside this
 * class may compare the stored text against a literal; ask the enum instead.
 */
public enum WorkOrderType {

    /** The ordinary order: work the customer booked and pays for. */
    STANDARD,

    /** Work redone on an earlier order, normally at no charge. */
    RECLAMATION,

    /** Work on the workshop's own vehicles. */
    INTERNAL;

    /**
     * The type the stored text stands for. An empty text is the ordinary order, which is what the
     * model used when a column was missing, and an unknown text is read the same way rather than
     * leaving a row without a type to show.
     */
    public static WorkOrderType fromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return STANDARD;
        }
        try {
            return valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return STANDARD;
        }
    }
}
