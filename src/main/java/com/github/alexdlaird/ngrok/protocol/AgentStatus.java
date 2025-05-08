/*
 * Copyright (c) 2021-2025 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import java.util.List;

/**
 * An object representing an AgentStatus response from <code>ngrok</code>'s API.
 */
public class AgentStatus {

    private String status;
    private String agentVersion;
    private Session session;
    private String uri;

    /**
     * Get the description of the response.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Get the version of the Agent.
     */
    public String getAgentVersion() {
        return agentVersion;
    }

    /**
     * Get the {@link Session} details.
     */
    public Session getSession() {
        return session;
    }

    /**
     * Get the Agent URI.
     */
    public String getUri() {
        return uri;
    }

    /**
     * An object representing a nested Session from <code>ngrok</code>'s Agent.
     */
    public static class Session {
        private List<Leg> legs;

        /**
         * Get the list of {@link Leg}s.
         */
        public List<Leg> getLegs() {
            return legs;
        }

        /**
         * An object representing a nested Leg from <code>ngrok</code>'s Agent Session.
         */
        public static class Leg {
            private String region;
            private int latency;

            /**
             * Get the region of the leg.
             */
            public String getRegion() {
                return region;
            }

            /**
             * Get the latency of the leg.
             */
            public int getLatency() {
                return latency;
            }
        }
    }
}
