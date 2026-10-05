package com.wac.autocore.ui.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Hjälpmetoder för dataformatering i gränssnittet. */
public final class UiFormatters {

    private static final DecimalFormat MONEY_FORMAT =
            new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));

    /** Datum och tid utan sekunder, så en tabellrad inte domineras av millisekunder. */
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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
     * Datum och tid som den visas i gränssnittet, utan sekunder och millisekunder:
     * "2026-10-01 21:30" i stället för "2026-10-01T21:30:15.862".
     */
    public static String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME_FORMAT);
    }

    /**
     * Betalsättet som ett läsvänligt ord: det sparade värdet "SWISH" visas som "Swish". Ett okänt
     * betalsätt visas som det sparades, så en ny typ inte blir tom i tabellen.
     */
    public static String paymentTypeWord(String stored) {
        if (stored == null || stored.trim().isEmpty()) {
            return "-";
        }
        String type = stored.trim().toUpperCase(Locale.US);
        if ("SWISH".equals(type)) {
            return com.wac.autocore.ui.i18n.I18n.get("dialog.payment.method_swish");
        }
        if ("CARD".equals(type)) {
            return com.wac.autocore.ui.i18n.I18n.get("dialog.payment.method_card");
        }
        if ("CASH".equals(type)) {
            return com.wac.autocore.ui.i18n.I18n.get("dialog.payment.method_cash");
        }
        return stored;
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
