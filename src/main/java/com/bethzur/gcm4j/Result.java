package com.bethzur.gcm4j;

public interface Result {
    /**
     * Gets the message id, if any.
     */
    public String getMessageId();

    /**
     * Gets the canonical registration id, if any.
     */
    public String getCanonicalRegistrationId();

    /**
     * Gets the error code, if any.
     */
    public ResponseType.IndividualResponse getErrorCodeName();
}
