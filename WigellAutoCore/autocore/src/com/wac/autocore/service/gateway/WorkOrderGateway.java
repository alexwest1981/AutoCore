package com.wac.autocore.service.gateway;

import com.wac.autocore.model.WorkOrder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface WorkOrderGateway {

    List<WorkOrder> getWorkOrders();

    WorkOrder findWorkOrder(int id);

    WorkOrder createWorkOrder(int bookingId, int mechanicId, List<Integer> serviceItemIds, String type);

    WorkOrder createDraft(int vehicleId, String description);

    WorkOrder updateDraft(int workOrderId, int vehicleId, String description, int mechanicId,
                          List<Integer> serviceItemIds, LocalDate plannedDate, LocalTime plannedTime,
                          String customerInstructions, String otherComments);

    WorkOrder createReclamation(int originalWorkOrderId, String description);

    void startWorkOrder(int workOrderId);

    void completeWorkOrder(int workOrderId);

    boolean cancelWorkOrder(int workOrderId);

    boolean confirmWorkOrder(int workOrderId);
}
