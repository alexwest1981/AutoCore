package com.wac.autocore.exception;

/** A value that cannot be stored. Carries the text key, not the finished sentence. */
public class ValidationException extends AppException {

    private final String keySuffix;

    public ValidationException(String keySuffix) {
        super(keySuffix);
        this.keySuffix = keySuffix;
    }

    /** The full i18n key, for example dialog.validation.phone_ten_digits. */
    public String getMessageKey() {
        return "dialog.validation." + keySuffix;
    }

    public String getKeySuffix() {
        return keySuffix;
    }
}
