package com.github.alexdlaird.ngrok.http;

import com.github.alexdlaird.http.Request;

public class TunnelRequest implements Request {
    private String name;
    private String proto;
    private String addr;
    private String inspect;
    private String auth;
    private String hostHeader;
    private String bindTls;
    private String subdomain;
    private String hostname;
    private String crt;
    private String key;
    private String clientCas;
    private String remoteAddr;
    private String metadata;
}
