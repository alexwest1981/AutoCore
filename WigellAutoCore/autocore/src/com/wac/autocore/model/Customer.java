package com.wac.autocore.model;

public class Customer {

    private int id;
    private String name;
    private String phone;
    private String email;
    private boolean vip;

    public Customer(int id, String name, String phone, String email) {
        this.id = id;
        this.name = name;
        this.phone = normalizePhone(phone);
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
        this.phone = normalizePhone(phone);
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

    /** Suffix i {@code dialog.validation.<suffix>}, eller null när raden går att spara. */
    public static String validationProblem(String name, String phone, String email) {
        return ContactRules.problemWith(name, phone, email);
    }

    /** Se {@link ContactRules#normalizePhone}. */
    public static String normalizePhone(String raw) {
        return ContactRules.normalizePhone(raw);
    }

    @Override
    public String toString() {
        return id + " - " + name +
                " | Phone: " + phone +
                " | Email: " + email +
                " | VIP: " + (vip ? "Yes" : "No");
    }
}