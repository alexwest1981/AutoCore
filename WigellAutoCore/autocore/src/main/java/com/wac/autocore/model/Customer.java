package com.wac.autocore.model;

public class Customer {

    private int id;
    private String name;
    private String phone;
    private String email;
    /** The billing address. The invoice must show who it is sent to, so it is part of the customer. */
    private String address = "";
    private String postalCode = "";
    private String city = "";
    private boolean vip;

    public Customer(int id, String name, String phone, String email) {
        this.id = id;
        this.name = name;
        this.phone = ContactRules.normalizePhone(phone);
        this.email = email;
        this.vip = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = ContactRules.normalizePhone(phone);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isVip() {
        return vip;
    }

    public void setVip(boolean vip) {
        this.vip = vip;
    }

    public String getAddress() {
        return address == null ? "" : address;
    }

    public void setAddress(String address) {
        this.address = address == null ? "" : address.trim();
    }

    public String getPostalCode() {
        return postalCode == null ? "" : postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode == null ? "" : postalCode.trim();
    }

    public String getCity() {
        return city == null ? "" : city;
    }

    public void setCity(String city) {
        this.city = city == null ? "" : city.trim();
    }

    /** Postcode and city on one line, or empty when neither is filled in. */
    public String getPostalAndCity() {
        String postal = getPostalCode();
        String town = getCity();
        if (postal.isEmpty()) {
            return town;
        }
        return town.isEmpty() ? postal : postal + " " + town;
    }

    /** Suffix in {@code dialog.validation.<suffix>}, or null when the row can be saved. */
    public static String validationProblem(String name, String phone, String email) {
        return ContactRules.problemWith(name, phone, email);
    }

    @Override
    public String toString() {
        return id + " - " + name +
                " | Phone: " + phone +
                " | Email: " + email +
                " | VIP: " + (vip ? "Yes" : "No");
    }
}