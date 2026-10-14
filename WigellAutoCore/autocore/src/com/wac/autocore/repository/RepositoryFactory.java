package com.wac.autocore.repository;

/** Creates the classes that talk to the database. */
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
