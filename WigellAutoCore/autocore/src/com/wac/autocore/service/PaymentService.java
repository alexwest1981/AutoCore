package com.wac.autocore.service;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.repository.InvoiceRepository;
import com.wac.autocore.repository.PaymentRepository;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Service som hanterar genomförande och registrering av betalningar.
 *
 * Ansvarar för:
 * - Validering av fakturastatus (att fakturan finns och är obetald)
 * - Behandling av betalning via olika betalsätt
 * - Uppdatering av fakturans status till betald (paid = true)
 * - Registrering i betalningsjournalen
 */
public class PaymentService {

    private final PaymentRepository paymentRepository = new PaymentRepository();
    private final InvoiceRepository invoiceRepository = new InvoiceRepository();

    public List<Payment> getAll() {
        try {
            return paymentRepository.findAll();
        } catch (SQLException e) {
            System.out.println("Could not read payments: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public Payment findById(int id) {
        try {
            return paymentRepository.findById(id);
        } catch (SQLException e) {
            System.out.println("Could not read payment " + id + ": " + e.getMessage());
            return null;
        }
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

        Payment payment = new Payment(0, invoiceId, invoice.getTotalAmount(), paymentType);

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

        try {
            paymentRepository.save(payment);
        } catch (SQLException e) {
            System.out.println("Could not save payment: " + e.getMessage());
            return null;
        }

        if (successful) {
            invoice.setPaid(true);

            try {
                invoiceRepository.save(invoice);
            } catch (SQLException e) {
                System.out.println("Could not update invoice " + invoiceId + ": " + e.getMessage());
            }

            System.out.println("Payment completed successfully.");
            System.out.println("Sending payment confirmation to customer...");
            System.out.println("Confirmation sent.");
        } else {
            System.out.println("Payment failed.");
        }

        return payment;
    }

    private Invoice findInvoice(int id) {
        try {
            return invoiceRepository.findById(id);
        } catch (SQLException e) {
            System.out.println("Could not read invoice " + id + ": " + e.getMessage());
            return null;
        }
    }
}
