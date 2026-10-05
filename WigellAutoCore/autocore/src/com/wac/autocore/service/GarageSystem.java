package com.wac.autocore.service;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.CustomerRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.wac.autocore.seed.SeedText;

public class GarageSystem {

    public GarageSystem() {
        Db.ensureReady();
    }

    public List<Customer> getCustomers() {
        return customerService.getAll();
    }

    public List<Vehicle> getVehicles() {
        return vehicleService.getAll();
    }

    public List<Booking> getBookings() {
        return bookingService.getAll();
    }

    public List<ServiceItem> getServiceItems() {
        try {
            return serviceItemRepository.findAll();
        } catch (SQLException e) {
            System.out.println("Could not read service items: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Mechanic> getMechanics() {
        try {
            return mechanicRepository.findAll();
        } catch (SQLException e) {
            System.out.println("Could not read mechanics: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Mechanic> getQualifiedMechanics(ServiceItem service) {
        if (service == null) {
            return getMechanics();
        }
        return getQualifiedMechanics(Collections.singletonList(service));
    }

    public List<Mechanic> getQualifiedMechanics(Collection<ServiceItem> services) {
        List<Mechanic> all = getMechanics();
        if (services == null || services.isEmpty()) {
            return all;
        }
        List<Mechanic> qualified = new ArrayList<Mechanic>();
        for (Mechanic m : all) {
            boolean allQualified = true;
            for (ServiceItem s : services) {
                if (!isMechanicQualified(m, s)) {
                    allQualified = false;
                    break;
                }
            }
            if (allQualified) {
                qualified.add(m);
            }
        }
        // Fallback: om ingen specifik specialist finns för de valda tjänsterna
        if (qualified.isEmpty() && services.size() == 1) {
            for (Mechanic m : all) {
                String resolvedSpec = SeedText.resolve(m.getSpecialization());
                String s = resolvedSpec != null ? resolvedSpec.toLowerCase() : "";
                if (s.contains("general") || s.contains("allmän")) {
                    qualified.add(m);
                }
            }
        }
        // Sortera så att den bäst lämpade mekanikern visas först
        qualified.sort(new Comparator<Mechanic>() {
            @Override
            public int compare(Mechanic m1, Mechanic m2) {
                String spec1 = SeedText.resolve(m1.getSpecialization()).toLowerCase();
                String spec2 = SeedText.resolve(m2.getSpecialization()).toLowerCase();
                boolean g1 = spec1.contains("general") || spec1.contains("allmän");
                boolean g2 = spec2.contains("general") || spec2.contains("allmän");
                // Om enbart allmänna tjänster valts, sätt allmänmekaniker först
                boolean onlyGeneral = services.stream().allMatch(s -> {
                    String n = SeedText.resolve(s.getName()).toLowerCase();
                    return n.contains("oil") || n.contains("olja") || n.contains("annual") || n.contains("årlig");
                });
                if (onlyGeneral) {
                    if (g1 && !g2) return -1;
                    if (!g1 && g2) return 1;
                }
                return m1.getName().compareToIgnoreCase(m2.getName());
            }
        });
        return qualified;
    }

    /**
     * Returnerar listan av mekaniker som behövs för att bemanna samtliga valda tjänster.
     * T.ex. för Bromsar + Diagnostik returneras [Sara Nilsson, Mikael Berg] ("Vi bokar in: Sara Nilsson, Mikael Berg").
     */
    public List<Mechanic> getRequiredMechanics(Collection<ServiceItem> services) {
        List<Mechanic> result = new ArrayList<Mechanic>();
        if (services == null || services.isEmpty()) {
            return result;
        }
        List<Mechanic> all = getMechanics();
        for (ServiceItem s : services) {
            Mechanic best = null;
            // 1. Kolla om någon redan i teamet kan utföra tjänsten
            for (Mechanic m : result) {
                if (isMechanicQualified(m, s)) {
                    best = m;
                    break;
                }
            }
            // 2. Annars hitta bäst lämpad mekaniker bland samtliga mekaniker
            if (best == null) {
                for (Mechanic m : all) {
                    if (isMechanicQualified(m, s)) {
                        best = m;
                        break;
                    }
                }
            }
            // 3. Fallback: en allmänmekaniker kan ta tjänsten
            if (best == null) {
                for (Mechanic m : all) {
                    String spec = SeedText.resolve(m.getSpecialization());
                    String specStr = spec != null ? spec.toLowerCase() : "";
                    if (specStr.contains("general") || specStr.contains("allmän")) {
                        best = m;
                        break;
                    }
                }
            }
            // Hittas ingen behörig mekaniker lämnas tjänsten utanför teamet. Att fylla på med en
            // mekaniker som saknar behörigheten ger ett valt fält som kontrollen sedan underkänner.
            if (best != null && !result.contains(best)) {
                result.add(best);
            }
        }
        return result;
    }

    /**
     * Om mekanikern får utföra tjänsten. Jämförelsen sker på nycklar: tjänsten säger vilken
     * specialisering den kräver, och mekanikern bär sin egen. En tjänst utan krav kan utföras av alla.
     */
    public boolean isMechanicQualified(Mechanic mechanic, ServiceItem service) {
        if (service == null) {
            return true;
        }
        if (mechanic == null) {
            return false;
        }
        if (service.requiresAnyMechanic()) {
            return true;
        }
        String needed = service.getSpecialization().trim();
        String has = mechanic.getSpecialization();
        return has != null && needed.equals(has.trim());
    }

    public List<WorkOrder> getWorkOrders() {
        return workOrderService.getAll();
    }

    public List<Invoice> getInvoices() {
        return billingService.getAll();
    }

    public List<Payment> getPayments() {
        return paymentService.getAll();
    }

    private final com.wac.autocore.ui.ConsolePrinter printer = new com.wac.autocore.ui.ConsolePrinter();
    private final CustomerService customerService = new CustomerService();
    private final VehicleService vehicleService = new VehicleService(customerService);
    private final WorkOrderService workOrderService = new WorkOrderService();
    private final BookingService bookingService = new BookingService(workOrderService);
    private final BillingService billingService = new BillingService();
    private final PaymentService paymentService = new PaymentService();
    private final ServiceItemRepository serviceItemRepository = new ServiceItemRepository();
    private final MechanicRepository mechanicRepository = new MechanicRepository();
    private final CustomerRepository customerRepository = new CustomerRepository();
    private final VehicleRepository vehicleRepository = new VehicleRepository();
    private final BookingRepository bookingRepository = new BookingRepository();

    public void showCustomers() {
        printer.printCustomers(customerService.getAll());
    }

    public void showVehicles() {
        printer.printVehicles(vehicleService.getAll());
    }

    public void showBookings() {
        printer.printBookings(bookingService.getAll());
    }

    public void showServiceItems() {
        printer.printServiceItems(getServiceItems());
    }

    public void showMechanics() {
        printer.printMechanics(getMechanics());
    }

    public void showWorkOrders() {
        printer.printWorkOrders(workOrderService.getAll());
    }

    public void showInvoices() {
        printer.printInvoices(billingService.getAll());
    }

    public void showPayments() {
        printer.printPayments(paymentService.getAll());
    }

    public Customer createCustomer(String name, String phone, String email) {
        return customerService.createCustomer(name, phone, email);
    }

    public Vehicle createVehicle(String registrationNumber,
                                 String brand,
                                 String model,
                                 int year,
                                 int customerId) {
        return vehicleService.createVehicle(registrationNumber, brand, model, year, customerId);
    }

    public Booking createBooking(int vehicleId,
                                 LocalDate date,
                                 String description) {
        return bookingService.createBooking(vehicleId, date, description);
    }

    public Booking createBooking(int vehicleId,
                                 LocalDate date,
                                 String description, LocalTime startTime, int mechanicId, int serviceItemId) throws SQLException {
        return bookingService.createBooking(vehicleId, date, description,
                startTime, mechanicId, serviceItemId);
    }

    public WorkOrder createWorkOrder(int bookingId, int mechanicId) {
        return workOrderService.createWorkOrder(bookingId, mechanicId);
    }

    /**
     * Skapar en arbetsorder för ett urval av bokningens tjänster, så en bokning med flera tjänster
     * kan delas på flera mekaniker (en arbetsorder per mekaniker).
     */
    public WorkOrder createWorkOrder(int bookingId, int mechanicId, java.util.List<Integer> serviceItemIds) {
        return workOrderService.createWorkOrder(bookingId, mechanicId, serviceItemIds);
    }

    public int getEstimatedDuration(int... serviceItemsIds) {
        return workOrderService.getTotalEstimatedMinutes(serviceItemsIds);
    }

    public void startWorkOrder(int workOrderId) {
        workOrderService.startWorkOrder(workOrderId);
    }

    /** Markerar tjänster som utförda och sparar deras aktuella priser. */
    public boolean markServicesAsCompleted(int workOrderId, int[] serviceItemIds) {
        return workOrderService.markServicesAsCompleted(workOrderId, serviceItemIds);
    }

    public void completeWorkOrder(int workOrderId) {
        workOrderService.completeWorkOrder(workOrderId);
    }

    public Invoice createInvoice(int workOrderId, String discountCode) {
        return billingService.createInvoice(workOrderId, discountCode);
    }

    /**
     * Fakturerar allt utfört arbete på en bokning i en faktura — även när bokningen delats på flera
     * arbetsordrar (en per mekaniker).
     */
    public Invoice createInvoiceForBooking(int bookingId, String discountCode) {
        return billingService.createInvoiceForBooking(bookingId, discountCode);
    }

    /** Bokningar med utfört arbete kvar att fakturera. */
    public java.util.List<Booking> getInvoiceableBookings() {
        return billingService.getInvoiceableBookings();
    }

    /** Sant om det finns bokningar med arbete som inte är slutfört än. */
    public boolean hasBookingWithUnfinishedWork() {
        return billingService.hasBookingWithUnfinishedWork();
    }

    public Payment processPayment(int invoiceId, String paymentType) {
        return paymentService.processPayment(invoiceId, paymentType);
    }

    public void updateMechanic(Mechanic mechanic) throws SQLException {
        if (mechanic == null) {
            return;
        }
        refuseUnlessStorableMechanic(mechanic.getName(), mechanic.getPhone());
        mechanicRepository.save(mechanic);
    }

    /** Samma regel som för kunden, i den väg alla skrivare av en mekanikerrad går genom. */
    private static void refuseUnlessStorableMechanic(String name, String phone) {
        String problem = Mechanic.validationProblem(name, phone);
        if (problem != null) {
            throw new IllegalArgumentException("Mechanic data rejected: " + problem);
        }
    }

    public boolean canDeleteMechanic(int mechanicId) {
        List<WorkOrder> orders = getWorkOrders();
        for (WorkOrder wo : orders) {
            if (wo.getMechanicId() == mechanicId && !"COMPLETED".equalsIgnoreCase(wo.getStatus())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Nekar en borttagning som skulle lämna en rad utan förälder. Skydden fanns tidigare
     * bara i gränssnittet, så ett anrop underifrån — en meny, ett skript, ett tangentkommando —
     * kunde ta bort en bokning med faktura kvar och lämna fakturan pekande i tomma luften.
     */
    private void refuseUnless(boolean allowed, String reason) {
        if (!allowed) {
            throw new IllegalStateException(reason);
        }
    }

    public void deleteMechanic(int mechanicId) throws SQLException {
        refuseUnless(canDeleteMechanic(mechanicId),
                "Mekanikern kan inte tas bort: den används av en bokning eller ett pågående arbete.");
        mechanicRepository.delete(mechanicId);
        MechanicSchedule.getInstance().removeSlotsForMechanic(mechanicId);
    }

    public Mechanic createMechanic(String name, String phone, String specialization) throws SQLException {
        refuseUnlessStorableMechanic(name, phone);
        Mechanic mechanic = new Mechanic(0, name, phone, specialization);
        mechanicRepository.save(mechanic);
        return mechanic;
    }

    public void updateCustomer(Customer customer) throws SQLException {
        customerService.updateCustomer(customer);
    }

    public boolean canDeleteCustomer(int customerId) {
        for (Vehicle v : getVehicles()) {
            if (v.getCustomerId() == customerId) {
                if (!canDeleteVehicle(v.getId())) {
                    return false;
                }
            }
        }
        return true;
    }

    public void deleteCustomer(int customerId) throws SQLException {
        refuseUnless(canDeleteCustomer(customerId),
                "Kunden kan inte tas bort: ett av kundens fordon har ett fakturerat jobb.");
        for (Vehicle v : getVehicles()) {
            if (v.getCustomerId() == customerId) {
                vehicleRepository.delete(v.getId());
            }
        }
        customerRepository.delete(customerId);
    }

    public void updateVehicle(Vehicle vehicle) throws SQLException {
        vehicleRepository.save(vehicle);
    }

    public boolean canDeleteVehicle(int vehicleId) {
        for (Booking b : getBookings()) {
            if (b.getVehicleId() == vehicleId && !"CANCELLED".equalsIgnoreCase(b.getStatus())) {
                return false;
            }
        }
        for (WorkOrder wo : getWorkOrders()) {
            if (!"COMPLETED".equalsIgnoreCase(wo.getStatus())) {
                for (Booking b : getBookings()) {
                    if (b.getId() == wo.getBookingId() && b.getVehicleId() == vehicleId) {
                        return false;
                    }
                }
            }
        }
        // Ett fordon vars jobb har fakturerats får inte tas bort. Fakturan pekar på arbetet,
        // arbetet på bokningen och bokningen på fordonet. Kunden nekas via sina fordon.
        for (Booking b : getBookings()) {
            if (b.getVehicleId() == vehicleId) {
                for (WorkOrder wo : getWorkOrders()) {
                    if (wo.getBookingId() == b.getId() && hasInvoiceForWorkOrder(wo.getId())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void deleteVehicle(int vehicleId) throws SQLException {
        refuseUnless(canDeleteVehicle(vehicleId),
                "Fordonet kan inte tas bort: det har en bokning eller ett fakturerat jobb.");
        vehicleRepository.delete(vehicleId);
    }

    public void updateBooking(Booking booking) throws SQLException {
        bookingRepository.save(booking);
        MechanicSchedule.getInstance().syncFromDatabase();
    }

    /** Sant om arbetsordern har en faktura. Fakturan pekar på arbetet, arbetet på bokningen. */
    private boolean hasInvoiceForWorkOrder(int workOrderId) {
        for (Invoice invoice : getInvoices()) {
            if (invoice.getWorkOrderId() == workOrderId) {
                return true;
            }
        }
        return false;
    }

    public boolean canCancelOrDeleteBooking(int bookingId) {
        for (WorkOrder wo : getWorkOrders()) {
            if (wo.getBookingId() == bookingId) {
                if (!"COMPLETED".equalsIgnoreCase(wo.getStatus())) {
                    return false;
                }
                // En slutförd order får inte lämna en faktura som pekar på en bokning
                // som inte finns, alltså nekas borttagningen så länge fakturan finns.
                if (hasInvoiceForWorkOrder(wo.getId())) {
                    return false;
                }
            }
        }
        return true;
    }

    public void cancelBooking(int bookingId) throws SQLException {
        Booking b = bookingService.findById(bookingId);
        if (b != null) {
            b.setStatus("CANCELLED");
            bookingRepository.save(b);
            MechanicSchedule.getInstance().cancelSlotForBooking(bookingId);
            MechanicSchedule.getInstance().syncFromDatabase();
        }
    }

    public void deleteBooking(int bookingId) throws SQLException {
        refuseUnless(canCancelOrDeleteBooking(bookingId),
                "Bokningen kan inte tas bort: den har en arbetsorder med faktura.");
        bookingRepository.delete(bookingId);
        MechanicSchedule.getInstance().cancelSlotForBooking(bookingId);
        MechanicSchedule.getInstance().syncFromDatabase();
    }

    public ServiceItem createServiceItem(String name, String description, double price, int estimatedMinutes) throws SQLException {
        return createServiceItem(name, description, price, estimatedMinutes, "");
    }

    /** Samma som ovan, men med kravet på specialisering: en nyckel, eller tomt för vilken mekaniker som helst. */
    public ServiceItem createServiceItem(String name, String description, double price, int estimatedMinutes,
                                         String specialization) throws SQLException {
        ServiceItem item = new ServiceItem(0, name, description, price, estimatedMinutes, specialization);
        serviceItemRepository.save(item);
        return item;
    }

    public void updateServiceItem(ServiceItem item) throws SQLException {
        serviceItemRepository.save(item);
    }

    public boolean canDeleteServiceItem(int serviceItemId) {
        for (WorkOrder wo : getWorkOrders()) {
            if (!"COMPLETED".equalsIgnoreCase(wo.getStatus()) && wo.getServiceItemIds() != null) {
                for (int id : wo.getServiceItemIds()) {
                    if (id == serviceItemId) {
                        return false;
                    }
                }
            }
        }
        // Tjänsten får inte heller tas bort så länge den ligger i en bokning, oavsett
        // arbetsorderns status. Annars pekar bokningsraden på en tjänst som inte finns.
        for (Booking b : getBookings()) {
            if (b.getServiceItemIds() != null) {
                for (int id : b.getServiceItemIds()) {
                    if (id == serviceItemId) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void deleteServiceItem(int serviceItemId) throws SQLException {
        refuseUnless(canDeleteServiceItem(serviceItemId),
                "Tjänsten kan inte tas bort: den ligger i en bokning eller en arbetsorder.");
        serviceItemRepository.delete(serviceItemId);
    }
}
