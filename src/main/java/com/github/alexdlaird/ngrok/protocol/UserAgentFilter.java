/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import java.util.List;
import java.util.Map;

/**
 * An object that represents UserAgent filter for a {@link CreateTunnel}.
 */
public class UserAgentFilter {

    private final List<String> allow;
    private final List<String> deny;

    private UserAgentFilter(final Builder builder) {
        this.allow = builder.allow;
        this.deny = builder.deny;
    }

    /**
     * Get the list of allowed UserAgent filters.
     */
    public List<String> getAllow() {
        return allow;
    }

    /**
     * Get the list of denied UserAgent filters.
     */
    public List<String> getDeny() {
        return deny;
    }

    /**
     * Builder for a {@link UserAgentFilter}.
     */
    public static class Builder {

        private List<String> allow;
        private List<String> deny;

        /**
         * Construct a UserAgentFilter Builder.
         */
        public Builder() {
        }

        /**
         * Construct a UserAgentFilter Builder from tunnel definition of <code>user_agent_filters</code>.
         *
         * @param userAgentFilterDefinitions The map of UserAgent filter attributes.
         */
        public Builder(Map<String, Object> userAgentFilterDefinitions) {
            if (userAgentFilterDefinitions.containsKey("allow")) {
                this.allow = (List<String>) userAgentFilterDefinitions.get("allow");
            }
            if (userAgentFilterDefinitions.containsKey("deny")) {
                this.deny = (List<String>) userAgentFilterDefinitions.get("deny");
            }
        }

        /**
         * The list of allowed UserAgent filters.
         */
        public UserAgentFilter.Builder withAllow(final List<String> allow) {
            this.allow = allow;
            return this;
        }

        /**
         * The list of denied UserAgent filters.
         */
        public UserAgentFilter.Builder withDeny(final List<String> deny) {
            this.deny = deny;
            return this;
        }

        /**
         * Build the {@link UserAgentFilter}.
         */
        public UserAgentFilter build() {
            return new UserAgentFilter(this);
        }
    }
}
