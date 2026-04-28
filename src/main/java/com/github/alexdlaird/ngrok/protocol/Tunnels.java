/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import com.google.gson.annotations.SerializedName;
import java.util.List;

import static java.util.Objects.nonNull;

/**
 * An object representing a Tunnels response from <code>ngrok</code>'s API.
 */
public class Tunnels {

    @SerializedName(value = "tunnels", alternate = {"endpoints"})
    private List<Tunnel> tunnels;
    private String uri;

    /**
     * Get the list of tunnels.
     */
    public List<Tunnel> getTunnels() {
        return nonNull(tunnels) ? tunnels : List.of();
    }

    /**
     * Get the URI.
     */
    public String getUri() {
        return uri;
    }

}
