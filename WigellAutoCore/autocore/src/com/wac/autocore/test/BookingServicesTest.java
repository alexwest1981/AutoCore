package com.wac.autocore.test;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.ServiceItem;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Enhetstest för SCRUM-147 (A1):
 * Bokningen bär en lista av tjänster i stället för ett enda id.
 * Verifierar att en bokning kan skapas med en eller flera tjänster,
 * att listan läses tillbaka med exakt de som lades in, att antalet
 * och namnen kan skrivas ut, samt att bakåtkompatibilitet för
 * en enskild tjänst upprätthålls.
 */
public class BookingServicesTest {

    public void testBookingWithMultipleServices() {
        ServiceItem s1 = new ServiceItem(1, "Oljebyte", "Byte av motorolja och filter", 899.0, 45);
        ServiceItem s2 = new ServiceItem(2, "Bromsservice", "Kontroll och byte av bromsbelägg", 1495.0, 90);
        ServiceItem s3 = new ServiceItem(3, "Däckbyte", "Skifte av fyra hjul och däcktryckskontroll", 399.0, 30);

        List<ServiceItem> services = Arrays.asList(s1, s2, s3);
        Booking booking = new Booking(1042, 1, LocalDate.of(2026, 10, 15), "Stor genomgång",
                LocalTime.of(8, 0), LocalTime.of(10, 45), 2, services);

        List<ServiceItem> readBack = booking.getServiceItems();

        System.out.println("--- Bokning med flera tjänster (SCRUM-147) ---");
        System.out.println("Antal tjänster i bokning " + booking.getId() + ": " + readBack.size());
        for (ServiceItem s : readBack) {
            System.out.println(" • " + s.getName() + " – " + s.getPrice() + " kr – " + s.getEstimatedMinutes() + " minuter");
        }
        System.out.println("Beräknad total arbetstid: " + booking.getTotalEstimatedMinutes() + " minuter");
        System.out.println("Beräknat pris: " + booking.getTotalEstimatedCost() + " kr");

        TestRunner.assertEquals(3, readBack.size(), "Bokningen ska innehålla exakt 3 tjänster");
        TestRunner.assertEquals("Oljebyte", readBack.get(0).getName(), "Första tjänsten ska vara Oljebyte");
        TestRunner.assertEquals("Bromsservice", readBack.get(1).getName(), "Andra tjänsten ska vara Bromsservice");
        TestRunner.assertEquals("Däckbyte", readBack.get(2).getName(), "Tredje tjänsten ska vara Däckbyte");

        TestRunner.assertEquals(165, booking.getTotalEstimatedMinutes(), "Total arbetstid ska vara 165 minuter");
        TestRunner.assertEquals(2793.0, booking.getTotalEstimatedCost(), "Totalt pris ska vara 2793 kr");

        List<Integer> ids = booking.getServiceItemIds();
        TestRunner.assertEquals(3, ids.size(), "Bokningen ska returnera 3 tjänste-id:n");
        TestRunner.assertEquals(Integer.valueOf(1), ids.get(0), "Första ID ska vara 1");
        TestRunner.assertEquals(Integer.valueOf(2), ids.get(1), "Andra ID ska vara 2");
        TestRunner.assertEquals(Integer.valueOf(3), ids.get(2), "Tredje ID ska vara 3");
    }

    public void testBookingWithSingleService() {
        ServiceItem s1 = new ServiceItem(1, "Oljebyte", "Byte av motorolja och filter", 899.0, 45);

        Booking booking = new Booking(1043, 2, LocalDate.of(2026, 10, 16), "Endast olja",
                LocalTime.of(9, 0), LocalTime.of(9, 45), 1, s1);

        List<ServiceItem> readBack = booking.getServiceItems();

        System.out.println("--- Bokning med en enda tjänst (SCRUM-147) ---");
        System.out.println("Antal tjänster i bokning " + booking.getId() + ": " + readBack.size());
        for (ServiceItem s : readBack) {
            System.out.println(" • " + s.getName() + " – " + s.getPrice() + " kr – " + s.getEstimatedMinutes() + " minuter");
        }

        TestRunner.assertEquals(1, readBack.size(), "Bokningen ska innehålla exakt 1 tjänst");
        TestRunner.assertEquals("Oljebyte", readBack.get(0).getName(), "Tjänstens namn ska vara Oljebyte");
        TestRunner.assertEquals(1, booking.getServiceItemId(), "getServiceItemId() ska vara bakåtkompatibelt och returnera 1");
        TestRunner.assertEquals(45, booking.getTotalEstimatedMinutes(), "Total arbetstid ska vara 45 minuter");
        TestRunner.assertEquals(899.0, booking.getTotalEstimatedCost(), "Totalt pris ska vara 899 kr");
    }

    public void testAddAndRemoveServicesDynamically() {
        ServiceItem s1 = new ServiceItem(1, "Oljebyte", "Byte av motorolja", 899.0, 45);
        ServiceItem s2 = new ServiceItem(2, "Bromsservice", "Byte av bromsbelägg", 1495.0, 90);

        Booking booking = new Booking(3, LocalDate.now(), "Dynamisk tjänstetest");
        booking.addServiceItem(s1);

        TestRunner.assertEquals(1, booking.getServiceItems().size(), "En tjänst tillagd");
        TestRunner.assertEquals(899.0, booking.getTotalEstimatedCost(), "Pris för 1 tjänst");

        booking.addServiceItem(s2);
        TestRunner.assertEquals(2, booking.getServiceItems().size(), "Två tjänster tillagda");
        TestRunner.assertEquals(2394.0, booking.getTotalEstimatedCost(), "Pris för 2 tjänster sammanlagt");

        boolean removed = booking.removeServiceItem(s1);
        TestRunner.assertTrue(removed, "Tjänst s1 ska ha tagits bort");
        TestRunner.assertEquals(1, booking.getServiceItems().size(), "Endast en tjänst kvar efter borttagning");
        TestRunner.assertEquals("Bromsservice", booking.getServiceItems().get(0).getName(), "Kvarvarande tjänst är Bromsservice");
        TestRunner.assertEquals(1495.0, booking.getTotalEstimatedCost(), "Pris efter borttagning");
    }

    public void testLegacyConstructorCompatibility() {
        Booking legacyBooking = new Booking(4, LocalDate.now(), "Äldre bokning",
                LocalTime.of(13, 0), LocalTime.of(14, 0), 2, 7);

        TestRunner.assertEquals(7, legacyBooking.getServiceItemId(), "Äldre konstruktor ska sätta serviceItemId");
        List<Integer> ids = legacyBooking.getServiceItemIds();
        TestRunner.assertEquals(1, ids.size(), "getServiceItemIds() ska innehålla det gamla ID:t");
        TestRunner.assertEquals(Integer.valueOf(7), ids.get(0), "ID ska matcha gamla serviceItemId");
    }

    public void testEntityLookupBookingServicesFormatting() {
        ServiceItem s1 = new ServiceItem(1, "Oljebyte", "Byte av olja", 899.0, 45);
        ServiceItem s2 = new ServiceItem(2, "Bromsservice", "Byte av bromsar", 1495.0, 90);
        ServiceItem s3 = new ServiceItem(3, "Däckbyte", "Skifte av hjul", 399.0, 30);

        Booking booking = new Booking(1, 1, LocalDate.now(), "Fler tjänster bokning",
                LocalTime.of(8, 0), LocalTime.of(10, 45), 1, Arrays.asList(s1, s2, s3));

        String formatted = com.wac.autocore.ui.util.EntityLookup.bookingServices(null, booking);
        TestRunner.assertEquals("Oljebyte, Bromsservice, Däckbyte", formatted,
                "EntityLookup ska formatera samtliga tjänstenamn kommaseparerade för vyn");

        Booking singleBooking = new Booking(2, 1, LocalDate.now(), "En tjänst",
                LocalTime.of(8, 0), LocalTime.of(8, 45), 1, s1);
        String singleFormatted = com.wac.autocore.ui.util.EntityLookup.bookingServices(null, singleBooking);
        TestRunner.assertEquals("Oljebyte", singleFormatted,
                "EntityLookup ska visa enskilt tjänstenamn för bokning med en tjänst");
    }
}
