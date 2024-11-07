/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An object that represents the OAuth configuration for a {@link CreateTunnel}.
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
     * Builder for OAuth configuration that conforms to <a
     * href="https://ngrok.com/docs/agent/config/v2/#tunnel-configurations"
     * target="_blank"><code>ngrok</code>'s tunnel definition</a>. See docs for that class for example usage.
     */
    public static class Builder {

        private String provider;
        private List<String> scopes;
        private List<String> allowEmails;
        private List<String> allowDomains;

        /**
         * Construct TunnelOAuth Builder.
         */
        public Builder() {
        }

        /**
         * Construct a TunnelOAuth Builder from tunnel definition of <code>oauth</code>.
         *
         * @param tunnelOAuthDefinition The map of Tunnel OAuth attributes.
         */
        public Builder(final Map<String, Object> tunnelOAuthDefinition) {
            if (tunnelOAuthDefinition.containsKey("provider")) {
                this.provider = (String) tunnelOAuthDefinition.get("provider");
            }
            if (tunnelOAuthDefinition.containsKey("scopes")) {
                this.scopes = Collections.unmodifiableList(
                    (List<String>) tunnelOAuthDefinition.get("scopes")
                );
            }
            if (tunnelOAuthDefinition.containsKey("allow_emails")) {
                this.allowEmails = Collections.unmodifiableList(
                    (List<String>) tunnelOAuthDefinition.get("allow_emails")
                );
            }
            if (tunnelOAuthDefinition.containsKey("allow_domains")) {
                this.allowDomains = Collections.unmodifiableList(
                    (List<String>) tunnelOAuthDefinition.get("allow_domains")
                );
            }
        }

        /**
         * The OAuth provider. This setting is <b>required</b>. For a list of valid providers, see
         * <a href="https://ngrok.com/docs/http/oauth/"><code>ngrok</code>'s docs</a>.
         */
        public Builder withProvider(final String provider) {
            this.provider = provider;
            return this;
        }

        /**
         * The list of OAuth scopes.
         */
        public Builder withScopes(final List<String> scopes) {
            this.scopes = Collections.unmodifiableList(scopes);
            return this;
        }

        /**
         * The list of allowed OAuth emails.
         */
        public Builder withAllowEmails(final List<String> emails) {
            this.allowEmails = Collections.unmodifiableList(emails);
            return this;
        }

        /**
         * The list of allowed OAuth domains.
         */
        public Builder withAllowDomains(final List<String> domains) {
            this.allowDomains = Collections.unmodifiableList(domains);
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
