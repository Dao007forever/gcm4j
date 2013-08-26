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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.bethzur.gcm4j.Message;
import com.bethzur.gcm4j.Response;

/**
 * Implementation of {@code AsyncHandlers}. Can be instantiated using the
 * factory methods in {@link AsyncHandlersFactory}.
 * 
 * @author David R. Bild
 * 
 */
class AsyncHandlersImpl implements AsyncHandlers {
    private final CopyOnWriteArrayList<ResponseHandler<? extends Response>> responseHandlers;

    private final CopyOnWriteArrayList<ThrowableHandler<? extends Throwable>> throwableHandlers;

    private final List<MessageFilter> enqueueFilters;

    private final List<MessageFilter> dequeueFilters;

    /**
     * Constructs a new intance.
     */
    public AsyncHandlersImpl() {
        responseHandlers = new CopyOnWriteArrayList<ResponseHandler<? extends Response>>();
        throwableHandlers = new CopyOnWriteArrayList<ThrowableHandler<? extends Throwable>>();
        enqueueFilters = new CopyOnWriteArrayList<MessageFilter>();
        dequeueFilters = new CopyOnWriteArrayList<MessageFilter>();
    }

    @Override
    public void appendEnqueueFilter(MessageFilter filter) {
        enqueueFilters.add(filter);
    }

    @Override
    public void appendDequeueFilter(MessageFilter filter) {
        dequeueFilters.add(filter);
    }

    @Override
    public <T extends Response> void appendResponseHandler(
            ResponseHandler<T> handler) {
        responseHandlers.add(handler);
    }

    @Override
    public <T extends Throwable> void appendThrowableHandler(
            ThrowableHandler<T> handler) {
        throwableHandlers.add(handler);
    }

    @Override
    public void filterMessageEnqueue(
            Context<Message, MessageDecision> messageContext) {
        for (MessageFilter f : enqueueFilters) {
            f.enqueueFilter(messageContext);
        }
    }

    @Override
    public void filterMessageDequeue(
            Context<Message, MessageDecision> messageContext) {
        for (MessageFilter f : dequeueFilters) {
            f.dequeueFilter(messageContext);
        }
    }

    // Type safety ensured by only adding elements to the container via the
    // type-parameterized appendResponseHandler() method.
    @SuppressWarnings("unchecked")
    @Override
    public <R extends Response> void handleResponse(
            Context<R, ResultDecision> responseContext) {
        for (ResponseHandler<? extends Response> h : responseHandlers) {
            ResponseHandler<R> handler = (ResponseHandler<R>) h;
            if (handler.support(responseContext)) {
                handler.handleResponse(responseContext);
            }
        }
    }

    // Type safety ensured by only adding elements to the container via the
    // type-parameterized appendThrowableHandler() method.
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Throwable> void handleThrowable(
            Context<T, ResultDecision> throwableContext) {
        for (ThrowableHandler<? extends Throwable> h : throwableHandlers) {
            ((ThrowableHandler<T>) h).handleThrowable(throwableContext);
        }
    }
}
