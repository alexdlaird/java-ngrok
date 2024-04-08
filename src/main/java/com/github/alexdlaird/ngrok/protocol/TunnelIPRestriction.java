/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import java.util.List;
import java.util.Map;

/**
 * An object that represents IP restriction for a {@link CreateTunnel}.
 */
public class TunnelIPRestriction {

    private final List<String> allowCidrs;
    private final List<String> denyCidrs;

    private TunnelIPRestriction(final Builder builder) {
        this.allowCidrs = builder.allowCidrs;
        this.denyCidrs = builder.denyCidrs;
    }

    /**
     * Get the list of allowed CIDRs.
     */
    public List<String> getAllowCidrs() {
        return allowCidrs;
    }

    /**
     * Get the list of denied CIDRs.
     */
    public List<String> getDenyCidrs() {
        return denyCidrs;
    }

    /**
     * Builder for a {@link TunnelIPRestriction}.
     */
    public static class Builder {

        private List<String> allowCidrs;
        private List<String> denyCidrs;

        /**
         * Construct a TunnelIPRestriction Builder.
         */
        public Builder() {
        }

        /**
         * Construct a TunnelIPRestriction Builder from tunnel definition of <code>ip_restriction</code>.
         *
         * @param tunnelIPRestrictionDefinition The map of Tunnel IP restriction attributes.
         */
        public Builder(final Map<String, Object> tunnelIPRestrictionDefinition) {
            if (tunnelIPRestrictionDefinition.containsKey("allow_cidrs")) {
                this.allowCidrs = (List<String>) tunnelIPRestrictionDefinition.get("allow_cidrs");
            }
            if (tunnelIPRestrictionDefinition.containsKey("deny_cidrs")) {
                this.denyCidrs = (List<String>) tunnelIPRestrictionDefinition.get("deny_cidrs");
            }
        }

        /**
         * The list of allowed CIDRs.
         */
        public TunnelIPRestriction.Builder withAllowCidrs(final List<String> allowCidrs) {
            this.allowCidrs = allowCidrs;
            return this;
        }

        /**
         * The list of denied CIDRs.
         */
        public TunnelIPRestriction.Builder withDenyCidrs(final List<String> denyCidrs) {
            this.denyCidrs = denyCidrs;
            return this;
        }

        /**
         * Build the {@link TunnelIPRestriction}.
         */
        public TunnelIPRestriction build() {
            return new TunnelIPRestriction(this);
        }
    }
}
