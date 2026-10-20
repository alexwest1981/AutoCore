package com.wac.autocore.model;

import java.util.ArrayList;
import java.util.List;
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

    /** The fields the form must have, in the order it shows them. */
    public static List<String> missingFields(String registrationNumber, String brand, String model) {
        List<String> missing = new ArrayList<String>();

        if (registrationNumber == null || registrationNumber.trim().isEmpty()) {
            missing.add("reg_nr");
        }
        if (brand == null || brand.trim().isEmpty()) {
            missing.add("brand");
        }
        if (model == null || model.trim().isEmpty()) {
            missing.add("model");
        }

        return missing;
    }

    /** True when the text is a year the system accepts, that is 1900 to 2100. */
    public static boolean isReadableYear(String yearText) {
        if (yearText == null || yearText.trim().isEmpty()) {
            return false;
        }
        try {
            return isReadableYear(Integer.parseInt(yearText.trim()));
        } catch (NumberFormatException notANumber) {
            return false;
        }
    }

    /** The same rule when the year is already a number. */
    public static boolean isReadableYear(int year) {
        return year >= 1900 && year <= 2100;
    }

    /** The first rule the vehicle breaks, or null when the row can be saved. */
    public static String validationProblem(String registrationNumber, String brand, String model, int year) {

        if (!missingFields(registrationNumber, brand, model).isEmpty()) {
            return ContactRules.PROBLEM_REQUIRED;
        }

        if (!isReadableYear(year)) {
            return "invalid_year";
        }
        return null;
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
