/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import java.util.List;
import java.util.Map;

/**
 * An object that represents policy configuration for a {@link CreateTunnel}.
 */
public class TunnelPolicy {

    private final String name;
    private final List<String> expressions;
    private final TunnelPolicyActions actions;

    private TunnelPolicy(final Builder builder) {
        this.name = builder.name;
        this.expressions = builder.expressions;
        this.actions = builder.actions;
    }

    /**
     * Get the policy name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the policy expressions.
     */
    public List<String> getExpressions() {
        return expressions;
    }

    /**
     * Get the policy actions.
     */
    public TunnelPolicyActions getActions() {
        return actions;
    }

    /**
     * Builder for a {@link TunnelPolicy}.
     */
    public static class Builder {

        private String name;
        private List<String> expressions;
        private TunnelPolicyActions actions;

        /**
         * Construct a TunnelPolicy Builder.
         */
        public Builder() {
        }

        /**
         * Construct a TunnelPolicy Builder from tunnel definition of <code>policy</code>.
         *
         * @param tunnelPolicyDefinition The map of Tunnel policy attributes.
         */
        public Builder(final Map<String, Object> tunnelPolicyDefinition) {
            if (tunnelPolicyDefinition.containsKey("name")) {
                this.name = (String) tunnelPolicyDefinition.get("name");
            }
            if (tunnelPolicyDefinition.containsKey("expressions")) {
                this.expressions = (List<String>) tunnelPolicyDefinition.get("expressions");
            }
            if (tunnelPolicyDefinition.containsKey("actions")) {
                this.actions = new TunnelPolicyActions
                    .Builder((Map<String, Object>) tunnelPolicyDefinition.get("actions"))
                    .build();
            }
        }

        /**
         * The policy name.
         */
        public TunnelPolicy.Builder withName(final String name) {
            this.name = name;
            return this;
        }

        /**
         * The list of policy expressions.
         */
        public TunnelPolicy.Builder withExpressions(final List<String> expressions) {
            this.expressions = expressions;
            return this;
        }

        /**
         * The policy actions.
         */
        public TunnelPolicy.Builder withActions(final TunnelPolicyActions actions) {
            this.actions = actions;
            return this;
        }

        /**
         * Build the {@link TunnelPolicy}.
         */
        public TunnelPolicy build() {
            return new TunnelPolicy(this);
        }
    }
}
