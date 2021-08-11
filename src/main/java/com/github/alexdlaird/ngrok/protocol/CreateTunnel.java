package com.github.alexdlaird.ngrok.protocol;

import java.util.UUID;

import static java.util.Objects.isNull;

public class CreateTunnel {
    private final String name;
    private final String proto;
    private final String addr;
    private final boolean inspect;
    private final String auth;
    private final String hostHeader;
    private final boolean bindTls;
    private final String subdomain;
    private final String hostname;
    private final String crt;
    private final String key;
    private final String clientCas;
    private final String remoteAddr;
    private final String metadata;

    public CreateTunnel(final Builder builder) {
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
        private String name = null;
        private String proto = "http";
        private String addr = "80";
        private boolean inspect = true;
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

        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public Builder withProto(final String proto) {
            this.proto = proto;
            return this;
        }

        public Builder withAddr(final String addr) {
            this.addr = addr;
            return this;
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

        public CreateTunnel build() {
            if (isNull(name)) {
                if (!addr.startsWith("file://")) {
                    name = String.format("%s-%s-%s", proto, addr, UUID.randomUUID());
                } else {
                    name = String.format("%s-file-%s", proto, UUID.randomUUID());
                }
            }

            return new CreateTunnel(this);
        }
    }
}
