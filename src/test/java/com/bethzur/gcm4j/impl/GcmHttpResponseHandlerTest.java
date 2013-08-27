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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.datatype.Duration;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.message.BasicHttpResponse;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.bethzur.gcm4j.Message;
import com.bethzur.gcm4j.MessageBuilder;
import com.bethzur.gcm4j.Response;
import com.bethzur.gcm4j.ResponseType;
import com.bethzur.gcm4j.UnexpectedResponseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link C2dmHttpResponseHandler}.
 *
 * @author David R. Bild
 *
 */
public class GcmHttpResponseHandlerTest {
    private static final long MULTICAST_MESSAGE_ID = 123456789L;

    private static final ObjectMapper mapper = new ObjectMapper();

    private Message message;

    private GcmHttpResponseHandler cut;

    @BeforeMethod
    public void setup() {
        message = new MessageBuilder().collapseKey("My collapse key")
                .registrationId("My GCM registration id").delayWhileIdle(true)
                .put("mykey", "myvalue").build();
        cut = new GcmHttpResponseHandler(message);
    }

    private HttpResponse buildResponse(int statusCode) {
        return new BasicHttpResponse(HttpVersion.HTTP_1_1, statusCode,
                EnglishReasonPhraseCatalog.INSTANCE.getReason(statusCode,
                        Locale.US));
    }

    @Test(expectedExceptions = UnexpectedResponseException.class)
    public void throwsUnexpectedResponseExceptionForMalformattedSuccessEntity()
            throws IOException {
        HttpResponse response = buildResponse(200);
        response.setEntity(new StringEntity("id=1234;id=1234"));

        cut.handleResponse(response);
    }

    @Test(expectedExceptions = UnexpectedResponseException.class)
    public void throwsUnexpectedResponseExceptionForInvalidSuccessEntityKey()
            throws IOException {
        HttpResponse response = buildResponse(200);
        response.setEntity(new StringEntity("badkey=0122345678910"));

        cut.handleResponse(response);
    }

    private HttpResponse buildSuccessResponse()
            throws UnsupportedEncodingException, JsonProcessingException {
        HttpResponse response = buildResponse(200);
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("multicast_id", MULTICAST_MESSAGE_ID);
        json.put("success", 1);
        json.put("failure", 0);
        json.put("canonical_ids", 0);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("message_id", "0:1355822022916886%8ae6056ef9fd7ecd");
        json.put("results", Arrays.asList(result));

        response.setEntity(new StringEntity(mapper.writeValueAsString(json)));
        return response;
    }

    @Test
    public void parsesSuccess() throws IOException {
        HttpResponse response = buildSuccessResponse();

        Response result = cut.handleResponse(response);
        assertThat(result.getServerResponse(),
                is(ResponseType.ServerResponse.Success));
        assertThat(result.getMessage(), is(message));
        assertThat((result instanceof ResponseImpl), is(true));

        assertThat(result.getResult().getMulticastId(),
                is(MULTICAST_MESSAGE_ID));
    }

    private HttpResponse buildErrorResponse(String error)
            throws UnsupportedEncodingException {
        HttpResponse response = buildResponse(200);
        response.setEntity(new StringEntity(String.format("Error=%s", error)));
        return response;
    }

    private void parsesServerError(String error,
            ResponseType.ServerResponse expected)
            throws IOException {
        HttpResponse response = buildErrorResponse(error);

        Response result = cut.handleResponse(response);
        assertThat(result.getServerResponse(), is(expected));
        assertThat(result.getMessage(), is(message));
    }

    private void parsesIndividualError(String error,
            ResponseType.IndividualResponse expected)
            throws IOException {
        HttpResponse response = buildErrorResponse(error);

        Response result = cut.handleResponse(response);
        assertThat(result.getResult().getResults().get(0).getErrorCodeName(),
                is(expected));
        assertThat(result.getMessage(), is(message));
    }

    @Test
    public void parsesMissingRegistration() throws IOException {
        parsesIndividualError("MissingRegistration",
                ResponseType.IndividualResponse.MissingRegistration);
    }

    @Test
    public void parsesInvalidRegistration() throws IOException {
        parsesIndividualError("InvalidRegistration",
                ResponseType.IndividualResponse.InvalidRegistration);
    }

    @Test
    public void parsesMismatchSenderId() throws IOException {
        parsesIndividualError("MismatchSenderId",
                ResponseType.IndividualResponse.MismatchSenderId);
    }

    @Test
    public void parsesNotRegistered() throws IOException {
        parsesIndividualError("NotRegistered",
                ResponseType.IndividualResponse.NotRegistered);
    }

    @Test
    public void parsesMessageTooBig() throws IOException {
        parsesIndividualError("MessageTooBig",
                ResponseType.IndividualResponse.MessageTooBig);
    }

    @Test(expectedExceptions = UnexpectedResponseException.class)
    public void throwsUnexpectedResponseExceptionForUnknownErrorValue()
            throws IOException {
        parsesServerError("not_a_real_error_code", null);
    }

    @Test
    public void parsesUnauthorized() throws IOException {
        HttpResponse response = buildResponse(401);

        Response result = cut.handleResponse(response);
        assertThat(result.getServerResponse(),
                is(ResponseType.ServerResponse.Unauthorized));
        assertThat(result.getMessage(), is(message));
    }

    @Test
    public void parsesUnavailableWithoutRetryHeader() throws IOException {
        HttpResponse response = buildResponse(503);

        Response result = cut.handleResponse(response);
        assertThat(result.getServerResponse(),
                is(ResponseType.ServerResponse.ServiceUnavailable));
        assertThat(result.getMessage(), is(message));
        assertThat((result instanceof ResponseImpl), is(true));

        assertThat(result.retryAfter(), is(nullValue()));
    }

    @Test
    public void parsesUnavailableWithRetryAsDate() throws IOException {
        HttpResponse response = buildResponse(503);

        Date retryAfter = new Date(1000000000000L);
        response.setHeader("Retry-After", "Sun, 09 Sep 2001 01:46:40 GMT");

        Response result = cut.handleResponse(response);
        assertThat(result.getServerResponse(),
                is(ResponseType.ServerResponse.ServiceUnavailable));
        assertThat(result.getMessage(), is(message));
        assertThat((result instanceof ResponseImpl), is(true));

        assertThat(result.retryAfter(), is(retryAfter));
    }

    @Test
    public void parsesUnavailableWithRetryAsSeconds() throws IOException {
        HttpResponse response = buildResponse(503);

        DateTime expected = new DateTime().plusSeconds(42);
        response.setHeader("Retry-After", "42");

        Response result = cut.handleResponse(response);
        assertThat(result.getResponseType(),
                is(ResponseType.ServiceUnavailable));
        assertThat(result.getMessage(), is(message));
        assertThat((result instanceof UnavailableResponse), is(true));

        UnavailableResponse uResult = (UnavailableResponse) result;
        assertThat(uResult.hasRetryAfter(), is(true));
        DateTime retryAfter = new DateTime(uResult.retryAfter());
        // Check that the different between the expected and returned retry time
        // is less than one second.
        assertThat(new Duration(expected, retryAfter).isShorterThan(Duration
                .standardSeconds(1)), is(true));
    }

    @Test(expectedExceptions = UnexpectedResponseException.class)
    public void throwsUnexpectedResponseForUnexpectedStatusCode()
            throws IOException {
        HttpResponse response = buildResponse(404);

        cut.handleResponse(response);
    }

    @Test
    public void retrievesAuthTokenHeader() throws IOException {
        HttpResponse response = buildSuccessResponse();

        AuthToken newAuthToken = new AuthToken("My new auth token");
        response.addHeader("Update-Client-Auth", newAuthToken.toString());

        Response result = cut.handleResponse(response);
        assertThat(result.hasUpdatedAuthToken(), is(true));
        assertThat(result.getUpdatedAuthToken(), is(newAuthToken));
    }

    @Test
    public void acceptsMissingAuthTokenHeader() throws IOException {
        HttpResponse response = buildSuccessResponse();

        Response result = cut.handleResponse(response);
        assertThat(result.hasUpdatedAuthToken(), is(false));
    }

}
