package com.wac.autocore.ui.motion;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Press depth for every cap in a scene.
 *
 * Installed once per scene: a mouse press sinks the nearest button by a couple of percent and
 * one pixel, and letting go springs it back with a small overshoot. No call site changes, so a
 * button added later behaves like the rest.
 *
 * Space and Enter on a focused button run the same press and release.
 */
public final class PressDepth {

    private PressDepth() {}

    private static final Map<Node, Timeline> RUNNING = new WeakHashMap<Node, Timeline>();
    private static final String INSTALLED = "wac.pressDepth";

    public static void install(Scene scene) {
        if (scene == null || scene.getProperties().containsKey(INSTALLED)) {
            return;
        }
        scene.getProperties().put(INSTALLED, Boolean.TRUE);

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                ButtonBase b = buttonAt(e.getTarget());
                if (b != null) {
                    press(b);
                }
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                // Released anywhere: a drag that leaves the cap must not leave it sunk.
                release(buttonAt(e.getTarget()));
                releaseAllExcept(buttonAt(e.getTarget()));
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_EXITED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                ButtonBase b = buttonAt(e.getTarget());
                if (b != null) {
                    release(b);
                }
            }
        });

        scene.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent e) {
                if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER) {
                    ButtonBase b = buttonAt(e.getTarget());
                    if (b != null) {
                        press(b);
                    }
                }
            }
        });

        scene.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent e) {
                if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER) {
                    release(buttonAt(e.getTarget()));
                }
            }
        });
    }

    /** Visible for the offscreen probe: the scale a cap is heading for. */
    public static void press(Node node) {
        if (node == null || node.isDisable() || Motion.isReduced()) {
            return;
        }
        animate(node, Motion.PRESS_SCALE, Motion.scaled(Motion.PRESS), javafx.animation.Interpolator.EASE_OUT);
    }

    public static void release(Node node) {
        if (node == null) {
            return;
        }
        Timeline old = RUNNING.get(node);
        if (old != null) {
            old.stop();
        }
        if (Motion.isReduced()) {
            node.setScaleX(1);
            node.setScaleY(1);
            node.setTranslateY(0);
            return;
        }
        Timeline t = Motion.springBack(node, node.getScaleX());
        RUNNING.put(node, t);
        t.play();
    }

    private static void animate(Node node, double scale, javafx.util.Duration duration, javafx.animation.Interpolator ease) {
        Timeline old = RUNNING.get(node);
        if (old != null) {
            old.stop();
        }
        if (duration.lessThanOrEqualTo(javafx.util.Duration.ZERO)) {
            node.setScaleX(scale);
            node.setScaleY(scale);
            node.setTranslateY(scale < 1 ? 1 : 0);
            return;
        }
        Timeline t = new Timeline(
                new KeyFrame(duration,
                        new KeyValue(node.scaleXProperty(), scale, ease),
                        new KeyValue(node.scaleYProperty(), scale, ease),
                        new KeyValue(node.translateYProperty(), scale < 1 ? 1.5 : 0.0, ease)));
        RUNNING.put(node, t);
        t.play();
    }

    private static void releaseAllExcept(Node keep) {
        for (Node n : new java.util.ArrayList<Node>(RUNNING.keySet())) {
            if (n != keep && n.getScaleX() != 1.0) {
                release(n);
            }
        }
    }

    private static ButtonBase buttonAt(javafx.event.EventTarget target) {
        if (!(target instanceof Node)) {
            return null;
        }
        Node n = (Node) target;
        while (n != null) {
            if (n instanceof ButtonBase) {
                return (ButtonBase) n;
            }
            n = n.getParent();
        }
        return null;
    }
}
