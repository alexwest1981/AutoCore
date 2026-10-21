package com.wac.autocore.exception;

/** Nobody is signed in, which is not the same as being signed in without the right. */
public class NotAuthenticatedException extends AppException {

    public NotAuthenticatedException(String message) {
        super(message);
    }

    @Override
    public String getMessageKey() {
        return "dialog.access.not_signed_in";
    }
}