package com.github.alexdlaird.ngrok.protocol;

import java.util.List;
import java.util.Map;

/**
 * An object that represents IP restrictions for a {@link com.github.alexdlaird.ngrok.protocol.CreateTunnel}.
 */
public class TunnelIPRestrictions {
    private final List<String> allowCidrs;
    private final List<String> denyCidrs;

    public TunnelIPRestrictions(TunnelIPRestrictions.Builder builder) {
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

    public static class Builder {
        public List<String> allowCidrs;
        public List<String> denyCidrs;

        public Builder() {
        }

        public Builder(Map<String, Object> tunnelDefinitions) {
            if (tunnelDefinitions.containsKey("allow_cidrs")) {
                this.allowCidrs = (List<String>) tunnelDefinitions.get("allow_cidrs");
            }
            if (tunnelDefinitions.containsKey("deny_cidrs")) {
                this.denyCidrs = (List<String>) tunnelDefinitions.get("deny_cidrs");
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

        public TunnelIPRestrictions build() {
            return new TunnelIPRestrictions(this);
        }
    }
}
