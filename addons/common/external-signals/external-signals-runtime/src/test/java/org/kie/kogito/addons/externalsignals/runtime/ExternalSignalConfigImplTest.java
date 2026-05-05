/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kie.kogito.addons.externalsignals.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.kie.kogito.addons.externalsignals.ExternalSignalConfig;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalSignalConfigImplTest {

    @Test
    void testDefaultConfiguration() {
        // Given
        Map<String, String> emptyProps = new HashMap<>();

        // When
        ExternalSignalConfig config = new ExternalSignalConfigImpl(emptyProps);

        // Then
        assertThat(config.getSignalTriggerMapping()).isEmpty();
        assertThat(config.getDefaultTriggerPrefix()).isEqualTo(ExternalSignalConfig.DEFAULT_TRIGGER_PREFIX);
    }

    @Test
    void testLoadSignalMappings() {
        // Given
        Map<String, String> props = new HashMap<>();
        props.put("kogito.external-signals.mapping.OrderCreated", "order-events");
        props.put("kogito.external-signals.mapping.PaymentProcessed", "payment-topic");
        props.put("other.property", "ignored");

        // When
        ExternalSignalConfig config = new ExternalSignalConfigImpl(props);

        // Then
        assertThat(config.getSignalTriggerMapping())
                .hasSize(2)
                .containsEntry("OrderCreated", "order-events")
                .containsEntry("PaymentProcessed", "payment-topic");
    }

    @Test
    void testLoadCustomDefaultPrefix() {
        // Given
        Map<String, String> props = new HashMap<>();
        props.put("kogito.external-signals.default-prefix", "my-app-signal");

        // When
        ExternalSignalConfig config = new ExternalSignalConfigImpl(props);

        // Then
        assertThat(config.getDefaultTriggerPrefix()).isEqualTo("my-app-signal");
    }

    @Test
    void testTrimsWhitespaceFromValues() {
        // Given
        Map<String, String> props = new HashMap<>();
        props.put("kogito.external-signals.mapping.TestSignal", "  test-topic  ");
        props.put("kogito.external-signals.default-prefix", "  my-prefix  ");

        // When
        ExternalSignalConfig config = new ExternalSignalConfigImpl(props);

        // Then
        assertThat(config.getSignalTriggerMapping().get("TestSignal")).isEqualTo("test-topic");
        assertThat(config.getDefaultTriggerPrefix()).isEqualTo("my-prefix");
    }

    @Test
    void testIgnoresEmptySignalName() {
        // Given
        Map<String, String> props = new HashMap<>();
        props.put("kogito.external-signals.mapping.", "should-be-ignored");
        props.put("kogito.external-signals.mapping.ValidSignal", "valid-topic");

        // When
        ExternalSignalConfig config = new ExternalSignalConfigImpl(props);

        // Then
        assertThat(config.getSignalTriggerMapping())
                .hasSize(1)
                .containsEntry("ValidSignal", "valid-topic");
    }

    @Test
    void testIgnoresEmptyTriggerValue() {
        // Given
        Map<String, String> props = new HashMap<>();
        props.put("kogito.external-signals.mapping.EmptyTrigger", "");
        props.put("kogito.external-signals.mapping.WhitespaceTrigger", "   ");
        props.put("kogito.external-signals.mapping.ValidSignal", "valid-topic");

        // When
        ExternalSignalConfig config = new ExternalSignalConfigImpl(props);

        // Then
        assertThat(config.getSignalTriggerMapping())
                .hasSize(1)
                .containsEntry("ValidSignal", "valid-topic");
    }

    @Test
    void testGetMappedTrigger() {
        // Given
        Map<String, String> props = new HashMap<>();
        props.put("kogito.external-signals.mapping.OrderCreated", "order-events");

        ExternalSignalConfig config = new ExternalSignalConfigImpl(props);

        // When/Then
        assertThat(config.getMappedTrigger("OrderCreated")).hasValue("order-events");
        assertThat(config.getMappedTrigger("UnmappedSignal")).isEmpty();
    }

    @Test
    void testResolveTriggerWithMapping() {
        // Given
        Map<String, String> props = new HashMap<>();
        props.put("kogito.external-signals.mapping.OrderCreated", "order-events");
        props.put("kogito.external-signals.default-prefix", "my-app");

        ExternalSignalConfig config = new ExternalSignalConfigImpl(props);

        // When/Then
        assertThat(config.resolveTrigger("OrderCreated")).isEqualTo("order-events");
    }

    @Test
    void testResolveTriggerWithoutMapping() {
        // Given
        Map<String, String> props = new HashMap<>();
        props.put("kogito.external-signals.default-prefix", "my-app");

        ExternalSignalConfig config = new ExternalSignalConfigImpl(props);

        // When/Then
        assertThat(config.resolveTrigger("UnmappedSignal")).isEqualTo("my-app-UnmappedSignal");
    }

    @Test
    void testResolveTriggerWithDefaultPrefix() {
        // Given
        Map<String, String> emptyProps = new HashMap<>();
        ExternalSignalConfig config = new ExternalSignalConfigImpl(emptyProps);

        // When/Then
        assertThat(config.resolveTrigger("TestSignal"))
                .isEqualTo(ExternalSignalConfig.DEFAULT_TRIGGER_PREFIX + "-TestSignal");
    }

    @Test
    void testConstructorWithProperties() {
        // Given
        Properties props = new Properties();
        props.setProperty("kogito.external-signals.mapping.TestSignal", "test-topic");
        props.setProperty("kogito.external-signals.default-prefix", "test-prefix");

        // When
        ExternalSignalConfig config = new ExternalSignalConfigImpl(props);

        // Then
        assertThat(config.getSignalTriggerMapping()).containsEntry("TestSignal", "test-topic");
        assertThat(config.getDefaultTriggerPrefix()).isEqualTo("test-prefix");
    }

    @Test
    void testGetSignalTriggerMappingIsUnmodifiable() {
        // Given
        Map<String, String> props = new HashMap<>();
        props.put("kogito.external-signals.mapping.TestSignal", "test-topic");
        ExternalSignalConfig config = new ExternalSignalConfigImpl(props);

        // When/Then
        assertThat(config.getSignalTriggerMapping())
                .containsEntry("TestSignal", "test-topic");

        // Verify it's unmodifiable (would throw UnsupportedOperationException)
        assertThat(config.getSignalTriggerMapping()).isUnmodifiable();
    }

    @Test
    void testToString() {
        // Given
        Map<String, String> props = new HashMap<>();
        props.put("kogito.external-signals.mapping.Signal1", "topic1");
        props.put("kogito.external-signals.mapping.Signal2", "topic2");
        props.put("kogito.external-signals.default-prefix", "test-prefix");

        ExternalSignalConfig config = new ExternalSignalConfigImpl(props);

        // When
        String toString = config.toString();

        // Then
        assertThat(toString)
                .contains("ExternalSignalConfigImpl")
                .contains("mappings=2")
                .contains("test-prefix");
    }
}

// Made with Bob
