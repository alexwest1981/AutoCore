package com.wac.autocore.model;

/** Reglerna för namn, telefon och e-post, delade mellan kund och mekaniker. */
public final class ContactRules {

    /** Returned by {@link #problemWith} and appended to the i18n key {@code dialog.validation.}. */
    public static final String PROBLEM_REQUIRED = "required";
    public static final String PROBLEM_NAME_DIGITS = "name_digits";
    public static final String PROBLEM_PHONE_TEN_DIGITS = "phone_ten_digits";
    public static final String PROBLEM_EMAIL_FORMAT = "email_format";

    private ContactRules() {}

/** Första regeln kontakten bryter, eller null när raden går att spara. E-post är frivillig. */
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

/** Numret kontrollerat för sig. En mekaniker får ha siffror i namnet, inte i numret. */
    public static String phoneProblem(String phone) {
        if (isBlank(phone)) {
            return PROBLEM_REQUIRED;
        }
        if (!digitsOf(phone).matches("\\d{10}")) {
            return PROBLEM_PHONE_TEN_DIGITS;
        }
        return null;
    }

/** Gör om ett svenskt nummer till 070-1234 56 78, oavsett avskiljarna som skrevs. */
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
