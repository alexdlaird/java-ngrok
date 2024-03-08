/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import java.util.List;
import java.util.Map;

/**
 * An object that represents header configuration for a {@link CreateTunnel}.
 */
public class TunnelHeader {

    private final List<String> add;
    private final List<String> remove;

    private TunnelHeader(final Builder builder) {
        this.add = builder.add;
        this.remove = builder.remove;
    }

    /**
     * Get the list of headers to add.
     */
    public List<String> getAdd() {
        return add;
    }

    /**
     * Get the list of headers to remove.
     */
    public List<String> getRemove() {
        return remove;
    }

    /**
     * Builder for a {@link TunnelHeader}.
     */
    public static class Builder {

        private List<String> add;
        private List<String> remove;

        /**
         * Construct a TunnelHeader Builder.
         */
        public Builder() {
        }

        /**
         * Construct a TunnelHeader Builder from tunnel definition of <code>request_header</code>
         * or <code>response_header</code>.
         *
         * @param tunnelHeaderDefinitions The map of Tunnel header attributes.
         */
        public Builder(Map<String, Object> tunnelHeaderDefinitions) {
            if (tunnelHeaderDefinitions.containsKey("add")) {
                this.add = (List<String>) tunnelHeaderDefinitions.get("add");
            }
            if (tunnelHeaderDefinitions.containsKey("remove")) {
                this.remove = (List<String>) tunnelHeaderDefinitions.get("remove");
            }
        }

        /**
         * The list of headers to add.
         */
        public TunnelHeader.Builder withAdd(final List<String> add) {
            this.add = add;
            return this;
        }

        /**
         * The list of headers to remove.
         */
        public TunnelHeader.Builder withRemove(final List<String> remove) {
            this.remove = remove;
            return this;
        }

        /**
         * Build the {@link TunnelHeader}.
         */
        public TunnelHeader build() {
            return new TunnelHeader(this);
        }
    }
}
