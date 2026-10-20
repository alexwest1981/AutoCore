package com.wac.autocore.test;

import com.wac.autocore.config.DatabasePath;
import com.wac.autocore.data.Db;
import com.wac.autocore.model.DraftChanges;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.gateway.WorkOrderGateway;

import java.util.Arrays;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Proves that a work order can be created, changed and confirmed with no window in the way, so
 * the rule about running the business outside the ui rests on a run and not on a document.
 */
public final class WorkOrderProof {

    private WorkOrderProof() {}

    public static void main(String[] args) {


        // The gateway covers the work order only. so the caller brings the three ids.
        int vehicleId = Integer.parseInt(args[1]);
        int mechanicId = Integer.parseInt(args[2]);
        int serviceItemId = Integer.parseInt(args[3]);

        WorkOrderGateway gateway = new GarageSystem(new SqliteRepositoryFactory(new Db(DatabasePath.resolve(args))));

        WorkOrder draft = gateway.createDraft(vehicleId, "Bromsok och bromsbelägg");

        System.out.println("1. utkastet skapat, nummer " + draft.getId() + ", status " + draft.getStatus());

        WorkOrder changed = gateway.updateDraft(DraftChanges.forWorkOrder(draft.getId())
                .vehicle(vehicleId)
                .description("Bromsok och bromsbelägg")
                .mechanic(mechanicId)
                .services(Arrays.asList(serviceItemId))
                .plannedAt(LocalDate.of(2026, 10, 18), LocalTime.of(15, 2))
                .customerInstructions("")
                .otherComments("")
                .build());

        System.out.println("2. utkastet ändrat, mekaniker " + changed.getMechanicId());

        boolean confirmed = gateway.confirmWorkOrder(draft.getId());

        System.out.println("3. bekräftad: " + (confirmed ? "ja" : "nej"));

        System.out.println("status efter: " + gateway.findWorkOrder(draft.getId()).getStatus());
    }
}
