package com.wac.autocore.test;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.WorkOrderRepository;
import com.wac.autocore.service.GarageSystem;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Mätetalen på översikten, kontrollerade mot databasen i stället för mot sig själva.
 *
 * De tre proven var tidigare tautologier — "antalet är inte negativt", "antalet överstiger
 * inte antalet" — och kunde därför aldrig falla. Punkt 9 i genomgången (intäkten tappade
 * öret) gick rakt igenom den vakten. Proven jämför nu två oberoende vägar till samma tal,
 * eller följer ett tillstånd hela vägen i stället för att döma databasen som helhet.
 */
public class OverviewMetricsTest {

    public void testActiveWorkOrdersCalculation() throws SQLException {
        GarageSystem garage = new GarageSystem();
        List<WorkOrder> orders = garage.getWorkOrders();

        int activeInMemory = 0;
        for (int i = 0; i < orders.size(); i++) {
            if (!"COMPLETED".equalsIgnoreCase(orders.get(i).getStatus())) {
                activeInMemory++;
            }
        }

        int activeInDatabase = countFromDatabase(
                "SELECT COUNT(*) FROM work_orders WHERE status IS NULL OR status <> 'COMPLETED'");
        TestRunner.assertEquals(Integer.valueOf(activeInDatabase), Integer.valueOf(activeInMemory),
                "Antalet pågående arbetsordrar ska stämma med databasen, inte bara med listan i minnet");
    }

    /**
     * Intäkten räknas på två oberoende vägar: ur betalningarna och ur de betalda fakturorna.
     * Är de inte samma tal har ören tappats någonstans på vägen.
     */
    /**
     * Intäkten ska gå att följa hela vägen: det kunden betalar är fakturans belopp med moms, och
     * fakturans egna belopp är exklusive moms. Provet bygger sin egen kedja, så det säger något
     * även mot en tom databas, och kontrollerar sedan varje betalning mot sin faktura.
     */
    public void testTotalRevenueCalculation() throws SQLException {
        GarageSystem garage = new GarageSystem();
        Mechanic mechanic = null;
        int bookingId = 0;
        int orderId = 0;
        int invoiceId = 0;
        int paymentId = 0;

        try {
            ServiceItem service = garage.getServiceItems().get(0);
            mechanic = garage.createMechanic("Intäktsprov", "070-0000031", "Allmän service");

            Booking booking = new Booking(0, garage.getVehicles().get(0).getId(),
                    LocalDate.now().plusDays(21), "Intäktsprov");
            List<Integer> ids = new ArrayList<Integer>();
            ids.add(Integer.valueOf(service.getId()));
            booking.setServiceItemIds(ids);
            new BookingRepository().save(booking);
            bookingId = booking.getId();

            WorkOrder order = garage.createWorkOrder(bookingId, mechanic.getId());
            orderId = order.getId();
            garage.startWorkOrder(orderId);
            garage.markServicesAsCompleted(orderId, new int[]{service.getId()});
            garage.completeWorkOrder(orderId);

            Invoice invoice = garage.createInvoice(orderId, null);
            invoiceId = invoice.getId();
            Payment payment = garage.processPayment(invoiceId, "CARD");
            paymentId = payment.getId();

            TestRunner.assertEquals(Double.valueOf(round(invoice.getTotalIncludingVat())),
                    Double.valueOf(round(payment.getAmount())),
                    "Kunden ska betala fakturans belopp med moms, men betalningen var "
                            + payment.getAmount() + " kr och beloppet med moms är "
                            + invoice.getTotalIncludingVat() + " kr");
            System.out.println("    [INTÄKTSBEVIS] Faktura " + invoiceId + " på "
                    + invoice.getTotalAmount() + " kr exklusive moms ger " + invoice.getVatAmount()
                    + " kr moms och betalningen blev " + payment.getAmount() + " kr.");
        } finally {
            if (paymentId > 0) {
                new com.wac.autocore.repository.PaymentRepository().delete(paymentId);
            }
            if (invoiceId > 0) {
                new com.wac.autocore.repository.InvoiceRepository().delete(invoiceId);
            }
            if (orderId > 0) {
                new WorkOrderRepository().delete(orderId);
            }
            if (bookingId > 0) {
                new BookingRepository().delete(bookingId);
            }
            if (mechanic != null) {
                garage.deleteMechanic(mechanic.getId());
            }
        }

        // Och samma regel på allt som redan finns i databasen, faktura för faktura.
        int checked = 0;
        List<Payment> payments = garage.getPayments();
        List<Invoice> invoices = garage.getInvoices();
        for (int i = 0; i < invoices.size(); i++) {
            Invoice invoice = invoices.get(i);
            if (!invoice.isPaid()) {
                continue;
            }
            double paidOnThisInvoice = 0.0;
            for (int j = 0; j < payments.size(); j++) {
                Payment payment = payments.get(j);
                if (payment.isSuccessful() && payment.getInvoiceId() == invoice.getId()) {
                    paidOnThisInvoice += payment.getAmount();
                }
            }
            if (paidOnThisInvoice == 0.0) {
                continue;
            }
            TestRunner.assertTrue(round(paidOnThisInvoice) == round(invoice.getTotalAmount())
                            || round(paidOnThisInvoice) == round(invoice.getTotalIncludingVat()),
                    "Betalningen på faktura " + invoice.getId() + " ska vara beloppet exklusive moms ("
                            + invoice.getTotalAmount() + " kr) eller med moms ("
                            + invoice.getTotalIncludingVat() + " kr), men var "
                            + paidOnThisInvoice + " kr");
            checked++;
        }
        if (checked > 0) {
            System.out.println("    [INTÄKTSBEVIS] " + checked + " befintliga betalda fakturor "
                    + "kontrollerade mot sina betalningar.");
        }
    }

    /**
     * Tillståndet ska följa arbetet: mekanikern blir upptagen när arbetet startar och ledig när
     * det är klart. Provet följer sin egen mekaniker hela vägen i stället för att döma hela
     * databasen, så kvarglömd data från en avbruten körning inte får det att falla.
     */
    public void testMechanicAvailabilityFollowsTheWorkOrder() throws SQLException {
        GarageSystem garage = new GarageSystem();
        Vehicle vehicle = garage.getVehicles().get(0);
        ServiceItem service = garage.getServiceItems().get(0);
        Mechanic mechanic = garage.createMechanic("Revision-tillgänglighet", "070-0000029", "Allmän service");
        Booking booking = new Booking(0, vehicle.getId(), LocalDate.now().plusDays(21), "Revision: tillgänglighet");
        List<Integer> serviceIds = new ArrayList<Integer>();
        serviceIds.add(Integer.valueOf(service.getId()));
        booking.setServiceItemIds(serviceIds);
        new BookingRepository().save(booking);

        int orderId = 0;
        try {
            int mechanicId = mechanic.getId();
            TestRunner.assertTrue(available(garage, mechanicId), "En ny mekaniker ska stå som ledig");

            WorkOrder order = garage.createWorkOrder(booking.getId(), mechanicId);
            orderId = order.getId();
            garage.startWorkOrder(orderId);
            TestRunner.assertFalse(available(garage, mechanicId),
                    "Mekanikern ska stå som upptagen när arbetet är igång");

            garage.markServicesAsCompleted(orderId, new int[]{service.getId()});
            garage.completeWorkOrder(orderId);
            TestRunner.assertTrue(available(garage, mechanicId),
                    "Mekanikern ska stå som ledig igen när arbetet är klart");
        } finally {
            if (orderId > 0) {
                new WorkOrderRepository().delete(orderId);
            }
            new BookingRepository().delete(booking.getId());
            garage.deleteMechanic(mechanic.getId());
        }
    }

    private boolean available(GarageSystem garage, int mechanicId) {
        List<Mechanic> mechanics = garage.getMechanics();
        for (int i = 0; i < mechanics.size(); i++) {
            if (mechanics.get(i).getId() == mechanicId) {
                return mechanics.get(i).isAvailable();
            }
        }
        return false;
    }

    private int countFromDatabase(String sql) throws SQLException {
        Connection connection = Db.getConnection();
        try {
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery(sql);
            return result.next() ? result.getInt(1) : 0;
        } finally {
            connection.close();
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
