package com.github.alexdlaird.ngrok.protocol;

import java.util.Map;

public class Tunnel {
    private String name;
    private String uri;
    private String publicUrl;
    private String proto;
    private TunnelConfig config;
    private Map<String, Metrics> metrics;

    public String getName() {
        return name;
    }

    public String getUri() {
        return uri;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public String getProto() {
        return proto;
    }

    public TunnelConfig getConfig() {
        return config;
    }

    public Map<String, Metrics> getMetrics() {
        return metrics;
    }

    public static class TunnelConfig {
        private String addr;
        private boolean inspect;

        public String getAddr() {
            return addr;
        }

        public boolean isInspect() {
            return inspect;
        }
    }

    private static class Metrics {
        private int count;
        private int gauge;
        private int rate1;
        private int rate5;
        private int rate15;
        private int p50;
        private int p90;
        private int p95;
        private int p99;

        public int getCount() {
            return count;
        }

        public int getGauge() {
            return gauge;
        }

        public int getRate1() {
            return rate1;
        }

        public int getRate5() {
            return rate5;
        }

        public int getRate15() {
            return rate15;
        }

        public int getP50() {
            return p50;
        }

        public int getP90() {
            return p90;
        }

        public int getP95() {
            return p95;
        }

        public int getP99() {
            return p99;
        }
    }
}
