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
import java.util.Objects;

/**
 * Represents an external signal event that is dispatched outside the process engine scope.
 * This event model is CloudEvent-compatible and contains all necessary metadata for
 * signal correlation and routing.
 * 
 * <p>
 * External signals are used when a BPMN signal throw event has an "external" scope,
 * indicating that the signal should be sent to an external system via messaging infrastructure
 * (e.g., Kafka, HTTP) rather than being processed internally by the process engine.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * {
 *     &#64;code
 *     ExternalSignalEvent event = ExternalSignalEvent.builder()
 *             .signalName("OrderApproval")
 *             .signalData(orderData)
 *             .sourceProcessInstanceId("order-123")
 *             .correlationId("signal-456")
 *             .build();
 * }
 * </pre>
 * 
 * @see ExternalSignalDispatcher
 */
public class ExternalSignalEvent {

    private String signalName;
    private Object signalData;
    private String sourceProcessInstanceId;
    private String correlationId;
    private Instant timestamp;
    private Map<String, Object> metadata;

    /**
     * Default constructor for serialization frameworks.
     */
    public ExternalSignalEvent() {
        this.metadata = new HashMap<>();
        this.timestamp = Instant.now();
    }

    /**
     * Gets the name of the signal being sent.
     * 
     * @return the signal name
     */
    public String getSignalName() {
        return signalName;
    }

    /**
     * Sets the name of the signal being sent.
     * 
     * @param signalName the signal name
     */
    public void setSignalName(String signalName) {
        this.signalName = signalName;
    }

    /**
     * Gets the data payload associated with this signal.
     * 
     * @return the signal data, may be null
     */
    public Object getSignalData() {
        return signalData;
    }

    /**
     * Sets the data payload associated with this signal.
     * 
     * @param signalData the signal data
     */
    public void setSignalData(Object signalData) {
        this.signalData = signalData;
    }

    /**
     * Gets the process instance ID that originated this signal.
     * 
     * @return the source process instance ID
     */
    public String getSourceProcessInstanceId() {
        return sourceProcessInstanceId;
    }

    /**
     * Sets the process instance ID that originated this signal.
     * 
     * @param sourceProcessInstanceId the source process instance ID
     */
    public void setSourceProcessInstanceId(String sourceProcessInstanceId) {
        this.sourceProcessInstanceId = sourceProcessInstanceId;
    }

    /**
     * Gets the correlation ID for this signal, used for request-response patterns.
     * 
     * @return the correlation ID
     */
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Sets the correlation ID for this signal.
     * 
     * @param correlationId the correlation ID
     */
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    /**
     * Gets the timestamp when this signal was created.
     * 
     * @return the timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp when this signal was created.
     * 
     * @param timestamp the timestamp
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets additional metadata associated with this signal.
     * 
     * @return the metadata map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Sets additional metadata associated with this signal.
     * 
     * @param metadata the metadata map
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    /**
     * Adds a metadata entry to this signal.
     * 
     * @param key the metadata key
     * @param value the metadata value
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    /**
     * Gets the CloudEvent type for this signal.
     * 
     * @return the CloudEvent type
     */
    public String getType() {
        return "org.kie.kogito.signal.external";
    }

    /**
     * Gets the CloudEvent source URI for this signal.
     * 
     * @return the source URI
     */
    public URI getSource() {
        return URI.create("kogito://process/" +
                (sourceProcessInstanceId != null ? sourceProcessInstanceId : "unknown"));
    }

    /**
     * Gets the CloudEvent ID (same as correlation ID).
     * 
     * @return the event ID
     */
    public String getId() {
        return correlationId;
    }

    /**
     * Creates a new builder for constructing ExternalSignalEvent instances.
     * 
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ExternalSignalEvent that = (ExternalSignalEvent) o;
        return Objects.equals(signalName, that.signalName) &&
                Objects.equals(correlationId, that.correlationId) &&
                Objects.equals(sourceProcessInstanceId, that.sourceProcessInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(signalName, correlationId, sourceProcessInstanceId);
    }

    @Override
    public String toString() {
        return "ExternalSignalEvent{" +
                "signalName='" + signalName + '\'' +
                ", sourceProcessInstanceId='" + sourceProcessInstanceId + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    /**
     * Builder for creating ExternalSignalEvent instances.
     */
    public static class Builder {
        private final ExternalSignalEvent event;

        private Builder() {
            this.event = new ExternalSignalEvent();
        }

        /**
         * Sets the signal name.
         * 
         * @param signalName the signal name
         * @return this builder
         */
        public Builder signalName(String signalName) {
            event.setSignalName(signalName);
            return this;
        }

        /**
         * Sets the signal data.
         * 
         * @param signalData the signal data
         * @return this builder
         */
        public Builder signalData(Object signalData) {
            event.setSignalData(signalData);
            return this;
        }

        /**
         * Sets the source process instance ID.
         * 
         * @param sourceProcessInstanceId the source process instance ID
         * @return this builder
         */
        public Builder sourceProcessInstanceId(String sourceProcessInstanceId) {
            event.setSourceProcessInstanceId(sourceProcessInstanceId);
            return this;
        }

        /**
         * Sets the correlation ID.
         * 
         * @param correlationId the correlation ID
         * @return this builder
         */
        public Builder correlationId(String correlationId) {
            event.setCorrelationId(correlationId);
            return this;
        }

        /**
         * Sets the timestamp.
         * 
         * @param timestamp the timestamp
         * @return this builder
         */
        public Builder timestamp(Instant timestamp) {
            event.setTimestamp(timestamp);
            return this;
        }

        /**
         * Sets the metadata map.
         * 
         * @param metadata the metadata map
         * @return this builder
         */
        public Builder metadata(Map<String, Object> metadata) {
            event.setMetadata(metadata);
            return this;
        }

        /**
         * Adds a metadata entry.
         * 
         * @param key the metadata key
         * @param value the metadata value
         * @return this builder
         */
        public Builder addMetadata(String key, Object value) {
            event.addMetadata(key, value);
            return this;
        }

        /**
         * Builds the ExternalSignalEvent instance.
         * 
         * @return the constructed event
         */
        public ExternalSignalEvent build() {
            return event;
        }
    }
}

// Made with Bob
