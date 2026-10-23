package com.wac.autocore.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
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
 * What a job cost when it was done stays what it cost, whatever the price list says later.
 *
 * WAC 2.5, kriterium 8: en prisändring påverkas inte av tidigare utförda arbeten eller gamla
 * fakturor. Priset fryses när ordern slutförs, och en faktura som redan är skriven rör sig inte
 * alls när katalogen ändras efteråt.
 */
class PriceFreezeTest {

    private File file;

    private GarageSystem garage;

    private ServiceItem service;

    /** Each job in a test gets its own Monday, so two orders never fight over the same hour. */
    private int jobs;

    @BeforeEach
    void aDatabaseWithOneService() {
        try {
            file = File.createTempFile("price-freeze-", ".db");
            file.deleteOnExit();
            file.delete();

            Db db = new Db(file.getAbsolutePath());
            db.ensureReady();

            IdentityHolder identity = new IdentityHolder();
            identity.set(new Identity("prov", Role.ADMIN));
            garage = new GuardedGarageSystem(new SqliteRepositoryFactory(db), identity);
            service = garage.getServiceItems().get(0);

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
    void theInvoiceUsesThePriceFromWhenTheWorkWasFinished() {
        int priceWhenFinished = service.getPrice();
        WorkOrder order = completedOrder();

        changeCataloguePrice(priceWhenFinished + 50000);

        Invoice invoice = garage.createInvoice(order.getId(), null);

        assertNotNull(invoice);
        assertEquals(priceWhenFinished, invoice.getLines().get(0).getPrice(),
                "fakturan ska använda priset som frystes när ordern slutfördes");
    }

    @Test
    void anInvoiceAlreadyWrittenDoesNotMoveWhenThePriceChanges() {
        WorkOrder order = completedOrder();
        Invoice invoice = garage.createInvoice(order.getId(), null);
        assertNotNull(invoice);

        int priceOnInvoice = invoice.getLines().get(0).getPrice();
        changeCataloguePrice(priceOnInvoice + 100000);

        Invoice again = garage.findInvoice(invoice.getId());
        assertEquals(priceOnInvoice, again.getLines().get(0).getPrice(),
                "en skriven faktura ska stå still");
        assertEquals(priceOnInvoice, again.getTotalAmount());
    }

    @Test
    void theNewPriceAppliesToTheNextJob() {
        int oldPrice = service.getPrice();
        completedOrder();

        changeCataloguePrice(oldPrice + 50000);
        WorkOrder next = completedOrder();

        Invoice invoice = garage.createInvoice(next.getId(), null);

        assertNotNull(invoice);
        assertEquals(oldPrice + 50000, invoice.getLines().get(0).getPrice(),
                "en ny order ska få det nya priset");
    }

    /** The catalogue price in öre, changed the way the administration changes it. */
    private void changeCataloguePrice(int price) {
        ServiceItem current = garage.findServiceItem(service.getId());
        current.setPrice(price);
        garage.updateServiceItem(current);
        service = current;
    }

    /** A booking of its own with one service, driven through to completed. */
    private WorkOrder completedOrder() {
        int vehicleId = garage.getVehicles().get(0).getId();
        LocalDate day = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                .plusWeeks(jobs++);

        Booking booking = garage.createBooking(vehicleId, day, "Prov för prisfrysning",
                LocalTime.of(9, 0), freeMechanic(), service.getId());
        WorkOrder order = garage.createWorkOrder(booking.getId(), freeMechanic(),
                List.of(service.getId()));

        garage.confirmWorkOrder(order.getId());
        garage.startWorkOrder(order.getId());
        garage.completeWorkOrder(order.getId());

        return garage.findWorkOrder(order.getId());
    }

    private int freeMechanic() {
        for (Mechanic mechanic : garage.getMechanics()) {
            if (mechanic.isAvailable()) {
                return mechanic.getId();
            }
        }
        throw new IllegalStateException("Ingen ledig mekaniker i provets databas");
    }
}
