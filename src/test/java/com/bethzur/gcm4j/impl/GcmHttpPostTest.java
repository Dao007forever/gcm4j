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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.util.EntityUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.bethzur.gcm4j.Message;
import com.bethzur.gcm4j.MessageBuilder;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link C2dmHttpPost}.
 * 
 * @author David R. Bild
 * 
 */

public class GcmHttpPostTest {
    private static final String REGISTRATION_ID = "My Registration Id";
    private static final String COLLAPSE_KEY = "My Collapse Key";
    private static final String DATA_KEY = "mykey";
    private static final String DATA_VALUE = "myvalue";
    private static final String AUTH_TOKEN = "My Auth Token";

    private static final ObjectMapper mapper = new ObjectMapper();

    private URI uri;

    private Message message;

    private String token;

    private GcmHttpPost post;

    @BeforeMethod
    public void setup() throws URISyntaxException {
        message = new MessageBuilder().registrationId(REGISTRATION_ID)
                .collapseKey(COLLAPSE_KEY).delayWhileIdle(true)
                .put(DATA_KEY, DATA_VALUE).build();
        uri = new URI("https://my.test.com/my/test/");
        token = AUTH_TOKEN;
        post = new GcmHttpPost(message, token, uri);
    }

    private Map<String, Object> getJson() throws IOException,
            JsonParseException, JsonMappingException {
        String data = EntityUtils.toString(post.getEntity());
        Map<String, Object> json = mapper.readValue(data, mapper
                .getTypeFactory().
                constructMapType(HashMap.class, String.class, Object.class));
        return json;
    }

    @Test
    public void uriIsSet() {
        assertThat(post.getURI(), is(uri));
    }

    @Test
    public void authTokenHeaderIsSet() {
        String authTokenHeaderValue = String.format("key=%s",
                token);

        assertThat(post.containsHeader("Authorization"), is(true));
        assertThat(post.getFirstHeader("Authorization").getValue(),
                is(authTokenHeaderValue));
    }

    @Test
    public void entityIsFormEncoded() {
        assertThat(post.getEntity().getContentType().getValue(),
                is("application/json"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void entityContainsRegistrationId() throws IOException {
        Map<String, Object> json = getJson();
        assertThat((List<String>) json.get("registration_ids"),
                is(Arrays.asList(REGISTRATION_ID)));
    }

    @Test
    public void entityContainsCollapseKey() throws IOException {
        Map<String, Object> json = getJson();
        assertThat((String) json.get("collapse_key"), is(COLLAPSE_KEY));
    }

    @Test
    public void entityContainsDelayWhileIdle() throws IOException {
        Map<String, Object> json = getJson();
        assertThat((Boolean) json.get("delay_while_idle"), is(true));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void entityContainsDataPairs() throws IOException {
        Map<String, Object> json = getJson();
        Map<String, String> data = (Map<String, String>) json.get("data");
        assertThat(data.get(DATA_KEY), is(DATA_VALUE));
    }
}
