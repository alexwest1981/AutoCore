package com.wac.autocore.ui.util;

import java.text.DecimalFormat;
import java.time.LocalDate;

/**
 * Hjälpmetoder för ren dataformatering och presentationslogik i gränssnittet.
 * Frikopplad från JavaFX-fönsterkontexten för enkel enhetstestning.
 */
public final class UiFormatters {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0");

    private UiFormatters() {}

    /**
     * Formaterar ett belopp med tusentalsavgränsare och valutatillägg, t.ex. "1,500 kr".
     */
    public static String formatMoney(long amount) {
        return MONEY_FORMAT.format(amount) + " kr";
    }

    /**
     * Formaterar ett double-belopp med tusentalsavgränsare och valutatillägg, t.ex. "1,500 kr".
     */
    public static String formatMoney(double amount) {
        return MONEY_FORMAT.format(amount) + " kr";
    }

    /**
     * Formaterar ett belopp med tusentalsavgränsare utan enhet, t.ex. "1,500".
     */
    public static String formatMoneyRaw(long amount) {
        return MONEY_FORMAT.format(amount);
    }

    /**
     * Formaterar ett double-belopp med tusentalsavgränsare utan enhet, t.ex. "1,500".
     */
    public static String formatMoneyRaw(double amount) {
        return MONEY_FORMAT.format(amount);
    }

    /**
     * Trunkerar en sträng till maxtecken och lägger till "…" om den klipps.
     */
    public static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /**
     * Returnerar dagens datum snyggt formaterat på engelska, t.ex. "Wednesday 16 September 2026".
     */
    public static String todayFormatted() {
        return formatDate(LocalDate.now());
    }

    /**
     * Formaterar ett givet LocalDate på engelska.
     */
    public static String formatDate(LocalDate d) {
        if (d == null) {
            return "";
        }
        String[] week = {"", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        String[] months = {"", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        return week[d.getDayOfWeek().getValue()] + " " + d.getDayOfMonth()
                + " " + months[d.getMonthValue()] + " " + d.getYear();
    }

    /**
     * Översätter en statuskod till ett läsvänligt visningsord.
     */
    public static String statusWord(String status) {
        if (status == null) {
            return "";
        }
        if ("BOOKED".equalsIgnoreCase(status)) {
            return "Booked";
        }
        if ("CREATED".equalsIgnoreCase(status)) {
            return "Created";
        }
        if ("WORK_ORDER_CREATED".equalsIgnoreCase(status)) {
            return "Work order created";
        }
        if ("IN_PROGRESS".equalsIgnoreCase(status)) {
            return "In progress";
        }
        if ("COMPLETED".equalsIgnoreCase(status)) {
            return "Completed";
        }
        return status;
    }

    /**
     * Returnerar true om statusen representerar ett positivt/klart tillstånd.
     */
    public static boolean isGood(String s) {
        if (s == null) {
            return false;
        }
        return s.equals("Yes") || s.equals("Successful")
                || s.equals("Completed") || s.equals("Paid");
    }

    /**
     * Returnerar CSS-klass för statusbadge ("success", "danger", "warn", "info" eller "").
     */
    public static String badgeClass(String s) {
        if (s == null) {
            return "";
        }
        if (isGood(s)) {
            return "success";
        }
        if (s.equals("No") || s.equals("Failed")) {
            return "danger";
        }
        if (s.equals("In progress")) {
            return "warn";
        }
        if (s.equals("Booked") || s.equals("Created") || s.equals("Work order created")) {
            return "info";
        }
        return "";
    }

    /**
     * Returnerar CSS-klass för statuspunkter i översiktspaneler.
     */
    public static String dotClass(String s) {
        String b = badgeClass(s);
        if (b.isEmpty()) {
            return "info";
        }
        return b;
    }
}
