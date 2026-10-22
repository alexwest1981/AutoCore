package com.wac.autocore.model;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

public class Invoice {

    /** The VAT is added on the invoice only, not in the tables. */
    public static final int VAT_PERCENT = 25;

    private int id;
    private int workOrderId;
    private LocalDate invoiceDate;
    /**
     * The number the invoice was issued with, in the year's series. Empty until the invoice is
     * stored, and never changed afterwards — a correction is a credit invoice, not a new number.
     */
    private String number = "";
    /** The invoice this one credits, or 0 for an ordinary invoice. */
    private int creditOf;
    private String creditReason = "";
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

    public String getNumber() {
        return number == null ? "" : number;
    }

    public void setNumber(String number) {
        this.number = number == null ? "" : number;
    }

    public int getCreditOf() {
        return creditOf;
    }

    public void setCreditOf(int creditOf) {
        this.creditOf = creditOf;
    }

    /** True when this invoice takes back an earlier one. Its amounts are then negative. */
    public boolean isCredit() {
        return creditOf > 0;
    }

    public String getCreditReason() {
        return creditReason == null ? "" : creditReason;
    }

    public void setCreditReason(String creditReason) {
        this.creditReason = creditReason == null ? "" : creditReason;
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

    /** The VAT on what is to be paid, rounded to the nearest öre, half upward. */
    public int getVatAmount() {
        // A credit invoice carries negative amounts and must give back exactly what was charged,
        // so the öre is rounded on the size and the sign is put back afterwards.
        int vat = (Math.abs(totalAmount) * VAT_PERCENT + 50) / 100;
        return totalAmount < 0 ? -vat : vat;
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
                " - Number: " + getNumber() +
                " - Work order ID: " + workOrderId +
                (isCredit() ? " | Credits: " + creditOf : "") +
                " | Date: " + invoiceDate +
                " | Amount: " + amount + " öre" +
                " | Discount: " + discount + " öre" +
                " | Total: " + totalAmount + " öre" +
                " | Paid: " + (paid ? "Yes" : "No");
    }
}
