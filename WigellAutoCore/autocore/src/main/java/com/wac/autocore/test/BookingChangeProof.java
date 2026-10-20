package com.wac.autocore.test;

import com.wac.autocore.data.Db;
import com.wac.autocore.exception.RuleViolationException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.service.GarageSystem;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A changed booking is tested the same way as a new one: a change that collides is refused with
 * the same message a new booking gets, and a change that only touches itself goes through.
 */
public final class BookingChangeProof {

    private BookingChangeProof() {}

    public static void main(String[] args) throws Exception {

        File database = File.createTempFile("booking-change-", ".db");
        database.deleteOnExit();
        // Nothing to copy: the program makes the database itself.
        database.delete();


        GarageSystem garage = new GarageSystem(new SqliteRepositoryFactory(new Db(database.getAbsolutePath())));

        int vehicleId = garage.getVehicles().get(0).getId();
        Mechanic mechanic = garage.getMechanics().get(0);
        ServiceItem service = garage.getServiceItems().get(0);
        // A Wednesday far ahead, so the times never meet the seed data.
        LocalDate day = LocalDate.of(2027, 3, 3);

        Booking first = garage.createBooking(vehicleId, day, "Första", LocalTime.of(10, 0),
                mechanic.getId(), service.getId());
        Booking second = garage.createBooking(vehicleId, day, "Andra", LocalTime.of(13, 0),
                mechanic.getId(), service.getId());

        System.out.println("Ändrad bokning mot ny bokning");
        System.out.println();

        // The same collision, once as a new booking and once as a change of the second one.
        String asNew = refusal(() -> garage.createBooking(vehicleId, day, "Ny", LocalTime.of(10, 0),
                mechanic.getId(), service.getId()));
        String asChange = refusal(() -> {
            second.setStartTime(LocalTime.of(10, 0));
            second.setEndTime(LocalTime.of(11, 0));
            garage.saveBooking(second);
        });
        System.out.println("ny bokning kl 10:     " + asNew);
        System.out.println("ändrad bokning kl 10: " + asChange);

        // A change that keeps the booking in its own time is not a collision with itself.
        String ownTime = refusal(() -> {
            first.setDescription("Första, ändrad text");
            garage.saveBooking(first);
        });
        System.out.println("ändrad text, samma tid: " + ownTime);

        String weekendNew = refusal(() -> garage.createBooking(vehicleId, LocalDate.of(2027, 3, 6), "Lördag",
                LocalTime.of(9, 0), mechanic.getId(), service.getId()));
        String weekendChange = refusal(() -> {
            first.setDate(LocalDate.of(2027, 3, 6));
            garage.saveBooking(first);
        });
        System.out.println("ny bokning på lördag:     " + weekendNew);
        System.out.println("ändrad bokning på lördag: " + weekendChange);

        String lateNew = refusal(() -> garage.createBooking(vehicleId, day, "Sen", LocalTime.of(16, 50),
                mechanic.getId(), service.getId()));
        String lateChange = refusal(() -> {
            second.setStartTime(LocalTime.of(16, 50));
            second.setEndTime(LocalTime.of(17, 50));
            garage.saveBooking(second);
        });
        System.out.println("ny bokning kl 16:50:     " + lateNew);
        System.out.println("ändrad bokning kl 16:50: " + lateChange);

        boolean sameCollision = asNew.equals(asChange) && asNew.endsWith("slot_busy_error");
        boolean sameWeekend = weekendNew.equals(weekendChange) && weekendNew.endsWith("date_weekend");
        boolean sameClosing = lateNew.equals(lateChange) && lateNew.endsWith("closing_time_exceeded");
        boolean selfFree = ownTime.equals("sparad");

        System.out.println();
        System.out.println("krocken nekas lika:         " + sameCollision);
        System.out.println("helgen nekas lika:          " + sameWeekend);
        System.out.println("stängningstiden nekas lika: " + sameClosing);
        System.out.println("egen tid är ingen krock:    " + selfFree);
        System.exit(sameCollision && sameWeekend && sameClosing && selfFree ? 0 : 1);
    }

    /** "sparad" when the save went through, otherwise the message key of the refusal. */
    private static String refusal(Runnable save) {
        try {
            save.run();
            return "sparad";
        } catch (RuleViolationException refused) {
            return "nekad: " + refused.getMessageKey();
        }
    }
}