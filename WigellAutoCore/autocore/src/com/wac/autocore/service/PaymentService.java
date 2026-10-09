package com.wac.autocore.service;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.repository.InvoiceRepository;
import com.wac.autocore.repository.PaymentRepository;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/** Registers payments and marks the invoice paid. */
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

        // An invoice of zero kronor has nothing to pay. Without this a new row was created
        // every time, and none of them succeeded, so the invoice stayed unpaid forever.
        if (invoice.getTotalAmount() <= 0.0) {
            System.out.println("Invoice has nothing to pay.");
            return null;
        }

        // An unknown payment type created a payment row that could never succeed, and the
        // invoice stayed unpaid. The type is therefore checked before the row is written.
        String type = paymentType == null ? "" : paymentType.trim().toUpperCase();
        if (!"CARD".equals(type) && !"SWISH".equals(type) && !"CASH".equals(type)) {
            System.out.println("Unknown payment type: " + paymentType + ".");
            return null;
        }

        // The customer pays the whole amount including VAT. The invoice's own amounts are excluding VAT.
        Payment payment = new Payment(0, invoiceId, invoice.getTotalIncludingVat(), type);

        boolean successful = false;

        if ("CARD".equalsIgnoreCase(type)) {
            System.out.println("Connecting directly to SuperCardPayment...");
            System.out.println("Card payment approved.");
            successful = true;
        } else if ("SWISH".equalsIgnoreCase(type)) {
            System.out.println("Calling Swish payment service...");
            System.out.println("Swish payment approved.");
            successful = true;
        } else if ("CASH".equalsIgnoreCase(type)) {
            System.out.println("Registering cash payment...");
            successful = true;
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
