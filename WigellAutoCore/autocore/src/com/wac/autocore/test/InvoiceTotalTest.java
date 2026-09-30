package com.wac.autocore.test;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.InvoiceRepository;
import com.wac.autocore.repository.WorkOrderRepository;
import com.wac.autocore.service.GarageSystem;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * BEVIS för SCRUM-164 (E3): fakturans totalbelopp är summan av radernas slutpriser.
 * Kontrolleras på tre fakturor med en, två och tre tjänster.
 */
public class InvoiceTotalTest {

    private final GarageSystem garage = new GarageSystem();

    public void testTotalEqualsSumOfLinesOnThreeInvoices() throws SQLException {
        List<ServiceItem> services = garage.getServiceItems();
        TestRunner.assertTrue(services.size() >= 3, "sample data should contain at least three services");

        int vehicleId = findVehicleOfNonVipCustomer();
        TestRunner.assertTrue(vehicleId > 0, "sample data should contain a vehicle owned by a non-VIP customer");
        int mechanicId = garage.getMechanics().get(0).getId();

        for (int serviceCount = 1; serviceCount <= 3; serviceCount++) {
            Booking booking = garage.createBooking(vehicleId, LocalDate.now(), "Invoice total check");
            WorkOrder workOrder = new WorkOrder(0, booking.getId(), mechanicId);
            for (int i = 0; i < serviceCount; i++) {
                workOrder.addServiceItem(services.get(i).getId());
            }
            workOrder.setStatus("COMPLETED");
            WorkOrderRepository workOrderRepository = new WorkOrderRepository();
            workOrderRepository.save(workOrder);

            Invoice invoice = garage.createInvoice(workOrder.getId(), null);

            try {
                Invoice readBack = new InvoiceRepository().findById(invoice.getId());
                double linesTotal = readBack.getLinesTotal();

                System.out.println("    Invoice " + readBack.getId()
                        + " (" + readBack.getLines().size() + " lines): sum of lines = " + linesTotal
                        + " | invoice total = " + readBack.getTotalAmount());

                TestRunner.assertEquals(serviceCount, readBack.getLines().size(), "one line per service");
                TestRunner.assertEquals(linesTotal, readBack.getTotalAmount(), "the total should equal the sum of the lines");
            } finally {
                new InvoiceRepository().delete(invoice.getId());
                workOrderRepository.delete(workOrder.getId());
                new BookingRepository().delete(booking.getId());
            }
        }
    }

    private int findVehicleOfNonVipCustomer() {
        for (Vehicle vehicle : garage.getVehicles()) {
            for (Customer customer : garage.getCustomers()) {
                if (customer.getId() == vehicle.getCustomerId() && !customer.isVip()) {
                    return vehicle.getId();
                }
            }
        }
        return 0;
    }
}