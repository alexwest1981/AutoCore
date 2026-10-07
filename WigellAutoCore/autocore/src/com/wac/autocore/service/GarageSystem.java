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
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.CustomerRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Ingången som gränssnittet och textversionen anropar. Kopplar vidare till tjänsterna. */
public class GarageSystem {

    private final com.wac.autocore.ui.ConsolePrinter printer = new com.wac.autocore.ui.ConsolePrinter();
    private final CustomerService customerService = new CustomerService();
    private final VehicleService vehicleService = new VehicleService(customerService);
    private final WorkOrderService workOrderService = new WorkOrderService();
    private final BookingService bookingService = new BookingService(workOrderService);
    private final BillingService billingService = new BillingService();
    private final PaymentService paymentService = new PaymentService();
    private final ServicePackageService servicePackageService = new ServicePackageService();
    private final MechanicRules mechanicRules = new MechanicRules();
    private final RemovalRules removalRules = new RemovalRules(
            workOrderService, bookingService, billingService, vehicleService);
    private final ServiceItemRepository serviceItemRepository = new ServiceItemRepository();
    private final MechanicRepository mechanicRepository = new MechanicRepository();
    private final CustomerRepository customerRepository = new CustomerRepository();
    private final VehicleRepository vehicleRepository = new VehicleRepository();
    private final BookingRepository bookingRepository = new BookingRepository();

    public GarageSystem() {
        Db.ensureReady();
    }

    // --- läsa ---

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
        return mechanicRules.getAll();
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

    public List<Mechanic> getQualifiedMechanics(ServiceItem service) {
        return mechanicRules.qualifiedFor(service);
    }

    public List<Mechanic> getQualifiedMechanics(Collection<ServiceItem> services) {
        return mechanicRules.qualifiedFor(services);
    }

/** Mekanikerna som behövs för att bemanna tjänsterna. */
    public List<Mechanic> getRequiredMechanics(Collection<ServiceItem> services) {
        return mechanicRules.requiredFor(services);
    }

/** Jämför tjänstens krav med mekanikerns specialisering. Utan krav får alla utföra den. */
    public boolean isMechanicQualified(Mechanic mechanic, ServiceItem service) {
        return mechanicRules.isMechanicQualified(mechanic, service);
    }

    /** Hur länge bilen håller verkstaden: den mest belastade mekanikern bestämmer sluttiden. */
    public int busyMinutes(List<ServiceItem> services, List<Mechanic> mechanics) {
        return mechanicRules.busyMinutes(services, mechanics);
    }

    /** Bokningar med utfört arbete kvar att fakturera. */
    public java.util.List<Booking> getInvoiceableBookings() {
        return billingService.getInvoiceableBookings();
    }

    /** Sant om det finns bokningar med arbete som inte är slutfört än. */
    public boolean hasBookingWithUnfinishedWork() {
        return billingService.hasBookingWithUnfinishedWork();
    }

    // --- textversionen ---

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

    // --- skapa ---

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

/** Skapar en arbetsorder för ett urval av tjänsterna, så en bokning kan delas. */
    public WorkOrder createWorkOrder(int bookingId, int mechanicId, java.util.List<Integer> serviceItemIds) {
        return workOrderService.createWorkOrder(bookingId, mechanicId, serviceItemIds);
    }

    /** Skapar en arbetsorder av en viss typ: Standard, Reklamation eller Intern. */
    public WorkOrder createWorkOrder(int bookingId, int mechanicId, java.util.List<Integer> serviceItemIds,
                                     String type) {
        return workOrderService.createWorkOrder(bookingId, mechanicId, serviceItemIds, type);
    }

    /** Skapar en reklamation på en tidigare utförd arbetsorder. */
    public WorkOrder createReclamation(int originalWorkOrderId, String description) {
        return workOrderService.createReclamation(originalWorkOrderId, description);
    }

    // Skapar ett utkast, alltså en arbetsorder utan bokning och utan tjänster.
    public WorkOrder createDraft(int vehicleId, String description) {
        return workOrderService.createDraft(vehicleId, description);
    }

    /** Drop-in utan bokad tid: bokningsraden skapas först, i samma svep, så allt som slår upp
     *  kund och fordon via bokningen fortsätter fungera. Mekanikerna kommer från formuläret,
     *  som fyller på dem ur tjänsternas krav. */
    public Booking createDropInBooking(int vehicleId, java.util.List<ServiceItem> services,
                                       java.util.List<Mechanic> mechanics) {
        return bookingService.createDropInBooking(vehicleId, services, mechanics);
    }

    /** Tiden en drop-in skulle få just nu, eller null när teamets timmar är slut i dag. */
    public LocalTime dropInStartTime(java.util.List<ServiceItem> services,
                                     java.util.List<Mechanic> mechanics) {
        return bookingService.dropInStartTime(services, mechanics);
    }

    public Invoice createInvoice(int workOrderId, String discountCode) {
        return billingService.createInvoice(workOrderId, discountCode);
    }

/** Fakturerar en arbetsorder, med en ny kostnad som egen rad. */
    public Invoice createInvoice(int workOrderId, String discountCode, String extraName, double extraAmount) {
        return billingService.createInvoice(workOrderId, discountCode, extraName, extraAmount);
    }

/** Fakturerar bokningen. Själva arbetet ligger i {@link BillingService}. */
    public Invoice createInvoiceForBooking(int bookingId, String discountCode) {
        return billingService.createInvoiceForBooking(bookingId, discountCode);
    }

/** Samma faktura, men med plats för en ny kostnad som reklamationen för med sig. */
    public Invoice createInvoiceForBooking(int bookingId, String discountCode, String extraName, double extraAmount) {
        return billingService.createInvoiceForBooking(bookingId, discountCode, extraName, extraAmount);
    }

    public Mechanic createMechanic(String name, String phone, String specialization) throws SQLException {
        refuseUnlessStorableMechanic(name, phone);
        Mechanic mechanic = new Mechanic(0, name, phone, specialization);
        mechanicRepository.save(mechanic);
        return mechanic;
    }

    public ServiceItem createServiceItem(String name, String description, double price, int estimatedMinutes) throws SQLException {
        return createServiceItem(name, description, price, estimatedMinutes, "");
    }

/** Samma som ovan, men med kravet på specialisering. */
    public ServiceItem createServiceItem(String name, String description, double price, int estimatedMinutes,
                                         String specialization) throws SQLException {
        ServiceItem item = new ServiceItem(0, name, description, price, estimatedMinutes, specialization);
        serviceItemRepository.save(item);
        return item;
    }

    // --- arbetsordern och betalningen ---

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

    public boolean cancelWorkOrder(int workOrderId) {
        return workOrderService.cancelWorkOrder(workOrderId);
    }

    public boolean confirmWorkOrder(int workOrderId) {
        return workOrderService.confirmWorkOrder(workOrderId);
    }

    public Payment processPayment(int invoiceId, String paymentType) {
        return paymentService.processPayment(invoiceId, paymentType);
    }

    // --- uppdatera ---

    public void updateCustomer(Customer customer) throws SQLException {
        customerService.updateCustomer(customer);
    }

    public void updateVehicle(Vehicle vehicle) throws SQLException {
        vehicleRepository.save(vehicle);
    }

    public void updateMechanic(Mechanic mechanic) throws SQLException {
        if (mechanic == null) {
            return;
        }
        refuseUnlessStorableMechanic(mechanic.getName(), mechanic.getPhone());
        mechanicRepository.save(mechanic);
    }

    public void updateBooking(Booking booking) throws SQLException {
        bookingRepository.save(booking);
        MechanicSchedule.getInstance().syncFromDatabase();
    }

    public void updateServiceItem(ServiceItem item) throws SQLException {
        serviceItemRepository.save(item);
    }

    // --- ta bort ---

    public boolean canDeleteMechanic(int mechanicId) {
        return removalRules.canDeleteMechanic(mechanicId);
    }

    public void deleteMechanic(int mechanicId) throws SQLException {
        refuseUnless(canDeleteMechanic(mechanicId),
                "Mekanikern kan inte tas bort: den används av en bokning eller ett pågående arbete.");
        mechanicRepository.delete(mechanicId);
        MechanicSchedule.getInstance().removeSlotsForMechanic(mechanicId);
    }

    public boolean canDeleteCustomer(int customerId) {
        return removalRules.canDeleteCustomer(customerId);
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

    public boolean canDeleteVehicle(int vehicleId) {
        return removalRules.canDeleteVehicle(vehicleId);
    }

    public void deleteVehicle(int vehicleId) throws SQLException {
        refuseUnless(canDeleteVehicle(vehicleId),
                "Fordonet kan inte tas bort: det har en bokning eller ett fakturerat jobb.");
        vehicleRepository.delete(vehicleId);
    }

    public boolean canCancelOrDeleteBooking(int bookingId) {
        return removalRules.canCancelOrDeleteBooking(bookingId);
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

/** Nekar borttagningen med en förklaring. Reglerna finns i {@link RemovalRules}. */
    private void refuseUnless(boolean allowed, String reason) {
        if (!allowed) {
            throw new IllegalStateException(reason);
        }
    }

    public void deleteBooking(int bookingId) throws SQLException {
        refuseUnless(canCancelOrDeleteBooking(bookingId),
                "Bokningen kan inte tas bort: den har en arbetsorder med faktura.");
        bookingRepository.delete(bookingId);
        MechanicSchedule.getInstance().cancelSlotForBooking(bookingId);
        MechanicSchedule.getInstance().syncFromDatabase();
    }

    public boolean canDeleteServiceItem(int serviceItemId) {
        return removalRules.canDeleteServiceItem(serviceItemId);
    }

    public void deleteServiceItem(int serviceItemId) throws SQLException {
        refuseUnless(canDeleteServiceItem(serviceItemId),
                "Tjänsten kan inte tas bort: den ligger i en bokning eller en arbetsorder.");
        serviceItemRepository.delete(serviceItemId);
    }

    /** Samma regel som för kunden, i den väg alla skrivare av en mekanikerrad går genom. */
    private static void refuseUnlessStorableMechanic(String name, String phone) {
        String problem = Mechanic.validationProblem(name, phone);
        if (problem != null) {
            throw new IllegalArgumentException("Mechanic data rejected: " + problem);
        }
    }
    // --- Servicepaket ---

    public List<ServicePackage> getServicePackages() {
        return servicePackageService.getAll();
    }

    public ServicePackage findServicePackage(int id) {
        return servicePackageService.findById(id);
    }

    public ServicePackage createServicePackage(String name, String description, List<ServiceItem> serviceItems) {
        return servicePackageService.createPackage(name, description, serviceItems);
    }

    public void updateServicePackage(ServicePackage servicePackage) throws SQLException {
        servicePackageService.updatePackage(servicePackage);
    }

    public void deleteServicePackage(int id) throws SQLException {
        servicePackageService.deletePackage(id);
    }
}