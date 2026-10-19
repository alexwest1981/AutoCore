package com.wac.autocore.ui.motion;

import javafx.animation.PauseTransition;
import javafx.scene.Node;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A tint that marks a value as changed, then clears itself.
 *
 * Ported from interior's value-flash and applied from the table cell factory, so every text
 * column gets it without a toast or a badge.
 */
public final class ValueFlash {

    private ValueFlash() {}

    private static final String CLASS = "value-flash";
    private static final Map<Node, PauseTransition> RUNNING = Collections.synchronizedMap(new WeakHashMap<Node, PauseTransition>());

    /** Flashes when a cell's text changes. A cell's first fill does not flash. */
    public static void onValueChange(Node node, String before, String after) {
        if (node == null || before == null || after == null) {
            return;
        }
        if (before.equals(after) || before.isEmpty() || after.isEmpty()) {
            return;
        }
        flash(node);
    }

    public static void flash(Node node) {
        if (node == null || Motion.isReduced()) {
            return;
        }
        if (!node.getStyleClass().contains(CLASS)) {
            node.getStyleClass().add(CLASS);
        }
        PauseTransition old = RUNNING.get(node);
        if (old != null) {
            old.stop();
        }
        PauseTransition off = new PauseTransition(Motion.FLASH);
        off.setOnFinished(e -> node.getStyleClass().remove(CLASS));
        RUNNING.put(node, off);
        off.play();
    }
}
