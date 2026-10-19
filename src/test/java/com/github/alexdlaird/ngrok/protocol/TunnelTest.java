/*
 * Copyright (c) 2021-2026 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TunnelTest {

    private final Gson gson = new Gson();

    @Test
    public void testProtoFallsBackToUrlScheme() {
        // WHEN
        final Tunnel httpsTunnel = gson.fromJson("{\"name\": \"web\", \"url\": \"https://my.ngrok.dev\"}",
            Tunnel.class);
        final Tunnel tcpTunnel = gson.fromJson("{\"name\": \"ssh\", \"url\": \"tcp://1.tcp.ngrok.io:12345\"}",
            Tunnel.class);

        // THEN
        assertEquals("https", httpsTunnel.getProto());
        assertEquals("tcp", tcpTunnel.getProto());
    }
}
