/*
 * Copyright (c) 2021-2025 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import com.github.alexdlaird.http.HttpClient;
import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.installer.NgrokVersion;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * An object that represents a <code>ngrok</code> Tunnel creation request. This object can be serialized and passed to
 * the {@link HttpClient}.
 *
 * <h2>Basic Usage</h2>
 * <pre>
 * final NgrokClient ngrokClient = new NgrokClient.Builder().build();
 *
 * final CreateTunnel createTunnel = new CreateTunnel.Builder()
 *         .withName("my-tunnel")
 *         .withProto(Proto.TCP)
 *         .withAddr(5000)
 *         .build();
 *
 * final Tunnel httpTunnel = ngrokClient.connect(createTunnel);
 * </pre>
 * <h2><code>ngrok</code> Version Compatibility</h2>
 * <code>java-ngrok</code> is compatible with <code>ngrok</code> v2 and v3, but by default it will install v3. To
 * install v2 instead, set the version with {@link JavaNgrokConfig.Builder#withNgrokVersion(NgrokVersion)} and
 * {@link CreateTunnel.Builder#withNgrokVersion(NgrokVersion)}.
 */
public class CreateTunnel {

    // The ngrokVersion is transient so that it can be serialized to a valid request
    private final transient NgrokVersion ngrokVersion;
    private final String name;
    private final Proto proto;
    private final String domain;
    private final String addr;
    private final Boolean inspect;
    private final String auth;
    private final String hostHeader;
    private final BindTls bindTls;
    private final String subdomain;
    private final String crt;
    private final String key;
    private final String remoteAddr;
    private final String metadata;
    private final List<String> schemes;
    private final List<String> basicAuth;
    private final TunnelOAuth oauth;
    private final Float circuitBreaker;
    private final Boolean compression;
    private final String mutualTlsCas;
    private final String proxyProto;
    private final Boolean websocketTcpConverter;
    private final String terminateAt;
    private final TunnelHeader requestHeader;
    private final TunnelHeader responseHeader;
    private final TunnelIPRestriction ipRestriction;
    private final TunnelVerifyWebhook verifyWebhook;
    private final TunnelUserAgentFilter userAgentFilter;
    private final TunnelPolicy policyInbound;
    private final TunnelPolicy policyOutbound;
    private final List<String> labels;
    private final Boolean poolingEnabled;

    private CreateTunnel(final Builder builder) {
        this.ngrokVersion = builder.ngrokVersion;
        this.name = builder.name;
        this.proto = builder.proto;
        this.domain = builder.domain;
        this.addr = builder.addr;
        this.inspect = builder.inspect;
        this.auth = builder.auth;
        this.hostHeader = builder.hostHeader;
        this.bindTls = builder.bindTls;
        this.subdomain = builder.subdomain;
        this.crt = builder.crt;
        this.key = builder.key;
        this.remoteAddr = builder.remoteAddr;
        this.metadata = builder.metadata;
        this.schemes = builder.schemes;
        this.basicAuth = builder.basicAuth;
        this.oauth = builder.oauth;
        this.circuitBreaker = builder.circuitBreaker;
        this.compression = builder.compression;
        this.mutualTlsCas = builder.mutualTlsCas;
        this.proxyProto = builder.proxyProto;
        this.websocketTcpConverter = builder.websocketTcpConverter;
        this.terminateAt = builder.terminateAt;
        this.requestHeader = builder.requestHeader;
        this.responseHeader = builder.responseHeader;
        this.ipRestriction = builder.ipRestriction;
        this.verifyWebhook = builder.verifyWebhook;
        this.userAgentFilter = builder.userAgentFilter;
        this.policyInbound = builder.policyInbound;
        this.policyOutbound = builder.policyOutbound;
        this.labels = builder.labels;
        this.poolingEnabled = builder.poolingEnabled;
    }

    /**
     * Get the version of <code>ngrok</code> for which the tunnel was created.
     */
    public NgrokVersion getNgrokVersion() {
        return ngrokVersion;
    }

    /**
     * Get the name of the tunnel.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the tunnel protocol.
     */
    public Proto getProto() {
        return proto;
    }

    /**
     * Get the tunnel domain.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Get the local port to which the tunnel will forward traffic.
     */
    public String getAddr() {
        return addr;
    }

    /**
     * Whether HTTP request inspection on tunnels is enabled.
     */
    public Boolean isInspect() {
        return inspect;
    }

    /**
     * Get HTTP basic authentication credentials enforced on tunnel requests.
     */
    public String getAuth() {
        return auth;
    }

    /**
     * Get the HTTP Host header.
     */
    public String getHostHeader() {
        return hostHeader;
    }

    /**
     * Get <code>ngrok</code>'s <code>bind_tls</code> value.
     */
    public BindTls getBindTls() {
        return bindTls;
    }

    /**
     * Get the subdomain.
     */
    public String getSubdomain() {
        return subdomain;
    }

    /**
     * Get the PEM TLS certificate path that will be used to terminate TLS traffic before forwarding locally.
     */
    public String getCrt() {
        return crt;
    }

    /**
     * Get the PEM TLS private key path that will be used to terminate TLS traffic before forwarding locally.
     */
    public String getKey() {
        return key;
    }

    /**
     * Get the bound remote TCP port on the given address.
     */
    public String getRemoteAddr() {
        return remoteAddr;
    }

    /**
     * Get the arbitrary user-defined metadata that will appear in the ngrok service API when listing tunnels.
     */
    public String getMetadata() {
        return metadata;
    }

    /**
     * Get the schemes to be bound.
     */
    public List<String> getSchemes() {
        return schemes;
    }

    /**
     * Get the list of HTTP basic authentication credentials to enforce on tunneled requests.
     */
    public List<String> getBasicAuth() {
        return basicAuth;
    }

    /**
     * Get the OAuth settings to be setup on the tunnel.
     */
    public TunnelOAuth getOauth() {
        return oauth;
    }

    /**
     * Get the circuit breaker trigger.
     */
    public Float getCircuitBreaker() {
        return circuitBreaker;
    }

    /**
     * Whether compression is enabled on this tunnel.
     */
    public Boolean isCompression() {
        return compression;
    }

    /**
     * Get the path to the TLS certificate authority to verify client certs.
     */
    public String getMutualTlsCas() {
        return mutualTlsCas;
    }

    /**
     * Get the proxy proto.
     */
    public String getProxyProto() {
        return proxyProto;
    }

    /**
     * Whether ingress connections are converted to TCP upstream.
     */
    public Boolean isWebsocketTcpConverter() {
        return websocketTcpConverter;
    }

    /**
     * Get the termination point.
     */
    public String getTerminateAt() {
        return terminateAt;
    }

    /**
     * Get the Headers to be added or removed from requests.
     */
    public TunnelHeader getRequestHeader() {
        return requestHeader;
    }

    /**
     * Get the Headers to be added or removed from responses.
     */
    public TunnelHeader getResponseHeader() {
        return responseHeader;
    }

    /**
     * Get the IP restrictions for the tunnel.
     */
    public TunnelIPRestriction getIpRestriction() {
        return ipRestriction;
    }

    /**
     * Get the signature for webhooks.
     */
    public TunnelVerifyWebhook getVerifyWebhook() {
        return verifyWebhook;
    }

    /**
     * Get the UserAgent filters.
     */
    public TunnelUserAgentFilter getUserAgentFilter() {
        return userAgentFilter;
    }

    /**
     * Get the inbound policy.
     */
    public TunnelPolicy getPolicyInbound() {
        return policyInbound;
    }

    /**
     * Get the outbound policy.
     */
    public TunnelPolicy getPolicyOutbound() {
        return policyOutbound;
    }

    /**
     * Get the labels. Note that labels can only be provisioned from the <code>ngrok</code> config file and not the
     * Builder.
     */
    public List<String> getLabels() {
        return labels;
    }

    /**
     * Whether pooling is enabled on this tunnel.
     */
    public Boolean isPoolingEnabled() {
        return poolingEnabled;
    }

    /**
     * Builder for a {@link CreateTunnel}, which can be used to construct a request that conforms to <a
     * href="https://ngrok.com/docs/agent/config/v2/#tunnel-configurations"
     * target="_blank"><code>ngrok</code>'s tunnel definition</a>. See docs for that class for example usage.
     */
    public static class Builder {

        private boolean setDefaults = false;

        private NgrokVersion ngrokVersion;
        private String name;
        private Proto proto;
        private String domain;
        private String addr;
        private Boolean inspect;
        private BindTls bindTls;
        private String auth;
        private String hostHeader;
        private String subdomain;
        private String crt;
        private String key;
        private String remoteAddr;
        private String metadata;
        private List<String> schemes;
        private List<String> basicAuth;
        private TunnelOAuth oauth;
        private Float circuitBreaker;
        private Boolean compression;
        private String mutualTlsCas;
        private String proxyProto;
        private Boolean websocketTcpConverter;
        private String terminateAt;
        private TunnelHeader requestHeader;
        private TunnelHeader responseHeader;
        private TunnelIPRestriction ipRestriction;
        private TunnelVerifyWebhook verifyWebhook;
        private TunnelUserAgentFilter userAgentFilter;
        private TunnelPolicy policyInbound;
        private TunnelPolicy policyOutbound;
        private List<String> labels;
        private Boolean poolingEnabled;

        /**
         * Use this constructor if default values should not be populated in required attributes when {@link #build()}
         * is called.
         *
         * <p>If required attributes are not set in the built {@link CreateTunnel}, default values will be used in
         * methods like {@link NgrokClient#connect(CreateTunnel)}.
         */
        public Builder() {
        }

        /**
         * Use this constructor if default values should be populated in required attributes when {@link #build()} is
         * called.
         *
         * @param setDefaults <code>true</code> to populate defaults.
         */
        public Builder(final boolean setDefaults) {
            this.setDefaults = setDefaults;
        }

        /**
         * Copy a {@link CreateTunnel} in to a new Builder. Using this constructor will also set default attributes when
         * {@link #build} is called.
         *
         * @param createTunnel The CreateTunnel to copy.
         */
        public Builder(final CreateTunnel createTunnel) {
            this.setDefaults = true;

            this.ngrokVersion = createTunnel.ngrokVersion;
            this.name = createTunnel.name;
            this.proto = createTunnel.proto;
            this.domain = createTunnel.domain;
            this.addr = createTunnel.addr;
            this.inspect = createTunnel.inspect;
            this.bindTls = createTunnel.bindTls;
            this.auth = createTunnel.auth;
            this.hostHeader = createTunnel.hostHeader;
            this.subdomain = createTunnel.subdomain;
            this.crt = createTunnel.crt;
            this.key = createTunnel.key;
            this.remoteAddr = createTunnel.remoteAddr;
            this.metadata = createTunnel.metadata;
            this.schemes = createTunnel.schemes;
            this.basicAuth = createTunnel.basicAuth;
            this.oauth = createTunnel.oauth;
            this.circuitBreaker = createTunnel.circuitBreaker;
            this.compression = createTunnel.compression;
            this.mutualTlsCas = createTunnel.mutualTlsCas;
            this.proxyProto = createTunnel.proxyProto;
            this.websocketTcpConverter = createTunnel.websocketTcpConverter;
            this.terminateAt = createTunnel.terminateAt;
            this.requestHeader = createTunnel.requestHeader;
            this.responseHeader = createTunnel.responseHeader;
            this.ipRestriction = createTunnel.ipRestriction;
            this.verifyWebhook = createTunnel.verifyWebhook;
            this.userAgentFilter = createTunnel.userAgentFilter;
            this.policyInbound = createTunnel.policyInbound;
            this.policyOutbound = createTunnel.policyOutbound;
            this.poolingEnabled = createTunnel.poolingEnabled;
        }

        /**
         * The major version of <code>ngrok</code> for which the tunnel will be created.
         */
        public Builder withNgrokVersion(final NgrokVersion ngrokVersion) {
            this.ngrokVersion = ngrokVersion;
            return this;
        }

        /**
         * A friendly name for the tunnel, or the name of a <a
         * href="https://ngrok.com/docs/agent/config/v2/#tunnel-configurations"
         * target="_blank">ngrok tunnel definition</a> defined in <code>ngrok</code>'s config file.
         */
        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        /**
         * The tunnel protocol, defaults to {@link Proto#HTTP}.
         */
        public Builder withProto(final Proto proto) {
            this.proto = proto;
            return this;
        }

        /**
         * The tunnel domain.
         */
        public Builder withDomain(final String domain) {
            this.domain = domain;
            return this;
        }

        /**
         * The local port to which the tunnel will forward traffic, or a
         * <a href="https://ngrok.com/docs/universal-gateway/http/?cty=agent-config#agent-endpoint">local directory or network address</a>, defaults to "80".
         */
        public Builder withAddr(final String addr) {
            this.addr = addr;
            return this;
        }

        /**
         * See {@link #withAddr(String)}.
         */
        public Builder withAddr(final int addr) {
            return withAddr(String.valueOf(addr));
        }

        /**
         * Disable HTTP request inspection on tunnels.
         */
        public Builder withoutInspect() {
            this.inspect = false;
            return this;
        }

        /**
         * HTTP basic authentication credentials to enforce on tunneled requests.
         *
         * @throws IllegalArgumentException The argument was invalid.
         */
        public Builder withAuth(final String auth) {
            if (nonNull(basicAuth)) {
                throw new IllegalArgumentException("Cannot set both 'auth' and 'basicAuth'.");
            }

            this.auth = auth;
            return this;
        }

        /**
         * Rewrite the HTTP Host header to this value, or <code>preserve</code> to leave it unchanged.
         */
        public Builder withHostHeader(final String hostHeader) {
            this.hostHeader = hostHeader;
            return this;
        }

        /**
         * Bind an HTTPS ({@link BindTls#TRUE} or HTTP ({@link BindTls#FALSE}) endpoint, defaults to
         * {@link BindTls#BOTH}.
         *
         * @throws IllegalArgumentException The argument was invalid.
         */
        public Builder withBindTls(final BindTls bindTls) {
            if (nonNull(schemes)) {
                throw new IllegalArgumentException("Cannot set both 'schemes' and 'bindTls'.");
            }

            this.bindTls = bindTls;
            return this;
        }

        /**
         * See {@link #withBindTls(BindTls)}.
         */
        public Builder withBindTls(final boolean bindTls) {
            return withBindTls(BindTls.valueOf(String.valueOf(bindTls).toUpperCase()));
        }

        /**
         * Subdomain name to request. If unspecified, uses the tunnel name.
         */
        public Builder withSubdomain(final String subdomain) {
            this.subdomain = subdomain;
            return this;
        }

        /**
         * PEM TLS certificate at this path to terminate TLS traffic before forwarding locally.
         */
        public Builder withCrt(final String crt) {
            this.crt = crt;
            return this;
        }

        /**
         * PEM TLS private key at this path to terminate TLS traffic before forwarding locally.
         */
        public Builder withKey(final String key) {
            this.key = key;
            return this;
        }

        /**
         * Bind the remote TCP port on the given address.
         */
        public Builder withRemoteAddr(final String remoteAddr) {
            this.remoteAddr = remoteAddr;
            return this;
        }

        /**
         * Arbitrary user-defined metadata that will appear in the ngrok service API when listing tunnels.
         */
        public Builder withMetadata(final String metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * The schemes to be bound.
         *
         * @throws IllegalArgumentException The argument was invalid.
         */
        public Builder withSchemes(final List<String> schemes) {
            if (nonNull(bindTls)) {
                throw new IllegalArgumentException("Cannot set both 'schemes' and 'bindTls'.");
            }

            this.schemes = Collections.unmodifiableList(schemes);
            return this;
        }

        /**
         * List of HTTP basic authentication credentials to enforce on tunneled requests.
         *
         * @throws IllegalArgumentException The argument was invalid.
         */
        public Builder withBasicAuth(final List<String> basicAuth) {
            if (nonNull(auth)) {
                throw new IllegalArgumentException("Cannot set both 'auth' and 'basicAuth'.");
            }

            this.basicAuth = Collections.unmodifiableList(basicAuth);
            return this;
        }

        /**
         * Set of OAuth settings to enable OAuth authentication on the tunnel endpoint.
         */
        public Builder withOAuth(final TunnelOAuth oauth) {
            this.oauth = oauth;
            return this;
        }

        /**
         * The circuit breaker trigger.
         */
        public Builder withCircuitBreaker(final Float circuitBreaker) {
            this.circuitBreaker = circuitBreaker;
            return this;
        }

        /**
         * Whether compression is enabled on this tunnel.
         */
        public Builder withCompression(final Boolean compression) {
            this.compression = compression;
            return this;
        }

        /**
         * The path to the TLS certificate authority to verify client certs.
         */
        public Builder withMutualTlsCas(final String mutualTlsCas) {
            this.mutualTlsCas = mutualTlsCas;
            return this;
        }

        /**
         * The proxy proto.
         */
        public Builder withProxyProto(final String proxyProto) {
            this.proxyProto = proxyProto;
            return this;
        }

        /**
         * Whether ingress connections are converted to TCP upstream.
         */
        public Builder withWebsocketTcpConverter(final Boolean websocketTcpConverter) {
            this.websocketTcpConverter = websocketTcpConverter;
            return this;
        }

        /**
         * The termination point.
         */
        public Builder withTerminateAt(final String terminateAt) {
            this.terminateAt = terminateAt;
            return this;
        }

        /**
         * The Headers to be added or removed from requests.
         */
        public Builder withRequestHeader(final TunnelHeader requestHeader) {
            this.requestHeader = requestHeader;
            return this;
        }

        /**
         * The Headers to be added or removed from responses.
         */
        public Builder withResponseHeader(final TunnelHeader responseHeader) {
            this.responseHeader = responseHeader;
            return this;
        }

        /**
         * The IP restrictions for the tunnel.
         */
        public Builder withIpRestriction(final TunnelIPRestriction ipRestriction) {
            this.ipRestriction = ipRestriction;
            return this;
        }

        /**
         * The signature for webhooks.
         */
        public Builder withVerifyWebhook(final TunnelVerifyWebhook verifyWebhook) {
            this.verifyWebhook = verifyWebhook;
            return this;
        }

        /**
         * The UserAgent filter for the tunnel.
         */
        public Builder withUserAgentFilter(final TunnelUserAgentFilter userAgentFilter) {
            this.userAgentFilter = userAgentFilter;
            return this;
        }

        /**
         * The inbound policy for the tunnel.
         */
        public Builder withPolicyInbound(final TunnelPolicy policyInbound) {
            this.policyInbound = policyInbound;
            return this;
        }

        /**
         * The outbound policy for the tunnel.
         */
        public Builder withPolicyOutbound(final TunnelPolicy policyOutbound) {
            this.policyOutbound = policyOutbound;
            return this;
        }

        /**
         * Whether pooling is enabled on this tunnel.
         */
        public Builder withPoolingEnabled(final Boolean poolingEnabled) {
            this.poolingEnabled = poolingEnabled;
            return this;
        }

        /**
         * Populate any <code>null</code> attributes (except for <code>name</code>) in this Builder with values from the
         * given <code>tunnelDefinition</code>.
         *
         * @param tunnelDefinition The map from which <code>null</code> attributes will be populated.
         * @throws IllegalArgumentException The argument was invalid.
         */
        public Builder withTunnelDefinition(Map<String, Object> tunnelDefinition) {
            if (isNull(this.proto) && tunnelDefinition.containsKey("proto")) {
                this.proto = Proto.valueOf(((String) tunnelDefinition.get("proto")).toUpperCase());
            }
            if (isNull(this.domain) && tunnelDefinition.containsKey("domain")) {
                this.domain = (String) tunnelDefinition.get("domain");
            }
            if (isNull(this.addr) && tunnelDefinition.containsKey("addr")) {
                this.addr = (String) tunnelDefinition.get("addr");
            }
            if (isNull(this.inspect) && tunnelDefinition.containsKey("inspect")) {
                this.inspect = Boolean.valueOf(String.valueOf(tunnelDefinition.get("inspect")));
            }
            if (isNull(this.bindTls) && tunnelDefinition.containsKey("bind_tls")) {
                this.bindTls = BindTls.valueOf((String.valueOf(tunnelDefinition.get("bind_tls"))).toUpperCase());
            }
            if (isNull(this.auth) && tunnelDefinition.containsKey("auth")) {
                this.auth = (String) tunnelDefinition.get("auth");
            }
            if (isNull(this.hostHeader) && tunnelDefinition.containsKey("host_header")) {
                this.hostHeader = (String) tunnelDefinition.get("host_header");
            }
            if (isNull(this.subdomain) && tunnelDefinition.containsKey("subdomain")) {
                this.subdomain = (String) tunnelDefinition.get("subdomain");
            }
            if (isNull(this.crt) && tunnelDefinition.containsKey("crt")) {
                this.crt = (String) tunnelDefinition.get("crt");
            }
            if (isNull(this.key) && tunnelDefinition.containsKey("key")) {
                this.key = (String) tunnelDefinition.get("key");
            }
            if (isNull(this.remoteAddr) && tunnelDefinition.containsKey("remote_addr")) {
                this.remoteAddr = (String) tunnelDefinition.get("remote_addr");
            }
            if (isNull(this.metadata) && tunnelDefinition.containsKey("metadata")) {
                this.metadata = (String) tunnelDefinition.get("metadata");
            }
            if (isNull(this.schemes) && tunnelDefinition.containsKey("schemes")) {
                this.schemes = Collections.unmodifiableList(
                    (List<String>) tunnelDefinition.get("schemes")
                );
            }
            if (isNull(this.basicAuth) && tunnelDefinition.containsKey("basic_auth")) {
                this.basicAuth = Collections.unmodifiableList(
                    (List<String>) tunnelDefinition.get("basic_auth")
                );
            }
            if (isNull(this.oauth) && tunnelDefinition.containsKey("oauth")) {
                this.oauth = new TunnelOAuth.Builder((Map<String, Object>) tunnelDefinition.get("oauth")).build();
            }
            if (isNull(this.circuitBreaker) && tunnelDefinition.containsKey("circuit_breaker")) {
                this.circuitBreaker = Float.valueOf(String.valueOf(tunnelDefinition.get("circuit_breaker")));
            }
            if (isNull(this.compression) && tunnelDefinition.containsKey("compression")) {
                this.compression = Boolean.valueOf(String.valueOf(tunnelDefinition.get("compression")));
            }
            if (isNull(this.mutualTlsCas) && tunnelDefinition.containsKey("mutual_tls_cas")) {
                this.mutualTlsCas = (String) tunnelDefinition.get("mutual_tls_cas");
            }
            if (isNull(this.proxyProto) && tunnelDefinition.containsKey("proxy_proto")) {
                this.proxyProto = (String) tunnelDefinition.get("proxy_proto");
            }
            if (isNull(this.websocketTcpConverter) && tunnelDefinition.containsKey("websocket_tcp_converter")) {
                this.websocketTcpConverter = Boolean.valueOf(
                    String.valueOf(tunnelDefinition.get("websocket_tcp_converter"))
                );
            }
            if (isNull(this.terminateAt) && tunnelDefinition.containsKey("terminate_at")) {
                this.terminateAt = (String) tunnelDefinition.get("terminate_at");
            }
            if (isNull(this.requestHeader) && tunnelDefinition.containsKey("request_header")) {
                this.requestHeader = new TunnelHeader
                    .Builder((Map<String, Object>) tunnelDefinition.get("request_header"))
                    .build();
            }
            if (isNull(this.responseHeader) && tunnelDefinition.containsKey("response_header")) {
                this.responseHeader = new TunnelHeader
                    .Builder((Map<String, Object>) tunnelDefinition.get("response_header"))
                    .build();
            }
            if (isNull(this.ipRestriction) && tunnelDefinition.containsKey("ip_restriction")) {
                this.ipRestriction = new TunnelIPRestriction
                    .Builder((Map<String, Object>) tunnelDefinition.get("ip_restriction"))
                    .build();
            }
            if (isNull(this.verifyWebhook) && tunnelDefinition.containsKey("verify_webhook")) {
                this.verifyWebhook = new TunnelVerifyWebhook
                    .Builder((Map<String, Object>) tunnelDefinition.get("verify_webhook"))
                    .build();
            }
            if (isNull(this.userAgentFilter) && tunnelDefinition.containsKey("user_agent_filter")) {
                this.userAgentFilter = new TunnelUserAgentFilter
                    .Builder((Map<String, Object>) tunnelDefinition.get("user_agent_filter"))
                    .build();
            }
            if (isNull(this.poolingEnabled) && tunnelDefinition.containsKey("pooling_enabled")) {
                this.poolingEnabled = Boolean.valueOf(String.valueOf(tunnelDefinition.get("pooling_enabled")));
            }
            if (tunnelDefinition.containsKey("policy") || tunnelDefinition.containsKey("traffic_policy")) {
                final String policyKey = tunnelDefinition.containsKey("policy") ? "policy" : "traffic_policy";

                final Map<String, Object> policy = (Map<String, Object>) tunnelDefinition.get(policyKey);
                if (isNull(this.policyInbound) && policy.containsKey("inbound")) {
                    this.policyInbound = new TunnelPolicy
                        .Builder((Map<String, Object>) policy.get("inbound"))
                        .build();
                }
                // For HTTP, ngrok has renamed this key to "on_http_request", but it functions the same
                if (isNull(this.policyInbound) && policy.containsKey("on_http_request")) {
                    this.policyInbound = new TunnelPolicy
                        .Builder((Map<String, Object>) policy.get("on_http_request"))
                        .build();
                }
                if (isNull(this.policyOutbound) && policy.containsKey("outbound")) {
                    this.policyOutbound = new TunnelPolicy
                        .Builder((Map<String, Object>) policy.get("outbound"))
                        .build();
                }
                // For HTTP, ngrok has renamed this key to "on_http_response", but it functions the same
                if (isNull(this.policyOutbound) && policy.containsKey("on_http_response")) {
                    this.policyOutbound = new TunnelPolicy
                        .Builder((Map<String, Object>) policy.get("on_http_response"))
                        .build();
                }
            }
            if (tunnelDefinition.containsKey("labels")) {
                if (nonNull(bindTls)) {
                    throw new IllegalArgumentException("'bindTls' cannot be set when 'labels' is also on the "
                                                       + "tunnel definition.");
                }
                this.labels = Collections.unmodifiableList(
                    (List<String>) tunnelDefinition.get("labels")
                );
            }

            // Returning this to allow chained configuration of
            // properties not visible in ngrok's GET /api/tunnels endpoint
            return this;
        }

        /**
         * Build the {@link CreateTunnel}.
         */
        public CreateTunnel build() {
            if (isNull(ngrokVersion)) {
                ngrokVersion = NgrokVersion.V3;
            }

            if (setDefaults) {
                if (isNull(proto) && isNull(labels)) {
                    proto = Proto.HTTP;
                }
                if (isNull(addr)) {
                    addr = "80";
                }
                if (isNull(name)) {
                    if (!addr.startsWith("file://")) {
                        name = String.format("%s-%s-%s", proto, addr, UUID.randomUUID());
                    } else {
                        name = String.format("%s-file-%s", proto, UUID.randomUUID());
                    }
                }
                if (ngrokVersion == NgrokVersion.V2 && isNull(bindTls)) {
                    bindTls = BindTls.BOTH;
                }
                if (ngrokVersion == NgrokVersion.V3) {
                    if (nonNull(bindTls)) {
                        if (bindTls == BindTls.TRUE) {
                            schemes = List.of("https");
                        } else if (bindTls == BindTls.FALSE) {
                            schemes = List.of("http");
                        } else {
                            schemes = List.of("http", "https");
                        }

                        bindTls = null;
                    }
                    if (nonNull(auth)) {
                        basicAuth = List.of(auth);

                        auth = null;
                    }
                }
            }

            return new CreateTunnel(this);
        }
    }
}
