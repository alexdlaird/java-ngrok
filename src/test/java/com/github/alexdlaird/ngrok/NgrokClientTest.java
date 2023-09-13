/*
 * Copyright (c) 2023 Alex Laird
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

import com.github.alexdlaird.http.Response;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.installer.NgrokVersion;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.github.alexdlaird.util.StringUtils.isNotBlank;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertEquals(javaNgrokConfigV3, ngrokClientV3.getJavaNgrokConfig());
        assertEquals(ngrokProcessV3, ngrokClientV3.getNgrokProcess());
        assertEquals(ngrokInstaller, ngrokClientV3.getNgrokProcess().getNgrokInstaller());
        assertNotNull(ngrokClientV3.getHttpClient());
    }

    @Test
    public void testConnectV2() {
        // GIVEN
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");
        ngrokClientV2.setAuthToken(ngrokAuthToken);
        assertFalse(ngrokProcessV2.isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withNgrokVersion(NgrokVersion.V2)
                .withAddr(5000)
                .build();

        // WHEN
        final Tunnel tunnel = ngrokClientV2.connect(createTunnel);

        // THEN
        assertTrue(ngrokClientV2.getNgrokProcess().getVersion().startsWith("2"));
        assertTrue(ngrokClientV2.getNgrokProcess().isRunning());
        assertNull(tunnel.getId());
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
                .withNgrokVersion(NgrokVersion.V3)
                .withAddr(5000)
                .build();

        // WHEN
        final Tunnel tunnel = ngrokClientV3.connect(createTunnel);

        // THEN
        assertTrue(ngrokClientV3.getNgrokProcess().getVersion().startsWith("3"));
        assertTrue(ngrokClientV3.getNgrokProcess().isRunning());
        assertNotNull(tunnel.getId());
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
                .withNgrokVersion(NgrokVersion.V3)
                .withName("my-tunnel")
                .build();

        // WHEN
        final Tunnel tunnel = ngrokClientV3.connect(createTunnel);

        // THEN
        assertThat(tunnel.getName(), startsWith("my-tunnel"));
        assertEquals("https", tunnel.getProto());
        assertEquals("http://localhost:80", tunnel.getConfig().getAddr());
    }

    @Test
    public void testGetTunnels() {
        // GIVEN
        final Tunnel tunnel = ngrokClientV3.connect();

        // WHEN
        final List<Tunnel> tunnels = ngrokClientV3.getTunnels();

        // THEN
        assertEquals(1, tunnels.size());
        assertEquals("https", tunnels.get(0).getProto());
        assertEquals(tunnel.getPublicUrl(), tunnels.get(0).getPublicUrl());
        assertEquals("http://localhost:80", tunnels.get(0).getConfig().getAddr());
    }

    @Test
    public void testConnectBindTlsBothV2() {
        // GIVEN
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");
        ngrokClientV2.setAuthToken(ngrokAuthToken);
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withNgrokVersion(NgrokVersion.V2)
                .withBindTls(BindTls.BOTH)
                .build();
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
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");
        ngrokClientV2.setAuthToken(ngrokAuthToken);
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withNgrokVersion(NgrokVersion.V2)
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
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");
        ngrokClientV2.setAuthToken(ngrokAuthToken);
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withNgrokVersion(NgrokVersion.V2)
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
                .withNgrokVersion(NgrokVersion.V3)
                .withSchemes(List.of("http"))
                .build();
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
                .withNgrokVersion(NgrokVersion.V3)
                .withSchemes(List.of("https"))
                .build();
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
                .withNgrokVersion(NgrokVersion.V3)
                .withSchemes(List.of("http", "https"))
                .build();
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
                .withNgrokVersion(NgrokVersion.V3)
                .withBindTls(BindTls.BOTH)
                .build();
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
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");
        ngrokClientV2.setAuthToken(ngrokAuthToken);
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withNgrokVersion(NgrokVersion.V2)
                .withName("my-tunnel")
                .build();
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
                .withNgrokVersion(NgrokVersion.V3)
                .withName("my-tunnel")
                .build();
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
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withName("my-tunnel").build();
        ngrokClientV3.connect(createTunnel);

        // WHEN
        ngrokClientV3.kill();

        // THEN
        assertFalse(ngrokClientV3.getNgrokProcess().isRunning());
    }

    @Test
    public void testGetVersion() {
        // WHEN
        final Version version = ngrokClientV3.getVersion();

        // THEN
        assertNotNull(version.getJavaNgrokVersion());
        assertNotEquals(version.getNgrokVersion(), "unknown");
    }

    @Test
    public void testRegionalTcpV2() {
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        final String subdomain = createUniqueSubdomain();
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
                .withNgrokVersion(NgrokVersion.V2)
                .withProto(Proto.TCP)
                .withAddr(5000)
                .withSubdomain(subdomain)
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
                .withNgrokVersion(NgrokVersion.V3)
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
    public void testAuthV2() {
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV2)
                .withAuthToken(ngrokAuthToken)
                .build();
        ngrokProcessV2_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder()
                .withJavaNgrokConfig(javaNgrokConfig2)
                .withNgrokProcess(ngrokProcessV2_2)
                .build();
        assertFalse(ngrokClient2.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withNgrokVersion(NgrokVersion.V2)
                .withAddr(5000)
                .withAuth("username:password")
                .build();

        // WHEN
        final Tunnel tunnel = ngrokClient2.connect(createTunnel);

        // THEN
        assertTrue(ngrokClient2.getNgrokProcess().isRunning());
        assertNotNull(tunnel.getPublicUrl());
    }

    @Test
    public void testAuthV3() {
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
                .withAuthToken(ngrokAuthToken)
                .build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder()
                .withJavaNgrokConfig(javaNgrokConfig2)
                .withNgrokProcess(ngrokProcessV3_2)
                .build();
        assertFalse(ngrokClient2.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withNgrokVersion(NgrokVersion.V3)
                .withAddr(5000)
                .withAuth("username:password")
                .build();

        // WHEN
        final Tunnel tunnel = ngrokClient2.connect(createTunnel);

        // THEN
        assertTrue(ngrokClient2.getNgrokProcess().isRunning());
        assertNotNull(tunnel.getPublicUrl());
    }

    @Test
    public void testRegionalSubdomain() {
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
        final String subdomain = createUniqueSubdomain();
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withSubdomain(subdomain)
                .build();

        // WHEN
        final Tunnel tunnel = ngrokClient2.connect(createTunnel);

        // THEN
        assertTrue(ngrokClient2.getNgrokProcess().isRunning());
        assertNotNull(tunnel.getPublicUrl());
        assertThat(tunnel.getPublicUrl(), containsString("https://"));
        assertThat(tunnel.getPublicUrl(), containsString(".au."));
        assertThat(tunnel.getPublicUrl(), containsString(subdomain));
    }

    @Test
    public void testConnectFileserver() {
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        ngrokClientV3.getNgrokProcess().setAuthToken(ngrokAuthToken);
        assertFalse(ngrokClientV3.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withAddr("file:///")
                .build();

        // WHEN
        final Tunnel tunnel = ngrokClientV3.connect(createTunnel);

        // THEN
        assertTrue(ngrokClientV3.getNgrokProcess().isRunning());
        assertThat(tunnel.getName(), startsWith("http-file-"));
        assertEquals("https", tunnel.getProto());
        assertEquals("file:///", tunnel.getConfig().getAddr());
        assertNotNull(tunnel.getPublicUrl());
        assertThat(tunnel.getPublicUrl(), startsWith("https://"));
    }

    @Test
    public void testDisconnectFileserverV2() throws InterruptedException {
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        ngrokClientV2.getNgrokProcess().setAuthToken(ngrokAuthToken);
        assertFalse(ngrokClientV2.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withAddr("file:///")
                .build();
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
        ngrokClientV3.getNgrokProcess().setAuthToken(ngrokAuthToken);
        assertFalse(ngrokClientV3.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withAddr("file:///")
                .build();
        final Tunnel tunnel = ngrokClientV3.connect(createTunnel);
        Thread.sleep(1000);
        final String apiUrl = ngrokClientV3.getNgrokProcess().getApiUrl();

        // WHEN
        final Response<Tunnel> response = ngrokClientV3.getHttpClient().get(String.format("%s%s", apiUrl, tunnel.getUri()), Tunnel.class);

        // THEN
        assertEquals(tunnel.getName(), response.getBody().getName());
        assertThat(tunnel.getName(), startsWith("http-file-"));
    }

    @Test
    public void testRefreshMetrics() throws MalformedURLException, InterruptedException {
        // GIVEN
        ngrokClientV3.getNgrokProcess().start();
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withName("my-tunnel")
                .withAddr(new URL(ngrokClientV3.getNgrokProcess().getApiUrl()).getPort())
                .withBindTls(true)
                .build();
        final Tunnel tunnel = ngrokClientV3.connect(createTunnel);
        Thread.sleep(1000);
        assertEquals(0, tunnel.getMetrics().get("http").getCount());

        ngrokClientV3.getHttpClient().get(String.format("%s/status", tunnel.getPublicUrl()), Object.class);

        Thread.sleep(3000);

        ngrokClientV3.refreshMetrics(tunnel);

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
                .withNgrokVersion(NgrokVersion.V2)
                .withName("http-tunnel")
                .build();
        final Tunnel httpTunnel = ngrokClient2.connect(createHttpTunnel);
        final CreateTunnel createSshTunnel = new CreateTunnel.Builder()
                .withNgrokVersion(NgrokVersion.V2)
                .withName("tcp-tunnel")
                .build();
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
                "schemes", List.of("http"),
                "circuit_breaker", 0.5f);
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
                .withNgrokVersion(NgrokVersion.V3)
                .withName("http-tunnel")
                .build();
        final Tunnel httpTunnel = ngrokClient2.connect(createHttpTunnel);
        final CreateTunnel createSshTunnel = new CreateTunnel.Builder()
                .withNgrokVersion(NgrokVersion.V3)
                .withName("tcp-tunnel")
                .build();
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
    public void testTunnelDefinitionsV3CloudEdge() {
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");
        final String ngrokHttpEdge = System.getenv("NGROK_HTTP_EDGE");
        assumeTrue(isNotBlank(System.getenv("NGROK_HTTP_EDGE")), "NGROK_HTTP_EDGE environment variable not set");
        final String ngrokTcpEdge = System.getenv("NGROK_TCP_EDGE");
        assumeTrue(isNotBlank(System.getenv("NGROK_TCP_EDGE")), "NGROK_TCP_EDGE environment variable not set");
        final String ngrokApiKey = System.getenv("NGROK_API_KEY");
        assumeTrue(isNotBlank(System.getenv("NGROK_API_KEY")), "NGROK_API_KEY environment variable not set");
        final String ngrokHttpEdgeEndpoint = System.getenv("NGROK_HTTP_EDGE_ENDPOINT");
        assumeTrue(isNotBlank(System.getenv("NGROK_HTTP_EDGE_ENDPOINT")), "NGROK_HTTP_EDGE_ENDPOINT environment variable not set");
        final String ngrokTcpEdgeEndpoint = System.getenv("NGROK_TCP_EDGE_ENDPOINT");
        assumeTrue(isNotBlank(System.getenv("NGROK_TCP_EDGE_ENDPOINT")), "NGROK_TCP_EDGE_ENDPOINT environment variable not set");

        // GIVEN
        final Map<String, Object> edgeHttpTunnelConfig = Map.of(
                "addr", "80",
                "labels", List.of(String.format("edge=%s", ngrokHttpEdge)));
        final Map<String, Object> edgeTcpTunnelConfig = Map.of(
                "addr", "22",
                "labels", List.of(String.format("edge=%s", ngrokTcpEdge)));
        final Map<String, Object> tunnelsConfig = Map.of(
                "edge-http-tunnel", edgeHttpTunnelConfig,
                "edge-tcp-tunnel", edgeTcpTunnelConfig);
        final Map<String, Object> config = Map.of("tunnels", tunnelsConfig);

        final Path configPath2 = Paths.get(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV3.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
                .withConfigPath(configPath2)
                .withAuthToken(ngrokAuthToken)
                .withApiKey(ngrokApiKey)
                .build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder()
                .withJavaNgrokConfig(javaNgrokConfig2)
                .withNgrokProcess(ngrokProcessV3_2)
                .build();

        // WHEN
        final CreateTunnel createHttpEdgeTunnel = new CreateTunnel.Builder()
                .withNgrokVersion(NgrokVersion.V3)
                .withName("edge-http-tunnel")
                .build();
        final CreateTunnel createTcpEdgeTunnel = new CreateTunnel.Builder()
                .withNgrokVersion(NgrokVersion.V3)
                .withName("edge-tcp-tunnel")
                .build();
        final Tunnel httpEdgeTunnel = ngrokClient2.connect(createHttpEdgeTunnel);
        final Tunnel tcpEdgeTunnel = ngrokClient2.connect(createTcpEdgeTunnel);
        final List<Tunnel> tunnels = ngrokClient2.getTunnels();
        tunnels.sort(Comparator.comparing(Tunnel::getProto));

        // THEN
        assertEquals("edge-http-tunnel", httpEdgeTunnel.getName());
        assertEquals("http://localhost:80", httpEdgeTunnel.getConfig().getAddr());
        assertEquals("https", httpEdgeTunnel.getProto());
        assertEquals(ngrokHttpEdgeEndpoint, httpEdgeTunnel.getPublicUrl());
        assertEquals("edge-tcp-tunnel", tcpEdgeTunnel.getName());
        assertEquals("tcp://localhost:22", tcpEdgeTunnel.getConfig().getAddr());
        assertEquals("tcp", tcpEdgeTunnel.getProto());
        assertEquals(ngrokTcpEdgeEndpoint, tcpEdgeTunnel.getPublicUrl());
        assertEquals(2, tunnels.size());
        assertEquals("edge-http-tunnel", tunnels.get(0).getName());
        assertEquals("http://localhost:80", tunnels.get(0).getConfig().getAddr());
        assertEquals("https", tunnels.get(0).getProto());
        assertEquals(ngrokHttpEdgeEndpoint, tunnels.get(0).getPublicUrl());
        assertEquals("edge-tcp-tunnel", tunnels.get(1).getName());
        assertEquals("tcp://localhost:22", tunnels.get(1).getConfig().getAddr());
        assertEquals("tcp", tunnels.get(1).getProto());
        assertEquals(ngrokTcpEdgeEndpoint, tunnels.get(1).getPublicUrl());
    }

    @Test
    public void testTunnelDefinitionsV3OAuth() {
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        final String subdomain = createUniqueSubdomain();
        final Map<String, Object> httpTunnelConfig = Map.of(
                "proto", "http",
                "addr", "8000",
                "subdomain", subdomain,
                "oauth", Map.of(
                        "provider", "google",
                        "allow_domains", List.of("pyngrok.com"),
                        "allow_emails", List.of("email@pyngrok.com")
                ));
        final Map<String, Object> tunnelsConfig = Map.of(
                "http-tunnel", httpTunnelConfig);
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
                .build();
        final Tunnel httpTunnel = ngrokClient2.connect(createHttpTunnel);
        final List<Tunnel> tunnels = ngrokClient2.getTunnels();

        final Response<Object> response = ngrokClientV3.getHttpClient().get(String.format(httpTunnel.getPublicUrl()), Object.class);

        // THEN
        assertEquals(1, tunnels.size());
        assertEquals("http-tunnel", httpTunnel.getName());
        assertEquals("http://localhost:8000", httpTunnel.getConfig().getAddr());
        assertEquals("https", httpTunnel.getProto());
        assertEquals(String.format("https://%s.ngrok.io", subdomain), httpTunnel.getPublicUrl());
        assertTrue(response.getBodyRaw().contains("Sign in - Google Accounts"));
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
        final Tunnel ngrokTunnel1 = ngrokClient2.connect();
        final String subdomain2 = createUniqueSubdomain();
        final CreateTunnel createTunnelSubdomain = new CreateTunnel.Builder()
                .withSubdomain(subdomain2)
                .withAddr(5000)
                .build();
        final Tunnel ngrokTunnel2 = ngrokClient2.connect(createTunnelSubdomain);

        // THEN
        assertEquals("java-ngrok-default", ngrokTunnel1.getName());
        assertEquals("http://localhost:8080", ngrokTunnel1.getConfig().getAddr());
        assertEquals("https", ngrokTunnel1.getProto());
        assertEquals(String.format("https://%s.ngrok.io", subdomain1), ngrokTunnel1.getPublicUrl());
        assertEquals("java-ngrok-default", ngrokTunnel2.getName());
        assertEquals("http://localhost:5000", ngrokTunnel2.getConfig().getAddr());
        assertEquals("https", ngrokTunnel2.getProto());
        assertEquals(String.format("https://%s.ngrok.io", subdomain2), ngrokTunnel2.getPublicUrl());
    }

    @Test
    public void testSetAuthToken() throws IOException {
        // WHEN
        ngrokClientV3.setAuthToken("807ad30a-73be-48d8");
        final String contents = Files.readString(javaNgrokConfigV3.getConfigPath());

        // THEN
        assertThat(contents, containsString("807ad30a-73be-48d8"));
        assertFalse(ngrokClientV3.getNgrokProcess().isRunning());
    }

    @Test
    public void testUpdate() {
        // WHEN
        ngrokClientV3.update();

        // THEN
        assertFalse(ngrokClientV3.getNgrokProcess().isRunning());
    }
}
