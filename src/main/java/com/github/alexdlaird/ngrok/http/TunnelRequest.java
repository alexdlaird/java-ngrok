package com.github.alexdlaird.ngrok.http;

public class TunnelRequest {
    private String name;
    private String proto;
    private String addr;
    private boolean inspect;
    private String auth;
    private String hostHeader;
    private boolean bindTls;
    private String subdomain;
    private String hostname;
    private String crt;
    private String key;
    private String clientCas;
    private String remoteAddr;
    private String metadata;

    public TunnelRequest(final Builder builder) {
        this.name = builder.name;
        this.proto = builder.proto;
        this.addr = builder.addr;
        this.inspect = builder.inspect;
        this.auth = builder.auth;
        this.hostHeader = builder.hostHeader;
        this.bindTls = builder.bindTls;
        this.subdomain = builder.subdomain;
        this.hostname = builder.hostname;
        this.crt = builder.crt;
        this.key = builder.key;
        this.clientCas = builder.clientCas;
        this.remoteAddr = builder.remoteAddr;
        this.metadata = builder.metadata;
    }

    public static class Builder {
        private final String name;
        private final String proto;
        private final String addr;
        private boolean inspect;
        private String auth;
        private String hostHeader;
        private boolean bindTls;
        private String subdomain;
        private String hostname;
        private String crt;
        private String key;
        private String clientCas;
        private String remoteAddr;
        private String metadata;

        public Builder(final String name,
                       final String proto,
                       final String addr) {
            this.name = name;
            this.proto = proto;
            this.addr = addr;
            this.inspect = true;
        }

        public Builder withoutInspect() {
            this.inspect = false;
            return this;
        }

        public Builder setAuth(final String auth) {
            this.auth = auth;
            return this;
        }

        public Builder withHostHeader(final String hostHeader) {
            this.hostHeader = hostHeader;
            return this;
        }

        public Builder withBindTls() {
            this.bindTls = true;
            return this;
        }

        public Builder withSubdomain(final String subdomain) {
            this.subdomain = subdomain;
            return this;
        }

        public Builder withHostname(final String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder withCrt(final String crt) {
            this.crt = crt;
            return this;
        }

        public Builder withKey(final String key) {
            this.key = key;
            return this;
        }

        public Builder withClientCas(final String clientCas) {
            this.clientCas = clientCas;
            return this;
        }

        public Builder withRemoteAddr(final String remoteAddr) {
            this.remoteAddr = remoteAddr;
            return this;
        }

        public Builder withMetadata(final String metadata) {
            this.metadata = metadata;
            return this;
        }

        public TunnelRequest build() {
            return new TunnelRequest(this);
        }
    }
}
