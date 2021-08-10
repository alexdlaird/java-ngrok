package com.github.alexdlaird.ngrok.http;

import java.util.List;
import java.util.Map;

public class CaptureRequest {
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

    private static class Request {
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

    private static class Response {
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
