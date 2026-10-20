package com.wac.autocore.exception;

/** The registration number already belongs to another vehicle. The message names it for the reader. */
public class DuplicateRegistrationNumberException extends AppException {

    public DuplicateRegistrationNumberException(String message) {
        super(message);
    }
}
