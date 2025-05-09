/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import com.google.gson.Gson;
import java.util.Map;

/**
 * A response from the <code>ngrok</code> API.
 */
public class ApiResponse {

    private final String status;

    private final Map data;

    protected static final Gson gson = new Gson();

    /**
     * Construct an API response.
     *
     * @param status The description of the response.
     * @param data   The parsed API response.
     */
    private ApiResponse(final String status,
                        final Map data) {

        this.status = status;
        this.data = data;
    }

    /**
     * Construct an object from a response body.
     *
     * @param body The response body to be parsed.
     * @return A constructed object.
     */
    public static ApiResponse fromBody(final String body) {
        final int jsonStartsIndex = body.indexOf("{");

        if (jsonStartsIndex < 0) {
            return new ApiResponse(body, null);
        } else {
            return new ApiResponse(body.substring(0, jsonStartsIndex),
                gson.fromJson(body.substring(jsonStartsIndex), Map.class));
        }
    }

    /**
     * The description of the response.
     */
    public String getStatus() {
        return status;
    }

    /**
     * The parsed API response.
     */
    public Map getData() {
        return data;
    }
}
