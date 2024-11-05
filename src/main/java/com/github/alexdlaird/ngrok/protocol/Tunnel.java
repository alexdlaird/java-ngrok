/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

/**
 * An object representing a Tunnel response from <code>ngrok</code>'s API.
 */
public class Tunnel {

    @SerializedName("ID")
    private String id;
    private String name;
    private String uri;
    private String publicUrl;
    private String proto;
    private TunnelConfig config;
    private Map<String, Metrics> metrics;

    /**
     * Get the ID of the tunnel.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the name of the tunnel.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the relative URI of the tunnel.
     */
    public String getUri() {
        return uri;
    }

    /**
     * Get the public URL of the tunnel.
     */
    public String getPublicUrl() {
        return publicUrl;
    }

    /**
     * Set the tunnel's public URL.
     *
     * @param publicUrl The updated public URL.
     */
    public void setPublicUrl(final String publicUrl) {
        this.publicUrl = publicUrl;
    }

    /**
     * Get the proto of the tunnel.
     */
    public String getProto() {
        return proto;
    }

    /**
     * Set tunnel proto.
     *
     * @param proto The updated proto.
     */
    public void setProto(final String proto) {
        this.proto = proto;
    }

    /**
     * Get the tunnel config.
     */
    public TunnelConfig getConfig() {
        return config;
    }

    /**
     * Get the <a href="https://ngrok.com/docs/agent/api#list-tunnels" target="_blank">tunnel metrics</a>.
     */
    public Map<String, Metrics> getMetrics() {
        return metrics;
    }

    /**
     * Set tunnel metrics.
     *
     * @param metrics The updated metrics.
     */
    public void setMetrics(final Map<String, Metrics> metrics) {
        this.metrics = metrics;
    }

    /**
     * An object representing a nested TunnelConfig from <code>ngrok</code>'s API.
     */
    public static class TunnelConfig {

        private String addr;
        private boolean inspect;

        /**
         * Get the local addr to which the tunnel forwards traffic.
         */
        public String getAddr() {
            return addr;
        }

        /**
         * Whether tunnel traffic is being inspected.
         */
        public boolean isInspect() {
            return inspect;
        }
    }

    /**
     * An object representing a nested Metrics from <code>ngrok</code>'s API.
     */
    public static class Metrics {

        private int count;
        private int gauge;
        private double rate1;
        private double rate5;
        private double rate15;
        private double p50;
        private double p90;
        private double p95;
        private double p99;

        /**
         * Get the count metric.
         */
        public int getCount() {
            return count;
        }

        /**
         * Get the gauge metric.
         */
        public int getGauge() {
            return gauge;
        }

        /**
         * Get the rate1 metric.
         */
        public double getRate1() {
            return rate1;
        }

        /**
         * Get the rate5 metric.
         */
        public double getRate5() {
            return rate5;
        }

        /**
         * Get the rate15 metric.
         */
        public double getRate15() {
            return rate15;
        }

        /**
         * Get the p50 metric.
         */
        public double getP50() {
            return p50;
        }

        /**
         * Get the p90 metric.
         */
        public double getP90() {
            return p90;
        }

        /**
         * Get the p95 metric.
         */
        public double getP95() {
            return p95;
        }

        /**
         * Get the p99 metric.
         */
        public double getP99() {
            return p99;
        }
    }
}
