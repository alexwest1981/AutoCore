package com.wac.autocore.service;

import com.wac.autocore.exception.NotFoundException;
import com.wac.autocore.exception.DataAccessException;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.PaymentType;
import com.wac.autocore.repository.InvoiceRepository;
import com.wac.autocore.repository.PaymentRepository;

import java.util.List;

/** Registers payments and marks the invoice paid. */
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;

    public PaymentService(PaymentRepository paymentRepository, InvoiceRepository invoiceRepository) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public List<Payment> getAll() {
        try {
            return paymentRepository.findAll();
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read payments", e);
        }
    }

    public Payment processPayment(int invoiceId, PaymentType type) {
        Invoice invoice = findInvoice(invoiceId);
        if (invoice == null) {
            throw new NotFoundException("Invoice with ID " + invoiceId + " does not exist.");
        }

        if (invoice.isPaid()) {
            return null;
        }

        // An invoice of zero kronor has nothing to pay. Without this a new row was created
        // every time, and none of them succeeded, so the invoice stayed unpaid forever.
        if (invoice.getTotalAmount() <= 0.0) {
            return null;
        }

        // An unknown payment type created a payment row that could never succeed, and the
        // invoice stayed unpaid. The type is therefore checked before the row is written. The
        // text from a caller becomes a PaymentType before it gets here, so what is left to refuse
        // is the missing one.
        if (type == null) {
            return null;
        }

        // The customer pays the whole amount including VAT. The invoice's own amounts are excluding VAT.
        Payment payment = new Payment(0, invoiceId, invoice.getTotalIncludingVat(), type);

        // All three methods settle at once here; it is the type that differs, not the outcome.
        payment.setSuccessful(true);

        try {
            paymentRepository.save(payment);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not save payment", e);
        }

        invoice.setPaid(true);

        try {
            invoiceRepository.save(invoice);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not update invoice " + invoiceId, e);
        }

        return payment;
    }

    private Invoice findInvoice(int id) {
        try {
            return invoiceRepository.findById(id);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read invoice " + id, e);
        }
    }
}
