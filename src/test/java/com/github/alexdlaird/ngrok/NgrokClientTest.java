package com.github.alexdlaird.ngrok;

import com.github.alexdlaird.exception.JavaNgrokHTTPException;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnels;
import com.github.alexdlaird.ngrok.protocol.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertFalse(ngrokClient.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withAddr(5000)
                .build();

        // WHEN
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // THEN
        assertTrue(ngrokClient.getNgrokProcess().isRunning());
        assertTrue(tunnel.getName().startsWith("http-5000-"));
        assertEquals(tunnel.getProto(), "http");
        assertEquals(tunnel.getConfig().getAddr(), "http://localhost:5000");
        assertNotNull(tunnel.getPublicUrl());
        assertTrue(tunnel.getPublicUrl().startsWith("http://"));
    }

    @Test
    public void testConnectName() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withName("my-tunnel")
                .build();

        // WHEN
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // THEN
        assertTrue(tunnel.getName().startsWith("my-tunnel (http)"));
        assertEquals(tunnel.getProto(), "http");
        assertEquals(tunnel.getConfig().getAddr(), "http://localhost:80");
    }

    @Test
    public void testMultipleConnectionsNoTokenFails() throws InterruptedException {
        // WHEN
        ngrokClient.connect(new CreateTunnel.Builder().withAddr(5000).build());
        Thread.sleep(1000);
        final JavaNgrokHTTPException exception = assertThrows(JavaNgrokHTTPException.class, () -> {
            ngrokClient.connect(new CreateTunnel.Builder().withAddr(5001).build());
        });

        // THEN
        assertTrue(exception.getBody().contains("account may not run more than 2 tunnels"));
    }

    @Test
    public void testGetTunnels() {
        // GIVEN
        final Tunnel tunnel = ngrokClient.connect();

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
    public void testConnectBindTlsBoth() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withBindTls("both")
                .build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // WHEN
        final Tunnels tunnels = ngrokClient.getTunnels();

        // THEN
        assertEquals(tunnels.getTunnels().size(), 2);
        assertTrue(tunnel.getPublicUrl().startsWith("http://"));
    }

    @Test
    public void testConnectBindTlsHttpsOnly() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withBindTls(true)
                .build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // WHEN
        final Tunnels tunnels = ngrokClient.getTunnels();

        // THEN
        assertEquals(tunnels.getTunnels().size(), 1);
        assertTrue(tunnel.getPublicUrl().startsWith("https://"));
    }

    @Test
    public void testConnectBindTlsHttpOnly() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withBindTls(false)
                .build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // WHEN
        final Tunnels tunnels = ngrokClient.getTunnels();

        // THEN
        assertEquals(tunnels.getTunnels().size(), 1);
        assertTrue(tunnel.getPublicUrl().startsWith("http://"));
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

    // TODO: testRegionalTcp()

    // TODO: testRegionalSubdomain()

    // TODO: testConnectFileserver()

    // TODO: testDisconnectFileserver()

    // TODO: testGetTunnelFileserver()

    // TODO: testRefreshMetrics()

    // TODO: testTunnelDefinitions()

    // TODO: testTunnelDefinitionsJavaNgrokDefaultWithOverrides()

    @Test
    public void testSetAuthToken() throws IOException {
        // WHEN
        ngrokClient.setAuthToken("807ad30a-73be-48d8");
        final String contents = Files.readString(javaNgrokConfig.getConfigPath());

        // THEN
        assertTrue(contents.contains("807ad30a-73be-48d8"));
    }
}
