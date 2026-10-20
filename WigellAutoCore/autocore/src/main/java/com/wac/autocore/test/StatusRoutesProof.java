package com.wac.autocore.test;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.DraftChanges;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.model.BookingStatus;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.exception.RuleViolationException;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;


/**
 * The four roads to a work order (booking, drop-in, reclamation, draft) answer the same status
 * changes the same way, and a refused change leaves nothing behind.
 */
public final class StatusRoutesProof {

    /** The same changes are tried on every road, in this order. A no is as much an answer as a yes. */
    private static final WorkOrderStatus[] SERIES = {
            WorkOrderStatus.IN_PROGRESS,
            WorkOrderStatus.CONFIRMED,
            WorkOrderStatus.COMPLETED,
            WorkOrderStatus.IN_PROGRESS,
            WorkOrderStatus.CANCELLED,
            WorkOrderStatus.IN_PROGRESS,
            WorkOrderStatus.COMPLETED,
            WorkOrderStatus.CANCELLED,
            WorkOrderStatus.CONFIRMED
    };

    private StatusRoutesProof() {}

    public static void main(String[] args) throws Exception {

        File database = File.createTempFile("status-routes-", ".db");
        database.deleteOnExit();
        // Nothing to copy: the program makes the database itself.
        database.delete();

        Db.use(database.getAbsolutePath());
        Db.ensureReady();

        GarageSystem garage = new GarageSystem(new SqliteRepositoryFactory());

        int vehicleId = garage.getVehicles().get(0).getId();
        Mechanic mechanic = garage.getMechanics().get(0);
        ServiceItem service = garage.getServiceItems().get(0);
        // A weekday far ahead, so the booked times never collide with the seed data.
        LocalDate day = LocalDate.of(2027, 3, 3);

        // The road's name is kept next to its order, in the order they are tried.
        Map<String, WorkOrder> roads = new LinkedHashMap<String, WorkOrder>();

        // A reclamation needs completed work to point at, so that order is run through first.
        WorkOrder original = fromBooking(garage, vehicleId, mechanic, service, day, LocalTime.of(8, 0));
        garage.confirmWorkOrder(original.getId());
        garage.startWorkOrder(original.getId());
        garage.completeWorkOrder(original.getId());

        roads.put("bokning", fromBooking(garage, vehicleId, mechanic, service, day, LocalTime.of(10, 0)));

        // A drop-in takes the current hour today: none after closing, and none at all on a weekend.
        Booking dropIn = null;
        try {
            dropIn = garage.createDropInBooking(vehicleId, Arrays.asList(service), Arrays.asList(mechanic));
        } catch (RuleViolationException closedToday) {
            // Same answer as no free hour: this road is not tried in this run.
        }
        if (dropIn != null) {
            roads.put("drop-in", garage.createWorkOrder(dropIn.getId(), mechanic.getId(),
                    Arrays.asList(service.getId())));
        } else {
            System.out.println("drop-in: ingen ledig tid kvar i dag, vägen prövas inte i den här körningen");
        }

        roads.put("reklamation", garage.createReclamation(original.getId(), "Reklamation i provet"));

        WorkOrder draft = garage.createDraft(vehicleId, "Utkast i provet");
        garage.updateDraft(DraftChanges.forWorkOrder(draft.getId())
                .vehicle(vehicleId)
                .description("Utkast i provet")
                .mechanic(mechanic.getId())
                .services(Arrays.asList(service.getId()))
                .plannedAt(day, LocalTime.of(13, 0))
                .customerInstructions("")
                .otherComments("")
                .build());
        roads.put("utkast", draft);


        System.out.println("Samma statusbyten på de fyra vägarna");
        System.out.println();

        List<String> first = null;
        boolean same = true;

        for (Map.Entry<String, WorkOrder> road : roads.entrySet()) {
            List<String> answers = run(garage, road.getValue().getId());
            System.out.println(road.getKey() + ": " + answers);

            if (first == null) {
                first = answers;
            } else if (!first.equals(answers)) {
                same = false;
            }
        }

        System.out.println();
        System.out.println("alla vägar svarar lika: " + same);


        // A no must leave the booking and the mechanic as they were, not only the order.
        System.out.println();
        boolean clean = refusalsLeaveNothing(garage, vehicleId, mechanic, service, day);
        System.out.println("ett nej lämnar inget efter sig: " + clean);

        System.exit(same && clean ? 0 : 1);
    }

    /** A booking with a time, and the work order made from it — the way the booking dialog does it. */
    private static WorkOrder fromBooking(GarageSystem garage, int vehicleId, Mechanic mechanic,
                                         ServiceItem service, LocalDate day, LocalTime time) {
        Booking booking = garage.createBooking(vehicleId, day, "Prov", time, mechanic.getId(), service.getId());
        return garage.createWorkOrder(booking.getId(), mechanic.getId(), Arrays.asList(service.getId()));
    }
    /** Tries the series on one order and writes down the answer to each change. */
    private static List<String> run(GarageSystem garage, int workOrderId) {
        List<String> answers = new ArrayList<String>();

        for (WorkOrderStatus next : SERIES) {
            WorkOrderStatus before = garage.findWorkOrder(workOrderId).getStatus();

            try {
                change(garage, workOrderId, next);
                answers.add(next + " ja");
            } catch (RuleViolationException refused) {
                // A no must leave the order where it was.
                boolean unchanged = garage.findWorkOrder(workOrderId).getStatus() == before;
                answers.add(next + (unchanged ? " nej" : " NEJ MEN ÄNDRAD"));
            }
        }

        return answers;
    }

    /** The button behind each status: the same calls the details dialog makes. */
    private static void change(GarageSystem garage, int workOrderId, WorkOrderStatus next) {
        switch (next) {
            case CONFIRMED:
                garage.confirmWorkOrder(workOrderId);
                break;
            case IN_PROGRESS:
                garage.startWorkOrder(workOrderId);
                break;
            case COMPLETED:
                garage.completeWorkOrder(workOrderId);
                break;
            case CANCELLED:
                garage.cancelWorkOrder(workOrderId);
                break;
            default:
                throw new IllegalArgumentException("Inget byte till " + next);
        }
    }
    /** Two refusals on a fresh order: start before it is confirmed, and cancel after it is completed. */
    private static boolean refusalsLeaveNothing(GarageSystem garage, int vehicleId, Mechanic mechanic,
                                                ServiceItem service, LocalDate day) {
        WorkOrder order = fromBooking(garage, vehicleId, mechanic, service, day, LocalTime.of(15, 0));
        boolean startClean = leavesNothing(garage, order, WorkOrderStatus.IN_PROGRESS);

        garage.confirmWorkOrder(order.getId());
        garage.startWorkOrder(order.getId());
        garage.completeWorkOrder(order.getId());
        boolean cancelClean = leavesNothing(garage, order, WorkOrderStatus.CANCELLED);

        return startClean && cancelClean;
    }

    /** Tries a change that must be refused, and compares the booking and the mechanic before and after. */
    private static boolean leavesNothing(GarageSystem garage, WorkOrder order, WorkOrderStatus next) {
        BookingStatus bookingBefore = garage.findBooking(order.getBookingId()).getStatus();
        boolean freeBefore = garage.findMechanic(order.getMechanicId()).isAvailable();

        try {
            change(garage, order.getId(), next);
            System.out.println("nej till " + next + ": FEL, bytet gick igenom");
            return false;
        } catch (RuleViolationException refused) {
            BookingStatus bookingAfter = garage.findBooking(order.getBookingId()).getStatus();
            boolean freeAfter = garage.findMechanic(order.getMechanicId()).isAvailable();

            System.out.println("nej till " + next + ": bokning " + bookingBefore + " -> " + bookingAfter
                    + ", mekaniker ledig " + freeBefore + " -> " + freeAfter);
            return bookingBefore == bookingAfter && freeBefore == freeAfter;
        }
    }
}