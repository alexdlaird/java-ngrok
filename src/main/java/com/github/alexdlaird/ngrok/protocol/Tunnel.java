/*
 * Copyright (c) 2021 Alex Laird
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.alexdlaird.ngrok.protocol;

import java.util.Map;

/**
 * An object representing a Tunnel response from <code>ngrok</code>'s API.
 */
public class Tunnel {

    private String name;
    private String uri;
    private String publicUrl;
    private String proto;
    private TunnelConfig config;
    private Map<String, Metrics> metrics;

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
     * Get the proto of the tunnel.
     */
    public String getProto() {
        return proto;
    }

    /**
     * Get the tunnel config.
     */
    public TunnelConfig getConfig() {
        return config;
    }

    /**
     * Get the <a href="https://ngrok.com/docs#list-tunnels" target="_blank">tunnel metrics</a>.
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

        public int getCount() {
            return count;
        }

        public int getGauge() {
            return gauge;
        }

        public double getRate1() {
            return rate1;
        }

        public double getRate5() {
            return rate5;
        }

        public double getRate15() {
            return rate15;
        }

        public double getP50() {
            return p50;
        }

        public double getP90() {
            return p90;
        }

        public double getP95() {
            return p95;
        }

        public double getP99() {
            return p99;
        }
    }
}
