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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.definition.process.WorkflowElementIdentifier;
import org.kie.kogito.addons.externalsignals.ExternalSignalDispatchException;
import org.kie.kogito.addons.externalsignals.ExternalSignalDispatcher;
import org.kie.kogito.addons.externalsignals.ExternalSignalEvent;
import org.kie.kogito.internal.process.runtime.KogitoNodeInstance;
import org.kie.kogito.internal.process.workitem.KogitoWorkItem;
import org.kie.kogito.internal.process.workitem.KogitoWorkItemManager;
import org.kie.kogito.internal.process.workitem.WorkItemTransition;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comprehensive test suite for {@link ExternalSignalWorkItemHandler}.
 * 
 * <p>
 * This test class verifies the work item handler's behavior including:
 * </p>
 * <ul>
 * <li>Successful signal dispatch with all parameters</li>
 * <li>Signal dispatch with minimal parameters</li>
 * <li>Signal dispatch with optional parameters</li>
 * <li>Missing required parameter handling</li>
 * <li>Dispatcher exception handling</li>
 * <li>Work item lifecycle management</li>
 * <li>Metadata extraction and event creation</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ExternalSignalWorkItemHandlerTest {

    @Mock
    private ExternalSignalDispatcher dispatcher;

    @Mock
    private KogitoWorkItemManager workItemManager;

    @Mock
    private KogitoWorkItem workItem;

    @Mock
    private KogitoNodeInstance nodeInstance;

    private ExternalSignalWorkItemHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ExternalSignalWorkItemHandler(dispatcher);
    }

    @Test
    void testConstructorWithNullDispatcher() {
        assertThatThrownBy(() -> new ExternalSignalWorkItemHandler(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ExternalSignalDispatcher cannot be null");
    }

    @Test
    void testGetName() {
        assertThat(handler.getName()).isEqualTo("External Send Task");
    }

    @Test
    void testSuccessfulSignalDispatchWithAllParameters() {
        // Given: Work item with all parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("Signal", "OrderCreated");
        parameters.put("Data", Map.of("orderId", "123", "amount", 100.0));
        parameters.put("SignalProcessInstanceId", "target-process-123");
        parameters.put("SignalWorkItemId", "target-workitem-456");
        parameters.put("SignalDeploymentId", "deployment-789");

        when(workItem.getPhaseStatus()).thenReturn("Activated");
        when(workItem.getParameter("Signal")).thenReturn("OrderCreated");
        when(workItem.getParameter("Data")).thenReturn(Map.of("orderId", "123", "amount", 100.0));
        when(workItem.getParameter("SignalProcessInstanceId")).thenReturn("target-process-123");
        when(workItem.getParameter("SignalWorkItemId")).thenReturn("target-workitem-456");
        when(workItem.getParameter("SignalDeploymentId")).thenReturn("deployment-789");
        when(workItem.getProcessInstanceStringId()).thenReturn("source-process-001");
        when(workItem.getStringId()).thenReturn("workitem-001");
        when(workItem.getNodeInstance()).thenReturn(nodeInstance);
        when(nodeInstance.getStringId()).thenReturn("node-instance-001");
        WorkflowElementIdentifier nodeId1 = mock(WorkflowElementIdentifier.class);
        when(nodeId1.toString()).thenReturn("10");
        when(nodeInstance.getNodeId()).thenReturn(nodeId1);
        when(workItem.getId()).thenReturn(1L);

        // When: Handler activates work item
        Optional<WorkItemTransition> result = handler.activateWorkItemHandler(
                workItemManager, handler, workItem, null);

        // Then: Dispatcher called with correct event
        ArgumentCaptor<ExternalSignalEvent> eventCaptor = ArgumentCaptor.forClass(ExternalSignalEvent.class);
        verify(dispatcher).dispatch(eventCaptor.capture());

        ExternalSignalEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getSignalName()).isEqualTo("OrderCreated");
        assertThat(capturedEvent.getSignalData()).isEqualTo(Map.of("orderId", "123", "amount", 100.0));
        assertThat(capturedEvent.getSourceProcessInstanceId()).isEqualTo("source-process-001");
        assertThat(capturedEvent.getCorrelationId()).isEqualTo("workitem-001");
        assertThat(capturedEvent.getTimestamp()).isNotNull();

        // Verify metadata
        assertThat(capturedEvent.getMetadata()).containsEntry("targetProcessInstanceId", "target-process-123");
        assertThat(capturedEvent.getMetadata()).containsEntry("targetWorkItemId", "target-workitem-456");
        assertThat(capturedEvent.getMetadata()).containsEntry("targetDeploymentId", "deployment-789");
        assertThat(capturedEvent.getMetadata()).containsEntry("workItemId", "workitem-001");
        assertThat(capturedEvent.getMetadata()).containsEntry("nodeInstanceId", "node-instance-001");
        assertThat(capturedEvent.getMetadata()).containsEntry("nodeId", "10");

        // Verify work item completed
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("complete");
    }

    @Test
    void testSuccessfulSignalDispatchWithMinimalParameters() {
        // Given: Work item with only required parameters
        when(workItem.getPhaseStatus()).thenReturn("Activated");
        when(workItem.getParameter("Signal")).thenReturn("PaymentProcessed");
        when(workItem.getParameter("Data")).thenReturn(Map.of("paymentId", "PAY-456"));
        when(workItem.getParameter("SignalProcessInstanceId")).thenReturn(null);
        when(workItem.getParameter("SignalWorkItemId")).thenReturn(null);
        when(workItem.getParameter("SignalDeploymentId")).thenReturn(null);
        when(workItem.getProcessInstanceStringId()).thenReturn("process-002");
        when(workItem.getStringId()).thenReturn("workitem-002");
        when(workItem.getNodeInstance()).thenReturn(nodeInstance);
        when(nodeInstance.getStringId()).thenReturn("node-instance-002");
        WorkflowElementIdentifier nodeId2 = mock(WorkflowElementIdentifier.class);
        when(nodeId2.toString()).thenReturn("20");
        when(nodeInstance.getNodeId()).thenReturn(nodeId2);
        when(workItem.getId()).thenReturn(2L);

        // When: Handler activates work item
        Optional<WorkItemTransition> result = handler.activateWorkItemHandler(
                workItemManager, handler, workItem, null);

        // Then: Dispatcher called with correct event
        ArgumentCaptor<ExternalSignalEvent> eventCaptor = ArgumentCaptor.forClass(ExternalSignalEvent.class);
        verify(dispatcher).dispatch(eventCaptor.capture());

        ExternalSignalEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getSignalName()).isEqualTo("PaymentProcessed");
        assertThat(capturedEvent.getSignalData()).isEqualTo(Map.of("paymentId", "PAY-456"));
        assertThat(capturedEvent.getSourceProcessInstanceId()).isEqualTo("process-002");
        assertThat(capturedEvent.getCorrelationId()).isEqualTo("workitem-002");

        // Verify optional metadata not present
        assertThat(capturedEvent.getMetadata()).doesNotContainKey("targetProcessInstanceId");
        assertThat(capturedEvent.getMetadata()).doesNotContainKey("targetWorkItemId");
        assertThat(capturedEvent.getMetadata()).doesNotContainKey("targetDeploymentId");

        // Verify work item completed
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("complete");
    }

    @Test
    void testSignalDispatchWithNullData() {
        // Given: Work item with null data
        when(workItem.getPhaseStatus()).thenReturn("Activated");
        when(workItem.getParameter("Signal")).thenReturn("NotificationSent");
        when(workItem.getParameter("Data")).thenReturn(null);
        when(workItem.getParameter("SignalProcessInstanceId")).thenReturn(null);
        when(workItem.getParameter("SignalWorkItemId")).thenReturn(null);
        when(workItem.getParameter("SignalDeploymentId")).thenReturn(null);
        when(workItem.getProcessInstanceStringId()).thenReturn("process-003");
        when(workItem.getStringId()).thenReturn("workitem-003");
        when(workItem.getNodeInstance()).thenReturn(nodeInstance);
        when(nodeInstance.getStringId()).thenReturn("node-instance-003");
        WorkflowElementIdentifier nodeId3 = mock(WorkflowElementIdentifier.class);
        when(nodeId3.toString()).thenReturn("30");
        when(nodeInstance.getNodeId()).thenReturn(nodeId3);
        when(workItem.getId()).thenReturn(3L);

        // When: Handler activates work item
        Optional<WorkItemTransition> result = handler.activateWorkItemHandler(
                workItemManager, handler, workItem, null);

        // Then: Dispatcher called with null data
        ArgumentCaptor<ExternalSignalEvent> eventCaptor = ArgumentCaptor.forClass(ExternalSignalEvent.class);
        verify(dispatcher).dispatch(eventCaptor.capture());

        ExternalSignalEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getSignalName()).isEqualTo("NotificationSent");
        assertThat(capturedEvent.getSignalData()).isNull();

        // Verify work item completed
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("complete");
    }

    @Test
    void testMissingSignalParameter() {
        // Given: Work item without Signal parameter
        when(workItem.getPhaseStatus()).thenReturn("Activated");
        when(workItem.getParameter("Signal")).thenReturn(null);
        when(workItem.getId()).thenReturn(4L);

        // When: Handler activates work item
        Optional<WorkItemTransition> result = handler.activateWorkItemHandler(
                workItemManager, handler, workItem, null);

        // Then: Dispatcher NOT called
        verify(dispatcher, never()).dispatch(any());

        // Verify work item aborted
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("abort");
    }

    @Test
    void testEmptySignalParameter() {
        // Given: Work item with empty Signal parameter
        when(workItem.getPhaseStatus()).thenReturn("Activated");
        when(workItem.getParameter("Signal")).thenReturn("   ");
        when(workItem.getId()).thenReturn(5L);

        // When: Handler activates work item
        Optional<WorkItemTransition> result = handler.activateWorkItemHandler(
                workItemManager, handler, workItem, null);

        // Then: Dispatcher NOT called
        verify(dispatcher, never()).dispatch(any());

        // Verify work item aborted
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("abort");
    }

    @Test
    void testInvalidSignalParameterType() {
        // Given: Work item with non-String Signal parameter
        when(workItem.getPhaseStatus()).thenReturn("Activated");
        when(workItem.getParameter("Signal")).thenReturn(12345);
        when(workItem.getId()).thenReturn(6L);

        // When: Handler activates work item
        Optional<WorkItemTransition> result = handler.activateWorkItemHandler(
                workItemManager, handler, workItem, null);

        // Then: Dispatcher NOT called
        verify(dispatcher, never()).dispatch(any());

        // Verify work item aborted
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("abort");
    }

    @Test
    void testDispatcherThrowsException() {
        // Given: Dispatcher throws exception
        when(workItem.getParameter("Signal")).thenReturn("ErrorSignal");
        when(workItem.getParameter("Data")).thenReturn(Map.of("error", "test"));
        when(workItem.getParameter("SignalProcessInstanceId")).thenReturn(null);
        when(workItem.getParameter("SignalWorkItemId")).thenReturn(null);
        when(workItem.getParameter("SignalDeploymentId")).thenReturn(null);
        when(workItem.getProcessInstanceStringId()).thenReturn("process-007");
        when(workItem.getStringId()).thenReturn("workitem-007");
        when(workItem.getNodeInstance()).thenReturn(nodeInstance);
        when(nodeInstance.getStringId()).thenReturn("node-instance-007");
        WorkflowElementIdentifier nodeId7 = mock(WorkflowElementIdentifier.class);
        when(nodeId7.toString()).thenReturn("70");
        when(nodeInstance.getNodeId()).thenReturn(nodeId7);
        when(workItem.getId()).thenReturn(7L);
        when(workItem.getPhaseStatus()).thenReturn("Activated");

        doThrow(new ExternalSignalDispatchException("Dispatch failed"))
                .when(dispatcher).dispatch(any(ExternalSignalEvent.class));

        // When: Handler activates work item
        Optional<WorkItemTransition> result = handler.activateWorkItemHandler(
                workItemManager, handler, workItem, null);

        // Then: Work item aborted
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("abort");
    }

    @Test
    void testDispatcherThrowsRuntimeException() {
        // Given: Dispatcher throws generic runtime exception
        when(workItem.getParameter("Signal")).thenReturn("ErrorSignal");
        when(workItem.getParameter("Data")).thenReturn(null);
        when(workItem.getParameter("SignalProcessInstanceId")).thenReturn(null);
        when(workItem.getParameter("SignalWorkItemId")).thenReturn(null);
        when(workItem.getParameter("SignalDeploymentId")).thenReturn(null);
        when(workItem.getProcessInstanceStringId()).thenReturn("process-008");
        when(workItem.getStringId()).thenReturn("workitem-008");
        when(workItem.getNodeInstance()).thenReturn(nodeInstance);
        when(nodeInstance.getStringId()).thenReturn("node-instance-008");
        WorkflowElementIdentifier nodeId8 = mock(WorkflowElementIdentifier.class);
        when(nodeId8.toString()).thenReturn("80");
        when(nodeInstance.getNodeId()).thenReturn(nodeId8);
        when(workItem.getId()).thenReturn(8L);
        when(workItem.getPhaseStatus()).thenReturn("Activated");

        doThrow(new RuntimeException("Unexpected error"))
                .when(dispatcher).dispatch(any(ExternalSignalEvent.class));

        // When: Handler activates work item
        Optional<WorkItemTransition> result = handler.activateWorkItemHandler(
                workItemManager, handler, workItem, null);

        // Then: Work item aborted
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("abort");
    }

    @Test
    void testSignalNameTrimming() {
        // Given: Work item with signal name that needs trimming
        when(workItem.getPhaseStatus()).thenReturn("Activated");
        when(workItem.getParameter("Signal")).thenReturn("  TrimmedSignal  ");
        when(workItem.getParameter("Data")).thenReturn(null);
        when(workItem.getParameter("SignalProcessInstanceId")).thenReturn(null);
        when(workItem.getParameter("SignalWorkItemId")).thenReturn(null);
        when(workItem.getParameter("SignalDeploymentId")).thenReturn(null);
        when(workItem.getProcessInstanceStringId()).thenReturn("process-009");
        when(workItem.getStringId()).thenReturn("workitem-009");
        when(workItem.getNodeInstance()).thenReturn(nodeInstance);
        when(nodeInstance.getStringId()).thenReturn("node-instance-009");
        WorkflowElementIdentifier nodeId9 = mock(WorkflowElementIdentifier.class);
        when(nodeId9.toString()).thenReturn("90");
        when(nodeInstance.getNodeId()).thenReturn(nodeId9);
        when(workItem.getId()).thenReturn(9L);

        // When: Handler activates work item
        Optional<WorkItemTransition> result = handler.activateWorkItemHandler(
                workItemManager, handler, workItem, null);

        // Then: Signal name trimmed
        ArgumentCaptor<ExternalSignalEvent> eventCaptor = ArgumentCaptor.forClass(ExternalSignalEvent.class);
        verify(dispatcher).dispatch(eventCaptor.capture());

        ExternalSignalEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getSignalName()).isEqualTo("TrimmedSignal");

        // Verify work item completed
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("complete");
    }

    @Test
    void testMetadataExtraction() {
        // Given: Work item with all metadata
        when(workItem.getParameter("Signal")).thenReturn("MetadataTest");
        when(workItem.getParameter("Data")).thenReturn(null);
        when(workItem.getParameter("SignalProcessInstanceId")).thenReturn("target-proc");
        when(workItem.getParameter("SignalWorkItemId")).thenReturn("target-wi");
        when(workItem.getParameter("SignalDeploymentId")).thenReturn("target-dep");
        when(workItem.getProcessInstanceStringId()).thenReturn("source-proc");
        when(workItem.getStringId()).thenReturn("source-wi");
        when(workItem.getNodeInstance()).thenReturn(nodeInstance);
        when(nodeInstance.getStringId()).thenReturn("node-inst");
        WorkflowElementIdentifier nodeId10 = mock(WorkflowElementIdentifier.class);
        when(nodeId10.toString()).thenReturn("100");
        when(nodeInstance.getNodeId()).thenReturn(nodeId10);
        when(workItem.getId()).thenReturn(10L);
        when(workItem.getPhaseStatus()).thenReturn("Activated");

        // When: Handler activates work item
        handler.activateWorkItemHandler(workItemManager, handler, workItem, null);

        // Then: All metadata extracted correctly
        ArgumentCaptor<ExternalSignalEvent> eventCaptor = ArgumentCaptor.forClass(ExternalSignalEvent.class);
        verify(dispatcher).dispatch(eventCaptor.capture());

        ExternalSignalEvent capturedEvent = eventCaptor.getValue();
        Map<String, Object> metadata = capturedEvent.getMetadata();

        assertThat(metadata).hasSize(6);
        assertThat(metadata).containsEntry("targetProcessInstanceId", "target-proc");
        assertThat(metadata).containsEntry("targetWorkItemId", "target-wi");
        assertThat(metadata).containsEntry("targetDeploymentId", "target-dep");
        assertThat(metadata).containsEntry("workItemId", "source-wi");
        assertThat(metadata).containsEntry("nodeInstanceId", "node-inst");
        assertThat(metadata).containsEntry("nodeId", "100");
    }

    @Test
    void testEventTimestampCreation() {
        // Given: Work item with signal
        when(workItem.getParameter("Signal")).thenReturn("TimestampTest");
        when(workItem.getParameter("Data")).thenReturn(null);
        when(workItem.getParameter("SignalProcessInstanceId")).thenReturn(null);
        when(workItem.getParameter("SignalWorkItemId")).thenReturn(null);
        when(workItem.getParameter("SignalDeploymentId")).thenReturn(null);
        when(workItem.getProcessInstanceStringId()).thenReturn("process-010");
        when(workItem.getStringId()).thenReturn("workitem-010");
        when(workItem.getNodeInstance()).thenReturn(nodeInstance);
        when(nodeInstance.getStringId()).thenReturn("node-instance-010");
        WorkflowElementIdentifier nodeId11 = mock(WorkflowElementIdentifier.class);
        when(nodeId11.toString()).thenReturn("110");
        when(nodeInstance.getNodeId()).thenReturn(nodeId11);
        when(workItem.getId()).thenReturn(11L);
        when(workItem.getPhaseStatus()).thenReturn("Activated");

        Instant before = Instant.now();

        // When: Handler activates work item
        handler.activateWorkItemHandler(workItemManager, handler, workItem, null);

        Instant after = Instant.now();

        // Then: Timestamp is within expected range
        ArgumentCaptor<ExternalSignalEvent> eventCaptor = ArgumentCaptor.forClass(ExternalSignalEvent.class);
        verify(dispatcher).dispatch(eventCaptor.capture());

        ExternalSignalEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getTimestamp()).isNotNull();
        assertThat(capturedEvent.getTimestamp()).isBetween(before, after);
    }

    @Test
    void testToString() {
        String result = handler.toString();
        assertThat(result).contains("ExternalSignalWorkItemHandler");
        assertThat(result).contains("External Send Task");
    }

    @Test
    void testDefaultConstructor() {
        // When: Using default constructor
        ExternalSignalWorkItemHandler defaultHandler = new ExternalSignalWorkItemHandler();

        // Then: Handler is created successfully
        assertThat(defaultHandler).isNotNull();
        assertThat(defaultHandler.getName()).isEqualTo("External Send Task");
    }

    @Test
    void testComplexDataPayload() {
        // Given: Work item with complex nested data structure
        when(workItem.getPhaseStatus()).thenReturn("Activated");
        Map<String, Object> complexData = new HashMap<>();
        complexData.put("customer", Map.of("id", "CUST-123", "name", "John Doe"));
        complexData.put("items", java.util.Arrays.asList(
                Map.of("sku", "ITEM-1", "quantity", 2),
                Map.of("sku", "ITEM-2", "quantity", 1)));
        complexData.put("total", 299.99);

        when(workItem.getParameter("Signal")).thenReturn("OrderPlaced");
        when(workItem.getParameter("Data")).thenReturn(complexData);
        when(workItem.getParameter("SignalProcessInstanceId")).thenReturn(null);
        when(workItem.getParameter("SignalWorkItemId")).thenReturn(null);
        when(workItem.getParameter("SignalDeploymentId")).thenReturn(null);
        when(workItem.getProcessInstanceStringId()).thenReturn("process-011");
        when(workItem.getStringId()).thenReturn("workitem-011");
        when(workItem.getNodeInstance()).thenReturn(nodeInstance);
        when(nodeInstance.getStringId()).thenReturn("node-instance-011");
        WorkflowElementIdentifier nodeId12 = mock(WorkflowElementIdentifier.class);
        when(nodeId12.toString()).thenReturn("120");
        when(nodeInstance.getNodeId()).thenReturn(nodeId12);
        when(workItem.getId()).thenReturn(12L);

        // When: Handler activates work item
        Optional<WorkItemTransition> result = handler.activateWorkItemHandler(
                workItemManager, handler, workItem, null);

        // Then: Complex data preserved
        ArgumentCaptor<ExternalSignalEvent> eventCaptor = ArgumentCaptor.forClass(ExternalSignalEvent.class);
        verify(dispatcher).dispatch(eventCaptor.capture());

        ExternalSignalEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getSignalData()).isEqualTo(complexData);

        // Verify work item completed
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("complete");
    }
}

// Made with Bob
