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

/** The entry point the interface and the text version call. Passes on to the services. */
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

    /** The mechanics needed to staff the services. */
    public List<Mechanic> getRequiredMechanics(Collection<ServiceItem> services) {
        return mechanicRules.requiredFor(services);
    }

    /** Compares the service's requirement with the mechanic's specialization. With no requirement anyone may do it. */
    public boolean isMechanicQualified(Mechanic mechanic, ServiceItem service) {
        return mechanicRules.isMechanicQualified(mechanic, service);
    }

    /** How long the car holds the shop: the busiest mechanic decides the end time. */
    public int busyMinutes(List<ServiceItem> services, List<Mechanic> mechanics) {
        return mechanicRules.busyMinutes(services, mechanics);
    }

    /** Bookings with finished work still to invoice. */
    public java.util.List<Booking> getInvoiceableBookings() {
        return billingService.getInvoiceableBookings();
    }

    /** True if there are bookings with work that is not finished yet. */
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

    /** Creates a work order for a selection of the services, so a booking can be split. */
    public WorkOrder createWorkOrder(int bookingId, int mechanicId, java.util.List<Integer> serviceItemIds) {
        return workOrderService.createWorkOrder(bookingId, mechanicId, serviceItemIds);
    }

    /** Creates a work order of a given type: Standard, Reclamation or Internal. */
    public WorkOrder createWorkOrder(int bookingId, int mechanicId, java.util.List<Integer> serviceItemIds,
                                     String type) {
        return workOrderService.createWorkOrder(bookingId, mechanicId, serviceItemIds, type);
    }

    /** Creates a reclamation on a previously performed work order. */
    public WorkOrder createReclamation(int originalWorkOrderId, String description) {
        return workOrderService.createReclamation(originalWorkOrderId, description);
    }

    // Creates a draft, that is a work order with no booking and no services.
    public WorkOrder createDraft(int vehicleId, String description) {
        return workOrderService.createDraft(vehicleId, description);
    }

    public WorkOrder updateDraft(int workOrderId, int vehicleId, String description, int mechanicId,
            List<Integer> serviceItemIds, String plannedDate, String customerInstructions, String otherComments) {
        return workOrderService.updateDraft(workOrderId, vehicleId, description, mechanicId, serviceItemIds,
                plannedDate, customerInstructions, otherComments);
    }

    /** Drop-in with no booked time: the booking row is created first, in the same sweep, so that
     *  everything that looks up customer and vehicle through the booking keeps working. The
     *  mechanics come from the form, which fills them in from the services' requirements. */
    public Booking createDropInBooking(int vehicleId, java.util.List<ServiceItem> services,
                                       java.util.List<Mechanic> mechanics) {
        return bookingService.createDropInBooking(vehicleId, services, mechanics);
    }

    /** The time a drop-in would get right now, or null when the team's hours are out for the day. */
    public LocalTime dropInStartTime(java.util.List<ServiceItem> services,
                                     java.util.List<Mechanic> mechanics) {
        return bookingService.dropInStartTime(services, mechanics);
    }

    public Invoice createInvoice(int workOrderId, String discountCode) {
        return billingService.createInvoice(workOrderId, discountCode);
    }

    /** Invoices a work order, with a new charge as its own line. */
    public Invoice createInvoice(int workOrderId, String discountCode, String extraName, double extraAmount) {
        return billingService.createInvoice(workOrderId, discountCode, extraName, extraAmount);
    }

    /** Invoices the booking. The work itself sits in {@link BillingService}. */
    public Invoice createInvoiceForBooking(int bookingId, String discountCode) {
        return billingService.createInvoiceForBooking(bookingId, discountCode);
    }

    /** The same invoice, but with room for a new charge the reclamation brings with it. */
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

    /** As above, but with the requirement on specialization. */
    public ServiceItem createServiceItem(String name, String description, double price, int estimatedMinutes,
                                         String specialization) throws SQLException {
        ServiceItem item = new ServiceItem(0, name, description, price, estimatedMinutes, specialization);
        serviceItemRepository.save(item);
        return item;
    }

    public void startWorkOrder(int workOrderId) {
        workOrderService.startWorkOrder(workOrderId);
    }

    /** Marks services as performed and stores their current prices. */
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

    /** Refuses the removal with an explanation. The rules live in {@link RemovalRules}. */
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

    /** The same rule as for the customer, on the path every writer of a mechanic row goes through. */
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