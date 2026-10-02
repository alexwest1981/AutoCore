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

    /**
     * Registreringsnumret sparas alltid i versaler, utan lösa mellanslag, och med ett mellanslag
     * mellan bokstäverna och siffrorna så att plåten går att läsa: "abc 123" blir "ABC 123".
     *
     * Mellanslaget sätts bara in när numret börjar med minst två bokstäver följt av en siffra,
     * så udda nummer som redan finns i systemet (till exempel testplåten G2V001) lämnas orörda.
     *
     * Normaliseringen ligger i modellen eftersom varje väg in — gränssnittet, tjänsten, seeddatan,
     * testen och uppläsningen ur databasen — går genom den här klassen. Låg den i ett formulär
     * skulle nästa väg in kringgå den, vilket är precis vad som hände med skydden för borttagning.
     */
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
