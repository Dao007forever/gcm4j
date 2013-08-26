/*
 * Copyright 2012 The Regents of the University of Michigan
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bethzur.gcm4j.impl;

import java.util.Date;

import com.bethzur.gcm4j.Message;
import com.bethzur.gcm4j.MulticastResult;
import com.bethzur.gcm4j.Response;
import com.bethzur.gcm4j.ResponseType;

/**
 * Implementation of {@link Response} used by {@link DefaultGcmManager}.
 * 
 * @author David R. Bild
 * 
 */
class ResponseImpl implements Response {

    private final ResponseType.ServerResponse serverResponse;

    private final MulticastResult result;

    private final Date retryAfter;

    private final Message message;

    /**
     * Constructs a new response for the specified type and associated message.
     * 
     * @param results
     *            response types
     * @param message
     *            message for which this is a response
     */
    public ResponseImpl(ResponseType.ServerResponse serverResponse,
            MulticastResult result, Date retryAfter, Message message) {
        super();
        this.serverResponse = serverResponse;
        this.result = result;
        this.retryAfter = retryAfter;
        this.message = validateMessage(message);
    }

    private Message validateMessage(Message message) {
        if (message == null)
            throw new IllegalArgumentException(
                    "Argument 'message' may not be null.");

        return message;
    }

    @Override
    public ResponseType.ServerResponse getServerResponse() {
        return serverResponse;
    }

    @Override
    public MulticastResult getResult() {
        return result;
    }

    @Override
    public Date retryAfter() {
        return retryAfter;
    }

    @Override
    public Message getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return String
                .format("DefaultResponseImpl(serverResponse=%s, result=%s, retryAfter=%s, messages=%s)",
                        this.getServerResponse(),
                        this.getResult(),
                        this.retryAfter(),
                        this.getMessage());
    }
}
