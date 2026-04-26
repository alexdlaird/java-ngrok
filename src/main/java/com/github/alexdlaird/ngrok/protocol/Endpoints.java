/*
 * Copyright (c) 2021-2026 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import com.google.gson.annotations.SerializedName;
import java.util.List;

import static java.util.Objects.nonNull;

/**
 * An object representing an Endpoints response from <code>ngrok</code>'s API.
 */
public class Endpoints {

    @SerializedName(value = "endpoints", alternate = {"tunnels"})
    private List<Endpoint> endpoints;
    private String uri;

    /**
     * Get the list of endpoints. The agent endpoint API at the time of writing returns this as
     * <code>tunnels</code>; both keys are accepted on deserialization.
     */
    public List<Endpoint> getEndpoints() {
        return nonNull(endpoints) ? endpoints : List.of();
    }

    /**
     * Get the URI.
     */
    public String getUri() {
        return uri;
    }
}
