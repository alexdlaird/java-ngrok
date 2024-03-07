/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.http;

/**
 * Root exception for {@link DefaultHttpClient} interactions.
 */
public class HttpClientException extends RuntimeException {
    
    private final String url;
    private final int statusCode;
    private final String body;

    /**
     * An exception with a message and a root cause.
     *
     * @param message The message describing the exception.
     * @param cause   The initial cause of the exception.
     */
    public HttpClientException(final String message, final Exception cause) {
        super(message, cause);

        this.url = null;
        this.statusCode = -1;
        this.body = null;
    }

    /**
     * An exception with a message, root cause, and HTTP response data.
     *
     * @param message    The message describing the exception.
     * @param cause      The initial cause of the exception.
     * @param url        The URL.
     * @param statusCode The HTTP status code.
     * @param body       The response body.
     */
    public HttpClientException(final String message, final Exception cause, final String url,
                               final int statusCode, final String body) {
        super(message, cause);

        this.url = url;
        this.statusCode = statusCode;
        this.body = body;
    }

    /**
     * Get the URL.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Get the HTTP status code.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Get the response body.
     */
    public String getBody() {
        return body;
    }
}
