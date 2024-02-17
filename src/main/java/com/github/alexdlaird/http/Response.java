/*
 * Copyright (c) 2023 Alex Laird
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
    public Response(final int statusCode, final T body, final String bodyRaw, final Map<String, List<String>> headerFields) {
        this.statusCode = statusCode;
        this.body = body;
        this.bodyRaw = bodyRaw;
        this.headerFields = headerFields;
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
