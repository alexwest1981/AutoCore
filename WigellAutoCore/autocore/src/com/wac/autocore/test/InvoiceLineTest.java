package com.wac.autocore.test;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.InvoiceRepository;
import com.wac.autocore.repository.WorkOrderRepository;
import com.wac.autocore.seed.SeedText;
import com.wac.autocore.service.GarageSystem;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * BEVIS för SCRUM-162 (E1): en faktura med flera tjänster får en rad per tjänst,
 * och varje rad har tjänstenamn, pris, rabatt och slutpris.
 */
public class InvoiceLineTest {

    private final GarageSystem garage = new GarageSystem();

    public void testOneLinePerPerformedService() throws SQLException {
        List<Vehicle> vehicles = garage.getVehicles();
        List<ServiceItem> services = garage.getServiceItems();
        List<Mechanic> mechanics = garage.getMechanics();
        TestRunner.assertTrue(!vehicles.isEmpty(), "sample data should contain vehicles");
        TestRunner.assertTrue(services.size() >= 2, "sample data should contain at least two services");
        TestRunner.assertTrue(!mechanics.isEmpty(), "sample data should contain mechanics");

        ServiceItem s1 = services.get(0);
        ServiceItem s2 = services.get(1);

        Booking booking = garage.createBooking(vehicles.get(0).getId(), LocalDate.now(), "Invoice line check");

        WorkOrder workOrder = new WorkOrder(0, booking.getId(), mechanics.get(0).getId());
        workOrder.addServiceItem(s1.getId());
        workOrder.addServiceItem(s2.getId());
        workOrder.setStatus("COMPLETED");
        WorkOrderRepository workOrderRepository = new WorkOrderRepository();
        workOrderRepository.save(workOrder);

        Invoice invoice = garage.createInvoice(workOrder.getId(), null);

        try {
            TestRunner.assertNotNull(invoice, "the invoice should be created");
            TestRunner.assertEquals(2, invoice.getLines().size(), "one line per performed service");

            System.out.println("    Invoice " + invoice.getId() + ":");
            for (InvoiceLine line : invoice.getLines()) {
                System.out.println("      " + SeedText.resolve(line.getServiceName())
                        + " | price " + line.getPrice()
                        + " | discount " + line.getDiscount()
                        + " | final " + line.getFinalPrice());
            }

            InvoiceLine first = invoice.getLines().get(0);
            InvoiceLine second = invoice.getLines().get(1);
            TestRunner.assertEquals(s1.getName(), first.getServiceName(), "line 1 should carry the service name");
            TestRunner.assertEquals(s1.getPrice(), first.getPrice(), "line 1 should carry the service price");
            TestRunner.assertEquals(s2.getName(), second.getServiceName(), "line 2 should carry the service name");
            TestRunner.assertEquals(s2.getPrice(), second.getPrice(), "line 2 should carry the service price");
            TestRunner.assertEquals(invoice.getId(), first.getInvoiceId(), "the line should point to its invoice");
        } finally {
            if (invoice != null) {
                new InvoiceRepository().delete(invoice.getId());
            }
            workOrderRepository.delete(workOrder.getId());
            new BookingRepository().delete(booking.getId());
        }
    }
}