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
package com.bethzur.gcm4j;

/**
 * An enum representing the response from the GCM service for a message push
 * request. The class encapsulating the response can be retrieved via the
 * {@link #associatedClass()} method.
 * <p>
 * Descriptions for the individual enum values are taken from the <a
 * href="http://code.google.com/android/c2dm/#server">GCM web page</a>.
 * 
 * @see Response
 * 
 * @author David R. Bild
 * 
 */
public class ResponseType {
    public static enum ServerResponse {
        /**
         * Message was successfully received by the GCM service. N.B., this does
         * not indicate the message has been delivered to the client yet.
         */
        Success,
        /**
         * Indicates that the server is temporarily unavailable (i.e., because
         * of
         * timeouts, etc). Sender must retry later, honoring any
         * {@code Retry-After} header included in the response. Application
         * servers must implement the
         * exponential back off. Senders that create problems risk being
         * blacklisted.
         */
        ServiceUnavailable,
        /**
         * Indicates that the <a href=
         * "http://code.google.com/apis/accounts/docs/AuthForInstalledApps.html"
         * >ClientLogin</a> {@code AUTH_TOKEN} used to validate the sender is
         * invalid.
         */
        Unauthorized,
        /**
         * There was an internal error in the GCM server while trying to process
         * the
         * request.
         */
        InternalError;
    }

    public static enum IndividualResponse {
        /**
         * Indicates that the server is temporarily unavailable (i.e., because
         * of
         * timeouts, etc). Sender must retry later, honoring any
         * {@code Retry-After} header included in the response. Application
         * servers must implement the
         * exponential back off. Senders that create problems risk being
         * blacklisted.
         */
        ServiceUnavailable,
        /**
         * Too many messages sent by the sender. Retry after a while.
         */
        MissingRegistration,
        /**
         * Bad {@code registration_id}. Sender should remove this
         * {@code registration_id}.
         */
        InvalidRegistration,
        /**
         * The {@code sender_id} contained in the {@code registration_id} does
         * not
         * match the sender id used to register with the GCM servers.
         */
        MismatchSenderId,
        /**
         * The user has uninstalled the application or turned off notifications.
         * Sender should stop sending messages to this device and delete the
         * {@code registration_id}. The client needs to re-register with the GCM
         * servers to receive notifications again.
         */
        NotRegistered,
        /**
         * The total size of the payload data that is included in a message
         * can't
         * exceed 4096 bytes. Note that this includes both the size of the keys
         * as
         * well as the values.
         * Reduce the size of the message.
         */
        MessageTooBig,

        /**
         * The key in the payload is reserved by Google.
         * Use another key.
         */
        InvalidDataKey,
        /**
         * The value for the Time to Live field must be an integer representing
         * a
         * duration in seconds between 0 and 2,419,200 (4 weeks).
         */
        InvalidTtl,
        /**
         * There was an internal error in the GCM server while trying to process
         * the
         * request.
         */
        InternalError,
        /**
         * A message was addressed to a registration ID whose package name did
         * not
         * match the value passed in the request.
         */
        InvalidPackageName;
    }
}
