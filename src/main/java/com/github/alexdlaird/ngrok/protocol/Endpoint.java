/*
 * Copyright (c) 2021-2026 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * An object representing an Endpoint response from <code>ngrok</code>'s API.
 */
public class Endpoint {

    @SerializedName(value = "ID", alternate = {"id"})
    private String id;
    private String name;
    private String uri;
    @SerializedName(value = "url", alternate = {"public_url"})
    private String url;
    private String proto;
    private Upstream upstream;
    private Tunnel.TunnelConfig config;
    private Boolean poolingEnabled;
    private Map<String, Object> trafficPolicy;
    private Map<String, Tunnel.Metrics> metrics;

    /**
     * Get the ID of the endpoint.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the name of the endpoint.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the relative URI of the endpoint. Falls back to <code>/api/endpoints/{name}</code> if the API response did
     * not include an explicit <code>uri</code>.
     */
    public String getUri() {
        if (isNull(uri) && nonNull(name)) {
            return String.format("/api/endpoints/%s", name);
        }

        return uri;
    }

    /**
     * Get the public URL of the endpoint.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the endpoint's public URL.
     *
     * @param url The updated public URL.
     */
    public void setUrl(final String url) {
        this.url = url;
    }

    /**
     * Get the proto of the endpoint.
     */
    public String getProto() {
        return proto;
    }

    /**
     * Set endpoint proto.
     *
     * @param proto The updated proto.
     */
    public void setProto(final String proto) {
        this.proto = proto;
    }

    /**
     * Get the upstream of the endpoint.
     */
    public Upstream getUpstream() {
        if (nonNull(upstream)) {
            return upstream;
        }
        if (nonNull(config) && nonNull(config.getAddr())) {
            return new Upstream.Builder().withUrl(config.getAddr()).build();
        }
        return null;
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
     * Get the <a href="https://ngrok.com/docs/agent/api#endpoints" target="_blank">endpoint metrics</a>.
     */
    public Map<String, Tunnel.Metrics> getMetrics() {
        return metrics;
    }

    /**
     * Set endpoint metrics.
     *
     * @param metrics The updated metrics.
     */
    public void setMetrics(final Map<String, Tunnel.Metrics> metrics) {
        this.metrics = metrics;
    }
}
