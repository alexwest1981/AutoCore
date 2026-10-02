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
import com.wac.autocore.repository.VehicleRepository;
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
            Payment payment = garage.processPayment(invoiceId, "CARD");
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

            Payment payment = garage.processPayment(invoiceId, "CARD");
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

    /**
     * Ett jobb ska bara kunna faktureras en gång. Gränssnittet hindrar att samma bokning
     * väljs om, men tjänsten själv frågade aldrig om arbetsordern redan hade en faktura.
     */
    public void testAnInvoiceIsCreatedOnlyOncePerWorkOrder() throws SQLException {
        GarageSystem garage = new GarageSystem();
        int mechanicId = 0;
        int bookingId = 0;
        int orderId = 0;
        int invoiceId = 0;

        try {
            Mechanic mechanic = garage.createMechanic("Revision-en-faktura", "070-0000024", "Allmän service");
            mechanicId = mechanic.getId();
            ServiceItem service = garage.getServiceItems().get(0);
            Booking booking = bookingWith(garage, new int[]{service.getId()}, "Revision: en faktura");
            bookingId = booking.getId();
            WorkOrder order = garage.createWorkOrder(bookingId, mechanicId);
            orderId = order.getId();
            garage.startWorkOrder(orderId);
            garage.markServicesAsCompleted(orderId, new int[]{service.getId()});
            garage.completeWorkOrder(orderId);
            invoiceId = garage.createInvoice(orderId, null).getId();

            Invoice again = garage.createInvoice(orderId, null);
            TestRunner.assertEquals(null, again,
                    "En arbetsorder som redan är fakturerad ska inte kunna faktureras igen");
            TestRunner.assertEquals(Integer.valueOf(1), Integer.valueOf(invoicesFor(orderId)),
                    "Arbetsordern ska ha exakt en faktura");
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
     * Ett arbete ska inte kunna markeras utfört innan det har startat. Annars fryses priset
     * på en order som står i CREATED, och fakturan kan byggas på arbete som aldrig påbörjades.
     */
    public void testServicesCannotBeMarkedPerformedBeforeTheWorkHasStarted() throws SQLException {
        GarageSystem garage = new GarageSystem();
        int mechanicId = 0;
        int bookingId = 0;
        int orderId = 0;

        try {
            Mechanic mechanic = garage.createMechanic("Revision-innan-start", "070-0000025", "Allmän service");
            mechanicId = mechanic.getId();
            ServiceItem service = garage.getServiceItems().get(0);
            Booking booking = bookingWith(garage, new int[]{service.getId()}, "Revision: innan start");
            bookingId = booking.getId();
            WorkOrder order = garage.createWorkOrder(bookingId, mechanicId);
            orderId = order.getId();

            boolean marked = garage.markServicesAsCompleted(orderId, new int[]{service.getId()});
            TestRunner.assertFalse(marked,
                    "En tjänst ska inte kunna markeras utförd på en arbetsorder som inte har startat");
            WorkOrder reloaded = new WorkOrderRepository().findById(orderId);
            TestRunner.assertEquals(Integer.valueOf(0), Integer.valueOf(reloaded.getCompletedServiceItems().size()),
                    "Ingen tjänst ska stå som utförd innan arbetet har startat");
        } finally {
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

    /** Statusen ska bara ga framat: en slutförd arbetsorder far inte startas igen. */
    public void testACompletedWorkOrderCannotBeStartedAgain() throws SQLException {
        GarageSystem garage = new GarageSystem();
        int mechanicId = 0;
        int bookingId = 0;
        int orderId = 0;

        try {
            Mechanic mechanic = garage.createMechanic("Revision-status", "070-0000026", "Allmän service");
            mechanicId = mechanic.getId();
            ServiceItem service = garage.getServiceItems().get(0);
            Booking booking = bookingWith(garage, new int[]{service.getId()}, "Revision: status framat");
            bookingId = booking.getId();
            WorkOrder order = garage.createWorkOrder(bookingId, mechanicId);
            orderId = order.getId();
            garage.startWorkOrder(orderId);
            garage.markServicesAsCompleted(orderId, new int[]{service.getId()});
            garage.completeWorkOrder(orderId);

            garage.startWorkOrder(orderId);
            WorkOrder reloaded = new WorkOrderRepository().findById(orderId);
            TestRunner.assertEquals("COMPLETED", reloaded.getStatus(),
                    "En slutförd arbetsorder ska inte kunna startas igen");
        } finally {
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

    /** Rabatten ska klämmas mot beloppet, så en faktura aldrig kan bli negativ. */
    public void testTheDiscountCannotExceedTheAmount() throws SQLException {
        GarageSystem garage = new GarageSystem();
        int mechanicId = 0;
        int bookingId = 0;
        int orderId = 0;
        int invoiceId = 0;
        int serviceId = 0;
        int paymentId = 0;

        try {
            ServiceItem cheap = garage.createServiceItem("Revision-billig", "Prov", 99.0, 10);
            serviceId = cheap.getId();
            Mechanic mechanic = garage.createMechanic("Revision-betalning", "070-0000027", "Allmän service");
            mechanicId = mechanic.getId();
            Booking booking = bookingWith(garage, new int[]{serviceId}, "Revision: dubbel betalning");
            bookingId = booking.getId();
            WorkOrder order = garage.createWorkOrder(bookingId, mechanicId);
            orderId = order.getId();
            garage.startWorkOrder(orderId);
            garage.markServicesAsCompleted(orderId, new int[]{serviceId});
            garage.completeWorkOrder(orderId);

            Invoice invoice = garage.createInvoice(orderId, "SERVICE200");
            invoiceId = invoice.getId();
            TestRunner.assertTrue(invoice.getTotalAmount() >= 0.0,
                    "En 200-kronorsrabatt på en 99-kronorsfaktura får inte ge ett negativt belopp, fick "
                            + invoice.getTotalAmount());
            TestRunner.assertEquals(Double.valueOf(round(99.0)), Double.valueOf(round(invoice.getAmount())),
                    "Fakturans belopp före rabatt ska vara tjänstens pris, 99 kr");
            TestRunner.assertEquals(Double.valueOf(round(99.0)), Double.valueOf(round(invoice.getDiscount())),
                    "Rabatten ska klämmas till beloppet, inte dra av mer än det finns");
            TestRunner.assertEquals(Double.valueOf(0.0), Double.valueOf(round(invoice.getTotalAmount())),
                    "Efter klämd rabatt ska summan bli noll, aldrig negativ");

            TestRunner.assertEquals(null, garage.processPayment(invoiceId, "KORT"),
                    "En faktura på noll kronor har inget att betala");
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
            if (serviceId > 0) {
                garage.deleteServiceItem(serviceId);
            }
        }
    }

    /** En betald faktura ska inte kunna betalas en gång till. */
    public void testAPaidInvoiceCannotBePaidTwice() throws SQLException {
        GarageSystem garage = new GarageSystem();
        int mechanicId = 0;
        int bookingId = 0;
        int orderId = 0;
        int invoiceId = 0;
        int paymentId = 0;

        try {
            Mechanic mechanic = garage.createMechanic("Revision-dubbelbetalning", "070-0000028", "Allmän service");
            mechanicId = mechanic.getId();
            ServiceItem service = garage.getServiceItems().get(0);
            Booking booking = bookingWith(garage, new int[]{service.getId()}, "Revision: dubbel betalning");
            bookingId = booking.getId();
            WorkOrder order = garage.createWorkOrder(bookingId, mechanicId);
            orderId = order.getId();
            garage.startWorkOrder(orderId);
            garage.markServicesAsCompleted(orderId, new int[]{service.getId()});
            garage.completeWorkOrder(orderId);
            Invoice invoice = garage.createInvoice(orderId, null);
            invoiceId = invoice.getId();

            Payment first = garage.processPayment(invoiceId, "CARD");
            TestRunner.assertNotNull(first, "Fakturan ska kunna betalas en gång");
            paymentId = first.getId();

            TestRunner.assertEquals(null, garage.processPayment(invoiceId, "BITCOIN"),
                    "En betaltyp systemet inte känner igen ska nekas");

            Payment second = garage.processPayment(invoiceId, "KORT");
            TestRunner.assertEquals(null, second, "En betald faktura ska inte kunna betalas igen");
            TestRunner.assertEquals(Integer.valueOf(1), Integer.valueOf(paymentsFor(invoiceId)),
                    "Fakturan ska ha exakt en betalning");
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
        }
    }

    /**
     * Registreringsnumret ska se likadant ut hur det än skrivs in: versaler, och ett mellanslag
     * mellan bokstäverna och siffrorna. "abc 123" och "ABC123" är samma plåt, inte två fordon.
     */
    public void testRegistrationNumbersAreStoredTheSameWayEveryTime() throws SQLException {
        GarageSystem garage = new GarageSystem();
        int customerId = garage.getCustomers().get(0).getId();
        int vehicleId = 0;

        try {
            Vehicle created = garage.createVehicle("rev 123", "Volvo", "Prov", 2020, customerId);
            TestRunner.assertNotNull(created, "Fordonet ska kunna skapas");
            vehicleId = created.getId();
            TestRunner.assertEquals("REV 123", created.getRegistrationNumber(),
                    "Numret ska sparas i versaler med mellanslag mellan bokstäverna och siffrorna");

            Vehicle reloaded = new VehicleRepository().findById(vehicleId);
            TestRunner.assertEquals("REV 123", reloaded.getRegistrationNumber(),
                    "Numret ska stå likadant när det läses tillbaka ur databasen");
        } finally {
            if (vehicleId > 0) {
                garage.deleteVehicle(vehicleId);
            }
        }
    }

    /**
     * Hela registret ska vara skrivet på samma sätt — även rader som sparades innan regeln fanns,
     * och även när de kommit in via seeddatan eller en äldre databasfil.
     */
    public void testEveryRegistrationNumberInTheDatabaseIsWrittenTheSameWay() throws SQLException {
        GarageSystem garage = new GarageSystem();
        List<Vehicle> vehicles = garage.getVehicles();
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle vehicle = vehicles.get(i);
            String stored = vehicle.getRegistrationNumber();
            if (stored == null) {
                continue;
            }
            TestRunner.assertEquals(Vehicle.normalizeRegistrationNumber(stored), stored,
                    "Fordon " + vehicle.getId() + " har numret \"" + stored + "\" i en annan skepnad än regeln");
        }
    }

    /** Ett registreringsnummer tillhör ett fordon, även när numret skrivs in med olika bokstäver. */
    public void testTheSameRegistrationNumberCannotBeSavedTwice() throws SQLException {
        GarageSystem garage = new GarageSystem();
        int customerId = garage.getCustomers().get(0).getId();
        int vehicleId = 0;

        try {
            Vehicle first = garage.createVehicle("XYZ 789", "Saab", "Prov", 2015, customerId);
            TestRunner.assertNotNull(first, "Det första fordonet ska kunna skapas");
            vehicleId = first.getId();

            Vehicle second = null;
            try {
                second = garage.createVehicle("xyz789", "Volvo", "Prov", 2016, customerId);
            } catch (RuntimeException refused) {
                // Ett nej är ett godkänt svar: det är just vad regeln är till för.
            }
            TestRunner.assertEquals(null, second,
                    "Samma registreringsnummer ska inte kunna läggas in två gånger");
            TestRunner.assertEquals(Integer.valueOf(1), Integer.valueOf(countQuery(
                    "SELECT COUNT(*) FROM vehicles WHERE registration_number = 'XYZ 789'")),
                    "Numret ska bara finnas på ett fordon");
        } finally {
            if (vehicleId > 0) {
                garage.deleteVehicle(vehicleId);
            }
        }
    }

    /** Antal betalningar på en faktura. */
    private int paymentsFor(int invoiceId) throws SQLException {
        return countQuery("SELECT COUNT(*) FROM payments WHERE invoice_id = " + invoiceId);
    }

    /** Antal fakturor som pekar på en arbetsorder. */
    private int invoicesFor(int workOrderId) throws SQLException {
        return countQuery("SELECT COUNT(*) FROM invoices WHERE work_order_id = " + workOrderId);
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

    /**
     * Planterar en betalning som pekar på en faktura som inte finns.
     *
     * Läser tillbaka id:t på samma anslutning med last_insert_rowid(). Att öppna en andra
     * anslutning här — vilket provet gjorde först — låste SQLite och hela sviten stannade.
     */
    private int insertOrphanPayment() throws SQLException {
        Connection connection = Db.getConnection();
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate("INSERT INTO payments (invoice_id, amount, payment_type, payment_date, successful) "
                    + "VALUES (999999999, 1.0, 'REVISION', '2026-01-01', 1)");
            ResultSet keys = statement.executeQuery("SELECT last_insert_rowid()");
            return keys.next() ? keys.getInt(1) : 0;
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
