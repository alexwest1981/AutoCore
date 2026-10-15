package com.wac.autocore.model;

public class InvoiceLine {

    private int id;
    private int invoiceId;
    private int serviceItemId;
    private String serviceName;
    /** The price in öre, like every other amount in the program. */
    private int price;
    private int discount;
    /** The package the service came from, or empty when it was booked on its own. */
    private String packageName = "";

    public InvoiceLine(int id, int invoiceId, int serviceItemId,
                       String serviceName, int price, int discount) {
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

    public int getPrice() {
        return price;
    }

    public int getDiscount() {
        return discount;
    }

    public void setDiscount(int discount) {
        this.discount = discount;
    }

    public String getPackageName() {
        return packageName == null ? "" : packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName == null ? "" : packageName;
    }

    public int getFinalPrice() {
        int finalPrice = price - discount;
        return finalPrice < 0 ? 0 : finalPrice;
    }

    @Override
    public String toString() {
        return serviceName +
                " | Price: " + price + " öre" +
                " | Discount: " + discount + " öre" +
                " | Final: " + getFinalPrice() + " öre";
    }
}