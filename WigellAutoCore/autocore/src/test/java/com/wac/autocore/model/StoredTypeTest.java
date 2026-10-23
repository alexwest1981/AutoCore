package com.wac.autocore.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * The two types that used to be free text: how a payment was made, and what kind of work order it
 * is. The enum constant's name is the text in the database, so a row written before the enum
 * existed still reads, and nothing has to guess a word or a colour from a literal.
 */
class StoredTypeTest {

    @Test
    void theStoredTextIsTheConstantName() {
        assertEquals(PaymentType.CARD, PaymentType.fromText("CARD"));
        assertEquals(PaymentType.SWISH, PaymentType.fromText("SWISH"));
        assertEquals(PaymentType.CASH, PaymentType.fromText("CASH"));

        assertEquals(WorkOrderType.STANDARD, WorkOrderType.fromText("STANDARD"));
        assertEquals(WorkOrderType.RECLAMATION, WorkOrderType.fromText("RECLAMATION"));
        assertEquals(WorkOrderType.INTERNAL, WorkOrderType.fromText("INTERNAL"));
    }

    @Test
    void everyValueSurvivesARoundTripThroughTheDatabase() {
        for (PaymentType type : PaymentType.values()) {
            assertEquals(type, PaymentType.fromText(type.name()), type.name());
        }
        for (WorkOrderType type : WorkOrderType.values()) {
            assertEquals(type, WorkOrderType.fromText(type.name()), type.name());
        }
    }

    @Test
    void theTextIsReadWithoutCareForCaseOrSpaces() {
        assertEquals(PaymentType.CARD, PaymentType.fromText(" card "));
        assertEquals(WorkOrderType.RECLAMATION, WorkOrderType.fromText("reclamation"));
    }

    @Test
    void anUnknownPaymentTypeIsNothing() {
        assertNull(PaymentType.fromText("BITCOIN"));
        assertNull(PaymentType.fromText(""));
        assertNull(PaymentType.fromText(null));
    }

    @Test
    void anUnknownWorkOrderTypeIsTheOrdinaryOne() {
        assertEquals(WorkOrderType.STANDARD, WorkOrderType.fromText("PÅHITTAD"));
        assertEquals(WorkOrderType.STANDARD, WorkOrderType.fromText(""));
        assertEquals(WorkOrderType.STANDARD, WorkOrderType.fromText(null));
    }

    @Test
    void aNewWorkOrderIsStandard() {
        assertEquals(WorkOrderType.STANDARD, new WorkOrder(0, 1, 1).getType());
    }
}
