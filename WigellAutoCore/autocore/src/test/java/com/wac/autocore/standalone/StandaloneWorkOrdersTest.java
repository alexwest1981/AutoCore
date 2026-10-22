package com.wac.autocore.standalone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.wac.autocore.data.Db;
import com.wac.autocore.exception.AccessDeniedException;
import com.wac.autocore.exception.NotAuthenticatedException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.security.Identity;
import com.wac.autocore.security.IdentityHolder;
import com.wac.autocore.security.Role;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.GuardedGarageSystem;

/** The program the task asks for, checked the same way a person would: run it, then read the row. */
class StandaloneWorkOrdersTest {

    @Test
    void finishesAWorkOrderWithoutSpring() throws Exception {

        Path file = Files.createTempFile("autocore-standalone-", ".db");
        Files.deleteIfExists(file);

        StandaloneWorkOrders.main(new String[] { file.toString() });

        Db db = new Db(file.toString());
        db.ensureReady();
        GarageSystem garage = new GarageSystem(new SqliteRepositoryFactory(db));

        WorkOrder newest = newest(garage.getWorkOrders());

        assertNotNull(newest);
        assertEquals(WorkOrderStatus.COMPLETED, newest.getStatus());
    }

    @Test
    void refusesAWriteForAReadOnlyAccount() throws Exception {

        Path file = Files.createTempFile("autocore-refused-", ".db");
        Files.deleteIfExists(file);

        Db db = new Db(file.toString());
        db.ensureReady();

        IdentityHolder identity = new IdentityHolder();
        identity.set(new Identity("lasare", Role.READ_ONLY));

        GarageSystem garage = new GuardedGarageSystem(new SqliteRepositoryFactory(db), identity);

        assertThrows(AccessDeniedException.class,
                () -> garage.createCustomer("Nekad kund", "070-0000000", "nekad@exempel.com"));

        identity.set(new Identity("personal", Role.STAFF));

        assertNotNull(garage.createCustomer("Tillåten kund", "070-0000001", "tillaten@exempel.com"));
    }

    @Test
    void refusesAWriteWhenNobodyIsSignedIn() throws Exception {

        Path file = Files.createTempFile("autocore-anonymous-", ".db");
        Files.deleteIfExists(file);

        Db db = new Db(file.toString());
        db.ensureReady();

        GarageSystem garage = new GuardedGarageSystem(new SqliteRepositoryFactory(db), new IdentityHolder());

        assertThrows(NotAuthenticatedException.class,
                () -> garage.createCustomer("Anonym kund", "070-0000002", "anonym@exempel.com"));
    }

    @Test
    void theDayIsTheFirstOneTheWorkshopHasOpen() {

        // 2027-03-05 is a Friday, 6 and 7 are the weekend.
        assertEquals(LocalDate.of(2027, 3, 5),
                StandaloneWorkOrders.firstOpenDayOnOrAfter(LocalDate.of(2027, 3, 5)));

        assertEquals(LocalDate.of(2027, 3, 8),
                StandaloneWorkOrders.firstOpenDayOnOrAfter(LocalDate.of(2027, 3, 6)));

        assertEquals(LocalDate.of(2027, 3, 8),
                StandaloneWorkOrders.firstOpenDayOnOrAfter(LocalDate.of(2027, 3, 7)));
    }

    @Test
    void aBookingLandsOnAnOpenDayEvenWhenTheFirstTryIsASaturday() throws Exception {

        Path file = Files.createTempFile("autocore-open-day-", ".db");
        Files.deleteIfExists(file);

        Db db = new Db(file.toString());
        db.ensureReady();

        IdentityHolder identity = new IdentityHolder();
        identity.set(new Identity("fristaende", Role.STAFF));

        GarageSystem garage = new GuardedGarageSystem(new SqliteRepositoryFactory(db), identity);

        // The morning the suite went red: 22 October plus thirty days was a Saturday.
        Booking booking = StandaloneWorkOrders.bookOnFirstOpenDay(garage,
                garage.getVehicles().get(0).getId(),
                garage.getMechanics().get(0),
                garage.getServiceItems().get(0),
                LocalDate.of(2026, 11, 21));

        assertNotNull(booking);
        assertEquals(LocalDate.of(2026, 11, 23), booking.getDate());
    }

    private static WorkOrder newest(List<WorkOrder> orders) {

        WorkOrder newest = null;

        for (WorkOrder order : orders) {

            if (newest == null || order.getId() > newest.getId()) {
                newest = order;
            }
        }

        return newest;
    }
}
