/*
 * Copyright (c) 2021-2026 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An object that represents the agent TLS termination configuration for a v3 {@link CreateTunnel} endpoint
 * with a <code>tls://</code> URL.
 */
public class AgentTlsTermination {

    private final String serverCertificate;
    private final String serverPrivateKey;
    private final List<String> mutualTlsCertificateAuthorities;

    private AgentTlsTermination(final Builder builder) {
        this.serverCertificate = builder.serverCertificate;
        this.serverPrivateKey = builder.serverPrivateKey;
        this.mutualTlsCertificateAuthorities = builder.mutualTlsCertificateAuthorities;
    }

    /**
     * Get the server certificate (file path or PEM data).
     */
    public String getServerCertificate() {
        return serverCertificate;
    }

    /**
     * Get the private key for the server certificate.
     */
    public String getServerPrivateKey() {
        return serverPrivateKey;
    }

    /**
     * Get the root CAs for client certificate validation.
     */
    public List<String> getMutualTlsCertificateAuthorities() {
        return mutualTlsCertificateAuthorities;
    }

    /**
     * Builder for an {@link AgentTlsTermination}.
     */
    public static class Builder {

        private String serverCertificate;
        private String serverPrivateKey;
        private List<String> mutualTlsCertificateAuthorities;

        /**
         * Construct an AgentTlsTermination Builder.
         */
        public Builder() {
        }

        /**
         * Construct an AgentTlsTermination Builder from a definition map.
         *
         * @param definition The map from which attributes will be populated.
         */
        public Builder(final Map<String, Object> definition) {
            if (definition.containsKey("server_certificate")) {
                this.serverCertificate = (String) definition.get("server_certificate");
            }
            if (definition.containsKey("server_private_key")) {
                this.serverPrivateKey = (String) definition.get("server_private_key");
            }
            if (definition.containsKey("mutual_tls_certificate_authorities")) {
                this.mutualTlsCertificateAuthorities = Collections.unmodifiableList(
                    (List<String>) definition.get("mutual_tls_certificate_authorities")
                );
            }
        }

        /**
         * The server certificate (file path or PEM data).
         */
        public Builder withServerCertificate(final String serverCertificate) {
            this.serverCertificate = serverCertificate;
            return this;
        }

        /**
         * The private key for the server certificate.
         */
        public Builder withServerPrivateKey(final String serverPrivateKey) {
            this.serverPrivateKey = serverPrivateKey;
            return this;
        }

        /**
         * The root CAs for client certificate validation.
         */
        public Builder withMutualTlsCertificateAuthorities(final List<String> mutualTlsCertificateAuthorities) {
            this.mutualTlsCertificateAuthorities = Collections.unmodifiableList(mutualTlsCertificateAuthorities);
            return this;
        }

        /**
         * Build the {@link AgentTlsTermination}.
         */
        public AgentTlsTermination build() {
            return new AgentTlsTermination(this);
        }
    }
}
