package com.wac.autocore.service;

import com.wac.autocore.model.InvoiceLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** The discount rules for an invoice. */
public class DiscountRules {

    /** The VIP customer's discount as a percentage of the whole amount. */
    public static final int VIP_PERCENT = 10;

    /** A percentage of an amount in öre, rounded to the nearest öre, half upward. */
    public static int percentOf(int amount, int percent) {
        return (amount * percent + 50) / 100;
    }

    /** The discount in öre for a campaign code: fixed amount, percentage or one of the named ones. */
    public static int forCode(String discountCode, int amount) {
        if (discountCode == null || discountCode.trim().isEmpty()) {
            return 0;
        }

        String code = discountCode.trim();

        if (code.equalsIgnoreCase("WELCOME10") || code.equalsIgnoreCase("10off") || code.equalsIgnoreCase("10%")) {
            return percentOf(amount, 10);
        }
        if (code.equalsIgnoreCase("SERVICE200")) {
            return 20000;
        }

        try {
            if (code.endsWith("%")) {
                int pct = Integer.parseInt(code.substring(0, code.length() - 1).trim());
                return percentOf(amount, pct);
            } else if (code.toLowerCase().endsWith("off")) {
                int val = Integer.parseInt(code.substring(0, code.length() - 3).trim());
                if (val <= 100 && val > 0) {
                    return percentOf(amount, val);
                }
                return val * 100;
            } else {
                int val = toOre(code);
                if (val > 0) {
                    return val;
                }
            }
        } catch (NumberFormatException | ArithmeticException ignored) {
        }

        return 0;
    }

    /** A typed amount as öre. This is the one place a written amount becomes a number. */
    private static int toOre(String text) {
        return new BigDecimal(text).setScale(2, RoundingMode.HALF_UP).movePointRight(2).intValueExact();
    }

    /** Spreads the discount in proportion to the line price. The remainder goes on the dearest line. */
    public static void distributeDiscount(List<InvoiceLine> lines, int amount, int discount) {
        if (lines.isEmpty() || amount <= 0 || discount <= 0) {
            return;
        }

        int distributed = 0;
        InvoiceLine mostExpensive = lines.get(0);
        for (InvoiceLine line : lines) {
            // The share is whole öre, and the odd öre or two are laid on the dearest line below.
            int share = (int) ((long) discount * line.getPrice() / amount);
            line.setDiscount(share);
            distributed += share;
            if (line.getPrice() > mostExpensive.getPrice()) {
                mostExpensive = line;
            }
        }

        int rest = discount - distributed;
        mostExpensive.setDiscount(mostExpensive.getDiscount() + rest);
    }
}
