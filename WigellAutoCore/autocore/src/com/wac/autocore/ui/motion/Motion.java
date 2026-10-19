package com.wac.autocore.ui.motion;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Timing and curves for the interface's micro-interactions, ported from interior.dev.
 *
 * Nothing moves unless something happened, and the response lands inside the frame where the
 * click happened. JavaFX 8 has no spring engine, so the springs here are damped oscillators
 * sampled into keyframes.
 */
public final class Motion {

    private Motion() {}

    /** How far a pressed cap sinks. interior uses ~2.5%. */
    public static final double PRESS_SCALE = 0.965;

    /** Press lands within one frame: 90 ms, no easing delay. */
    public static final Duration PRESS = Duration.millis(90);

    /**
     * The way back is longer and overshoots by about 3 %, which reads as a release.
     * JavaFX 8's SPLINE rejects control points outside [0,1] (measured: IllegalArgumentException),
     * so the overshoot comes from a damped oscillator sampled into keyframes instead.
     */
    public static final Duration RELEASE = Duration.millis(260);

    /** Fades a view in when it replaces another. Short enough to feel instant. */
    public static final Duration FADE_IN = Duration.millis(120);

    public static void fadeIn(Node node) {
        if (node == null) {
            return;
        }
        if (isReduced()) {
            node.setOpacity(1);
            return;
        }
        node.setOpacity(0);
        Timeline t = new Timeline(new KeyFrame(FADE_IN, new KeyValue(node.opacityProperty(), 1, Interpolator.EASE_OUT)));
        t.play();
    }

    /** Fades a new page in and lifts it 6 px. */
    public static void enterPage(Node node) {
        if (node == null) {
            return;
        }
        if (isReduced()) {
            node.setOpacity(1);
            node.setTranslateY(0);
            return;
        }
        node.setOpacity(0);
        node.setTranslateY(6);
        new Timeline(new KeyFrame(FADE_IN,
                new KeyValue(node.opacityProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(node.translateYProperty(), 0, Interpolator.EASE_OUT))).play();
    }

    /** Fades a list in one item after another, so it reads as loading. */
    public static void stagger(java.util.List<? extends Node> nodes, int stepMs) {
        if (nodes == null) {
            return;
        }
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            if (node == null) {
                continue;
            }
            if (isReduced()) {
                node.setOpacity(1);
                continue;
            }
            node.setOpacity(0);
            node.setTranslateY(8);
            Timeline t = new Timeline(new KeyFrame(Duration.millis(180),
                    new KeyValue(node.opacityProperty(), 1, Interpolator.EASE_OUT),
                    new KeyValue(node.translateYProperty(), 0, Interpolator.EASE_OUT)));
            t.setDelay(Duration.millis(Math.min(240, i * stepMs)));
            t.play();
        }
    }

    /** One scale pulse, to draw the eye when a value on the node changed. */
    public static void pulse(Node node) {
        if (node == null || isReduced()) {
            return;
        }
        new Timeline(
                new KeyFrame(Duration.millis(90),
                        new KeyValue(node.scaleXProperty(), 1.18, Interpolator.EASE_OUT),
                        new KeyValue(node.scaleYProperty(), 1.18, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(240),
                        new KeyValue(node.scaleXProperty(), 1.0, Interpolator.EASE_IN),
                        new KeyValue(node.scaleYProperty(), 1.0, Interpolator.EASE_IN))).play();
    }

    /** Damped spring from {@code fromScale} to 1: ~3 % overshoot, settles inside RELEASE. */
    public static Timeline springBack(Node node, double fromScale) {
        Timeline tl = new Timeline();
        int frames = (int) Math.max(6, Math.round(RELEASE.toMillis() / 16.0));
        for (int i = 0; i <= frames; i++) {
            double t = (double) i / frames;
            double eased = 1 - Math.exp(-6.0 * t) * Math.cos(9.0 * t);
            double scale = fromScale + (1 - fromScale) * eased;
            tl.getKeyFrames().add(new KeyFrame(
                    Duration.millis(16.0 * i),
                    new KeyValue(node.scaleXProperty(), scale, Interpolator.LINEAR),
                    new KeyValue(node.scaleYProperty(), scale, Interpolator.LINEAR),
                    new KeyValue(node.translateYProperty(), (1 - scale) * 40, Interpolator.LINEAR)));
        }
        return tl;
    }

    /** Value flash lasts 320 ms. */
    public static final Duration FLASH = Duration.millis(320);

    /** Hold-to-confirm arms for this long before the action commits. */
    public static final Duration HOLD = Duration.millis(1800);
    /** Draining an aborted hold runs this many times faster than filling it. */
    public static final double HOLD_DRAIN_RATE = 2.5;
    /** How far the pointer may wander before a hold is abandoned. */
    public static final double HOLD_MOVE_TOLERANCE = 10;

    private static boolean reduced;

    /** Under reduced motion the outcome happens at once, without the movement. */
    public static boolean isReduced() {
        return reduced;
    }

    public static void setReducedMotion(boolean value) {
        reduced = value;
    }

    /** A duration that collapses to zero under reduced motion. */
    public static Duration scaled(Duration d) {
        return reduced ? Duration.ZERO : d;
    }
}
