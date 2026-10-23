package com.wac.autocore.service;

import com.wac.autocore.data.Db;
import com.wac.autocore.exception.RuleViolationException;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.security.Identity;
import com.wac.autocore.security.IdentityHolder;
import com.wac.autocore.security.Role;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorkOrderRulesTest {

    private File file;

    private Db db;

    private GarageSystem garage;

    @BeforeEach
    void buildDatabase() {
        try {
            file = File.createTempFile("workorder-rules-", ".db");
            file.deleteOnExit();
            file.delete();

            db = new Db(file.getAbsolutePath());
            db.ensureReady();

            IdentityHolder identity = new IdentityHolder();
            identity.set(new Identity("prov", Role.ADMIN));
            garage = new GuardedGarageSystem(new SqliteRepositoryFactory(db), identity);

        } catch (Exception cannotStart) {
            throw new IllegalStateException("Kunde inte bygga provets databas", cannotStart);
        }
    }

    @AfterEach
    void removeDatabase() {
        if (file != null) {
            file.delete();
        }
    }

    /** En egen bokning, så att inget från seedningen råkar ligga i vägen. */
    private Booking ownBooking() {
        int vehicleId = garage.getVehicles().get(0).getId();
        return garage.createBooking(vehicleId, LocalDate.now().plusDays(30), "Prov för arbetsorderns regler");
    }

    /** Mekanikerns id, och en kontroll att hen går att boka. */
    private int freeMechanic() {
        Mechanic mechanic = garage.getMechanics().get(0);
        mechanic.setAvailable(true);
        garage.updateMechanic(mechanic);
        return mechanic.getId();
    }

    private int aService() {
        return garage.getServiceItems().get(0).getId();
    }

    /** Två olika tjänster, så att två ordrar kan prövas mot samma bokning. */
    private List<Integer> twoServices() {
        List<ServiceItem> services = garage.getServiceItems();
        return List.of(services.get(0).getId(), services.get(1).getId());
    }

    @Test
    void anUnknownTypeIsRefused() {
        RuleViolationException refused = assertThrows(RuleViolationException.class,
                () -> garage.createWorkOrder(ownBooking().getId(), freeMechanic(), List.of(aService()), "PÅHITTAD"));

        assertEquals("dialog.workorder.unknown_type", refused.getMessageKey());
    }

    @Test
    void anOrderWithoutServicesIsRefused() {
        RuleViolationException refused = assertThrows(RuleViolationException.class,
                () -> garage.createWorkOrder(ownBooking().getId(), freeMechanic(), List.of()));

        assertEquals("dialog.workorder.no_services", refused.getMessageKey());
    }

    @Test
    void aServiceAlreadyOnAnOrderIsRefused() {
        Booking booking = ownBooking();
        int mechanicId = freeMechanic();
        int serviceItemId = aService();

        garage.createWorkOrder(booking.getId(), mechanicId, List.of(serviceItemId));

        RuleViolationException refused = assertThrows(RuleViolationException.class,
                () -> garage.createWorkOrder(booking.getId(), mechanicId, List.of(serviceItemId)));

        assertEquals("dialog.workorder.service_already_claimed", refused.getMessageKey());
    }

    @Test
    void aBusyMechanicIsRefused() {
        int mechanicId = freeMechanic();
        Mechanic mechanic = garage.findMechanic(mechanicId);
        mechanic.setAvailable(false);
        garage.updateMechanic(mechanic);

        RuleViolationException refused = assertThrows(RuleViolationException.class,
                () -> garage.createWorkOrder(ownBooking().getId(), mechanicId, List.of(aService())));

        assertEquals("dialog.workorder.mechanic_busy", refused.getMessageKey());
    }

    @Test
    void aValidOrderIsCreatedOnTheBooking() {
        Booking booking = ownBooking();
        int mechanicId = freeMechanic();
        int serviceItemId = aService();

        WorkOrder created = garage.createWorkOrder(booking.getId(), mechanicId, List.of(serviceItemId));

        assertTrue(created.getId() > 0);
        assertEquals(booking.getId(), created.getBookingId());
        assertEquals(booking.getVehicleId(), created.getVehicleId());
        assertEquals(mechanicId, created.getMechanicId());
        assertEquals(List.of(serviceItemId), created.getServiceItemIds());
        assertEquals(WorkOrderStatus.CREATED, created.getStatus());
        assertTrue(garage.getWorkOrders().stream()
                .anyMatch(order -> order.getId() == created.getId()));
    }

    @Test
    void anotherServiceOnTheSameBookingIsAllowed() {
        Booking booking = ownBooking();
        int mechanicId = freeMechanic();
        List<Integer> services = twoServices();

        WorkOrder first = garage.createWorkOrder(booking.getId(), mechanicId, List.of(services.get(0)));
        WorkOrder second = garage.createWorkOrder(booking.getId(), mechanicId, List.of(services.get(1)));

        assertNotEquals(first.getId(), second.getId());
        assertEquals(List.of(services.get(1)), second.getServiceItemIds());
    }

    @Test
    void aCancelledOrdersServiceCanBeUsedAgain() {
        Booking booking = ownBooking();
        int mechanicId = freeMechanic();
        int serviceItemId = aService();

        WorkOrder first = garage.createWorkOrder(booking.getId(), mechanicId, List.of(serviceItemId));
        assertTrue(garage.cancelWorkOrder(first.getId()));

        WorkOrder second = garage.createWorkOrder(booking.getId(), mechanicId, List.of(serviceItemId));

        assertNotEquals(first.getId(), second.getId());
        assertEquals(WorkOrderStatus.CANCELLED, garage.findWorkOrder(first.getId()).getStatus());
    }
    @Test
    void aCancelledReclamationLeavesTheBookingCompleted() {
        Booking booking = ownBooking();
        WorkOrder original = garage.createWorkOrder(booking.getId(), freeMechanic(), List.of(aService()));
        garage.confirmWorkOrder(original.getId());
        garage.startWorkOrder(original.getId());
        garage.completeWorkOrder(original.getId());

        WorkOrder reclamation = garage.createReclamation(original.getId(), "Reklamation i provet");
        garage.cancelWorkOrder(reclamation.getId());

        assertEquals(BookingStatus.COMPLETED, garage.findBooking(booking.getId()).getStatus());
    }

    @Test
    void cancellingOneOfTwoOrdersKeepsTheBooking() {
        Booking booking = ownBooking();
        int mechanicId = freeMechanic();
        List<Integer> services = twoServices();
        WorkOrder first = garage.createWorkOrder(booking.getId(), mechanicId, List.of(services.get(0)));
        garage.createWorkOrder(booking.getId(), mechanicId, List.of(services.get(1)));

        garage.cancelWorkOrder(first.getId());

        assertNotEquals(BookingStatus.CANCELLED, garage.findBooking(booking.getId()).getStatus());
    }

    @Test
    void cancellingTheOnlyOrderStillCancelsTheBooking() {
        Booking booking = ownBooking();
        WorkOrder only = garage.createWorkOrder(booking.getId(), freeMechanic(), List.of(aService()));

        garage.cancelWorkOrder(only.getId());

        assertEquals(BookingStatus.CANCELLED, garage.findBooking(booking.getId()).getStatus());
    }
    @Test
    void aCancelledOrderGivesTheBookingItsServicesBack() {
        ServiceItem service = garage.getServiceItems().get(0);
        Mechanic mechanic = garage.getQualifiedMechanics(service).get(0);
        mechanic.setAvailable(true);
        garage.updateMechanic(mechanic);
        LocalDate monday = LocalDate.now().plusDays(30).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        int vehicleId = garage.getVehicles().get(0).getId();

        Booking booking = garage.createBooking(vehicleId, monday, "Prov för en återställd bokning",
                LocalTime.of(8, 0), mechanic.getId(), service.getId());
        WorkOrder order = garage.createWorkOrder(booking.getId(), mechanic.getId());
        assertFalse(garage.hasServicesLeftForAWorkOrder(booking.getId()), "tjänsten sitter på en order");

        garage.cancelWorkOrder(order.getId());

        assertTrue(garage.hasServicesLeftForAWorkOrder(booking.getId()), "en avbruten order håller inte tjänsten");
    }
    /** A booked order on a Monday a month ahead, with a qualified mechanic. */
    private WorkOrder aBookedOrder() {
        ServiceItem service = garage.getServiceItems().get(0);
        Mechanic mechanic = garage.getQualifiedMechanics(service).get(0);
        mechanic.setAvailable(true);
        garage.updateMechanic(mechanic);
        LocalDate monday = LocalDate.now().plusDays(30).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        Booking booking = garage.createBooking(garage.getVehicles().get(0).getId(), monday,
                "Prov för att flytta en bokad order", LocalTime.of(8, 0), mechanic.getId(), service.getId());
        return garage.createWorkOrder(booking.getId(), mechanic.getId());
    }

    /** The same order with only the day and time changed, the way the edit dialog sends it. */
    private static DraftChanges sameOrderAt(WorkOrder order, LocalDate date, LocalTime time) {
        return DraftChanges.forWorkOrder(order.getId())
                .vehicle(order.getVehicleId())
                .description(order.getDescription())
                .mechanic(order.getMechanicId())
                .services(order.getServiceItemIds())
                .plannedAt(date, time)
                .build();
    }

    @Test
    void aNewTimeOnABookedOrderMovesTheBooking() {
        WorkOrder order = aBookedOrder();
        Booking before = garage.findBooking(order.getBookingId());
        LocalDate tuesday = before.getDate().plusDays(1);

        garage.updateDraft(sameOrderAt(order, tuesday, LocalTime.of(10, 0)));

        Booking after = garage.findBooking(order.getBookingId());
        assertEquals(tuesday, after.getDate());
        assertEquals(LocalTime.of(10, 0), after.getStartTime());
        assertEquals(java.time.Duration.between(before.getStartTime(), before.getEndTime()),
                java.time.Duration.between(after.getStartTime(), after.getEndTime()), "längden är densamma");

        assertTrue(garage.getSlotsForDay(order.getMechanicId(), tuesday).stream()
                        .anyMatch(slot -> slot.getBookingId() == order.getBookingId() && slot.getHour() == 10),
                "Kanban visar den nya tiden");
        assertTrue(garage.getSlotsForDay(order.getMechanicId(), before.getDate()).stream()
                        .noneMatch(slot -> slot.getBookingId() == order.getBookingId()),
                "den gamla tiden är släppt");
    }

    @Test
    void aBookedOrderCannotBeMovedToASaturday() {
        WorkOrder order = aBookedOrder();
        Booking before = garage.findBooking(order.getBookingId());
        LocalDate saturday = before.getDate().with(TemporalAdjusters.next(DayOfWeek.SATURDAY));

        RuleViolationException refused = assertThrows(RuleViolationException.class,
                () -> garage.updateDraft(sameOrderAt(order, saturday, LocalTime.of(10, 0))));

        assertEquals("dialog.booking.date_weekend", refused.getMessageKey());
        assertEquals(before.getDate(), garage.findBooking(order.getBookingId()).getDate(), "bokningen står kvar");
    }
}
