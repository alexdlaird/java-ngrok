/*
 * Copyright (c) 2021-2026 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import com.github.alexdlaird.http.HttpClient;
import com.github.alexdlaird.ngrok.NgrokClient;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * An object that represents a <code>ngrok</code> Endpoint creation request. This object can be serialized and passed to
 * the {@link HttpClient}.
 *
 * <h2>Basic Usage</h2>
 * <pre>
 * final NgrokClient ngrokClient = new NgrokClient.Builder().build();
 *
 * final CreateEndpoint createEndpoint = new CreateEndpoint.Builder()
 *         .withName("my-endpoint")
 *         .withUpstream("http://localhost:8000")
 *         .build();
 *
 * final Endpoint endpoint = ngrokClient.connectEndpoint(createEndpoint);
 * </pre>
 *
 * @see <a href="https://ngrok.com/docs/agent/api/#endpoints" target="_blank">ngrok's Agent API: Endpoints</a>
 */
public class CreateEndpoint {

    private final String name;
    private final String url;
    private final Upstream upstream;
    private final Boolean poolingEnabled;
    private final Map<String, Object> trafficPolicy;
    private final String metadata;
    private final String description;
    private final List<String> bindings;

    private CreateEndpoint(final Builder builder) {
        this.name = builder.name;
        this.url = builder.url;
        this.upstream = builder.upstream;
        this.poolingEnabled = builder.poolingEnabled;
        this.trafficPolicy = builder.trafficPolicy;
        this.metadata = builder.metadata;
        this.description = builder.description;
        this.bindings = builder.bindings;
    }

    /**
     * Get the name of the endpoint.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the public URL for the endpoint.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Get the upstream to which the endpoint will forward traffic.
     */
    public Upstream getUpstream() {
        return upstream;
    }

    /**
     * Whether pooling is enabled on this endpoint.
     */
    public Boolean isPoolingEnabled() {
        return poolingEnabled;
    }

    /**
     * Get the traffic policy for this endpoint, as an inline object.
     */
    public Map<String, Object> getTrafficPolicy() {
        return trafficPolicy;
    }

    /**
     * Get the arbitrary user-defined metadata that will appear in the ngrok service API when listing endpoints.
     */
    public String getMetadata() {
        return metadata;
    }

    /**
     * Get the human-readable description of this endpoint.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the bindings for this endpoint.
     */
    public List<String> getBindings() {
        return bindings;
    }

    /**
     * Builder for a {@link CreateEndpoint}. See docs for that class for example usage.
     */
    public static class Builder {

        private String name;
        private String url;
        private Upstream upstream;
        private Boolean poolingEnabled;
        private Map<String, Object> trafficPolicy;
        private String metadata;
        private String description;
        private List<String> bindings;

        /**
         * Use this constructor to build a new {@link CreateEndpoint}.
         */
        public Builder() {
        }

        /**
         * Copy a {@link CreateEndpoint} in to a new Builder.
         *
         * @param createEndpoint The CreateEndpoint to copy.
         */
        public Builder(final CreateEndpoint createEndpoint) {
            this.name = createEndpoint.name;
            this.url = createEndpoint.url;
            this.upstream = createEndpoint.upstream;
            this.poolingEnabled = createEndpoint.poolingEnabled;
            this.trafficPolicy = createEndpoint.trafficPolicy;
            this.metadata = createEndpoint.metadata;
            this.description = createEndpoint.description;
            this.bindings = createEndpoint.bindings;
        }

        /**
         * A friendly name for the endpoint, or the name of a <a
         * href="https://ngrok.com/docs/agent/config/v2/#tunnel-configurations"
         * target="_blank">ngrok endpoint definition</a> defined in <code>ngrok</code>'s config file.
         */
        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        /**
         * The public URL for the endpoint.
         */
        public Builder withUrl(final String url) {
            this.url = url;
            return this;
        }

        /**
         * The upstream to which the endpoint will forward traffic.
         */
        public Builder withUpstream(final Upstream upstream) {
            this.upstream = upstream;
            return this;
        }

        /**
         * Convenience that builds an {@link Upstream} with just a URL. Defaults to <code>http1</code> protocol.
         */
        public Builder withUpstream(final String url) {
            this.upstream = new Upstream.Builder().withUrl(url).build();
            return this;
        }

        /**
         * Whether pooling is enabled on this endpoint.
         */
        public Builder withPoolingEnabled(final Boolean poolingEnabled) {
            this.poolingEnabled = poolingEnabled;
            return this;
        }

        /**
         * The traffic policy for this endpoint, as an inline object. The agent's
         * <a href="https://ngrok.com/docs/agent/api/#endpoints" target="_blank">create-endpoint API</a>
         * expects this as a parsed object (not a YAML/JSON string).
         */
        public Builder withTrafficPolicy(final Map<String, Object> trafficPolicy) {
            this.trafficPolicy = trafficPolicy;
            return this;
        }

        /**
         * Arbitrary user-defined metadata that will appear in the ngrok service API when listing endpoints.
         */
        public Builder withMetadata(final String metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * A human-readable description of this endpoint.
         */
        public Builder withDescription(final String description) {
            this.description = description;
            return this;
        }

        /**
         * The bindings for this endpoint.
         */
        public Builder withBindings(final List<String> bindings) {
            this.bindings = Collections.unmodifiableList(bindings);
            return this;
        }

        /**
         * Populate any <code>null</code> attributes (except for <code>name</code>) in this Builder with values from the
         * given <code>endpointDefinition</code>.
         *
         * @param endpointDefinition The map from which <code>null</code> attributes will be populated.
         */
        public Builder withEndpointDefinition(final Map<String, Object> endpointDefinition) {
            if (isNull(this.url) && endpointDefinition.containsKey("url")) {
                this.url = (String) endpointDefinition.get("url");
            }
            if (isNull(this.upstream) && endpointDefinition.containsKey("upstream")) {
                final Object upstreamValue = endpointDefinition.get("upstream");
                if (upstreamValue instanceof Map) {
                    this.upstream = new Upstream.Builder((Map<String, Object>) upstreamValue).build();
                } else if (upstreamValue instanceof String) {
                    this.upstream = new Upstream.Builder().withUrl((String) upstreamValue).build();
                }
            }
            if (isNull(this.poolingEnabled) && endpointDefinition.containsKey("pooling_enabled")) {
                this.poolingEnabled = Boolean.valueOf(String.valueOf(endpointDefinition.get("pooling_enabled")));
            }
            if (isNull(this.trafficPolicy) && endpointDefinition.containsKey("traffic_policy")) {
                final Object value = endpointDefinition.get("traffic_policy");
                if (value instanceof Map) {
                    this.trafficPolicy = (Map<String, Object>) value;
                }
            }
            if (isNull(this.metadata) && endpointDefinition.containsKey("metadata")) {
                this.metadata = (String) endpointDefinition.get("metadata");
            }
            if (isNull(this.description) && endpointDefinition.containsKey("description")) {
                this.description = (String) endpointDefinition.get("description");
            }
            if (isNull(this.bindings) && endpointDefinition.containsKey("bindings")) {
                this.bindings = Collections.unmodifiableList((List<String>) endpointDefinition.get("bindings"));
            }

            return this;
        }

        /**
         * Build the {@link CreateEndpoint}.
         *
         * @throws IllegalArgumentException <code>upstream</code> was not set.
         */
        public CreateEndpoint build() {
            if (isNull(upstream)) {
                throw new IllegalArgumentException("\"upstream\" is required to create an endpoint.");
            }
            if (nonNull(upstream.getUrl()) && upstream.getUrl().isEmpty()) {
                throw new IllegalArgumentException("\"upstream.url\" must not be empty.");
            }

            return new CreateEndpoint(this);
        }
    }
}
