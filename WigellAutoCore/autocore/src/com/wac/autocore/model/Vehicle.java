package com.wac.autocore.model;

import java.util.Locale;

public class Vehicle {

    private int id;
    private String registrationNumber;
    private String brand;
    private String model;
    private int year;
    private int customerId;

    public Vehicle(int id, String registrationNumber, String brand,
                   String model, int year, int customerId) {
        this.id = id;
        this.registrationNumber = normalizeRegistrationNumber(registrationNumber);
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.customerId = customerId;
    }

    /** Normalizes the registration number to capitals and a readable space. */
    public static String normalizeRegistrationNumber(String registrationNumber) {
        if (registrationNumber == null) {
            return null;
        }
        String letters = registrationNumber.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
        return letters.replaceFirst("^([A-ZÅÄÖ]{2,})(\\d.*)$", "$1 $2");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = normalizeRegistrationNumber(registrationNumber);
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    @Override
    public String toString() {
        return id + " - " +
                registrationNumber + " | " +
                brand + " " + model +
                " | Year: " + year +
                " | Customer ID: " + customerId;
    }
}
