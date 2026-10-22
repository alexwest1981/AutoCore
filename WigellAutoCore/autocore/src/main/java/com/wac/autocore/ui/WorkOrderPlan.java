package com.wac.autocore.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.wac.autocore.model.*;
import com.wac.autocore.service.GarageSystem;

/**
 * One entry per mechanic with that mechanic's services, handed out by qualification.
 * Used both for a booking and for a draft.
 */
public final class WorkOrderPlan {

    private WorkOrderPlan() {}

    /** The plan: one entry per mechanic with that mechanic's services. */
    public static LinkedHashMap<Integer, List<ServiceItem>> planWorkOrders(
            GarageSystem garage, Booking booking) {

        LinkedHashMap<Integer, List<ServiceItem>> plan = new LinkedHashMap<Integer, List<ServiceItem>>();
        if (booking == null) {
            return plan;
        }

        // The booking's mechanics, in the order they were chosen. The first one holds the time.
        List<Mechanic> team = new ArrayList<Mechanic>();
        for (Integer mechanicId : booking.getMechanicIds()) {
            Mechanic m = mechanicById(garage, mechanicId.intValue());
            if (m != null && !team.contains(m)) {
                team.add(m);
            }
        }
        if (team.isEmpty()) {
            team.add(null);
        }

        return planWorkOrders(garage, team, booking.getServiceItems());
    }

    /** The plan for a draft: the team and the services come from the form, not from a booking. */
    public static LinkedHashMap<Integer, List<ServiceItem>> planWorkOrders(
            GarageSystem garage, List<Mechanic> team, List<ServiceItem> services) {

        LinkedHashMap<Integer, List<ServiceItem>> plan = new LinkedHashMap<Integer, List<ServiceItem>>();

        if (services == null) {
            return plan;
        }

        for (ServiceItem service : services) {

            Mechanic who = mechanicForService(garage, team, service);

            if (who == null) {
                continue;
            }

            List<ServiceItem> mine = plan.get(Integer.valueOf(who.getId()));

            if (mine == null) {

                mine = new ArrayList<ServiceItem>();

                plan.put(Integer.valueOf(who.getId()), mine);
            }
            mine.add(service);
        }

        return plan;
    }
    /**
     * A booking that can get a work order now: booked or confirmed, with a service no active
     * order has taken. The dialog lists these and the menu counts them, so both say the same.
     */
    public static boolean waitsForAWorkOrder(GarageSystem garage, Booking booking) {
        BookingStatus status = booking.getStatus();
        return (status == BookingStatus.BOOKED || status == BookingStatus.CONFIRMED)
                && garage.hasServicesLeftForAWorkOrder(booking.getId());
    }

    /** A booking without services cannot become a work order. */
    static boolean hasServices(Booking booking) {
        return booking != null && !booking.getServiceItemIds().isEmpty();
    }

    /** The mechanic with the given id, or null. */
    static Mechanic mechanicById(GarageSystem garage, int mechanicId) {
        return garage.findMechanic(mechanicId);
    }

    /** The booking's mechanic if qualified, otherwise the first qualified one. */
    private static Mechanic mechanicForService(GarageSystem garage, List<Mechanic> team, ServiceItem service) {
        List<Mechanic> qualified = garage.getQualifiedMechanics(service);
        // First in line is the mechanic chosen first in the booking.
        for (Mechanic m : team) {
            if (m != null && containsId(qualified, m.getId())) {
                return m;
            }
        }
        return qualified.isEmpty() ? null : qualified.get(0);
    }

    private static boolean containsId(List<Mechanic> mechanics, int mechanicId) {
        for (Mechanic m : mechanics) {
            if (m.getId() == mechanicId) {
                return true;
            }
        }
        return false;
    }
}
