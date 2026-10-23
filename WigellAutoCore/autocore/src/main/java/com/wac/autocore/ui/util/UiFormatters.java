package com.wac.autocore.ui.util;


import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.model.BookingStatus;


import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

/** Helper methods for formatting data in the interface. */
public final class UiFormatters {

    private static final DecimalFormat MONEY_FORMAT =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.of("sv", "SE")));

    /** An amount the way a person writes it: 1234,50 without spaces, or 1 234,50 in threes. */
    private static final Pattern AMOUNT = Pattern.compile(
            "\\d{1,3}(?: \\d{3})+(?:[.,]\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?");

    /** Date and time without seconds, so a table row is not dominated by milliseconds. */
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private UiFormatters() {}

    /** Amount in öre as kronor in Swedish style, like 1 295,00 kr. */
    public static String formatMoney(int ore) {
        return MONEY_FORMAT.format(BigDecimal.valueOf(ore).movePointLeft(2)) + " kr";
    }

    /** A typed amount as öre, or null when the text is not an amount. The way in from a field. */
    public static Integer parseOre(String text) {
        if (text == null) {
            return null;
        }
        String clean = text.replace('\u00a0', ' ').trim();
        if (clean.toLowerCase(Locale.ROOT).endsWith("kr")) {
            clean = clean.substring(0, clean.length() - 2).trim();
        }
        if (!AMOUNT.matcher(clean).matches()) {
            return null;
        }
        try {
            return Integer.valueOf(new BigDecimal(clean.replace(" ", "").replace(",", "."))
                    .setScale(2, java.math.RoundingMode.HALF_UP).movePointRight(2).intValueExact());
        } catch (ArithmeticException tooBig) {
            return null;
        }
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

    /** Same as above, for a work order status. */
    public static String statusWord(WorkOrderStatus status) {
        return status == null ? "" : statusWord(status.name());
    }
    /** Same as above, for a booking status. */
    public static String statusWord(BookingStatus status) {
        return status == null ? "" : statusWord(status.name());
    }

    /**
     * What a status chip shows: the word for the reader, and the colour that belongs to the value.
     *
     * The two travel together on purpose. The colour is decided by the status itself, never by the
     * word, so a new wording or a new language cannot change what a row looks like.
     */
    public record Badge(String text, String styleClass) {}

    /** The badge for a booking's status. */
    public static Badge badge(BookingStatus status) {
        if (status == null) {
            return new Badge("", "");
        }
        return new Badge(statusWord(status), badgeClass(status));
    }

    /** The badge for a work order's status. */
    public static Badge badge(WorkOrderStatus status) {
        if (status == null) {
            return new Badge("", "");
        }
        return new Badge(statusWord(status), badgeClass(status));
    }

    /**
     * The badge for a flag that is either settled or not — paid, successful, available, VIP. The
     * words are the caller's, since the same yes means different things in different columns.
     */
    public static Badge badge(boolean settled, String whenTrueKey, String whenFalseKey) {
        return new Badge(
                com.wac.autocore.ui.i18n.I18n.get(settled ? whenTrueKey : whenFalseKey),
                settled ? "success" : "danger");
    }

    /** The CSS class for a booking status. Every status has one; none falls back to a default. */
    public static String badgeClass(BookingStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case COMPLETED -> "success";
            case IN_PROGRESS -> "warn";
            case BOOKED, CONFIRMED, WORK_ORDER_CREATED, CANCELLED -> "info";
        };
    }

    /** The CSS class for a work order status. Every status has one; none falls back to a default. */
    public static String badgeClass(WorkOrderStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case COMPLETED -> "success";
            case IN_PROGRESS -> "warn";
            case CREATED, CONFIRMED, CANCELLED -> "info";
        };
    }

    /** The CSS class for the status dot in the overview, which is never colourless. */
    public static String dotClass(BookingStatus status) {
        String colour = badgeClass(status);
        return colour.isEmpty() ? "info" : colour;
    }
}
