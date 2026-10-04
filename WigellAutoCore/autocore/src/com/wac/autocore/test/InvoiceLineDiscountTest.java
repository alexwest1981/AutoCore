package com.wac.autocore.test;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
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
 * BEVIS för SCRUM-166 (E5): rabatten fördelas på raderna, inget slutpris är negativt
 * och raderna summerar till fakturans total.
 */
public class InvoiceLineDiscountTest {

    private final GarageSystem garage = new GarageSystem();

    public void testPercentDiscountIsSpreadOnEveryLine() throws SQLException {
        List<ServiceItem> services = garage.getServiceItems();
        Invoice invoice = createInvoice(false, "WELCOME10", services.get(0), services.get(1), services.get(2));
        try {
            Invoice readBack = new InvoiceRepository().findById(invoice.getId());
            printLines("WELCOME10", readBack);

            for (InvoiceLine line : readBack.getLines()) {
                TestRunner.assertEquals(round(line.getPrice() * 0.10), line.getDiscount(), "each line should carry 10 %");
                TestRunner.assertTrue(line.getFinalPrice() >= 0, "a line should never be negative");
            }
            TestRunner.assertEquals(readBack.getTotalAmount(), readBack.getLinesTotal(), "the lines should sum to the invoice total");
        } finally {
            cleanUp(invoice);
        }
    }

    public void testFixedDiscountIsSpreadByPrice() throws SQLException {
        List<ServiceItem> services = garage.getServiceItems();
        Invoice invoice = createInvoice(false, "SERVICE200", services.get(0), services.get(1));
        try {
            Invoice readBack = new InvoiceRepository().findById(invoice.getId());
            printLines("SERVICE200", readBack);

            double discountOnLines = 0.0;
            for (InvoiceLine line : readBack.getLines()) {
                TestRunner.assertTrue(line.getDiscount() > 0, "every line should carry part of the 200 kr");
                TestRunner.assertTrue(line.getFinalPrice() >= 0, "a line should never be negative");
                discountOnLines += line.getDiscount();
            }
            TestRunner.assertEquals(200.0, round(discountOnLines), "the parts should add up to 200 kr");
            TestRunner.assertEquals(readBack.getTotalAmount(), readBack.getLinesTotal(), "the lines should sum to the invoice total");
        } finally {
            cleanUp(invoice);
        }
    }

    public void testNoLineBecomesNegative() throws SQLException {
        ServiceItem cheap = garage.createServiceItem("E5 cheap test service", "", 150.0, 10);
        Invoice invoice = null;
        try {
            invoice = createInvoice(false, "SERVICE200", cheap);
            Invoice readBack = new InvoiceRepository().findById(invoice.getId());
            printLines("SERVICE200 on 150 kr", readBack);

            InvoiceLine line = readBack.getLines().get(0);
            TestRunner.assertEquals(150.0, line.getDiscount(), "the discount should be capped at the line price");
            TestRunner.assertEquals(0.0, line.getFinalPrice(), "the final price should be 0, never negative");
            TestRunner.assertEquals(readBack.getTotalAmount(), readBack.getLinesTotal(), "the lines should sum to the invoice total");
        } finally {
            cleanUp(invoice);
            garage.deleteServiceItem(cheap.getId());
        }
    }

    // ---- hjälpmetoder ----

    private Invoice createInvoice(boolean vipCustomer, String discountCode, ServiceItem... services) throws SQLException {
        int vehicleId = findVehicle(vipCustomer);
        TestRunner.assertTrue(vehicleId > 0, "sample data should contain a vehicle for vip=" + vipCustomer);

        Booking booking = garage.createBooking(vehicleId, LocalDate.now(), "Line discount check");
        WorkOrder workOrder = new WorkOrder(0, booking.getId(), garage.getMechanics().get(0).getId());
        for (ServiceItem service : services) {
            workOrder.addServiceItem(service.getId());
        }
        workOrder.setStatus("COMPLETED");
        new WorkOrderRepository().save(workOrder);

        Invoice invoice = garage.createInvoice(workOrder.getId(), discountCode);
        TestRunner.assertNotNull(invoice, "the invoice should be created");
        return invoice;
    }

    private int findVehicle(boolean vipCustomer) {
        for (Vehicle vehicle : garage.getVehicles()) {
            for (Customer customer : garage.getCustomers()) {
                if (customer.getId() == vehicle.getCustomerId() && customer.isVip() == vipCustomer) {
                    return vehicle.getId();
                }
            }
        }
        return 0;
    }

    private void printLines(String rule, Invoice invoice) {
        System.out.println("    " + rule + " - invoice " + invoice.getId() + ":");
        for (InvoiceLine line : invoice.getLines()) {
            System.out.println("      " + SeedText.resolve(line.getServiceName())
                    + " | price " + line.getPrice()
                    + " | discount " + line.getDiscount()
                    + " | final " + line.getFinalPrice());
        }
        System.out.println("      Sum of lines: " + invoice.getLinesTotal()
                + " | invoice total: " + invoice.getTotalAmount());
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void cleanUp(Invoice invoice) throws SQLException {
        if (invoice == null) {
            return;
        }
        WorkOrder workOrder = new WorkOrderRepository().findById(invoice.getWorkOrderId());
        new InvoiceRepository().delete(invoice.getId());
        if (workOrder != null) {
            new WorkOrderRepository().delete(workOrder.getId());
            new BookingRepository().delete(workOrder.getBookingId());
        }
    }
}