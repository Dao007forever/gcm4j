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

import static com.bethzur.gcm4j.Constants.JSON_CANONICAL_IDS;
import static com.bethzur.gcm4j.Constants.JSON_ERROR;
import static com.bethzur.gcm4j.Constants.JSON_FAILURE;
import static com.bethzur.gcm4j.Constants.JSON_MESSAGE_ID;
import static com.bethzur.gcm4j.Constants.JSON_MULTICAST_ID;
import static com.bethzur.gcm4j.Constants.JSON_RESULTS;
import static com.bethzur.gcm4j.Constants.JSON_SUCCESS;
import static com.bethzur.gcm4j.Constants.TOKEN_CANONICAL_REG_ID;

import java.io.IOException;
import java.util.Date;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.util.EntityUtils;

import com.bethzur.gcm4j.Message;
import com.bethzur.gcm4j.MulticastResult;
import com.bethzur.gcm4j.Response;
import com.bethzur.gcm4j.ResponseType;
import com.bethzur.gcm4j.UnexpectedResponseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A handler responsible for parsing GCM http responsesTOKEN to construct
 * {@link ResponseImpl}, {@link SuccesssResponseImpl}, and
 * {@link UnavailableResponseImpl} objects encapsulating them.
 * 
 * @author David R. Bild
 * 
 */
class GcmHttpResponseHandler implements ResponseHandler<Response> {
    private static final ObjectMapper mapper = new ObjectMapper();

    final Message message;

    public GcmHttpResponseHandler(Message message) {
        this.message = message;
    }

    @Override
    public Response handleResponse(HttpResponse response)
            throws IOException {
        switch (response.getStatusLine().getStatusCode()) {
        case 200:
            JsonNode root = parseBody(response);

            MulticastResult result = getResult(root);
            return new ResponseImpl(ResponseType.ServerResponse.Success,
                    result, null, message);
        case 500:
            return new ResponseImpl(ResponseType.ServerResponse.InternalError,
                    null, null, message);
        case 503:
            Date retryAfter = getRetryAfter(response);
            return new ResponseImpl(
                    ResponseType.ServerResponse.ServiceUnavailable,
                    null, retryAfter, message);
            // case 400: // JSON
        case 401:
            return new ResponseImpl(ResponseType.ServerResponse.Unauthorized,
                    null, null, message);
        default:
            throw new UnexpectedResponseException(String.format(
                    "Unexpected HTTP status code: %d", response.getStatusLine()
                            .getStatusCode()));
        }
    }

    private Date getRetryAfter(HttpResponse response) {
        Header retryAfterHeader = response.getFirstHeader("Retry-After");
        if (retryAfterHeader != null) {
            // Read as HTTP-Date
            try {
                return org.apache.http.impl.cookie.DateUtils
                        .parseDate(retryAfterHeader.getValue());
            } catch (DateParseException e) {
            }

            // Read as seconds
            try {
                return new Date(System.currentTimeMillis() + 1000L
                        * Integer.valueOf(retryAfterHeader.getValue()));
            } catch (NumberFormatException e) {
            }
        }

        // Otherwise
        return null;
    }

    private JsonNode parseBody(HttpResponse response)
            throws UnexpectedResponseException {
        try {
            String body = EntityUtils.toString(response.getEntity());
            JsonNode root = mapper.readTree(body);

            return root;
        } catch (ParseException e) {
            throw new UnexpectedResponseException(e);
        } catch (IOException e) {
            throw new UnexpectedResponseException(e);
        }
    }

    private MulticastResult getResult(JsonNode root)
            throws UnexpectedResponseException {
        int success = root.get(JSON_SUCCESS).intValue();
        int failure = root.get(JSON_FAILURE).intValue();
        int canonicalIds = root.get(JSON_CANONICAL_IDS).intValue();
        long multicastId = root.get(JSON_MULTICAST_ID).longValue();
        MulticastResultImpl.Builder resultBuilder = new MulticastResultImpl.Builder(
                success, failure, canonicalIds, multicastId);

        JsonNode jsonResults = root.get(JSON_RESULTS);
        if (jsonResults != null) {
            for (final JsonNode jsonResult : jsonResults) {
                ResultImpl.Builder builder = new ResultImpl.Builder();
                String messageId = jsonResult.path(JSON_MESSAGE_ID).asText();
                String canonicalRegId = jsonResult.path(TOKEN_CANONICAL_REG_ID)
                        .asText();

                builder.messageId(messageId).canonicalRegistrationId(
                        canonicalRegId);

                String error = jsonResult.path(JSON_ERROR).asText();
                if (!error.isEmpty()) {
                    switch (ResponseErrorValues.valueOf(error)) {
                    case MissingRegistration:
                        builder.errorCode(ResponseType.IndividualResponse.MissingRegistration);
                    case InvalidRegistration:
                        builder.errorCode(ResponseType.IndividualResponse.InvalidRegistration);
                    case MismatchSenderId:
                        builder.errorCode(ResponseType.IndividualResponse.MismatchSenderId);
                    case NotRegistered:
                        builder.errorCode(ResponseType.IndividualResponse.NotRegistered);
                    case MessageTooBig:
                        builder.errorCode(ResponseType.IndividualResponse.MessageTooBig);
                    case InvalidDataKey:
                        builder.errorCode(ResponseType.IndividualResponse.InvalidDataKey);
                    case InvalidTtl:
                        builder.errorCode(ResponseType.IndividualResponse.InvalidTtl);
                    case Unavailable:
                        builder.errorCode(ResponseType.IndividualResponse.ServiceUnavailable);
                    case InternalServerError:
                        builder.errorCode(ResponseType.IndividualResponse.InternalError);
                    case InvalidPackageName:
                        builder.errorCode(ResponseType.IndividualResponse.InvalidPackageName);
                    default:
                        throw new UnexpectedResponseException(
                                "Unexpected error message.");
                    }
                }
                resultBuilder.addResult(builder.build());
            }
        }

        return resultBuilder.build();
    }

    /**
     * Possible values for the {@code Error} key in {@code 200} responses from
     * the GCM service.
     * 
     * @author David R. Bild
     * 
     */
    static enum ResponseErrorValues {
        MissingRegistration, InvalidRegistration, MismatchSenderId, NotRegistered, MessageTooBig, InvalidDataKey, InvalidTtl, Unavailable, InternalServerError, InvalidPackageName
    }
}
