package com.wac.autocore.model;

public class InvoiceLine {

    private int id;
    private int invoiceId;
    private int serviceItemId;
    private String serviceName;
    private double price;
    private double discount;
    /** The package the service came from, or empty when it was booked on its own. */
    private String packageName = "";

    public InvoiceLine(int id, int invoiceId, int serviceItemId,
                       String serviceName, double price, double discount) {
        this.id = id;
        this.invoiceId = invoiceId;
        this.serviceItemId = serviceItemId;
        this.serviceName = serviceName;
        this.price = price;
        this.discount = discount;
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

    public int getServiceItemId() {
        return serviceItemId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public double getPrice() {
        return price;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public String getPackageName() {
        return packageName == null ? "" : packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName == null ? "" : packageName;
    }

    public double getFinalPrice() {
        double finalPrice = price - discount;
        return finalPrice < 0 ? 0 : finalPrice;
    }

    @Override
    public String toString() {
        return serviceName +
                " | Price: " + price + " SEK" +
                " | Discount: " + discount + " SEK" +
                " | Final: " + getFinalPrice() + " SEK";
    }
}