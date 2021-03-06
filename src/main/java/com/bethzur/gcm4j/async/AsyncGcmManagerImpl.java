/*
 * Copyright 2012 The Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bethzur.gcm4j.async;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bethzur.gcm4j.GcmManager;
import com.bethzur.gcm4j.Message;
import com.bethzur.gcm4j.Response;
import com.bethzur.gcm4j.async.handler.AsyncHandlers;
import com.bethzur.gcm4j.async.handler.MessageFilter;
import com.bethzur.gcm4j.async.handler.ResponseHandler;
import com.bethzur.gcm4j.async.handler.ThrowableHandler;

/**
 * Default implementation of {@link AsyncGcmManager}. Instances can be created
 * using the factory methods in {@link AsyncGcmManagerFactory}.
 * <p>
 * Internally, each message is wrapped in a {@link MessageContext} linking to
 * the {@link Future}, {@link Message}, and context available to any registered
 * {@link MessageFilter}, {@link ResponseHandler}, or {@link ThrowableHandler}.
 * <p>
 * Each message is passed through all registered
 * {@link MessageFilter#enqueueFilter(Context)} before being enqueued to await
 * sending and passed through all registered
 * {@link MessageFilter#dequeueFilter(Context)} when dequeued for sending. Any
 * filter may choose to cancel or delay the message. Responses are passed
 * through all registered {@link ResponseHandler#handleResponse(Context)} when
 * received. Similarly, exceptions are passed through all registered
 * {@link ThrowableHandler#handleThrowable(Context)} when thrown. Each handler
 * may choose to return the response/throwable via the {@link Future} or retry
 * the message (with delay).
 *
 * @author David R. Bild
 *
 */
class AsyncGcmManagerImpl implements AsyncGcmManager {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(AsyncGcmManagerImpl.class);

	private ScheduledExecutorService executor;

	private AsyncHandlers handlers;

	private GcmManager gcm;

	/**
	 * Constructs a new asynchronous manager.
	 * <p>
	 * The {@code Manager} must be thread-safe up to the number of
	 * concurrent threads backing the executor.
	 *
	 * @param Manager
	 *            the synchronous manager for communicating with the GCM
	 *            service
	 * @param handlers
	 *            the handlers for automatically handling responses and
	 *            exceptions
	 * @param executor
	 *            the executor providing the background threads to deliver
	 *            queued messages
	 */
	public AsyncGcmManagerImpl(GcmManager gcmManager,
			AsyncHandlers handlers, ScheduledExecutorService executor) {
		this.gcm = gcmManager;
		this.executor = executor;
		this.handlers = handlers;
	}

	@Override
	public FutureResponse pushMessage(Message msg) {
		FutureResponse future = new FutureResponse();
		submit(new MessageContext(future, msg));
		return future;
	}

	private void submit(MessageContext context) {
		handlers.filterMessageEnqueue(context);
		switch (context.getDecision()) {
		case SEND:
			executor.schedule(new MessageRunnable(context), context.getDelay(),
					TimeUnit.MILLISECONDS);
			return;
		case CANCEL:
			context.getFutureResponse().setCancelled();
			return;
		}
	}

	private void process(MessageContext context) {
		context.setDelay(0);
		handlers.filterMessageDequeue(context);
		switch (context.getDecision()) {
		case SEND:
			if (context.getDelay() > 0) {
				submit(context);
			} else {
				issue(context);
			}
			return;
		case CANCEL:
			context.getFutureResponse().setCancelled();
			return;
		}
	}

	private void issue(MessageContext context) {
		try {
			Response response = gcm.pushMessage(context.unwrap());
			handleResponse(context, response);
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable throwable) {
			handleThrowable(context, throwable);
		}
	}

	private void handleResponse(MessageContext messageContext, Response response) {
		ResponseContext context = new ResponseContext(messageContext, response);
		handlers.handleResponse(context);
		switch (context.getDecision()) {
		case RETURN:
			context.getFutureResponse().setResponse(response);
			return;
		case RETRY:
			submit(new MessageContext(context, messageContext.unwrap()));
			return;
		}
	}

	private void handleThrowable(MessageContext messageContext,
			Throwable throwable) {
		ThrowableContext context = new ThrowableContext(messageContext,
				throwable);
		handlers.handleThrowable(context);
		switch (context.getDecision()) {
		case RETURN:
			context.getFutureResponse().setThrowable(throwable);
			return;
		case RETRY:
			submit(new MessageContext(context, messageContext.unwrap()));
			return;
		}
	}

	private class MessageRunnable implements Runnable {

		private final MessageContext context;

		public MessageRunnable(MessageContext context) {
			this.context = context;
		}

		@Override
		public void run() {
			try {
				AsyncGcmManagerImpl.this.process(context);
			} catch (RuntimeException e) {
				LOGGER.warn(
						"Unexpected RuntimeException while processing GCM message.",
						e);
				try {
					context.getFutureResponse().setThrowable(e);
				} catch (RuntimeException e2) {
					LOGGER.warn(
							"Unable to return RuntimeException via Future.  Dropping exception.",
							e2);
				}
			}
		}

	}

}
