package com.wac.autocore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * The window is drawn at scale 1, whatever the session that started it happens to carry.
 *
 * A stale GDK_SCALE in the environment doubled every button, every font and the whole window,
 * because JavaFX asks GTK how large everything should be. The property set here overrides the
 * environment, so the value a terminal was started with cannot reach the window.
 */
class MainScaleTest {

    @AfterEach
    void forgetTheProperty() {
        System.clearProperty(Main.SCALE_PROPERTY);
    }

    @Test
    void theWindowIsDrawnAtScaleOne() {
        Assumptions.assumeTrue(System.getenv(Main.SCALE_VARIABLE) == null,
                "maskinen har " + Main.SCALE_VARIABLE + " satt, då gäller den");

        Main.chooseScale();

        assertEquals("1", System.getProperty(Main.SCALE_PROPERTY));
    }

    @Test
    void aScaleGivenWhenTheProgramStartsIsKept() {
        System.setProperty(Main.SCALE_PROPERTY, "2");

        Main.chooseScale();

        assertEquals("2", System.getProperty(Main.SCALE_PROPERTY),
                "den som startar med -D" + Main.SCALE_PROPERTY + " bestämmer själv");
    }
}
