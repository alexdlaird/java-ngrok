package com.github.alexdlaird.ngrok.http;

import java.util.List;
import java.util.Map;

public class TunnelResponse {
    private List<Tunnel> tunnels;

    private String uri;

    private static class Tunnel {
        private String name;
        private String uri;
        private String publicUrl;
        private String proto;
        private TunnelConfig config;
        private Map<String, Metrics> metrics;

        private static class TunnelConfig {
            private String addr;
            private boolean inspect;
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
        }
    }
}
