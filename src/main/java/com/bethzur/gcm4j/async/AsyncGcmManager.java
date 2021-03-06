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

import java.util.concurrent.Future;

import com.bethzur.gcm4j.Message;
import com.bethzur.gcm4j.Response;
import com.bethzur.gcm4j.async.handler.AsyncHandlers;
import com.bethzur.gcm4j.async.handler.AsyncHandlersFactory;

/**
 * An interface for asynchronously pushing messages to clients via the GCM
 * service. Configuration details (e.g., providing authentication tokens) are
 * implementation dependent. Implementations should support automated handling
 * of GCM responses (e.g., exponential backoff and retry on quota exceeded or
 * service unavailable errors) via {@link AsyncHandlers}.
 * <p>
 * A default implementation can be instantiated via
 * {@link AsyncGcmManagerFactory}.
 *
 * @see AsyncGcmManagerFactory
 * @see AsyncHandlers
 * @see AsyncHandlersFactory
 *
 * @author David R. Bild
 *
 */
public interface AsyncGcmManager {

	/**
	 * Queues a message to be sent to the GCM service for delivery to the
	 * client specified in the message header. The response or error is
	 * accessible via the returned {@link Future} when available.
	 *
	 * @param msg
	 *            the message to deliver
	 * @return a future for accessing the response from the GCM service or an
	 *         exception
	 */
	public Future<Response> pushMessage(Message msg);

}
