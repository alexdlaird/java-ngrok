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

    public String getUri() {
        return uri;
    }

    public String getId() {
        return id;
    }

    public String getTunnelName() {
        return tunnelName;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public String getStart() {
        return start;
    }

    public int getDuration() {
        return duration;
    }

    public Request getRequest() {
        return request;
    }

    public Response getResponse() {
        return response;
    }

    public static class Request {
        private String method;
        private String proto;
        private Map<String, List<String>> headers;
        private String uri;
        private String raw;

        public String getMethod() {
            return method;
        }

        public String getProto() {
            return proto;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public String getUri() {
            return uri;
        }

        public String getRaw() {
            return raw;
        }
    }

    public static class Response {
        private String status;
        private int statusCode;
        private String proto;
        private Map<String, List<String>> headers;
        private String raw;

        public String getStatus() {
            return status;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getProto() {
            return proto;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public String getRaw() {
            return raw;
        }
    }
}
