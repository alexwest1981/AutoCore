package com.wac.autocore.ui;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Ingången till arbetsorderdialogerna. Själva dialogerna ligger i varsin klass:
 * CreateWorkOrderDialog, WorkOrderDetailsDialog och MarkPerformedDialog,
 * och fördelningen av tjänster i WorkOrderPlan.
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

    /** Planen: en post per mekaniker med den mekanikerns tjänster. */
    public static LinkedHashMap<Integer, List<ServiceItem>> planWorkOrders(GarageSystem garage, Booking booking) {
        return WorkOrderPlan.planWorkOrders(garage, booking);
    }
}
