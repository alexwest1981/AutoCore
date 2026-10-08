package com.wac.autocore.model;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

public class Invoice {

    private int id;
    private int workOrderId;
    private LocalDate invoiceDate;
    private double amount;
    private double discount;
    private double totalAmount;
    private boolean paid;
    private List<InvoiceLine> lines = new ArrayList<InvoiceLine>();

    public Invoice(int id, int workOrderId, LocalDate invoiceDate, double amount) {
        this.id = id;
        this.workOrderId = workOrderId;
        this.invoiceDate = invoiceDate;
        this.amount = amount;
        this.discount = 0.0;
        this.totalAmount = amount;
        this.paid = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(int workOrderId) {
        this.workOrderId = workOrderId;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public double getAmount() {
        return amount;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
        calculateTotalAmount();
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    private void calculateTotalAmount() {
        this.totalAmount = Math.round((amount - discount) * 100.0) / 100.0;
    }

    /** The VAT is added on the invoice only, not in the tables. */
    public static final double VAT_RATE = 0.25;

    /** The VAT on what is to be paid, rounded to the nearest öre. */
    public double getVatAmount() {
        return Math.round(this.totalAmount * VAT_RATE * 100.0) / 100.0;
    }

    /** The amount due including VAT. The sum is exactly the two rows together. */
    public double getTotalIncludingVat() {
        return Math.round((this.totalAmount + getVatAmount()) * 100.0) / 100.0;
    }

    public List<InvoiceLine> getLines() {
        return lines;
    }

    public void addLine(InvoiceLine line) {
        lines.add(line);
    }

    @Override
    public String toString() {
        return id +
                " - Work order ID: " + workOrderId +
                " | Date: " + invoiceDate +
                " | Amount: " + amount + " SEK" +
                " | Discount: " + discount + " SEK" +
                " | Total: " + totalAmount + " SEK" +
                " | Paid: " + (paid ? "Yes" : "No");
    }
}