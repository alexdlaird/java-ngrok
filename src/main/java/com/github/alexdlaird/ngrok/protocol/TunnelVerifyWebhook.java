package com.github.alexdlaird.ngrok.protocol;

import java.util.Map;

/**
 * An object that represents webhook signature verification for a {@link com.github.alexdlaird.ngrok.protocol.CreateTunnel}.
 */
public class TunnelVerifyWebhook {
    private final String provider;
    private final String secret;

    public TunnelVerifyWebhook(TunnelVerifyWebhook.Builder builder) {
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

    public static class Builder {
        public String provider;
        public String secret;

        public Builder() {
        }

        public Builder(Map<String, Object> tunnelDefinitions) {
            if (tunnelDefinitions.containsKey("provider")) {
                this.provider = (String) tunnelDefinitions.get("provider");
            }
            if (tunnelDefinitions.containsKey("secret")) {
                this.secret = (String) tunnelDefinitions.get("secret");
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

        public TunnelVerifyWebhook build() {
            return new TunnelVerifyWebhook(this);
        }
    }
}
