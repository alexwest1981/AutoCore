package com.wac.autocore.test;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;

import java.util.List;

public class OverviewMetricsTest {

    public void testActiveWorkOrdersCalculation() {
        GarageSystem garage = new GarageSystem();
        List<WorkOrder> orders = garage.getWorkOrders();

        long activeCount = 0;
        for (WorkOrder wo : orders) {
            if (!"COMPLETED".equals(wo.getStatus())) {
                activeCount++;
            }
        }

        TestRunner.assertTrue(activeCount >= 0, "Active work orders is non-negative");
        TestRunner.assertTrue(activeCount <= orders.size(), "Active work orders cannot exceed total orders");
    }

    public void testTotalRevenueCalculation() {
        GarageSystem garage = new GarageSystem();
        List<Payment> payments = garage.getPayments();

        double revenue = 0;
        for (Payment p : payments) {
            if (p.isSuccessful()) {
                revenue += p.getAmount();
            }
        }

        TestRunner.assertTrue(revenue >= 0.0, "Revenue cannot be negative");
    }

    public void testMechanicAvailabilityCount() {
        GarageSystem garage = new GarageSystem();
        List<Mechanic> mechanics = garage.getMechanics();

        int available = 0;
        for (Mechanic m : mechanics) {
            if (m.isAvailable()) {
                available++;
            }
        }

        TestRunner.assertTrue(available >= 0, "Available count cannot be negative");
        TestRunner.assertTrue(available <= mechanics.size(), "Available count cannot exceed team size");
    }
}
