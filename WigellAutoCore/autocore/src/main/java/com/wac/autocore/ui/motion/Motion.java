package com.wac.autocore.ui.motion;

import javafx.util.Duration;

/**
 * Timing for the two places where the interface still shows time passing: how long a hold has to
 * stay down before it commits, and how long a changed value keeps its tint.
 *
 * Nothing here moves anything. Buttons, pages, dialogs and lists used to fade, slide and spring,
 * and the window was seen to bounce while it worked. A control is now drawn where it ends up, and
 * the feedback is colour.
 */
public final class Motion {

    private Motion() {}

    /** Hold-to-confirm arms for this long before the action commits. */
    public static final Duration HOLD = Duration.millis(1800);
    /** Draining an aborted hold runs this many times faster than filling it. */
    public static final double HOLD_DRAIN_RATE = 2.5;
    /** How far the pointer may wander before a hold is abandoned. */
    public static final double HOLD_MOVE_TOLERANCE = 10;

    /** A changed value keeps its tint this long. */
    public static final Duration FLASH = Duration.millis(320);

    private static boolean reduced;

    /** Under reduced motion the outcome happens at once, without the wait. */
    public static boolean isReduced() {
        return reduced;
    }

    public static void setReducedMotion(boolean value) {
        reduced = value;
    }
}
