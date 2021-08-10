package com.github.alexdlaird.ngrok.http;

import java.util.List;

public class TunnelsResponse {
    private List<TunnelResponse> tunnels;

    private String uri;

    public List<TunnelResponse> getTunnels() {
        return tunnels;
    }

    public String getUri() {
        return uri;
    }

}
