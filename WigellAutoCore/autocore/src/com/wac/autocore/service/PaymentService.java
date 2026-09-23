package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;

import java.util.Collections;
import java.util.List;

/**
 * Service som hanterar genomförande och registrering av betalningar.
 *
 * Ansvarar för:
 * - Validering av fakturastatus (att fakturan finns och är obetald)
 * - Behandling av betalning via olika betalsätt (förberett för Strategy Pattern / PaymentMethod)
 * - Uppdatering av fakturans status till betald (paid = true)
 * - Registrering i betalningsjournalen
 */
public class PaymentService {

    public List<Payment> getAll() {
        return Collections.unmodifiableList(Database.getPayments());
    }

    public Payment findById(int id) {
        for (Payment payment : Database.getPayments()) {
            if (payment.getId() == id) {
                return payment;
            }
        }
        return null;
    }

    public Payment processPayment(int invoiceId, String paymentType) {
        Invoice invoice = findInvoice(invoiceId);
        if (invoice == null) {
            System.out.println("Invoice with ID " + invoiceId + " does not exist.");
            return null;
        }

        if (invoice.isPaid()) {
            System.out.println("Invoice has already been paid.");
            return null;
        }

        int id = Database.getPayments().size() + 1;
        Payment payment = new Payment(
                id,
                invoiceId,
                invoice.getTotalAmount(),
                paymentType
        );

        boolean successful = false;

        if ("CARD".equalsIgnoreCase(paymentType)) {
            System.out.println("Connecting directly to SuperCardPayment...");
            System.out.println("Card payment approved.");
            successful = true;
        } else if ("SWISH".equalsIgnoreCase(paymentType)) {
            System.out.println("Calling Swish payment service...");
            System.out.println("Swish payment approved.");
            successful = true;
        } else if ("CASH".equalsIgnoreCase(paymentType)) {
            System.out.println("Registering cash payment...");
            successful = true;
        } else {
            System.out.println("Unknown payment type.");
        }

        payment.setSuccessful(successful);
        Database.getPayments().add(payment);

        if (successful) {
            invoice.setPaid(true);
            System.out.println("Payment completed successfully.");
            System.out.println("Sending payment confirmation to customer...");
            System.out.println("Confirmation sent.");
        } else {
            System.out.println("Payment failed.");
        }

        return payment;
    }

    private Invoice findInvoice(int id) {
        for (Invoice invoice : Database.getInvoices()) {
            if (invoice.getId() == id) {
                return invoice;
            }
        }
        return null;
    }
}
