package com.wac.autocore.test;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.InvoiceRepository;
import com.wac.autocore.repository.WorkOrderRepository;
import com.wac.autocore.service.GarageSystem;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Beviset för SCRUM-160 (D2): ett utfört arbete behåller priset som gällde när det utfördes.
 * Provet använder beställningens eget exempel: ett oljebyte för 899 kr som höjs till 999 kr.
 */
public class WorkOrderPriceFreezeTest {

    private static final double PRICE_WHEN_PERFORMED = 899.0;
    private static final double PRICE_AFTER_INCREASE = 999.0;

    public void testPriceIsFrozenWhenTheWorkIsPerformed() throws SQLException {
        GarageSystem garage = new GarageSystem();
        ServiceItem service = garage.getServiceItems().get(0);
        double catalogPrice = service.getPrice();

        Mechanic mechanic = garage.createMechanic("D2-mekaniker", "070-0000010", "Allmän service");
        Booking booking = bookingWithService(garage, service.getId(), "D2-prov, arbetet utförs");
        int orderId = 0;
        int invoiceId = 0;
        int laterOrderId = 0;
        int laterBookingId = 0;

        try {
            // Den 10 oktober: priset är 899 kr och arbetet utförs.
            service.setPrice(PRICE_WHEN_PERFORMED);
            garage.updateServiceItem(service);

            WorkOrder order = garage.createWorkOrder(booking.getId(), mechanic.getId());
            TestRunner.assertNotNull(order, "Arbetsordern ska skapas");
            orderId = order.getId();
            garage.startWorkOrder(orderId);
            garage.markServicesAsCompleted(orderId, new int[]{service.getId()});

            // Den 1 november: priset höjs till 999 kr.
            service.setPrice(PRICE_AFTER_INCREASE);
            garage.updateServiceItem(service);

            // Den gamla arbetsordern behåller priset från utförandet.
            WorkOrder oldOrder = new WorkOrderRepository().findById(orderId);
            TestRunner.assertNotNull(oldOrder, "Arbetsordern ska gå att läsa tillbaka");
            Double frozen = oldOrder.getCompletedServicePrice(service.getId());
            TestRunner.assertNotNull(frozen, "Arbetsordern ska ha sparat priset från utförandet");
            TestRunner.assertEquals(Double.valueOf(PRICE_WHEN_PERFORMED), frozen,
                    "Arbetsordern ska visa 899 kr, inte 999 kr");
            TestRunner.assertEquals(Double.valueOf(PRICE_WHEN_PERFORMED),
                    Double.valueOf(oldOrder.getCompletedServicePrice(service.getId()).doubleValue()),
                    "Priset ska stå kvar efter att tjänsten höjts i katalogen");

            // Fakturan byggs ur det frysta priset.
            garage.completeWorkOrder(orderId);
            Invoice invoice = garage.createInvoice(orderId, null);
            TestRunner.assertNotNull(invoice, "Fakturan ska kunna skapas");
            invoiceId = invoice.getId();

            Invoice readBack = new InvoiceRepository().findById(invoiceId);
            TestRunner.assertEquals(1, readBack.getLines().size(), "Fakturan ska ha en rad");
            TestRunner.assertEquals(Double.valueOf(PRICE_WHEN_PERFORMED),
                    Double.valueOf(readBack.getLines().get(0).getPrice()),
                    "Fakturaraden ska visa 899 kr, inte 999 kr");
            TestRunner.assertEquals(Double.valueOf(PRICE_WHEN_PERFORMED),
                    Double.valueOf(readBack.getAmount()),
                    "Fakturans belopp ska räknas ur det frysta priset");

            // En ny arbetsorder, efter höjningen, får det nya priset.
            Booking laterBooking = bookingWithService(garage, service.getId(), "D2-prov, arbetet utförs senare");
            laterBookingId = laterBooking.getId();
            WorkOrder laterOrder = garage.createWorkOrder(laterBookingId, mechanic.getId());
            TestRunner.assertNotNull(laterOrder, "Den nya arbetsordern ska skapas");
            laterOrderId = laterOrder.getId();
            garage.startWorkOrder(laterOrderId);
            garage.markServicesAsCompleted(laterOrderId, new int[]{service.getId()});

            Double newFrozen = new WorkOrderRepository().findById(laterOrderId)
                    .getCompletedServicePrice(service.getId());
            TestRunner.assertNotNull(newFrozen, "Även den nya arbetsordern ska ha ett fryst pris");
            TestRunner.assertEquals(Double.valueOf(PRICE_AFTER_INCREASE), newFrozen,
                    "En ny arbetsorder ska få det nya priset, 999 kr");

            System.out.println("    [SCRUM-160 BEVIS] Arbetsorder " + orderId + " utfördes för "
                    + PRICE_WHEN_PERFORMED + " kr och visar " + frozen + " kr även efter höjningen till "
                    + PRICE_AFTER_INCREASE + " kr. Faktura " + invoiceId + " visar "
                    + readBack.getAmount() + " kr. Ny arbetsorder " + laterOrderId + " fick "
                    + newFrozen + " kr.");
        } finally {
            if (invoiceId > 0) {
                new InvoiceRepository().delete(invoiceId);
            }
            if (orderId > 0) {
                new WorkOrderRepository().delete(orderId);
            }
            if (laterOrderId > 0) {
                new WorkOrderRepository().delete(laterOrderId);
            }
            new BookingRepository().delete(booking.getId());
            if (laterBookingId > 0) {
                new BookingRepository().delete(laterBookingId);
            }
            garage.deleteMechanic(mechanic.getId());
            service.setPrice(catalogPrice);
            garage.updateServiceItem(service);
        }
    }

    /** En arbetsorder som markerades utförd före D2 saknade pris, och ska då faktureras som förut. */
    public void testOrderWithoutFrozenPriceFallsBackToTheCatalog() throws SQLException {
        GarageSystem garage = new GarageSystem();
        ServiceItem service = garage.getServiceItems().get(0);
        Mechanic mechanic = garage.createMechanic("D2-äldre-mekaniker", "070-0000011", "Allmän service");
        Booking booking = bookingWithService(garage, service.getId(), "D2-prov, äldre arbetsorder");
        int orderId = 0;
        int invoiceId = 0;

        try {
            WorkOrder order = garage.createWorkOrder(booking.getId(), mechanic.getId());
            TestRunner.assertNotNull(order, "Arbetsordern ska skapas");
            orderId = order.getId();
            garage.startWorkOrder(orderId);

            // Markerad på det gamla sättet, utan pris, som en arbetsorder från före D2.
            WorkOrder loaded = new WorkOrderRepository().findById(orderId);
            loaded.markServiceAsCompleted(service.getId());
            new WorkOrderRepository().save(loaded);

            garage.completeWorkOrder(orderId);
            Invoice invoice = garage.createInvoice(orderId, null);
            TestRunner.assertNotNull(invoice, "Fakturan ska kunna skapas");
            invoiceId = invoice.getId();

            TestRunner.assertEquals(1, invoice.getLines().size(), "Fakturan ska ha en rad");
            TestRunner.assertEquals(Double.valueOf(service.getPrice()),
                    Double.valueOf(invoice.getLines().get(0).getPrice()),
                    "Utan fryst pris används katalogens pris, som före D2");

            System.out.println("    [SCRUM-160 BAKÅT] Arbetsorder utan fryst pris fakturerades med katalogens pris, "
                    + invoice.getLines().get(0).getPrice() + " kr.");
        } finally {
            if (invoiceId > 0) {
                new InvoiceRepository().delete(invoiceId);
            }
            if (orderId > 0) {
                new WorkOrderRepository().delete(orderId);
            }
            new BookingRepository().delete(booking.getId());
            garage.deleteMechanic(mechanic.getId());
        }
    }

    private Booking bookingWithService(GarageSystem garage, int serviceItemId, String text) throws SQLException {
        List<Vehicle> vehicles = garage.getVehicles();
        Booking booking = new Booking(0, vehicles.get(0).getId(), LocalDate.now().plusDays(12), text);
        List<Integer> ids = new ArrayList<Integer>();
        ids.add(serviceItemId);
        booking.setServiceItemIds(ids);
        new BookingRepository().save(booking);
        return booking;
    }
}
