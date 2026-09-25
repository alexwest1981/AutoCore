package com.wac.autocore.data;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.CustomerRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepository;
import com.wac.autocore.repository.WorkOrderRepository;

import java.sql.SQLException;
import java.time.LocalDate;

public class SeedData {

    public static void seedIfEmpty() {
        CustomerRepository customerRepository = new CustomerRepository();
        VehicleRepository vehicleRepository = new VehicleRepository();
        ServiceItemRepository serviceItemRepository = new ServiceItemRepository();
        MechanicRepository mechanicRepository = new MechanicRepository();
        BookingRepository bookingRepository = new BookingRepository();
        WorkOrderRepository workOrderRepository = new WorkOrderRepository();

        try {
            if (!customerRepository.findAll().isEmpty()) {
                return;
            }

            Customer anna = new Customer(0, "Anna Andersson", "070-1111111", "anna.andersson@email.se");
            customerRepository.save(anna);

            Customer erik = new Customer(0, "Erik Eriksson", "070-2222222", "erik.eriksson@email.se");
            erik.setVip(true);
            customerRepository.save(erik);

            Customer maria = new Customer(0, "Maria Svensson", "070-3333333", "maria.svensson@email.se");
            customerRepository.save(maria);

            Customer olof = new Customer(0, "Olof Palme", "070-4444444", "olof.palme@email.se");
            customerRepository.save(olof);

            Customer sven = new Customer(0, "Sven Melander", "070-5555555", "sven.melander@email.se");
            customerRepository.save(sven);

            Customer gustav = new Customer(0, "Gustav Vasa", "070-6666666", "gustav.vasa@email.se");
            customerRepository.save(gustav);

            Vehicle volvo = new Vehicle(0, "ABC123", "Volvo", "V70", 2012, anna.getId());
            vehicleRepository.save(volvo);

            Vehicle volkswagen = new Vehicle(0, "DEF456", "Volkswagen", "Passat", 2018, erik.getId());
            vehicleRepository.save(volkswagen);

            Vehicle toyota = new Vehicle(0, "GHI789", "Toyota", "Corolla", 2020, maria.getId());
            vehicleRepository.save(toyota);

            Vehicle bmw = new Vehicle(0, "XYZ999", "BMW", "320d", 2021, olof.getId());
            vehicleRepository.save(bmw);

            Vehicle audi = new Vehicle(0, "AAA001", "Audi", "A4", 2019, sven.getId());
            vehicleRepository.save(audi);

            Vehicle mercedes = new Vehicle(0, "BBB002", "Mercedes", "C220", 2022, gustav.getId());
            vehicleRepository.save(mercedes);

            ServiceItem oilChange = new ServiceItem(0, "seed.service.oil_change.name",
                    "seed.service.oil_change.desc", 1295.0, 45);
            serviceItemRepository.save(oilChange);

            ServiceItem brakeService = new ServiceItem(0, "seed.service.brake_service.name",
                    "seed.service.brake_service.desc", 2495.0, 90);
            serviceItemRepository.save(brakeService);

            ServiceItem diagnostics = new ServiceItem(0, "seed.service.diagnostics.name",
                    "seed.service.diagnostics.desc", 995.0, 60);
            serviceItemRepository.save(diagnostics);

            ServiceItem annualService = new ServiceItem(0, "seed.service.annual_service.name",
                    "seed.service.annual_service.desc", 3495.0, 120);
            serviceItemRepository.save(annualService);

            Mechanic johan = new Mechanic(0, "Johan Karlsson", "070-5551111", "seed.mechanic.general_service.specialization");
            mechanicRepository.save(johan);

            Mechanic sara = new Mechanic(0, "Sara Nilsson", "070-5552222", "seed.mechanic.brakes.specialization");
            mechanicRepository.save(sara);

            Mechanic mikael = new Mechanic(0, "Mikael Berg", "070-5553333", "seed.mechanic.diagnostics.specialization");
            mechanicRepository.save(mikael);

            LocalDate today = LocalDate.now();

            Booking firstBooking = new Booking(0, volvo.getId(), today, "seed.booking.oil_change_filter.description");
            bookingRepository.save(firstBooking);

            Booking secondBooking = new Booking(0, volkswagen.getId(), today, "seed.booking.front_brake_inspection.description");
            bookingRepository.save(secondBooking);

            Booking thirdBooking = new Booking(0, toyota.getId(), today.plusDays(1), "seed.booking.full_brake_overhaul.description");
            bookingRepository.save(thirdBooking);

            Booking fourthBooking = new Booking(0, bmw.getId(), today, "seed.booking.brake_calipers_pads.description");
            bookingRepository.save(fourthBooking);

            Booking fifthBooking = new Booking(0, audi.getId(), today, "seed.booking.obd2_fault_codes.description");
            bookingRepository.save(fifthBooking);

            Booking sixthBooking = new Booking(0, mercedes.getId(), today.plusDays(1), "seed.booking.electronic_fault_diagnosis.description");
            bookingRepository.save(sixthBooking);

            WorkOrder firstOrder = new WorkOrder(0, firstBooking.getId(), johan.getId());
            firstOrder.addServiceItem(oilChange.getId());
            firstOrder.setStatus("IN_PROGRESS");
            workOrderRepository.save(firstOrder);

            WorkOrder secondOrder = new WorkOrder(0, secondBooking.getId(), johan.getId());
            secondOrder.addServiceItem(brakeService.getId());
            workOrderRepository.save(secondOrder);

            WorkOrder thirdOrder = new WorkOrder(0, fourthBooking.getId(), sara.getId());
            thirdOrder.addServiceItem(brakeService.getId());
            thirdOrder.setStatus("IN_PROGRESS");
            workOrderRepository.save(thirdOrder);

            WorkOrder fourthOrder = new WorkOrder(0, fifthBooking.getId(), mikael.getId());
            fourthOrder.addServiceItem(diagnostics.getId());
            fourthOrder.setStatus("IN_PROGRESS");
            workOrderRepository.save(fourthOrder);

            WorkOrder fifthOrder = new WorkOrder(0, thirdBooking.getId(), johan.getId());
            fifthOrder.addServiceItem(brakeService.getId());
            fifthOrder.addServiceItem(annualService.getId());
            workOrderRepository.save(fifthOrder);

            WorkOrder sixthOrder = new WorkOrder(0, sixthBooking.getId(), mikael.getId());
            sixthOrder.addServiceItem(diagnostics.getId());
            workOrderRepository.save(sixthOrder);
        } catch (SQLException e) {
            System.out.println("Could not seed sample data: " + e.getMessage());
        }
    }
}
