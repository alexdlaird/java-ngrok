package com.github.alexdlaird.ngrok;

import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnels;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class NgrokClientTest {

    private final NgrokClient ngrokClient = new NgrokClient.Builder().build();

    @AfterEach
    public void tearDown() throws InterruptedException {
        ngrokClient.kill();
    }

    @Test
    public void testConnect() throws IOException, InterruptedException {
        // GIVEN
        final CreateTunnel tunnelRequest = new CreateTunnel.Builder().withName("my-tunnel").build();
        assertNull(ngrokClient.getNgrokProcess().getProc());

        // WHEN
        final Tunnel tunnel = ngrokClient.connect(tunnelRequest);

        // THEN
        assertNotNull(ngrokClient.getNgrokProcess().getProc());
        assertEquals(tunnel.getName(), "my-tunnel");
        assertEquals(tunnel.getProto(), "http");
        assertEquals(tunnel.getConfig().getAddr(), "http://localhost:80");
    }

    @Test
    public void testDisconnect() throws IOException, InterruptedException {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withName("my-tunnel").build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);
        assertNotNull(ngrokClient.getNgrokProcess().getProc());

        // WHEN
        ngrokClient.disconnect(tunnel.getPublicUrl());

        // THEN
        final Tunnels tunnels = ngrokClient.getTunnels();
        assertEquals(tunnels.getTunnels().size(), 0);
    }

    @Test
    public void testKill() throws IOException, InterruptedException {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withName("my-tunnel").build();
        ngrokClient.connect(createTunnel);

        // WHEN
        ngrokClient.kill();

        // THEN
        assertNull(ngrokClient.getNgrokProcess().getProc());
    }
}