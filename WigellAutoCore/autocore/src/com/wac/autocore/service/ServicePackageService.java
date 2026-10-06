package com.wac.autocore.service;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.repository.ServicePackageRepository;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class ServicePackageService {

    private final ServicePackageRepository packageRepository = new ServicePackageRepository();

    public List<ServicePackage> getAll() {
        try {
            return packageRepository.findAll();
        } catch (SQLException e) {
            System.out.println("Could not read service packages: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public ServicePackage findById(int id) {
        try {
            return packageRepository.findById(id);
        } catch (SQLException e) {
            System.out.println("Could not read service package " + id + ": " + e.getMessage());
            return null;
        }
    }

    public ServicePackage createPackage(String name, String description, List<ServiceItem> serviceItems) {
        refuseUnlessValid(name, serviceItems);

        ServicePackage servicePackage = new ServicePackage(0, name.trim(), description);
        servicePackage.setServiceItems(serviceItems);

        try {
            packageRepository.save(servicePackage);
        } catch (SQLException e) {
            System.out.println("Could not save service package: " + e.getMessage());
            return null;
        }
        return servicePackage;
    }

    public void updatePackage(ServicePackage servicePackage) throws SQLException {
        if (servicePackage == null) {
            return;
        }
        refuseUnlessValid(servicePackage.getName(), servicePackage.getServiceItems());
        packageRepository.save(servicePackage);
    }

    public void deletePackage(int id) throws SQLException {
        packageRepository.delete(id);
    }

    private static void refuseUnlessValid(String name, List<ServiceItem> serviceItems) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Ett servicepaket måste ha ett namn.");
        }
        if (serviceItems == null || serviceItems.isEmpty()) {
            throw new IllegalArgumentException("Ett servicepaket måste innehålla minst en tjänst.");
        }
    }
}
