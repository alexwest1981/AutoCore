package com.wac.autocore.test;

import com.wac.autocore.data.Db;
import com.wac.autocore.exception.RuleViolationException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.InvoiceLineRepository;
import com.wac.autocore.repository.InvoiceRepository;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.security.Identity;
import com.wac.autocore.security.IdentityHolder;
import com.wac.autocore.security.Role;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.GuardedGarageSystem;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * SCRUM-286: an order that was created and called off by mistake can be removed, and nothing else.
 *
 * The four roads are printed one by one: the order that was called off goes away with its service
 * rows, and an order that is open, started or invoiced stays where it is. Exit 0 when every answer
 * is the one the rule promises, 1 otherwise.
 */
public final class DeleteWorkOrderProof {

    private DeleteWorkOrderProof() {}

    public static void main(String[] args) throws Exception {

        File database = File.createTempFile("delete-work-order-", ".db");
        database.deleteOnExit();
        database.delete();

        Db db = new Db(database.getAbsolutePath());
        db.ensureReady();

        IdentityHolder identity = new IdentityHolder();
        identity.set(new Identity("chefen", Role.ADMIN));
        GarageSystem garage = new GuardedGarageSystem(new SqliteRepositoryFactory(db), identity);

        boolean holds = true;

        System.out.println("SCRUM-286 · en order som avbröts av misstag går att ta bort");
        System.out.println("register: " + database.getAbsolutePath() + " (byggt av provet)");
        System.out.println();
        System.out.printf("%-34s %-10s %s%n", "väg", "regeln", "efteråt");

        WorkOrder cancelled = anOrder(garage, 0);
        garage.cancelWorkOrder(cancelled.getId());
        boolean allowed = garage.canDeleteWorkOrder(cancelled.getId());
        garage.deleteWorkOrder(cancelled.getId());
        boolean gone = garage.findWorkOrder(cancelled.getId()) == null;
        System.out.printf("%-34s %-10s %s%n", "avbruten, inget bakom", allowed ? "ja" : "nej",
                gone ? "borta" : "kvar");
        holds &= allowed && gone;

        WorkOrder open = anOrder(garage, 1);
        String openAnswer = tryDelete(garage, open.getId());
        System.out.printf("%-34s %-10s %s%n", "skapad men inte avbruten",
                garage.canDeleteWorkOrder(open.getId()) ? "ja" : "nej", openAnswer);
        holds &= !garage.canDeleteWorkOrder(open.getId()) && openAnswer.startsWith("nekad");

        WorkOrder started = anOrder(garage, 2);
        garage.confirmWorkOrder(started.getId());
        garage.startWorkOrder(started.getId());
        String startedAnswer = tryDelete(garage, started.getId());
        System.out.printf("%-34s %-10s %s%n", "påbörjad",
                garage.canDeleteWorkOrder(started.getId()) ? "ja" : "nej", startedAnswer);
        holds &= !garage.canDeleteWorkOrder(started.getId()) && startedAnswer.startsWith("nekad");

        WorkOrder invoiced = anOrder(garage, 3);
        garage.cancelWorkOrder(invoiced.getId());
        InvoiceRepository invoices = new InvoiceRepository(db, new InvoiceLineRepository(db));
        invoices.save(new Invoice(0, invoiced.getId(), LocalDate.now(), 10000));
        String invoicedAnswer = tryDelete(garage, invoiced.getId());
        System.out.printf("%-34s %-10s %s%n", "avbruten men fakturerad",
                garage.canDeleteWorkOrder(invoiced.getId()) ? "ja" : "nej", invoicedAnswer);
        holds &= !garage.canDeleteWorkOrder(invoiced.getId()) && invoicedAnswer.startsWith("nekad");

        System.out.println();
        System.out.println("en nekad radering lämnar raden kvar: " + (garage.findWorkOrder(open.getId()) != null));
        holds &= garage.findWorkOrder(open.getId()) != null;
        System.out.println("stämmer med kraven: " + holds);

        System.exit(holds ? 0 : 1);
    }

    private static String tryDelete(GarageSystem garage, int workOrderId) {
        try {
            garage.deleteWorkOrder(workOrderId);
            return "borta";
        } catch (RuleViolationException refused) {
            return "nekad (" + refused.getMessageKey() + ")";
        }
    }

    private static WorkOrder anOrder(GarageSystem garage, int slot) {

        // A different mechanic per road, so one busy day cannot hide the next answer.
        java.util.List<Mechanic> mechanics = garage.getMechanics();
        Mechanic mechanic = mechanics.get(slot % mechanics.size());
        ServiceItem service = garage.getServiceItems().get(0);
        Vehicle vehicle = garage.getVehicles().get(0);
        LocalTime start = LocalTime.of(8 + slot, 0);

        // Closed days and busy hours are part of the workshop's rules, and this program is not here
        // to test them: it looks for the first day that takes the booking.
        for (int daysAhead = 4; daysAhead < 40; daysAhead++) {
            try {
                Booking booking = garage.createBooking(vehicle.getId(), LocalDate.now().plusDays(daysAhead),
                        "Prov för radering", start, mechanic.getId(), service.getId());
                return garage.createWorkOrder(booking.getId(), mechanic.getId());
            } catch (RuleViolationException closedOrBusy) {
                // Try the next day.
            }
        }

        throw new IllegalStateException("Provet kunde inte skapa en arbetsorder att pröva raderingen på.");
    }
}
