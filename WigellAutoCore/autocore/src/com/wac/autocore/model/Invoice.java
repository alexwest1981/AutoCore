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

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
        calculateTotalAmount();
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

    public List<InvoiceLine> getLines() {
        return lines;
    }

    public void addLine(InvoiceLine line) {
        lines.add(line);
    }

    public double getLinesTotal() {
        double sum = 0.0;
        for (InvoiceLine line : lines) {
            sum += line.getFinalPrice();
        }
        return Math.round(sum * 100.0) / 100.0;
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