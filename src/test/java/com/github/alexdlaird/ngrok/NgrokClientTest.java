/*
 * Copyright (c) 2021 Alex Laird
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.alexdlaird.ngrok;

import com.github.alexdlaird.exception.JavaNgrokHTTPException;
import com.github.alexdlaird.http.Response;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.process.NgrokProcess;
import com.github.alexdlaird.ngrok.protocol.BindTls;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Proto;
import com.github.alexdlaird.ngrok.protocol.Region;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.github.alexdlaird.ngrok.protocol.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.alexdlaird.util.StringUtils.isNotBlank;
import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.StringContains.containsString;
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
    public void testGetters() {
        // THEN
        assertEquals(javaNgrokConfig, ngrokClient.getJavaNgrokConfig());
        assertEquals(ngrokProcess, ngrokClient.getNgrokProcess());
        assertEquals(ngrokInstaller, ngrokClient.getNgrokProcess().getNgrokInstaller());
        assertNotNull(ngrokClient.getHttpClient());
    }

    @Test
    public void testConnect() {
        // GIVEN
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");
        assertFalse(ngrokClient.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withAddr(5000)
                .build();

        // WHEN
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // THEN
        assertTrue(ngrokClient.getNgrokProcess().isRunning());
        assertThat(tunnel.getName(), startsWith("http-5000-"));
        assertEquals("http", tunnel.getProto());
        assertEquals("http://localhost:5000", tunnel.getConfig().getAddr());
        assertTrue(tunnel.getConfig().isInspect());
        assertNotNull(tunnel.getPublicUrl());
        assertThat(tunnel.getPublicUrl(), startsWith("http://"));
        assertNotNull(tunnel.getMetrics());
        assertThat(tunnel.getMetrics(), hasKey("conns"));
        assertEquals(0, tunnel.getMetrics().get("conns").getCount());
        assertEquals(0, tunnel.getMetrics().get("conns").getGauge());
        assertEquals(0, tunnel.getMetrics().get("conns").getP50());
        assertEquals(0, tunnel.getMetrics().get("conns").getP90());
        assertEquals(0, tunnel.getMetrics().get("conns").getP95());
        assertEquals(0, tunnel.getMetrics().get("conns").getP99());
        assertEquals(0, tunnel.getMetrics().get("conns").getRate1());
        assertEquals(0, tunnel.getMetrics().get("conns").getRate5());
        assertEquals(0, tunnel.getMetrics().get("conns").getRate15());
    }

    @Test
    public void testConnectName() {
        // GIVEN
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withName("my-tunnel")
                .build();

        // WHEN
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // THEN
        assertThat(tunnel.getName(), startsWith("my-tunnel (http)"));
        assertEquals("http", tunnel.getProto());
        assertEquals("http://localhost:80", tunnel.getConfig().getAddr());
    }

    @Test
    public void testMultipleConnectionsNoTokenFails() throws InterruptedException {
        // WHEN
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");
        ngrokClient.connect(new CreateTunnel.Builder().withAddr(5000).build());
        Thread.sleep(1000);
        final JavaNgrokHTTPException exception = assertThrows(JavaNgrokHTTPException.class, () -> ngrokClient.connect(new CreateTunnel.Builder().withAddr(5001).build()));

        // THEN
        assertEquals(HTTP_BAD_GATEWAY, exception.getStatusCode());
        assertEquals(String.format("%s/api/tunnels", ngrokProcess.getApiUrl()), exception.getUrl());
        assertThat(exception.getBody(), containsString("account may not run more than 2 tunnels"));
    }

    @Test
    public void testGetTunnels() {
        // GIVEN
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");
        final Tunnel tunnel = ngrokClient.connect();

        // WHEN
        final List<Tunnel> tunnels = ngrokClient.getTunnels();

        // THEN
        assertEquals(2, tunnels.size());
        for (final Tunnel t : tunnels) {
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
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withBindTls(BindTls.BOTH)
                .build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // WHEN
        final List<Tunnel> tunnels = ngrokClient.getTunnels();

        // THEN
        assertEquals(2, tunnels.size());
        assertThat(tunnel.getPublicUrl(), startsWith("http://"));
    }

    @Test
    public void testConnectBindTlsHttpsOnly() {
        // GIVEN
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withBindTls(true)
                .build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // WHEN
        final List<Tunnel> tunnels = ngrokClient.getTunnels();

        // THEN
        assertEquals(1, tunnels.size());
        assertThat(tunnel.getPublicUrl(), startsWith("https://"));
    }

    @Test
    public void testConnectBindTlsHttpOnly() {
        // GIVEN
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withBindTls(false)
                .build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // WHEN
        final List<Tunnel> tunnels = ngrokClient.getTunnels();

        // THEN
        assertEquals(1, tunnels.size());
        assertThat(tunnel.getPublicUrl(), startsWith("http://"));
    }

    @Test
    public void testDisconnect() {
        // GIVEN
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withName("my-tunnel")
                .withBindTls(true)
                .build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);
        assertTrue(ngrokClient.getNgrokProcess().isRunning());

        // WHEN
        ngrokClient.disconnect(tunnel.getPublicUrl());

        // THEN
        final List<Tunnel> tunnels = ngrokClient.getTunnels();
        assertEquals(0, tunnels.size());
    }

    @Test
    public void testGetVersion() {
        // WHEN
        final Version version = ngrokClient.getVersion();

        // THEN
        assertNotNull(version.getJavaNgrokVersion());
        assertNotEquals(version.getNgrokVersion(), "unknown");
    }

    @Test
    public void testRegionalTcp() {
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig)
                .withRegion(Region.AU)
                .build();
        ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder()
                .withJavaNgrokConfig(javaNgrokConfig2)
                .withNgrokProcess(ngrokProcess2)
                .build();
        assertFalse(ngrokClient2.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withProto(Proto.TCP)
                .withAddr(5000)
                .build();

        // WHEN
        final Tunnel tunnel = ngrokClient2.connect(createTunnel);

        // THEN
        assertTrue(ngrokClient2.getNgrokProcess().isRunning());
        assertNotNull(tunnel.getPublicUrl());
        assertEquals("localhost:5000", tunnel.getConfig().getAddr());
        assertThat(tunnel.getPublicUrl(), containsString("tcp://"));
        assertThat(tunnel.getPublicUrl(), containsString(".au."));
    }

    @Test
    public void testRegionalSubdomain() {
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig)
                .withRegion(Region.AU)
                .build();
        ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder()
                .withJavaNgrokConfig(javaNgrokConfig2)
                .withNgrokProcess(ngrokProcess2)
                .build();
        assertFalse(ngrokClient2.getNgrokProcess().isRunning());
        final String subdomain = createUniqueSubdomain();
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withSubdomain(subdomain)
                .build();

        // WHEN
        final Tunnel tunnel = ngrokClient2.connect(createTunnel);

        // THEN
        assertTrue(ngrokClient2.getNgrokProcess().isRunning());
        assertNotNull(tunnel.getPublicUrl());
        assertThat(tunnel.getPublicUrl(), containsString("http://"));
        assertThat(tunnel.getPublicUrl(), containsString(".au."));
        assertThat(tunnel.getPublicUrl(), containsString(subdomain));
    }

    @Test
    public void testConnectFileserver() {
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        assertFalse(ngrokClient.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withAddr("file:///")
                .build();

        // WHEN
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // THEN
        assertTrue(ngrokClient.getNgrokProcess().isRunning());
        assertThat(tunnel.getName(), startsWith("http-file-"));
        assertEquals("http", tunnel.getProto());
        assertEquals("file:///", tunnel.getConfig().getAddr());
        assertNotNull(tunnel.getPublicUrl());
        assertThat(tunnel.getPublicUrl(), startsWith("http://"));
    }

    @Test
    public void testDisconnectFileserver() throws InterruptedException {
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        assertFalse(ngrokClient.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withAddr("file:///")
                .build();
        final String publicUrl = ngrokClient.connect(createTunnel).getPublicUrl();
        Thread.sleep(1000);

        // WHEN
        ngrokClient.disconnect(publicUrl);
        Thread.sleep(1000);
        final List<Tunnel> tunnels = ngrokClient.getTunnels();

        // THEN
        assertTrue(ngrokClient.getNgrokProcess().isRunning());
        // There is still one tunnel left, as we only disconnected the http tunnel
        assertEquals(1, tunnels.size());
    }

    @Test
    public void testGetTunnelFileserver() throws InterruptedException {
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        assertFalse(ngrokClient.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withAddr("file:///")
                .build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);
        Thread.sleep(1000);
        final String apiUrl = ngrokClient.getNgrokProcess().getApiUrl();

        // WHEN
        final Response<Tunnel> response = ngrokClient.getHttpClient().get(String.format("%s%s", apiUrl, tunnel.getUri()), Tunnel.class);

        // THEN
        assertEquals(tunnel.getName(), response.getBody().getName());
        assertThat(tunnel.getName(), startsWith("http-file-"));
    }

    @Test
    public void testRefreshMetrics() throws MalformedURLException, InterruptedException {
        // GIVEN
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");
        ngrokClient.getNgrokProcess().start();
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withName("my-tunnel")
                .withAddr(new URL(ngrokClient.getNgrokProcess().getApiUrl()).getPort())
                .withBindTls(true)
                .build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);
        Thread.sleep(1000);
        assertEquals(0, tunnel.getMetrics().get("http").getCount());

        ngrokClient.getHttpClient().get(String.format("%s/status", tunnel.getPublicUrl()), Object.class);

        Thread.sleep(3000);

        ngrokClient.refreshMetrics(tunnel);

        assertThat(tunnel.getMetrics().get("http").getCount(), greaterThan(0));
    }

    @Test
    public void testTunnelDefinitions() {
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        final String subdomain = createUniqueSubdomain();
        final Map<String, Object> httpTunnelConfig = new HashMap<String, Object>() {{
            put("proto", "http");
            put("addr", "8000");
            put("subdomain", subdomain);
            put("inspect", Boolean.FALSE);
            put("bind_tls", Boolean.TRUE);
        }};
        final Map<String, Object> tcpTunnelConfig = new HashMap<String, Object>() {{
            put("proto", "tcp");
            put("addr", "22");
        }};
        final Map<String, Object> tunnelsConfig = new HashMap<String, Object>() {{
            put("http-tunnel", httpTunnelConfig);
            put("tcp-tunnel", tcpTunnelConfig);
        }};
        final Map<String, Object> config = Collections.singletonMap("tunnels", tunnelsConfig);

        final Path configPath2 = Paths.get(javaNgrokConfig.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config);
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig)
                .withConfigPath(configPath2)
                .build();
        ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder()
                .withJavaNgrokConfig(javaNgrokConfig2)
                .withNgrokProcess(ngrokProcess2)
                .build();

        // WHEN
        final CreateTunnel createHttpTunnel = new CreateTunnel.Builder()
                .withName("http-tunnel")
                .build();
        final Tunnel httpTunnel = ngrokClient2.connect(createHttpTunnel);
        final CreateTunnel createSshTunnel = new CreateTunnel.Builder()
                .withName("tcp-tunnel")
                .build();
        final Tunnel sshTunnel = ngrokClient2.connect(createSshTunnel);
        final List<Tunnel> tunnels = ngrokClient2.getTunnels();

        // THEN
        assertEquals(2, tunnels.size());
        assertEquals("http-tunnel", httpTunnel.getName());
        assertEquals("http://localhost:8000", httpTunnel.getConfig().getAddr());
        assertEquals("https", httpTunnel.getProto());
        assertEquals(String.format("https://%s.ngrok.io", subdomain), httpTunnel.getPublicUrl());
        assertEquals("tcp-tunnel", sshTunnel.getName());
        assertEquals("localhost:22", sshTunnel.getConfig().getAddr());
        assertEquals("tcp", sshTunnel.getProto());
        assertThat(sshTunnel.getPublicUrl(), startsWith("tcp://"));
    }

    @Test
    public void testTunnelDefinitionsJavaNgrokDefaultWithOverrides() {
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        final String subdomain1 = createUniqueSubdomain();
        final Map<String, Object> defaultTunnelConfig = new HashMap<String, Object>() {{
            put("proto", "http");
            put("addr", "8080");
            put("subdomain", subdomain1);
        }};
        final Map<String, Object> tunnelsConfig = Collections.singletonMap("java-ngrok-default", defaultTunnelConfig);
        final Map<String, Object> config = Collections.singletonMap("tunnels", tunnelsConfig);

        final Path configPath2 = Paths.get(javaNgrokConfig.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config);
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig)
                .withConfigPath(configPath2)
                .build();
        ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder()
                .withJavaNgrokConfig(javaNgrokConfig2)
                .withNgrokProcess(ngrokProcess2)
                .build();

        // WHEN
        final Tunnel ngrokTunnel1 = ngrokClient2.connect();
        final String subdomain2 = createUniqueSubdomain();
        final CreateTunnel createTunnelSubdomain = new CreateTunnel.Builder()
                .withSubdomain(subdomain2)
                .withAddr(5000)
                .build();
        final Tunnel ngrokTunnel2 = ngrokClient2.connect(createTunnelSubdomain);

        // THEN
        assertEquals("java-ngrok-default (http)", ngrokTunnel1.getName());
        assertEquals("http://localhost:8080", ngrokTunnel1.getConfig().getAddr());
        assertEquals(Proto.HTTP.toString(), ngrokTunnel1.getProto());
        assertEquals(String.format("http://%s.ngrok.io", subdomain1), ngrokTunnel1.getPublicUrl());
        assertEquals("java-ngrok-default (http)", ngrokTunnel2.getName());
        assertEquals("http://localhost:5000", ngrokTunnel2.getConfig().getAddr());
        assertEquals(Proto.HTTP.toString(), ngrokTunnel2.getProto());
        assertEquals(String.format("http://%s.ngrok.io", subdomain2), ngrokTunnel2.getPublicUrl());
    }

    @Test
    public void testSetAuthToken() throws IOException {
        // WHEN
        ngrokClient.setAuthToken("807ad30a-73be-48d8");
        final String contents = Files.lines(javaNgrokConfig.getConfigPath()).collect(Collectors.joining("\n"));

        // THEN
        assertThat(contents, containsString("807ad30a-73be-48d8"));
    }
}
