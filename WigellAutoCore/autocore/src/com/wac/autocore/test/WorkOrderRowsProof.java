package com.wac.autocore.test;

import com.wac.autocore.model.DraftChanges;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.gateway.WorkOrderGateway;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * B5: the same work order, created, changed and cancelled, has to leave the same rows in the
 * database no matter who asks for it. Section 2 wants the business to be usable without the
 * window, and that is only worth something if the window and the service layer end up in the
 * same place.
 *
 * A proof cannot open a window, so the window's side is the calls the dialogs make, in their
 * order: the draft and the change as EditDraftDialog makes them, the cancellation as
 * WorkOrderDetailsDialog does it. That side is compared against the same work done through
 * WorkOrderGateway, the narrow entrance from B3. What it catches is the two entrances drifting
 * apart, not a change made inside a dialog.
 *
 * Runs against a copy of the database; the real one is never touched.
 */
public final class WorkOrderRowsProof {

    private static final String DESCRIPTION = "Bromsok och bromsbelagg";
    private static final String CHANGED_DESCRIPTION = "Bromsok, bromsbelagg och vatska";
    private static final String INSTRUCTIONS = "Nycklarna ligger i bilen.";
    private static final String COMMENTS = "Kunden ringer innan hamtning.";
    private static final LocalDate PLANNED_DATE = LocalDate.of(2026, 10, 16);
    private static final LocalTime PLANNED_TIME = LocalTime.of(9, 30);

    private WorkOrderRowsProof() {}

    public static void main(String[] args) throws Exception {
        File original = new File(args.length > 0 ? args[0] : "data/autocore.db");
        File working = File.createTempFile("work-order-rows-", ".db");
        working.deleteOnExit();
        Files.copy(original.toPath(), working.toPath(), StandardCopyOption.REPLACE_EXISTING);

        System.out.println("B5 · samma operation ger samma svar utan fönstret");
        System.out.println("register: " + working.getAbsolutePath() + " (kopia av " + original + ")");
        System.out.println();

        GarageSystem garage = new GarageSystem(new SqliteRepositoryFactory(working.getAbsolutePath()));
        int vehicleId = firstVehicle(garage).getId();
        int mechanicId = firstMechanic(garage).getId();
        int serviceItemId = firstServiceItem(garage).getId();

        System.out.println("1. utan fönster, genom WorkOrderGateway");
        WorkOrderGateway gateway = garage;
        int withoutWindow = withoutTheWindow(gateway, vehicleId, mechanicId, serviceItemId);
        System.out.println();

        System.out.println("2. genom fönstret, samma anrop som dialogerna gör");
        int throughWindow = throughTheWindow(garage, vehicleId, mechanicId, serviceItemId);
        System.out.println();

        List<String> orderWithout = rows(working, "work_orders", "id", withoutWindow);
        List<String> orderThrough = rows(working, "work_orders", "id", throughWindow);
        List<String> servicesWithout = rows(working, "work_order_service_items", "work_order_id", withoutWindow);
        List<String> servicesThrough = rows(working, "work_order_service_items", "work_order_id", throughWindow);

        System.out.println("3. raderna, utan id som med flit skiljer sig");
        System.out.println("   arbetsorder, utan fönster:      " + orderWithout);
        System.out.println("   arbetsorder, genom fönstret:    " + orderThrough);
        System.out.println("   tjänster, utan fönster:         " + servicesWithout);
        System.out.println("   tjänster, genom fönstret:       " + servicesThrough);
        System.out.println();

        boolean sameOrder = orderWithout.equals(orderThrough);
        boolean sameServices = servicesWithout.equals(servicesThrough);
        boolean rightRows = rightRows(working, withoutWindow, vehicleId, mechanicId, serviceItemId);
        boolean readsTheSame = readsTheSame(garage, working, withoutWindow);

        System.out.println("samma rader för arbetsordern:         " + sameOrder);
        System.out.println("samma rader för tjänsterna:           " + sameServices);
        System.out.println("raderna är vad operationen ska lämna: " + rightRows);
        System.out.println("findWorkOrder svarar vad raden säger: " + readsTheSame);
        System.exit(sameOrder && sameServices && rightRows && readsTheSame ? 0 : 1);
    }

    /** The whole operation with nothing but the service layer, through the narrow entrance. */
    private static int withoutTheWindow(WorkOrderGateway gateway, int vehicleId, int mechanicId,
            int serviceItemId) {
        WorkOrder draft = gateway.createDraft(vehicleId, DESCRIPTION);
        System.out.println("   utkast " + draft.getId() + ", status " + draft.getStatus());

        gateway.updateDraft(DraftChanges.forWorkOrder(draft.getId())
                .vehicle(vehicleId)
                .description(CHANGED_DESCRIPTION)
                .mechanic(mechanicId)
                .services(Arrays.asList(Integer.valueOf(serviceItemId)))
                .plannedAt(PLANNED_DATE, PLANNED_TIME)
                .customerInstructions(INSTRUCTIONS)
                .otherComments(COMMENTS)
                .build());
        System.out.println("   ändrat: mekaniker " + mechanicId + ", tjänst " + serviceItemId);

        gateway.cancelWorkOrder(draft.getId());
        System.out.println("   avbrutet");

        return draft.getId();
    }

    /** The same three steps, in the calls the dialogs make, on the object the dialogs hold. */
    private static int throughTheWindow(GarageSystem garage, int vehicleId, int mechanicId,
            int serviceItemId) {
        // EditDraftDialog creates the draft this way.
        WorkOrder draft = garage.createDraft(vehicleId, DESCRIPTION);
        System.out.println("   utkast " + draft.getId() + ", status " + draft.getStatus());

        // EditDraftDialog saves the filled-in draft this way.
        garage.updateDraft(DraftChanges.forWorkOrder(draft.getId())
                .vehicle(vehicleId)
                .description(CHANGED_DESCRIPTION)
                .mechanic(mechanicId)
                .services(Arrays.asList(Integer.valueOf(serviceItemId)))
                .plannedAt(PLANNED_DATE, PLANNED_TIME)
                .customerInstructions(INSTRUCTIONS)
                .otherComments(COMMENTS)
                .build());
        System.out.println("   ändrat: mekaniker " + mechanicId + ", tjänst " + serviceItemId);

        // WorkOrderDetailsDialog cancels the selected order this way.
        garage.cancelWorkOrder(draft.getId());
        System.out.println("   avbrutet");

        return draft.getId();
    }

    /** The rows the operation is supposed to leave behind, the key left out. */
    private static List<String> rows(File database, String table, String keyColumn, int key)
            throws Exception {
        List<String> out = new ArrayList<String>();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT * FROM " + table + " WHERE " + keyColumn + " = " + key)) {
            ResultSetMetaData meta = result.getMetaData();
            while (result.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    String column = meta.getColumnName(i);
                    if (column.equals("id") || column.equals(keyColumn)) {
                        continue;
                    }
                    row.append(column).append('=').append(result.getString(i)).append("  ");
                }
                out.add(row.toString().trim());
            }
        }
        Collections.sort(out);
        return out;
    }

    /** What the three steps are worth: the change is stored, and the status says cancelled. */
    private static boolean rightRows(File database, int orderId, int vehicleId, int mechanicId,
            int serviceItemId) throws Exception {
        String where = " WHERE id = " + orderId;
        return "CANCELLED".equals(value(database, "SELECT status FROM work_orders" + where))
                && String.valueOf(vehicleId).equals(value(database, "SELECT vehicle_id FROM work_orders" + where))
                && String.valueOf(mechanicId).equals(value(database, "SELECT mechanic_id FROM work_orders" + where))
                && CHANGED_DESCRIPTION.equals(value(database, "SELECT description FROM work_orders" + where))
                && PLANNED_DATE.toString().equals(value(database, "SELECT planned_date FROM work_orders" + where))
                && INSTRUCTIONS.equals(value(database, "SELECT customer_instructions FROM work_orders" + where))
                && String.valueOf(serviceItemId).equals(value(database,
                        "SELECT service_item_id FROM work_order_service_items WHERE work_order_id = " + orderId));
    }

    /** What the service layer answers has to be what the row holds. */
    private static boolean readsTheSame(GarageSystem garage, File database, int orderId)
            throws Exception {
        WorkOrder order = garage.findWorkOrder(orderId);
        String where = " WHERE id = " + orderId;
        return order != null
                && order.getStatus().name().equals(value(database, "SELECT status FROM work_orders" + where))
                && String.valueOf(order.getMechanicId()).equals(value(database, "SELECT mechanic_id FROM work_orders" + where))
                && order.getServiceItemIds().contains(Integer.valueOf(
                        Integer.parseInt(value(database, "SELECT service_item_id FROM work_order_service_items"
                                + " WHERE work_order_id = " + orderId))));
    }

    private static String value(File database, String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            return result.next() ? result.getString(1) : null;
        }
    }

    private static Vehicle firstVehicle(GarageSystem garage) {
        List<Vehicle> rows = garage.getVehicles();
        if (rows.isEmpty()) {
            throw new IllegalStateException("Registret har inget fordon att pröva med.");
        }
        return rows.get(0);
    }

    private static Mechanic firstMechanic(GarageSystem garage) {
        List<Mechanic> rows = garage.getMechanics();
        if (rows.isEmpty()) {
            throw new IllegalStateException("Registret har ingen mekaniker att pröva med.");
        }
        return rows.get(0);
    }

    private static ServiceItem firstServiceItem(GarageSystem garage) {
        List<ServiceItem> rows = garage.getServiceItems();
        if (rows.isEmpty()) {
            throw new IllegalStateException("Registret har ingen tjänst att pröva med.");
        }
        return rows.get(0);
    }
}
