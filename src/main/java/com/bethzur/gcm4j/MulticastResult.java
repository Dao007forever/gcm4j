package com.bethzur.gcm4j;

import java.util.List;

public interface MulticastResult {
    /**
     * Gets the multicast id.
     */
    public long getMulticastId();

    /**
     * Gets the number of successful messages.
     */
    public int getSuccess();

    /**
     * Gets the number of failed messages.
     */
    public int getFailure();

    /**
     * Gets the number of successful messages that also returned a canonical
     * registration id.
     */
    public int getCanonicalIds();

    /**
     * Gets the results of each individual message, which is immutable.
     */
    public List<Result> getResults();
}
