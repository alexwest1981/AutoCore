package com.wac.autocore.test;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.WorkOrderRepository;
import com.wac.autocore.service.BillingService;
import com.wac.autocore.service.WorkOrderService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.wac.autocore.test.TestRunner.assertEquals;
import static com.wac.autocore.test.TestRunner.assertNotNull;

public class ServiceitemTests {

    WorkOrderRepository workOrderRepository = new WorkOrderRepository();
    WorkOrderService workOrderService = new WorkOrderService();
    BillingService billingService = new BillingService();


//    public void testInvoiceOnlyContainsCompletedServiceItems() {
//        System.out.println("--- STARTAR TEST: Faktura med endast utförda tjänster ---");
//
//        try {
//            int testWorkOrderId = 999;
//            WorkOrder workOrder = new WorkOrder(testWorkOrderId, 1, 1);
//
//            workOrder.addServiceItemId(101);
//            workOrder.addServiceItemId(102);
//            workOrder.addServiceItemId(103);
//
//            workOrderRepository.save(workOrder);
//
//            int[] completedServiceItemIds = {101, 102};
//            workOrderService.markServicesAsCompleted(testWorkOrderId, completedServiceItemIds);
//
//            Invoice invoice = billingService.createInvoice(testWorkOrderId, null);
//
//            if (invoice == null) {
//                System.out.println("TEST MISSLYCKADES: Fakturan skapades inte (är null).");
//                return;
//            }
//
//            int antaletRader = invoice.getLines().size();
//            if (antaletRader == 2) {
//                System.out.println("TEST LYCKADES! Fakturan innehåller exakt " + antaletRader + " rader.");
//            } else {
//                System.out.println("TEST MISSLYCKADES: Fakturan har " + antaletRader + " rader, men förväntade sig 2!");
//            }
//
//        } catch (Exception e) {
//            System.out.println("TEST MISSLYCKADES på grund av ett oväntat fel: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
}
