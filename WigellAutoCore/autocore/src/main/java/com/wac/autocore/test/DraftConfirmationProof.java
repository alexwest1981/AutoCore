package com.wac.autocore.test;

import com.wac.autocore.config.DatabasePath;
import com.wac.autocore.data.Db;
import com.wac.autocore.exception.RuleViolationException;
import com.wac.autocore.model.DraftChanges;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

/** Proves that a draft without what a work order needs cannot be confirmed. */
public final class DraftConfirmationProof {

    private DraftConfirmationProof() {}

    public static void main(String[] args) {

        I18n.setLanguage("sv");

        final GarageSystem garage = new GarageSystem(new SqliteRepositoryFactory(new Db(DatabasePath.resolve(args))));

        final int vehicleId = Integer.parseInt(args[1]);
        final int mechanicId = Integer.parseInt(args[2]);
        final int serviceItemId = Integer.parseInt(args[3]);

        // 1. An empty draft: no mechanic.
        final WorkOrder emptyDraft = garage.createDraft(vehicleId, "Utkast utan uppgifter");
        expectRefusal("tomt utkast", new Runnable() {
            public void run() { garage.confirmWorkOrder(emptyDraft.getId()); }
        });

        // 2. A mechanic but no services.
        final WorkOrder noServices = garage.createDraft(vehicleId, "Utkast utan tjänster");
        garage.updateDraft(DraftChanges.forWorkOrder(noServices.getId())
                .vehicle(vehicleId).description("Utkast utan tjänster").mechanic(mechanicId)
                .services(Collections.<Integer>emptyList())
                .plannedAt(LocalDate.of(2026, 10, 20), LocalTime.of(9, 0))
                .customerInstructions("").otherComments("").build());
        expectRefusal("utan tjänster", new Runnable() {
            public void run() { garage.confirmWorkOrder(noServices.getId()); }
        });

        // 3. A mechanic and a service, but no date.
        final WorkOrder noDate = garage.createDraft(vehicleId, "Utkast utan datum");
        garage.updateDraft(DraftChanges.forWorkOrder(noDate.getId())
                .vehicle(vehicleId).description("Utkast utan datum").mechanic(mechanicId)
                .services(Arrays.asList(serviceItemId)).plannedAt(null, null)
                .customerInstructions("").otherComments("").build());
        expectRefusal("utan datum", new Runnable() {
            public void run() { garage.confirmWorkOrder(noDate.getId()); }
        });

        // 4. Everything in place: goes through.
        final WorkOrder complete = garage.createDraft(vehicleId, "Utkast med allt");
        garage.updateDraft(DraftChanges.forWorkOrder(complete.getId())
                .vehicle(vehicleId).description("Utkast med allt").mechanic(mechanicId)
                .services(Arrays.asList(serviceItemId))
                .plannedAt(LocalDate.of(2026, 10, 20), LocalTime.of(10, 0))
                .customerInstructions("").otherComments("").build());
        garage.confirmWorkOrder(complete.getId());
        System.out.println("komplett utkast: status " + garage.findWorkOrder(complete.getId()).getStatus());
    }

    /** Prints the refusal the same way the other proofs do. */
    private static void expectRefusal(String label, Runnable call) {
        try {
            call.run();
            System.out.println(label + ": INGET NEKANDE, det är fel");
        } catch (RuleViolationException refused) {
            System.out.println(label + ": " + I18n.get(refused.getMessageKey())
                    + " (" + refused.getMessageKey() + ")");
        }
    }
}