package com.github.alexdlaird.ngrok.http;

import java.util.List;
import java.util.Map;

public class CaptureRequestResponse {
    private String uri;
    private String id;
    private String tunnelName;
    private String remoteAddr;
    private String start;
    private int duration;
    private Request request;
    private Response response;

    private static class Request {
        private String method;
        private String proto;
        private Map<String, List<String>> headers;
        private String uri;
        private String raw;
    }

    private static class Response {
        private String status;
        private int status_code;
        private String proto;
        private Map<String, List<String>> headers;
        private String raw;
    }
}
