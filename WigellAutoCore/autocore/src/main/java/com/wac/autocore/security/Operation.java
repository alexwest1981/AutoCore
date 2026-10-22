package com.wac.autocore.security;

/**
 * What a signed-in person can ask the system to do. One name per kind of work, so the role matrix
 * has something to answer about and the guard (E3-4) has something to name.
 *
 * The names follow the methods of {@code GarageSystem}. Where several methods do the same kind of
 * work — the three ways to create a work order, the two ways to invoice — they share one operation.
 * Reading is a single operation: whoever may read, may read all of it.
 */
public enum Operation {

    /** The day's work: customers, vehicles, bookings, work orders, services, mechanics, prices. */
    READ,

    /**
     * The money: invoices, payments and what a booking can be invoiced for.
     *
     * Reading is otherwise one operation, because whoever may read one part of the workshop's day
     * may read all of it. The economy is the exception the team agreed on: a read-only account is
     * there to look things up, not to see what the workshop has charged and been paid.
     */
    READ_FINANCE,

    CREATE_CUSTOMER,
    UPDATE_CUSTOMER,
    DELETE_CUSTOMER,

    CREATE_VEHICLE,
    UPDATE_VEHICLE,
    DELETE_VEHICLE,

    CREATE_BOOKING,
    UPDATE_BOOKING,
    CANCEL_BOOKING,
    DELETE_BOOKING,

    CREATE_WORK_ORDER,
    UPDATE_WORK_ORDER,
    CHANGE_WORK_ORDER_STATUS,
    CANCEL_WORK_ORDER,

    /**
     * Removing an order that was called off by mistake. Separate from cancelling: cancelling is
     * part of the day's work and a staff member may do it, removing the row is not, and an
     * administrator is the one who answers for it.
     */
    DELETE_WORK_ORDER,

    CREATE_INVOICE,
    TAKE_PAYMENT,

    CREATE_MECHANIC,
    UPDATE_MECHANIC,
    DELETE_MECHANIC,

    CREATE_SERVICE_ITEM,
    UPDATE_SERVICE_ITEM,
    DELETE_SERVICE_ITEM,

    CREATE_SERVICE_PACKAGE,
    UPDATE_SERVICE_PACKAGE,
    DELETE_SERVICE_PACKAGE,

    CHANGE_SETTINGS,

    /**
     * Choosing the language. It is stored next to the settings but is not one: it says something
     * about the person looking at the window, not about how the workshop runs, so every role has it.
     */
    CHANGE_LANGUAGE
}
