/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import java.util.List;
import java.util.Map;

/**
 * An object that represents IP restrictions for a {@link CreateTunnel}.
 */
public class TunnelIPRestrictions {

    private final List<String> allowCidrs;
    private final List<String> denyCidrs;

    private TunnelIPRestrictions(final Builder builder) {
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
     * Builder for a {@link TunnelIPRestrictions}.
     */
    public static class Builder {

        private List<String> allowCidrs;
        private List<String> denyCidrs;

        /**
         * Construct a TunnelIPRestrictions Builder.
         */
        public Builder() {
        }

        /**
         * Construct a TunnelIPRestrictions Builder from tunnel definition of <code>ip_restrictions</code>.
         *
         * @param tunnelIPRestrictionsDefinitions The map of Tunnel IP restrictions attributes.
         */
        public Builder(Map<String, Object> tunnelIPRestrictionsDefinitions) {
            if (tunnelIPRestrictionsDefinitions.containsKey("allow_cidrs")) {
                this.allowCidrs = (List<String>) tunnelIPRestrictionsDefinitions.get("allow_cidrs");
            }
            if (tunnelIPRestrictionsDefinitions.containsKey("deny_cidrs")) {
                this.denyCidrs = (List<String>) tunnelIPRestrictionsDefinitions.get("deny_cidrs");
            }
        }

        /**
         * The list of allowed CIDRs.
         */
        public TunnelIPRestrictions.Builder withAllowCidrs(final List<String> allowCidrs) {
            this.allowCidrs = allowCidrs;
            return this;
        }

        /**
         * The list of denied CIDRs.
         */
        public TunnelIPRestrictions.Builder withDenyCidrs(final List<String> denyCidrs) {
            this.denyCidrs = denyCidrs;
            return this;
        }

        /**
         * Build the {@link TunnelIPRestrictions}.
         */
        public TunnelIPRestrictions build() {
            return new TunnelIPRestrictions(this);
        }
    }
}
