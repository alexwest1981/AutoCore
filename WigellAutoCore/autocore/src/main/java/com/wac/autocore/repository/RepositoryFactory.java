package com.wac.autocore.repository;

import com.wac.autocore.data.Db;

/** Creates the classes that talk to the database. */
public interface RepositoryFactory {

    /** The storage every repository from this factory reads and writes. */
    Db db();

    CustomerRepository createCustomerRepository();

    VehicleRepository createVehicleRepository();

    BookingRepository createBookingRepository();

    ServiceItemRepository createServiceItemRepository();

    MechanicRepository createMechanicRepository();

    WorkOrderRepository createWorkOrderRepository();

    InvoiceRepository createInvoiceRepository();

    PaymentRepository createPaymentRepository();

    ServicePackageRepository createServicePackageRepository();

    ScheduleRepository createScheduleRepository();
}
