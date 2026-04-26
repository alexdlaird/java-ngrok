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
import com.github.alexdlaird.ngrok.installer.ConfigVersion;
import com.github.alexdlaird.ngrok.installer.NgrokVersion;
import com.github.alexdlaird.ngrok.process.NgrokProcess;
import com.github.alexdlaird.ngrok.protocol.ApiResponse;
import com.github.alexdlaird.ngrok.protocol.BindTls;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Proto;
import com.github.alexdlaird.ngrok.protocol.Region;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnels;
import com.github.alexdlaird.ngrok.protocol.Upstream;
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
import org.mockito.ArgumentCaptor;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NgrokClientTest extends NgrokTestCase {

    private NgrokClient ngrokClient;

    private final String ngrokSubdomain = System.getenv()
                                                .getOrDefault("NGROK_SUBDOMAIN", System.getProperty("user.name"));

    private String reservedDomain;

    private String reservedDomainId;

    private final JavaNgrokConfig testcaseJavaNgrokConfig = new JavaNgrokConfig.Builder()
        .withConfigPath(Path.of("build", ".testcase-ngrok", "config.yml").toAbsolutePath())
        .withNgrokPath(Path.of("build", "bin", "testcase-ngrok", getNgrokBin()))
        .build();

    private final NgrokClient testcaseClient = new NgrokClient.Builder().withJavaNgrokConfig(testcaseJavaNgrokConfig)
                                                                        .build();

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
            } else {
                this.reservedDomain = System.getenv("NGROK_DOMAIN");
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
                testcaseClient.api(List.of("reserved-domains", "delete", this.reservedDomainId));
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

        ngrokClient = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig)
                                                 .withNgrokProcess(ngrokProcess)
                                                 .withHttpClient(retryHttpClient)
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
        testRequiresEnvVar("NGROK_AUTHTOKEN");
        assertFalse(ngrokClient.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
                                                                    .withAddr(5000)
                                                                    .build();

        // WHEN
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // THEN
        assertTrue(ngrokClient.getNgrokProcess().getVersion().startsWith("3"));
        assertTrue(ngrokClient.getNgrokProcess().isRunning());
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
        assertFalse(ngrokClient.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
                                                                    .withAddr(80)
                                                                    .withProto(Proto.TLS)
                                                                    .withDomain(this.reservedDomain)
                                                                    .withTerminateAt("upstream")
                                                                    .withPoolingEnabled(true)
                                                                    .build();

        // WHEN
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // THEN
        assertTrue(ngrokClient.getNgrokProcess().getVersion().startsWith("3"));
        assertTrue(ngrokClient.getNgrokProcess().isRunning());
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
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // THEN
        assertThat(tunnel.getName(), startsWith("my-tunnel"));
        assertEquals("https", tunnel.getProto());
        assertEquals("http://localhost:80", tunnel.getConfig().getAddr());
    }

    @Test
    public void testGetTunnels() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final Tunnel tunnel = ngrokClient.connect();

        // WHEN
        final List<Tunnel> tunnels = ngrokClient.getTunnels();

        // THEN
        assertEquals(1, tunnels.size());
        assertEquals("https", tunnels.get(0).getProto());
        assertEquals(tunnel.getPublicUrl(), tunnels.get(0).getPublicUrl());
        assertEquals("http://localhost:80", tunnels.get(0).getConfig().getAddr());
    }

    @Test
    public void testConnectSchemesHttp() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
                                                                    .withSchemes(List.of("http"))
                                                                    .build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // WHEN
        final List<Tunnel> tunnels = ngrokClient.getTunnels();

        // THEN
        assertEquals(1, tunnels.size());
        assertThat(tunnel.getPublicUrl(), startsWith("http://"));
    }

    @Test
    public void testConnectSchemesHttps() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
                                                                    .withSchemes(List.of("https"))
                                                                    .build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // WHEN
        final List<Tunnel> tunnels = ngrokClient.getTunnels();

        // THEN
        assertEquals(1, tunnels.size());
        assertThat(tunnel.getPublicUrl(), startsWith("https://"));
    }

    @Test
    public void testConnectSchemesHttpHttps() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
                                                                    .withSchemes(List.of("http", "https"))
                                                                    .build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // WHEN
        final List<Tunnel> tunnels = ngrokClient.getTunnels();

        // THEN
        assertEquals(2, tunnels.size());
        assertThat(tunnel.getPublicUrl(), startsWith("https://"));
    }

    @Test
    public void testBindTlsUpgradedToSchemes() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
                                                                    .withBindTls(BindTls.BOTH)
                                                                    .build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // WHEN
        final List<Tunnel> tunnels = ngrokClient.getTunnels();

        // THEN
        assertEquals(2, tunnels.size());
        assertThat(tunnel.getPublicUrl(), startsWith("https://"));
    }

    @Test
    public void testDisconnect() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
                                                                    .withName("my-tunnel")
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
    public void testKill()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withName("my-tunnel").build();
        ngrokClient.connect(createTunnel);

        // WHEN
        ngrokClient.kill();
        Thread.sleep(1000);

        // THEN
        assertFalse(ngrokClient.getNgrokProcess().isRunning());
    }

    @Test
    public void testGetVersion() {
        // WHEN
        final Version version = ngrokClient.getVersion();

        // THEN
        assertNotNull(version.getJavaNgrokVersion());
        assertNotEquals("unknown", version.getNgrokVersion());
    }

    @Test
    public void testRegionalTcp() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig).withRegion(Region.AU)
                                                                                               .build();
        ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcess2)
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
    public void testAuth() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        ngrokProcess2 = new NgrokProcess(javaNgrokConfig, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig)
                                                                  .withNgrokProcess(ngrokProcess2)
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
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig).withRegion(Region.AU)
                                                                                               .build();
        ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcess2)
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
        assertFalse(ngrokClient.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withAddr("file:///").build();

        // WHEN
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        // THEN
        assertTrue(ngrokClient.getNgrokProcess().isRunning());
        assertThat(tunnel.getName(), startsWith("http-file-"));
        assertEquals("https", tunnel.getProto());
        assertEquals("file:///", tunnel.getConfig().getAddr());
        assertNotNull(tunnel.getPublicUrl());
        assertThat(tunnel.getPublicUrl(), startsWith("https://"));
    }

    @Test
    public void testGetTunnelFileserver()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        assertFalse(ngrokClient.getNgrokProcess().isRunning());
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withAddr("file:///").build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);
        Thread.sleep(1000);
        final String apiUrl = ngrokClient.getNgrokProcess().getApiUrl();

        // WHEN
        final Response<Tunnel> response = ngrokClient.getHttpClient()
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
        ngrokClient.getNgrokProcess().start();
        final CreateTunnel createTunnel = new CreateTunnel.Builder().withName("my-tunnel")
                                                                    .withAddr(new URL(ngrokClient.getNgrokProcess()
                                                                                                   .getApiUrl()).getPort())
                                                                    .build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);
        Thread.sleep(1000);
        assertEquals(0, tunnel.getMetrics().get("http").getCount());

        ngrokClient.getHttpClient().get(String.format("%s/api/status", tunnel.getPublicUrl()), Object.class);

        Thread.sleep(3000);

        ngrokClient.refreshMetrics(tunnel);

        assertThat(tunnel.getMetrics().get("http").getCount(), greaterThan(0));
    }

    @Test
    public void testTunnelDefinitions() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final String subdomain = generateNameForSubdomain();
        final Map<String, Object> httpTunnelConfig = Map.of("proto", "http", "addr", "8000", "subdomain", subdomain,
            "inspect", Boolean.FALSE, "schemes", List.of("http"), "circuit_breaker", 0.5f);
        final Map<String, Object> tcpTunnelConfig = Map.of("proto", "tcp", "addr", "22");
        final Map<String, Object> tunnelsConfig = Map.of("http-tunnel", httpTunnelConfig, "tcp-tunnel",
            tcpTunnelConfig);
        final Map<String, Object> config = Map.of("tunnels", tunnelsConfig);

        final Path configPath2 = Path.of(javaNgrokConfig.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfig.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig).withConfigPath(
            configPath2).build();
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokInstaller(ngrokInstaller)
                                                                  .build();
        ngrokProcess2 = ngrokClient2.getNgrokProcess();

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
        assertEquals("http-tunnel-api", httpTunnel.getName());
        assertEquals("http://localhost:8000", httpTunnel.getConfig().getAddr());
        assertEquals("http", httpTunnel.getProto());
        assertFalse(httpTunnel.getConfig().isInspect());
        assertEquals(String.format("http://%s.ngrok.io", subdomain), httpTunnel.getPublicUrl());
        assertEquals("tcp-tunnel-api", sshTunnel.getName());
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

        final Path configPath2 = Path.of(javaNgrokConfig.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfig.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig).withConfigPath(
            configPath2).build();
        ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcess2)
                                                                  .build();

        // WHEN
        final CreateTunnel createTlsTunnel = new CreateTunnel.Builder().withNgrokVersion(NgrokVersion.V3)
                                                                       .withName("tls-tunnel")
                                                                       .build();
        final Tunnel tlsTunnel = ngrokClient2.connect(createTlsTunnel);
        final List<Tunnel> tunnels = ngrokClient2.getTunnels();

        // THEN
        assertEquals(1, tunnels.size());
        assertEquals("tls-tunnel-api", tlsTunnel.getName());
        assertEquals("tls://localhost:443", tlsTunnel.getConfig().getAddr());
        assertEquals("tls", tlsTunnel.getProto());
        assertFalse(tlsTunnel.getConfig().isInspect());
        assertEquals(tlsTunnel.getPublicUrl(), String.format("tls://%s", this.reservedDomain));
    }

    @Test
    public void testNgrokHttpInternalEndpointPooling() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // WHEN
        final CreateTunnel createHttpEndpointTunnel = new CreateTunnel.Builder().withProto(Proto.HTTP)
                                                                                .withAddr(80)
                                                                                .withDomain("java-ngrok.internal")
                                                                                .withPoolingEnabled(true)
                                                                                .build();
        final Tunnel httpInternalEndpoint = ngrokClient.connect(createHttpEndpointTunnel);
        final List<Tunnel> tunnels = ngrokClient.getTunnels();

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
        final CreateTunnel createHttpEndpointTunnel = new CreateTunnel.Builder().withProto(Proto.TLS)
                                                                                .withAddr(443)
                                                                                .withDomain("java-ngrok.internal")
                                                                                .withPoolingEnabled(true)
                                                                                .build();
        final Tunnel httpInternalEndpoint = ngrokClient.connect(createHttpEndpointTunnel);
        final List<Tunnel> tunnels = ngrokClient.getTunnels();

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
    public void testTunnelDefinitionsOAuth() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final String subdomain = generateNameForSubdomain();
        final Map<String, Object> httpTunnelConfig = Map.of("proto", "http", "addr", "8000", "subdomain", subdomain,
            "oauth", Map.of("provider", "google", "allow_domains", List.of("java-ngrok.com"), "allow_emails",
                List.of("email@java-ngrok.com")));
        final Map<String, Object> tunnelsConfig = Map.of("http-tunnel", httpTunnelConfig);
        final Map<String, Object> config = Map.of("tunnels", tunnelsConfig);

        final Path configPath2 = Path.of(javaNgrokConfig.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfig.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig).withConfigPath(
            configPath2).build();
        ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcess2)
                                                                  .build();

        // WHEN
        final CreateTunnel createHttpTunnel = new CreateTunnel.Builder().withName("http-tunnel").build();
        final Tunnel httpTunnel = ngrokClient2.connect(createHttpTunnel);
        final List<Tunnel> tunnels = ngrokClient2.getTunnels();

        final String responseBody = ngrokClient.getHttpClient()
                                                 .get(String.format(httpTunnel.getPublicUrl()), Object.class)
                                                 .getBodyRaw();

        // THEN
        assertEquals(1, tunnels.size());
        assertEquals("http-tunnel-api", httpTunnel.getName());
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

        final Path configPath2 = Path.of(javaNgrokConfig.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfig.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig).withConfigPath(
            configPath2).build();
        ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcess2)
                                                                  .build();

        // WHEN
        final Tunnel ngrokTunnel = ngrokClient2.connect();

        // THEN
        assertEquals("java-ngrok-default-api", ngrokTunnel.getName());
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

        final Path configPath2 = Path.of(javaNgrokConfig.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfig.getNgrokVersion());
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig).withConfigPath(
            configPath2).build();
        ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcess2)
                                                                  .build();

        // WHEN
        final String subdomain = generateNameForSubdomain();
        final CreateTunnel createTunnelSubdomain = new CreateTunnel.Builder().withSubdomain(subdomain)
                                                                             .withAddr(5000)
                                                                             .build();
        final Tunnel ngrokTunnel = ngrokClient2.connect(createTunnelSubdomain);

        // THEN
        assertEquals("java-ngrok-default-api", ngrokTunnel.getName());
        assertEquals("http://localhost:5000", ngrokTunnel.getConfig().getAddr());
        assertEquals("https", ngrokTunnel.getProto());
        assertEquals(String.format("https://%s.ngrok.io", subdomain), ngrokTunnel.getPublicUrl());
    }

    @Test
    public void testSetAuthToken()
        throws IOException {
        // WHEN
        ngrokClient.setAuthToken("some-auth-token");
        final String contents = Files.readString(javaNgrokConfig.getConfigPath());

        // THEN
        assertThat(contents, containsString("some-auth-token"));
        assertFalse(ngrokClient.getNgrokProcess().isRunning());
    }

    @Test
    public void testSetApiKey()
        throws IOException {
        // WHEN
        ngrokClient.setApiKey("some-api-key");
        final String contents = Files.readString(javaNgrokConfig.getConfigPath());

        // THEN
        assertThat(contents, containsString("some-api-key"));
        assertFalse(ngrokClient.getNgrokProcess().isRunning());
    }

    @Test
    public void testUpdate() {
        // WHEN
        ngrokClient.update();

        // THEN
        assertFalse(ngrokClient.getNgrokProcess().isRunning());
    }

    @Test
    public void testNgrokConnectHttpClientCreateTunnelFails() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final HttpClient httpClient = mock(HttpClient.class);
        final HttpClientException httpClientException = new HttpClientException("some message",
            new SocketTimeoutException(), "http://localhost:4040/api/tunnels", 500, "error body");
        final NgrokClient mockNgrokClient = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig)
                                                                     .withNgrokProcess(ngrokProcess)
                                                                     .withHttpClient(httpClient)
                                                                     .build();
        when(httpClient.post(any(), any(), any())).thenThrow(httpClientException);

        // WHEN
        final JavaNgrokHTTPException javaNgrokHTTPException = assertThrows(JavaNgrokHTTPException.class,
            mockNgrokClient::connect);

        // THEN
        assertThat(javaNgrokHTTPException.getMessage(),
            startsWith("An error occurred when POSTing to create the " + "tunnel "));
        assertEquals("http://localhost:4040/api/tunnels", javaNgrokHTTPException.getUrl());
        assertEquals(500, javaNgrokHTTPException.getStatusCode());
        assertEquals("error body", javaNgrokHTTPException.getBody());
    }

    @Test
    public void testNgrokConnectHttpClientDeleteTunnelsFails() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final HttpClient httpClient = spy(new DefaultHttpClient.Builder().withRetryCount(3).build());
        final NgrokClient mockNgrokClient = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig)
                                                                     .withNgrokProcess(ngrokProcess)
                                                                     .withHttpClient(httpClient)
                                                                     .build();
        doAnswer(invocation -> {
            throw new HttpClientException("some message", new SocketTimeoutException(),
                "http://localhost:4040/api/tunnels", 500, "error body");
        }).when(httpClient).delete(any());
        final Tunnel tunnel = mockNgrokClient.connect();

        // WHEN
        final JavaNgrokHTTPException javaNgrokHTTPException = assertThrows(JavaNgrokHTTPException.class,
            () -> mockNgrokClient.disconnect(tunnel.getPublicUrl()));

        // THEN
        assertThat(javaNgrokHTTPException.getMessage(), startsWith("An error occurred when DELETing the tunnel"));
    }

    @Test
    public void testConfigV2WithUpstreamRaises() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
            .withUpstream("http://localhost:8000")
            .build();

        // WHEN / THEN
        assertThrows(JavaNgrokException.class, () -> ngrokClient.connect(createTunnel));
    }

    @Test
    public void testConfigV2WithBindingsRaises() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
            .withAddr(8000)
            .withProto(Proto.HTTP)
            .withBindings(List.of("public"))
            .build();

        // WHEN / THEN
        assertThrows(JavaNgrokException.class, () -> ngrokClient.connect(createTunnel));
    }

    @Test
    public void testConfigV3RoutesToEndpointsApi() {
        // GIVEN
        final JavaNgrokConfig v3Config = new JavaNgrokConfig.Builder(javaNgrokConfig)
            .withConfigVersion(ConfigVersion.V3)
            .build();
        final NgrokProcess mockProcess = mock(NgrokProcess.class);
        when(mockProcess.getApiUrl()).thenReturn("http://localhost:4040");
        when(mockProcess.getNgrokInstaller()).thenReturn(ngrokInstaller);
        final HttpClient mockHttpClient = mock(HttpClient.class);
        final Tunnel stubTunnel = mock(Tunnel.class);
        when(stubTunnel.getName()).thenReturn("my-tunnel");
        when(stubTunnel.getPublicUrl()).thenReturn("https://my.ngrok.dev");
        final Response<Tunnel> stubResponse = new Response<>(201, stubTunnel, "", Map.of());
        when(mockHttpClient.post(any(), any(), eq(Tunnel.class))).thenReturn(stubResponse);
        final NgrokClient v3Client = new NgrokClient.Builder().withJavaNgrokConfig(v3Config)
                                                              .withNgrokProcess(mockProcess)
                                                              .withHttpClient(mockHttpClient)
                                                              .build();

        // WHEN
        v3Client.connect(new CreateTunnel.Builder().withName("my-tunnel").withAddr(8000).build());

        // THEN
        verify(mockHttpClient, times(1)).post(eq("http://localhost:4040/api/endpoints"), any(),
            eq(Tunnel.class));
    }

    @Test
    public void testConfigV3TranslatesAddrToUpstream() {
        // GIVEN
        final JavaNgrokConfig v3Config = new JavaNgrokConfig.Builder(javaNgrokConfig)
            .withConfigVersion(ConfigVersion.V3)
            .build();
        final NgrokProcess mockProcess = mock(NgrokProcess.class);
        when(mockProcess.getApiUrl()).thenReturn("http://localhost:4040");
        when(mockProcess.getNgrokInstaller()).thenReturn(ngrokInstaller);
        final HttpClient mockHttpClient = mock(HttpClient.class);
        final Tunnel stubTunnel = mock(Tunnel.class);
        when(stubTunnel.getName()).thenReturn("my-tunnel");
        when(stubTunnel.getPublicUrl()).thenReturn("https://my.ngrok.dev");
        when(mockHttpClient.post(any(), any(), eq(Tunnel.class)))
            .thenReturn(new Response<>(201, stubTunnel, "", Map.of()));
        final ArgumentCaptor<CreateTunnel> requestCaptor =
            ArgumentCaptor.forClass(CreateTunnel.class);
        final NgrokClient v3Client = new NgrokClient.Builder().withJavaNgrokConfig(v3Config)
                                                              .withNgrokProcess(mockProcess)
                                                              .withHttpClient(mockHttpClient)
                                                              .build();

        // WHEN
        v3Client.connect(new CreateTunnel.Builder().withName("my-tunnel").withAddr(8000).build());

        // THEN
        verify(mockHttpClient).post(any(), requestCaptor.capture(), eq(Tunnel.class));
        final CreateTunnel posted = requestCaptor.getValue();
        assertNotNull(posted.getUpstream());
        assertEquals("http://localhost:8000", posted.getUpstream().getUrl());
    }

    @Test
    public void testConfigV3TcpAddrTranslation() {
        // GIVEN
        final JavaNgrokConfig v3Config = new JavaNgrokConfig.Builder(javaNgrokConfig)
            .withConfigVersion(ConfigVersion.V3)
            .build();
        final NgrokProcess mockProcess = mock(NgrokProcess.class);
        when(mockProcess.getApiUrl()).thenReturn("http://localhost:4040");
        when(mockProcess.getNgrokInstaller()).thenReturn(ngrokInstaller);
        final HttpClient mockHttpClient = mock(HttpClient.class);
        final Tunnel stubTunnel = mock(Tunnel.class);
        when(stubTunnel.getName()).thenReturn("ssh");
        when(stubTunnel.getPublicUrl()).thenReturn("tcp://1.tcp.ngrok.io:12345");
        when(mockHttpClient.post(any(), any(), eq(Tunnel.class)))
            .thenReturn(new Response<>(201, stubTunnel, "", Map.of()));
        final ArgumentCaptor<CreateTunnel> requestCaptor =
            ArgumentCaptor.forClass(CreateTunnel.class);
        final NgrokClient v3Client = new NgrokClient.Builder().withJavaNgrokConfig(v3Config)
                                                              .withNgrokProcess(mockProcess)
                                                              .withHttpClient(mockHttpClient)
                                                              .build();

        // WHEN
        v3Client.connect(new CreateTunnel.Builder().withName("ssh").withProto(Proto.TCP).withAddr(22).build());

        // THEN
        verify(mockHttpClient).post(any(), requestCaptor.capture(), eq(Tunnel.class));
        assertEquals("tcp://localhost:22", requestCaptor.getValue().getUpstream().getUrl());
    }

    @Test
    public void testConfigV3PassesUpstreamThrough() {
        // GIVEN
        final JavaNgrokConfig v3Config = new JavaNgrokConfig.Builder(javaNgrokConfig)
            .withConfigVersion(ConfigVersion.V3)
            .build();
        final NgrokProcess mockProcess = mock(NgrokProcess.class);
        when(mockProcess.getApiUrl()).thenReturn("http://localhost:4040");
        when(mockProcess.getNgrokInstaller()).thenReturn(ngrokInstaller);
        final HttpClient mockHttpClient = mock(HttpClient.class);
        final Tunnel stubTunnel = mock(Tunnel.class);
        when(stubTunnel.getName()).thenReturn("my-tunnel");
        when(stubTunnel.getPublicUrl()).thenReturn("https://my.ngrok.dev");
        when(mockHttpClient.post(any(), any(), eq(Tunnel.class)))
            .thenReturn(new Response<>(201, stubTunnel, "", Map.of()));
        final ArgumentCaptor<CreateTunnel> requestCaptor =
            ArgumentCaptor.forClass(CreateTunnel.class);
        final NgrokClient v3Client = new NgrokClient.Builder().withJavaNgrokConfig(v3Config)
                                                              .withNgrokProcess(mockProcess)
                                                              .withHttpClient(mockHttpClient)
                                                              .build();

        // WHEN
        v3Client.connect(new CreateTunnel.Builder()
            .withName("my-tunnel")
            .withUpstream(new Upstream.Builder().withUrl("http://localhost:8000").withProtocol("http1").build())
            .build());

        // THEN
        verify(mockHttpClient).post(any(), requestCaptor.capture(), eq(Tunnel.class));
        final Upstream upstream = requestCaptor.getValue().getUpstream();
        assertEquals("http://localhost:8000", upstream.getUrl());
        assertEquals("http1", upstream.getProtocol());
    }

    @Test
    public void testConfigV3GetTunnelsRoutesToEndpoints() {
        // GIVEN
        final JavaNgrokConfig v3Config = new JavaNgrokConfig.Builder(javaNgrokConfig)
            .withConfigVersion(ConfigVersion.V3)
            .build();
        final NgrokProcess mockProcess = mock(NgrokProcess.class);
        when(mockProcess.getApiUrl()).thenReturn("http://localhost:4040");
        when(mockProcess.getNgrokInstaller()).thenReturn(ngrokInstaller);
        final HttpClient mockHttpClient = mock(HttpClient.class);
        final Tunnels stubTunnels = mock(Tunnels.class);
        when(stubTunnels.getTunnels()).thenReturn(List.of());
        when(mockHttpClient.get(any(), eq(Tunnels.class)))
            .thenReturn(new Response<>(200, stubTunnels, "", Map.of()));
        final NgrokClient v3Client = new NgrokClient.Builder().withJavaNgrokConfig(v3Config)
                                                              .withNgrokProcess(mockProcess)
                                                              .withHttpClient(mockHttpClient)
                                                              .build();

        // WHEN
        v3Client.getTunnels();

        // THEN
        verify(mockHttpClient).get(eq("http://localhost:4040/api/endpoints"), eq(Tunnels.class));
    }

    @Test
    public void testV3EndpointDefinitions() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final Map<String, Object> config = Map.of(
            "endpoints", List.of(Map.of(
                "name", "v3-endpoint",
                "upstream", Map.of("url", "http://localhost:8000"))));
        final Path configPath2 = Path.of(javaNgrokConfig.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfig.getNgrokVersion(),
            ConfigVersion.V3);
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig)
            .withConfigPath(configPath2)
            .withConfigVersion(ConfigVersion.V3)
            .build();
        ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcess2)
                                                                  .build();

        // WHEN
        final Tunnel tunnel = ngrokClient2.connect(new CreateTunnel.Builder().withName("v3-endpoint").build());

        // THEN
        assertEquals("v3-endpoint-api", tunnel.getName());
        assertNotNull(tunnel.getUpstream());
        assertEquals("http://localhost:8000", tunnel.getUpstream().getUrl());
    }

    @Test
    public void testV3JavaNgrokDefault() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final Map<String, Object> config = Map.of(
            "endpoints", List.of(Map.of(
                "name", "java-ngrok-default",
                "upstream", Map.of("url", "http://localhost:8080"))));
        final Path configPath2 = Path.of(javaNgrokConfig.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfig.getNgrokVersion(),
            ConfigVersion.V3);
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig)
            .withConfigPath(configPath2)
            .withConfigVersion(ConfigVersion.V3)
            .build();
        ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcess2)
                                                                  .build();

        // WHEN
        final Tunnel tunnel = ngrokClient2.connect(new CreateTunnel.Builder().build());

        // THEN
        assertEquals("java-ngrok-default-api", tunnel.getName());
    }
}
