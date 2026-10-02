package com.wac.autocore.test;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Payment;
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
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Revision av helheten i stallet for av enskilda funktioner: letar efter de feltyper
 * som den manuella genomgangen hittade (nio fel, PR #68) — och efter deras syskon.
 *
 * Fyra feltyper provas, var och en som en regel och inte som ett enskilt fall:
 *
 *   1. en rad som tappar sin förälder (bokning, tjänst, faktura, betalning)
 *   2. borttagningar som lämnar sadana rader efter sig, aven nar de anropas underifran
 *      i stallet for via gränssnittet
 *   3. en bokning som faktureras två ganger, eller tas bort med en faktura kvar
 *   4. pengar som tappar ören nar de summeras
 *
 * Proven mäter skillnaden fore och efter, inte ett absolut antal: databasen kan bara
 * redan trasiga rader fran utvecklingen, och då ska provet saga det i stallet for att
 * falla pa något det inte sjalv orsakat.
 *
 * testTheOrphanScanCatchesADeliberateOrphan bevisar att själva sökningen inte är tom:
 * den planterar en föräldralös rad och kräver att sökningen hittar den.
 */
public class DataIntegrityTest {

    /** {barn-tabell, kolumn, förälder-tabell, vad kopplingen betyder} */
    private static final String[][] LINKS = {
        {"vehicles", "customer_id", "customers", "fordon tillhör kund"},
        {"bookings", "vehicle_id", "vehicles", "bokning tillhör fordon"},
        {"bookings", "mechanic_id", "mechanics", "bokning tilldelad mekaniker"},
        {"booking_service_items", "booking_id", "bookings", "bokningens tjänsterad"},
        {"booking_service_items", "service_item_id", "service_items", "bokningens tjänst"},
        {"work_orders", "booking_id", "bookings", "arbetsorder fran bokning"},
        {"work_orders", "mechanic_id", "mechanics", "arbetsorderns mekaniker"},
        {"work_order_service_items", "work_order_id", "work_orders", "arbetsorderns tjänsterad"},
        {"work_order_service_items", "service_item_id", "service_items", "arbetsorderns tjänst"},
        {"invoices", "work_order_id", "work_orders", "faktura fran arbetsorder"},
        {"invoice_lines", "invoice_id", "invoices", "fakturarad"},
        {"payments", "invoice_id", "invoices", "betalning pa faktura"},
    };

    /** Hela kedjan: bokning med två tjänster, arbetsorder, faktura och betalning. */
    public void testAFullWorkflowLeavesNoOrphanRows() throws SQLException {
        GarageSystem garage = new GarageSystem();
        int orphansBefore = orphanCount();
        int mechanicId = 0;
        int bookingId = 0;
        int orderId = 0;
        int invoiceId = 0;
        int paymentId = 0;
        double servicePrice = 0;
        ServiceItem service = null;

        try {
            service = garage.getServiceItems().get(0);
            servicePrice = service.getPrice();
            Mechanic mechanic = garage.createMechanic("Revision-mekaniker", "070-0000020", "Allmän service");
            mechanicId = mechanic.getId();
            Booking booking = bookingWith(garage, new int[]{service.getId()}, "Revision: hel kedja");
            bookingId = booking.getId();

            WorkOrder order = garage.createWorkOrder(bookingId, mechanicId);
            TestRunner.assertNotNull(order, "Arbetsordern ska skapas ur bokningen");
            orderId = order.getId();
            garage.startWorkOrder(orderId);
            garage.markServicesAsCompleted(orderId, new int[]{service.getId()});
            garage.completeWorkOrder(orderId);

            Invoice invoice = garage.createInvoice(orderId, null);
            TestRunner.assertNotNull(invoice, "Fakturan ska skapas ur arbetsordern");
            invoiceId = invoice.getId();
            Payment payment = garage.processPayment(invoiceId, "KORT");
            TestRunner.assertNotNull(payment, "Betalningen ska registreras");
            paymentId = payment.getId();

            TestRunner.assertEquals(Integer.valueOf(orphansBefore), Integer.valueOf(orphanCount()),
                    "En hel kedja ska inte lämna en enda föräldralös rad efter sig");
        } finally {
            deletePayment(paymentId);
            if (invoiceId > 0) {
                new InvoiceRepository().delete(invoiceId);
            }
            if (orderId > 0) {
                new WorkOrderRepository().delete(orderId);
            }
            if (bookingId > 0) {
                new BookingRepository().delete(bookingId);
            }
            if (mechanicId > 0) {
                garage.deleteMechanic(mechanicId);
            }
            if (service != null && servicePrice > 0) {
                service.setPrice(servicePrice);
                garage.updateServiceItem(service);
            }
        }
    }

    /** Beviset for att sökningen inte är tom: en planterad rad maste hittas. */
    public void testTheOrphanScanCatchesADeliberateOrphan() throws SQLException {
        int orphansBefore = orphanCount();
        int plantedId = 0;

        try {
            plantedId = insertOrphanPayment();
            int orphansAfter = orphanCount();
            TestRunner.assertTrue(orphansAfter > orphansBefore,
                    "En betalning som pekar pa en faktura som inte finns ska räkna som föräldralös");
            TestRunner.assertTrue(orphanSummary().contains("payments.invoice_id"),
                    "Sokningen ska namnge kopplingen som är bruten, fick: " + orphanSummary());
        } finally {
            deletePayment(plantedId);
        }
        TestRunner.assertEquals(Integer.valueOf(orphansBefore), Integer.valueOf(orphanCount()),
                "Den planterade raden ska vara borta igen");
    }

    /**
     * Skydden i gränssnittet ska svara nej for en bokning med faktura, ett fordon med ett
     * fakturerat jobb och en tjänst som ligger i en bokning. Det har är de tre fallen ur
     * punkt 6, lasta som regressionsprov.
     */
    public void testTheGuardsRefuseWhatWouldOrphanRows() throws SQLException {
        GarageSystem garage = new GarageSystem();
        int mechanicId = 0;
        int bookingId = 0;
        int orderId = 0;
        int invoiceId = 0;
        ServiceItem service = garage.getServiceItems().get(0);
        Vehicle vehicle = garage.getVehicles().get(0);

        try {
            Mechanic mechanic = garage.createMechanic("Revision-skydd", "070-0000021", "Allmän service");
            mechanicId = mechanic.getId();
            Booking booking = bookingWith(garage, new int[]{service.getId()}, "Revision: skyddsregler");
            bookingId = booking.getId();
            WorkOrder order = garage.createWorkOrder(bookingId, mechanicId);
            orderId = order.getId();
            garage.startWorkOrder(orderId);
            garage.markServicesAsCompleted(orderId, new int[]{service.getId()});
            garage.completeWorkOrder(orderId);
            invoiceId = garage.createInvoice(orderId, null).getId();

            TestRunner.assertFalse(garage.canCancelOrDeleteBooking(bookingId),
                    "En bokning med en faktura ska inte få tas bort");
            TestRunner.assertFalse(garage.canDeleteVehicle(vehicle.getId()),
                    "Ett fordon vars jobb är fakturerat ska inte få tas bort");
            TestRunner.assertFalse(garage.canDeleteServiceItem(service.getId()),
                    "En tjänst som ligger i en bokning ska inte få tas bort");
        } finally {
            if (invoiceId > 0) {
                new InvoiceRepository().delete(invoiceId);
            }
            if (orderId > 0) {
                new WorkOrderRepository().delete(orderId);
            }
            if (bookingId > 0) {
                new BookingRepository().delete(bookingId);
            }
            if (mechanicId > 0) {
                garage.deleteMechanic(mechanicId);
            }
        }
    }

    /**
     * Skyddet maste halla aven nar tjänsten anropas direkt, utan gränssnittet emellan.
     * Gors det inte har, racker det med ett anrop fran en meny, ett skript eller ett
     * tangentkommando for att fakturan ska peka pa en bokning som inte finns.
     */
    public void testDeletingABookingWithAnInvoiceLeavesNoOrphans() throws SQLException {
        GarageSystem garage = new GarageSystem();
        int mechanicId = 0;
        int bookingId = 0;
        int orderId = 0;
        int invoiceId = 0;

        try {
            Mechanic mechanic = garage.createMechanic("Revision-borttagning", "070-0000022", "Allmän service");
            mechanicId = mechanic.getId();
            ServiceItem service = garage.getServiceItems().get(0);
            Booking booking = bookingWith(garage, new int[]{service.getId()}, "Revision: borttagning");
            bookingId = booking.getId();
            WorkOrder order = garage.createWorkOrder(bookingId, mechanicId);
            orderId = order.getId();
            garage.startWorkOrder(orderId);
            garage.markServicesAsCompleted(orderId, new int[]{service.getId()});
            garage.completeWorkOrder(orderId);
            invoiceId = garage.createInvoice(orderId, null).getId();

            int orphansBefore = orphanCount();
            boolean refused = false;
            try {
                garage.deleteBooking(bookingId);
            } catch (RuntimeException refusal) {
                // Ett nej är ett godkänt svar: det är just vad skyddet är till för.
                refused = true;
            }
            TestRunner.assertEquals(Integer.valueOf(orphansBefore), Integer.valueOf(orphanCount()),
                    "En bokning med faktura får inte kunna tas bort så att fakturan blir föräldralös: "
                            + orphanSummary());
            System.out.println(refused
                    ? "    [REVISION] Borttagningen nekades, som den ska — skyddet gäller även underifrån."
                    : "    [REVISION] Borttagningen gick igenom men lämnade inga föräldralösa rader.");
        } finally {
            if (invoiceId > 0) {
                new InvoiceRepository().delete(invoiceId);
            }
            if (orderId > 0) {
                new WorkOrderRepository().delete(orderId);
            }
            if (bookingId > 0) {
                new BookingRepository().delete(bookingId);
            }
            if (mechanicId > 0) {
                garage.deleteMechanic(mechanicId);
            }
        }
    }

    /** Beloppen ska behalla sina ören hela vagen: rad for rad, faktura och betalning. */
    public void testMoneyKeepsItsOreAllTheWay() throws SQLException {
        GarageSystem garage = new GarageSystem();
        int mechanicId = 0;
        int bookingId = 0;
        int orderId = 0;
        int invoiceId = 0;
        int firstServiceId = 0;
        int secondServiceId = 0;
        int paymentId = 0;

        try {
            ServiceItem first = garage.createServiceItem("Revision-ören A", "Prov med ören", 899.10, 30);
            firstServiceId = first.getId();
            ServiceItem second = garage.createServiceItem("Revision-ören B", "Prov med ören", 1495.45, 60);
            secondServiceId = second.getId();
            Mechanic mechanic = garage.createMechanic("Revision-pengar", "070-0000023", "Allmän service");
            mechanicId = mechanic.getId();
            Booking booking = bookingWith(garage, new int[]{firstServiceId, secondServiceId}, "Revision: ören");
            bookingId = booking.getId();

            WorkOrder order = garage.createWorkOrder(bookingId, mechanicId);
            orderId = order.getId();
            garage.startWorkOrder(orderId);
            garage.markServicesAsCompleted(orderId, new int[]{firstServiceId, secondServiceId});
            garage.completeWorkOrder(orderId);
            Invoice invoice = garage.createInvoice(orderId, null);
            invoiceId = invoice.getId();

            double lineSum = 0;
            for (int i = 0; i < invoice.getLines().size(); i++) {
                lineSum += invoice.getLines().get(i).getFinalPrice();
            }
            TestRunner.assertEquals(Double.valueOf(round(lineSum)), Double.valueOf(round(invoice.getAmount())),
                    "Fakturans belopp ska vara summan av raderna, med ören kvar");
            TestRunner.assertEquals(Double.valueOf(round(2394.55)), Double.valueOf(round(invoice.getAmount())),
                    "899,10 + 1495,45 ska bli 2394,55 — tappas ören blir det 2394,00");

            Payment payment = garage.processPayment(invoiceId, "KORT");
            paymentId = payment.getId();
            double paid = 0;
            List<Payment> payments = garage.getPayments();
            for (int i = 0; i < payments.size(); i++) {
                if (payments.get(i).getId() == paymentId) {
                    paid += payments.get(i).getAmount();
                }
            }
            TestRunner.assertEquals(Double.valueOf(round(invoice.getTotalAmount())), Double.valueOf(round(paid)),
                    "Betalningen ska tacka hela fakturans belopp, ören inraknade");
        } finally {
            deletePayment(paymentId);
            if (invoiceId > 0) {
                new InvoiceRepository().delete(invoiceId);
            }
            if (orderId > 0) {
                new WorkOrderRepository().delete(orderId);
            }
            if (bookingId > 0) {
                new BookingRepository().delete(bookingId);
            }
            if (mechanicId > 0) {
                garage.deleteMechanic(mechanicId);
            }
            if (firstServiceId > 0) {
                garage.deleteServiceItem(firstServiceId);
            }
            if (secondServiceId > 0) {
                garage.deleteServiceItem(secondServiceId);
            }
        }
    }

    /** Antal rader vars förälder inte finns. */
    private int orphanCount() throws SQLException {
        int total = 0;
        for (int i = 0; i < LINKS.length; i++) {
            total += countQuery("SELECT COUNT(*) FROM " + LINKS[i][0]
                    + " WHERE " + LINKS[i][1] + " IS NOT NULL AND " + LINKS[i][1]
                    + " NOT IN (SELECT id FROM " + LINKS[i][2] + ")");
        }
        return total;
    }

    /** Vilka kopplingar som är brutna, sa felet går att läsa utan att gissa. */
    private String orphanSummary() throws SQLException {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < LINKS.length; i++) {
            int rows = countQuery("SELECT COUNT(*) FROM " + LINKS[i][0]
                    + " WHERE " + LINKS[i][1] + " IS NOT NULL AND " + LINKS[i][1]
                    + " NOT IN (SELECT id FROM " + LINKS[i][2] + ")");
            if (rows > 0) {
                text.append(LINKS[i][0]).append(".").append(LINKS[i][1])
                        .append(" (").append(LINKS[i][3]).append("): ").append(rows).append(" rader. ");
            }
        }
        return text.length() == 0 ? "inga brutna kopplingar" : text.toString();
    }

    private int countQuery(String sql) throws SQLException {
        Connection connection = Db.getConnection();
        try {
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery(sql);
            return result.next() ? result.getInt(1) : 0;
        } finally {
            connection.close();
        }
    }

    private int insertOrphanPayment() throws SQLException {
        Connection connection = Db.getConnection();
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate("INSERT INTO payments (invoice_id, amount, payment_type, payment_date, successful) "
                    + "VALUES (999999999, 1.0, 'REVISION', '2026-01-01', 1)");
            return countQuery("SELECT MAX(id) FROM payments WHERE invoice_id = 999999999");
        } finally {
            connection.close();
        }
    }

    private void deletePayment(int id) throws SQLException {
        if (id <= 0) {
            return;
        }
        Connection connection = Db.getConnection();
        try {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM payments WHERE id = ?");
            statement.setInt(1, id);
            statement.executeUpdate();
        } finally {
            connection.close();
        }
    }

    private Booking bookingWith(GarageSystem garage, int[] serviceItemIds, String text) throws SQLException {
        List<Vehicle> vehicles = garage.getVehicles();
        Booking booking = new Booking(0, vehicles.get(0).getId(), LocalDate.now().plusDays(20), text);
        List<Integer> ids = new ArrayList<Integer>();
        for (int i = 0; i < serviceItemIds.length; i++) {
            ids.add(Integer.valueOf(serviceItemIds[i]));
        }
        booking.setServiceItemIds(ids);
        new BookingRepository().save(booking);
        return booking;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
