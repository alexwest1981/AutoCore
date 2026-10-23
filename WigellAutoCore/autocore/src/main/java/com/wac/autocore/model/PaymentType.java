package com.wac.autocore.model;

import java.util.Locale;

/**
 * How a customer paid.
 *
 * The constant's name is the text stored in {@code payments.payment_type}, so the database keeps
 * readable values and a row written before this enum existed still reads. Nothing outside this
 * class may compare the stored text against a literal; ask the enum instead.
 */
public enum PaymentType {
    CARD,
    SWISH,
    CASH;

    /**
     * The type the stored text stands for, or null when the text is not one of them. An unknown
     * text keeps the older behaviour: no payment is made, rather than one that can never settle.
     */
    public static PaymentType fromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }
}
