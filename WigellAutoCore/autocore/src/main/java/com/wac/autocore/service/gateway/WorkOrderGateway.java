package com.wac.autocore.service.gateway;

import com.wac.autocore.model.DraftChanges;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderType;
import java.util.List;

public interface WorkOrderGateway {

    List<WorkOrder> getWorkOrders();

    WorkOrder findWorkOrder(int id);

    WorkOrder createWorkOrder(int bookingId, int mechanicId, List<Integer> serviceItemIds, WorkOrderType type);

    WorkOrder createDraft(int vehicleId, String description);

    WorkOrder updateDraft(DraftChanges changes);

    WorkOrder createReclamation(int originalWorkOrderId, String description);

    void startWorkOrder(int workOrderId);

    void completeWorkOrder(int workOrderId);

    boolean cancelWorkOrder(int workOrderId);

    boolean confirmWorkOrder(int workOrderId);
}
