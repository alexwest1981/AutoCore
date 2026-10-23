package com.wac.autocore.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
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

/**
 * The decision in SCRUM-352: a work order may be completed without anyone having marked the work
 * as performed, and the invoice then covers every service on the order.
 *
 * It is allowed on purpose. A completed order says the work is done, and the marking is
 * bookkeeping on top of that; a workshop that finds nothing to do has cancelling as its answer,
 * and a cancelled order is not invoiced. Refusing in {@code completeWorkOrder} was the other way,
 * and it was turned down because it would make the window, the console and the standalone program
 * disagree about what a finished order is, and because the price freeze and the invoice already
 * give a defined answer.
 *
 * These tests pin the behaviour so a later change has to argue with them.
 */
class CompleteWithoutMarkingTest {

    private File file;

    private GarageSystem garage;

    private ServiceItem service;

    private WorkOrder order;

    @BeforeEach
    void anOrderThatIsReadyToFinish() {
        try {
            file = File.createTempFile("complete-without-marking-", ".db");
            file.deleteOnExit();
            file.delete();

            Db db = new Db(file.getAbsolutePath());
            db.ensureReady();

            IdentityHolder identity = new IdentityHolder();
            identity.set(new Identity("prov", Role.ADMIN));
            garage = new GuardedGarageSystem(new SqliteRepositoryFactory(db), identity);

            service = garage.getServiceItems().get(0);
            Booking booking = ownBooking();
            order = garage.createWorkOrder(booking.getId(), freeMechanic(),
                    List.of(service.getId()));

            garage.confirmWorkOrder(order.getId());
            garage.startWorkOrder(order.getId());
            garage.completeWorkOrder(order.getId());

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

    @Test
    void theOrderIsFinishedEvenThoughNothingWasMarked() {
        WorkOrder finished = garage.findWorkOrder(order.getId());

        assertEquals(WorkOrderStatus.COMPLETED, finished.getStatus());
        assertTrue(finished.getCompletedServiceItems().isEmpty(),
                "inget arbete markerades, och det ska synas att inget gjorde det");
    }

    @Test
    void theInvoiceCoversEveryServiceOnTheOrder() {
        Invoice invoice = garage.createInvoice(order.getId(), null);

        assertNotNull(invoice, "en slutförd order ska gå att fakturera");
        assertFalse(invoice.getLines().isEmpty(), "fakturan ska inte vara tom");
        assertEquals(List.of(Integer.valueOf(service.getId())),
                invoice.getLines().stream().map(line -> Integer.valueOf(line.getServiceItemId())).toList());
        assertEquals(service.getPrice(), invoice.getLines().get(0).getPrice());
        assertTrue(invoice.getTotalAmount() > 0);
    }

    @Test
    void thePriceIsTheOneThatStoodWhenTheOrderWasFinished() {
        int priceAtCompletion = service.getPrice();

        service.setPrice(priceAtCompletion + 50000);
        garage.updateServiceItem(service);

        Invoice invoice = garage.createInvoice(order.getId(), null);

        assertNotNull(invoice);
        InvoiceLine line = invoice.getLines().get(0);
        assertEquals(priceAtCompletion, line.getPrice(),
                "priset ska vara fryst när ordern slutförs, inte katalogens pris i efterhand");
    }

    /** A booking of its own, a weekday ahead, so nothing from the seed data is in the way. */
    private Booking ownBooking() {
        int vehicleId = garage.getVehicles().get(0).getId();
        LocalDate day = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        return garage.createBooking(vehicleId, day, "Prov för SCRUM-352",
                LocalTime.of(9, 0), freeMechanic(), service.getId());
    }

    private int freeMechanic() {
        for (com.wac.autocore.model.Mechanic mechanic : garage.getMechanics()) {
            if (mechanic.isAvailable()) {
                return mechanic.getId();
            }
        }
        throw new IllegalStateException("Ingen ledig mekaniker i provets databas");
    }
}
