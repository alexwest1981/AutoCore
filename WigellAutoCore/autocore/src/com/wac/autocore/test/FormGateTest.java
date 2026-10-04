package com.wac.autocore.test;

import com.wac.autocore.ui.ActionDialogs;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;

/**
 * FORMULÄR: OK-knappen ska vara låst tills fälten är ifyllda.
 *
 * Låsningen är det som hindrar att ett halvfyllt formulär skickas och att allt måste skrivas om
 * efter ett nej. Proven håller de två reglerna fast: vad som räknas som ett ifyllt fält, och att
 * knappen följer ett eget villkor (t.ex. "bokningen måste innehålla en tjänst").
 *
 * Dialogen är ett fönster och får bara byggas på JavaFX-tråden, därför körs hela provet där.
 */
public class FormGateTest {

    private static boolean fxInitialized = false;

    static {
        // Initierar JavaFX Toolkit så att TextField/ComboBox/Dialog kan instansieras
        try {
            new JFXPanel();
            fxInitialized = true;
        } catch (Throwable t) {
            System.out.println("  (Info: JavaFX grafikmiljö ej tillgänglig på denna nod: " + t.getMessage() + ")");
        }
    }

    public void testOkIsLockedUntilEveryFieldHasAValue() throws Exception {
        if (!fxInitialized) {
            System.out.println("  (Hoppar över formulärtestet: Grafikmiljö/DISPLAY ej aktiv)");
            return;
        }

        onFxThread(() -> {
            TextField name = new TextField();
            ComboBox<String> brand = new ComboBox<String>();
            brand.getItems().add("Volvo");
            DatePicker date = new DatePicker();

            Dialog<ButtonType> dialog = newDialog();
            ActionDialogs.requireFilled(dialog, name, brand, date);
            Node ok = dialog.getDialogPane().lookupButton(ButtonType.OK);

            TestRunner.assertTrue(ok.isDisable(), "OK ska vara låst så länge fälten är tomma");

            name.setText("Anna Andersson");
            TestRunner.assertTrue(ok.isDisable(), "Ett ifyllt fält av tre ska inte öppna knappen");

            brand.getSelectionModel().selectFirst();
            TestRunner.assertTrue(ok.isDisable(), "Ett val i rullistan men inget datum ska inte öppna knappen");

            date.setValue(LocalDate.now());
            TestRunner.assertFalse(ok.isDisable(), "När alla fält har ett värde ska knappen vara öppen");

            name.setText("   ");
            TestRunner.assertTrue(ok.isDisable(), "Bara blanksteg är inte ett ifyllt fält");
        });
    }

    public void testOkFollowsACustomRule() throws Exception {
        if (!fxInitialized) {
            System.out.println("  (Hoppar över formulärtestet: Grafikmiljö/DISPLAY ej aktiv)");
            return;
        }

        onFxThread(() -> {
            SimpleBooleanProperty filled = new SimpleBooleanProperty(false);
            Dialog<ButtonType> dialog = newDialog();
            ActionDialogs.requireFilled(dialog, filled);
            Node ok = dialog.getDialogPane().lookupButton(ButtonType.OK);

            TestRunner.assertTrue(ok.isDisable(), "Ett eget villkor som är falskt ska låsa knappen");
            filled.set(true);
            TestRunner.assertFalse(ok.isDisable(), "Knappen ska öppnas när villkoret blir sant");
        });
    }

    /** Kör kroppen på JavaFX-tråden och låter ett misslyckat prov fälla provet. */
    private void onFxThread(final Runnable body) throws Exception {
        final CountDownLatch done = new CountDownLatch(1);
        final Throwable[] failure = new Throwable[1];
        Platform.runLater(() -> {
            try {
                body.run();
            } catch (Throwable t) {
                failure[0] = t;
            } finally {
                done.countDown();
            }
        });
        done.await();
        if (failure[0] != null) {
            throw new AssertionError(failure[0].getMessage());
        }
    }

    private Dialog<ButtonType> newDialog() {
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        return dialog;
    }
}
