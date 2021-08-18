package com.github.alexdlaird.ngrok;

import com.github.alexdlaird.exception.JavaNgrokHTTPException;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.process.NgrokProcess;
import com.github.alexdlaird.ngrok.protocol.BindTls;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Region;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnels;
import com.github.alexdlaird.ngrok.protocol.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.github.alexdlaird.util.StringUtils.isNotBlank;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NgrokClientTest extends NgrokTestCase {

    private NgrokClient ngrokClient;

    @BeforeEach
    public void setUp() {
        super.setUp();

        ngrokClient = new NgrokClient.Builder()
                .withJavaNgrokConfig(javaNgrokConfig)
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
        assertEquals("http", tunnel.getProto());
        assertEquals("http://localhost:5000", tunnel.getConfig().getAddr());
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
        assertEquals("http", tunnel.getProto());
        assertEquals("http://localhost:80", tunnel.getConfig().getAddr());
    }

    @Test
    public void testMultipleConnectionsNoTokenFails() throws InterruptedException {
        // WHEN
        ngrokClient.connect(new CreateTunnel.Builder().withAddr(5000).build());
        Thread.sleep(1000);
        final JavaNgrokHTTPException exception = assertThrows(JavaNgrokHTTPException.class, () -> ngrokClient.connect(new CreateTunnel.Builder().withAddr(5001).build()));

        // THEN
        assertTrue(exception.getBody().contains("account may not run more than 2 tunnels"));
    }

    @Test
    public void testGetTunnels() {
        // GIVEN
        final Tunnel tunnel = ngrokClient.connect();

        // WHEN
        final Tunnels tunnels = ngrokClient.getTunnels();

        assertEquals(2, tunnels.getTunnels().size());
        for (final Tunnel t : tunnels.getTunnels()) {
            if (t.getProto().equals("http")) {
                assertEquals(tunnel.getPublicUrl(), t.getPublicUrl());
            } else {
                assertEquals("https", t.getProto());
                assertEquals(tunnel.getPublicUrl().replace("http", "https"), t.getPublicUrl());
            }
            assertEquals("http://localhost:80", t.getConfig().getAddr());
        }
    }

    @Test
    public void testConnectBindTlsBoth() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withBindTls(BindTls.BOTH)
                .build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // WHEN
        final Tunnels tunnels = ngrokClient.getTunnels();

        // THEN
        assertEquals(2, tunnels.getTunnels().size());
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
        assertEquals(1, tunnels.getTunnels().size());
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
        assertEquals(1, tunnels.getTunnels().size());
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
        assertEquals(0, tunnels.getTunnels().size());
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

    @Test
    public void testRegionalSubdomain() {
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        final JavaNgrokConfig javaNgrokConfig = new JavaNgrokConfig.Builder()
                .withAuthToken(ngrokAuthToken)
                .withRegion(Region.AU)
                .withConfigPath(Paths.get("build", ".ngrok2", "config.yml").toAbsolutePath())
                .withReconnectSessionRetries(10)
                .build();
        final NgrokProcess ngrokProcess = new NgrokProcess(javaNgrokConfig, ngrokInstaller);
        final NgrokClient ngrokClient = new NgrokClient.Builder()
                .withJavaNgrokConfig(javaNgrokConfig)
                .withNgrokProcess(ngrokProcess)
                .build();
        assertFalse(ngrokClient.getNgrokProcess().isRunning());
        final String subdomain = createUniqueSubdomain();
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withSubdomain(subdomain)
                .build();

        // WHEN
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // THEN
        assertTrue(ngrokClient.getNgrokProcess().isRunning());
        assertNotNull(tunnel.getPublicUrl());
        assertTrue(tunnel.getPublicUrl().contains("http://"));
        assertTrue(tunnel.getPublicUrl().contains(".au."));
        assertTrue(tunnel.getPublicUrl().contains(subdomain));

        ngrokProcess.stop();
    }

    // TODO: testRegionalSubdomain()

    @Test
    public void testConnectFileserver() {
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        ngrokClient.getNgrokProcess().setAuthToken(ngrokAuthToken);
        assertFalse(ngrokClient.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withAddr("file:///")
                .build();

        // WHEN
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // THEN
        assertTrue(ngrokClient.getNgrokProcess().isRunning());
        assertTrue(tunnel.getName().startsWith("http-file-"));
        assertEquals("http", tunnel.getProto());
        assertEquals("file:///", tunnel.getConfig().getAddr());
        assertNotNull(tunnel.getPublicUrl());
        assertTrue(tunnel.getPublicUrl().startsWith("http://"));
    }

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
