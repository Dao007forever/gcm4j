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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link MessageBuilder}.
 * 
 * @author David R. Bild
 * 
 */
public class MessageBuilderTest {
    private static final String REGISTRATION_ID = "My Registration Id";
    private static final String COLLAPSE_KEY = "My Collapse Key";
    private static final String DATA_KEY = "My Data Key";
    private static final String DATA_VALUE = "My Data Value";

    private MessageBuilder builder;

    private Map<String, String> data;

    @BeforeMethod
    public void setup() {
        this.builder = new MessageBuilder();
        data = new HashMap<String, String>();
        data.put(DATA_KEY, DATA_VALUE);
    }

    @Test
    public void registrationIdReturnsSameBuilder() {
        assertThat(builder.registrationId(REGISTRATION_ID), is(builder));
    }

    @Test
    public void collapseKeyReturnsSameBuilder() {
        assertThat(builder.collapseKey(COLLAPSE_KEY), is(builder));
    }

    @Test
    public void delayWhileIdleReturnsSameBuilder() {
        assertThat(builder.delayWhileIdle(true), is(builder));
    }

    @Test
    public void putReturnsSameBuilder() {
        assertThat(builder.put(DATA_KEY, DATA_VALUE), is(builder));
    }

    @Test
    public void putAllReturnsSameBuilder() {
        assertThat(builder.putAll(data), is(builder));
    }

    @Test
    public void removeReturnsSameBuilder() {
        assertThat(builder.remove(DATA_KEY), is(builder));
    }

    @Test
    public void buildReturnsMessageContainingSuppliedValues() {
        Map<String, String> data = new HashMap<String, String>();
        data.put(DATA_KEY, DATA_VALUE);

        Message message = builder.registrationId(REGISTRATION_ID)
                .collapseKey(COLLAPSE_KEY).delayWhileIdle(true)
                .put(DATA_KEY, DATA_VALUE).build();

        assertThat(message.getRegistrationIds().get(0), is(REGISTRATION_ID));
        assertThat(message.getCollapseKey(), is(COLLAPSE_KEY));
        assertThat(message.delayWhileIdle(), is(true));
        assertThat(message.getData(), is(data));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void buildThrowsExceptionForMissingRegistrationId() {
        builder.collapseKey(COLLAPSE_KEY);
        builder.build();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void buildThrowsExceptionForMissingCollapseKey() {
        builder.registrationId(REGISTRATION_ID);
        builder.build();
    }

}
