package com.wac.autocore.model;

import java.time.LocalDateTime;

public class Payment {

    private int id;
    private int invoiceId;
    /** In öre, like every other amount in the program. */
    private int amount;
    private PaymentType paymentType;
    private LocalDateTime paymentDate;
    private boolean successful;

    public Payment(int id, int invoiceId, int amount, PaymentType paymentType) {
        this.id = id;
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.paymentType = paymentType;
        this.paymentDate = LocalDateTime.now();
        this.successful = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(int invoiceId) {
        this.invoiceId = invoiceId;
    }

    public int getAmount() {
        return amount;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    @Override
    public String toString() {
        return id +
                " - Invoice ID: " + invoiceId +
                " | Amount: " + amount + " öre" +
                " | Payment type: " + paymentType +
                " | Date: " + paymentDate +
                " | Successful: " + (successful ? "Yes" : "No");
    }
}