/*
 * Copyright (c) 2021 Alex Laird
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
