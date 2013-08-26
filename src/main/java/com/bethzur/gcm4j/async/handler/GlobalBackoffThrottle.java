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
package com.bethzur.gcm4j.async.handler;

import java.util.HashSet;
import java.util.Set;

import com.bethzur.gcm4j.Message;
import com.bethzur.gcm4j.MulticastResult;
import com.bethzur.gcm4j.Response;
import com.bethzur.gcm4j.ResponseType;
import com.bethzur.gcm4j.backoff.Attempt;
import com.bethzur.gcm4j.backoff.Backoff;

/**
 * A message filter and response handler that implements global retry with
 * backoff for {@link ResponseType#QuotaExceeded QuotaExceeded} and
 * {@link ResponseType#ServiceUnavailable ServiceUnavailable} responses and
 * delays messages to respect {@code Retry-After} headers.
 * 
 * @author David R. Bild
 * 
 */
public class GlobalBackoffThrottle implements MessageFilter,
        ResponseHandler<Response> {
    static final String CONTEXT_KEY = GlobalBackoffThrottle.class
            .getCanonicalName();

    private static final Set<ResponseType.ServerResponse> SupportedTypes = new HashSet<ResponseType.ServerResponse>();

    private final Backoff backoff;

    private long nextRetryTime;

    {
        SupportedTypes.add(ResponseType.ServerResponse.ServiceUnavailable);
        SupportedTypes.add(ResponseType.ServerResponse.Success);
    }

    /**
     * Constructs a new throttle using the provided {@code Backoff} instance for
     * backoff.
     * 
     * @param backoff
     *            the backoff object
     */
    public GlobalBackoffThrottle(Backoff backoff) {
        this.backoff = backoff;
    }

    /**
     * Constructs a new throttle using the provided {@code Backoff} instance for
     * backoff and registers the underlying filters and handlers with the
     * provider {@link AsyncHandlers} instance.
     * 
     * @param backoff
     *            the backoff object
     * @param handlers
     *            the handlers object with which to register the filters and
     *            handlers
     */
    public GlobalBackoffThrottle(Backoff backoff, AsyncHandlers handlers) {
        this(backoff);
        register(handlers);
    }

    private void register(AsyncHandlers handlers) {
        handlers.appendEnqueueFilter(this);
        handlers.appendDequeueFilter(this);
        handlers.appendResponseHandler(this);
    }

    // ------------------------- Filter Messages ------------------------------
    @Override
    public boolean support(Response response) {
        return true;
    }

    @Override
    public void enqueueFilter(Context<Message, MessageDecision> context) {
        updateDelay(context);
    }

    @Override
    public void dequeueFilter(Context<Message, MessageDecision> context) {
        updateDelay(context);
    }

    private Attempt createAttempt(Context<Message, MessageDecision> context) {
        Attempt attempt = backoff.begin();
        context.put(CONTEXT_KEY, attempt);
        return attempt;
    }

    private void updateDelay(Context<Message, MessageDecision> context) {
        switch (context.getDecision()) {
        case SEND:
            Attempt attempt = createAttempt(context);
            long delay = Math.max(attempt.delay(), retryDelay());
            if (delay > context.getDelay())
                context.setDelay(delay);
            return;
        default:
            return;
        }
    }

    private long retryDelay() {
        long delay = nextRetryTime - System.currentTimeMillis();
        return Math.max(0, delay);
    }

    // ------------------------- Handle Responses -----------------------------
    @Override
    public boolean support(Context<Response, ResultDecision> context) {
        Response response = context.unwrap();
        ResponseType.ServerResponse serverResponse = response
                .getServerResponse();
        return SupportedTypes.contains(serverResponse);
    }

    @Override
    public void handleResponse(Context<Response, ResultDecision> context) {
        Response response = context.unwrap();
        MulticastResult result = response.getResult();
        switch (response.getServerResponse()) {
        case ServiceUnavailable:
            retrieveAttempt(context).recordFailure();
            updateRetryAfter(response);
            context.setDecision(ResultDecision.RETRY);
            return;
        case Success:
            if (result.getFailure() > 0) {
                retrieveAttempt(context).recordFailure();
                // TODO: modify message registration ids
                context.setDecision(ResultDecision.RETRY);
            } else {
                retrieveAttempt(context).recordSuccess();
            }
            return;
        default:
            return;
        }
    }

    private Attempt retrieveAttempt(Context<Response, ResultDecision> context) {
        return context.get(CONTEXT_KEY, Attempt.class);
    }

    private void updateRetryAfter(Response response) {
        nextRetryTime = response.retryAfter().getTime();
    }

}
