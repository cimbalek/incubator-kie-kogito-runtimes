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

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.kogito.addons.externalsignals.ExternalSignalConfig;
import org.kie.kogito.addons.externalsignals.ExternalSignalDispatchException;
import org.kie.kogito.addons.externalsignals.ExternalSignalEvent;
import org.kie.kogito.event.DataEvent;
import org.kie.kogito.event.EventEmitter;
import org.kie.kogito.event.impl.EventFactoryUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comprehensive test suite for {@link DefaultExternalSignalDispatcher}.
 * 
 * <p>
 * This test class verifies the dispatcher's behavior including:
 * </p>
 * <ul>
 * <li>Successful event emission via EventEmitter</li>
 * <li>Topic resolution from configuration</li>
 * <li>Default topic naming convention</li>
 * <li>CloudEvent creation with proper attributes</li>
 * <li>Metadata propagation to DataEvent</li>
 * <li>Error handling for missing EventEmitter</li>
 * <li>Validation of null or invalid signal events</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class DefaultExternalSignalDispatcherTest {

    @Mock
    private ExternalSignalConfig config;

    @Mock
    private EventEmitter eventEmitter;

    private DefaultExternalSignalDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new DefaultExternalSignalDispatcher(config);
    }

    @Test
    void testConstructorWithNullConfig() {
        assertThatThrownBy(() -> new DefaultExternalSignalDispatcher(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ExternalSignalConfig cannot be null");
    }

    @Test
    void testDefaultConstructor() {
        // When: Using default constructor
        DefaultExternalSignalDispatcher defaultDispatcher = new DefaultExternalSignalDispatcher();

        // Then: Dispatcher is created successfully
        assertThat(defaultDispatcher).isNotNull();
    }

    @Test
    void testSuccessfulDispatchWithMappedTrigger() {
        // Given: Event with mapped signal name
        ExternalSignalEvent event = ExternalSignalEvent.builder()
                .signalName("OrderCreated")
                .signalData(Map.of("orderId", "123"))
                .sourceProcessInstanceId("process-001")
                .correlationId("corr-001")
                .timestamp(Instant.now())
                .build();

        when(config.resolveTrigger("OrderCreated")).thenReturn("order-events");

        try (MockedStatic<EventFactoryUtils> mockedFactory = mockStatic(EventFactoryUtils.class)) {
            mockedFactory.when(() -> EventFactoryUtils.getEventEmitter("order-events"))
                    .thenReturn(eventEmitter);

            // When: Dispatching event
            dispatcher.dispatch(event);

            // Then: Event emitted to correct trigger
            ArgumentCaptor<DataEvent> dataEventCaptor = ArgumentCaptor.forClass(DataEvent.class);
            verify(eventEmitter).emit(dataEventCaptor.capture());

            DataEvent<?> capturedEvent = dataEventCaptor.getValue();
            assertThat(capturedEvent.getType()).isEqualTo("org.kie.kogito.signal.external.OrderCreated");
            assertThat(capturedEvent.getId()).isEqualTo("corr-001");
            assertThat(capturedEvent.getSource()).isEqualTo(URI.create("kogito://process/process-001"));
            assertThat(capturedEvent.getData()).isInstanceOf(ExternalSignalEvent.class);

            ExternalSignalEvent eventData = (ExternalSignalEvent) capturedEvent.getData();
            assertThat(eventData.getSignalName()).isEqualTo("OrderCreated");
            assertThat(eventData.getSignalData()).isEqualTo(Map.of("orderId", "123"));
        }
    }

    @Test
    void testSuccessfulDispatchWithDefaultTrigger() {
        // Given: Event with unmapped signal name
        ExternalSignalEvent event = ExternalSignalEvent.builder()
                .signalName("CustomSignal")
                .signalData(Map.of("key", "value"))
                .sourceProcessInstanceId("process-002")
                .correlationId("corr-002")
                .timestamp(Instant.now())
                .build();

        when(config.resolveTrigger("CustomSignal")).thenReturn("kogito-external-signal-CustomSignal");

        try (MockedStatic<EventFactoryUtils> mockedFactory = mockStatic(EventFactoryUtils.class)) {
            mockedFactory.when(() -> EventFactoryUtils.getEventEmitter("kogito-external-signal-CustomSignal"))
                    .thenReturn(eventEmitter);

            // When: Dispatching event
            dispatcher.dispatch(event);

            // Then: Event emitted to default trigger
            ArgumentCaptor<DataEvent> dataEventCaptor = ArgumentCaptor.forClass(DataEvent.class);
            verify(eventEmitter).emit(dataEventCaptor.capture());

            DataEvent<?> capturedEvent = dataEventCaptor.getValue();
            assertThat(capturedEvent.getType()).isEqualTo("org.kie.kogito.signal.external.CustomSignal");
            assertThat(capturedEvent.getId()).isEqualTo("corr-002");
        }
    }

    @Test
    void testDispatchWithNullEvent() {
        assertThatThrownBy(() -> dispatcher.dispatch(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ExternalSignalEvent cannot be null");
    }

    @Test
    void testDispatchWithNullSignalName() {
        // Given: Event with null signal name
        ExternalSignalEvent event = ExternalSignalEvent.builder()
                .signalName(null)
                .signalData(Map.of("key", "value"))
                .sourceProcessInstanceId("process-003")
                .correlationId("corr-003")
                .build();

        // When/Then: Exception thrown
        assertThatThrownBy(() -> dispatcher.dispatch(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Signal name cannot be null or empty");
    }

    @Test
    void testDispatchWithEmptySignalName() {
        // Given: Event with empty signal name
        ExternalSignalEvent event = ExternalSignalEvent.builder()
                .signalName("   ")
                .signalData(Map.of("key", "value"))
                .sourceProcessInstanceId("process-004")
                .correlationId("corr-004")
                .build();

        // When/Then: Exception thrown
        assertThatThrownBy(() -> dispatcher.dispatch(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Signal name cannot be null or empty");
    }

    @Test
    void testDispatchWithNullData() {
        // Given: Event with null data
        ExternalSignalEvent event = ExternalSignalEvent.builder()
                .signalName("NullDataSignal")
                .signalData(null)
                .sourceProcessInstanceId("process-005")
                .correlationId("corr-005")
                .timestamp(Instant.now())
                .build();

        when(config.resolveTrigger("NullDataSignal")).thenReturn("null-data-topic");

        try (MockedStatic<EventFactoryUtils> mockedFactory = mockStatic(EventFactoryUtils.class)) {
            mockedFactory.when(() -> EventFactoryUtils.getEventEmitter("null-data-topic"))
                    .thenReturn(eventEmitter);

            // When: Dispatching event
            dispatcher.dispatch(event);

            // Then: Event emitted successfully with null data
            ArgumentCaptor<DataEvent> dataEventCaptor = ArgumentCaptor.forClass(DataEvent.class);
            verify(eventEmitter).emit(dataEventCaptor.capture());

            DataEvent<?> capturedEvent = dataEventCaptor.getValue();
            ExternalSignalEvent eventData = (ExternalSignalEvent) capturedEvent.getData();
            assertThat(eventData.getSignalData()).isNull();
        }
    }

    @Test
    void testDispatchWithMetadata() {
        // Given: Event with metadata
        ExternalSignalEvent event = ExternalSignalEvent.builder()
                .signalName("MetadataSignal")
                .signalData(Map.of("data", "value"))
                .sourceProcessInstanceId("process-006")
                .correlationId("corr-006")
                .timestamp(Instant.now())
                .addMetadata("targetProcessInstanceId", "target-001")
                .addMetadata("targetWorkItemId", "workitem-001")
                .addMetadata("customKey", "customValue")
                .build();

        when(config.resolveTrigger("MetadataSignal")).thenReturn("metadata-topic");

        try (MockedStatic<EventFactoryUtils> mockedFactory = mockStatic(EventFactoryUtils.class)) {
            mockedFactory.when(() -> EventFactoryUtils.getEventEmitter("metadata-topic"))
                    .thenReturn(eventEmitter);

            // When: Dispatching event
            dispatcher.dispatch(event);

            // Then: Metadata preserved in emitted event
            ArgumentCaptor<DataEvent> dataEventCaptor = ArgumentCaptor.forClass(DataEvent.class);
            verify(eventEmitter).emit(dataEventCaptor.capture());

            DataEvent<?> capturedEvent = dataEventCaptor.getValue();
            ExternalSignalEvent eventData = (ExternalSignalEvent) capturedEvent.getData();

            assertThat(eventData.getMetadata()).containsEntry("targetProcessInstanceId", "target-001");
            assertThat(eventData.getMetadata()).containsEntry("targetWorkItemId", "workitem-001");
            assertThat(eventData.getMetadata()).containsEntry("customKey", "customValue");
        }
    }

    @Test
    void testDispatchWhenEventEmitterThrowsException() {
        // Given: EventEmitter throws exception
        ExternalSignalEvent event = ExternalSignalEvent.builder()
                .signalName("ErrorSignal")
                .signalData(Map.of("error", "test"))
                .sourceProcessInstanceId("process-007")
                .correlationId("corr-007")
                .timestamp(Instant.now())
                .build();

        when(config.resolveTrigger("ErrorSignal")).thenReturn("error-topic");

        try (MockedStatic<EventFactoryUtils> mockedFactory = mockStatic(EventFactoryUtils.class)) {
            mockedFactory.when(() -> EventFactoryUtils.getEventEmitter("error-topic"))
                    .thenReturn(eventEmitter);

            doThrow(new RuntimeException("Emission failed"))
                    .when(eventEmitter).emit(any(DataEvent.class));

            // When/Then: Exception wrapped and thrown
            assertThatThrownBy(() -> dispatcher.dispatch(event))
                    .isInstanceOf(ExternalSignalDispatchException.class)
                    .hasMessageContaining("Failed to dispatch external signal 'ErrorSignal'")
                    .hasMessageContaining("process-007")
                    .hasCauseInstanceOf(RuntimeException.class);
        }
    }

    @Test
    void testDispatchWhenEventEmitterNotAvailable() {
        // Given: EventEmitter not available
        ExternalSignalEvent event = ExternalSignalEvent.builder()
                .signalName("NoEmitterSignal")
                .signalData(Map.of("data", "value"))
                .sourceProcessInstanceId("process-008")
                .correlationId("corr-008")
                .timestamp(Instant.now())
                .build();

        when(config.resolveTrigger("NoEmitterSignal")).thenReturn("no-emitter-topic");

        try (MockedStatic<EventFactoryUtils> mockedFactory = mockStatic(EventFactoryUtils.class)) {
            mockedFactory.when(() -> EventFactoryUtils.getEventEmitter("no-emitter-topic"))
                    .thenThrow(new IllegalStateException("No EventEmitter available"));

            // When/Then: Exception wrapped and thrown
            assertThatThrownBy(() -> dispatcher.dispatch(event))
                    .isInstanceOf(ExternalSignalDispatchException.class)
                    .hasMessageContaining("Failed to dispatch external signal 'NoEmitterSignal'")
                    .hasCauseInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void testResolveTriggerWithMappedSignal() {
        // Given: Mapped signal
        when(config.resolveTrigger("OrderCreated")).thenReturn("order-events");

        // When: Resolving trigger
        String trigger = dispatcher.resolveTrigger("OrderCreated");

        // Then: Mapped trigger returned
        assertThat(trigger).isEqualTo("order-events");
        verify(config).resolveTrigger("OrderCreated");
    }

    @Test
    void testResolveTriggerWithUnmappedSignal() {
        // Given: Unmapped signal
        when(config.resolveTrigger("CustomSignal")).thenReturn("kogito-external-signal-CustomSignal");

        // When: Resolving trigger
        String trigger = dispatcher.resolveTrigger("CustomSignal");

        // Then: Default trigger returned
        assertThat(trigger).isEqualTo("kogito-external-signal-CustomSignal");
        verify(config).resolveTrigger("CustomSignal");
    }

    @Test
    void testResolveTriggerWithNullSignalName() {
        // When/Then: Exception thrown
        assertThatThrownBy(() -> dispatcher.resolveTrigger(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Signal name cannot be null or empty");
    }

    @Test
    void testResolveTriggerWithEmptySignalName() {
        // When/Then: Exception thrown
        assertThatThrownBy(() -> dispatcher.resolveTrigger("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Signal name cannot be null or empty");
    }

    @Test
    void testCloudEventTypeGeneration() {
        // Given: Event with specific signal name
        ExternalSignalEvent event = ExternalSignalEvent.builder()
                .signalName("PaymentProcessed")
                .signalData(Map.of("paymentId", "PAY-123"))
                .sourceProcessInstanceId("process-009")
                .correlationId("corr-009")
                .timestamp(Instant.now())
                .build();

        when(config.resolveTrigger("PaymentProcessed")).thenReturn("payment-topic");

        try (MockedStatic<EventFactoryUtils> mockedFactory = mockStatic(EventFactoryUtils.class)) {
            mockedFactory.when(() -> EventFactoryUtils.getEventEmitter("payment-topic"))
                    .thenReturn(eventEmitter);

            // When: Dispatching event
            dispatcher.dispatch(event);

            // Then: CloudEvent type follows convention
            ArgumentCaptor<DataEvent> dataEventCaptor = ArgumentCaptor.forClass(DataEvent.class);
            verify(eventEmitter).emit(dataEventCaptor.capture());

            DataEvent<?> capturedEvent = dataEventCaptor.getValue();
            assertThat(capturedEvent.getType()).isEqualTo("org.kie.kogito.signal.external.PaymentProcessed");
        }
    }

    @Test
    void testCloudEventSourceGeneration() {
        // Given: Event with process instance ID
        ExternalSignalEvent event = ExternalSignalEvent.builder()
                .signalName("TestSignal")
                .signalData(null)
                .sourceProcessInstanceId("my-process-instance-123")
                .correlationId("corr-010")
                .timestamp(Instant.now())
                .build();

        when(config.resolveTrigger("TestSignal")).thenReturn("test-topic");

        try (MockedStatic<EventFactoryUtils> mockedFactory = mockStatic(EventFactoryUtils.class)) {
            mockedFactory.when(() -> EventFactoryUtils.getEventEmitter("test-topic"))
                    .thenReturn(eventEmitter);

            // When: Dispatching event
            dispatcher.dispatch(event);

            // Then: CloudEvent source follows convention
            ArgumentCaptor<DataEvent> dataEventCaptor = ArgumentCaptor.forClass(DataEvent.class);
            verify(eventEmitter).emit(dataEventCaptor.capture());

            DataEvent<?> capturedEvent = dataEventCaptor.getValue();
            assertThat(capturedEvent.getSource()).isEqualTo(URI.create("kogito://process/my-process-instance-123"));
        }
    }

    @Test
    void testCloudEventIdMatchesCorrelationId() {
        // Given: Event with correlation ID
        ExternalSignalEvent event = ExternalSignalEvent.builder()
                .signalName("TestSignal")
                .signalData(null)
                .sourceProcessInstanceId("process-011")
                .correlationId("unique-correlation-id-456")
                .timestamp(Instant.now())
                .build();

        when(config.resolveTrigger("TestSignal")).thenReturn("test-topic");

        try (MockedStatic<EventFactoryUtils> mockedFactory = mockStatic(EventFactoryUtils.class)) {
            mockedFactory.when(() -> EventFactoryUtils.getEventEmitter("test-topic"))
                    .thenReturn(eventEmitter);

            // When: Dispatching event
            dispatcher.dispatch(event);

            // Then: CloudEvent ID matches correlation ID
            ArgumentCaptor<DataEvent> dataEventCaptor = ArgumentCaptor.forClass(DataEvent.class);
            verify(eventEmitter).emit(dataEventCaptor.capture());

            DataEvent<?> capturedEvent = dataEventCaptor.getValue();
            assertThat(capturedEvent.getId()).isEqualTo("unique-correlation-id-456");
        }
    }

    @Test
    void testComplexDataPayloadPreservation() {
        // Given: Event with complex nested data
        Map<String, Object> complexData = new HashMap<>();
        complexData.put("customer", Map.of("id", "CUST-123", "name", "John Doe"));
        complexData.put("items", java.util.Arrays.asList(
                Map.of("sku", "ITEM-1", "quantity", 2),
                Map.of("sku", "ITEM-2", "quantity", 1)));
        complexData.put("total", 299.99);

        ExternalSignalEvent event = ExternalSignalEvent.builder()
                .signalName("OrderPlaced")
                .signalData(complexData)
                .sourceProcessInstanceId("process-012")
                .correlationId("corr-012")
                .timestamp(Instant.now())
                .build();

        when(config.resolveTrigger("OrderPlaced")).thenReturn("order-topic");

        try (MockedStatic<EventFactoryUtils> mockedFactory = mockStatic(EventFactoryUtils.class)) {
            mockedFactory.when(() -> EventFactoryUtils.getEventEmitter("order-topic"))
                    .thenReturn(eventEmitter);

            // When: Dispatching event
            dispatcher.dispatch(event);

            // Then: Complex data preserved
            ArgumentCaptor<DataEvent> dataEventCaptor = ArgumentCaptor.forClass(DataEvent.class);
            verify(eventEmitter).emit(dataEventCaptor.capture());

            DataEvent<?> capturedEvent = dataEventCaptor.getValue();
            ExternalSignalEvent eventData = (ExternalSignalEvent) capturedEvent.getData();
            assertThat(eventData.getSignalData()).isEqualTo(complexData);
        }
    }

    @Test
    void testMultipleDispatchCalls() {
        // Given: Multiple events
        ExternalSignalEvent event1 = ExternalSignalEvent.builder()
                .signalName("Signal1")
                .signalData(Map.of("id", "1"))
                .sourceProcessInstanceId("process-013")
                .correlationId("corr-013")
                .timestamp(Instant.now())
                .build();

        ExternalSignalEvent event2 = ExternalSignalEvent.builder()
                .signalName("Signal2")
                .signalData(Map.of("id", "2"))
                .sourceProcessInstanceId("process-014")
                .correlationId("corr-014")
                .timestamp(Instant.now())
                .build();

        when(config.resolveTrigger("Signal1")).thenReturn("topic1");
        when(config.resolveTrigger("Signal2")).thenReturn("topic2");

        try (MockedStatic<EventFactoryUtils> mockedFactory = mockStatic(EventFactoryUtils.class)) {
            mockedFactory.when(() -> EventFactoryUtils.getEventEmitter("topic1"))
                    .thenReturn(eventEmitter);
            mockedFactory.when(() -> EventFactoryUtils.getEventEmitter("topic2"))
                    .thenReturn(eventEmitter);

            // When: Dispatching multiple events
            dispatcher.dispatch(event1);
            dispatcher.dispatch(event2);

            // Then: Both events emitted
            verify(eventEmitter, times(2)).emit(any(DataEvent.class));
        }
    }

    @Test
    void testDispatchWithConfigurationFromMap() {
        // Given: Dispatcher with map-based config
        Map<String, String> configMap = new HashMap<>();
        configMap.put("kogito.external-signals.mapping.OrderCreated", "order-events");
        configMap.put("kogito.external-signals.default-prefix", "my-app-signal");

        ExternalSignalConfig mapConfig = new ExternalSignalConfigImpl(configMap);
        DefaultExternalSignalDispatcher mapDispatcher = new DefaultExternalSignalDispatcher(mapConfig);

        ExternalSignalEvent event = ExternalSignalEvent.builder()
                .signalName("OrderCreated")
                .signalData(Map.of("orderId", "123"))
                .sourceProcessInstanceId("process-015")
                .correlationId("corr-015")
                .timestamp(Instant.now())
                .build();

        try (MockedStatic<EventFactoryUtils> mockedFactory = mockStatic(EventFactoryUtils.class)) {
            mockedFactory.when(() -> EventFactoryUtils.getEventEmitter("order-events"))
                    .thenReturn(eventEmitter);

            // When: Dispatching event
            mapDispatcher.dispatch(event);

            // Then: Event emitted successfully
            verify(eventEmitter).emit(any(DataEvent.class));
        }
    }
}

// Made with Bob
