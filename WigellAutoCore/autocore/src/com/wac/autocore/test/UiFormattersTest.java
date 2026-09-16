package com.wac.autocore.test;

import com.wac.autocore.ui.util.UiFormatters;

import java.time.LocalDate;

public class UiFormattersTest {

    public void testFormatMoneyLong() {
        TestRunner.assertEquals("1,500 kr", UiFormatters.formatMoney(1500L), "Format 1500L");
        TestRunner.assertEquals("0 kr", UiFormatters.formatMoney(0L), "Format 0L");
        TestRunner.assertEquals("1,000,000 kr", UiFormatters.formatMoney(1000000L), "Format 1M");
    }

    public void testFormatMoneyDouble() {
        TestRunner.assertEquals("2,450 kr", UiFormatters.formatMoney(2450.0), "Format 2450.0");
        TestRunner.assertEquals("99 kr", UiFormatters.formatMoney(99.0), "Format 99.0");
    }

    public void testFormatMoneyRaw() {
        TestRunner.assertEquals("5,200", UiFormatters.formatMoneyRaw(5200L), "Format raw 5200");
    }

    public void testTruncate() {
        TestRunner.assertEquals("", UiFormatters.truncate(null, 10), "Null truncate");
        TestRunner.assertEquals("Kort text", UiFormatters.truncate("Kort text", 20), "Short truncate");
        TestRunner.assertEquals("Kort…", UiFormatters.truncate("Kort text", 5), "Truncated with ellipsis");
    }

    public void testStatusWord() {
        TestRunner.assertEquals("Booked", UiFormatters.statusWord("BOOKED"), "BOOKED translation");
        TestRunner.assertEquals("Created", UiFormatters.statusWord("CREATED"), "CREATED translation");
        TestRunner.assertEquals("Work order created", UiFormatters.statusWord("WORK_ORDER_CREATED"), "WORK_ORDER_CREATED");
        TestRunner.assertEquals("In progress", UiFormatters.statusWord("IN_PROGRESS"), "IN_PROGRESS translation");
        TestRunner.assertEquals("Completed", UiFormatters.statusWord("COMPLETED"), "COMPLETED translation");
        TestRunner.assertEquals("Custom", UiFormatters.statusWord("Custom"), "Custom untouched");
    }

    public void testBadgeClass() {
        TestRunner.assertEquals("success", UiFormatters.badgeClass("Yes"), "Yes -> success");
        TestRunner.assertEquals("success", UiFormatters.badgeClass("Completed"), "Completed -> success");
        TestRunner.assertEquals("success", UiFormatters.badgeClass("Paid"), "Paid -> success");
        TestRunner.assertEquals("danger", UiFormatters.badgeClass("No"), "No -> danger");
        TestRunner.assertEquals("danger", UiFormatters.badgeClass("Failed"), "Failed -> danger");
        TestRunner.assertEquals("warn", UiFormatters.badgeClass("In progress"), "In progress -> warn");
        TestRunner.assertEquals("info", UiFormatters.badgeClass("Booked"), "Booked -> info");
        TestRunner.assertEquals("info", UiFormatters.badgeClass("Created"), "Created -> info");
    }

    public void testDotClass() {
        TestRunner.assertEquals("success", UiFormatters.dotClass("Completed"), "Completed dot");
        TestRunner.assertEquals("warn", UiFormatters.dotClass("In progress"), "In progress dot");
        TestRunner.assertEquals("info", UiFormatters.dotClass("Okänd"), "Unknown fallback to info");
    }

    public void testFormatDate() {
        LocalDate d = LocalDate.of(2026, 9, 16);
        String formatted = UiFormatters.formatDate(d);
        TestRunner.assertTrue(formatted.contains("Wednesday"), "Includes Wednesday");
        TestRunner.assertTrue(formatted.contains("16"), "Includes 16");
        TestRunner.assertTrue(formatted.contains("September"), "Includes September");
        TestRunner.assertTrue(formatted.contains("2026"), "Includes 2026");
    }
}
