package com.wac.autocore.test;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.util.BookingAvailability;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Prov för den felrapporterade dubbelbokningen: en bokning på 210 minuter som startar 12:00 går in i
 * en bokning 13:00-16:30, och tillgänglighetskontrollen tittade bara på den timme bokningen startade i
 * och släppte därför igenom överlappningen.
 *
 * Tiderna ligger långt fram i tiden, så provet rör inte de schemalagda demotiderna i MechanicSchedule.
 */
public class BookingOverlapTest {

    private static final LocalDate FAR_AWAY = LocalDate.of(2027, 6, 15);
    private static final int FREE_MECHANIC_ID = 9999;

    /** Skapar en bokning 13:00-16:30 (210 minuter) för en mekaniker utan andra uppdrag den dagen. */
    private Booking createBlockingBooking(GarageSystem garage) throws Exception {
        List<Vehicle> vehicles = garage.getVehicles();
        TestRunner.assertTrue(!vehicles.isEmpty(), "Det finns inget fordon att boka på — databasen är tom");

        Booking blocking = garage.createBooking(vehicles.get(0).getId(), FAR_AWAY, "Test: blockerande bokning");
        TestRunner.assertNotNull(blocking, "Den blockerande bokningen ska kunna skapas");
        blocking.setMechanicId(FREE_MECHANIC_ID);
        blocking.setStatus("BOOKED");
        blocking.setStartTime(LocalTime.of(13, 0));
        blocking.setEndTime(LocalTime.of(16, 30));
        garage.updateBooking(blocking);
        return blocking;
    }

    private Mechanic mechanic(int id) {
        return new Mechanic(id, "Testmekaniker", "070-0000000", "Bromsar");
    }

    public void testAJobStartingBeforeABookingMayNotRunIntoIt() throws Exception {
        GarageSystem garage = new GarageSystem();
        Booking blocking = createBlockingBooking(garage);
        try {
            // 210 minuter från 12:00 slutar 15:30 och ligger inne i 13:00-16:30.
            TestRunner.assertTrue(
                    BookingAvailability.isRangeBooked(garage, mechanic(FREE_MECHANIC_ID), FAR_AWAY,
                            LocalTime.of(12, 0), LocalTime.of(15, 30), 0),
                    "En bokning 12:00-15:30 får inte gå in i en befintlig bokning 13:00-16:30");

            // Den gamla kontrollen tittade bara på 12:00, som är ledig — det var därför överlappningen gick igenom.
            TestRunner.assertFalse(
                    BookingAvailability.isHourBooked(garage, mechanic(FREE_MECHANIC_ID), FAR_AWAY, 12, 0),
                    "Starttimmen 12:00 är ledig; felet var att hela bokningstiden inte kontrollerades");

            // Grannbokningen 10:00-12:00 slutar precis när den andra börjar och ska fortfarande gå bra.
            TestRunner.assertFalse(
                    BookingAvailability.isRangeBooked(garage, mechanic(FREE_MECHANIC_ID), FAR_AWAY,
                            LocalTime.of(10, 0), LocalTime.of(12, 0), 0),
                    "En bokning som slutar precis när nästa börjar krockar inte");

            // Vid redigering av bokningen själv får den inte krocka med sig själv.
            TestRunner.assertFalse(
                    BookingAvailability.isRangeBooked(garage, mechanic(FREE_MECHANIC_ID), FAR_AWAY,
                            LocalTime.of(12, 0), LocalTime.of(15, 30), blocking.getId()),
                    "Bokningen ska kunna flyttas utan att krocka med sin egen gamla tid");
        } finally {
            garage.deleteBooking(blocking.getId());
        }
    }

    public void testTheBusyHoursStillCoverTheWholeJob() throws Exception {
        GarageSystem garage = new GarageSystem();
        Booking blocking = createBlockingBooking(garage);
        try {
            for (int hour = 13; hour <= 15; hour++) {
                TestRunner.assertTrue(
                        BookingAvailability.isHourBooked(garage, mechanic(FREE_MECHANIC_ID), FAR_AWAY, hour, 0),
                        "Timmen " + hour + ":00 ligger inne i 13:00-16:30 och ska visas som upptagen");
            }
            TestRunner.assertTrue(
                    BookingAvailability.isHourBooked(garage, mechanic(FREE_MECHANIC_ID), FAR_AWAY, 16, 0),
                    "Timmen 16:00-17:00 täcks delvis av bokningen som slutar 16:30 och ska visas som upptagen");
            TestRunner.assertFalse(
                    BookingAvailability.isHourBooked(garage, mechanic(FREE_MECHANIC_ID), FAR_AWAY, 12, 0),
                    "Timmen 12:00-13:00 ligger före bokningen och ska vara ledig");
        } finally {
            garage.deleteBooking(blocking.getId());
        }
    }

    /**
     * Tjänstelagret har en egen överlappningskontroll för anrop som inte går via formuläret.
     * Den ska neka överlappningen och fortfarande tillåta en tid som är ledig.
     */
    public void testTheServiceRefusesAnOverlappingBooking() throws Exception {
        GarageSystem garage = new GarageSystem();
        Booking blocking = createBlockingBooking(garage);
        Booking accepted = null;
        try {
            try {
                garage.createBooking(blocking.getVehicleId(), FAR_AWAY, "Test: överlappande",
                        LocalTime.of(14, 0), FREE_MECHANIC_ID, 0);
                TestRunner.assertTrue(false, "Tjänstelagret ska neka en bokning som överlappar 13:00-16:30");
            } catch (IllegalArgumentException expected) {
                TestRunner.assertTrue(expected.getMessage() != null, "Nekandet ska förklara varför");
            }

            accepted = garage.createBooking(blocking.getVehicleId(), FAR_AWAY, "Test: ledig tid",
                    LocalTime.of(17, 0), FREE_MECHANIC_ID, 0);
            TestRunner.assertNotNull(accepted, "En tid utanför den bokade perioden ska fortfarande gå att boka");
        } finally {
            if (accepted != null) {
                garage.deleteBooking(accepted.getId());
            }
            garage.deleteBooking(blocking.getId());
        }
    }
}
