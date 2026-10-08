package com.wac.autocore.ui;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * The entry point to the work order dialogs. The dialogs themselves live in a class
 * each: CreateWorkOrderDialog, WorkOrderDetailsDialog and MarkPerformedDialog,
 * and the split of the services in WorkOrderPlan.
 */
public final class WorkOrderDialogs {

    private WorkOrderDialogs() {}

    public static void showCreateWorkOrderDialog(GarageSystem garage, Runnable onSuccess) {
        CreateWorkOrderDialog.show(garage, null, onSuccess);
    }

    public static void showCreateWorkOrderDialog(GarageSystem garage, Booking defaultBooking, Runnable onSuccess) {
        CreateWorkOrderDialog.show(garage, defaultBooking, onSuccess);
    }

    public static void showCreateDropInWorkOrderDialog(GarageSystem garage, Runnable onSuccess) {
        CreateDropInWorkOrderDialog.show(garage, onSuccess);
    }

    public static void showCreateReclamationDialog(GarageSystem garage, WorkOrder original, Runnable onSuccess) {
        CreateReclamationDialog.show(garage, original, onSuccess);
    }

    public static void showWorkOrderDetailsDialog(GarageSystem garage, WorkOrder workOrder, Runnable onRefresh) {
        WorkOrderDetailsDialog.show(garage, workOrder, onRefresh);
    }

    public static void showMarkPerformedDialog(GarageSystem garage, WorkOrder workOrder, Runnable onSuccess) {
        MarkPerformedDialog.show(garage, workOrder, onSuccess);
    }

    /** The plan: one entry per mechanic with that mechanic's services. */
    public static LinkedHashMap<Integer, List<ServiceItem>> planWorkOrders(GarageSystem garage, Booking booking) {
        return WorkOrderPlan.planWorkOrders(garage, booking);
    }
}
