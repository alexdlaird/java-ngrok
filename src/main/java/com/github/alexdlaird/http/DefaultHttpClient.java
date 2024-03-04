/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.http;

import com.github.alexdlaird.util.StringUtils;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.alexdlaird.util.StringUtils.isNotBlank;
import static java.util.Objects.nonNull;

/**
 * A default client for executing JSON-based HTTP requests.
 *
 * <h3>Basic Usage</h3>
 * <pre>
 * final HttpClient httpClient = new DefaultHttpClient.Builder();
 *
 * final SomePOJORequest postPojo = new MyPOJO("id", "data");
 * final Response&lt;SomePOJOResponse&gt; postResponse = httpClient.post("http://localhost/pojo",
 *                                                                 postPojo,
 *                                                                 SomePOJOResponse.class);
 *
 * final Response&lt;SomePOJOResponse&gt; getResponse = httpClient.get("http://localhost/pojo/id",
 *                                                                 SomePOJOResponse.class);
 *
 * final SomePOJORequest putPojo = new MyPOJO("updated-data");
 * final Response&lt;SomePOJOResponse&gt; postResponse = httpClient.post("http://localhost/pojo/id",
 *                                                                 putPojo,
 *                                                                 SomePOJOResponse.class);
 *
 * final Response&lt;Map&gt; deleteResponse = httpClient.delete("http://localhost/pojo/id");
 * </pre>
 */
public class DefaultHttpClient implements HttpClient {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(DefaultHttpClient.class));

    private final Gson gson;
    private final String encoding;
    private final String contentType;
    private final int timeout;
    private final int retryCount;

    private DefaultHttpClient(final Builder builder) {
        this.encoding = builder.encoding;
        this.contentType = builder.contentType;
        this.timeout = builder.timeout;
        this.retryCount = builder.retryCount;
        this.gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }

    @Override
    public <B> Response<B> get(final String url,
                               final List<Parameter> parameters,
                               final Map<String, String> additionalHeaders,
                               final Class<B> clazz) {
        try {
            return execute(urlWithParameters(url, parameters), null, "GET",
                    additionalHeaders, clazz);
        } catch (final IOException e) {
            throw new HttpClientException("HTTP GET error", e);
        }
    }

    @Override
    public void get(final String url,
                    final List<Parameter> parameters,
                    final Map<String, String> additionalHeaders,
                    final Path dest,
                    final int retries) throws InterruptedException {
        HttpURLConnection httpUrlConnection = null;
        InputStream inputStream = null;

        try {
            httpUrlConnection = createHttpUrlConnection(urlWithParameters(url, parameters));

            inputStream = getInputStream(httpUrlConnection, null, "GET", additionalHeaders);
            Files.copy(inputStream, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (final Exception ex) {
            if (retries < retryCount) {
                LOGGER.warning("GET failed, retrying in 0.5 seconds ...");
                Thread.sleep(500);

                get(url, parameters, additionalHeaders, dest, retries + 1);
            } else {
                String msg = "An unknown error occurred when downloading the file";

                int statusCode = -1;
                String errorResponse = null;
                if (httpUrlConnection != null) {
                    try {
                        statusCode = httpUrlConnection.getResponseCode();
                        if (nonNull(httpUrlConnection.getErrorStream())) {
                            errorResponse = StringUtils.streamToString(httpUrlConnection.getErrorStream(), Charset.forName(encoding));
                        }

                        msg = "An error occurred when downloading the file (" + httpUrlConnection.getResponseCode() + "): " + errorResponse;
                    } catch (final IOException | NullPointerException ignored) {
                    }
                }

                throw new HttpClientException(msg, ex, url, statusCode, errorResponse);
            }
        } finally {
            if (httpUrlConnection != null) {
                httpUrlConnection.disconnect();
            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (final IOException ex) {
                LOGGER.log(Level.INFO, "Unable to close connection", ex);
            }
        }
    }

    @Override
    public <R, B> Response<B> post(final String url,
                                   final R request,
                                   final List<Parameter> parameters,
                                   final Map<String, String> additionalHeaders,
                                   final Class<B> clazz) {
        try {
            return execute(urlWithParameters(url, parameters), convertRequestToString(request), "POST",
                    additionalHeaders, clazz);
        } catch (final IOException e) {
            throw new HttpClientException("HTTP POST error", e);
        }
    }

    @Override
    public <R, B> Response<B> put(final String url,
                                  final R request,
                                  final List<Parameter> parameters,
                                  final Map<String, String> additionalHeaders,
                                  final Class<B> clazz) {
        try {
            return execute(urlWithParameters(url, parameters), convertRequestToString(request), "PUT",
                    additionalHeaders, clazz);
        } catch (final IOException e) {
            throw new HttpClientException("HTTP PUT error", e);
        }
    }

    @Override
    public <B> Response<B> delete(final String url,
                                  final List<Parameter> parameters,
                                  final Map<String, String> additionalHeaders,
                                  final Class<B> clazz) {
        try {
            return execute(urlWithParameters(url, parameters), null, "DELETE",
                    additionalHeaders, clazz);
        } catch (final IOException e) {
            throw new HttpClientException("HTTP DELETE error", e);
        }
    }

    private void appendDefaultsToConnection(final HttpURLConnection httpUrlConnection,
                                            final Map<String, String> additionalHeaders) {
        httpUrlConnection.setRequestProperty("Content-Type", contentType);
        if (additionalHeaders != null) {
            for (final Map.Entry<String, String> entry : additionalHeaders.entrySet()) {
                httpUrlConnection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Override this method if you could like to implement a custom URL connection.
     *
     * @param url The URL to connect to.
     * @return A URL connection.
     * @throws IOException An I/O exception has occurred.
     */
    protected HttpURLConnection createHttpUrlConnection(final String url) throws IOException {
        return (HttpURLConnection) new URL(url).openConnection();
    }

    private <T> String convertRequestToString(final T request) {
        if (request != null) {
            return gson.toJson(request);
        } else {
            return null;
        }
    }

    private <T> T convertResponseFromString(final String response, final Class<T> clazz) {
        if (isNotBlank(response)) {
            try {
                return gson.fromJson(response, clazz);
            } catch (final JsonSyntaxException ex) {
                return null;
            }
        } else {
            return null;
        }
    }

    protected String urlWithParameters(final String url,
                                       final List<Parameter> parameters)
            throws UnsupportedEncodingException {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(url);

        if (parameters != null && parameters.size() > 0) {
            boolean first = true;
            for (final Parameter parameter : parameters) {
                if (!first) {
                    stringBuilder.append("&");
                } else {
                    stringBuilder.append("?");

                    first = false;
                }

                stringBuilder.append(URLEncoder.encode(parameter.getName(), encoding));
                stringBuilder.append("=");
                stringBuilder.append(URLEncoder.encode(parameter.getValue(), encoding));
            }
        }

        return stringBuilder.toString();
    }

    protected InputStream getInputStream(final HttpURLConnection httpUrlConnection,
                                         final String body,
                                         final String method,
                                         final Map<String, String> additionalHeaders) throws IOException {
        httpUrlConnection.setRequestMethod(method);
        httpUrlConnection.setConnectTimeout(timeout);
        httpUrlConnection.setReadTimeout(timeout);

        appendDefaultsToConnection(httpUrlConnection, additionalHeaders);
        modifyConnection(httpUrlConnection);

        if (isNotBlank(body)) {
            httpUrlConnection.setDoOutput(true);
            httpUrlConnection.connect();

            final OutputStream outputStream = httpUrlConnection.getOutputStream();
            outputStream.write(body.getBytes(Charset.forName(encoding)));
            outputStream.close();
        } else {
            httpUrlConnection.setRequestProperty("Content-Length", "0");
            httpUrlConnection.connect();
        }

        return httpUrlConnection.getInputStream();
    }

    private <B> Response<B> execute(final String url,
                                    final String body,
                                    final String method,
                                    final Map<String, String> additionalHeaders,
                                    final Class<B> clazz) throws IOException {
        HttpURLConnection httpUrlConnection = null;
        InputStream inputStream = null;

        try {
            httpUrlConnection = createHttpUrlConnection(url);

            inputStream = getInputStream(httpUrlConnection, body, method, additionalHeaders);
            final String responseBody = StringUtils.streamToString(inputStream, Charset.forName(encoding));

            return new Response<>(httpUrlConnection.getResponseCode(),
                    convertResponseFromString(responseBody, clazz),
                    responseBody,
                    httpUrlConnection.getHeaderFields());
        } catch (final Exception ex) {
            String msg = "An unknown error occurred when performing the operation";

            int statusCode = -1;
            String errorResponse = null;
            if (httpUrlConnection != null) {
                try {
                    statusCode = httpUrlConnection.getResponseCode();
                    errorResponse = StringUtils.streamToString(httpUrlConnection.getErrorStream(), Charset.forName(encoding));

                    msg = "An error occurred when performing the operation (" + httpUrlConnection.getResponseCode() + "): " + errorResponse;
                } catch (final IOException | NullPointerException ignored) {
                }
            }

            throw new HttpClientException(msg, ex, url, statusCode, errorResponse);
        } finally {
            if (httpUrlConnection != null) {
                httpUrlConnection.disconnect();
            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (final IOException ex) {
                LOGGER.log(Level.INFO, "Unable to close connection", ex);
            }
        }
    }

    /**
     * Builder for a {@link DefaultHttpClient}, see docs for that class for example usage.
     */
    public static class Builder {
        private String encoding = "UTF-8";
        private String contentType = "application/json";
        public int timeout = 4000;
        public int retryCount = 0;

        /**
         * Default encoding for requests.
         */
        public Builder withEncoding(final String encoding) {
            this.encoding = encoding;
            return this;
        }

        /**
         * Default contentType header for requests.
         */
        public Builder withContentType(final String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Default timeout for requests.
         */
        public Builder withTimeout(final int timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Default retry count for GET requests.
         */
        public Builder withRetryCount(final int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public DefaultHttpClient build() {
            return new DefaultHttpClient(this);
        }
    }
}
