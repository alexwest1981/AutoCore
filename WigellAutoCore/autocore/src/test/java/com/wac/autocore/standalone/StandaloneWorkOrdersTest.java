package com.wac.autocore.standalone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.service.GarageSystem;

/** The program the task asks for, checked the same way a person would: run it, then read the row. */
class StandaloneWorkOrdersTest {

    @Test
    void finishesAWorkOrderWithoutSpring() throws Exception {

        Path file = Files.createTempFile("autocore-standalone-", ".db");
        Files.deleteIfExists(file);

        StandaloneWorkOrders.main(new String[] { file.toString() });

        Db db = new Db(file.toString());
        db.ensureReady();
        GarageSystem garage = new GarageSystem(new SqliteRepositoryFactory(db));

        WorkOrder newest = newest(garage.getWorkOrders());

        assertNotNull(newest);
        assertEquals(WorkOrderStatus.COMPLETED, newest.getStatus());
    }

    private static WorkOrder newest(List<WorkOrder> orders) {

        WorkOrder newest = null;

        for (WorkOrder order : orders) {
            if (newest == null || order.getId() > newest.getId()) {
                newest = order;
            }
        }

        return newest;
    }
}
