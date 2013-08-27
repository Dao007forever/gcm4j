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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.bethzur.gcm4j.Constants;
import com.bethzur.gcm4j.Message;
import com.bethzur.gcm4j.MessageBuilder;
import com.bethzur.gcm4j.MulticastResult;
import com.bethzur.gcm4j.Response;
import com.bethzur.gcm4j.ResponseType;
import com.bethzur.gcm4j.Result;
import com.bethzur.gcm4j.auth.ApiKeyAuthProvider;

/**
 * Unit tests for {@link DefaultC2dmManager}.
 * 
 * @author David R. Bild
 * 
 */
public class DefaultGcmManagerTest {
    private static final String AUTH_TOKEN = "my auth token";
    private static final String MESSAGE_ID = "ABC";

    private Message message;

    private ApiKeyAuthProvider authProvider;

    private HttpClient client;

    private Response response;

    private MulticastResult result;

    private Date tryAfter;

    private DefaultGcmManager cut;

    private final ArgumentCaptor<GcmHttpPost> post = ArgumentCaptor
            .forClass(GcmHttpPost.class);

    private final ArgumentCaptor<GcmHttpResponseHandler> handler = ArgumentCaptor
            .forClass(GcmHttpResponseHandler.class);

    @BeforeMethod
    public void setup() throws ClientProtocolException, IOException {
        message = new MessageBuilder().collapseKey("collapsekey")
                .registrationId("myregistrationid").put("mykey", "mydata")
                .build();
        Result singleResult = new ResultImpl.Builder().messageId(MESSAGE_ID)
                .build();
        result = new MulticastResultImpl.Builder(1, 0, 0, 0L).addResult(
                singleResult).build();
        tryAfter = new Date();
        authProvider = new ApiKeyAuthProvider(AUTH_TOKEN);
        client = mock(HttpClient.class);

        cut = new DefaultGcmManager(client, authProvider);
    }

    @Test
    public void pushMessageSendsMessageAndReturnsResponse()
            throws ClientProtocolException, IOException {
        response = new ResponseImpl(ResponseType.ServerResponse.Success,
                result,
                tryAfter,
                message);
        when(
                client.execute(any(GcmHttpPost.class),
                        any(GcmHttpResponseHandler.class))).thenReturn(
                response);

        Response ret = cut.pushMessage(message);

        verify(client).execute(post.capture(), handler.capture());

        assertThat("URI", post.getValue().getURI().toString(),
                is(Constants.GCM_SEND_ENDPOINT));
        assertThat("AuthToken", post.getValue().getFirstHeader("Authorization")
                .getValue(),
                is(String.format("key=%s", authProvider
                        .getKey())));

        assertThat("Message", handler.getValue().message, is(message));

        assertThat("Response", ret, is(response));
    }
}
