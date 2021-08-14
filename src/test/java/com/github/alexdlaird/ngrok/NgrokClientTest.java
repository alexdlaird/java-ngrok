package com.github.alexdlaird.ngrok;

import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnels;
import com.github.alexdlaird.ngrok.protocol.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NgrokClientTest extends NgrokTestCase {

    private NgrokClient ngrokClient;

    @BeforeEach
    public void setUp() {
        super.setUp();

        ngrokClient = new NgrokClient.Builder()
                .withNgrokProcess(ngrokProcess)
                .build();
    }

    @Test
    public void testConnect() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withName("my-tunnel")
                .build();
        assertFalse(ngrokClient.getNgrokProcess().isRunning());

        // WHEN
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // THEN
        assertTrue(ngrokClient.getNgrokProcess().isRunning());
        assertEquals(tunnel.getName(), "my-tunnel (http)");
        assertEquals(tunnel.getProto(), "http");
        assertEquals(tunnel.getConfig().getAddr(), "http://localhost:80");
    }

    @Test
    public void testDisconnect() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withName("my-tunnel")
                .withBindTls(true)
                .build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);
        assertTrue(ngrokClient.getNgrokProcess().isRunning());

        // WHEN
        ngrokClient.disconnect(tunnel.getPublicUrl());

        // THEN
        final Tunnels tunnels = ngrokClient.getTunnels();
        assertEquals(tunnels.getTunnels().size(), 0);
    }

    @Test
    public void testGetTunnels() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withName("my-tunnel")
                .build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // WHEN
        final Tunnels tunnels = ngrokClient.getTunnels();

        assertEquals(tunnels.getTunnels().size(), 2);
        for (final Tunnel t : tunnels.getTunnels()) {
            if (t.getProto().equals("http")) {
                assertEquals(t.getPublicUrl(), tunnel.getPublicUrl());
            } else {
                assertEquals(t.getProto(), "https");
                assertEquals(t.getPublicUrl(), tunnel.getPublicUrl().replace("http", "https"));
            }
            assertEquals(t.getConfig().getAddr(), "http://localhost:80");
        }
    }

    @Test
    public void testKill() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withName("my-tunnel").build();
        ngrokClient.connect(createTunnel);

        // WHEN
        ngrokClient.kill();

        // THEN
        assertFalse(ngrokClient.getNgrokProcess().isRunning());
    }

    @Test
    public void testGetVersion() {
        // WHEN
        final Version version = ngrokClient.getVersion();

        // THEN
        assertNotNull(version.getJavaNgrokVersion());
        assertNotEquals(version.getNgrokVersion(), "unknown");
    }
}
