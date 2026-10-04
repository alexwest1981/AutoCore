package com.wac.autocore.model;

/**
 * Reglerna för namn, telefonnummer och e-postadress på en kontakt — kund eller mekaniker.
 *
 * De ligger här och inte i respektive formulär: kundvyn, mekanikervyn, konsolversionen och
 * FXML-kontrollerna är alla skrivare av samma slags rad, och en regel som bara finns i ett av
 * formulären går de andra förbi.
 */
public final class ContactRules {

    /** Returned by {@link #problemWith} and appended to the i18n key {@code dialog.validation.}. */
    public static final String PROBLEM_REQUIRED = "required";
    public static final String PROBLEM_NAME_DIGITS = "name_digits";
    public static final String PROBLEM_PHONE_TEN_DIGITS = "phone_ten_digits";
    public static final String PROBLEM_EMAIL_FORMAT = "email_format";

    private ContactRules() {}

    /**
     * Den första regeln en kontakt bryter, som suffix i {@code dialog.validation.<suffix>}, eller null
     * när raden går att spara. E-postadressen är frivillig men måste vara en adress om den fylls i.
     */
    public static String problemWith(String name, String phone, String email) {
        if (isBlank(name)) {
            return PROBLEM_REQUIRED;
        }
        if (name.matches(".*\\d.*")) {
            return PROBLEM_NAME_DIGITS;
        }
        String phoneProblem = phoneProblem(phone);
        if (phoneProblem != null) {
            return phoneProblem;
        }
        if (!isBlank(email) && !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            return PROBLEM_EMAIL_FORMAT;
        }
        return null;
    }

    /**
     * Telefonnumret kontrollerat för sig: en mekaniker får ha siffror i namnet — repots egna rader och
     * demodata använder namn som "D2-mekaniker" — men numret lyder under samma regel som kundens.
     */
    public static String phoneProblem(String phone) {
        if (isBlank(phone)) {
            return PROBLEM_REQUIRED;
        }
        if (!digitsOf(phone).matches("\\d{10}")) {
            return PROBLEM_PHONE_TEN_DIGITS;
        }
        return null;
    }

    /**
     * Ett svenskt nummer skrivet med tio siffror, med de avskiljare användaren råkade skriva borttagna:
     * "070 12 34 567" och "0701234567" blir båda "070-1234567". Allt som inte är tio siffror (utländska
     * nummer, äldre rader) lämnas orört, så en rad som skrevs innan regeln fanns förblir läsbar i stället
     * för att bli "-".
     * ponytail: en 3-7-delning, räcker för svensk mobil och fast telefon; gör delningen tabelldriven om
     * verkstaden någon gång sparar utländska nummer.
     */
    public static String normalizePhone(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = digitsOf(raw);
        if (!digits.matches("\\d{10}")) {
            return raw;
        }
        return digits.substring(0, 3) + "-" + digits.substring(3);
    }

    private static String digitsOf(String number) {
        return number.trim().replace(" ", "").replace("-", "");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
