package com.wac.autocore.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;

/**
 * Fördelningen av en boknings tjänster på arbetsordrar: en post per mekaniker
 * med den mekanikerns tjänster. Vilka tjänster som hamnar hos vem följer av
 * behörigheten — bokningens mekaniker används när hen är behörig, annars den som är det.
 */
public final class WorkOrderPlan {

    private WorkOrderPlan() {}

    /** Planen: en post per mekaniker med den mekanikerns tjänster. */
    public static LinkedHashMap<Integer, List<ServiceItem>> planWorkOrders(
            GarageSystem garage, Booking booking) {

        LinkedHashMap<Integer, List<ServiceItem>> plan = new LinkedHashMap<Integer, List<ServiceItem>>();
        if (booking == null) {
            return plan;
        }

        // Bokningens mekaniker, i den ordning de valdes. Den första är den som gäller tiden.
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

        for (ServiceItem service : booking.getServiceItems()) {
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

    /** Sant om bokningen har minst en tjänst som ingen arbetsorder tagit hand om. */
    static boolean hasServicesLeftForAWorkOrder(GarageSystem garage, Booking booking) {
        for (Integer serviceItemId : booking.getServiceItemIds()) {
            boolean claimed = false;
            for (WorkOrder order : garage.getWorkOrders()) {
                if (order.getBookingId() == booking.getId()
                        && order.getServiceItemIds().contains(serviceItemId)) {
                    claimed = true;
                    break;
                }
            }
            if (!claimed) {
                return true;
            }
        }
        return false;
    }

    /** En bokning utan tjänster kan inte bli en arbetsorder. */
    static boolean hasServices(Booking booking) {
        return booking != null && !booking.getServiceItemIds().isEmpty();
    }

    /** Mekanikern med angivet id, eller null. */
    static Mechanic mechanicById(GarageSystem garage, int mechanicId) {
        for (Mechanic m : garage.getMechanics()) {
            if (m.getId() == mechanicId) {
                return m;
            }
        }
        return null;
    }

    /** Bokningens mekaniker om hen är behörig, annars den första behöriga. */
    private static Mechanic mechanicForService(GarageSystem garage, List<Mechanic> team, ServiceItem service) {
        List<Mechanic> qualified = garage.getQualifiedMechanics(service);
        // Först i tur står mekanikern som valdes först i bokningen.
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
