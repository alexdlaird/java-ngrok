/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import java.util.Map;

/**
 * An object that represents webhook signature verification for a
 * {@link CreateTunnel}.
 */
public class TunnelVerifyWebhook {

    private final String provider;
    private final String secret;

    private TunnelVerifyWebhook(final Builder builder) {
        this.provider = builder.provider;
        this.secret = builder.secret;
    }

    /**
     * Get the provider.
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Get the secret.
     */
    public String getSecret() {
        return secret;
    }

    /**
     * Builder for a {@link TunnelVerifyWebhook}.
     */
    public static class Builder {

        private String provider;
        private String secret;

        /**
         * Construct a TunnelVerifyWebhook Builder.
         */
        public Builder() {
        }

        /**
         * Construct a TunnelVerifyWebhook Builder from tunnel definition of <code>verify_webhook</code>.
         *
         * @param tunnelVerifyWebhookDefinitions The map of Tunnel OAuth attributes.
         */
        public Builder(Map<String, Object> tunnelVerifyWebhookDefinitions) {
            if (tunnelVerifyWebhookDefinitions.containsKey("provider")) {
                this.provider = (String) tunnelVerifyWebhookDefinitions.get("provider");
            }
            if (tunnelVerifyWebhookDefinitions.containsKey("secret")) {
                this.secret = (String) tunnelVerifyWebhookDefinitions.get("secret");
            }
        }

        /**
         * The signature provider.
         */
        public TunnelVerifyWebhook.Builder withProvider(final String provider) {
            this.provider = provider;
            return this;
        }

        /**
         * The signature secret.
         */
        public TunnelVerifyWebhook.Builder withSecret(final String secret) {
            this.secret = secret;
            return this;
        }

        /**
         * Build the {@link TunnelVerifyWebhook}.
         */
        public TunnelVerifyWebhook build() {
            return new TunnelVerifyWebhook(this);
        }
    }
}
