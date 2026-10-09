package com.wac.autocore.exception;

/**
 * A filled-in value cannot be stored. It carries the key suffix instead of finished text, so
 * whoever catches it can show the sentence in the language the user picked.
 */
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
