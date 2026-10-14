package com.wac.autocore.repository;

/** The factory the application uses. Every repository it hands out goes through Db to SQLite. */
public class SqliteRepositoryFactory implements RepositoryFactory {

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
