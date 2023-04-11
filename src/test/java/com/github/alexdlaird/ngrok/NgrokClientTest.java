/*
 * Copyright (c) 2022 Alex Laird
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
import java.util.List;
import java.util.Map;

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

    private NgrokClient ngrokClientV2;

    private NgrokClient ngrokClientV3;

    @BeforeEach
    public void setUp() {
        super.setUp();

        ngrokClientV2 = new NgrokClient.Builder()
                .withJavaNgrokConfig(javaNgrokConfigV2)
                .withNgrokProcess(ngrokProcessV2)
                .build();
        ngrokClientV3 = new NgrokClient.Builder()
                .withJavaNgrokConfig(javaNgrokConfigV3)
                .withNgrokProcess(ngrokProcessV3)
                .build();
    }

    @Test
    public void testGetters() {
        // THEN
        assertEquals(javaNgrokConfigV2, ngrokClientV2.getJavaNgrokConfig());
        assertEquals(ngrokProcessV2, ngrokClientV2.getNgrokProcess());
        assertEquals(ngrokInstaller, ngrokClientV2.getNgrokProcess().getNgrokInstaller());
        assertNotNull(ngrokClientV2.getHttpClient());
    }

    @Test
    public void testConnectV2() {
        // GIVEN
        assertFalse(ngrokProcessV2.isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withAddr(5000)
                .build(javaNgrokConfigV2);

        // WHEN
        final Tunnel tunnel = ngrokClientV2.connect(createTunnel);

        // THEN
        assertTrue(ngrokClientV2.getNgrokProcess().getVersion().startsWith("2"));
        assertTrue(ngrokClientV2.getNgrokProcess().isRunning());
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
    public void testConnectV3() {
        // GIVEN
        assertFalse(ngrokClientV3.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withAddr(5000)
                .build(javaNgrokConfigV3);

        // WHEN
        final Tunnel tunnel = ngrokClientV3.connect(createTunnel);

        // THEN
        assertTrue(ngrokClientV3.getNgrokProcess().getVersion().startsWith("3"));
        assertTrue(ngrokClientV3.getNgrokProcess().isRunning());
        assertThat(tunnel.getName(), startsWith("http-5000-"));
        assertEquals("https", tunnel.getProto());
        assertEquals("http://localhost:5000", tunnel.getConfig().getAddr());
        assertTrue(tunnel.getConfig().isInspect());
        assertNotNull(tunnel.getPublicUrl());
        assertThat(tunnel.getPublicUrl(), startsWith("https://"));
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
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withName("my-tunnel")
                .build(javaNgrokConfigV2);

        // WHEN
        final Tunnel tunnel = ngrokClientV2.connect(createTunnel);

        // THEN
        assertThat(tunnel.getName(), startsWith("my-tunnel (http)"));
        assertEquals("http", tunnel.getProto());
        assertEquals("http://localhost:80", tunnel.getConfig().getAddr());
    }

    @Test
    public void testMultipleConnectionsNoTokenFailsV2() throws InterruptedException {
        // WHEN
        ngrokClientV2.connect(new CreateTunnel.Builder().withAddr(5000).build(javaNgrokConfigV2));
        Thread.sleep(1000);
        final JavaNgrokHTTPException exception = assertThrows(JavaNgrokHTTPException.class, () -> ngrokClientV2.connect(new CreateTunnel.Builder().withAddr(5001).build(javaNgrokConfigV2)));

        // THEN
        assertEquals(HTTP_BAD_GATEWAY, exception.getStatusCode());
        assertEquals(String.format("%s/api/tunnels", ngrokProcessV2.getApiUrl()), exception.getUrl());
        assertThat(exception.getBody(), containsString("account may not run more than 2 tunnels"));
    }

    @Test
    public void testGetTunnels() {
        // GIVEN
        final Tunnel tunnel = ngrokClientV2.connect();

        // WHEN
        final List<Tunnel> tunnels = ngrokClientV2.getTunnels();

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
    public void testConnectBindTlsBothV2() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withBindTls(BindTls.BOTH)
                .build(javaNgrokConfigV2);
        final Tunnel tunnel = ngrokClientV2.connect(createTunnel);

        // WHEN
        final List<Tunnel> tunnels = ngrokClientV2.getTunnels();

        // THEN
        assertEquals(2, tunnels.size());
        assertThat(tunnel.getPublicUrl(), startsWith("http://"));
    }

    @Test
    public void testConnectBindTlsHttpsOnlyV2() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withBindTls(true)
                .build();
        final Tunnel tunnel = ngrokClientV2.connect(createTunnel);

        // WHEN
        final List<Tunnel> tunnels = ngrokClientV2.getTunnels();

        // THEN
        assertEquals(1, tunnels.size());
        assertThat(tunnel.getPublicUrl(), startsWith("https://"));
    }

    @Test
    public void testConnectBindTlsHttpOnlyV2() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withBindTls(false)
                .build();
        final Tunnel tunnel = ngrokClientV2.connect(createTunnel);

        // WHEN
        final List<Tunnel> tunnels = ngrokClientV2.getTunnels();

        // THEN
        assertEquals(1, tunnels.size());
        assertThat(tunnel.getPublicUrl(), startsWith("http://"));
    }

    @Test
    public void testConnectSchemesHttpV3() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withSchemes(List.of("http"))
                .build(javaNgrokConfigV3);
        final Tunnel tunnel = ngrokClientV3.connect(createTunnel);

        // WHEN
        final List<Tunnel> tunnels = ngrokClientV3.getTunnels();

        // THEN
        assertEquals(1, tunnels.size());
        assertThat(tunnel.getPublicUrl(), startsWith("http://"));
    }

    @Test
    public void testConnectSchemesHttpsV3() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withSchemes(List.of("https"))
                .build(javaNgrokConfigV3);
        final Tunnel tunnel = ngrokClientV3.connect(createTunnel);

        // WHEN
        final List<Tunnel> tunnels = ngrokClientV3.getTunnels();

        // THEN
        assertEquals(1, tunnels.size());
        assertThat(tunnel.getPublicUrl(), startsWith("https://"));
    }

    @Test
    public void testConnectSchemesHttpHttpsV3() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withSchemes(List.of("http", "https"))
                .build(javaNgrokConfigV3);
        final Tunnel tunnel = ngrokClientV3.connect(createTunnel);

        // WHEN
        final List<Tunnel> tunnels = ngrokClientV3.getTunnels();

        // THEN
        assertEquals(2, tunnels.size());
        assertThat(tunnel.getPublicUrl(), startsWith("https://"));
    }

    @Test
    public void testBindTlsUpgradedToSchemesV3() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withBindTls(BindTls.BOTH)
                .build(javaNgrokConfigV3);
        final Tunnel tunnel = ngrokClientV3.connect(createTunnel);

        // WHEN
        final List<Tunnel> tunnels = ngrokClientV3.getTunnels();

        // THEN
        assertEquals(2, tunnels.size());
        assertThat(tunnel.getPublicUrl(), startsWith("https://"));
    }

    @Test
    public void testDisconnectV2() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withName("my-tunnel")
                .build(javaNgrokConfigV2);
        final Tunnel tunnel = ngrokClientV2.connect(createTunnel);
        assertTrue(ngrokClientV2.getNgrokProcess().isRunning());
        final List<Tunnel> tunnels1 = ngrokClientV2.getTunnels();
        assertEquals(2, tunnels1.size());

        // WHEN
        ngrokClientV2.disconnect(tunnel.getPublicUrl());

        // THEN
        final List<Tunnel> tunnels2 = ngrokClientV2.getTunnels();
        assertEquals(1, tunnels2.size());
    }

    @Test
    public void testDisconnectV3() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withName("my-tunnel")
                .build(javaNgrokConfigV3);
        final Tunnel tunnel = ngrokClientV3.connect(createTunnel);
        assertTrue(ngrokClientV3.getNgrokProcess().isRunning());

        // WHEN
        ngrokClientV3.disconnect(tunnel.getPublicUrl());

        // THEN
        final List<Tunnel> tunnels = ngrokClientV3.getTunnels();
        assertEquals(0, tunnels.size());
    }

    @Test
    public void testKill() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withName("my-tunnel").build(javaNgrokConfigV2);
        ngrokClientV2.connect(createTunnel);

        // WHEN
        ngrokClientV2.kill();

        // THEN
        assertFalse(ngrokClientV2.getNgrokProcess().isRunning());
    }

    @Test
    public void testGetVersion() {
        // WHEN
        final Version version = ngrokClientV2.getVersion();

        // THEN
        assertNotNull(version.getJavaNgrokVersion());
        assertNotEquals(version.getNgrokVersion(), "unknown");
    }

    @Test
    public void testRegionalTcpV2() {
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV2)
                .withAuthToken(ngrokAuthToken)
                .withRegion(Region.AU)
                .build();
        ngrokProcessV2_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder()
                .withJavaNgrokConfig(javaNgrokConfig2)
                .withNgrokProcess(ngrokProcessV2_2)
                .build();
        assertFalse(ngrokClient2.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withProto(Proto.TCP)
                .withAddr(5000)
                .build(javaNgrokConfigV2);

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
    public void testRegionalTcpV3() {
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
                .withAuthToken(ngrokAuthToken)
                .withRegion(Region.AU)
                .build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder()
                .withJavaNgrokConfig(javaNgrokConfig2)
                .withNgrokProcess(ngrokProcessV3_2)
                .build();
        assertFalse(ngrokClient2.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withProto(Proto.TCP)
                .withAddr(5000)
                .build(javaNgrokConfigV2);

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
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV2)
                .withAuthToken(ngrokAuthToken)
                .withRegion(Region.AU)
                .build();
        ngrokProcessV2_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder()
                .withJavaNgrokConfig(javaNgrokConfig2)
                .withNgrokProcess(ngrokProcessV2_2)
                .build();
        assertFalse(ngrokClient2.getNgrokProcess().isRunning());
        final String subdomain = createUniqueSubdomain();
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withSubdomain(subdomain)
                .build(javaNgrokConfigV2);

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
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        ngrokClientV2.getNgrokProcess().setAuthToken(ngrokAuthToken);
        assertFalse(ngrokClientV2.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withAddr("file:///")
                .build(javaNgrokConfigV2);

        // WHEN
        final Tunnel tunnel = ngrokClientV2.connect(createTunnel);

        // THEN
        assertTrue(ngrokClientV2.getNgrokProcess().isRunning());
        assertThat(tunnel.getName(), startsWith("http-file-"));
        assertEquals("http", tunnel.getProto());
        assertEquals("file:///", tunnel.getConfig().getAddr());
        assertNotNull(tunnel.getPublicUrl());
        assertThat(tunnel.getPublicUrl(), startsWith("http://"));
    }

    @Test
    public void testDisconnectFileserver() throws InterruptedException {
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        ngrokClientV2.getNgrokProcess().setAuthToken(ngrokAuthToken);
        assertFalse(ngrokClientV2.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withAddr("file:///")
                .build(javaNgrokConfigV2);
        final String publicUrl = ngrokClientV2.connect(createTunnel).getPublicUrl();
        Thread.sleep(1000);

        // WHEN
        ngrokClientV2.disconnect(publicUrl);
        Thread.sleep(1000);
        final List<Tunnel> tunnels = ngrokClientV2.getTunnels();

        // THEN
        assertTrue(ngrokClientV2.getNgrokProcess().isRunning());
        // There is still one tunnel left, as we only disconnected the http tunnel
        assertEquals(1, tunnels.size());
    }

    @Test
    public void testGetTunnelFileserver() throws InterruptedException {
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        ngrokClientV2.getNgrokProcess().setAuthToken(ngrokAuthToken);
        assertFalse(ngrokClientV2.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withAddr("file:///")
                .build(javaNgrokConfigV2);
        final Tunnel tunnel = ngrokClientV2.connect(createTunnel);
        Thread.sleep(1000);
        final String apiUrl = ngrokClientV2.getNgrokProcess().getApiUrl();

        // WHEN
        final Response<Tunnel> response = ngrokClientV2.getHttpClient().get(String.format("%s%s", apiUrl, tunnel.getUri()), Tunnel.class);

        // THEN
        assertEquals(tunnel.getName(), response.getBody().getName());
        assertThat(tunnel.getName(), startsWith("http-file-"));
    }

    @Test
    public void testRefreshMetrics() throws MalformedURLException, InterruptedException {
        // GIVEN
        ngrokClientV2.getNgrokProcess().start();
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withName("my-tunnel")
                .withAddr(new URL(ngrokClientV2.getNgrokProcess().getApiUrl()).getPort())
                .withBindTls(true)
                .build();
        final Tunnel tunnel = ngrokClientV2.connect(createTunnel);
        Thread.sleep(1000);
        assertEquals(0, tunnel.getMetrics().get("http").getCount());

        ngrokClientV2.getHttpClient().get(String.format("%s/status", tunnel.getPublicUrl()), Object.class);

        Thread.sleep(3000);

        ngrokClientV2.refreshMetrics(tunnel);

        assertThat(tunnel.getMetrics().get("http").getCount(), greaterThan(0));
    }

    @Test
    public void testTunnelDefinitionsV2() {
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        final String subdomain = createUniqueSubdomain();
        final Map<String, Object> httpTunnelConfig = Map.of(
                "proto", "http",
                "addr", "8000",
                "subdomain", subdomain,
                "inspect", Boolean.FALSE,
                "bind_tls", Boolean.TRUE);
        final Map<String, Object> tcpTunnelConfig = Map.of(
                "proto", "tcp",
                "addr", "22");
        final Map<String, Object> tunnelsConfig = Map.of(
                "http-tunnel", httpTunnelConfig,
                "tcp-tunnel", tcpTunnelConfig);
        final Map<String, Object> config = Map.of("tunnels", tunnelsConfig);

        final Path configPath2 = Paths.get(javaNgrokConfigV2.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV2.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV2)
                .withConfigPath(configPath2)
                .withAuthToken(ngrokAuthToken)
                .build();
        ngrokProcessV2_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder()
                .withJavaNgrokConfig(javaNgrokConfig2)
                .withNgrokProcess(ngrokProcessV2_2)
                .build();

        // WHEN
        final CreateTunnel createHttpTunnel = new CreateTunnel.Builder()
                .withName("http-tunnel")
                .build(javaNgrokConfigV2);
        final Tunnel httpTunnel = ngrokClient2.connect(createHttpTunnel);
        final CreateTunnel createSshTunnel = new CreateTunnel.Builder()
                .withName("tcp-tunnel")
                .build(javaNgrokConfigV2);
        final Tunnel sshTunnel = ngrokClient2.connect(createSshTunnel);
        final List<Tunnel> tunnels = ngrokClient2.getTunnels();

        // THEN
        assertEquals(2, tunnels.size());
        assertEquals("http-tunnel", httpTunnel.getName());
        assertEquals("http://localhost:8000", httpTunnel.getConfig().getAddr());
        assertEquals("https", httpTunnel.getProto());
        assertFalse(httpTunnel.getConfig().isInspect());
        assertEquals(String.format("https://%s.ngrok.io", subdomain), httpTunnel.getPublicUrl());
        assertEquals("tcp-tunnel", sshTunnel.getName());
        assertEquals("localhost:22", sshTunnel.getConfig().getAddr());
        assertEquals("tcp", sshTunnel.getProto());
        assertThat(sshTunnel.getPublicUrl(), startsWith("tcp://"));
    }

    @Test
    public void testTunnelDefinitionsV3() {
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        final String subdomain = createUniqueSubdomain();
        final Map<String, Object> httpTunnelConfig = Map.of(
                "proto", "http",
                "addr", "8000",
                "subdomain", subdomain,
                "inspect", Boolean.FALSE,
                "schemes", List.of("http"));
        final Map<String, Object> tcpTunnelConfig = Map.of(
                "proto", "tcp",
                "addr", "22");
        final Map<String, Object> tunnelsConfig = Map.of(
                "http-tunnel", httpTunnelConfig,
                "tcp-tunnel", tcpTunnelConfig);
        final Map<String, Object> config = Map.of("tunnels", tunnelsConfig);

        final Path configPath2 = Paths.get(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV3.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
                .withConfigPath(configPath2)
                .withAuthToken(ngrokAuthToken)
                .build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder()
                .withJavaNgrokConfig(javaNgrokConfig2)
                .withNgrokProcess(ngrokProcessV3_2)
                .build();

        // WHEN
        final CreateTunnel createHttpTunnel = new CreateTunnel.Builder()
                .withName("http-tunnel")
                .build(javaNgrokConfigV3);
        final Tunnel httpTunnel = ngrokClient2.connect(createHttpTunnel);
        final CreateTunnel createSshTunnel = new CreateTunnel.Builder()
                .withName("tcp-tunnel")
                .build(javaNgrokConfigV3);
        final Tunnel sshTunnel = ngrokClient2.connect(createSshTunnel);
        final List<Tunnel> tunnels = ngrokClient2.getTunnels();

        // THEN
        assertEquals(2, tunnels.size());
        assertEquals("http-tunnel", httpTunnel.getName());
        assertEquals("http://localhost:8000", httpTunnel.getConfig().getAddr());
        assertEquals("http", httpTunnel.getProto());
        assertFalse(httpTunnel.getConfig().isInspect());
        assertEquals(String.format("http://%s.ngrok.io", subdomain), httpTunnel.getPublicUrl());
        assertEquals("tcp-tunnel", sshTunnel.getName());
        assertEquals("localhost:22", sshTunnel.getConfig().getAddr());
        assertEquals("tcp", sshTunnel.getProto());
        assertThat(sshTunnel.getPublicUrl(), startsWith("tcp://"));
    }

    @Test
    public void testTunnelDefinitionsJavaNgrokDefaultWithOverrides() {
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        final String subdomain1 = createUniqueSubdomain();
        final Map<String, Object> defaultTunnelConfig = Map.of("proto", "http", "addr", "8080", "subdomain", subdomain1);
        final Map<String, Object> tunnelsConfig = Map.of("java-ngrok-default", defaultTunnelConfig);
        final Map<String, Object> config = Map.of("tunnels", tunnelsConfig);

        final Path configPath2 = Paths.get(javaNgrokConfigV2.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV2.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV2)
                .withConfigPath(configPath2)
                .withAuthToken(ngrokAuthToken)
                .build();
        ngrokProcessV2_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder()
                .withJavaNgrokConfig(javaNgrokConfig2)
                .withNgrokProcess(ngrokProcessV2_2)
                .build();

        // WHEN
        final Tunnel ngrokTunnel1 = ngrokClient2.connect();
        final String subdomain2 = createUniqueSubdomain();
        final CreateTunnel createTunnelSubdomain = new CreateTunnel.Builder()
                .withSubdomain(subdomain2)
                .withAddr(5000)
                .build(javaNgrokConfigV2);
        final Tunnel ngrokTunnel2 = ngrokClient2.connect(createTunnelSubdomain);

        // THEN
        assertEquals("java-ngrok-default (http)", ngrokTunnel1.getName());
        assertEquals("http://localhost:8080", ngrokTunnel1.getConfig().getAddr());
        assertEquals("http", ngrokTunnel1.getProto());
        assertEquals(String.format("http://%s.ngrok.io", subdomain1), ngrokTunnel1.getPublicUrl());
        assertEquals("java-ngrok-default (http)", ngrokTunnel2.getName());
        assertEquals("http://localhost:5000", ngrokTunnel2.getConfig().getAddr());
        assertEquals("http", ngrokTunnel2.getProto());
        assertEquals(String.format("http://%s.ngrok.io", subdomain2), ngrokTunnel2.getPublicUrl());
    }

    @Test
    public void testSetAuthToken() throws IOException {
        // WHEN
        ngrokClientV2.setAuthToken("807ad30a-73be-48d8");
        final String contents = Files.readString(javaNgrokConfigV2.getConfigPath());

        // THEN
        assertThat(contents, containsString("807ad30a-73be-48d8"));
        assertFalse(ngrokClientV2.getNgrokProcess().isRunning());
    }

    @Test
    public void testUpdate() {
        // WHEN
        ngrokClientV2.update();

        // THEN
        assertFalse(ngrokClientV2.getNgrokProcess().isRunning());
    }
}
