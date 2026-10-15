package com.wac.autocore.model;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

public class Invoice {

    private int id;
    private int workOrderId;
    private LocalDate invoiceDate;
    private int amount;
    private int discount;
    private int totalAmount;
    private boolean paid;
    private List<InvoiceLine> lines = new ArrayList<InvoiceLine>();

    public Invoice(int id, int workOrderId, LocalDate invoiceDate, int amount) {
        this.id = id;
        this.workOrderId = workOrderId;
        this.invoiceDate = invoiceDate;
        this.amount = amount;
        this.discount = 0;
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

    public int getAmount() {
        return amount;
    }

    public int getDiscount() {
        return discount;
    }

    public void setDiscount(int discount) {
        this.discount = discount;
        calculateTotalAmount();
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    private void calculateTotalAmount() {
        // Two amounts in öre, so the difference is exact and there is nothing to round.
        this.totalAmount = amount - discount;
    }

    /** The VAT is added on the invoice only, not in the tables. */
    public static final int VAT_PERCENT = 25;

    /** The VAT on what is to be paid, rounded to the nearest öre, half upward. */
    public int getVatAmount() {
        return (totalAmount * VAT_PERCENT + 50) / 100;
    }

    /** The amount due including VAT. The sum is exactly the two rows together. */
    public int getTotalIncludingVat() {
        return totalAmount + getVatAmount();
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
                " | Amount: " + amount + " öre" +
                " | Discount: " + discount + " öre" +
                " | Total: " + totalAmount + " öre" +
                " | Paid: " + (paid ? "Yes" : "No");
    }
}