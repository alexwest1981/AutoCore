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


/** Belopp med tusentalsavgränsare, som 1 500 kr. */
    public static String formatMoney(double amount) {
        return MONEY_FORMAT.format(amount) + " kr";
    }


/** Klipper texten till maxtecken och sätter punkt om den kortades. */
    public static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

/** Dagens datum på engelska, som Wednesday 16 September 2026. */
    public static String todayFormatted() {
        return formatDate(LocalDate.now());
    }

/** Datumet på aktivt språk. */
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

/** Datum och tid för visning, utan sekunder: 2026-10-01 21:30. */
    public static String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME_FORMAT);
    }

/** Betalsättet som ett ord: SWISH blir Swish. Okända värden visas som de sparades. */
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

/** Arbetsordertypen som ett ord: RECLAMATION blir Reklamation. Okända värden visas som de sparades. */
    public static String workOrderTypeWord(String stored) {
        if (stored == null || stored.trim().isEmpty()) {
            return "-";
        }
        String type = stored.trim().toUpperCase(Locale.US);
        if ("STANDARD".equals(type)) {
            return com.wac.autocore.ui.i18n.I18n.get("workorder_type.standard");
        }
        if ("RECLAMATION".equals(type)) {
            return com.wac.autocore.ui.i18n.I18n.get("workorder_type.reclamation");
        }
        if ("INTERNAL".equals(type)) {
            return com.wac.autocore.ui.i18n.I18n.get("workorder_type.internal");
        }
        return stored;
    }

/** Statuskoden som ett visningsord via I18n. */
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
        if ("CONFIRMED".equalsIgnoreCase(status)) {
            return com.wac.autocore.ui.i18n.I18n.get("status.confirmed");
        }
        if ("CANCELLED".equalsIgnoreCase(status)) {
            return com.wac.autocore.ui.i18n.I18n.get("status.cancelled");
        }

        return status;
    }

/** Sant om statusen betyder klart. */
    public static boolean isGood(String s) {
        if (s == null) {
            return false;
        }
        return s.equals("Yes") || s.equals("Ja") || s.equals("Successful") || s.equals("Genomförd")
                || s.equals("Completed") || s.equals("Slutförd") || s.equals("Paid") || s.equals("Betald");
    }

/** CSS-klassen för statusmärket. */
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

/** CSS-klassen för statuspricken i översikten. */
    public static String dotClass(String s) {
        String b = badgeClass(s);
        if (b.isEmpty()) {
            return "info";
        }
        return b;
    }
}
