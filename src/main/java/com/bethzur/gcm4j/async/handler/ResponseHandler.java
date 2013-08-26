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
package com.bethzur.gcm4j.async.handler;

import com.bethzur.gcm4j.Response;

/**
 * Interface for a response handler. A method is provided to handle responses
 * from the GCM service.
 * 
 * @author David R. Bild
 * 
 * @param <R>
 *            the type of {@code Response} to handle
 */
public interface ResponseHandler<R extends Response> {

	/**
	 * Handle a response received from the GCM service.
	 * 
	 * @param context
	 *            the context associated with the response
	 */
	public void handleResponse(Context<R, ResultDecision> context);
	
	/**
     * Handle a response received from the GCM service.
     * 
     * @param context
     *            the context associated with the response
     */
    public boolean support(Context<R, ResultDecision> context);
}
