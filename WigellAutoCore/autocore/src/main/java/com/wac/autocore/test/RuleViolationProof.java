package com.wac.autocore.test;

import com.wac.autocore.model.DraftChanges;
import com.wac.autocore.config.DatabasePath;
import com.wac.autocore.data.Db;
import com.wac.autocore.exception.RuleViolationException;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;

/** Shows that a refused deletion can be read in both languages. */
public class RuleViolationProof {

    private RuleViolationProof() {}

    public static void main(String[] args) {


        GarageSystem garage = new GarageSystem(new SqliteRepositoryFactory(new Db(DatabasePath.resolve(args))));

        int vehicleId = Integer.parseInt(args[1]);
        int mechanicId = Integer.parseInt(args[2]);
        int serviceItemId = Integer.parseInt(args[3]);

        // An open work order on the mechanic is what the rule refuses to delete.
        WorkOrder draft = garage.createDraft(vehicleId, "Bromsok och bromsbelägg");
        garage.updateDraft(DraftChanges.forWorkOrder(draft.getId())
                .vehicle(vehicleId)
                .description("Bromsok och bromsbelägg")
                .mechanic(mechanicId)
                .services(Arrays.asList(serviceItemId))
                .plannedAt(LocalDate.of(2026, 10, 16), LocalTime.of(9, 0))
                .customerInstructions("")
                .otherComments("")
                .build());

        String name = "";

        for (Mechanic mechanic : garage.getMechanics()) {

            if (mechanic.getId() == mechanicId) {
                name = mechanic.getName();
            }
        }

        try {

            garage.deleteMechanic(mechanicId);
            System.out.println("FEL: mekanikern gick att ta bort");

        } catch (RuleViolationException rejected) {

            System.out.println("nyckel: " + rejected.getMessageKey());
            System.out.println("meddelande: " + rejected.getMessage());

            I18n.setLanguage(I18n.LANG_SV);
            System.out.println("sv: " + I18n.get(rejected.getMessageKey(), name));

            I18n.setLanguage(I18n.LANG_EN);
            System.out.println("en: " + I18n.get(rejected.getMessageKey(), name));

        }



    }
}
