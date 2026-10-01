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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Bevisen för SCRUM-156 (C1) och SCRUM-158 (C3), körda mot en riktig databas.
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
