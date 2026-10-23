package com.wac.autocore.service;

import com.wac.autocore.exception.DataAccessException;
import com.wac.autocore.exception.RuleViolationException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.data.Db;
import com.wac.autocore.data.Settings;
import com.wac.autocore.model.*;
import com.wac.autocore.security.Identity;
import com.wac.autocore.security.Operation;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.CustomerRepository;
import com.wac.autocore.repository.InvoiceRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.PaymentRepository;
import com.wac.autocore.repository.RepositoryFactory;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.ServicePackageRepository;
import com.wac.autocore.repository.VehicleRepository;
import com.wac.autocore.repository.WorkOrderRepository;
import com.wac.autocore.service.gateway.WorkOrderGateway;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;

/** The entry point the interface and the text version call. Passes on to the services. */
public class GarageSystem implements WorkOrderGateway {

    /** The settings row the chosen language is kept in. */
    public static final String LANGUAGE_KEY = "language";

    private final Printer printer;

    private final Db db;

    /** Everyone who wants to hear that something was written, typically the open page. */
    private final List<Runnable> changeListeners = new java.util.concurrent.CopyOnWriteArrayList<Runnable>();
    private final CustomerService customerService;
    private final VehicleService vehicleService;
    private final WorkOrderService workOrderService;
    private final BookingService bookingService;
    private final BillingService billingService;
    private final PaymentService paymentService;
    private final ServicePackageService servicePackageService;
    private final MechanicRules mechanicRules;
    private final RemovalRules removalRules;
    private final ServiceItemRepository serviceItemRepository;
    private final MechanicRepository mechanicRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final MechanicSchedule mechanicSchedule;
    private final WorkOrderRepository workOrderRepository;

    public GarageSystem(RepositoryFactory repositories) {
        this(repositories, new Printer() { });
    }

    /**
     * Whether the person using the system may do this. The window asks before it draws a button,
     * so a function the role cannot use is not offered at all rather than offered and then refused.
     *
     * Without a guard around the service nobody is signed in and nothing is restricted, which is
     * the truth for a plain service object. {@link GuardedGarageSystem} answers for real.
     */
    public boolean may(Operation operation) {
        return true;
    }

    /**
     * Who is using the service, or null when nobody is. The window uses it for the log line when an
     * action is stopped before it reaches the service; the guard answers with the signed-in person.
     */
    public Identity identity() {
        return null;
    }

    public GarageSystem(RepositoryFactory repositories, Printer printer) {

        this.printer = printer;
        this.db = repositories.db();

        db.ensureReady();

        customerRepository = repositories.createCustomerRepository();
        vehicleRepository = repositories.createVehicleRepository();
        bookingRepository = repositories.createBookingRepository();
        serviceItemRepository = repositories.createServiceItemRepository();
        mechanicRepository = repositories.createMechanicRepository();

        workOrderRepository = repositories.createWorkOrderRepository();
        InvoiceRepository invoiceRepository = repositories.createInvoiceRepository();
        PaymentRepository paymentRepository = repositories.createPaymentRepository();
        ServicePackageRepository servicePackageRepository = repositories.createServicePackageRepository();

        mechanicSchedule = new MechanicSchedule(repositories.createScheduleRepository());

        mechanicRules = new MechanicRules(mechanicRepository);
        customerService = new CustomerService(customerRepository);
        vehicleService = new VehicleService(customerService, vehicleRepository);
        workOrderService = new WorkOrderService(workOrderRepository, bookingRepository,
                mechanicRepository, serviceItemRepository, vehicleRepository, mechanicSchedule);
        bookingService = new BookingService(workOrderService, mechanicRules,
                bookingRepository, vehicleRepository, serviceItemRepository);
        billingService = new BillingService(invoiceRepository, workOrderRepository,
                bookingRepository, vehicleRepository, customerRepository, serviceItemRepository);
        paymentService = new PaymentService(paymentRepository, invoiceRepository);
        servicePackageService = new ServicePackageService(servicePackageRepository);
        removalRules = new RemovalRules(workOrderService, bookingService, billingService, vehicleService);
    }

    /**
     * Registers interest in changes. The window uses it to redraw the page it shows, so a view is
     * never left showing what the database held before the last save.
     */
    public void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    /** Says that something was written. Called by the writers below, after the write went through. */
    private void changed() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
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
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read service items", e);
        }
    }

    public List<Mechanic> getMechanics() {
        return mechanicRules.getAll();
    }

    @Override
    public List<WorkOrder> getWorkOrders() {
        return workOrderService.getAll();
    }

    /** True if the booking still has a service no active work order has taken. */
    public boolean hasServicesLeftForAWorkOrder(int bookingId) {
        return workOrderService.hasServicesLeft(bookingId);
    }

    public List<Invoice> getInvoices() {
        return billingService.getAll();
    }

    public List<Payment> getPayments() {
        return paymentService.getAll();
    }

    /** The Mechanic's hour slot for one day. */
    public List<TimeSlot> getSlotsForDay(int mechanicId, LocalDate date) {
        return mechanicSchedule.getSlotsForDay(mechanicId, date);
    }
    /** The next day after the given one when the mechanic has a booking, or null. */
    public LocalDate getNextBookingDate(int mechanicId, LocalDate afterDate) {
        return mechanicSchedule.getNextBookingDate(mechanicId, afterDate);
    }

    /** How full the mechanic's days are in the week starting on the given Monday. */
    public List<DayLoad> getWeekLoads(int mechanicId, LocalDate weekStart) {
        return mechanicSchedule.getWeekLoads(mechanicId, weekStart);
    }

    /** The status of every day in the month for the mechanic. */
    public List<MonthDayStatus> getMonthDays(int mechanicId, YearMonth month, boolean mechanicAvailable) {
        return mechanicSchedule.getMonthDays(mechanicId, month, mechanicAvailable);
    }

    /** Reserves an hour in the schedule for a booking. */
    public boolean reserveSlot(int mechanicId, LocalDate date, int hour, int bookingId,
                               String customer, String vehicleReg, String description) {
        boolean booked = mechanicSchedule.bookSlot(mechanicId, date, hour, bookingId, 0, customer, vehicleReg, description);
        if (booked) {
            changed();
        }
        return booked;
    }

    /** Frees the schedule slot a booking held. */
    public void releaseSlot(int bookingId) {
        mechanicSchedule.cancelSlotForBooking(bookingId);
        changed();
    }

    /** Reads the schedule again after work orders have been created. */
    public void refreshSchedule() {
        mechanicSchedule.syncFromDatabase();
    }

    /** A stored setting, or the fallback when nothing is stored. */
    public String getSetting(String key, String fallback) {
        return Settings.get(db, key, fallback);
    }

    /** Stores a setting so it survives a restart. */
    public void saveSetting(String key, String value) {
        Settings.put(db, key, value);
    }

    /**
     * Stores the chosen language so it survives a restart. It lives in the settings table but is not
     * a business setting, so it has its own operation and every role may do it.
     */
    public void saveLanguage(String lang) {
        Settings.put(db, LANGUAGE_KEY, lang);
    }

    /** The booking's price. The views ask here instead of adding it up themselves. */
    public int bookingPrice(Booking booking) {
        return bookingService.priceOf(booking);
    }

    /** The customer with that id, or null when there is none. */
    public Customer findCustomer(int id) {
        for (Customer row : getCustomers()) {
            if (row.getId() == id) {
                return row;
            }
        }
        return null;
    }

    /** The vehicle with that id, or null when there is none. */
    public Vehicle findVehicle(int id) {
        for (Vehicle row : getVehicles()) {
            if (row.getId() == id) {
                return row;
            }
        }
        return null;
    }

    /** The mechanic with that id, or null when there is none. */
    public Mechanic findMechanic(int id) {
        for (Mechanic row : getMechanics()) {
            if (row.getId() == id) {
                return row;
            }
        }
        return null;
    }

    /** The service with that id, or null when there is none. */
    public ServiceItem findServiceItem(int id) {
        for (ServiceItem row : getServiceItems()) {
            if (row.getId() == id) {
                return row;
            }
        }
        return null;
    }

    /** The booking with that id, or null when there is none. */
    public Booking findBooking(int id) {
        for (Booking row : getBookings()) {
            if (row.getId() == id) {
                return row;
            }
        }
        return null;
    }

    /** The work order with that id, or null when there is none. */
    @Override
    public WorkOrder findWorkOrder(int id) {
        for (WorkOrder row : getWorkOrders()) {
            if (row.getId() == id) {
                return row;
            }
        }
        return null;
    }

    /** The invoice with that id, or null when there is none. */
    public Invoice findInvoice(int id) {
        for (Invoice row : getInvoices()) {
            if (row.getId() == id) {
                return row;
            }
        }
        return null;
    }

    /** The payment with that id, or null when there is none. */
    public Payment findPayment(int id) {
        for (Payment row : getPayments()) {
            if (row.getId() == id) {
                return row;
            }
        }
        return null;
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
        Customer customer = customerService.createCustomer(name, phone, email);
        changed();
        return customer;
    }

    public Vehicle createVehicle(String registrationNumber,
                                 String brand,
                                 String model,
                                 int year,
                                 int customerId) {
        Vehicle vehicle = vehicleService.createVehicle(registrationNumber, brand, model, year, customerId);
        changed();
        return vehicle;
    }

    public Booking createBooking(int vehicleId,
                                 LocalDate date,
                                 String description) {
        Booking booking = bookingService.createBooking(vehicleId, date, description);
        changed();
        return booking;
    }

    public Booking createBooking(int vehicleId,
                                 LocalDate date,
                                 String description, LocalTime startTime, int mechanicId, int serviceItemId) {
        Booking booking = bookingService.createBooking(vehicleId, date, description,
                startTime, mechanicId, serviceItemId);
        changed();
        return booking;
    }

    public WorkOrder createWorkOrder(int bookingId, int mechanicId) {
        WorkOrder order = workOrderService.createWorkOrder(bookingId, mechanicId);
        changed();
        return order;
    }

    /** Creates a work order for a selection of the services, so a booking can be split. */
    public WorkOrder createWorkOrder(int bookingId, int mechanicId, java.util.List<Integer> serviceItemIds) {
        WorkOrder order = workOrderService.createWorkOrder(bookingId, mechanicId, serviceItemIds);
        changed();
        return order;
    }

    /** Creates a work order of a given type: Standard, Reclamation or Internal. */
    @Override
    public WorkOrder createWorkOrder(int bookingId, int mechanicId, java.util.List<Integer> serviceItemIds,
                                     WorkOrderType type) {
        WorkOrder order = workOrderService.createWorkOrder(bookingId, mechanicId, serviceItemIds, type);
        changed();
        return order;
    }

    /** Creates a reclamation on a previously performed work order. */
    @Override
    public WorkOrder createReclamation(int originalWorkOrderId, String description) {
        WorkOrder order = workOrderService.createReclamation(originalWorkOrderId, description);
        changed();
        return order;
    }

    // Creates a draft, that is a work order with no booking and no services.
    @Override
    public WorkOrder createDraft(int vehicleId, String description) {
        WorkOrder draft = workOrderService.createDraft(vehicleId, description);
        changed();
        return draft;
    }

    @Override
    public WorkOrder updateDraft(DraftChanges changes) {
        WorkOrder draft = workOrderService.updateDraft(changes);
        changed();
        return draft;
    }

    /** Drop-in with no booked time: the booking row is created first, in the same sweep, so that
     *  everything that looks up customer and vehicle through the booking keeps working. The
     *  mechanics come from the form, which fills them in from the services' requirements. */
    public Booking createDropInBooking(int vehicleId, java.util.List<ServiceItem> services,
                                       java.util.List<Mechanic> mechanics) {
        Booking booking = bookingService.createDropInBooking(vehicleId, services, mechanics);
        changed();
        return booking;
    }

    /**
     * The same drop-in on a day and from an hour the caller decides. The window always uses the one
     * above, which reads the clock; a proof uses this one so its answer does not depend on when it
     * is run.
     */
    public Booking createDropInBooking(int vehicleId, java.util.List<ServiceItem> services,
                                       java.util.List<Mechanic> mechanics, LocalDate day, LocalTime from) {
        Booking booking = bookingService.createDropInBooking(vehicleId, services, mechanics, day, from);
        changed();
        return booking;
    }

    /** The time a drop-in would get right now, or null when the team's hours are out for the day. */
    public LocalTime dropInStartTime(java.util.List<ServiceItem> services,
                                     java.util.List<Mechanic> mechanics) {
        return bookingService.dropInStartTime(services, mechanics);
    }

    public Invoice createInvoice(int workOrderId, String discountCode) {
        Invoice invoice = billingService.createInvoice(workOrderId, discountCode);
        changed();
        return invoice;
    }

    /** Invoices a work order, with a new charge as its own line. */
    public Invoice createInvoice(int workOrderId, String discountCode, String extraName, int extraAmount) {
        Invoice invoice = billingService.createInvoice(workOrderId, discountCode, extraName, extraAmount);
        changed();
        return invoice;
    }

    /** Invoices the booking. The work itself sits in {@link BillingService}. */
    public Invoice createInvoiceForBooking(int bookingId, String discountCode) {
        Invoice invoice = billingService.createInvoiceForBooking(bookingId, discountCode);
        changed();
        return invoice;
    }

    /** The same invoice, but with room for a new charge the reclamation brings with it. */
    public Invoice createInvoiceForBooking(int bookingId, String discountCode, String extraName, int extraAmount) {
        Invoice invoice = billingService.createInvoiceForBooking(bookingId, discountCode, extraName, extraAmount);
        changed();
        return invoice;
    }

    public Mechanic createMechanic(String name, String phone, String specialization) {
        refuseUnlessStorableMechanic(name, phone);
        Mechanic mechanic = new Mechanic(0, name, phone, specialization);
        mechanicRepository.save(mechanic);
        changed();
        return mechanic;
    }

    public ServiceItem createServiceItem(String name, String description, int price, int estimatedMinutes) {
        return createServiceItem(name, description, price, estimatedMinutes, "");
    }

    /** As above, but with the requirement on specialization. */
    public ServiceItem createServiceItem(String name, String description, int price, int estimatedMinutes, String specialization) {
        ServiceItem item = new ServiceItem(0, name, description, price, estimatedMinutes, specialization);
        serviceItemRepository.save(item);
        changed();
        return item;
    }

    @Override
    public void startWorkOrder(int workOrderId) {
        workOrderService.startWorkOrder(workOrderId);
        changed();
    }

    /** Marks services as performed and stores their current prices. */
    public boolean markServicesAsCompleted(int workOrderId, int[] serviceItemIds) {
        boolean marked = workOrderService.markServicesAsCompleted(workOrderId, serviceItemIds);
        if (marked) {
            changed();
        }
        return marked;
    }

    @Override
    public void completeWorkOrder(int workOrderId) {
        workOrderService.completeWorkOrder(workOrderId);
        changed();
    }

    @Override
    public boolean cancelWorkOrder(int workOrderId) {
        boolean cancelled = workOrderService.cancelWorkOrder(workOrderId);
        if (cancelled) {
            changed();
        }
        return cancelled;
    }

    @Override
    public boolean confirmWorkOrder(int workOrderId) {
        boolean confirmed = workOrderService.confirmWorkOrder(workOrderId);
        if (confirmed) {
            changed();
        }
        return confirmed;
    }

    public Payment processPayment(int invoiceId, PaymentType paymentType) {
        Payment payment = paymentService.processPayment(invoiceId, paymentType);
        changed();
        return payment;
    }

    public void updateCustomer(Customer customer) {
        customerService.updateCustomer(customer);
        changed();
    }

    public void updateVehicle(Vehicle vehicle) {
        vehicleService.updateVehicle(vehicle);
        changed();
    }

    public void updateMechanic(Mechanic mechanic) {

        if (mechanic == null) {
            return;
        }

        refuseUnlessStorableMechanic(mechanic.getName(), mechanic.getPhone());
        mechanicRepository.save(mechanic);
        changed();
    }

    /** New or changed, the booking goes through the same rules before it is stored. */
    public Booking saveBooking(Booking booking) {
        Booking saved = bookingService.saveBooking(booking);
        mechanicSchedule.syncFromDatabase();
        changed();
        return saved;
    }

    public void updateServiceItem(ServiceItem item) {
        serviceItemRepository.save(item);
        changed();
    }

    public boolean canDeleteMechanic(int mechanicId) {
        return removalRules.canDeleteMechanic(mechanicId);
    }

    public void deleteMechanic(int mechanicId) {

        refuseUnless(canDeleteMechanic(mechanicId),
                "Mekanikern kan inte tas bort: den används av en bokning eller ett pågående arbete.",
                "dialog.mechanic.delete.has_active_orders");
        mechanicRepository.delete(mechanicId);
        mechanicSchedule.removeSlotsForMechanic(mechanicId);
        changed();
    }

    public boolean canDeleteCustomer(int customerId) {
        return removalRules.canDeleteCustomer(customerId);
    }

    public void deleteCustomer(int customerId) {

        refuseUnless(canDeleteCustomer(customerId),
                "Kunden kan inte tas bort: ett av kundens fordon har ett fakturerat jobb.",
                "dialog.customer.delete.has_active_orders");

        for (Vehicle v : getVehicles()) {
            if (v.getCustomerId() == customerId) {
                vehicleRepository.delete(v.getId());
            }
        }
        customerRepository.delete(customerId);
        changed();
    }

    public boolean canDeleteVehicle(int vehicleId) {
        return removalRules.canDeleteVehicle(vehicleId);
    }

    public void deleteVehicle(int vehicleId) {
        refuseUnless(canDeleteVehicle(vehicleId),
                "Fordonet kan inte tas bort: det har en bokning eller ett fakturerat jobb.",
                "dialog.vehicle.delete.has_active_orders");
        vehicleRepository.delete(vehicleId);
        changed();
    }

    public boolean canDeleteWorkOrder(int workOrderId) {
        return removalRules.canDeleteWorkOrder(workOrderId);
    }

    /**
     * Removes an order that was created and called off by mistake, with its service rows, which
     * the key takes with it. Anything that points at the order, an invoice or a reclamation, keeps
     * it in place; {@link RemovalRules} answers that before the database has to.
     */
    public void deleteWorkOrder(int workOrderId) {
        refuseUnless(canDeleteWorkOrder(workOrderId),
                "Arbetsordern kan inte tas bort: bara en avbruten order utan faktura eller reklamation går att ta bort.",
                "dialog.workorder.delete.has_history");
        workOrderRepository.delete(workOrderId);
        changed();
    }

    public boolean canCancelOrDeleteBooking(int bookingId) {
        return removalRules.canCancelOrDeleteBooking(bookingId);
    }

    public void cancelBooking(int bookingId) {
        Booking b = bookingService.findById(bookingId);
        if (b != null) {
            b.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(b);
            mechanicSchedule.cancelSlotForBooking(bookingId);
            mechanicSchedule.syncFromDatabase();
            changed();
        }
    }

    /** Refuses the removal with an explanation. The rules live in {@link RemovalRules}. */
    private void refuseUnless(boolean allowed, String reason, String messageKey) {

        if (!allowed) {
            throw new RuleViolationException(reason, messageKey);
        }
    }

    public void deleteBooking(int bookingId) {
        refuseUnless(canCancelOrDeleteBooking(bookingId),
                "Bokningen kan inte tas bort: den har en arbetsorder med faktura.",
                "dialog.booking.delete.has_active_orders");
        bookingRepository.delete(bookingId);
        mechanicSchedule.cancelSlotForBooking(bookingId);
        mechanicSchedule.syncFromDatabase();
        changed();
    }

    public boolean canDeleteServiceItem(int serviceItemId) {
        return removalRules.canDeleteServiceItem(serviceItemId);
    }

    public void deleteServiceItem(int serviceItemId) {
        refuseUnless(canDeleteServiceItem(serviceItemId),
                "Tjänsten kan inte tas bort: den ligger i en bokning eller en arbetsorder.",
                "dialog.service.delete.has_active_orders");
        serviceItemRepository.delete(serviceItemId);
        changed();
    }

    /** The same rule as for the customer, on the path every writer of a mechanic row goes through. */
    private static void refuseUnlessStorableMechanic(String name, String phone) {
        String problem = Mechanic.validationProblem(name, phone);
        if (problem != null) {
            throw new ValidationException(problem);
        }
    }
    public List<ServicePackage> getServicePackages() {
        return servicePackageService.getAll();
    }

    public ServicePackage createServicePackage(String name, String description, List<ServiceItem> serviceItems) {
        ServicePackage servicePackage = servicePackageService.createPackage(name, description, serviceItems);
        changed();
        return servicePackage;
    }

    public void updateServicePackage(ServicePackage servicePackage) {
        servicePackageService.updatePackage(servicePackage);
        changed();
    }

    public void deleteServicePackage(int id) {
        servicePackageService.deletePackage(id);
        changed();
    }
}