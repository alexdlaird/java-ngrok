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

import com.github.alexdlaird.exception.JavaNgrokException;
import com.github.alexdlaird.http.HttpClient;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.isNull;

/**
 * An object that represents a <code>ngrok</code> Tunnel creation request. This object can be serialized
 * and passed to the {@link HttpClient}.
 */
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

    private CreateTunnel(final Builder builder) {
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

    public String getName() {
        return name;
    }

    public String getProto() {
        return proto;
    }

    public String getBindTls() {
        return bindTls;
    }

    /**
     * Builder for a {@link CreateTunnel}, which can be used to construct a request that conforms to
     * <a href="https://ngrok.com/docs#tunnel-definitions"><code>ngrok</code>'s tunnel definition</a>.
     */
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

        /**
         * The name of the tunnel.
         */
        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        /**
         * A valid <a href="<https://ngrok.com/docs#tunnel-definitions>">tunnel protocol</a>, defaults to "http".
         */
        public Builder withProto(final String proto) {
            if (!validProtos.contains(proto)) {
                throw new IllegalArgumentException(String.format("Invalid proto %s, valid values are: %s", proto, validProtos));
            }

            this.proto = proto;
            return this;
        }

        /**
         * The local port to which the tunnel will forward traffic, or a
         * <a href="https://ngrok.com/docs#http-file-urls">local directory or network address</a>, defaults to "80"
         */
        public Builder withAddr(final String addr) {
            this.addr = addr;
            return this;
        }

        /**
         * See {@link #withAddr(String)}.
         */
        public Builder withAddr(final int addr) {
            return withAddr(String.valueOf(addr));
        }

        /**
         * Disable HTTP request inspection on tunnels.
         */
        public Builder withoutInspect() {
            this.inspect = false;
            return this;
        }

        /**
         * HTTP basic authentication credentials to enforce on tunneled requests
         */
        public Builder withAuth(final String auth) {
            this.auth = auth;
            return this;
        }

        /**
         * Rewrite the HTTP Host header to this value, or <code>preserve</code> to leave it unchanged.
         */
        public Builder withHostHeader(final String hostHeader) {
            this.hostHeader = hostHeader;
            return this;
        }

        /**
         * Bind an HTTPS ("true") or HTTP ("false") endpoint, defaults to "both".
         */
        public Builder withBindTls(final String bindTls) {
            if (!validBindTls.contains(bindTls)) {
                throw new IllegalArgumentException(String.format("Invalid bindTls %s, valid values are: %s", bindTls, validBindTls));
            }

            this.bindTls = bindTls;
            return this;
        }

        /**
         * See {@link #withBindTls(String)}.
         */
        public Builder withBindTls(final boolean bindTls) {
            return withBindTls(String.valueOf(bindTls));
        }

        /**
         * Subdomain name to request. If unspecified, uses the tunnel name.
         */
        public Builder withSubdomain(final String subdomain) {
            this.subdomain = subdomain;
            return this;
        }

        /**
         * Hostname to request (requires reserved name and DNS CNAME).
         */
        public Builder withHostname(final String hostname) {
            this.hostname = hostname;
            return this;
        }

        /**
         * PEM TLS certificate at this path to terminate TLS traffic before forwarding locally.
         */
        public Builder withCrt(final String crt) {
            this.crt = crt;
            return this;
        }

        /**
         * PEM TLS private key at this path to terminate TLS traffic before forwarding locally.
         */
        public Builder withKey(final String key) {
            this.key = key;
            return this;
        }

        /**
         * PEM TLS certificate authority at this path will verify incoming TLS client connection certificates.
         */
        public Builder withClientCas(final String clientCas) {
            this.clientCas = clientCas;
            return this;
        }

        /**
         * Bind the remote TCP port on the given address.
         */
        public Builder withRemoteAddr(final String remoteAddr) {
            this.remoteAddr = remoteAddr;
            return this;
        }

        /**
         * Arbitrary user-defined metadata that will appear in the ngrok service API when listing tunnels.
         */
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
