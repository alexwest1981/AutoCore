package com.wac.autocore.exception;

/**
 * Nobody is signed in, and the operation needs someone who is. Kept apart from
 * {@link AccessDeniedException} on purpose: the order asks the system to tell "not signed in"
 * from "signed in without the right to do this", and the two are answered differently.
 */
public class NotAuthenticatedException extends AppException {

    public NotAuthenticatedException(String message) {
        super(message);
    }

    @Override
    public String getMessageKey() {
        return "dialog.access.sign_in";
    }
}
