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
package com.bethzur.gcm4j;

import java.util.Collection;
import java.util.Map;

/**
 * Encapsulates a request to the GCM service. The message includes both the
 * delivery metadata needed by GCM (e.g., client identifier, collapse key,
 * etc.) and the key-value data to be pushed to the client. Instances are
 * immutable and can be obtained using {@link MessageBuilder}.
 * <p>
 * Full descriptions of the message fields can be found on the <a
 * href="http://developer.android.com/guide/google/gcm/index.html">GCM web page</a>.
 *
 * @see MessageBuilder
 *
 * @author David R. Bild
 *
 */
public interface Message {

	/**
	 * Gets the identifiers for the clients to whom the message will be sent.
	 *
	 * @return the registration ids of the clients.
	 */
	public Collection<String> getRegistrationIds();

	/**
	 * Gets the collapse key for the message. The collapse key is used to
	 * collapse similar messages queued by the GCM server when a device is
	 * offline. Only the last message will be delivered when the device
	 * reconnects.
	 *
	 * @return the collapse key
	 */
	public String getCollapseKey();

	/**
	 * Returns the key-value pairs that will be delivered to the client.
	 *
	 * @return the key-value pair payload data
	 */
	public Map<String, String> getData();

	/**
	 * Indicates if message delivery should wait until the device is active.
	 * active. If false, the device will be woken up to receive the message.
	 *
	 * @return {@code true} if message delivery should wait until the device is
	 *         active; {@code false} if it should be delivered immediately
	 */
	public boolean delayWhileIdle();

	/**
	 * The Time to Live (TTL) feature lets the sender specify the maximum
	 * lifespan of a message using the time_to_live parameter in the send
	 * request. The value of this parameter must be a duration from 0 to
	 * 2,419,200 seconds, and it corresponds to the maximum period of time for
	 * which GCM will store and try to deliver the message. Requests that don't
	 * contain this field default to the maximum period of 4 weeks.
	 *
	 * @return the time to live in seconds
	 */
	public int timeToLive();
}
