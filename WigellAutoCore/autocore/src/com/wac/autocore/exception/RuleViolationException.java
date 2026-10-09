package com.wac.autocore.exception;

/** The action is not allowed in the state the row is in. The message names the rule and the state. */
public class RuleViolationException extends AppException {

    public RuleViolationException(String message) {
        super(message);
    }
}
