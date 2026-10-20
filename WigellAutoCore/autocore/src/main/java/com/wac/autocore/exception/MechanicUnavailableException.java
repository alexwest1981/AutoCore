package com.wac.autocore.exception;

/** The mechanic is already booked for that time. The message names the slot for the reader. */
public class MechanicUnavailableException extends AppException {

    public MechanicUnavailableException(String message) {
        super(message);
    }
}
