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
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A simple interface for executing HTTP requests.
 */
public interface HttpClient {
    /**
     * Perform GET operation against an endpoint.
     *
     * @param url               The URL on which to perform the operation.
     * @param parameters        An arbitrary number of parameters to add to the URL.
     * @param additionalHeaders Additional headers for the request.
     * @param clazz             The class for the Response's body.
     * @param <B>               The response body type.
     * @return The results of the query.
     */
    <B> Response<B> get(final String url, final List<Parameter> parameters,
                        final Map<String, String> additionalHeaders, final Class<B> clazz);

    /**
     * See {@link #get(String, List, Map, Class)}.
     */
    default <B> Response<B> get(final String url, final Class<B> clazz) {
        return get(url, Collections.emptyList(), Collections.emptyMap(), clazz);
    }

    /**
     * Perform POST operation against an endpoint.
     *
     * @param url               The URL on which to perform the operation.
     * @param request           The element to be serialized into the request body.
     * @param parameters        An arbitrary number of parameters to add to the URL.
     * @param additionalHeaders Additional headers for the request.
     * @param clazz             The class for the Response's body.
     * @param <R>               The Request type.
     * @param <B>               The Response body type.
     * @return The results of the query.
     */
    <R, B> Response<B> post(final String url, final R request, final List<Parameter> parameters,
                            final Map<String, String> additionalHeaders, final Class<B> clazz);

    /**
     * See {@link #post(String, Object, List, Map, Class)}.
     */
    default <R, B> Response<B> post(final String url, final R request, final Class<B> clazz) {
        return post(url, request, Collections.emptyList(), Collections.emptyMap(), clazz);
    }

    /**
     * Perform PUT operation against an endpoint.
     *
     * @param url               The URL on which to perform the operation.
     * @param request           The element to be serialized into the request body.
     * @param parameters        An arbitrary number of parameters to add to the URL.
     * @param additionalHeaders Additional headers for the request.
     * @param clazz             The class for the Response's body.
     * @param <R>               The Request type.
     * @param <B>               The Response body type.
     * @return The results of the query.
     */
    <R, B> Response<B> put(final String url, final R request, final List<Parameter> parameters,
                           final Map<String, String> additionalHeaders, final Class<B> clazz);

    /**
     * See {@link #put(String, Object, List, Map, Class)}.
     */
    default <R, B> Response<B> put(final String url, final R request, final Class<B> clazz) {
        return put(url, request, Collections.emptyList(), Collections.emptyMap(), clazz);
    }

    /**
     * Perform DELETE operation against an endpoint.
     *
     * @param url               The URL on which to perform the operation.
     * @param parameters        An arbitrary number of parameters to add to the URL.
     * @param additionalHeaders Additional headers for the request.
     * @param clazz             The class for the Response's body.
     * @param <B>               The Response body type.
     * @return The results of the query.
     */
    <B> Response<B> delete(final String url, final List<Parameter> parameters,
                           final Map<String, String> additionalHeaders, final Class<B> clazz);

    /**
     * See {@link #delete(String, List, Map, Class)}.
     */
    default Response<Map> delete(final String url, final List<Parameter> parameters,
                                 final Map<String, String> additionalHeaders) {
        return delete(url, parameters, additionalHeaders, Map.class);
    }

    /**
     * See {@link #delete(String, List, Map)}.
     */
    default Response<Map> delete(final String url) {
        return delete(url, Collections.emptyList(), Collections.emptyMap());
    }

    /**
     * Override this method if you could like to extend {@link DefaultHttpClient} and perform customer HTTP operations
     * before {@link HttpURLConnection#connect()} is called on the instance of the passed in connection.
     *
     * @param httpUrlConnection The URL connection to modify.
     */
    default void modifyConnection(final HttpURLConnection httpUrlConnection) {
    }
}
