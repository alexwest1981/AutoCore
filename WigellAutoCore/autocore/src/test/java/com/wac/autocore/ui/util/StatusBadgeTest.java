package com.wac.autocore.ui.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wac.autocore.model.BookingStatus;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.util.Texts;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * The colour of a status badge belongs to the status value, not to the word that happens to be
 * shown for it.
 *
 * The old rule compared the finished text against a list of English and Swedish words. It worked
 * for the two languages that exist and broke silently for everything else: another wording, a
 * translation nobody had thought of, or a status code passed in unchanged. The overview's dot
 * passed {@code COMPLETED} where the list said {@code Completed}, so a finished booking got the
 * blue dot meant for a new one.
 *
 * These tests pin the mapping to the enums. A status without a colour fails here rather than in a
 * window, and the language may change without changing what a row looks like.
 */
class StatusBadgeTest {

    /** The four colours the themes define for .badge and .status. */
    private static final Set<String> COLOURS = Set.of("success", "info", "warn", "danger");

    @AfterEach
    void backToTheDefaultLanguage() {
        Texts.use(Texts.DEFAULT_LANG);
    }

    @Test
    void everyBookingStatusHasAColour() {
        for (BookingStatus status : BookingStatus.values()) {
            assertTrue(COLOURS.contains(UiFormatters.badgeClass(status)),
                    status + " saknar färg, och då faller den tillbaka på chippens grundfärg");
        }
    }

    @Test
    void everyWorkOrderStatusHasAColour() {
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            assertTrue(COLOURS.contains(UiFormatters.badgeClass(status)),
                    status + " saknar färg, och då faller den tillbaka på chippens grundfärg");
        }
    }

    @Test
    void everyStatusHasAWord() {
        for (BookingStatus status : BookingStatus.values()) {
            assertFalse(UiFormatters.badge(status).text().isEmpty(), status + " saknar ord");
        }
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            assertFalse(UiFormatters.badge(status).text().isEmpty(), status + " saknar ord");
        }
    }

    @Test
    void theColourDoesNotChangeWithTheLanguage() {
        Texts.use(Texts.LANG_EN);
        Map<BookingStatus, String> bookings = new EnumMap<BookingStatus, String>(BookingStatus.class);
        Map<WorkOrderStatus, String> orders = new EnumMap<WorkOrderStatus, String>(WorkOrderStatus.class);
        for (BookingStatus status : BookingStatus.values()) {
            bookings.put(status, UiFormatters.badge(status).styleClass());
        }
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            orders.put(status, UiFormatters.badge(status).styleClass());
        }

        Texts.use(Texts.LANG_SV);
        for (BookingStatus status : BookingStatus.values()) {
            assertEquals(bookings.get(status), UiFormatters.badge(status).styleClass(),
                    status + " byter färg när språket byts");
        }
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            assertEquals(orders.get(status), UiFormatters.badge(status).styleClass(),
                    status + " byter färg när språket byts");
        }
    }

    @Test
    void theWordFollowsTheLanguageWhileTheColourStays() {
        Texts.use(Texts.LANG_SV);
        UiFormatters.Badge swedish = UiFormatters.badge(BookingStatus.COMPLETED);
        Texts.use(Texts.LANG_EN);
        UiFormatters.Badge english = UiFormatters.badge(BookingStatus.COMPLETED);

        assertNotEquals(swedish.text(), english.text(), "ordet ska följa språket");
        assertEquals(swedish.styleClass(), english.styleClass(), "färgen ska stå still");
        assertEquals("success", english.styleClass());
    }

    @Test
    void theOverviewDotIsColouredByTheStatus() {
        assertEquals("success", UiFormatters.dotClass(BookingStatus.COMPLETED));
        assertEquals("warn", UiFormatters.dotClass(BookingStatus.IN_PROGRESS));
        assertEquals("info", UiFormatters.dotClass(BookingStatus.BOOKED));
        for (BookingStatus status : BookingStatus.values()) {
            assertTrue(COLOURS.contains(UiFormatters.dotClass(status)), status + " ger ingen färg");
        }
    }

    @Test
    void aSettledFlagIsGreenAndAnUnsettledOneIsRed() {
        assertEquals("success", UiFormatters.badge(true, "common.yes", "common.no").styleClass());
        assertEquals("danger", UiFormatters.badge(false, "common.yes", "common.no").styleClass());
    }

    @Test
    void aMissingStatusGetsNoColourAtAll() {
        assertEquals("", UiFormatters.badgeClass((BookingStatus) null));
        assertEquals("", UiFormatters.badgeClass((WorkOrderStatus) null));
        assertEquals("", UiFormatters.badge((BookingStatus) null).styleClass());
        assertEquals("info", UiFormatters.dotClass(null));
    }
}
