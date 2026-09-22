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
     * Formaterar ett givet LocalDate på aktivt språk (svenska eller engelska).
     */
    public static String formatDate(LocalDate d) {
        if (d == null) {
            return "";
        }
        if (com.wac.autocore.ui.i18n.I18n.isSwedish()) {
            String[] weekSv = {"", "Måndag", "Tisdag", "Onsdag", "Torsdag", "Fredag", "Lördag", "Söndag"};
            String[] monthsSv = {"", "januari", "februari", "mars", "april", "maj", "juni",
                    "juli", "augusti", "september", "oktober", "november", "december"};
            return weekSv[d.getDayOfWeek().getValue()] + " " + d.getDayOfMonth()
                    + " " + monthsSv[d.getMonthValue()] + " " + d.getYear();
        } else {
            String[] week = {"", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
            String[] months = {"", "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"};
            return week[d.getDayOfWeek().getValue()] + " " + d.getDayOfMonth()
                    + " " + months[d.getMonthValue()] + " " + d.getYear();
        }
    }

    /**
     * Översätter en statuskod till ett läsvänligt visningsord via I18n.
     */
    public static String statusWord(String status) {
        if (status == null) {
            return "";
        }
        if ("BOOKED".equalsIgnoreCase(status)) {
            return com.wac.autocore.ui.i18n.I18n.get("status.booked");
        }
        if ("CREATED".equalsIgnoreCase(status)) {
            return com.wac.autocore.ui.i18n.I18n.get("status.created");
        }
        if ("WORK_ORDER_CREATED".equalsIgnoreCase(status)) {
            return com.wac.autocore.ui.i18n.I18n.get("status.work_order_created");
        }
        if ("IN_PROGRESS".equalsIgnoreCase(status)) {
            return com.wac.autocore.ui.i18n.I18n.get("status.in_progress");
        }
        if ("COMPLETED".equalsIgnoreCase(status)) {
            return com.wac.autocore.ui.i18n.I18n.get("status.completed");
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
        return s.equals("Yes") || s.equals("Ja") || s.equals("Successful") || s.equals("Genomförd")
                || s.equals("Completed") || s.equals("Slutförd") || s.equals("Paid") || s.equals("Betald");
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
        if (s.equals("No") || s.equals("Nej") || s.equals("Failed") || s.equals("Misslyckad")) {
            return "danger";
        }
        if (s.equals("In progress") || s.equals("Pågående")) {
            return "warn";
        }
        if (s.equals("Booked") || s.equals("Bokad") || s.equals("Created") || s.equals("Skapad")
                || s.equals("Work order created") || s.equals("Arbetsorder skapad")) {
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
