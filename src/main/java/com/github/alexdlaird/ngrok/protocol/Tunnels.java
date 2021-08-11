package com.github.alexdlaird.ngrok.protocol;

import java.util.List;

public class Tunnels {
    private List<Tunnel> tunnels;

    private String uri;

    public List<Tunnel> getTunnels() {
        return tunnels;
    }

    public String getUri() {
        return uri;
    }

}
