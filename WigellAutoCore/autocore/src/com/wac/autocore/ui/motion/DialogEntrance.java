package com.wac.autocore.ui.motion;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.DialogPane;
import javafx.util.Duration;

/**
 * Dialogs fade in as the window shows.
 *
 * 130 ms and a 1.5 % scale. A confirmation is already where the eye expects it, so a longer
 * entrance would only make the dialog feel slower than it is. Under reduced motion the dialog
 * is simply there.
 */
public final class DialogEntrance {

    private DialogEntrance() {}

    private static final Duration IN = Duration.millis(130);
    private static final double FROM_SCALE = 0.985;

    /** Call from the dialog's onShowing handler, as the window is about to become visible. */
    public static void enter(DialogPane pane) {
        if (pane == null || Motion.isReduced()) {
            return;
        }
        Node root = pane;
        root.setOpacity(0);
        root.setScaleX(FROM_SCALE);
        root.setScaleY(FROM_SCALE);
        Timeline t = new Timeline(new KeyFrame(IN,
                new KeyValue(root.opacityProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(root.scaleXProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(root.scaleYProperty(), 1, Interpolator.EASE_OUT)));
        t.play();
    }
}
