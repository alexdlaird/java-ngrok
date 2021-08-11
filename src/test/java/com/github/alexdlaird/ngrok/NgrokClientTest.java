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
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withName("my-tunnel").build();
        assertNull(ngrokClient.getNgrokProcess().getProc());

        // WHEN
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // THEN
        assertNotNull(ngrokClient.getNgrokProcess().getProc());
        assertEquals(tunnel.getName(), "my-tunnel (http)");
        assertEquals(tunnel.getProto(), "http");
        assertEquals(tunnel.getConfig().getAddr(), "http://localhost:80");
    }

    @Test
    public void testDisconnect() throws IOException, InterruptedException {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withName("my-tunnel").withBindTls("true").build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);
        assertNotNull(ngrokClient.getNgrokProcess().getProc());

        // WHEN
        ngrokClient.disconnect(tunnel.getPublicUrl());

        // THEN
        final Tunnels tunnels = ngrokClient.getTunnels();
        assertEquals(tunnels.getTunnels().size(), 0);
    }

    @Test
    public void testGetTunnels() throws IOException, InterruptedException {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withName("my-tunnel").build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // WHEN
        final Tunnels tunnels = ngrokClient.getTunnels();

        assertEquals(tunnels.getTunnels().size(), 2);
        for (Tunnel t : tunnels.getTunnels()) {
            if (t.getProto().equals("http")) {
                assertEquals(t.getPublicUrl(), tunnel.getPublicUrl());
                assertEquals(t.getConfig().getAddr(), "http://localhost:80");
            } else {
                assertEquals(t.getProto(), "https");
                assertEquals(t.getPublicUrl(), tunnel.getPublicUrl().replace("http", "https"));
                assertEquals(t.getConfig().getAddr(), "http://localhost:80");
            }
        }
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