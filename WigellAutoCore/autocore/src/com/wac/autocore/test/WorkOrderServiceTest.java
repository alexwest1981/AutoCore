package com.wac.autocore.test;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.InvoiceRepository;
import com.wac.autocore.repository.WorkOrderRepository;
import com.wac.autocore.service.GarageSystem;

import com.wac.autocore.seed.SeedText;
import com.wac.autocore.ui.WorkOrderDialogs;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.EntityLookup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Bevisen för SCRUM-156 (C1), SCRUM-157 (C2) och SCRUM-158 (C3), körda mot en riktig databas.
 */
public class WorkOrderServiceTest {

    /** C1: en arbetsorder på en bokning med flera tjänster får en rad per tjänst. */
    public void testWorkOrderGetsTheBookingServices() throws SQLException {
        GarageSystem garage = new GarageSystem();
        Mechanic mechanic = temporaryMechanic(garage, "C1-mekaniker");
        Booking booking = bookingWithServices(garage, 3, "C1-prov");
        int orderId = 0;

        try {
            WorkOrder order = garage.createWorkOrder(booking.getId(), mechanic.getId());
            TestRunner.assertNotNull(order, "Arbetsordern ska skapas");
            orderId = order.getId();

            TestRunner.assertEquals(3, order.getServiceItemIds().size(),
                    "Arbetsordern ska ha bokningens tre tjänster");
            TestRunner.assertEquals(3, countLinks(orderId),
                    "work_order_service_items ska ha en rad per tjänst");

            garage.startWorkOrder(orderId);
            Booking reloaded = new BookingRepository().findById(booking.getId());
            TestRunner.assertNotNull(reloaded, "Bokningen ska gå att läsa tillbaka");
            TestRunner.assertEquals(3, reloaded.getServiceItemIds().size(),
                    "Bokningen behåller sina tjänster när arbetet startas");

            System.out.println("    [SCRUM-156 BEVIS] Arbetsorder " + orderId
                    + " fick " + countLinks(orderId) + " rader, en per tjänst i bokningen.");
        } finally {
            deleteQuietly(garage, orderId, booking.getId(), mechanic.getId());
        }
    }

    /** C2 (SCRUM-157): Arbetsordern visar vilka arbeten som ska utföras för mekanikern och användaren. */
    public void testWorkOrderDisplaysServicesToBePerformed() throws SQLException {
        GarageSystem garage = new GarageSystem();
        List<ServiceItem> services = garage.getServiceItems();
        TestRunner.assertTrue(services.size() >= 3, "Katalogen måste innehålla minst tre tjänster");
        Mechanic mechanic = temporaryMechanic(garage, "C2-mekaniker");
        Booking booking = bookingWithServices(garage, 3, "C2-prov");
        int orderId = 0;

        try {
            WorkOrder order = garage.createWorkOrder(booking.getId(), mechanic.getId());
            TestRunner.assertNotNull(order, "Arbetsordern ska skapas");
            orderId = order.getId();

            // 1. Verifiera att arbetsordern listar samtliga beställda tjänster
            String uiServices = EntityLookup.workOrderServicesWithPrices(garage, order);
            int expectedMinutes = 0;
            for (int i = 0; i < 3; i++) {
                ServiceItem s = services.get(i);
                expectedMinutes += s.getEstimatedMinutes();
                TestRunner.assertTrue(uiServices.contains(SeedText.resolve(s.getName())),
                        "Arbetsorderns tjänstelista i UI ska innehålla tjänsten " + s.getName());
            }

            // 2. Verifiera beräkning av total tid för alla arbeten som ska utföras
            int actualMinutes = EntityLookup.workOrderTotalMinutes(garage, order);
            TestRunner.assertEquals(expectedMinutes, actualMinutes,
                    "Total beräknad tid för arbetsordern ska matcha summan av dess tjänster");

            // 3. Verifiera att status per arbete visas: före start är alla 'Att utföra'
            String statusBefore = EntityLookup.workOrderServicesWithStatus(garage, order);
            TestRunner.assertTrue(statusBefore.contains(I18n.get("status.to_be_performed")),
                    "Före start ska tjänsterna markeras som 'Att utföra'");

            // 4. Markera första tjänsten som utförd och verifiera att statusen skiljer utfört från ej utfört
            garage.startWorkOrder(orderId);
            WorkOrder loaded = new WorkOrderRepository().findById(orderId);
            loaded.markServiceAsCompleted(services.get(0).getId());
            new WorkOrderRepository().save(loaded);

            WorkOrder reloaded = new WorkOrderRepository().findById(orderId);
            String statusAfter = EntityLookup.workOrderServicesWithStatus(garage, reloaded);
            TestRunner.assertTrue(statusAfter.contains(I18n.get("status.completed")),
                    "Den utförda tjänsten ska markeras som utförd");
            TestRunner.assertTrue(statusAfter.contains(I18n.get("status.to_be_performed")),
                    "Återstående tjänster ska fortfarande markeras som 'Att utföra'");

            System.out.println("    [SCRUM-157 BEVIS] Arbetsorder " + orderId
                    + " visar samtliga 3 arbeten som ska utföras, total beräknad tid (" + actualMinutes + " min) och status per moment.");
        } finally {
            deleteQuietly(garage, orderId, booking.getId(), mechanic.getId());
        }
    }

    /** C3: en arbetsorder där två av tre tjänster är utförda ger en faktura med två rader. */
    public void testInvoiceHasOnlyThePerformedServices() throws SQLException {
        GarageSystem garage = new GarageSystem();
        List<ServiceItem> services = garage.getServiceItems();
        Mechanic mechanic = temporaryMechanic(garage, "C3-mekaniker");
        Booking booking = bookingWithServices(garage, 3, "C3-prov");
        int orderId = 0;
        int invoiceId = 0;

        try {
            WorkOrder order = garage.createWorkOrder(booking.getId(), mechanic.getId());
            TestRunner.assertNotNull(order, "Arbetsordern ska skapas");
            orderId = order.getId();
            garage.startWorkOrder(orderId);

            int first = services.get(0).getId();
            int second = services.get(1).getId();

            WorkOrder loaded = new WorkOrderRepository().findById(orderId);
            loaded.markServiceAsCompleted(first);
            loaded.markServiceAsCompleted(second);
            new WorkOrderRepository().save(loaded);

            WorkOrder check = new WorkOrderRepository().findById(orderId);
            TestRunner.assertEquals(2, check.getCompletedServiceItems().size(),
                    "Två av tre tjänster ska vara markerade som utförda");

            garage.completeWorkOrder(orderId);

            Invoice invoice = garage.createInvoice(orderId, null);
            TestRunner.assertNotNull(invoice, "Fakturan ska kunna skapas");
            invoiceId = invoice.getId();

            Invoice reloaded = new InvoiceRepository().findById(invoiceId);
            TestRunner.assertNotNull(reloaded, "Fakturan ska gå att läsa tillbaka");
            TestRunner.assertEquals(2, reloaded.getLines().size(),
                    "Fakturan ska ha en rad för varje utförd tjänst, inte tre");

            List<Integer> lineIds = new ArrayList<Integer>();
            for (InvoiceLine line : reloaded.getLines()) {
                lineIds.add(line.getServiceItemId());
            }
            TestRunner.assertTrue(lineIds.contains(first),
                    "Den första utförda tjänsten ska finnas på fakturan");
            TestRunner.assertTrue(lineIds.contains(second),
                    "Den andra utförda tjänsten ska finnas på fakturan");

            System.out.println("    [SCRUM-158 BEVIS] Arbetsorder " + orderId
                    + " med två utförda tjänster gav faktura " + invoiceId
                    + " med " + reloaded.getLines().size() + " rader.");
        } finally {
            if (invoiceId > 0) {
                new InvoiceRepository().delete(invoiceId);
            }
            deleteQuietly(garage, orderId, booking.getId(), mechanic.getId());
        }
    }

    /** C3: en arbetsorder utan markeringar faktureras som förut, så gamla ordrar inte tappas. */
    public void testWorkOrderWithoutMarksStillInvoicesAllServices() throws SQLException {
        GarageSystem garage = new GarageSystem();
        Mechanic mechanic = temporaryMechanic(garage, "C3-bakåt-mekaniker");
        Booking booking = bookingWithServices(garage, 2, "C3-bakåt-prov");
        int orderId = 0;
        int invoiceId = 0;

        try {
            WorkOrder order = garage.createWorkOrder(booking.getId(), mechanic.getId());
            TestRunner.assertNotNull(order, "Arbetsordern ska skapas");
            orderId = order.getId();
            garage.startWorkOrder(orderId);
            garage.completeWorkOrder(orderId);

            Invoice invoice = garage.createInvoice(orderId, null);
            TestRunner.assertNotNull(invoice, "Fakturan ska kunna skapas utan markeringar");
            invoiceId = invoice.getId();
            TestRunner.assertEquals(2, invoice.getLines().size(),
                    "Utan markeringar gäller samtliga tjänster, som före C3");
        } finally {
            if (invoiceId > 0) {
                new InvoiceRepository().delete(invoiceId);
            }
            deleteQuietly(garage, orderId, booking.getId(), mechanic.getId());
        }
    }

    /**
     * En bokning med flera tjänster kan delas på flera mekaniker: en arbetsorder per mekaniker, och
     * en tjänst får bara ligga på en av dem. Annars kan samma arbete faktureras två gånger.
     */
    public void testBookingCanBeSplitIntoOneWorkOrderPerMechanic() throws SQLException {
        GarageSystem garage = new GarageSystem();
        Mechanic first = temporaryMechanic(garage, "Delprov mekaniker A");
        Mechanic second = temporaryMechanic(garage, "Delprov mekaniker B");
        Booking booking = bookingWithServices(garage, 4, "Delprov bokning");
        int bookingId = booking.getId();
        int firstOrderId = 0;
        int secondOrderId = 0;

        try {
            List<Integer> all = booking.getServiceItemIds();
            TestRunner.assertEquals(4, all.size(), "Bokningen ska ha fyra tjänster");

            List<Integer> mine = new ArrayList<Integer>(all.subList(0, 2));
            List<Integer> yours = new ArrayList<Integer>(all.subList(2, 4));

            WorkOrder a = garage.createWorkOrder(bookingId, first.getId(), mine);
            TestRunner.assertNotNull(a, "Första arbetsordern ska skapas");
            firstOrderId = a.getId();

            WorkOrder b = garage.createWorkOrder(bookingId, second.getId(), yours);
            TestRunner.assertNotNull(b,
                    "Andra arbetsordern ska skapas: samma bokning men en annan mekaniker");
            secondOrderId = b.getId();

            TestRunner.assertEquals(2, countLinks(firstOrderId),
                    "Första arbetsordern ska ha sina två tjänster");
            TestRunner.assertEquals(2, countLinks(secondOrderId),
                    "Andra arbetsordern ska ha sina två tjänster");

            // Spärren mot dubbel fakturering ligger nu på tjänsten, inte på bokningen.
            TestRunner.assertTrue(garage.createWorkOrder(bookingId, second.getId(), mine) == null,
                    "En tjänst som redan ligger på en arbetsorder får inte läggas på en till");

            TestRunner.assertTrue(garage.createWorkOrder(bookingId, first.getId()) == null,
                    "När alla bokningens tjänster är tagna finns inget kvar till en ny arbetsorder");

            System.out.println("    [DELNING BEVIS] Bokning " + bookingId + " blev arbetsorder "
                    + firstOrderId + " (2 tjänster) och " + secondOrderId + " (2 tjänster).");
        } finally {
            deleteQuietly(garage, firstOrderId, bookingId, first.getId());
            deleteQuietly(garage, secondOrderId, 0, second.getId());
        }
    }

    /** Planen fördelar bokningens tjänster på mekaniker: ingen tjänst tappas eller dubbleras. */
    public void testWorkOrderPlanCoversEveryServiceExactlyOnce() throws SQLException {
        GarageSystem garage = new GarageSystem();
        Booking saved = bookingWithServices(garage, 4, "Planprov bokning");

        try {
            // Samma väg som dialogen: bokningen läses tillbaka med sina tjänster, annars är listan tom.
            Booking booking = new BookingRepository().findById(saved.getId());
            TestRunner.assertNotNull(booking, "Bokningen ska gå att läsa tillbaka");
            TestRunner.assertEquals(4, booking.getServiceItems().size(),
                    "Bokningen ska bära sina fyra tjänster");

            java.util.LinkedHashMap<Integer, List<ServiceItem>> plan =
                    WorkOrderDialogs.planWorkOrders(garage, booking);

            List<Integer> planned = new ArrayList<Integer>();
            for (List<ServiceItem> mine : plan.values()) {
                for (ServiceItem service : mine) {
                    planned.add(Integer.valueOf(service.getId()));
                }
            }

            TestRunner.assertEquals(4, planned.size(),
                    "Alla fyra tjänsterna ska hamna i planen, ingen på två ställen");
            TestRunner.assertTrue(planned.containsAll(booking.getServiceItemIds()),
                    "Planen ska täcka precis bokningens tjänster");

            for (java.util.Map.Entry<Integer, List<ServiceItem>> entry : plan.entrySet()) {
                Mechanic who = null;
                for (Mechanic m : garage.getMechanics()) {
                    if (m.getId() == entry.getKey().intValue()) {
                        who = m;
                    }
                }
                TestRunner.assertNotNull(who, "Varje post i planen ska vara en riktig mekaniker");
            }

            System.out.println("    [PLAN BEVIS] " + booking.getServiceItemIds().size()
                    + " tjänster fördelade på " + plan.size() + " mekaniker.");
        } finally {
            new BookingRepository().delete(saved.getId());
        }
    }

    /**
     * Fakturan ska täcka allt utfört arbete på bokningen, även när arbetet ligger på flera
     * arbetsordrar — och ingen av dem får faktureras om.
     */
    public void testInvoiceCoversAllPerformedWorkOrdersOnABooking() throws SQLException {
        GarageSystem garage = new GarageSystem();
        Mechanic first = temporaryMechanic(garage, "Fakturadel A");
        Mechanic second = temporaryMechanic(garage, "Fakturadel B");
        Booking booking = bookingWithServices(garage, 4, "Fakturadel bokning");
        int bookingId = booking.getId();
        int[] orderIds = new int[]{0, 0};
        int invoiceId = 0;

        try {
            List<Integer> all = booking.getServiceItemIds();
            WorkOrder a = garage.createWorkOrder(bookingId, first.getId(),
                    new ArrayList<Integer>(all.subList(0, 2)));
            WorkOrder b = garage.createWorkOrder(bookingId, second.getId(),
                    new ArrayList<Integer>(all.subList(2, 4)));
            TestRunner.assertNotNull(a, "Första arbetsordern ska skapas");
            TestRunner.assertNotNull(b, "Andra arbetsordern ska skapas");
            orderIds[0] = a.getId();
            orderIds[1] = b.getId();

            garage.startWorkOrder(a.getId());
            garage.completeWorkOrder(a.getId());
            garage.startWorkOrder(b.getId());
            garage.completeWorkOrder(b.getId());

            TestRunner.assertTrue(containsBooking(garage.getInvoiceableBookings(), bookingId),
                    "Bokningen ska gå att fakturera när arbetet är utfört");

            Invoice invoice = garage.createInvoiceForBooking(bookingId, null);
            TestRunner.assertNotNull(invoice, "Fakturan ska skapas för hela bokningen");
            invoiceId = invoice.getId();
            TestRunner.assertEquals(4, invoice.getLines().size(),
                    "Fakturan ska ha alla fyra tjänsterna, från båda arbetsordrarna");

            TestRunner.assertTrue(garage.createInvoice(a.getId(), null) == null,
                    "Första arbetsordern är redan fakturerad");
            TestRunner.assertTrue(garage.createInvoice(b.getId(), null) == null,
                    "Andra arbetsordern är också fakturerad");
            TestRunner.assertTrue(garage.createInvoiceForBooking(bookingId, null) == null,
                    "Bokningen ska inte kunna faktureras två gånger");
            TestRunner.assertTrue(!containsBooking(garage.getInvoiceableBookings(), bookingId),
                    "En färdigfakturerad bokning ska inte erbjudas igen");

            System.out.println("    [FAKTURABEVIS] En faktura med " + invoice.getLines().size()
                    + " rader täcker arbetsorder " + a.getId() + " och " + b.getId() + ".");
        } finally {
            if (invoiceId > 0) {
                new InvoiceRepository().delete(invoiceId);
            }
            deleteQuietly(garage, orderIds[0], bookingId, first.getId());
            deleteQuietly(garage, orderIds[1], 0, second.getId());
        }
    }

    /**
     * Fakturan ska vänta tills hela bokningen är klar. Med en arbetsorder per mekaniker räcker det
     * inte att en eller två av dem är slutförda.
     */
    public void testInvoiceWaitsForEveryWorkOrderOnABooking() throws SQLException {
        GarageSystem garage = new GarageSystem();
        Mechanic first = temporaryMechanic(garage, "Vänteprov A");
        Mechanic second = temporaryMechanic(garage, "Vänteprov B");
        Mechanic third = temporaryMechanic(garage, "Vänteprov C");
        Booking booking = bookingWithServices(garage, 4, "Vänteprov bokning");
        int bookingId = booking.getId();
        int[] orderIds = new int[]{0, 0, 0};
        int invoiceId = 0;

        try {
            List<Integer> all = booking.getServiceItemIds();
            WorkOrder a = garage.createWorkOrder(bookingId, first.getId(),
                    new ArrayList<Integer>(all.subList(0, 2)));
            WorkOrder b = garage.createWorkOrder(bookingId, second.getId(),
                    new ArrayList<Integer>(all.subList(2, 3)));
            WorkOrder c = garage.createWorkOrder(bookingId, third.getId(),
                    new ArrayList<Integer>(all.subList(3, 4)));
            orderIds[0] = a.getId();
            orderIds[1] = b.getId();
            orderIds[2] = c.getId();

            garage.startWorkOrder(a.getId());
            garage.completeWorkOrder(a.getId());
            garage.startWorkOrder(b.getId());
            garage.completeWorkOrder(b.getId());

            TestRunner.assertTrue(!containsBooking(garage.getInvoiceableBookings(), bookingId),
                    "Bokningen ska inte erbjudas medan en arbetsorder återstår");
            TestRunner.assertTrue(garage.createInvoiceForBooking(bookingId, null) == null,
                    "Fakturan ska inte kunna skapas förrän alla arbetsordrar är slutförda");
            TestRunner.assertTrue(garage.hasBookingWithUnfinishedWork(),
                    "Systemet ska veta att det finns ofullbordat arbete");
            TestRunner.assertTrue(garage.createInvoice(a.getId(), null) == null,
                    "Inte heller en enskild arbetsorder ska gå att fakturera medan syskonen är kvar");
            System.out.println("    [FAKTURABEVIS] 2 av 3 arbetsordrar klara: ingen faktura, "
                    + "och bokningen erbjuds inte.");

            garage.startWorkOrder(c.getId());
            garage.completeWorkOrder(c.getId());

            TestRunner.assertTrue(containsBooking(garage.getInvoiceableBookings(), bookingId),
                    "Bokningen ska erbjudas när alla arbetsordrar är slutförda");
            Invoice invoice = garage.createInvoiceForBooking(bookingId, null);
            TestRunner.assertNotNull(invoice, "Fakturan ska skapas när hela bokningen är klar");
            invoiceId = invoice.getId();
            TestRunner.assertEquals(4, invoice.getLines().size(),
                    "Fakturan ska ha alla fyra tjänsterna");
            System.out.println("    [FAKTURABEVIS] 3 av 3 klara: faktura " + invoiceId
                    + " med " + invoice.getLines().size() + " rader.");
        } finally {
            if (invoiceId > 0) {
                new InvoiceRepository().delete(invoiceId);
            }
            deleteQuietly(garage, orderIds[0], 0, first.getId());
            deleteQuietly(garage, orderIds[1], 0, second.getId());
            deleteQuietly(garage, orderIds[2], bookingId, third.getId());
        }
    }

    private boolean containsBooking(List<Booking> bookings, int bookingId) {
        for (Booking booking : bookings) {
            if (booking.getId() == bookingId) {
                return true;
            }
        }
        return false;
    }

    private Booking bookingWithServices(GarageSystem garage, int howMany, String text) throws SQLException {
        List<Vehicle> vehicles = garage.getVehicles();
        List<ServiceItem> services = garage.getServiceItems();

        Booking booking = new Booking(0, vehicles.get(0).getId(), LocalDate.now().plusDays(9), text);
        List<Integer> ids = new ArrayList<Integer>();
        for (int i = 0; i < howMany; i++) {
            ids.add(services.get(i).getId());
        }
        booking.setServiceItemIds(ids);
        new BookingRepository().save(booking);
        return booking;
    }

    private Mechanic temporaryMechanic(GarageSystem garage, String name) throws SQLException {
        return garage.createMechanic(name, "070-0000000", "Allmän service");
    }

    private int countLinks(int workOrderId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM work_order_service_items WHERE work_order_id = ?";
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, workOrderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        return 0;
    }

    private void deleteQuietly(GarageSystem garage, int orderId, int bookingId, int mechanicId) {
        try {
            if (orderId > 0) {
                new WorkOrderRepository().delete(orderId);
            }
            if (bookingId > 0) {
                new BookingRepository().delete(bookingId);
            }
            if (mechanicId > 0) {
                garage.deleteMechanic(mechanicId);
            }
        } catch (Exception e) {
            System.out.println("    (städningen efter provet gav: " + e.getMessage() + ")");
        }
    }
}
