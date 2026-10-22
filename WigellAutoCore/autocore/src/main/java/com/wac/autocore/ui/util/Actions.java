package com.wac.autocore.ui.util;

import com.wac.autocore.security.Identity;
import com.wac.autocore.security.Operation;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.motion.HoldToConfirmButton;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps a function the role cannot use out of the window, and stops it if somebody reaches it
 * anyway.
 *
 * Two layers, and both are needed. The control is not drawn, so nobody is offered something that
 * would only end in a refusal — that is the courtesy. The control also swallows its own action
 * when the role is not allowed, so a press that arrives some other way than through the mouse, a
 * fired event from code or a node found by name, does nothing at all. Under both of them the guard
 * still refuses the call at the service, which is the rule itself.
 */
public final class Actions {

    private static final Logger log = LoggerFactory.getLogger(Actions.class);

    private Actions() {}

    /** True when the signed-in person may do this. False also when nobody is signed in. */
    public static boolean allowed(GarageSystem garage, Operation operation) {
        return garage != null && garage.may(operation);
    }

    /** Shows or hides a control, and takes its space out of the layout when it is hidden. */
    public static <T extends Node> T show(T control, GarageSystem garage, Operation operation) {
        if (control != null) {
            boolean visible = allowed(garage, operation);
            control.setVisible(visible);
            control.setManaged(visible);
        }
        return control;
    }

    /** The same for the buttons, which are what most of the window's actions are made of. */
    public static Button button(Button button, GarageSystem garage, Operation operation) {
        return guard(show(button, garage, operation), garage, operation);
    }

    /** The hold-to-confirm delete caps. */
    public static HoldToConfirmButton hold(HoldToConfirmButton button, GarageSystem garage, Operation operation) {
        return guard(show(button, garage, operation), garage, operation);
    }

    /** A context-menu entry, which has no managed flag: it is either in the menu or it is not. */
    public static MenuItem item(MenuItem item, GarageSystem garage, Operation operation) {
        if (item != null) {
            item.setVisible(allowed(garage, operation));
            item.addEventFilter(ActionEvent.ACTION, event -> refuse(event, garage, operation));
        }
        return item;
    }

    /**
     * Swallows the action before the button's own handler sees it, when the role is not allowed.
     * A filter runs ahead of the handler, so this holds even for an action that was fired from code
     * rather than pressed, and even if the control was enabled again by something else.
     */
    private static <T extends Node> T guard(T control, GarageSystem garage, Operation operation) {
        if (control != null) {
            control.addEventFilter(ActionEvent.ACTION, event -> refuse(event, garage, operation));
        }
        return control;
    }

    private static void refuse(ActionEvent event, GarageSystem garage, Operation operation) {
        if (allowed(garage, operation)) {
            return;
        }
        event.consume();
        log.warn("Åtgärd stoppad i fönstret: {} är inte tillåten för {}, anropet avvisades innan det nådde tjänsten",
                operation, who(garage));
    }

    private static String who(GarageSystem garage) {
        Identity identity = garage == null ? null : garage.identity();
        return identity == null ? "ingen inloggad" : identity.username() + " (" + identity.role() + ")";
    }
}
