package com.wac.autocore.ui.motion;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * A cap that only commits while the person keeps holding it.
 *
 * From interior's hold-to-confirm: press and hold, a fill sweeps across, let go and it drains
 * back at {@link Motion#HOLD_DRAIN_RATE} times the fill speed. A pointer that wanders more than
 * {@link Motion#HOLD_MOVE_TOLERANCE} px, or leaves the cap, drops the hold. Space or Enter held
 * down runs the same sweep.
 *
 * Built on StackPane because the fill has to sit under the label; the control carries the same
 * style classes, focus traversal and accessible text as a button.
 */
public final class HoldToConfirmButton extends StackPane {

    private final Rectangle fill = new Rectangle(0, 0);
    private final Label label = new Label();
    private final Label hint = new Label();
    private final PauseTransition reset = new PauseTransition(Duration.millis(1500));
    private final DoubleProperty progress = new SimpleDoubleProperty(0);

    private Timeline sweep;
    private String text = "";
    private String confirmText = "Klart";
    private Runnable onConfirm;
    private Runnable onAbort;
    private Duration hold = Motion.HOLD;
    private boolean confirmed;
    private double pressX;
    private double pressY;

    public HoldToConfirmButton(String text) {
        getStyleClass().addAll("button", "hold-to-confirm");
        setAlignment(Pos.CENTER_LEFT);
        setFocusTraversable(true);

        fill.getStyleClass().add("hold-fill");
        fill.setHeight(1);
        fill.heightProperty().bind(heightProperty().subtract(2));
        fill.setArcWidth(12);
        fill.setArcHeight(12);

        label.getStyleClass().add("hold-label");
        hint.getStyleClass().add("hold-hint");
        hint.setText("håll");

        // The label takes the room the hint leaves, so a squeezed button ellipsizes it
        // (measured: "Ta bort kund" ran under "håll").
        fill.setManaged(false);
        fill.setX(1);
        fill.setY(1);
        hint.setMinWidth(Region.USE_PREF_SIZE);
        label.setMinWidth(0);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        HBox.setHgrow(label, Priority.ALWAYS);
        HBox content = new HBox(6, label, hint);
        content.setAlignment(Pos.CENTER_LEFT);
        getChildren().setAll(fill, content);

        setText(text);
        installPointer();
        installKeyboard();

        widthProperty().addListener((obs, was, now) -> {
            if (!confirmed && (sweep == null || sweep.getStatus() != Animation.Status.RUNNING)) {
                fill.setWidth(0);
            }
        });
        reset.setOnFinished(e -> resetToIdle());
    }

    // ---- public API -------------------------------------------------------

    public HoldToConfirmButton text(String value) {
        setText(value);
        return this;
    }

    public void setText(String value) {
        this.text = value == null ? "" : value;
        label.setText(this.text);
        setAccessibleText(this.text);
        setAccessibleRoleDescription("Hold to confirm");
    }

    public String getText() {
        return text;
    }

    public HoldToConfirmButton confirmText(String value) {
        this.confirmText = value;
        return this;
    }

    /** The one-word cue on the right ("håll"); it says what the cap wants without a tooltip. */
    public HoldToConfirmButton hint(String value) {
        hint.setText(value == null ? "" : value);
        return this;
    }

    public HoldToConfirmButton hold(Duration duration) {
        this.hold = duration;
        return this;
    }

    public HoldToConfirmButton onConfirm(Runnable action) {
        this.onConfirm = action;
        return this;
    }

    public HoldToConfirmButton onAbort(Runnable action) {
        this.onAbort = action;
        return this;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    /** 0…1 of the way through the hold. Read by the offscreen probe. */
    public double getProgress() {
        return progress.get();
    }

    // ---- holding ---------------------------------------------------------

    public void beginHold() {
        if (confirmed || isDisable() || sweep != null && sweep.getStatus() == Animation.Status.RUNNING) {
            return;
        }
        double target = Math.max(1, getWidth());
        if (Motion.isReduced()) {
            // reduced motion: commit at once
            commit();
            return;
        }
        double from = fill.getWidth();
        double fractionLeft = 1 - (from / target);
        sweep = new Timeline(
                new KeyFrame(hold.multiply(fractionLeft),
                        new KeyValue(fill.widthProperty(), target, Interpolator.LINEAR),
                        new KeyValue(progress, 1.0, Interpolator.LINEAR)));
        sweep.setOnFinished(e -> commit());
        sweep.play();
    }

    public void cancelHold() {
        if (confirmed || sweep == null) {
            return;
        }
        sweep.stop();
        double target = Math.max(1, getWidth());
        double from = fill.getWidth();
        if (from <= 0) {
            return;
        }
        // Duration has no divide() in JavaFX 8, so the drain is computed in millis
        double drainMs = hold.toMillis() * (from / target) / Motion.HOLD_DRAIN_RATE;
        Timeline back = new Timeline(
                new KeyFrame(Duration.millis(drainMs),
                        new KeyValue(fill.widthProperty(), 0.0, Interpolator.EASE_OUT),
                        new KeyValue(progress, 0.0, Interpolator.EASE_OUT)));
        back.play();
        if (onAbort != null) {
            onAbort.run();
        }
    }

    private void commit() {
        confirmed = true;
        fill.setWidth(getWidth());
        progress.set(1);
        label.setText(confirmText);
        if (onConfirm != null) {
            onConfirm.run();
        }
        reset.playFromStart();
    }

    private void resetToIdle() {
        confirmed = false;
        label.setText(text);
        fill.setWidth(0);
        progress.set(0);
        sweep = null;
    }

    // ---- input -----------------------------------------------------------

    private void installPointer() {
        addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                pressX = e.getSceneX();
                pressY = e.getSceneY();
                requestFocus();
                beginHold();
            }
        });
        addEventHandler(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                if (Math.hypot(e.getSceneX() - pressX, e.getSceneY() - pressY) > Motion.HOLD_MOVE_TOLERANCE) {
                    cancelHold();
                }
            }
        });
        addEventHandler(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                cancelHold();
            }
        });
        addEventHandler(MouseEvent.MOUSE_EXITED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                cancelHold();
            }
        });
    }

    private void installKeyboard() {
        addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent e) {
                if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER) {
                    beginHold();
                    e.consume();
                }
            }
        });
        addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent e) {
                if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER) {
                    cancelHold();
                    e.consume();
                }
            }
        });
    }
}
