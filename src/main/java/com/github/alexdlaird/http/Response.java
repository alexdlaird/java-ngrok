/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.http;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A response from the {@link HttpClient}.
 */
public class Response<T> {

    private final int statusCode;
    private final T body;
    private final String bodyRaw;
    private final Map<String, List<String>> headerFields;

    /**
     * Construct a response.
     *
     * @param statusCode   The response code.
     * @param body         The body of the response.
     * @param bodyRaw      The unparsed body of the response.
     * @param headerFields Header fields in the response.
     */
    public Response(final int statusCode, final T body, final String bodyRaw,
                    final Map<String, List<String>> headerFields) {
        this.statusCode = statusCode;
        this.body = body;
        this.bodyRaw = bodyRaw;
        this.headerFields = Collections.unmodifiableMap(headerFields);
    }

    /**
     * Get the response code.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Get the body of the response.
     */
    public T getBody() {
        return body;
    }

    /**
     * Get the raw body of the response (can be useful if parsing fails).
     */
    public String getBodyRaw() {
        return bodyRaw;
    }

    /**
     * Get the response headers.
     */
    public Map<String, List<String>> getHeaderFields() {
        return headerFields;
    }
}
