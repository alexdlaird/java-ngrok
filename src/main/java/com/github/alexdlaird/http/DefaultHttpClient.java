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

import com.github.alexdlaird.StringUtils;
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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.alexdlaird.StringUtils.isBlank;

/**
 * Implementation of a default client for executing JSON-based HTTP requests.
 */
public class DefaultHttpClient implements HttpClient {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(String.valueOf(DefaultHttpClient.class));

    /**
     * Default serializer.
     */
    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    /**
     * Base URL for the API.
     */
    private final String baseUrl;

    /**
     * Default encoding for requests.
     */
    private final String encoding;

    /**
     * Default contentType header for requests.
     */
    private final String contentType;

    private DefaultHttpClient(final Builder builder) {
        this.baseUrl = builder.baseUrl;
        this.encoding = builder.encoding;
        this.contentType = builder.contentType;
    }

    @Override
    public <B> Response<B> get(final String uri,
                               final List<Parameter> parameters,
                               final Map<String, String> additionalHeaders,
                               final Class<B> clazz) {
        try {
            return execute(urlWithParameters(baseUrl + uri, parameters), null, "GET",
                    additionalHeaders, clazz);
        } catch (Exception ex) {
            throw new HttpClientException("Rest client error", ex);
        }
    }

    @Override
    public <R, B> Response<B> post(final String uri,
                                   final R request,
                                   final List<Parameter> parameters,
                                   final Map<String, String> additionalHeaders,
                                   final Class<B> clazz) {
        try {
            return execute(urlWithParameters(baseUrl + uri, parameters), convertRequestToString(request), "POST",
                    additionalHeaders, clazz);
        } catch (Exception ex) {
            throw new HttpClientException("Rest client error", ex);
        }
    }

    @Override
    public <R, B> Response<B> put(final String uri,
                                  final R request,
                                  final List<Parameter> parameters,
                                  final Map<String, String> additionalHeaders,
                                  final Class<B> clazz) {
        try {
            return execute(urlWithParameters(baseUrl + uri, parameters), convertRequestToString(request), "PUT",
                    additionalHeaders, clazz);
        } catch (Exception ex) {
            throw new HttpClientException("Rest client error", ex);
        }
    }

    @Override
    public <B> Response<B> delete(final String uri,
                                  final List<Parameter> parameters,
                                  final Map<String, String> additionalHeaders,
                                  final Class<B> clazz) {
        try {
            return execute(urlWithParameters(baseUrl + uri, parameters), null, "DELETE",
                    additionalHeaders, clazz);
        } catch (Exception ex) {
            throw new HttpClientException("Rest client error", ex);
        }
    }

    private void appendDefaultsToConnection(final HttpURLConnection httpUrlConnection,
                                            final Map<String, String> additionalHeaders) {
        httpUrlConnection.setRequestProperty("Content-Type", contentType);
        if (additionalHeaders != null) {
            for (Map.Entry<String, String> entry : additionalHeaders.entrySet()) {
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
        if (!isBlank(response)) {
            try {
                return gson.fromJson(response, clazz);
            } catch (JsonSyntaxException ex) {
                return null;
            }
        } else {
            return null;
        }
    }

    private String urlWithParameters(final String url,
                                     final List<Parameter> parameters)
            throws UnsupportedEncodingException {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(url);

        if (parameters != null && parameters.size() > 0) {
            boolean first = true;
            for (Parameter parameter : parameters) {
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

    private <B> Response<B> execute(final String url,
                                    final String body,
                                    final String method,
                                    final Map<String, String> additionalHeaders,
                                    final Class<B> clazz) {
        HttpURLConnection httpUrlConnection = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;

        try {
            httpUrlConnection = createHttpUrlConnection(url);
            httpUrlConnection.setRequestMethod(method);

            appendDefaultsToConnection(httpUrlConnection, additionalHeaders);
            modifyConnection(httpUrlConnection);

            if (StringUtils.isNotBlank(body)) {
                httpUrlConnection.setDoOutput(true);
                httpUrlConnection.connect();

                outputStream = httpUrlConnection.getOutputStream();

                outputStream.write(body.getBytes(Charset.forName(encoding)));
            } else {
                httpUrlConnection.setRequestProperty("Content-Length", "0");
                httpUrlConnection.connect();
            }

            inputStream = httpUrlConnection.getInputStream();

            final String responseBody = StringUtils.streamToString(inputStream, Charset.forName(encoding));

            return new Response<>(httpUrlConnection.getResponseCode(),
                    convertResponseFromString(responseBody, clazz),
                    responseBody,
                    httpUrlConnection.getHeaderFields());
        } catch (Exception ex) {
            String msg = "An unknown error occurred when performing the operation";

            if (httpUrlConnection != null) {
                try {
                    String errorString = StringUtils.streamToString(httpUrlConnection.getErrorStream(), Charset.forName(encoding));

                    msg = "An error occurred when performing the operation (" + httpUrlConnection.getResponseCode() + "): " + errorString;
                } catch (IOException | NullPointerException ignored) {
                }
            }

            throw new HttpClientException(msg, ex);
        } finally {
            if (httpUrlConnection != null) {
                httpUrlConnection.disconnect();
            }
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ex) {
                LOGGER.log(Level.INFO, "Unable to close connection", ex);
            }
        }
    }

    public static class Builder {
        private final String baseUrl;
        private String encoding = "UTF-8";
        private String contentType = "application/json";

        public Builder(final String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Builder withEncoding(final String encoding) {
            this.encoding = encoding;
            return this;
        }

        public Builder withContentType(final String contentType) {
            this.contentType = contentType;
            return this;
        }

        public DefaultHttpClient build() {
            return new DefaultHttpClient(this);
        }
    }
}
