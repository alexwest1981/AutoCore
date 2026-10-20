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

    /** Every read there is: lists, lookups, prices, availability. */
    READ,

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

    CHANGE_SETTINGS
}
