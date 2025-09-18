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
import com.github.alexdlaird.ngrok.protocol.ApiResponse;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;

import static com.github.alexdlaird.ngrok.installer.NgrokInstaller.getNgrokBin;
import static com.github.alexdlaird.util.StringUtils.isBlank;
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

    private String reservedDomain;

    private String reservedDomainId;

    private String tcpEdgeReservedAddr;

    private String tcpEdgeReservedAddrId;

    private String tcpEdgeId;

    private String httpEdgeReservedDomain;

    private String httpEdgeReservedDomainId;

    private String httpEdgeId;

    private String tlsEdgeReservedDomain;

    private String tlsEdgeReservedDomainId;

    private String tlsEdgeId;

    private final JavaNgrokConfig testcaseJavaNgrokConfig = new JavaNgrokConfig.Builder()
        .withConfigPath(Path.of("build", ".testcase-ngrok", "config.yml").toAbsolutePath())
        .withNgrokPath(Path.of("build", "bin", "testcase-ngrok", getNgrokBin()))
        .build();

    private final NgrokClient testcaseClient = new NgrokClient.Builder().withJavaNgrokConfig(testcaseJavaNgrokConfig)
                                                                        .build();

    private Map<String, String> edge;

    @BeforeAll
    public void setUpClass()
        throws IOException, InterruptedException {
        if (isNotBlank(System.getenv("NGROK_API_KEY"))) {
            if (!Files.exists(testcaseJavaNgrokConfig.getNgrokPath())) {
                ngrokInstaller.installNgrok(testcaseJavaNgrokConfig.getNgrokPath(),
                    testcaseJavaNgrokConfig.getNgrokVersion());
            }
            if (!Files.exists(testcaseJavaNgrokConfig.getConfigPath())) {
                ngrokInstaller.installDefaultConfig(testcaseJavaNgrokConfig.getConfigPath(), Map.of(),
                    testcaseJavaNgrokConfig.getNgrokVersion());
            }

            // NGROK_HOSTNAME is set when init_test_resources.py is done provisioning test resources, so if it
            // hasn't been set, we need to do that now. When running tests on CI, using the init script can protect
            // against rate limiting, as this allows API resources to be shared across the build matrix.
            if (isBlank(System.getenv("NGROK_HOSTNAME"))) {
                final String domain = String.format("%s.ngrok.dev", this.ngrokSubdomain);
                try {
                    this.testcaseClient.api(List.of("reserved-domains", "create",
                        "--domain", domain,
                        "--description", testResourceDescription));
                } catch (final NgrokException ex) {
                    if (!ex.getMessage().contains("domain is already reserved")) {
                        throw ex;
                    }
                }

                final String subdomain = this.generateNameForSubdomain();
                final String hostname = String.format("%s.%s.ngrok.dev", subdomain, this.ngrokSubdomain);
                final ApiResponse reservedDomainResponse = this.testcaseClient.api(
                    List.of("reserved-domains", "create",
                        "--domain", hostname,
                        "--description", testResourceDescription));
                this.reservedDomain = String.valueOf(reservedDomainResponse.getData().get("domain"));
                this.reservedDomainId = String.valueOf(reservedDomainResponse.getData().get("id"));

                final ApiResponse tcpEdgeReservedAddrResponse = testcaseClient.api(
                    List.of("reserved-addrs", "create",
                        "--description", testResourceDescription));
                this.tcpEdgeReservedAddr = String.valueOf(tcpEdgeReservedAddrResponse.getData().get("addr"));
                this.tcpEdgeReservedAddrId = String.valueOf(tcpEdgeReservedAddrResponse.getData().get("id"));
                Thread.sleep(500);
                final String[] hostAndPort = this.tcpEdgeReservedAddr.split(":");
                this.tcpEdgeId = String.valueOf(
                    testcaseClient.api(List.of("edges", "tcp", "create", "--hostports",
                                      String.format("%s:%s", hostAndPort[0],
                                          hostAndPort[1]), "--description",
                                      testResourceDescription))
                                  .getData()
                                  .get("id"));

                final String subdomainHttp = this.generateNameForSubdomain();
                final String httpEdgeHostname = String.format("%s.%s.ngrok.dev", subdomainHttp, this.ngrokSubdomain);
                final ApiResponse httpEdgeReservedDomainResponse = testcaseClient.api(
                    List.of("reserved-domains", "create",
                        "--domain", httpEdgeHostname,
                        "--description", testResourceDescription));
                this.httpEdgeReservedDomain = String.valueOf(httpEdgeReservedDomainResponse.getData().get("domain"));
                this.httpEdgeReservedDomainId = String.valueOf(httpEdgeReservedDomainResponse.getData().get("id"));
                Thread.sleep(500);
                this.httpEdgeId = String.valueOf(
                    testcaseClient.api(
                        List.of("edges", "https", "create",
                            "--hostports", String.format("%s:%s", httpEdgeHostname, 443),
                            "--description", testResourceDescription)).getData().get("id"));

                final String subdomainTls = this.generateNameForSubdomain();
                final String tlsEdgeHostname = String.format("%s.%s.ngrok.dev", subdomainTls, this.ngrokSubdomain);
                final ApiResponse tlsEdgeReservedDomainResponse = testcaseClient.api(
                    List.of("reserved-domains",
                        "create", "--domain", tlsEdgeHostname,
                        "--description", testResourceDescription));
                this.tlsEdgeReservedDomain = String.valueOf(tlsEdgeReservedDomainResponse.getData().get("domain"));
                this.tlsEdgeReservedDomainId = String.valueOf(tlsEdgeReservedDomainResponse.getData().get("id"));
                Thread.sleep(500);
                this.tlsEdgeId = String.valueOf(
                    testcaseClient.api(
                        List.of("edges", "tls", "create",
                            "--hostports", String.format("%s:%s", tlsEdgeHostname, 443),
                            "--description", testResourceDescription)).getData().get("id"));
            } else {
                this.reservedDomain = System.getenv("NGROK_DOMAIN");
                this.tcpEdgeReservedAddr = System.getenv("NGROK_TCP_EDGE_ADDR");
                this.tcpEdgeId = System.getenv("NGROK_TCP_EDGE_ID");
                this.httpEdgeReservedDomain = System.getenv("NGROK_HTTP_EDGE_DOMAIN");
                this.httpEdgeId = System.getenv("NGROK_HTTP_EDGE_ID");
                this.tlsEdgeReservedDomain = System.getenv("NGROK_TLS_EDGE_DOMAIN");
                this.tlsEdgeId = System.getenv("NGROK_TLS_EDGE_ID");
            }
        }
    }

    @AfterAll
    public void tearDownClass() {
        // NGROK_HOSTNAME is set when init_test_resources.py is done provisioning test resources, in which case
        // prune_test_resources.py should also be called to clean up test resources after all tests complete.
        // Otherwise, this testcase set up the resources, so it should also tear them down.
        if (isNotBlank(System.getenv("NGROK_API_KEY")) && isBlank(System.getenv("NGROK_HOSTNAME"))) {
            try {
                testcaseClient.api(List.of("edges", "https", "delete", this.httpEdgeId));
                Thread.sleep(200);
                testcaseClient.api(List.of("edges", "tcp", "delete", this.tcpEdgeId));
                Thread.sleep(200);
                testcaseClient.api(List.of("edges", "tls", "delete", this.tlsEdgeId));
                Thread.sleep(200);
                testcaseClient.api(List.of("reserved-domains", "delete", this.reservedDomainId));
                Thread.sleep(200);
                testcaseClient.api(List.of("reserved-domains", "delete", this.tlsEdgeReservedDomainId));
                Thread.sleep(200);
                testcaseClient.api(List.of("reserved-domains", "delete", this.httpEdgeReservedDomainId));
                Thread.sleep(200);
                testcaseClient.api(List.of("reserved-addrs", "delete", this.tcpEdgeReservedAddrId));
                Thread.sleep(200);
            } catch (Exception ex) {
                ex.printStackTrace(System.out);
                System.out.println("--> An error occurred while cleaning up test resources. Run "
                                   + "scripts/prune_test_resources.py to finish.");
            }
        }
    }

    @BeforeEach
    public void setUp() {
        super.setUp();

        ngrokClientV2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfigV2)
                                                 .withNgrokProcess(ngrokProcessV2)
                                                 .withHttpClient(retryHttpClient)
                                                 .build();
        ngrokClientV3 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfigV3)
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
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V2)
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
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
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
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
                                                                    .withAddr(80)
                                                                    .withProto(Proto.TLS)
                                                                    .withDomain(this.reservedDomain)
                                                                    .withTerminateAt("upstream")
                                                                    .withPoolingEnabled(true)
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
        assertEquals(tunnel.getPublicUrl(), String.format("tls://%s", this.reservedDomain));
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
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
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
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V2)
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
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V2)
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
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V2)
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
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
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
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
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
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V2)
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
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
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
        final String subdomain = generateNameForSubdomain();
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV2).withRegion(Region.AU)
                                                                                               .build();
        ngrokProcessV2_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcessV2_2)
                                                                  .build();
        assertFalse(ngrokClient2.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V2)
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
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3).withRegion(Region.AU)
                                                                                               .build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcessV3_2)
                                                                  .build();
        assertFalse(ngrokClient2.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
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
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfigV2)
                                                                  .withNgrokProcess(ngrokProcessV2_2)
                                                                  .build();
        assertFalse(ngrokClient2.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V2)
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
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfigV3)
                                                                  .withNgrokProcess(ngrokProcessV3_2)
                                                                  .build();
        assertFalse(ngrokClient2.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
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
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3).withRegion(Region.AU)
                                                                                               .build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcessV3_2)
                                                                  .build();
        assertFalse(ngrokClient2.getNgrokProcess().isRunning());
        final String subdomain = generateNameForSubdomain();
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withSubdomain(subdomain).build();

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
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withAddr("file:///").build();

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
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withAddr("file:///").build();
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
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withAddr("file:///").build();
        final Tunnel tunnel = ngrokClientV3.connect(createTunnel);
        Thread.sleep(1000);
        final String apiUrl = ngrokClientV3.getNgrokProcess().getApiUrl();

        // WHEN
        final Response<Tunnel> response = ngrokClientV3.getHttpClient()
                                                       .get(String.format("%s%s", apiUrl, tunnel.getUri()),
                                                           Tunnel.class);

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
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withName("my-tunnel")
                                                                    .withAddr(new URL(ngrokClientV3.getNgrokProcess()
                                                                                                   .getApiUrl()).getPort())
                                                                    .build();
        final Tunnel tunnel = ngrokClientV3.connect(createTunnel);
        Thread.sleep(1000);
        assertEquals(0, tunnel.getMetrics().get("http").getCount());

        ngrokClientV3.getHttpClient().get(String.format("%s/api/status", tunnel.getPublicUrl()), Object.class);

        Thread.sleep(3000);

        ngrokClientV3.refreshMetrics(tunnel);

        assertThat(tunnel.getMetrics().get("http").getCount(), greaterThan(0));
    }

    @Test
    public void testTunnelDefinitionsV2() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final String subdomain = generateNameForSubdomain();
        final Map<String, Object> httpTunnelConfig = Map.of("proto", "http", "addr", "8000", "subdomain", subdomain,
            "inspect", Boolean.FALSE, "bind_tls", Boolean.TRUE);
        final Map<String, Object> tcpTunnelConfig = Map.of("proto", "tcp", "addr", "22");
        final Map<String, Object> tunnelsConfig = Map.of("http-tunnel", httpTunnelConfig, "tcp-tunnel",
            tcpTunnelConfig);
        final Map<String, Object> config = Map.of("tunnels", tunnelsConfig);

        final Path configPath2 = Path.of(javaNgrokConfigV2.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV2.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV2).withConfigPath(
            configPath2).build();
        ngrokProcessV2_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcessV2_2)
                                                                  .build();

        // WHEN
        final CreateTunnel createHttpTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V2)
                                                                        .withName("http-tunnel")
                                                                        .build();
        final Tunnel httpTunnel = ngrokClient2.connect(createHttpTunnel);
        final CreateTunnel createSshTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V2)
                                                                       .withName("tcp-tunnel")
                                                                       .build();
        final Tunnel sshTunnel = ngrokClient2.connect(createSshTunnel);
        final List<Tunnel> tunnels = ngrokClient2.getTunnels();

        // THEN
        assertEquals(2, tunnels.size());
        assertEquals("http-tunnel-agent", httpTunnel.getName());
        assertEquals("http://localhost:8000", httpTunnel.getConfig().getAddr());
        assertEquals("https", httpTunnel.getProto());
        assertFalse(httpTunnel.getConfig().isInspect());
        assertEquals(String.format("https://%s.ngrok.io", subdomain), httpTunnel.getPublicUrl());
        assertEquals("tcp-tunnel-agent", sshTunnel.getName());
        assertEquals("localhost:22", sshTunnel.getConfig().getAddr());
        assertEquals("tcp", sshTunnel.getProto());
        assertThat(sshTunnel.getPublicUrl(), startsWith("tcp://"));
    }

    @Test
    public void testTunnelDefinitionsV3() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final String subdomain = generateNameForSubdomain();
        final Map<String, Object> httpTunnelConfig = Map.of("proto", "http", "addr", "8000", "subdomain", subdomain,
            "inspect", Boolean.FALSE, "schemes", List.of("http"), "circuit_breaker", 0.5f);
        final Map<String, Object> tcpTunnelConfig = Map.of("proto", "tcp", "addr", "22");
        final Map<String, Object> tunnelsConfig = Map.of("http-tunnel", httpTunnelConfig, "tcp-tunnel",
            tcpTunnelConfig);
        final Map<String, Object> config = Map.of("tunnels", tunnelsConfig);

        final Path configPath2 = Path.of(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV3.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3).withConfigPath(
            configPath2).build();
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokInstaller(ngrokInstaller)
                                                                  .build();
        ngrokProcessV3_2 = ngrokClient2.getNgrokProcess();

        // WHEN
        final CreateTunnel createHttpTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
                                                                        .withName("http-tunnel")
                                                                        .build();
        final Tunnel httpTunnel = ngrokClient2.connect(createHttpTunnel);
        final CreateTunnel createSshTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
                                                                       .withName("tcp-tunnel")
                                                                       .build();
        final Tunnel sshTunnel = ngrokClient2.connect(createSshTunnel);
        final List<Tunnel> tunnels = ngrokClient2.getTunnels();

        // THEN
        assertEquals(2, tunnels.size());
        assertEquals("http-tunnel-agent", httpTunnel.getName());
        assertEquals("http://localhost:8000", httpTunnel.getConfig().getAddr());
        assertEquals("http", httpTunnel.getProto());
        assertFalse(httpTunnel.getConfig().isInspect());
        assertEquals(String.format("http://%s.ngrok.io", subdomain), httpTunnel.getPublicUrl());
        assertEquals("tcp-tunnel-agent", sshTunnel.getName());
        assertEquals("localhost:22", sshTunnel.getConfig().getAddr());
        assertEquals("tcp", sshTunnel.getProto());
        assertThat(sshTunnel.getPublicUrl(), startsWith("tcp://"));
    }

    @Test
    public void testTunnelDefinitionsTls() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");
        testRequiresEnvVar("NGROK_API_KEY");

        // GIVEN
        final Map<String, Object> tlsTunnelConfig = Map.of("proto", "tls", "addr", "443", "domain", this.reservedDomain,
            "terminate_at", "upstream", "pooling_enabled", true);
        final Map<String, Object> tunnelsConfig = Map.of("tls-tunnel", tlsTunnelConfig);
        final Map<String, Object> config = Map.of("tunnels", tunnelsConfig);

        final Path configPath2 = Path.of(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV3.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3).withConfigPath(
            configPath2).build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcessV3_2)
                                                                  .build();

        // WHEN
        final CreateTunnel createTlsTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
                                                                       .withName("tls-tunnel")
                                                                       .build();
        final Tunnel tlsTunnel = ngrokClient2.connect(createTlsTunnel);
        final List<Tunnel> tunnels = ngrokClient2.getTunnels();

        // THEN
        assertEquals(1, tunnels.size());
        assertEquals("tls-tunnel-agent", tlsTunnel.getName());
        assertEquals("tls://localhost:443", tlsTunnel.getConfig().getAddr());
        assertEquals("tls", tlsTunnel.getProto());
        assertFalse(tlsTunnel.getConfig().isInspect());
        assertEquals(tlsTunnel.getPublicUrl(), String.format("tls://%s", this.reservedDomain));
    }

    @Test
    public void testTunnelDefinitionsHttpEdge() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");
        testRequiresEnvVar("NGROK_API_KEY");

        // GIVEN
        final Map<String, Object> edgeHttpTunnelConfig = Map.of("addr", "80", "labels",
            List.of(String.format("edge=%s", this.httpEdgeId)));
        final Map<String, Object> tunnelsConfig = Map.of("edge-http-tunnel", edgeHttpTunnelConfig);
        final Map<String, Object> config = Map.of("tunnels", tunnelsConfig);

        final Path configPath2 = Path.of(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV3.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3).withConfigPath(
            configPath2).build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcessV3_2)
                                                                  .build();

        // WHEN
        final CreateTunnel createHttpEdgeTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
                                                                            .withName("edge-http-tunnel")
                                                                            .build();
        final Tunnel httpEdgeTunnel = ngrokClient2.connect(createHttpEdgeTunnel);
        final List<Tunnel> tunnels = ngrokClient2.getTunnels();

        // THEN
        assertEquals("edge-http-tunnel-agent", httpEdgeTunnel.getName());
        assertEquals("http://localhost:80", httpEdgeTunnel.getConfig().getAddr());
        assertEquals("https", httpEdgeTunnel.getProto());
        assertEquals(String.format("https://%s:443", this.httpEdgeReservedDomain), httpEdgeTunnel.getPublicUrl());
        assertEquals(1, tunnels.size());
        assertEquals("edge-http-tunnel-agent", tunnels.get(0).getName());
        assertEquals("http://localhost:80", tunnels.get(0).getConfig().getAddr());
        assertEquals("https", tunnels.get(0).getProto());
        assertEquals(String.format("https://%s:443", this.httpEdgeReservedDomain), tunnels.get(0).getPublicUrl());
    }

    @Test
    public void testTunnelDefinitionsTcpEdge() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");
        testRequiresEnvVar("NGROK_API_KEY");

        // GIVEN
        final String[] hostAndPort = this.tcpEdgeReservedAddr.split(":");
        final Map<String, Object> edgeTcpTunnelConfig = Map.of("addr", "22", "labels",
            List.of(String.format("edge=%s", this.tcpEdgeId)));
        final Map<String, Object> tunnelsConfig = Map.of("edge-tcp-tunnel", edgeTcpTunnelConfig);
        final Map<String, Object> config = Map.of("tunnels", tunnelsConfig);

        final Path configPath2 = Path.of(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV3.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3).withConfigPath(
            configPath2).build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcessV3_2)
                                                                  .build();

        // WHEN
        final CreateTunnel createTcpEdgeTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
                                                                           .withName("edge-tcp-tunnel")
                                                                           .build();
        final Tunnel tcpEdgeTunnel = ngrokClient2.connect(createTcpEdgeTunnel);
        final List<Tunnel> tunnels = ngrokClient2.getTunnels();

        // THEN
        assertEquals("edge-tcp-tunnel-agent", tcpEdgeTunnel.getName());
        assertEquals("tcp://localhost:22", tcpEdgeTunnel.getConfig().getAddr());
        assertEquals("tcp", tcpEdgeTunnel.getProto());
        assertEquals(String.format("tcp://%s:%s", hostAndPort[0], hostAndPort[1]), tcpEdgeTunnel.getPublicUrl());
        assertEquals(1, tunnels.size());
        assertEquals("edge-tcp-tunnel-agent", tunnels.get(0).getName());
        assertEquals("tcp://localhost:22", tunnels.get(0).getConfig().getAddr());
        assertEquals("tcp", tunnels.get(0).getProto());
        assertEquals(String.format("tcp://%s:%s", hostAndPort[0], hostAndPort[1]), tunnels.get(0).getPublicUrl());
    }

    @Test
    public void testTunnelDefinitionsTlsEdge() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");
        testRequiresEnvVar("NGROK_API_KEY");

        // GIVEN
        final Map<String, Object> edgeTlsTunnelConfig = Map.of("addr", "443", "labels",
            List.of(String.format("edge=%s", this.tlsEdgeId)));
        final Map<String, Object> tunnelsConfig = Map.of("edge-tls-tunnel", edgeTlsTunnelConfig);
        final Map<String, Object> config = Map.of("tunnels", tunnelsConfig);

        final Path configPath2 = Path.of(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV3.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3).withConfigPath(
            configPath2).build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcessV3_2)
                                                                  .build();

        // WHEN
        final CreateTunnel createTlsEdgeTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
                                                                           .withName("edge-tls-tunnel")
                                                                           .build();
        final Tunnel tlsEdgeTunnel = ngrokClient2.connect(createTlsEdgeTunnel);
        final List<Tunnel> tunnels = ngrokClient2.getTunnels();

        // THEN
        assertEquals("edge-tls-tunnel-agent", tlsEdgeTunnel.getName());
        assertEquals("https://localhost:443", tlsEdgeTunnel.getConfig().getAddr());
        assertEquals("tls", tlsEdgeTunnel.getProto());
        assertEquals(String.format("tls://%s:443", this.tlsEdgeReservedDomain), tlsEdgeTunnel.getPublicUrl());
        assertEquals(1, tunnels.size());
        assertEquals("edge-tls-tunnel-agent", tunnels.get(0).getName());
        assertEquals("https://localhost:443", tunnels.get(0).getConfig().getAddr());
        assertEquals("tls", tunnels.get(0).getProto());
        assertEquals(String.format("tls://%s:443", this.tlsEdgeReservedDomain), tunnels.get(0).getPublicUrl());
    }

    @Test
    public void testBindTlsAndLabelsNotAllowed() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");
        testRequiresEnvVar("NGROK_API_KEY");

        // GIVEN
        final Map<String, Object> edgeTlsTunnelConfig = Map.of("addr", "443", "labels",
            List.of(String.format("edge=%s", this.tlsEdgeId)));
        final Map<String, Object> tunnelsConfig = Map.of("edge-tls-tunnel", edgeTlsTunnelConfig);
        final Map<String, Object> config = Map.of("tunnels", tunnelsConfig);

        final Path configPath2 = Path.of(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV3.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3).withConfigPath(
            configPath2).build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcessV3_2)
                                                                  .build();

        // WHEN
        final CreateTunnel createEdgeTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
                                                                        .withName("edge-tls-tunnel")
                                                                        .withBindTls(true)
                                                                        .build();
        assertThrows(IllegalArgumentException.class, () -> ngrokClient2.connect(createEdgeTunnel));
    }

    @Test
    @ClearEnvironmentVariable(key = "NGROK_API_KEY")
    public void testLabelsNoApiKeyFails() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final Map<String, Object> edgeHttpTunnelConfig = Map.of("addr", "80", "labels", List.of("edge=edghts_some-id"));
        final Map<String, Object> tunnelsConfig = Map.of("edge-tunnel", edgeHttpTunnelConfig);
        final Map<String, Object> config = Map.of("tunnels", tunnelsConfig);

        final Path configPath2 = Path.of(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV3.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3).withConfigPath(
            configPath2).withApiKey(null).build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcessV3_2)
                                                                  .build();
        assertNull(javaNgrokConfig2.getApiKey());

        // WHEN
        final CreateTunnel createEdgeTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
                                                                        .withName("edge-tunnel")
                                                                        .build();
        assertThrows(JavaNgrokException.class, () -> ngrokClient2.connect(createEdgeTunnel));
    }

    @Test
    public void testNgrokHttpInternalEndpointPooling() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // WHEN
        final CreateTunnel createHttpEdgeTunnel = new CreateTunnel.Builder().withProto(Proto.HTTP)
                                                                            .withAddr(80)
                                                                            .withDomain("java-ngrok.internal")
                                                                            .withPoolingEnabled(true)
                                                                            .build();
        final Tunnel httpInternalEndpoint = ngrokClientV3.connect(createHttpEdgeTunnel);
        final List<Tunnel> tunnels = ngrokClientV3.getTunnels();

        // THEN
        assertEquals("http://localhost:80", httpInternalEndpoint.getConfig().getAddr());
        assertEquals("https", httpInternalEndpoint.getProto());
        assertEquals("https://java-ngrok.internal", httpInternalEndpoint.getPublicUrl());
        assertEquals(1, tunnels.size());
        assertEquals("http://localhost:80", tunnels.get(0).getConfig().getAddr());
        assertEquals("https", tunnels.get(0).getProto());
        assertEquals("https://java-ngrok.internal", tunnels.get(0).getPublicUrl());
    }

    @Test
    public void testNgrokTlsInternalEndpointPooling() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // WHEN
        final CreateTunnel createHttpEdgeTunnel = new CreateTunnel.Builder().withProto(Proto.TLS)
                                                                            .withAddr(443)
                                                                            .withDomain("java-ngrok.internal")
                                                                            .withPoolingEnabled(true)
                                                                            .build();
        final Tunnel httpInternalEndpoint = ngrokClientV3.connect(createHttpEdgeTunnel);
        final List<Tunnel> tunnels = ngrokClientV3.getTunnels();

        // THEN
        assertEquals("tls://localhost:443", httpInternalEndpoint.getConfig().getAddr());
        assertEquals("tls", httpInternalEndpoint.getProto());
        assertEquals("tls://java-ngrok.internal", httpInternalEndpoint.getPublicUrl());
        assertEquals(1, tunnels.size());
        assertEquals("tls://localhost:443", tunnels.get(0).getConfig().getAddr());
        assertEquals("tls", tunnels.get(0).getProto());
        assertEquals("tls://java-ngrok.internal", tunnels.get(0).getPublicUrl());
    }

    @Test
    public void testTunnelDefinitionsV3OAuth() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final String subdomain = generateNameForSubdomain();
        final Map<String, Object> httpTunnelConfig = Map.of("proto", "http", "addr", "8000", "subdomain", subdomain,
            "oauth", Map.of("provider", "google", "allow_domains", List.of("java-ngrok.com"), "allow_emails",
                List.of("email@java-ngrok.com")));
        final Map<String, Object> tunnelsConfig = Map.of("http-tunnel", httpTunnelConfig);
        final Map<String, Object> config = Map.of("tunnels", tunnelsConfig);

        final Path configPath2 = Path.of(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV3.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3).withConfigPath(
            configPath2).build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcessV3_2)
                                                                  .build();

        // WHEN
        final CreateTunnel createHttpTunnel = new CreateTunnel.Builder().withName("http-tunnel").build();
        final Tunnel httpTunnel = ngrokClient2.connect(createHttpTunnel);
        final List<Tunnel> tunnels = ngrokClient2.getTunnels();

        final String responseBody = ngrokClientV3.getHttpClient()
                                                 .get(String.format(httpTunnel.getPublicUrl()), Object.class)
                                                 .getBodyRaw();

        // THEN
        assertEquals(1, tunnels.size());
        assertEquals("http-tunnel-agent", httpTunnel.getName());
        assertEquals("http://localhost:8000", httpTunnel.getConfig().getAddr());
        assertEquals("https", httpTunnel.getProto());
        assertEquals(String.format("https://%s.ngrok.io", subdomain), httpTunnel.getPublicUrl());
        assertTrue(responseBody.contains("Sign in - Google Accounts"));
    }

    @Test
    public void testTunnelDefinitionsJavaNgrokDefault() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final String subdomain1 = generateNameForSubdomain();
        final Map<String, Object> defaultTunnelConfig = Map.of("proto", "http", "addr", "8080", "subdomain",
            subdomain1);
        final Map<String, Object> tunnelsConfig = Map.of("java-ngrok-default", defaultTunnelConfig);
        final Map<String, Object> config = Map.of("tunnels", tunnelsConfig);

        final Path configPath2 = Path.of(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV3.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3).withConfigPath(
            configPath2).build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcessV3_2)
                                                                  .build();

        // WHEN
        final Tunnel ngrokTunnel = ngrokClient2.connect();

        // THEN
        assertEquals("java-ngrok-default-agent", ngrokTunnel.getName());
        assertEquals("http://localhost:8080", ngrokTunnel.getConfig().getAddr());
        assertEquals("https", ngrokTunnel.getProto());
        assertEquals(String.format("https://%s.ngrok.io", subdomain1), ngrokTunnel.getPublicUrl());
    }

    @Test
    public void testTunnelDefinitionsJavaNgrokDefaultWithOverrides() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final String subdomain1 = generateNameForSubdomain();
        final Map<String, Object> defaultTunnelConfig = Map.of("proto", "http", "addr", "8080", "subdomain",
            subdomain1);
        final Map<String, Object> tunnelsConfig = Map.of("java-ngrok-default", defaultTunnelConfig);
        final Map<String, Object> config = Map.of("tunnels", tunnelsConfig);

        final Path configPath2 = Path.of(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfigV3.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3).withConfigPath(
            configPath2).build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcessV3_2)
                                                                  .build();

        // WHEN
        final String subdomain = generateNameForSubdomain();
        final CreateTunnel createTunnelSubdomain = new CreateTunnel.Builder().withSubdomain(subdomain)
                                                                             .withAddr(5000)
                                                                             .build();
        final Tunnel ngrokTunnel = ngrokClient2.connect(createTunnelSubdomain);

        // THEN
        assertEquals("java-ngrok-default-agent", ngrokTunnel.getName());
        assertEquals("http://localhost:5000", ngrokTunnel.getConfig().getAddr());
        assertEquals("https", ngrokTunnel.getProto());
        assertEquals(String.format("https://%s.ngrok.io", subdomain), ngrokTunnel.getPublicUrl());
    }

    @Test
    public void testSetAuthTokenV2()
        throws IOException {
        // WHEN
        ngrokClientV2.setAuthToken("some-auth-token");
        final String contents = Files.readString(javaNgrokConfigV2.getConfigPath());

        // THEN
        assertThat(contents, containsString("some-auth-token"));
        assertFalse(ngrokClientV2.getNgrokProcess().isRunning());
    }

    @Test
    public void testSetAuthTokenV3()
        throws IOException {
        // WHEN
        ngrokClientV3.setAuthToken("some-auth-token");
        final String contents = Files.readString(javaNgrokConfigV3.getConfigPath());

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
        final String contents = Files.readString(javaNgrokConfigV3.getConfigPath());

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
        final HttpClientException httpClientException = new HttpClientException("some message",
            new SocketTimeoutException(), "http://localhost:4040/api/tunnels", 500, "error body");
        final NgrokClient ngrokClient = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfigV3)
                                                                 .withNgrokProcess(ngrokProcessV3)
                                                                 .withHttpClient(httpClient)
                                                                 .build();
        when(httpClient.post(any(), any(), any())).thenThrow(httpClientException);

        // WHEN
        final JavaNgrokHTTPException javaNgrokHTTPException = assertThrows(JavaNgrokHTTPException.class,
            ngrokClient::connect);

        // THEN
        assertThat(javaNgrokHTTPException.getMessage(),
            startsWith("An error occurred when POSTing to create the " + "tunnel "));
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
        final HttpClientException httpClientException = new HttpClientException("some message",
            new SocketTimeoutException(), "http://localhost:4040/api/tunnels", 500, "error body");
        final NgrokClient ngrokClient = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfigV2)
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
        assertEquals("An error occurred when GETing the HTTP tunnel my-tunnel.", javaNgrokHTTPException.getMessage());
        assertEquals("http://localhost:4040/api/tunnels", javaNgrokHTTPException.getUrl());
        assertEquals(500, javaNgrokHTTPException.getStatusCode());
        assertEquals("error body", javaNgrokHTTPException.getBody());
    }

    @Test
    public void testNgrokConnectHttpClientDeleteTunnelsFails() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final HttpClient httpClient = spy(new DefaultHttpClient.Builder().withRetryCount(3).build());
        final NgrokClient ngrokClient = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfigV2)
                                                                 .withNgrokProcess(ngrokProcessV2)
                                                                 .withHttpClient(httpClient)
                                                                 .build();
        doAnswer(invocation -> {
            throw new HttpClientException("some message", new SocketTimeoutException(),
                "http://localhost:4040/api/tunnels", 500, "error body");
        }).when(httpClient).delete(any());
        final Tunnel tunnel = ngrokClient.connect();

        // WHEN
        final JavaNgrokHTTPException javaNgrokHTTPException = assertThrows(JavaNgrokHTTPException.class,
            () -> ngrokClient.disconnect(tunnel.getPublicUrl()));

        // THEN
        assertThat(javaNgrokHTTPException.getMessage(), startsWith("An error occurred when DELETing the tunnel"));
    }
}
