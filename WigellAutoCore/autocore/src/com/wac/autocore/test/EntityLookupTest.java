package com.wac.autocore.test;

import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.util.EntityLookup;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EntityLookupTest {

    public void testCustomerNameLookup() {
        GarageSystem garage = new GarageSystem();
        List<Customer> customers = garage.getCustomers();
        if (!customers.isEmpty()) {
            Customer first = customers.get(0);
            String name = EntityLookup.customerName(garage, first.getId());
            TestRunner.assertEquals(first.getName(), name, "Match existing customer name");
        }
        String fallback = EntityLookup.customerName(garage, 999999);
        TestRunner.assertEquals("Customer #999999", fallback, "Fallback for non-existent customer");
    }

    public void testVehicleRegLookup() {
        GarageSystem garage = new GarageSystem();
        List<Vehicle> vehicles = garage.getVehicles();
        if (!vehicles.isEmpty()) {
            Vehicle first = vehicles.get(0);
            String reg = EntityLookup.vehicleReg(garage, first.getId());
            TestRunner.assertEquals(first.getRegistrationNumber(), reg, "Match existing vehicle registration");
        }
        String fallback = EntityLookup.vehicleReg(garage, 888888);
        TestRunner.assertEquals("Vehicle #888888", fallback, "Fallback for non-existent vehicle");
    }

    public void testMechanicNameLookup() {
        GarageSystem garage = new GarageSystem();
        List<Mechanic> mechanics = garage.getMechanics();
        if (!mechanics.isEmpty()) {
            Mechanic first = mechanics.get(0);
            String name = EntityLookup.mechanicName(garage, first.getId());
            TestRunner.assertEquals(first.getName(), name, "Match existing mechanic name");
        }
        String fallback = EntityLookup.mechanicName(garage, 777777);
        TestRunner.assertEquals("Mechanic #777777", fallback, "Fallback for non-existent mechanic");
    }

    public void testServiceNamesLookup() {
        GarageSystem garage = new GarageSystem();
        List<ServiceItem> services = garage.getServiceItems();
        if (services.size() >= 2) {
            ServiceItem s1 = services.get(0);
            ServiceItem s2 = services.get(1);
            String joined = EntityLookup.serviceNames(garage, Arrays.asList(s1.getId(), s2.getId()));
            TestRunner.assertTrue(joined.contains(s1.getName()), "Contains service 1");
            TestRunner.assertTrue(joined.contains(s2.getName()), "Contains service 2");
        }
        TestRunner.assertEquals("", EntityLookup.serviceNames(garage, Collections.emptyList()), "Empty list returns empty");
        TestRunner.assertEquals("", EntityLookup.serviceNames(garage, null), "Null list returns empty");
    }
}
