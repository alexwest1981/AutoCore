package com.wac.autocore.test;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.InvoiceDocument;
import com.wac.autocore.ui.util.EntityLookup;
import com.wac.autocore.ui.util.UiFormatters;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * FAKTURAN SOM SKRIVS UT: företagsnamnet, kunden, fordonet, arbetena med priser och summan ska stå
 * med, annars är den inte värd att skicka till en kund.
 *
 * Provet bygger samma dokument som förhandsgranskningen visar och läser texten i det, så det som
 * kontrolleras är det kunden faktiskt får se på pappret.
 */
public class InvoiceDocumentTest {

    private static boolean fxInitialized = false;

    static {
        // Initierar JavaFX Toolkit så att etiketter och layouter kan byggas
        try {
            new JFXPanel();
            fxInitialized = true;
        } catch (Throwable t) {
            System.out.println("  (Info: JavaFX grafikmiljö ej tillgänglig på denna nod: " + t.getMessage() + ")");
        }
    }

    public void testPrintedInvoiceCarriesCustomerVehicleWorkAndTotal() throws Exception {
        if (!fxInitialized) {
            System.out.println("  (Hoppar över fakturaprovet: Grafikmiljö/DISPLAY ej aktiv)");
            return;
        }

        onFxThread(() -> {
            GarageSystem garage = new GarageSystem();
            if (garage.getWorkOrders().isEmpty()) {
                System.out.println("  (Hoppar över fakturaprovet: inga arbetsordrar i databasen)");
                return;
            }
            WorkOrder order = garage.getWorkOrders().get(0);

            Invoice invoice = new Invoice(4242, order.getId(), LocalDate.now(), 1500.0);
            invoice.setDiscount(200.0);
            invoice.addLine(new InvoiceLine(1, 4242, 1, "Bromsservice fram", 900.0, 0.0));
            invoice.addLine(new InvoiceLine(2, 4242, 2, "Oljebyte", 600.0, 0.0));

            List<String> texts = textsOf(InvoiceDocument.build(garage, invoice));

            assertContains(texts, "#4242", "fakturanumret ska stå på fakturan");
            assertContains(texts, "Wigell AutoCore", "företagsnamnet ska stå i huvudet");
            assertContains(texts, "Bromsservice fram", "varje utfört arbete ska stå som en rad");
            assertContains(texts, "Oljebyte", "varje utfört arbete ska stå som en rad");
            assertContains(texts, UiFormatters.formatMoney(900.0), "priset per rad ska stå med");
            assertContains(texts, UiFormatters.formatMoney(1300.0), "summan efter rabatt ska stå med");
            assertContains(texts, i18nText("invoice.due_date"), "förfallodatumet ska stå med");

            String customer = EntityLookup.workOrderCustomerName(garage, order);
            if (customer != null && !customer.trim().isEmpty()) {
                assertContains(texts, customer, "kunden ska stå på fakturan");
            }
            String reg = EntityLookup.workOrderVehicleReg(garage, order);
            if (reg != null && !reg.trim().isEmpty()) {
                assertContains(texts, reg, "fordonet ska stå på fakturan");
            }
        });
    }

    private static String i18nText(String key) {
        return com.wac.autocore.ui.i18n.I18n.get(key);
    }

    /** All text som står i dokumentet, oavsett var den sitter i layouten. */
    private static List<String> textsOf(Node node) {
        List<String> texts = new ArrayList<String>();
        collect(node, texts);
        return texts;
    }

    private static void collect(Node node, List<String> texts) {
        if (node instanceof Label) {
            texts.add(((Label) node).getText());
        }
        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                collect(child, texts);
            }
        }
    }

    private static void assertContains(List<String> texts, String expected, String what) {
        for (String text : texts) {
            if (text != null && text.contains(expected)) {
                return;
            }
        }
        TestRunner.assertTrue(false, "Saknas på fakturan: " + expected + " (" + what + ")");
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
}
