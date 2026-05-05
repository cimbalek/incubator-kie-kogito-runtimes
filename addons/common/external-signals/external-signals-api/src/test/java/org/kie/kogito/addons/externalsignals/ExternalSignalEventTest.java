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
package org.kie.kogito.addons.externalsignals;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalSignalEventTest {

    @Test
    void testBuilderCreatesEventWithAllFields() {
        // Given
        String signalName = "OrderApproval";
        Object signalData = Map.of("orderId", "12345");
        String processInstanceId = "process-123";
        String correlationId = "corr-456";
        Instant timestamp = Instant.now();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");

        // When
        ExternalSignalEvent event = ExternalSignalEvent.builder()
                .signalName(signalName)
                .signalData(signalData)
                .sourceProcessInstanceId(processInstanceId)
                .correlationId(correlationId)
                .timestamp(timestamp)
                .metadata(metadata)
                .build();

        // Then
        assertThat(event.getSignalName()).isEqualTo(signalName);
        assertThat(event.getSignalData()).isEqualTo(signalData);
        assertThat(event.getSourceProcessInstanceId()).isEqualTo(processInstanceId);
        assertThat(event.getCorrelationId()).isEqualTo(correlationId);
        assertThat(event.getTimestamp()).isEqualTo(timestamp);
        assertThat(event.getMetadata()).containsEntry("key1", "value1");
    }

    @Test
    void testBuilderWithAddMetadata() {
        // When
        ExternalSignalEvent event = ExternalSignalEvent.builder()
                .signalName("TestSignal")
                .addMetadata("key1", "value1")
                .addMetadata("key2", "value2")
                .build();

        // Then
        assertThat(event.getMetadata())
                .containsEntry("key1", "value1")
                .containsEntry("key2", "value2");
    }

    @Test
    void testCloudEventCompatibility() {
        // Given
        String processInstanceId = "process-123";
        String correlationId = "corr-456";

        ExternalSignalEvent event = ExternalSignalEvent.builder()
                .signalName("TestSignal")
                .sourceProcessInstanceId(processInstanceId)
                .correlationId(correlationId)
                .build();

        // Then - CloudEvent attributes
        assertThat(event.getType()).isEqualTo("org.kie.kogito.signal.external");
        assertThat(event.getSource()).isEqualTo(URI.create("kogito://process/" + processInstanceId));
        assertThat(event.getId()).isEqualTo(correlationId);
    }

    @Test
    void testCloudEventSourceWithNullProcessInstanceId() {
        // Given
        ExternalSignalEvent event = ExternalSignalEvent.builder()
                .signalName("TestSignal")
                .build();

        // Then
        assertThat(event.getSource()).isEqualTo(URI.create("kogito://process/unknown"));
    }

    @Test
    void testDefaultConstructorInitializesMetadata() {
        // When
        ExternalSignalEvent event = new ExternalSignalEvent();

        // Then
        assertThat(event.getMetadata()).isNotNull().isEmpty();
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void testAddMetadataMethod() {
        // Given
        ExternalSignalEvent event = new ExternalSignalEvent();

        // When
        event.addMetadata("key1", "value1");
        event.addMetadata("key2", 123);

        // Then
        assertThat(event.getMetadata())
                .containsEntry("key1", "value1")
                .containsEntry("key2", 123);
    }

    @Test
    void testSetMetadataWithNull() {
        // Given
        ExternalSignalEvent event = new ExternalSignalEvent();
        event.addMetadata("key1", "value1");

        // When
        event.setMetadata(null);

        // Then
        assertThat(event.getMetadata()).isNotNull().isEmpty();
    }

    @Test
    void testEqualsAndHashCode() {
        // Given
        ExternalSignalEvent event1 = ExternalSignalEvent.builder()
                .signalName("TestSignal")
                .sourceProcessInstanceId("process-123")
                .correlationId("corr-456")
                .build();

        ExternalSignalEvent event2 = ExternalSignalEvent.builder()
                .signalName("TestSignal")
                .sourceProcessInstanceId("process-123")
                .correlationId("corr-456")
                .build();

        ExternalSignalEvent event3 = ExternalSignalEvent.builder()
                .signalName("DifferentSignal")
                .sourceProcessInstanceId("process-123")
                .correlationId("corr-456")
                .build();

        // Then
        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
        assertThat(event1).isNotEqualTo(event3);
    }

    @Test
    void testToString() {
        // Given
        ExternalSignalEvent event = ExternalSignalEvent.builder()
                .signalName("TestSignal")
                .sourceProcessInstanceId("process-123")
                .correlationId("corr-456")
                .build();

        // When
        String toString = event.toString();

        // Then
        assertThat(toString)
                .contains("TestSignal")
                .contains("process-123")
                .contains("corr-456");
    }
}

// Made with Bob
