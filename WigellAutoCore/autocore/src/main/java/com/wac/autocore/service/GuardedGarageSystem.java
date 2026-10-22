package com.wac.autocore.service;

import com.wac.autocore.exception.AccessDeniedException;
import com.wac.autocore.exception.NotAuthenticatedException;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.RepositoryFactory;
import com.wac.autocore.security.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class GuardedGarageSystem extends GarageSystem {

    private static final Logger log = LoggerFactory.getLogger(GuardedGarageSystem.class);

    private final IdentityHolder identityHolder;

    public GuardedGarageSystem(RepositoryFactory repositories, IdentityHolder identityHolder) {
        super(repositories);
        this.identityHolder = identityHolder;
    }

    /** The same guard over a service that writes somewhere other than the window, such as a console. */
    public GuardedGarageSystem(RepositoryFactory repositories, IdentityHolder identityHolder, Printer printer) {
        super(repositories, printer);
        this.identityHolder = identityHolder;
    }

    /**
     * The same rule the guard enforces, as a question. The window asks this before it draws a
     * button, so a function the role cannot use is never offered.
     */
    @Override
    public boolean may(Operation operation) {

        return identityHolder.current()
                .map(identity -> RoleMatrix.allows(identity.role(), operation))
                .orElse(false);
    }

    @Override
    public Identity identity() {

        return identityHolder.current().orElse(null);
    }

    private void require(Operation operation) {

        // A new operation starts here: every line it logs, down to the dialog that shows a
        // refusal, carries the same trace-id and the operation's name. A read joins the operation
        // that is going on (GarageSystem asks canDeleteCustomer in the middle of deleteCustomer,
        // for example); only a change starts a new trace.
        if (operation != Operation.READ || MDC.get("traceId") == null) {
            MDC.put("traceId", UUID.randomUUID().toString().substring(0, 8));
            MDC.put("operation", operation.name());
        }

        // Two refusals, two answers. Nobody signed in is a different thing from being signed in
        // without the right, and the order asks the system to tell them apart.
        java.util.Optional<Identity> current = identityHolder.current();
        if (current.isEmpty()) {
            log.warn("Åtkomst nekad: ingen inloggad försökte {}", operation);
            throw new NotAuthenticatedException("Nobody is signed in, and " + operation + " needs one.");
        }

        Role role = current.get().role();
        if (!RoleMatrix.allows(role, operation)) {
            log.warn("Åtkomst nekad: {} ({}) försökte {}", current.get().username(), role, operation);
            throw new AccessDeniedException("Role " + role + " may not " + operation + ".");
        }
    }

    @Override
    public Customer createCustomer(String name, String phone, String email) {

        require(Operation.CREATE_CUSTOMER);

        return super.createCustomer(name, phone, email);
    }

    @Override
    public void updateCustomer(Customer customer) {

        require(Operation.UPDATE_CUSTOMER);

        super.updateCustomer(customer);
    }

    @Override
    public void deleteCustomer(int customerId) {

        require(Operation.DELETE_CUSTOMER);

        super.deleteCustomer(customerId);
    }

    /**
     * The row is gone afterwards, so the log is the only place left that says who removed it. It is
     * written after the delete went through, never before, so a refused delete leaves no trace of
     * one that did not happen.
     */
    @Override
    public void deleteWorkOrder(int workOrderId) {

        require(Operation.DELETE_WORK_ORDER);

        super.deleteWorkOrder(workOrderId);

        java.util.Optional<Identity> current = identityHolder.current();
        String who = current.isPresent() ? current.get().username() : "okänd";
        log.info("Arbetsorder {} borttagen av {}", workOrderId, who);
    }

    @Override
    public Vehicle createVehicle(String registrationNumber, String brand, String model, int year, int customerId) {

        require(Operation.CREATE_VEHICLE);

        return super.createVehicle(registrationNumber, brand, model, year, customerId);
    }

    @Override
    public void updateVehicle(Vehicle vehicle) {

        require(Operation.UPDATE_VEHICLE);

        super.updateVehicle(vehicle);
    }

    @Override
    public void deleteVehicle(int vehicleId) {

        require(Operation.DELETE_VEHICLE);

        super.deleteVehicle(vehicleId);
    }

    @Override
    public Booking createBooking(int vehicleId, LocalDate date, String description) {

        require(Operation.CREATE_BOOKING);

        return super.createBooking(vehicleId, date, description);
    }

    @Override
    public Booking createBooking(int vehicleId, LocalDate date, String description, LocalTime startTime, int mechanicId, int serviceItemId) {

        require(Operation.CREATE_BOOKING);

        return super.createBooking(vehicleId, date, description, startTime, mechanicId, serviceItemId);
    }

    @Override
    public Booking saveBooking(Booking booking) {

        require(Operation.UPDATE_BOOKING);

        return super.saveBooking(booking);
    }

    @Override
    public void cancelBooking(int bookingId) {

        require(Operation.CANCEL_BOOKING);

        super.cancelBooking(bookingId);
    }

    @Override
    public void deleteBooking(int bookingId) {

        require(Operation.DELETE_BOOKING);

        super.deleteBooking(bookingId);
    }

    @Override
    public Booking createDropInBooking(int vehicleId, List<ServiceItem> services, List<Mechanic> mechanics) {

        require(Operation.CREATE_BOOKING);

        return super.createDropInBooking(vehicleId, services, mechanics);
    }

    /** The proof's road, with the day in the caller's hand. It writes the same row, so it is guarded
     *  the same way. */
    @Override
    public Booking createDropInBooking(int vehicleId, List<ServiceItem> services, List<Mechanic> mechanics,
                                       LocalDate day, LocalTime from) {

        require(Operation.CREATE_BOOKING);

        return super.createDropInBooking(vehicleId, services, mechanics, day, from);
    }

    @Override
    public WorkOrder createWorkOrder(int bookingId, int mechanicId) {

        require(Operation.CREATE_WORK_ORDER);

        return super.createWorkOrder(bookingId, mechanicId);
    }

    @Override
    public WorkOrder createWorkOrder(int bookingId, int mechanicId, List<Integer> serviceItemIds) {

        require(Operation.CREATE_WORK_ORDER);

        return super.createWorkOrder(bookingId, mechanicId, serviceItemIds);
    }

    @Override
    public WorkOrder createWorkOrder(int bookingId, int mechanicId, List<Integer> serviceItemIds, String type) {

        require(Operation.CREATE_WORK_ORDER);

        return super.createWorkOrder(bookingId, mechanicId, serviceItemIds, type);
    }

    @Override
    public WorkOrder createReclamation(int originalWorkOrderId, String description) {

        require(Operation.CREATE_WORK_ORDER);

        return super.createReclamation(originalWorkOrderId, description);
    }

    @Override
    public WorkOrder createDraft(int vehicleId, String description) {

        require(Operation.CREATE_WORK_ORDER);

        return super.createDraft(vehicleId, description);
    }

    @Override
    public WorkOrder updateDraft(DraftChanges changes) {

        require(Operation.UPDATE_WORK_ORDER);

        return super.updateDraft(changes);
    }

    @Override
    public boolean reserveSlot(int mechanicId, LocalDate date, int hour, int bookingId, String customer, String vehicleReg, String description) {

        require(Operation.UPDATE_WORK_ORDER);

        return super.reserveSlot(mechanicId, date, hour, bookingId, customer, vehicleReg, description);
    }

    @Override
    public void releaseSlot(int bookingId) {

        require(Operation.UPDATE_WORK_ORDER);

        super.releaseSlot(bookingId);
    }

    @Override
    public void startWorkOrder(int workOrderId) {

        require(Operation.CHANGE_WORK_ORDER_STATUS);

        super.startWorkOrder(workOrderId);
    }

    @Override
    public boolean confirmWorkOrder(int workOrderId) {

        require(Operation.CHANGE_WORK_ORDER_STATUS);

        return super.confirmWorkOrder(workOrderId);
    }

    @Override
    public void completeWorkOrder(int workOrderId) {

        require(Operation.CHANGE_WORK_ORDER_STATUS);

        super.completeWorkOrder(workOrderId);
    }

    @Override
    public boolean cancelWorkOrder(int workOrderId) {

        require(Operation.CANCEL_WORK_ORDER);

        return super.cancelWorkOrder(workOrderId);
    }

    @Override
    public boolean markServicesAsCompleted(int workOrderId, int[] serviceItemIds) {

        require(Operation.CHANGE_WORK_ORDER_STATUS);

        return super.markServicesAsCompleted(workOrderId, serviceItemIds);
    }

    @Override
    public Invoice createInvoice(int workOrderId, String discountCode) {

        require(Operation.CREATE_INVOICE);

        return super.createInvoice(workOrderId, discountCode);
    }

    @Override
    public Invoice createInvoice(int workOrderId, String discountCode, String extraName, int extraAmount) {

        require(Operation.CREATE_INVOICE);

        return super.createInvoice(workOrderId, discountCode, extraName, extraAmount);
    }

    @Override
    public Invoice createInvoiceForBooking(int bookingId, String discountCode) {

        require(Operation.CREATE_INVOICE);

        return super.createInvoiceForBooking(bookingId, discountCode);
    }

    @Override
    public Invoice createInvoiceForBooking(int bookingId, String discountCode, String extraName, int extraAmount) {

        require(Operation.CREATE_INVOICE);

        return super.createInvoiceForBooking(bookingId, discountCode, extraName, extraAmount);
    }

    @Override
    public Payment processPayment(int invoiceId, String paymentType) {

        require(Operation.TAKE_PAYMENT);

        return super.processPayment(invoiceId, paymentType);
    }

    @Override
    public Mechanic createMechanic(String name, String phone, String specialization) {

        require(Operation.CREATE_MECHANIC);

        return super.createMechanic(name, phone, specialization);
    }

    @Override
    public void updateMechanic(Mechanic mechanic) {

        require(Operation.UPDATE_MECHANIC);

        super.updateMechanic(mechanic);
    }

    @Override
    public void deleteMechanic(int mechanicId) {

        require(Operation.DELETE_MECHANIC);

        super.deleteMechanic(mechanicId);
    }

    @Override
    public ServiceItem createServiceItem(String name, String description, int price, int estimatedMinutes) {

        require(Operation.CREATE_SERVICE_ITEM);

        return super.createServiceItem(name, description, price, estimatedMinutes);
    }

    @Override
    public ServiceItem createServiceItem(String name, String description, int price, int estimatedMinutes, String specialization) {

        require(Operation.CREATE_SERVICE_ITEM);

        return super.createServiceItem(name, description, price, estimatedMinutes, specialization);
    }

    @Override
    public void updateServiceItem(ServiceItem item) {

        require(Operation.UPDATE_SERVICE_ITEM);

        super.updateServiceItem(item);
    }

    @Override
    public void deleteServiceItem(int serviceItemId) {

        require(Operation.DELETE_SERVICE_ITEM);

        super.deleteServiceItem(serviceItemId);
    }

    @Override
    public ServicePackage createServicePackage(String name, String description, List<ServiceItem> serviceItems) {

        require(Operation.CREATE_SERVICE_PACKAGE);

        return super.createServicePackage(name, description, serviceItems);
    }

    @Override
    public void updateServicePackage(ServicePackage servicePackage) {

        require(Operation.UPDATE_SERVICE_PACKAGE);

        super.updateServicePackage(servicePackage);
    }

    @Override
    public void deleteServicePackage(int id) {

        require(Operation.DELETE_SERVICE_PACKAGE);

        super.deleteServicePackage(id);
    }

    @Override
    public void saveSetting(String key, String value) {

        require(Operation.CHANGE_SETTINGS);

        super.saveSetting(key, value);
    }

    /**
     * Every role may choose the language for its own window, so this one is not behind
     * CHANGE_SETTINGS. Nobody signed in still may not write it, for the same reason as the rest.
     */
    @Override
    public void saveLanguage(String lang) {

        require(Operation.CHANGE_LANGUAGE);

        super.saveLanguage(lang);
    }

    @Override
    public void refreshSchedule() {

        require(Operation.READ);

        super.refreshSchedule();
    }

    // Reading is an operation too. Without this block a caller with no identity could walk straight
    // past the window and read every customer, vehicle and work order in the database, which is
    // exactly the way round the guard that the order says must not exist.
    // addChangeListener stays open on purpose: it hands the window a Runnable and reads nothing.

    @Override
    public List<Customer> getCustomers() {

        require(Operation.READ);

        return super.getCustomers();
    }

    @Override
    public List<Vehicle> getVehicles() {

        require(Operation.READ);

        return super.getVehicles();
    }

    @Override
    public List<Booking> getBookings() {

        require(Operation.READ);

        return super.getBookings();
    }

    @Override
    public List<ServiceItem> getServiceItems() {

        require(Operation.READ);

        return super.getServiceItems();
    }

    @Override
    public List<Mechanic> getMechanics() {

        require(Operation.READ);

        return super.getMechanics();
    }

    @Override
    public List<WorkOrder> getWorkOrders() {

        require(Operation.READ);

        return super.getWorkOrders();
    }

    @Override
    public List<Invoice> getInvoices() {

        require(Operation.READ_FINANCE);

        return super.getInvoices();
    }

    @Override
    public List<Payment> getPayments() {

        require(Operation.READ_FINANCE);

        return super.getPayments();
    }

    @Override
    public List<ServicePackage> getServicePackages() {

        require(Operation.READ);

        return super.getServicePackages();
    }

    @Override
    public List<TimeSlot> getSlotsForDay(int mechanicId, LocalDate date) {

        require(Operation.READ);

        return super.getSlotsForDay(mechanicId, date);
    }

    @Override
    public LocalDate getNextBookingDate(int mechanicId, LocalDate afterDate) {

        require(Operation.READ);

        return super.getNextBookingDate(mechanicId, afterDate);
    }

    @Override
    public List<DayLoad> getWeekLoads(int mechanicId, LocalDate weekStart) {

        require(Operation.READ);

        return super.getWeekLoads(mechanicId, weekStart);
    }

    @Override
    public List<MonthDayStatus> getMonthDays(int mechanicId, YearMonth month, boolean mechanicAvailable) {

        require(Operation.READ);

        return super.getMonthDays(mechanicId, month, mechanicAvailable);
    }

    @Override
    public String getSetting(String key, String fallback) {

        require(Operation.READ);

        return super.getSetting(key, fallback);
    }

    @Override
    public int bookingPrice(Booking booking) {

        require(Operation.READ);

        return super.bookingPrice(booking);
    }

    @Override
    public Customer findCustomer(int id) {

        require(Operation.READ);

        return super.findCustomer(id);
    }

    @Override
    public Vehicle findVehicle(int id) {

        require(Operation.READ);

        return super.findVehicle(id);
    }

    @Override
    public Mechanic findMechanic(int id) {

        require(Operation.READ);

        return super.findMechanic(id);
    }

    @Override
    public ServiceItem findServiceItem(int id) {

        require(Operation.READ);

        return super.findServiceItem(id);
    }

    @Override
    public Booking findBooking(int id) {

        require(Operation.READ);

        return super.findBooking(id);
    }

    @Override
    public WorkOrder findWorkOrder(int id) {

        require(Operation.READ);

        return super.findWorkOrder(id);
    }

    @Override
    public boolean hasServicesLeftForAWorkOrder(int bookingId) {

        require(Operation.READ);

        return super.hasServicesLeftForAWorkOrder(bookingId);
    }

    @Override
    public Invoice findInvoice(int id) {

        require(Operation.READ_FINANCE);

        return super.findInvoice(id);
    }

    @Override
    public Payment findPayment(int id) {

        require(Operation.READ_FINANCE);

        return super.findPayment(id);
    }

    @Override
    public List<Mechanic> getQualifiedMechanics(ServiceItem service) {

        require(Operation.READ);

        return super.getQualifiedMechanics(service);
    }

    @Override
    public List<Mechanic> getQualifiedMechanics(Collection<ServiceItem> services) {

        require(Operation.READ);

        return super.getQualifiedMechanics(services);
    }

    @Override
    public List<Mechanic> getRequiredMechanics(Collection<ServiceItem> services) {

        require(Operation.READ);

        return super.getRequiredMechanics(services);
    }

    @Override
    public boolean isMechanicQualified(Mechanic mechanic, ServiceItem service) {

        require(Operation.READ);

        return super.isMechanicQualified(mechanic, service);
    }

    @Override
    public int busyMinutes(List<ServiceItem> services, List<Mechanic> mechanics) {

        require(Operation.READ);

        return super.busyMinutes(services, mechanics);
    }

    @Override
    public List<Booking> getInvoiceableBookings() {

        require(Operation.READ_FINANCE);

        return super.getInvoiceableBookings();
    }

    @Override
    public boolean hasBookingWithUnfinishedWork() {

        require(Operation.READ);

        return super.hasBookingWithUnfinishedWork();
    }

    @Override
    public LocalTime dropInStartTime(List<ServiceItem> services, List<Mechanic> mechanics) {

        require(Operation.READ);

        return super.dropInStartTime(services, mechanics);
    }

    @Override
    public boolean canDeleteMechanic(int mechanicId) {

        require(Operation.READ);

        return super.canDeleteMechanic(mechanicId);
    }

    @Override
    public boolean canDeleteCustomer(int customerId) {

        require(Operation.READ);

        return super.canDeleteCustomer(customerId);
    }

    @Override
    public boolean canDeleteVehicle(int vehicleId) {

        require(Operation.READ);

        return super.canDeleteVehicle(vehicleId);
    }

    @Override
    public boolean canCancelOrDeleteBooking(int bookingId) {

        require(Operation.READ);

        return super.canCancelOrDeleteBooking(bookingId);
    }

    @Override
    public boolean canDeleteServiceItem(int serviceItemId) {

        require(Operation.READ);

        return super.canDeleteServiceItem(serviceItemId);
    }

    @Override
    public boolean canDeleteWorkOrder(int workOrderId) {

        require(Operation.READ);

        // The question is whether this person may remove this order, so the role is part of the
        // answer. Without it the window drew a delete button for a staff member and the refusal
        // only arrived after the press, which is the behaviour the team asked to be rid of.
        return may(Operation.DELETE_WORK_ORDER) && super.canDeleteWorkOrder(workOrderId);
    }

    @Override
    public void showCustomers() {

        require(Operation.READ);

        super.showCustomers();
    }

    @Override
    public void showVehicles() {

        require(Operation.READ);

        super.showVehicles();
    }

    @Override
    public void showBookings() {

        require(Operation.READ);

        super.showBookings();
    }

    @Override
    public void showServiceItems() {

        require(Operation.READ);

        super.showServiceItems();
    }

    @Override
    public void showMechanics() {

        require(Operation.READ);

        super.showMechanics();
    }

    @Override
    public void showWorkOrders() {

        require(Operation.READ);

        super.showWorkOrders();
    }

    @Override
    public void showInvoices() {

        require(Operation.READ_FINANCE);

        super.showInvoices();
    }

    @Override
    public void showPayments() {

        require(Operation.READ_FINANCE);

        super.showPayments();
    }
}
