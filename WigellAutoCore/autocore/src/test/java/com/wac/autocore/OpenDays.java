package com.wac.autocore;

import com.wac.autocore.model.OpeningHours;
import java.time.LocalDate;

/**
 * Dates for the tests, chosen so a run never depends on which weekday it happens to be.
 *
 * <p>A fixed number of days ahead lands on a Saturday roughly two times in seven, and the workshop
 * is closed then. The date is therefore moved forward to the first day the workshop is open.
 */
public final class OpenDays {

    private OpenDays() {
    }

    /** The given number of days ahead, or the first open day after it. */
    public static LocalDate daysAhead(int days) {

        LocalDate candidate = LocalDate.now().plusDays(days);

        while (!OpeningHours.isOpenOn(candidate)) {
            candidate = candidate.plusDays(1);
        }

        return candidate;
    }
}
