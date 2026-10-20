package com.wac.autocore.test;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.service.GarageSystem;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;

/** The everyday flow from a new customer to a paid invoice, through the services only. */
public final class FlowProof {

    private FlowProof() {}

    public static void main(String[] args) throws Exception {
        File database = File.createTempFile("flow-", ".db");
        database.deleteOnExit();
        database.delete();
        Db.use(database.getAbsolutePath());
        Db.ensureReady();

        GarageSystem garage = new GarageSystem(new SqliteRepositoryFactory());
        Mechanic mechanic = garage.getMechanics().get(0);
        ServiceItem service = garage.getServiceItems().get(0);
        LocalDate day = LocalDate.of(2027, 3, 3);

        Customer customer = garage.createCustomer("Flowprovets kund", "070-0000000", "flow@example.com");
        System.out.println("1. kund skapad, nummer " + customer.getId());

        Vehicle vehicle = garage.createVehicle("FLOW100", "Volvo", "V70", 2012, customer.getId());
        System.out.println("2. fordon skapat, nummer " + vehicle.getId());

        Booking booking = garage.createBooking(vehicle.getId(), day, "Flödesprov",
                LocalTime.of(9, 0), mechanic.getId(), service.getId());
        System.out.println("3. bokning skapad, nummer " + booking.getId() + ", status " + booking.getStatus());

        WorkOrder order = garage.createWorkOrder(booking.getId(), mechanic.getId(),
                Arrays.asList(service.getId()));
        System.out.println("4. arbetsorder skapad, nummer " + order.getId() + ", status " + order.getStatus());

        garage.confirmWorkOrder(order.getId());
        garage.startWorkOrder(order.getId());
        garage.completeWorkOrder(order.getId());
        WorkOrderStatus done = garage.findWorkOrder(order.getId()).getStatus();
        System.out.println("5. bekräftad, startad och slutförd: " + done);

        Invoice invoice = garage.createInvoice(order.getId(), null);
        System.out.println("6. faktura skapad, nummer " + invoice.getId()
                + ", totalt " + invoice.getTotalAmount());

        Payment payment = garage.processPayment(invoice.getId(), "CARD");
        System.out.println("7. betalning registrerad, nummer " + payment.getId()
                + ", belopp " + payment.getAmount());

        boolean flow = done == WorkOrderStatus.COMPLETED;
        boolean billed = invoice.getTotalAmount() > 0;
        // The payment carries the invoice total plus moms, so it is larger than the total.
        boolean paid = payment.getAmount() > invoice.getTotalAmount();
        System.out.println();
        System.out.println("arbetsordern gick hela vägen:     " + flow);
        System.out.println("fakturan har ett belopp:          " + billed);
        System.out.println("betalningen är större än totalen: " + paid);
        System.exit(flow && billed && paid ? 0 : 1);
    }
}
