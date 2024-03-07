/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.exception;

import com.github.alexdlaird.ngrok.NgrokClient;

/**
 * Thrown from {@link NgrokClient} when an error occurs making a request to the <code>ngrok</code> web interface.
 */
public class JavaNgrokHTTPException extends JavaNgrokException {

    private final String url;
    private final int statusCode;
    private final String body;

    /**
     * An exception with a message and a root cause.
     *
     * @param message    The message describing the exception.
     * @param cause      The initial cause of the exception.
     * @param url        The URL.
     * @param statusCode The HTTP status code.
     * @param body       The response body.
     */
    public JavaNgrokHTTPException(final String message, final Throwable cause, final String url,
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
