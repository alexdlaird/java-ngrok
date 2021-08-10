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

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

/**
 * A simple client for executing HTTP requests.
 */
public interface HttpClient {
    /**
     * Perform GET operation against an endpoint.
     *
     * @param url               The URL relative to the base URL on which to perform the operation.
     * @param parameters        An arbitrary number of parameters to add to the URL.
     * @param additionalHeaders Additional headers for the request.
     * @return The results of the query.
     */
    Response get(final String url, final List<Parameter> parameters,
                 final Map<String, String> additionalHeaders);

    /**
     * Perform POST operation against an endpoint.
     *
     * @param url               The URL relative to the base URL on which to perform the operation.
     * @param request              The element to be serialized into the request body.
     * @param parameters        An arbitrary number of parameters to add to the URL.
     * @param additionalHeaders Additional headers for the request.
     * @return The results of the query.
     */
    Response post(final String url, final Request request, final List<Parameter> parameters,
                  final Map<String, String> additionalHeaders);

    /**
     * Perform PUT operation against an endpoint.
     *
     * @param url               The URL relative to the base URL on which to perform the operation.
     * @param request              The element to be serialized into the request body.
     * @param parameters        An arbitrary number of parameters to add to the URL.
     * @param additionalHeaders Additional headers for the request.
     * @return The results of the query.
     */
    Response put(final String url, final Request request, final List<Parameter> parameters,
                 final Map<String, String> additionalHeaders);

    /**
     * Perform DELETE operation against an endpoint.
     *
     * @param url               The URL relative to the base URL on which to perform the operation.
     * @param parameters        An arbitrary number of parameters to add to the URL.
     * @param additionalHeaders Additional headers for the request.
     * @return The results of the query.
     */
    Response delete(final String url, final List<Parameter> parameters,
                    final Map<String, String> additionalHeaders);

    /**
     * Override this method if you could like to extend {@link DefaultHttpClient} and perform customer HTTP operations
     * before {@link HttpURLConnection#connect()} is called on the instance of the passed in connection.
     *
     * @param httpUrlConnection The URL connection to modify.
     */
    default void modifyConnection(final HttpURLConnection httpUrlConnection) {}

    class HttpClientException extends RuntimeException {
        public HttpClientException(final String msg) {
            super(msg);
        }

        public HttpClientException(final String msg, final Exception cause) {
            super(msg, cause);
        }
    }

    class HttpClientTokenExpiredException extends HttpClientException {
        public HttpClientTokenExpiredException(final String msg) {
            super(msg);
        }
    }
}
