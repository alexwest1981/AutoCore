package com.wac.autocore.ui.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Helper methods for formatting data in the interface. */
public final class UiFormatters {

    private static final DecimalFormat MONEY_FORMAT =
            new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));

    /** Date and time without seconds, so a table row is not dominated by milliseconds. */
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private UiFormatters() {}

    /** Amount with a thousands separator, like 1 500 kr. */
    public static String formatMoney(double amount) {
        return MONEY_FORMAT.format(amount) + " kr";
    }

    /** Cuts the text to max characters and adds a full stop if it was shortened. */
    public static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /** Today's date in English, like Wednesday 16 September 2026. */
    public static String todayFormatted() {
        return formatDate(LocalDate.now());
    }

    /** The date in the active language. */
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

    /** Date and time for display, without seconds: 2026-10-01 21:30. */
    public static String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME_FORMAT);
    }

    /** The payment type as one word: SWISH becomes Swish. Unknown values are shown as stored. */
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

    /** The work order type as one word: RECLAMATION becomes Reclamation. Unknown values are shown
     *  as they were stored. */
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

    /** The status code as a display word, via I18n. */
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

    /** True if the status means finished. */
    public static boolean isGood(String s) {
        if (s == null) {
            return false;
        }
        return s.equals("Yes") || s.equals("Ja") || s.equals("Successful") || s.equals("Genomförd")
                || s.equals("Completed") || s.equals("Slutförd") || s.equals("Paid") || s.equals("Betald");
    }

    /** The CSS class for the status badge. */
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

    /** The CSS class for the status dot in the overview. */
    public static String dotClass(String s) {
        String b = badgeClass(s);
        if (b.isEmpty()) {
            return "info";
        }
        return b;
    }
}
