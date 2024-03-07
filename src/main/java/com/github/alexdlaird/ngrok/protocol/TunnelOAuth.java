/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import java.util.List;
import java.util.Map;

/**
 * An object that represents the OAuth configuration for a {@link com.github.alexdlaird.ngrok.protocol.CreateTunnel}.
 */
public class TunnelOAuth {

    private final String provider;
    private final List<String> scopes;
    private final List<String> allowEmails;
    private final List<String> allowDomains;

    private TunnelOAuth(final Builder builder) {
        this.provider = builder.provider;
        this.scopes = builder.scopes;
        this.allowDomains = builder.allowDomains;
        this.allowEmails = builder.allowEmails;
    }

    /**
     * Get the OAuth provider.
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Get the list of OAuth scopes.
     */
    public List<String> getScopes() {
        return scopes;
    }

    /**
     * Get the list of OAuth allowed emails.
     */
    public List<String> getAllowEmails() {
        return allowEmails;
    }

    /**
     * Get the list of OAuth allowed domains.
     */
    public List<String> getAllowDomains() {
        return allowDomains;
    }

    /**
     * Builder for OAuth configuration that conforms to
     * <a href="https://ngrok.com/docs/secure-tunnels/ngrok-agent/reference/config/#tunnel-definitions" target="_blank"><code>ngrok</code>'s tunnel definition</a>.
     * See docs for that class for example usage.
     */
    public static class Builder {

        private String provider;
        private List<String> scopes;
        private List<String> allowEmails;
        private List<String> allowDomains;

        /**
         * Default constructor for {@link TunnelOAuth.Builder}.
         */
        public Builder() {
        }

        /**
         * Constructor for {@link TunnelOAuth.Builder} to be built from <code>oauth</code>
         * portion of a tunnel definition.
         *
         * @param tunnelOAuthDefinitions The map of Tunnel OAuth attributes.
         */
        public Builder(Map<String, Object> tunnelOAuthDefinitions) {
            if (tunnelOAuthDefinitions.containsKey("provider")) {
                this.provider = (String) tunnelOAuthDefinitions.get("provider");
            }
            if (tunnelOAuthDefinitions.containsKey("scopes")) {
                this.scopes = (List<String>) tunnelOAuthDefinitions.get("scopes");
            }
            if (tunnelOAuthDefinitions.containsKey("allow_emails")) {
                this.allowEmails = (List<String>) tunnelOAuthDefinitions.get("allow_emails");
            }
            if (tunnelOAuthDefinitions.containsKey("allow_domains")) {
                this.allowDomains = (List<String>) tunnelOAuthDefinitions.get("allow_domains");
            }
        }

        /**
         * The OAuth provider. This setting is <b>required</b>. For a list of valid providers, see
         * <a href="https://ngrok.com/docs/cloud-edge/modules/oauth/"><code>ngrok</code>'s docs</a>.
         */
        public Builder withProvider(final String provider) {
            this.provider = provider;
            return this;
        }

        /**
         * The list of OAuth scopes.
         */
        public Builder withScopes(final List<String> scopes) {
            this.scopes = scopes;
            return this;
        }

        /**
         * The list of allowed OAuth emails.
         */
        public Builder withAllowEmails(final List<String> emails) {
            this.allowEmails = emails;
            return this;
        }

        /**
         * The list of allowed OAuth domains.
         */
        public Builder withAllowDomains(final List<String> domains) {
            this.allowDomains = domains;
            return this;
        }

        /**
         * Build the {@link TunnelOAuth}.
         */
        public TunnelOAuth build() {
            return new TunnelOAuth(this);
        }
    }
}
