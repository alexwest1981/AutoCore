package com.wac.autocore.ui.util;

import com.wac.autocore.security.Operation;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.motion.HoldToConfirmButton;

import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.Node;

/**
 * Keeps a function the role cannot use out of the window altogether.
 *
 * The order forbids offering a protected operation to someone who is not allowed to do it, and it
 * also says that hiding alone is not enough. Both hold here: the button is not drawn, and the
 * guard still refuses the call if somebody reaches it another way. Hiding is the courtesy; the
 * guard is the rule.
 */
public final class Actions {

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
        return show(button, garage, operation);
    }

    /** The hold-to-confirm delete caps. */
    public static HoldToConfirmButton hold(HoldToConfirmButton button, GarageSystem garage, Operation operation) {
        return show(button, garage, operation);
    }

    /** A context-menu entry, which has no managed flag: it is either in the menu or it is not. */
    public static MenuItem item(MenuItem item, GarageSystem garage, Operation operation) {
        if (item != null) {
            item.setVisible(allowed(garage, operation));
        }
        return item;
    }
}
