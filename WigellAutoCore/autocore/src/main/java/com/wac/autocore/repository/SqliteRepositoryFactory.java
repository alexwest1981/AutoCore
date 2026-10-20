package com.wac.autocore.repository;

import com.wac.autocore.data.Db;

/** The one the application uses. */
public class SqliteRepositoryFactory implements RepositoryFactory {

    private final Db db;

    /** Repositories for the file the caller chose. */
    public SqliteRepositoryFactory(Db db) {
        this.db = db;
    }

    @Override
    public Db db() {
        return db;
    }

    @Override
    public CustomerRepository createCustomerRepository() {
        return new CustomerRepository(db);
    }

    @Override
    public VehicleRepository createVehicleRepository() {
        return new VehicleRepository(db);
    }

    @Override
    public BookingRepository createBookingRepository() {
        return new BookingRepository(db, new BookingServiceItemRepository(db), new BookingMechanicRepository(db));
    }

    @Override
    public ServiceItemRepository createServiceItemRepository() {
        return new ServiceItemRepository(db);
    }

    @Override
    public MechanicRepository createMechanicRepository() {
        return new MechanicRepository(db);
    }

    @Override
    public WorkOrderRepository createWorkOrderRepository() {
        return new WorkOrderRepository(db);
    }

    @Override
    public InvoiceRepository createInvoiceRepository() {
        return new InvoiceRepository(db, new InvoiceLineRepository(db));
    }

    @Override
    public PaymentRepository createPaymentRepository() {
        return new PaymentRepository(db);
    }

    @Override
    public ServicePackageRepository createServicePackageRepository() {
        return new ServicePackageRepository(db);
    }

    @Override
    public ScheduleRepository createScheduleRepository() {
        return new ScheduleRepository(db);
    }
}
