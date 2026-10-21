package com.wac.autocore.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wac.autocore.data.Db;
import com.wac.autocore.exception.RuleViolationException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.InvoiceRepository;
import com.wac.autocore.repository.InvoiceLineRepository;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.security.Identity;
import com.wac.autocore.security.IdentityHolder;
import com.wac.autocore.security.Role;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * SCRUM-286: an order that was created and called off by mistake can be removed, and nothing else
 * can. The three keys that point at a work order decide what is left, and the rule answers before
 * the database has to, so a person gets a sentence and not a technical error.
 */
class DeleteWorkOrderTest {

    private File file;

    private Db db;

    private GarageSystem garage;

    @BeforeEach
    void openADatabase() throws Exception {
        file = File.createTempFile("delete-work-order-", ".db");
        file.deleteOnExit();
        file.delete();

        db = new Db(file.getAbsolutePath());
        db.ensureReady();

        IdentityHolder identity = new IdentityHolder();
        identity.set(new Identity("chefen", Role.ADMIN));
        garage = new GuardedGarageSystem(new SqliteRepositoryFactory(db), identity);
    }

    @AfterEach
    void closeTheDatabase() {
        file.delete();
    }

    @Test
    void anOrderThatWasCalledOffIsRemovedWithItsServiceRows() throws Exception {

        WorkOrder order = anOrder();
        garage.cancelWorkOrder(order.getId());

        assertTrue(garage.canDeleteWorkOrder(order.getId()), "en avbruten order utan faktura ska gå att ta bort");
        assertEquals(1, serviceRowsOf(order.getId()));

        garage.deleteWorkOrder(order.getId());

        assertNull(garage.findWorkOrder(order.getId()), "ordern ska vara borta");
        assertEquals(0, serviceRowsOf(order.getId()), "tjänsteraderna ska följa med");
    }

    @Test
    void anOrderThatIsStillOpenIsNotRemoved() {

        WorkOrder order = anOrder();

        assertFalse(garage.canDeleteWorkOrder(order.getId()));

        RuleViolationException refused = assertThrows(RuleViolationException.class,
                () -> garage.deleteWorkOrder(order.getId()));

        assertEquals("dialog.workorder.delete.has_history", refused.getMessageKey());
        assertNotNull(garage.findWorkOrder(order.getId()), "en nekad radering lämnar raden kvar");
    }

    @Test
    void anOrderThatWasStartedIsNotRemoved() {

        WorkOrder order = anOrder();
        garage.confirmWorkOrder(order.getId());
        garage.startWorkOrder(order.getId());

        assertEquals("IN_PROGRESS", garage.findWorkOrder(order.getId()).getStatus().name());
        assertFalse(garage.canDeleteWorkOrder(order.getId()));
        assertThrows(RuleViolationException.class, () -> garage.deleteWorkOrder(order.getId()));
    }

    @Test
    void anOrderWithAnInvoiceIsNotRemoved() throws Exception {

        // The status rule alone would not catch this one, so the invoice check is tried on its own:
        // the order is called off and something still points at it.
        WorkOrder order = anOrder();
        garage.cancelWorkOrder(order.getId());

        InvoiceRepository invoices = new InvoiceRepository(db, new InvoiceLineRepository(db));
        Invoice invoice = new Invoice(0, order.getId(), LocalDate.now(), 10000);
        invoices.save(invoice);

        assertFalse(garage.canDeleteWorkOrder(order.getId()), "en fakturerad order får inte tas bort");
        assertThrows(RuleViolationException.class, () -> garage.deleteWorkOrder(order.getId()));
        assertNotNull(garage.findWorkOrder(order.getId()));
    }

    @Test
    void anOrderThatSomethingElsePointsAtIsKept() {

        // A reclamation is its own order and points back at the one it was made from. That order is
        // finished, so the status rule refuses it first; the reclamation check is there for the day
        // the status table changes, the same way the service item rule guards its key.
        WorkOrder original = anOrder();
        garage.confirmWorkOrder(original.getId());
        garage.startWorkOrder(original.getId());
        garage.completeWorkOrder(original.getId());

        WorkOrder reclamation = garage.createReclamation(original.getId(), "Reklamation i provet");
        assertNotNull(reclamation);

        assertFalse(garage.canDeleteWorkOrder(original.getId()));
        assertThrows(RuleViolationException.class, () -> garage.deleteWorkOrder(original.getId()));
        assertNotNull(garage.findWorkOrder(original.getId()));
    }

    private int serviceRowsOf(int workOrderId) throws Exception {

        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM work_order_service_items WHERE work_order_id = ?")) {
            statement.setInt(1, workOrderId);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }

    /** A booking far enough ahead, and the work order on it. */
    private WorkOrder anOrder() {

        Mechanic mechanic = garage.getMechanics().get(0);
        ServiceItem service = garage.getServiceItems().get(0);
        Vehicle vehicle = garage.getVehicles().get(0);

        LocalDate day = LocalDate.now().plusDays(40);
        Booking booking = garage.createBooking(vehicle.getId(), day, "Prov för radering",
                LocalTime.of(9, 0), mechanic.getId(), service.getId());

        return garage.createWorkOrder(booking.getId(), mechanic.getId());
    }
}
