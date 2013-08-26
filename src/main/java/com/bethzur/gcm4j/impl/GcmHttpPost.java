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
package com.bethzur.gcm4j.impl;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;

import com.bethzur.gcm4j.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Encapsulates an HTTP POST request to the GCM service. This class is
 * responsible for constructing the request from a {@link Message} instance.
 *
 * @author David R. Bild
 *
 */
class GcmHttpPost extends HttpPost {

	private static final String REGISTRATION_IDS = "registration_ids";
	private static final String COLLAPSE_ID = "collapse_key";
	private static final String DELAY_WHILE_IDLE = "delay_while_idle";
	private static final String TIME_TO_LIVE = "time_to_live";
	private static final String DATA = "data";
	
	private static final ObjectMapper mapper = new ObjectMapper();

	/**
	 * Constructs a new POST requests for the specified message, authentication
	 * token, and endpoint.
	 *
	 * @param message
	 *            the message to be placed into the request body
	 * @param token
	 *            the authentication token for the request
	 * @param uri
	 *            the remote endpoint for the request
	 */
	public GcmHttpPost(Message message, String api_key, URI uri) {
		super(uri);
		initAuthKey(api_key);
		initPostEntity(message);
	}

	private void initAuthKey(String api_key) {
		this.setHeader("Authorization", "key=" + api_key);
	}

	private void initPostEntity(Message message) {
		Map<String, Object> json = new HashMap<String, Object>();

		json.put(REGISTRATION_IDS, message.getRegistrationIds());
		json.put(COLLAPSE_ID, message.getCollapseKey());
		if (message.delayWhileIdle())
			json.put(DELAY_WHILE_IDLE, Boolean.TRUE);

		if (message.timeToLive() >= 0)
			json.put(TIME_TO_LIVE, message.timeToLive());

		Map<String, String> data = message.getData();
		if (data != null && !data.isEmpty()) {
		    json.put(DATA, data);
		}

		try {
		    ByteArrayEntity entity = new ByteArrayEntity(mapper.writeValueAsBytes(json));
		    entity.setContentEncoding("UTF-8");
		    entity.setContentType("application/json; charset=UTF-8");
			this.setEntity(entity);
		} catch (JsonProcessingException e) {
		    throw new RuntimeException(e.getMessage(), e);
        }
	}
}
