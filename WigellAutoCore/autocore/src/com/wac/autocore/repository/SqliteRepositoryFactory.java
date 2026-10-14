package com.wac.autocore.repository;

import com.wac.autocore.data.Db;

/** The one the application uses. */
public class SqliteRepositoryFactory implements RepositoryFactory {

    /** Repositories for the file the storage is already pointed at. */
    public SqliteRepositoryFactory() {
    }

    /** Repositories for a given file. */
    public SqliteRepositoryFactory(String databasePath) {
        Db.use(databasePath);
    }

    @Override
    public CustomerRepository createCustomerRepository() {
        return new CustomerRepository();
    }

    @Override
    public VehicleRepository createVehicleRepository() {
        return new VehicleRepository();
    }

    @Override
    public BookingRepository createBookingRepository() {
        return new BookingRepository();
    }

    @Override
    public ServiceItemRepository createServiceItemRepository() {
        return new ServiceItemRepository();
    }

    @Override
    public MechanicRepository createMechanicRepository() {
        return new MechanicRepository();
    }

    @Override
    public WorkOrderRepository createWorkOrderRepository() {
        return new WorkOrderRepository();
    }

    @Override
    public InvoiceRepository createInvoiceRepository() {
        return new InvoiceRepository();
    }

    @Override
    public PaymentRepository createPaymentRepository() {
        return new PaymentRepository();
    }

    @Override
    public ServicePackageRepository createServicePackageRepository() {
        return new ServicePackageRepository();
    }
}
