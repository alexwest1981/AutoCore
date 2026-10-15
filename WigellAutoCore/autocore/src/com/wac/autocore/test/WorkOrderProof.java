package com.wac.autocore.test;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.gateway.WorkOrderGateway;

import java.util.Arrays;

/**
 * Proves that a work order can be created, changed and confirmed with no window in the way, so
 * the rule about running the business outside the ui rests on a run and not on a document.
 */
public final class WorkOrderProof {

    private WorkOrderProof() {}

    public static void main(String[] args) {

        Db.use(Db.pathFrom(args));
        Db.ensureReady();

        // The gateway covers the work order only. so the caller brings the three ids.
        int vehicleId = Integer.parseInt(args[1]);
        int mechanicId = Integer.parseInt(args[2]);
        int serviceItemId = Integer.parseInt(args[3]);

        WorkOrderGateway gateway = new GarageSystem(new SqliteRepositoryFactory());

        WorkOrder draft = gateway.createDraft(vehicleId, "Bromsok och bromsbelägg");

        System.out.println("1. utkastet skapat, nummer " + draft.getId() + ", status " + draft.getStatus());

        WorkOrder changed = gateway.updateDraft(draft.getId(), vehicleId, "Bromsok och bromsbelägg",
                mechanicId, Arrays.asList(serviceItemId), "2026-10-15", "15:02",
                "", "");

        System.out.println("2. utkastet ändrat, mekaniker " + changed.getMechanicId());

        boolean confirmed = gateway.confirmWorkOrder(draft.getId());

        System.out.println("3. bekräftad: " + (confirmed ? "ja" : "nej"));

        System.out.println("   status efter: " + gateway.findWorkOrder(draft.getId()).getStatus());
    }
}
