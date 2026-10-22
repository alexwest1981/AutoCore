package com.wac.autocore.model;

/**
 * The workshop's own details, as they stand on the invoice: who sold the work, where, and under
 * which organisation and VAT numbers. Kept in the settings so the workshop can change them without
 * a new build, and read whenever a document is printed.
 */
public record Seller(String name, String street, String postalCode, String city,
                     String phone, String email, String orgNumber, String vatNumber,
                     int paymentTermsDays) {

    public static final String KEY_NAME = "seller.name";
    public static final String KEY_STREET = "seller.street";
    public static final String KEY_POSTAL_CODE = "seller.postal_code";
    public static final String KEY_CITY = "seller.city";
    public static final String KEY_PHONE = "seller.phone";
    public static final String KEY_EMAIL = "seller.email";
    public static final String KEY_ORG_NUMBER = "seller.org_number";
    public static final String KEY_VAT_NUMBER = "seller.vat_number";
    public static final String KEY_PAYMENT_TERMS_DAYS = "seller.payment_terms_days";

    /**
     * What a fresh installation starts with: the workshop the demo was built for. The organisation
     * and VAT numbers are placeholders and must be replaced with the real ones before an invoice
     * leaves the shop, which is what the company details dialog is for.
     */
    public static Seller defaults() {
        return new Seller("Wigell AutoCore", "Verkstadsvägen 1", "512 00", "Svenljunga",
                "070-000 00 00", "info@wigellautocore.se", "556000-0000", "SE556000000001", 30);
    }

    /** Postcode and city on one line, or empty when neither is filled in. */
    public String postalAndCity() {
        String postal = postalCode == null ? "" : postalCode.trim();
        String town = city == null ? "" : city.trim();
        if (postal.isEmpty()) {
            return town;
        }
        return town.isEmpty() ? postal : postal + " " + town;
    }
}
