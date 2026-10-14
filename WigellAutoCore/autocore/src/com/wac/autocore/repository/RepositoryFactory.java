package com.wac.autocore.repository;

/**
 * Creates the classes that talk to the database. Whatever needs one asks here
 * instead of building its own, so the rest of the tree never names a concrete repository.
 */
public interface RepositoryFactory {

    CustomerRepository createCustomerRepository();

    VehicleRepository createVehicleRepository();

    BookingRepository createBookingRepository();

    ServiceItemRepository createServiceItemRepository();

    MechanicRepository createMechanicRepository();

    WorkOrderRepository createWorkOrderRepository();

    InvoiceRepository createInvoiceRepository();

    PaymentRepository createPaymentRepository();

    ServicePackageRepository createServicePackageRepository();
}
