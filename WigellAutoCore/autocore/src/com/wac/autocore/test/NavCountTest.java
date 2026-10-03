package com.wac.autocore.test;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.ui.navigation.PageRouter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * MENYN: räknaren på menyvalen ska visa det som väntar på att bli hanterat, inte allt som finns.
 *
 * Räknaren är den enda ledtråden om att det kommit in något nytt medan man stått på en annan sida,
 * så den får varken visa för mycket (då slutar man lita på den) eller för lite (då missas jobbet).
 */
public class NavCountTest {

    public void testOnlyUnstartedWorkOrdersAreCounted() {
        List<WorkOrder> orders = new ArrayList<WorkOrder>();
        orders.add(order("CREATED"));
        orders.add(order("IN_PROGRESS"));
        orders.add(order("COMPLETED"));

        WorkOrder withoutStatus = order("CREATED");
        withoutStatus.setStatus(null);
        orders.add(withoutStatus);

        TestRunner.assertEquals(Integer.valueOf(2), Integer.valueOf(PageRouter.countNewWorkOrders(orders)),
                "Bara aktiva arbetsordrar (CREATED eller IN_PROGRESS) som inte slutförts ska räknas — "
                        + "slutförda och statuslösa är klara eller okända");
        TestRunner.assertEquals(Integer.valueOf(0), Integer.valueOf(PageRouter.countNewWorkOrders(new ArrayList<WorkOrder>())),
                "En tom lista ska ge noll, inte ett negativt eller påhittat antal");
    }

    public void testOnlyBookingsWithoutWorkOrderAreCounted() {
        List<Booking> bookings = new ArrayList<Booking>();
        bookings.add(booking("BOOKED"));
        bookings.add(booking("WORK_ORDER_CREATED"));
        bookings.add(booking("IN_PROGRESS"));
        bookings.add(booking("COMPLETED"));
        bookings.add(booking("CANCELLED"));

        TestRunner.assertEquals(Integer.valueOf(1), Integer.valueOf(PageRouter.countNewBookings(bookings)),
                "Bara bokningar som ingen arbetsorder skapats ur ska räknas som nya — resten är "
                        + "påbörjade, klara eller avbokade");
    }

    public void testOnlyUnpaidInvoicesAndFailedPaymentsAreCounted() {
        List<Invoice> invoices = new ArrayList<Invoice>();
        invoices.add(invoice(false));
        invoices.add(invoice(true));
        invoices.add(invoice(true));

        TestRunner.assertEquals(Integer.valueOf(1), Integer.valueOf(PageRouter.countUnpaidInvoices(invoices)),
                "Bara obetalda fakturor ska räknas — en betald faktura är redan hanterad");

        List<Payment> payments = new ArrayList<Payment>();
        payments.add(payment(false));
        payments.add(payment(true));
        payments.add(payment(true));

        TestRunner.assertEquals(Integer.valueOf(2), Integer.valueOf(PageRouter.countPaidPayments(payments)),
                "Betalda fakturor / genomförda betalningar ger +1 i betalningsmenyn");
        TestRunner.assertEquals(Integer.valueOf(1), Integer.valueOf(PageRouter.countFailedPayments(payments)),
                "Bakåtkompatibilitet: countFailedPayments räknar misslyckade betalningar");

        // Verifierar hela pipelinen steg för steg:
        // 1. Skapa bokning (+1 på bokningar)
        List<Booking> pipelineBookings = new ArrayList<Booking>();
        Booking b = booking("BOOKED");
        pipelineBookings.add(b);
        TestRunner.assertEquals(Integer.valueOf(1), Integer.valueOf(PageRouter.countNewBookings(pipelineBookings)),
                "Skapa bokning ger +1 i sidebar");

        // 2. Bokning fått workorder (-1 på bokningar, +1 på workorders)
        b.setStatus("WORK_ORDER_CREATED");
        List<WorkOrder> pipelineOrders = new ArrayList<WorkOrder>();
        WorkOrder wo = order("CREATED");
        pipelineOrders.add(wo);
        TestRunner.assertEquals(Integer.valueOf(0), Integer.valueOf(PageRouter.countNewBookings(pipelineBookings)),
                "Bokning med arbetsorder lämnar bokningsräknaren");
        TestRunner.assertEquals(Integer.valueOf(1), Integer.valueOf(PageRouter.countNewWorkOrders(pipelineOrders)),
                "Bokning fått workorder ger +1 i workorder sidebar");

        // 3. Workorder klar skapar faktura (-1 på workorders, +1 på fakturor)
        wo.setStatus("COMPLETED");
        TestRunner.assertEquals(Integer.valueOf(0), Integer.valueOf(PageRouter.countNewWorkOrders(pipelineOrders)),
                "Klar workorder lämnar workorder-räknaren");
        List<Invoice> pipelineInvoices = new ArrayList<Invoice>();
        Invoice inv = invoice(false);
        pipelineInvoices.add(inv);
        TestRunner.assertEquals(Integer.valueOf(1), Integer.valueOf(PageRouter.countUnpaidInvoices(pipelineInvoices)),
                "Workorder klar skapar faktura och ger +1 i faktura sidebar");

        // 4. Betald faktura (-1 på fakturor, +1 på payments)
        inv.setPaid(true);
        TestRunner.assertEquals(Integer.valueOf(0), Integer.valueOf(PageRouter.countUnpaidInvoices(pipelineInvoices)),
                "Betald faktura lämnar obetalda fakturor");
        List<Payment> pipelinePayments = new ArrayList<Payment>();
        pipelinePayments.add(payment(true));
        TestRunner.assertEquals(Integer.valueOf(1), Integer.valueOf(PageRouter.countPaidPayments(pipelinePayments)),
                "Betald faktura ger +1 i payment sidebar");
    }

    private Invoice invoice(boolean paid) {
        Invoice invoice = new Invoice(0, 1, LocalDate.now(), 1000.0);
        invoice.setPaid(paid);
        return invoice;
    }

    private Payment payment(boolean successful) {
        Payment payment = new Payment(0, 1, 1000.0, "SWISH");
        payment.setSuccessful(successful);
        return payment;
    }

    private WorkOrder order(String status) {
        WorkOrder order = new WorkOrder(0, 1, 1);
        order.setStatus(status);
        return order;
    }

    private Booking booking(String status) {
        Booking booking = new Booking(0, 1, LocalDate.now(), "Revision: räknaren");
        booking.setStatus(status);
        return booking;
    }
}
