package com.wac.autocore.exception;

/** The signed-in person may not do this, and the guard said no before anything was written. */
public class AccessDeniedException extends AppException {

    public AccessDeniedException(String message) {
        super(message);
    }

    @Override
    public String getMessageKey() {
        return "dialog.access.denied";
    }
}
