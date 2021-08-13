package com.github.alexdlaird.ngrok.protocol;

import com.github.alexdlaird.ngrok.NgrokException;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.isNull;

public class CreateTunnel {
    private final String name;
    private final String proto;
    private final String addr;
    private final boolean inspect;
    private final String auth;
    private final String hostHeader;
    private final String bindTls;
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

    public String getProto() {
        return proto;
    }

    public String getBindTls() {
        return bindTls;
    }

    public static class Builder {
        private final List<String> validProtos = List.of("http", "tcp", "tls");
        private final List<String> validBindTls = List.of("true", "false", "both");

        private String name;
        private String proto = "http";
        private String addr = "80";
        private boolean inspect = true;
        private String bindTls = "both";
        private String auth;
        private String hostHeader;
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
            if (!validProtos.contains(proto)) {
                throw new NgrokException(String.format("Invalid proto %s, valid values are: %s", proto, validProtos));
            }

            this.proto = proto;
            return this;
        }

        public Builder withAddr(final String addr) {
            this.addr = addr;
            return this;
        }

        public Builder withAddr(final int addr) {
            return withAddr(String.valueOf(addr));
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

        public Builder withBindTls(final String bindTls) {
            if (!validBindTls.contains(bindTls)) {
                throw new NgrokException(String.format("Invalid bindTls %s, valid values are: %s", bindTls, validBindTls));
            }

            this.bindTls = bindTls;
            return this;
        }

        public Builder withBindTls(final boolean bindTls) {
            return withBindTls(String.valueOf(bindTls));
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
