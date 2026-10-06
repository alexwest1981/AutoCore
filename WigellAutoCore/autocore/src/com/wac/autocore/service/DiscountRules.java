package com.wac.autocore.service;

import com.wac.autocore.model.InvoiceLine;

import java.util.List;

/** Rabattreglerna för en faktura. */
public class DiscountRules {

    /** VIP-kundens rabatt i procent av hela beloppet. */
    public static final double VIP_PERCENT = 10.0;

/** Rabatten i kronor för en kampanjkod: fast belopp, procent eller ett av de namngivna. */
    public static double forCode(String discountCode, double amount) {
        if (discountCode == null || discountCode.trim().isEmpty()) {
            return 0.0;
        }

        String code = discountCode.trim();

        if (code.equalsIgnoreCase("WELCOME10") || code.equalsIgnoreCase("10off") || code.equalsIgnoreCase("10%")) {
            System.out.println("Discount code " + code + " applied: 10%");
            return amount * 0.10;
        }
        if (code.equalsIgnoreCase("SERVICE200")) {
            System.out.println("Discount code SERVICE200 applied.");
            return 200.0;
        }

        try {
            if (code.endsWith("%")) {
                double pct = Double.parseDouble(code.substring(0, code.length() - 1).trim());
                System.out.println("Discount code " + pct + "% applied.");
                return amount * (pct / 100.0);
            } else if (code.toLowerCase().endsWith("off")) {
                double val = Double.parseDouble(code.substring(0, code.length() - 3).trim());
                System.out.println("Discount code " + code + " applied.");
                if (val <= 100 && val > 0) {
                    return amount * (val / 100.0);
                }
                return val;
            } else {
                double val = Double.parseDouble(code);
                if (val > 0) {
                    System.out.println("Discount " + val + " SEK applied.");
                    return val;
                }
            }
        } catch (NumberFormatException ignored) {
            System.out.println("Unknown discount code. No code discount applied.");
        }

        return 0.0;
    }

/** Fördelar rabatten i proportion till radpriset. Resten läggs på dyraste raden. */
    public static void distributeDiscount(List<InvoiceLine> lines, double amount, double discount) {
        if (lines.isEmpty() || amount <= 0 || discount <= 0) {
            return;
        }

        double distributed = 0.0;
        InvoiceLine mostExpensive = lines.get(0);
        for (InvoiceLine line : lines) {
            double share = roundToOre(discount * line.getPrice() / amount);
            line.setDiscount(share);
            distributed += share;
            if (line.getPrice() > mostExpensive.getPrice()) {
                mostExpensive = line;
            }
        }

        double rest = discount - distributed;
        mostExpensive.setDiscount(roundToOre(mostExpensive.getDiscount() + rest));
    }

    public static double roundToOre(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
