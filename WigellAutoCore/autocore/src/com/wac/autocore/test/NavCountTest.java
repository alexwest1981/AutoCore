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

        // Pipelinen genom menyn, i samma ordning användaren möter den:
        // 1. Bokning skapad: +1 på arbetsordrar, för den väntar på en arbetsorder. Inget märke på bokningar.
        Booking b = new Booking(1, 1, LocalDate.now(), "Revision: räknaren");
        List<Booking> pipelineBookings = new ArrayList<Booking>();
        pipelineBookings.add(b);
        List<WorkOrder> pipelineOrders = new ArrayList<WorkOrder>();
        List<Invoice> pipelineInvoices = new ArrayList<Invoice>();
        TestRunner.assertEquals(Integer.valueOf(1),
                Integer.valueOf(PageRouter.countBookingsWithoutWorkOrder(pipelineBookings, pipelineOrders)),
                "Ny bokning ger +1 på arbetsordrar, den väntar på en arbetsorder");

        // 2. Arbetsordern skapad: märket försvinner.
        WorkOrder wo = new WorkOrder(1, 1, 1);
        pipelineOrders.add(wo);
        TestRunner.assertEquals(Integer.valueOf(0),
                Integer.valueOf(PageRouter.countBookingsWithoutWorkOrder(pipelineBookings, pipelineOrders)),
                "Bokning med arbetsorder lämnar arbetsordermärket");

        // 3. Ordern slutförd: +1 på fakturor, för den väntar på att faktureras.
        wo.setStatus("IN_PROGRESS");
        TestRunner.assertEquals(Integer.valueOf(0),
                Integer.valueOf(PageRouter.countCompletedOrdersWithoutInvoice(pipelineOrders, pipelineInvoices)),
                "Pågående arbetsorder räknas inte, den är inte klar");
        wo.setStatus("COMPLETED");
        TestRunner.assertEquals(Integer.valueOf(1),
                Integer.valueOf(PageRouter.countCompletedOrdersWithoutInvoice(pipelineOrders, pipelineInvoices)),
                "Slutförd arbetsorder utan faktura ger +1 på fakturor");

        // 4. Fakturan skapad: fakturmärket försvinner, +1 på betalningar.
        Invoice inv = new Invoice(1, 1, LocalDate.now(), 1000.0);
        pipelineInvoices.add(inv);
        TestRunner.assertEquals(Integer.valueOf(0),
                Integer.valueOf(PageRouter.countCompletedOrdersWithoutInvoice(pipelineOrders, pipelineInvoices)),
                "Fakturerad arbetsorder lämnar fakturmärket");
        TestRunner.assertEquals(Integer.valueOf(1), Integer.valueOf(PageRouter.countUnpaidInvoices(pipelineInvoices)),
                "Obetald faktura ger +1 på betalningar");

        // 5. Betald: betalningsmärket försvinner.
        inv.setPaid(true);
        TestRunner.assertEquals(Integer.valueOf(0), Integer.valueOf(PageRouter.countUnpaidInvoices(pipelineInvoices)),
                "Betald faktura lämnar betalningsmärket");
    }

    /** En avbokad bokning ska inte be om en arbetsorder. */
    public void testCancelledBookingIsNotCounted() {
        List<Booking> bookings = new ArrayList<Booking>();
        Booking cancelled = new Booking(1, 1, LocalDate.now(), "Avbokad");
        cancelled.setStatus("CANCELLED");
        bookings.add(cancelled);
        bookings.add(new Booking(2, 1, LocalDate.now(), "Bokad"));

        TestRunner.assertEquals(Integer.valueOf(1),
                Integer.valueOf(PageRouter.countBookingsWithoutWorkOrder(bookings, new ArrayList<WorkOrder>())),
                "Bara den bokade bokningen ska räknas, den avbokade ska inte bli någon arbetsorder");
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
