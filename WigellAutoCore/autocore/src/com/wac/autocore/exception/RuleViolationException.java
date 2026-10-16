package com.wac.autocore.exception;

/** The action is not allowed in the state the row is in. The message names the rule and the state. */
public class RuleViolationException extends AppException {

    private final String messageKey;

    public RuleViolationException(String message) {
        this(message, null);
    }

    public RuleViolationException(String message, String messageKey) {
        super(message);
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
