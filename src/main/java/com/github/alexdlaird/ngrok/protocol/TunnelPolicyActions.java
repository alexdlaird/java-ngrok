/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import java.util.Map;

/**
 * An object that represents policy actions configuration for a {@link CreateTunnel}.
 */
public class TunnelPolicyActions {

    private final String type;
    private final String config;

    private TunnelPolicyActions(final Builder builder) {
        this.type = builder.type;
        this.config = builder.config;
    }

    /**
     * Get the action type.
     */
    public String getType() {
        return type;
    }

    /**
     * Get the action config.
     */
    public String getConfig() {
        return config;
    }

    /**
     * Builder for a {@link TunnelPolicyActions}.
     */
    public static class Builder {

        private String type;
        private String config;

        /**
         * Construct a TunnelPolicy Builder.
         */
        public Builder() {
        }

        /**
         * Construct a TunnelPolicyActions Builder from tunnel definition of <code>policy.inbound.actions</code> or
         * <code>policy.outbound.actions</code>.
         *
         * @param tunnelPolicyActionsDefinition The map of Tunnel policy action attributes.
         */
        public Builder(final Map<String, Object> tunnelPolicyActionsDefinition) {
            if (tunnelPolicyActionsDefinition.containsKey("type")) {
                this.type = (String) tunnelPolicyActionsDefinition.get("type");
            }
            if (tunnelPolicyActionsDefinition.containsKey("config")) {
                this.config = (String) tunnelPolicyActionsDefinition.get("config");
            }
        }

        /**
         * The action type.
         */
        public TunnelPolicyActions.Builder withType(final String type) {
            this.type = type;
            return this;
        }

        /**
         * The action config.
         */
        public TunnelPolicyActions.Builder withConfig(final String config) {
            this.config = config;
            return this;
        }

        /**
         * Build the {@link TunnelPolicyActions}.
         */
        public TunnelPolicyActions build() {
            return new TunnelPolicyActions(this);
        }
    }
}
