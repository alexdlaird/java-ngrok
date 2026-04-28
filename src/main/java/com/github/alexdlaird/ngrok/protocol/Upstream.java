/*
 * Copyright (c) 2021-2026 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import java.util.Map;

import static java.util.Objects.isNull;

/**
 * An object representing the upstream of a <code>ngrok</code> tunnel.
 *
 * @see CreateTunnel
 * @see Tunnel
 */
public class Upstream {

    private String url;
    private String protocol;
    private String proxyProtocol;

    private Upstream() {
    }

    private Upstream(final Builder builder) {
        this.url = builder.url;
        this.protocol = builder.protocol;
        this.proxyProtocol = builder.proxyProtocol;
    }

    /**
     * Get the upstream URL to which the tunnel forwards traffic.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Get the upstream protocol (e.g., <code>http1</code>, <code>http2</code>).
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Get the PROXY protocol version (e.g., <code>1</code>, <code>2</code>).
     */
    public String getProxyProtocol() {
        return proxyProtocol;
    }

    /**
     * Builder for an {@link Upstream}, see docs for that class for example usage.
     */
    public static class Builder {

        private String url;
        private String protocol;
        private String proxyProtocol;

        /**
         * Use this constructor to build a new {@link Upstream}.
         */
        public Builder() {
        }

        /**
         * Populate this Builder from an upstream definition map.
         *
         * @param upstreamDefinition The map from which attributes will be populated.
         */
        public Builder(final Map<String, Object> upstreamDefinition) {
            if (upstreamDefinition.containsKey("url")) {
                this.url = (String) upstreamDefinition.get("url");
            }
            if (upstreamDefinition.containsKey("protocol")) {
                this.protocol = (String) upstreamDefinition.get("protocol");
            }
            if (upstreamDefinition.containsKey("proxy_protocol")) {
                this.proxyProtocol = (String) upstreamDefinition.get("proxy_protocol");
            }
        }

        /**
         * The upstream URL to which the tunnel will forward traffic.
         */
        public Builder withUrl(final String url) {
            this.url = url;
            return this;
        }

        /**
         * The upstream protocol (e.g., <code>http1</code>, <code>http2</code>).
         */
        public Builder withProtocol(final String protocol) {
            this.protocol = protocol;
            return this;
        }

        /**
         * The PROXY protocol version (e.g., <code>1</code>, <code>2</code>).
         */
        public Builder withProxyProtocol(final String proxyProtocol) {
            this.proxyProtocol = proxyProtocol;
            return this;
        }

        /**
         * Build the {@link Upstream}.
         *
         * @throws IllegalArgumentException <code>url</code> was not set.
         */
        public Upstream build() {
            if (isNull(url)) {
                throw new IllegalArgumentException("\"upstream.url\" is required.");
            }

            return new Upstream(this);
        }
    }
}
