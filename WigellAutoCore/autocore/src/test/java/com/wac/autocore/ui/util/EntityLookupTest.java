package com.wac.autocore.ui.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.security.Identity;
import com.wac.autocore.security.IdentityHolder;
import com.wac.autocore.security.Role;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.GuardedGarageSystem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/** The time column on the work order list, which a draft fills in from its services. */
class EntityLookupTest {

    private File file;

    private GarageSystem garage;

    @BeforeEach
    void buildDatabase() {
        try {
            file = File.createTempFile("entity-lookup-", ".db");
            file.deleteOnExit();
            file.delete();

            Db db = new Db(file.getAbsolutePath());
            db.ensureReady();

            IdentityHolder identity = new IdentityHolder();
            identity.set(new Identity("provet", Role.ADMIN));
            garage = new GuardedGarageSystem(new SqliteRepositoryFactory(db), identity);

        } catch (Exception cannotStart) {
            throw new IllegalStateException("Kunde inte bygga provets databas", cannotStart);
        }
    }

    @AfterEach
    void removeDatabase() {
        if (file != null) {
            file.delete();
        }
    }

    /** A draft shows the hour it was planned for and the hour its services end. */
    @Test
    void aDraftShowsStartAndTheEndItsServicesGive() {
        ServiceItem brakes = serviceNamed("brake_service");
        WorkOrder draft = draftAt(LocalTime.of(10, 0), brakes.getId());

        LocalTime end = LocalTime.of(10, 0).plusMinutes(brakes.getEstimatedMinutes());

        assertEquals("10:00 - " + end, EntityLookup.workOrderTime(garage, draft));
    }

    /** Two services add their time up, the same way a booking counts. */
    @Test
    void twoServicesAddUpTheirTime() {
        ServiceItem brakes = serviceNamed("brake_service");
        ServiceItem oil = serviceNamed("oil_change");
        WorkOrder draft = draftAt(LocalTime.of(8, 15), brakes.getId(), oil.getId());

        int minutes = brakes.getEstimatedMinutes() + oil.getEstimatedMinutes();

        assertEquals("08:15 - " + LocalTime.of(8, 15).plusMinutes(minutes),
                EntityLookup.workOrderTime(garage, draft));
    }

    /** A draft with no service yet still has an hour, like a booking without a length. */
    @Test
    void aDraftWithoutServicesGetsAnHour() {
        WorkOrder draft = draftAt(LocalTime.of(13, 0));

        assertEquals("13:00 - 14:00", EntityLookup.workOrderTime(garage, draft));
    }

    /** A draft with no hour chosen shows nothing rather than a made up time. */
    @Test
    void aDraftWithoutAnHourShowsADash() {
        WorkOrder draft = draftAt(null, serviceNamed("oil_change").getId());

        assertEquals("-", EntityLookup.workOrderTime(garage, draft));
        assertTrue(draft.getPlannedTime() == null);
    }

    private WorkOrder draftAt(LocalTime start, Integer... serviceIds) {
        WorkOrder draft = new WorkOrder(0, 0, 0);
        draft.setPlannedTime(start);
        draft.setServiceItemIds(Arrays.asList(serviceIds));
        return draft;
    }

    private ServiceItem serviceNamed(String code) {
        List<ServiceItem> services = garage.getServiceItems();
        for (ServiceItem service : services) {
            String name = service.getName() == null ? "" : service.getName();
            if (name.contains(code)) {
                return service;
            }
        }
        throw new IllegalStateException("Hittade ingen tjänst med " + code + " i namnet");
    }
}
