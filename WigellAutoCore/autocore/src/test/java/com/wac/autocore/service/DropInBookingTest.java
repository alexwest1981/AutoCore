package com.wac.autocore.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wac.autocore.data.Db;
import com.wac.autocore.exception.RuleViolationException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.SqliteRepositoryFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;

/**
 * SCRUM-326: a drop-in happens the day it happens, so the day and the hour are the caller's to give.
 * The window hands it the clock; a proof hands it a fixed weekday, and then the answer is the same
 * whenever the proof is run.
 */
class DropInBookingTest {

    /** A weekday the workshop is open, far enough ahead that the seed data cannot hold it. */
    private static final LocalDate OPEN_DAY = LocalDate.of(2027, 3, 3);

    private File file;

    private GarageSystem garage;

    private int vehicleId;

    private Mechanic mechanic;

    private ServiceItem service;

    @BeforeEach
    void openADatabase() throws Exception {
        file = File.createTempFile("dropin-", ".db");
        file.deleteOnExit();
        file.delete();

        Db db = new Db(file.getAbsolutePath());
        db.ensureReady();
        garage = new GarageSystem(new SqliteRepositoryFactory(db));

        Vehicle vehicle = garage.getVehicles().get(0);
        vehicleId = vehicle.getId();
        mechanic = garage.getMechanics().get(0);
        service = garage.getServiceItems().get(0);
    }

    @AfterEach
    void closeTheDatabase() {
        file.delete();
    }

    @Test
    void theDayTheCallerGivesIsTheDayTheCarComesIn() {

        Booking dropIn = garage.createDropInBooking(vehicleId, Arrays.asList(service),
                Arrays.asList(mechanic), OPEN_DAY, LocalTime.of(8, 0));

        assertNotNull(dropIn);
        assertEquals(OPEN_DAY, dropIn.getDate());
        assertEquals(LocalTime.of(8, 0), dropIn.getStartTime());
    }

    @Test
    void theHourTheCallerGivesIsWhereTheSearchStarts() {

        Booking fromTen = garage.createDropInBooking(vehicleId, Arrays.asList(service),
                Arrays.asList(mechanic), OPEN_DAY, LocalTime.of(10, 0));

        assertNotNull(fromTen);
        assertEquals(LocalTime.of(10, 0), fromTen.getStartTime());
    }

    @Test
    void aTakenHourIsSteppedOverOnTheDayTheCallerGave() {

        // The first drop-in holds eight o'clock, so a second one from eight has to take nine.
        garage.createDropInBooking(vehicleId, Arrays.asList(service), Arrays.asList(mechanic),
                OPEN_DAY, LocalTime.of(8, 0));

        Booking second = garage.createDropInBooking(vehicleId, Arrays.asList(service),
                Arrays.asList(mechanic), OPEN_DAY, LocalTime.of(8, 0));

        assertNotNull(second);
        assertEquals(OPEN_DAY, second.getDate());
        assertEquals(LocalTime.of(9, 0), second.getStartTime());
    }

    @Test
    void aSaturdayIsRefusedHoweverTheHourIsGiven() {

        LocalDate saturday = LocalDate.of(2027, 3, 6);

        assertThrows(RuleViolationException.class,
                () -> garage.createDropInBooking(vehicleId, Arrays.asList(service),
                        Arrays.asList(mechanic), saturday, LocalTime.of(9, 0)));
    }
}
