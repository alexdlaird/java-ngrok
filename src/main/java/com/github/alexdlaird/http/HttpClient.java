/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.http;

import java.net.HttpURLConnection;
import java.nio.file.Path;
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
     * @throws HttpClientException An error has occurred while executing the HTTP request.
     */
    <B> Response<B> get(final String url, final List<Parameter> parameters,
                        final Map<String, String> additionalHeaders, final Class<B> clazz);

    /**
     * See {@link #get(String, List, Map, Class)}.
     */
    default <B> Response<B> get(final String url, final Class<B> clazz) {
        return get(url, List.of(), Map.of(), clazz);
    }

    /**
     * Perform GET operation that downloads a file to the given path.
     *
     * @param url               The URL on which to perform the operation.
     * @param parameters        An arbitrary number of parameters to add to the URL.
     * @param additionalHeaders Additional headers for the request.
     * @param dest              The destination to which the file will be downloaded.
     * @param retries           The retry attempt index, if download fails.
     * @throws InterruptedException The thread is interrupted during retries.
     * @throws HttpClientException An error has occurred while executing the HTTP request.
     */
    void get(final String url, final List<Parameter> parameters,
             final Map<String, String> additionalHeaders, final Path dest, final int retries)
        throws InterruptedException;

    /**
     * See {@link #get(String, List, Map, Path, int)}.
     */
    default void get(final String url, final List<Parameter> parameters,
                     final Map<String, String> additionalHeaders, final Path dest) throws InterruptedException {
        get(url, parameters, additionalHeaders, dest, 0);
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
     * @throws HttpClientException An error has occurred while executing the HTTP request.
     */
    <R, B> Response<B> post(final String url, final R request, final List<Parameter> parameters,
                            final Map<String, String> additionalHeaders, final Class<B> clazz);

    /**
     * See {@link #post(String, Object, List, Map, Class)}.
     */
    default <R, B> Response<B> post(final String url, final R request, final Class<B> clazz) {
        return post(url, request, List.of(), Map.of(), clazz);
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
     * @throws HttpClientException An error has occurred while executing the HTTP request.
     */
    <R, B> Response<B> put(final String url, final R request, final List<Parameter> parameters,
                           final Map<String, String> additionalHeaders, final Class<B> clazz);

    /**
     * See {@link #put(String, Object, List, Map, Class)}.
     */
    default <R, B> Response<B> put(final String url, final R request, final Class<B> clazz) {
        return put(url, request, List.of(), Map.of(), clazz);
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
     * @throws HttpClientException An error has occurred while executing the HTTP request.
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
        return delete(url, List.of(), Map.of());
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
