/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import java.util.List;

/**
 * An object representing a Tunnels response from <code>ngrok</code>'s API.
 */
public class Tunnels {

    private List<Tunnel> tunnels;
    private String uri;

    /**
     * Get the list tunnels.
     */
    public List<Tunnel> getTunnels() {
        return tunnels;
    }

    /**
     * Get the URI.
     */
    public String getUri() {
        return uri;
    }

}
