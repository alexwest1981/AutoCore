package com.wac.autocore.model;

public class Customer {

    private int id;
    private String name;
    private String phone;
    private String email;
    private boolean vip;

    public Customer(int id, String name, String phone, String email) {
        this.id = id;
        this.name = name;
        this.phone = normalizePhone(phone);
        this.email = email;
        this.vip = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = normalizePhone(phone);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isVip() {
        return vip;
    }

    public void setVip(boolean vip) {
        this.vip = vip;
    }

    /** Returned by {@link #validationProblem} and appended to the i18n key {@code dialog.validation.}. */
    public static final String PROBLEM_REQUIRED = "required";
    public static final String PROBLEM_NAME_DIGITS = "name_digits";
    public static final String PROBLEM_PHONE_TEN_DIGITS = "phone_ten_digits";
    public static final String PROBLEM_EMAIL_FORMAT = "email_format";

    /**
     * The first rule a customer record breaks, as a suffix of {@code dialog.validation.<suffix>},
     * or null when the record is storable. Lives here and not in the form, because the form, the
     * console and the FXML controller are three writers of the same row: a rule kept in one of them
     * is a rule the other two walk straight past.
     */
    public static String validationProblem(String name, String phone, String email) {
        if (isBlank(name) || isBlank(phone)) {
            return PROBLEM_REQUIRED;
        }
        if (name.matches(".*\\d.*")) {
            return PROBLEM_NAME_DIGITS;
        }
        if (!digitsOf(phone).matches("\\d{10}")) {
            return PROBLEM_PHONE_TEN_DIGITS;
        }
        if (!isBlank(email) && !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            return PROBLEM_EMAIL_FORMAT;
        }
        return null;
    }

    /**
     * A Swedish number written as 10 digits, with the separators the user happened to type removed:
     * "070 12 34 567" and "0701234567" both become "070-1234567". Anything that is not 10 digits
     * (a foreign number, a landline with an area code of another shape) is returned untouched, so a
     * row written before the rule existed stays readable instead of turning into "-".
     * ponytail: one 3-7 split, enough for Swedish mobile and landline; make the split table-driven
     * if the workshop ever stores foreign numbers.
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

    @Override
    public String toString() {
        return id + " - " + name +
                " | Phone: " + phone +
                " | Email: " + email +
                " | VIP: " + (vip ? "Yes" : "No");
    }
}