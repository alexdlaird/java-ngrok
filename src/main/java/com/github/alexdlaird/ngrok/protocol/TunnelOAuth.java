/*
 * Copyright (c) 2023 Alex Laird
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
     * <a href="https://ngrok.com/docs/ngrok-agent/config/#tunnel-definitions" target="_blank"><code>ngrok</code>'s tunnel definition</a>.
     * See docs for that class for example usage.
     */
    public static class Builder {
        private String provider;
        private List<String> scopes;
        private List<String> allowEmails;
        private List<String> allowDomains;

        public Builder() {
        }

        public Builder(Map<String, Object> tunnelDefinitions) {
            if (tunnelDefinitions.containsKey("provider")) {
                this.provider = (String) tunnelDefinitions.get("provider");
            }
            if (tunnelDefinitions.containsKey("scopes")) {
                this.scopes = (List<String>) tunnelDefinitions.get("scopes");
            }
            if (tunnelDefinitions.containsKey("allow_emails")) {
                this.allowEmails = (List<String>) tunnelDefinitions.get("allow_emails");
            }
            if (tunnelDefinitions.containsKey("allow_domains")) {
                this.allowDomains = (List<String>) tunnelDefinitions.get("allow_domains");
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

        public TunnelOAuth build() {
            return new TunnelOAuth(this);
        }
    }
}
