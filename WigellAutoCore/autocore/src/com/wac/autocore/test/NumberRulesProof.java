package com.wac.autocore.test;

import com.wac.autocore.data.Db;
import com.wac.autocore.exception.AppException;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.service.DiscountRules;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The numeric rules: which values are accepted, and how amounts are rounded and spread. */
public final class NumberRulesProof {

    private NumberRulesProof() {}

    public static void main(String[] args) {
        Db.use(Db.pathFrom(args));
        Db.ensureReady();
        I18n.setLanguage("sv");

        GarageSystem garage = new GarageSystem(new SqliteRepositoryFactory());
        int customerId = Integer.parseInt(args[1]);
        int[][ ] years = {{1899, 0}, {1900, 1}, {2100, 1}, {2101, 0}};
        boolean yearsRight = true;
        for (int[] pair : years) {
            boolean accepted = accepted(garage, "NR" + pair[0], customerId, pair[0]);
            boolean expected = pair[1] == 1;
            System.out.println("år " + pair[0] + ": " + (accepted ? "går igenom" : "nekas")
                    + (accepted == expected ? "" : "   FEL, väntat " + (expected ? "gå igenom" : "nekas")));
            yearsRight = yearsRight && accepted == expected;
        }
        System.out.println("ett år som inte är en siffra: "
                + (Vehicle.isReadableYear("abc") ? "FEL, går igenom" : "nekas"));

        boolean tenth = DiscountRules.percentOf(999, 10) == 100;
        boolean none = DiscountRules.percentOf(2500, 0) == 0;
        boolean whole = DiscountRules.percentOf(2500, 100) == 2500;
        System.out.println();
        System.out.println("tio procent av 999 öre blir 100:  " + tenth);
        System.out.println("noll procent blir noll:           " + none);
        System.out.println("hundra procent blir hela beloppet: " + whole);

        List<InvoiceLine> lines = new ArrayList<InvoiceLine>(Arrays.asList(
                new InvoiceLine(0, 1, 1, "Bromsar", 700, 0),
                new InvoiceLine(0, 1, 1, "Olja", 300, 0)));
        DiscountRules.distributeDiscount(lines, 1000, 100);
        int spread = lines.get(0).getDiscount() + lines.get(1).getDiscount();
        boolean spreadRight = spread == 100 && lines.get(0).getDiscount() == 70;
        System.out.println("hundra öre fördelade på 700 och 300: " + lines.get(0).getDiscount()
                + " + " + lines.get(1).getDiscount() + " = " + spread);

        System.out.println();
        System.out.println("årtalen följer regeln:                  " + yearsRight);
        System.out.println("avrundningen följer regeln:              " + (tenth && none && whole));
        System.out.println("rabatten fördelas utan att tappa ören:   " + spreadRight);
        System.exit(yearsRight && tenth && none && whole && spreadRight ? 0 : 1);
    }

    /** True when the service stores the vehicle, false when it refuses it. */
    private static boolean accepted(GarageSystem garage, String registrationNumber, int customerId, int year) {
        try {
            garage.createVehicle(registrationNumber, "Volvo", "V70", year, customerId);
            return true;
        } catch (AppException refused) {
            return false;
        }
    }
}
