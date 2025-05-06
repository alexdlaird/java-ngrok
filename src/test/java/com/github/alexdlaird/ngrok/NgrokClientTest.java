/*
 * Copyright (c) 2021-2025 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok;

import com.github.alexdlaird.exception.JavaNgrokException;
import com.github.alexdlaird.exception.JavaNgrokHTTPException;
import com.github.alexdlaird.exception.NgrokException;
import com.github.alexdlaird.http.DefaultHttpClient;
import com.github.alexdlaird.http.HttpClient;
import com.github.alexdlaird.http.HttpClientException;
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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static com.github.alexdlaird.ngrok.installer.NgrokInstaller.getNgrokBin;
import static com.github.alexdlaird.util.ProcessUtils.captureRunProcess;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NgrokClientTest extends NgrokTestCase {

    private NgrokClient ngrokClientV2;

    private NgrokClient ngrokClientV3;

    private final String ngrokSubdomain = System.getenv()
                                                .getOrDefault("NGROK_SUBDOMAIN", System.getProperty("user.name"));

    private Map<String, String> reservedAddrTcpEdge;

    private Map<String, String> reservedDomain;

    private Map<String, String> reservedDomainHttpEdge;

    private Map<String, String> reservedDomainTlsEdge;

    private Map<String, String> tcpEdge;

    private Map<String, String> httpEdge;

    private Map<String, String> tlsEdge;

    private final JavaNgrokConfig testcaseJavaNgrokConfig = new JavaNgrokConfig.Builder()
        .withConfigPath(Paths.get("build", ".testcase-ngrok", "config.yml").toAbsolutePath())
        .withNgrokPath(Paths.get("build", "bin", "testcase-ngrok", getNgrokBin()))
        .build();

    private Map<String, String> edge;

    @BeforeAll
    public void setUpClass()
        throws IOException, InterruptedException {
        if (isNotBlank(System.getenv("NGROK_API_KEY"))) {
            new NgrokProcess(testcaseJavaNgrokConfig, ngrokInstaller);

            final String domain = String.format("%s.ngrok.dev", this.ngrokSubdomain);
            try {
                this.givenNgrokReservedDomain(this.testcaseJavaNgrokConfig, domain);
            } catch (final NgrokException ex) {
                if (!ex.getMessage().contains("domain is already reserved")) {
                    throw ex;
                }
            }

            this.reservedAddrTcpEdge = this.givenNgrokReservedAddr(this.testcaseJavaNgrokConfig);
            final String[] hostAndPort = this.reservedAddrTcpEdge.get("addr").split(":");
            this.tcpEdge = this.givenNgrokEdgeExists(this.testcaseJavaNgrokConfig, "tcp", hostAndPort[0],
                Integer.parseInt(hostAndPort[1]));

            final String subdomain = this.createUniqueSubdomain();
            final String reservedDomain = String.format("%s.%s.ngrok.dev", subdomain, this.ngrokSubdomain);
            this.reservedDomain = this.givenNgrokReservedDomain(this.testcaseJavaNgrokConfig, reservedDomain);

            final String subdomainHttp = this.createUniqueSubdomain();
            final String reservedDomainHttp = String.format("%s.%s.ngrok.dev", subdomainHttp, this.ngrokSubdomain);
            this.reservedDomainHttpEdge = this.givenNgrokReservedDomain(this.testcaseJavaNgrokConfig,
                reservedDomainHttp);
            this.httpEdge = this.givenNgrokEdgeExists(this.testcaseJavaNgrokConfig, "https", reservedDomainHttp, 443);

            final String subdomainTls = this.createUniqueSubdomain();
            final String reservedDomainTls = String.format("%s.%s.ngrok.dev", subdomainTls, this.ngrokSubdomain);
            this.reservedDomainTlsEdge = this.givenNgrokReservedDomain(this.testcaseJavaNgrokConfig,
                reservedDomainTls);
            this.tlsEdge = this.givenNgrokEdgeExists(this.testcaseJavaNgrokConfig, "tls", reservedDomainTls, 443);
        }
    }

    @AfterAll
    public void tearDownClass()
        throws IOException, InterruptedException {
        if (isNotBlank(System.getenv("NGROK_API_KEY"))) {
            captureRunProcess(this.testcaseJavaNgrokConfig.getNgrokPath(),
                Collections.unmodifiableList(
                    Stream.of("--config", this.testcaseJavaNgrokConfig.getConfigPath().toString(),
                    "api", "edges", "tls", "delete", this.tlsEdge.get("id")).collect(Collectors.toList())));
            captureRunProcess(this.testcaseJavaNgrokConfig.getNgrokPath(),
                Collections.unmodifiableList(
                    Stream.of("--config", this.testcaseJavaNgrokConfig.getConfigPath().toString(),
                    "api", "edges", "https", "delete", this.httpEdge.get("id")).collect(Collectors.toList())));
            captureRunProcess(this.testcaseJavaNgrokConfig.getNgrokPath(),
                Collections.unmodifiableList(
                    Stream.of("--config", this.testcaseJavaNgrokConfig.getConfigPath().toString(),
                    "api", "edges", "tcp", "delete", this.tcpEdge.get("id")).collect(Collectors.toList())));
            captureRunProcess(this.testcaseJavaNgrokConfig.getNgrokPath(),
                Collections.unmodifiableList(
                    Stream.of("--config", this.testcaseJavaNgrokConfig.getConfigPath().toString(),
                    "api", "reserved-domains", "delete", this.reservedDomain.get("id")).collect(Collectors.toList())));
            captureRunProcess(this.testcaseJavaNgrokConfig.getNgrokPath(),
                Collections.unmodifiableList(
                    Stream.of("--config", this.testcaseJavaNgrokConfig.getConfigPath().toString(),
                    "api", "reserved-domains", "delete", this.reservedDomainTlsEdge.get("id")).collect(Collectors.toList())));
            captureRunProcess(this.testcaseJavaNgrokConfig.getNgrokPath(),
                Collections.unmodifiableList(
                    Stream.of("--config", this.testcaseJavaNgrokConfig.getConfigPath().toString(),
                    "api", "reserved-domains", "delete", this.reservedDomainHttpEdge.get("id")).collect(Collectors.toList())));
            captureRunProcess(this.testcaseJavaNgrokConfig.getNgrokPath(),
                Collections.unmodifiableList(
                    Stream.of("--config", this.testcaseJavaNgrokConfig.getConfigPath().toString(),
                    "api", "reserved-addrs", "delete", this.reservedAddrTcpEdge.get("id")).collect(Collectors.toList())));
        }
    }

    @BeforeEach
    public void setUp() {
        super.setUp();

        ngrokClientV2 = new NgrokClient.Builder()
            .withJavaNgrokConfig(javaNgrokConfigV2)
            .withNgrokProcess(ngrokProcessV2)
            .withHttpClient(retryHttpClient)
            .build();
        ngrokClientV3 = new NgrokClient.Builder()
            .withJavaNgrokConfig(javaNgrokConfigV3)
            .withNgrokProcess(ngrokProcessV3)
            .withHttpClient(retryHttpClient)
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
        testRequiresEnvVar("NGROK_AUTHTOKEN");
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
        testRequiresEnvVar("NGROK_AUTHTOKEN");
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
    public void testConnectTls() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");
        testRequiresEnvVar("NGROK_API_KEY");

        // GIVEN
        assertFalse(ngrokClientV3.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
            .withNgrokVersion(NgrokVersion.V3)
            .withAddr(80)
            .withProto(Proto.TLS)
            .withDomain(this.reservedDomain.get("domain"))
            .withTerminateAt("upstream")
            .build();

        // WHEN
        final Tunnel tunnel = ngrokClientV3.connect(createTunnel);

        // THEN
        assertTrue(ngrokClientV3.getNgrokProcess().getVersion().startsWith("3"));
        assertTrue(ngrokClientV3.getNgrokProcess().isRunning());
        assertNotNull(tunnel.getId());
        assertThat(tunnel.getName(), startsWith("tls-80-"));
        assertEquals("tls", tunnel.getProto());
        assertEquals("localhost:80", tunnel.getConfig().getAddr());
        assertNotNull(tunnel.getPublicUrl());
        assertThat(tunnel.getPublicUrl(), startsWith("tls://"));
        assertEquals(tunnel.getPublicUrl(), String.format("tls://%s", this.reservedDomain.get("domain")));
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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
            .withNgrokVersion(NgrokVersion.V3)
            .withSchemes(Collections.singletonList("http"))
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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
            .withNgrokVersion(NgrokVersion.V3)
            .withSchemes(Collections.singletonList("https"))
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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
            .withNgrokVersion(NgrokVersion.V3)
            .withSchemes(Collections.unmodifiableList(
                Stream.of("http", "https")
                      .collect(Collectors.toList())))
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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

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
    public void testKill()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withName("my-tunnel").build();
        ngrokClientV3.connect(createTunnel);

        // WHEN
        ngrokClientV3.kill();
        Thread.sleep(1000);

        // THEN
        assertFalse(ngrokClientV3.getNgrokProcess().isRunning());
    }

    @Test
    public void testGetVersion() {
        // WHEN
        final Version version = ngrokClientV3.getVersion();

        // THEN
        assertNotNull(version.getJavaNgrokVersion());
        assertNotEquals("unknown", version.getNgrokVersion());
    }

    @Test
    public void testRegionalTcpV2() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final String subdomain = createUniqueSubdomain();
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV2)
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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        ngrokProcessV2_2 = new NgrokProcess(javaNgrokConfigV2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder()
            .withJavaNgrokConfig(javaNgrokConfigV2)
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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfigV3, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder()
            .withJavaNgrokConfig(javaNgrokConfigV3)
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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
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
    public void testDisconnectFileserverV2()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
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
    public void testGetTunnelFileserver()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        assertFalse(ngrokClientV3.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
            .withAddr("file:///")
            .build();
        final Tunnel tunnel = ngrokClientV3.connect(createTunnel);
        Thread.sleep(1000);
        final String apiUrl = ngrokClientV3.getNgrokProcess().getApiUrl();

        // WHEN
        final Response<Tunnel> response = ngrokClientV3.getHttpClient().get(String.format("%s%s", apiUrl,
            tunnel.getUri()), Tunnel.class);

        // THEN
        assertEquals(tunnel.getName(), response.getBody().getName());
        assertThat(tunnel.getName(), startsWith("http-file-"));
    }

    @Test
    public void testRefreshMetrics()
        throws MalformedURLException, InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final String subdomain = createUniqueSubdomain();
        final Map<String, Object> httpTunnelConfig = new HashMap<>();
        httpTunnelConfig.put("proto", "http");
        httpTunnelConfig.put("addr", "8000");
        httpTunnelConfig.put("subdomain", subdomain);
        httpTunnelConfig.put("inspect", Boolean.FALSE);
        httpTunnelConfig.put("bind_tls", Boolean.TRUE);
        final Map<String, Object> tcpTunnelConfig = new HashMap<>();
        tcpTunnelConfig.put("proto", "tcp");
        tcpTunnelConfig.put("addr", "22");
        final Map<String, Map<String, Object>> tunnelsConfig = new HashMap<>();
        tunnelsConfig.put("http-tunnel", Collections.unmodifiableMap(httpTunnelConfig));
        tunnelsConfig.put("tcp-tunnel", Collections.unmodifiableMap(tcpTunnelConfig));
        final Map<String, Object> config = Collections.singletonMap("tunnels",
            Collections.unmodifiableMap(tunnelsConfig));

        final Path configPath2 = Paths.get(javaNgrokConfigV2.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV2.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV2)
            .withConfigPath(configPath2)
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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final String subdomain = createUniqueSubdomain();
        final Map<String, Object> httpTunnelConfig = new HashMap<>();
        httpTunnelConfig.put("proto", "http");
        httpTunnelConfig.put("addr", "8000");
        httpTunnelConfig.put("subdomain", subdomain);
        httpTunnelConfig.put("inspect", Boolean.FALSE);
        httpTunnelConfig.put("schemes", Collections.singletonList("http"));
        httpTunnelConfig.put("circuit_breaker", 0.5f);
        final Map<String, Object> tcpTunnelConfig = new HashMap<>();
        tcpTunnelConfig.put("proto", "tcp");
        tcpTunnelConfig.put("addr", "22");
        final Map<String, Object> tunnelsConfig = new HashMap<>();
        tunnelsConfig.put("http-tunnel", Collections.unmodifiableMap(httpTunnelConfig));
        tunnelsConfig.put("tcp-tunnel", Collections.unmodifiableMap(tcpTunnelConfig));
        final Map<String, Object> config = Collections.singletonMap("tunnels",
            Collections.unmodifiableMap(tunnelsConfig));

        final Path configPath2 = Paths.get(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV3.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
            .withConfigPath(configPath2)
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
    public void testTunnelDefinitionsTls() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");
        testRequiresEnvVar("NGROK_API_KEY");

        // GIVEN
        final Map<String, Object> tlsTunnelConfig = new HashMap<>();
        tlsTunnelConfig.put("proto", "tls");
        tlsTunnelConfig.put("addr", "443");
        tlsTunnelConfig.put("domain", this.reservedDomain.get("domain"));
        tlsTunnelConfig.put("terminate_at", "upstream");
        final Map<String, Object> tunnelsConfig = Collections.singletonMap("tls-tunnel",
            Collections.unmodifiableMap(tlsTunnelConfig));
        final Map<String, Object> config = Collections.singletonMap("tunnels", tunnelsConfig);

        final Path configPath2 = Paths.get(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV3.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
            .withConfigPath(configPath2)
            .build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder()
            .withJavaNgrokConfig(javaNgrokConfig2)
            .withNgrokProcess(ngrokProcessV3_2)
            .build();

        // WHEN
        final CreateTunnel createTlsTunnel = new CreateTunnel.Builder()
            .withNgrokVersion(NgrokVersion.V3)
            .withName("tls-tunnel")
            .build();
        final Tunnel tlsTunnel = ngrokClient2.connect(createTlsTunnel);
        final List<Tunnel> tunnels = ngrokClient2.getTunnels();

        // THEN
        assertEquals(1, tunnels.size());
        assertEquals("tls-tunnel", tlsTunnel.getName());
        assertEquals("tls://localhost:443", tlsTunnel.getConfig().getAddr());
        assertEquals("tls", tlsTunnel.getProto());
        assertFalse(tlsTunnel.getConfig().isInspect());
        assertEquals(tlsTunnel.getPublicUrl(), String.format("tls://%s", this.reservedDomain.get("domain")));
    }

    @Test
    public void testTunnelDefinitionsV3HTTPEdge() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");
        testRequiresEnvVar("NGROK_API_KEY");

        // GIVEN
        final Map<String, Object> edgeHttpTunnelConfig = new HashMap<>();
        edgeHttpTunnelConfig.put("addr", "80");
        edgeHttpTunnelConfig.put("labels", Collections.singletonList(String.format("edge=%s",
            this.httpEdge.get("id"))));
        final Map<String, Object> tunnelsConfig = Collections.singletonMap("edge-http-tunnel", edgeHttpTunnelConfig);
        final Map<String, Object> config = Collections.singletonMap("tunnels", tunnelsConfig);

        final Path configPath2 = Paths.get(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV3.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
            .withConfigPath(configPath2)
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
        final Tunnel httpEdgeTunnel = ngrokClient2.connect(createHttpEdgeTunnel);
        final List<Tunnel> tunnels = ngrokClient2.getTunnels();

        // THEN
        assertEquals("edge-http-tunnel", httpEdgeTunnel.getName());
        assertEquals("http://localhost:80", httpEdgeTunnel.getConfig().getAddr());
        assertEquals("https", httpEdgeTunnel.getProto());
        assertEquals(String.format("https://%s:443", this.reservedDomainHttpEdge.get("domain")),
            httpEdgeTunnel.getPublicUrl());
        assertEquals(1, tunnels.size());
        assertEquals("edge-http-tunnel", tunnels.get(0).getName());
        assertEquals("http://localhost:80", tunnels.get(0).getConfig().getAddr());
        assertEquals("https", tunnels.get(0).getProto());
        assertEquals(String.format("https://%s:443", this.reservedDomainHttpEdge.get("domain")),
            tunnels.get(0).getPublicUrl());
    }

    @Test
    public void testTunnelDefinitionsV3TCPEdge() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");
        testRequiresEnvVar("NGROK_API_KEY");

        // GIVEN
        final String[] hostAndPort = this.reservedAddrTcpEdge.get("addr").split(":");
        final Map<String, Object> edgeTcpTunnelConfig = new HashMap<>();
        edgeTcpTunnelConfig.put("addr", "22");
        edgeTcpTunnelConfig.put("labels", Collections.singletonList(String.format("edge=%s", tcpEdge.get("id"))));
        final Map<String, Object> tunnelsConfig = Collections.singletonMap("edge-tcp-tunnel",
            Collections.unmodifiableMap(edgeTcpTunnelConfig));
        final Map<String, Object> config = Collections.singletonMap("tunnels", tunnelsConfig);

        final Path configPath2 = Paths.get(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV3.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
            .withConfigPath(configPath2)
            .build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder()
            .withJavaNgrokConfig(javaNgrokConfig2)
            .withNgrokProcess(ngrokProcessV3_2)
            .build();

        // WHEN
        final CreateTunnel createTcpEdgeTunnel = new CreateTunnel.Builder()
            .withNgrokVersion(NgrokVersion.V3)
            .withName("edge-tcp-tunnel")
            .build();
        final Tunnel tcpEdgeTunnel = ngrokClient2.connect(createTcpEdgeTunnel);
        final List<Tunnel> tunnels = ngrokClient2.getTunnels();

        // THEN
        assertEquals("edge-tcp-tunnel", tcpEdgeTunnel.getName());
        assertEquals("tcp://localhost:22", tcpEdgeTunnel.getConfig().getAddr());
        assertEquals("tcp", tcpEdgeTunnel.getProto());
        assertEquals(String.format("tcp://%s:%s", hostAndPort[0], hostAndPort[1]), tcpEdgeTunnel.getPublicUrl());
        assertEquals(1, tunnels.size());
        assertEquals("edge-tcp-tunnel", tunnels.get(0).getName());
        assertEquals("tcp://localhost:22", tunnels.get(0).getConfig().getAddr());
        assertEquals("tcp", tunnels.get(0).getProto());
        assertEquals(String.format("tcp://%s:%s", hostAndPort[0], hostAndPort[1]), tunnels.get(0).getPublicUrl());
    }

    @Test
    public void testTunnelDefinitionsV3TLSEdge() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");
        testRequiresEnvVar("NGROK_API_KEY");

        // GIVEN
        final Map<String, Object> edgeTlsTunnelConfig = new HashMap<>();
        edgeTlsTunnelConfig.put("addr", "443");
        edgeTlsTunnelConfig.put("labels", Collections.singletonList(String.format("edge=%s", tlsEdge.get("id"))));
        final Map<String, Object> tunnelsConfig = Collections.singletonMap("edge-tls-tunnel",
            Collections.unmodifiableMap(edgeTlsTunnelConfig));
        final Map<String, Object> config = Collections.singletonMap("tunnels", tunnelsConfig);

        final Path configPath2 = Paths.get(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV3.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
            .withConfigPath(configPath2)
            .build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder()
            .withJavaNgrokConfig(javaNgrokConfig2)
            .withNgrokProcess(ngrokProcessV3_2)
            .build();

        // WHEN
        final CreateTunnel createTlsEdgeTunnel = new CreateTunnel.Builder()
            .withNgrokVersion(NgrokVersion.V3)
            .withName("edge-tls-tunnel")
            .build();
        final Tunnel tlsEdgeTunnel = ngrokClient2.connect(createTlsEdgeTunnel);
        final List<Tunnel> tunnels = ngrokClient2.getTunnels();

        // THEN
        assertEquals("edge-tls-tunnel", tlsEdgeTunnel.getName());
        assertEquals("https://localhost:443", tlsEdgeTunnel.getConfig().getAddr());
        assertEquals("tls", tlsEdgeTunnel.getProto());
        assertEquals(String.format("tls://%s:443", this.reservedDomainTlsEdge.get("domain")),
            tlsEdgeTunnel.getPublicUrl());
        assertEquals(1, tunnels.size());
        assertEquals("edge-tls-tunnel", tunnels.get(0).getName());
        assertEquals("https://localhost:443", tunnels.get(0).getConfig().getAddr());
        assertEquals("tls", tunnels.get(0).getProto());
        assertEquals(String.format("tls://%s:443", this.reservedDomainTlsEdge.get("domain")),
            tunnels.get(0).getPublicUrl());
    }

    @Test
    public void testLabelsNoApiKeyFails() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");
        final String ngrokHttpEdge = testRequiresEnvVar("NGROK_HTTP_EDGE");

        // GIVEN
        final Map<String, Object> edgeHttpTunnelConfig = new HashMap<>();
        edgeHttpTunnelConfig.put("addr", "80");
        edgeHttpTunnelConfig.put("labels", Collections.singletonList(String.format("edge=%s", ngrokHttpEdge)));
        final Map<String, Object> tunnelsConfig = Collections.singletonMap("edge-tunnel",
            Collections.unmodifiableMap(edgeHttpTunnelConfig));
        final Map<String, Object> config = Collections.singletonMap("tunnels", tunnelsConfig);

        final Path configPath2 = Paths.get(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV3.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
            .withConfigPath(configPath2)
            .withApiKey("")
            .build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder()
            .withJavaNgrokConfig(javaNgrokConfig2)
            .withNgrokProcess(ngrokProcessV3_2)
            .build();

        // WHEN
        final CreateTunnel createEdgeTunnel = new CreateTunnel.Builder()
            .withNgrokVersion(NgrokVersion.V3)
            .withName("edge-tunnel")
            .build();
        assertThrows(JavaNgrokException.class, () -> ngrokClient2.connect(createEdgeTunnel));
    }

    @Test
    public void testTunnelDefinitionsV3OAuth()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final String subdomain = createUniqueSubdomain();
        HashMap<String, Object> oauthConfig = new HashMap<>();
        oauthConfig.put("provider", "google");
        oauthConfig.put("allow_domains", Collections.singletonList("java-ngrok.com"));
        oauthConfig.put("allow_emails", Collections.singletonList("email@java-ngrok.com"));
        final Map<String, Object> httpTunnelConfig = new HashMap<>();
        httpTunnelConfig.put("proto", "http");
        httpTunnelConfig.put("addr", "8000");
        httpTunnelConfig.put("subdomain", subdomain);
        httpTunnelConfig.put("oauth", Collections.unmodifiableMap(oauthConfig));
        final Map<String, Object> tunnelsConfig = Collections.singletonMap("http-tunnel",
            Collections.unmodifiableMap(httpTunnelConfig));
        final Map<String, Object> config = Collections.singletonMap("tunnels", tunnelsConfig);

        final Path configPath2 = Paths.get(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV3.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
            .withConfigPath(configPath2)
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

        final String responseBody = ngrokClientV3.getHttpClient().get(String.format(httpTunnel.getPublicUrl()),
            Object.class).getBodyRaw();

        // THEN
        assertEquals(1, tunnels.size());
        assertEquals("http-tunnel", httpTunnel.getName());
        assertEquals("http://localhost:8000", httpTunnel.getConfig().getAddr());
        assertEquals("https", httpTunnel.getProto());
        assertEquals(String.format("https://%s.ngrok.io", subdomain), httpTunnel.getPublicUrl());
        assertTrue(responseBody.contains("Sign in - Google Accounts"));
    }

    @Test
    public void testTunnelDefinitionsJavaNgrokDefaultWithOverrides() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final String subdomain1 = createUniqueSubdomain();
        final Map<String, Object> defaultTunnelConfig = new HashMap<>();
        defaultTunnelConfig.put("proto", "http");
        defaultTunnelConfig.put("addr", "8080");
        defaultTunnelConfig.put("subdomain", subdomain1);
        final Map<String, Object> tunnelsConfig = Collections.singletonMap("java-ngrok-default",
            Collections.unmodifiableMap(defaultTunnelConfig));
        final Map<String, Object> config = Collections.singletonMap("tunnels", tunnelsConfig);

        final Path configPath2 = Paths.get(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV3.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
            .withConfigPath(configPath2)
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
    public void testSetAuthTokenV2()
        throws IOException {
        // WHEN
        ngrokClientV2.setAuthToken("some-auth-token");
        final String contents = new String(Files.readAllBytes(javaNgrokConfigV2.getConfigPath()),
            StandardCharsets.UTF_8);

        // THEN
        assertThat(contents, containsString("some-auth-token"));
        assertFalse(ngrokClientV2.getNgrokProcess().isRunning());
    }

    @Test
    public void testSetAuthTokenV3()
        throws IOException {
        // WHEN
        ngrokClientV3.setAuthToken("some-auth-token");
        final String contents = new String(Files.readAllBytes(javaNgrokConfigV3.getConfigPath()),
            StandardCharsets.UTF_8);

        // THEN
        assertThat(contents, containsString("some-auth-token"));
        assertFalse(ngrokClientV3.getNgrokProcess().isRunning());
    }

    @Test
    public void testSetApiKeyV2Fails() {
        // WHEN
        assertThrows(JavaNgrokException.class, () -> ngrokClientV2.setApiKey("some-api-key"));
    }

    @Test
    public void testSetApiKeyV3()
        throws IOException {
        // WHEN
        ngrokClientV3.setApiKey("some-api-key");
        final String contents = new String(Files.readAllBytes(javaNgrokConfigV3.getConfigPath()),
            StandardCharsets.UTF_8);

        // THEN
        assertThat(contents, containsString("some-api-key"));
        assertFalse(ngrokClientV3.getNgrokProcess().isRunning());
    }

    @Test
    public void testUpdate() {
        // WHEN
        ngrokClientV3.update();

        // THEN
        assertFalse(ngrokClientV3.getNgrokProcess().isRunning());
    }

    @Test
    public void testNgrokConnectHttpClientCreateTunnelFails() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final HttpClient httpClient = mock(HttpClient.class);
        final HttpClientException httpClientException = new HttpClientException(
            "some message",
            new SocketTimeoutException(),
            "http://localhost:4040/api/tunnels",
            500,
            "error body");
        final NgrokClient ngrokClient = new NgrokClient.Builder()
            .withJavaNgrokConfig(javaNgrokConfigV3)
            .withNgrokProcess(ngrokProcessV3)
            .withHttpClient(httpClient)
            .build();
        when(httpClient.post(any(), any(), any())).thenThrow(httpClientException);

        // WHEN
        final JavaNgrokHTTPException javaNgrokHTTPException = assertThrows(JavaNgrokHTTPException.class,
            ngrokClient::connect);

        // THEN
        assertThat(javaNgrokHTTPException.getMessage(), startsWith("An error occurred when POSTing to create the "
                                                                   + "tunnel "));
        assertEquals("http://localhost:4040/api/tunnels", javaNgrokHTTPException.getUrl());
        assertEquals(500, javaNgrokHTTPException.getStatusCode());
        assertEquals("error body", javaNgrokHTTPException.getBody());
    }

    @Test
    public void testNgrokConnectHttpClientGetTunnelsFails() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final HttpClient httpClient = mock(HttpClient.class);
        final Response response = mock(Response.class);
        final Tunnel tunnel = mock(Tunnel.class);
        final HttpClientException httpClientException = new HttpClientException(
            "some message",
            new SocketTimeoutException(),
            "http://localhost:4040/api/tunnels",
            500,
            "error body");
        final NgrokClient ngrokClient = new NgrokClient.Builder()
            .withJavaNgrokConfig(javaNgrokConfigV2)
            .withNgrokProcess(ngrokProcessV2)
            .withHttpClient(httpClient)
            .build();
        when(tunnel.getName()).thenReturn("my-tunnel");
        when(tunnel.getUri()).thenReturn("/api/tunnels/my-tunnel");
        when(response.getBody()).thenReturn(tunnel);
        when(httpClient.post(any(), any(), any())).thenReturn(response);
        when(httpClient.get(any(), any())).thenThrow(httpClientException);

        // WHEN
        final JavaNgrokHTTPException javaNgrokHTTPException = assertThrows(JavaNgrokHTTPException.class,
            ngrokClient::connect);

        // THEN
        assertEquals("An error occurred when GETing the HTTP tunnel my-tunnel.",
            javaNgrokHTTPException.getMessage());
        assertEquals("http://localhost:4040/api/tunnels", javaNgrokHTTPException.getUrl());
        assertEquals(500, javaNgrokHTTPException.getStatusCode());
        assertEquals("error body", javaNgrokHTTPException.getBody());
    }

    @Test
    public void testNgrokConnectHttpClientDeleteTunnelsFails() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final HttpClient httpClient = spy(new DefaultHttpClient.Builder()
            .withRetryCount(3)
            .build());
        final NgrokClient ngrokClient = new NgrokClient.Builder()
            .withJavaNgrokConfig(javaNgrokConfigV2)
            .withNgrokProcess(ngrokProcessV2)
            .withHttpClient(httpClient)
            .build();
        doAnswer(invocation -> {
            throw new HttpClientException(
                "some message",
                new SocketTimeoutException(),
                "http://localhost:4040/api/tunnels",
                500,
                "error body");
        }).when(httpClient).delete(any());
        final Tunnel tunnel = ngrokClient.connect();

        // WHEN
        final JavaNgrokHTTPException javaNgrokHTTPException = assertThrows(JavaNgrokHTTPException.class,
            () -> ngrokClient.disconnect(tunnel.getPublicUrl()));

        // THEN
        assertThat(javaNgrokHTTPException.getMessage(), startsWith("An error occurred when DELETing the tunnel"));
    }
}
