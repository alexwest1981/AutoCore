package com.wac.autocore.service;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.repository.InvoiceRepository;
import com.wac.autocore.repository.PaymentRepository;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/** Registrerar betalningar och markerar fakturan betald. */
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

        // En faktura på noll kronor har inget att betala. Utan det här skapades en ny rad
        // varje gång, och ingen av dem blev lyckad, så fakturan stod kvar som obetald för alltid.
        if (invoice.getTotalAmount() <= 0.0) {
            System.out.println("Invoice has nothing to pay.");
            return null;
        }

        // En okänd betaltyp skapade en betalningsrad som aldrig kunde bli lyckad, och fakturan
        // stod kvar som obetald. Typen kontrolleras därför innan raden skrivs.
        String type = paymentType == null ? "" : paymentType.trim().toUpperCase();
        if (!"CARD".equals(type) && !"SWISH".equals(type) && !"CASH".equals(type)) {
            System.out.println("Unknown payment type: " + paymentType + ".");
            return null;
        }

        // Kunden betalar hela beloppet med moms. Fakturans egna belopp är exklusive moms.
        Payment payment = new Payment(0, invoiceId, invoice.getTotalIncludingVat(), paymentType);

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
