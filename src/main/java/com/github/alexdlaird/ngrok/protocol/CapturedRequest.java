/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import java.util.List;
import java.util.Map;

/**
 * An object representing a CapturedRequest response from <code>ngrok</code>'s API.
 */
public class CapturedRequest {

    private String uri;
    private String id;
    private String tunnelName;
    private String remoteAddr;
    private String start;
    private int duration;
    private Request request;
    private Response response;

    /**
     * Get the URI of the captured request.
     */
    public String getUri() {
        return uri;
    }

    /**
     * Get the ID of the captured request.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the tunnel name of the captured request.
     */
    public String getTunnelName() {
        return tunnelName;
    }

    /**
     * Get the remote addr of the captured request.
     */
    public String getRemoteAddr() {
        return remoteAddr;
    }

    /**
     * Get the start of the captured request.
     */
    public String getStart() {
        return start;
    }

    /**
     * Get the duration of the request.
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Get the captured {@link Request}.
     */
    public Request getRequest() {
        return request;
    }

    /**
     * Get the captured {@link Response}.
     */
    public Response getResponse() {
        return response;
    }

    /**
     * An object representing a nested Request from <code>ngrok</code>'s API.
     */
    public static class Request {

        private String method;
        private String proto;
        private Map<String, List<String>> headers;
        private String uri;
        private String raw;

        /**
         * Get the method of the request.
         */
        public String getMethod() {
            return method;
        }

        /**
         * Get the proto of the request.
         */
        public String getProto() {
            return proto;
        }

        /**
         * Get the map of request headers.
         */
        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        /**
         * Get the URI of the request.
         */
        public String getUri() {
            return uri;
        }

        /**
         * Get the raw request.
         */
        public String getRaw() {
            return raw;
        }
    }

    /**
     * An object representing a nested Response from <code>ngrok</code>'s API.
     */
    public static class Response {

        private String status;
        private int statusCode;
        private String proto;
        private Map<String, List<String>> headers;
        private String raw;

        /**
         * Get the description of the response.
         */
        public String getStatus() {
            return status;
        }

        /**
         * Get the status code of the response.
         */
        public int getStatusCode() {
            return statusCode;
        }

        /**
         * Get the proto of the response.
         */
        public String getProto() {
            return proto;
        }

        /**
         * Get the map of response headers.
         */
        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        /**
         * Get the raw response.
         */
        public String getRaw() {
            return raw;
        }
    }
}
