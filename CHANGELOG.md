# Changelog

All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased](https://github.com/alexdlaird/java-ngrok/compare/2.3.16...HEAD)

## [2.3.16](https://github.com/alexdlaird/java-ngrok/compare/2.3.15...2.3.16) - 2025-07-09

### Added

- Support for `s390x`, `ppc64`, and `ppc64le` architectures.
- Stability improvements.

### Added

- Stability improvements.

## [2.3.15](https://github.com/alexdlaird/java-ngrok/compare/2.3.14...2.3.15) - 2025-05-28

### Added

- Documentation improvements.

## [2.3.14](https://github.com/alexdlaird/java-ngrok/compare/2.3.12...2.3.14) - 2025-05-16

### Added

- Documentation improvements.

### Fixed

- Noisy log around `ProcessMonitor` thread shutting down.

## [2.3.12](https://github.com/alexdlaird/java-ngrok/compare/2.3.11...2.3.12) - 2025-05-16

### Added

- Support for [MSYS2's `mingw`](https://www.msys2.org/) when installing the agent. Like `cygwin`, it is mapped to its corresponding Windows binary.
- Windows ARM 64-bit support.
- FreeBSD ARM support.
- Build and stability improvements.
- Documentation improvements.

### Fixed

- `NgrokV2CDNUrl` and `NgrokV3CDNUrl` both improperly defined the constant for Apple Silicon as `DARWIN_i386_arm`. It has been renamed to `DARWIN_x86_64_arm`.

## [2.3.11](https://github.com/alexdlaird/java-ngrok/compare/2.3.10...2.3.11) - 2025-05-11

### Added

- [`NgrokClient.api()`](https://javadoc.io/static/com.github.alexdlaird/java-ngrok/2.3.11/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/NgrokClient.html#api(java.util.List)), allowing `api` commands to be executed from the agent.
- [Agent interface](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.3.11/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/agent/NgrokAgent.html), which provides access to Captured Requests and agent status.
- [`slf4j`](https://www.slf4j.org/) dependency, making logging improvements.
- Stability improvements.
- Documentation improvements.

### Changed

- `NgrokLog.lvl` now consistently parses `ERR`, `EROR`, and `CRIT` to `ERROR` instead of `SEVERE`.

### Fixed

- Refactored concurrency around `ProcessMonitor` and startup logging, as the old design could lead to race conditions when startup errored.
- `NgrokLog` string may contain a malformed or empty `lvl`. Default is now `NOTSET` in such cases.

## [2.3.10](https://github.com/alexdlaird/java-ngrok/compare/2.3.9...2.3.10) - 2025-05-07

### Added

- Support for `pooling_enabled` in [CreateTunnel](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.3.10/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/protocol/CreateTunnel.Builder.html#withPoolingEnabled(java.lang.Boolean)), which allows for the use of `https` and `tls` [Internal Endpoints](https://ngrok.com/docs/universal-gateway/internal-endpoints/).
- Stability improvements around concurrency, process execution.
- Documentation improvements.
- Test improvements.

## [2.3.9](https://github.com/alexdlaird/java-ngrok/compare/2.3.8...2.3.9) - 2025-04-23

### Added

- Deprecation warning when using Labeled Tunnels, since `ngrok` Edges will be sunset December 31st, 2025. See [this issue](https://github.com/alexdlaird/java-ngrok/issues/158) for more details.
- Migrate build from OSSRH to Central publishing. 
- Updates to Maven Central badge links.
- Documentation improvements.
- Build improvements.

## [2.3.8](https://github.com/alexdlaird/java-ngrok/compare/2.3.7...2.3.8) - 2025-03-16

### Added

- [`DefaultHttpClient`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.3.8/com.github.alexdlaird.ngrok/com/github/alexdlaird/http/DefaultHttpClient.html#get(java.lang.String,java.util.List,java.util.Map,java.lang.Class)) now respects `retryCount`, so API `GET` operations also retry if configured.

### Fixed

- `process` and `processMonitor` are no longer set to `null` when [NgrokProcess.stop()](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.3.8/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/process/NgrokProcess.html#stop()) is called, as this was error prone behavior that also suppressed useful information.

## [2.3.7](https://github.com/alexdlaird/java-ngrok/compare/2.3.6...2.3.7) - 2025-02-18

### Added

- Documentation improvements.

## [2.3.6](https://github.com/alexdlaird/java-ngrok/compare/2.3.5...2.3.6) - 2025-01-28

### Added

- Documentation improvements.

## [2.3.5](https://github.com/alexdlaird/java-ngrok/compare/2.3.4...2.3.5) - 2025-01-08

### Added

- Support for `ngrok` config version 3.
- Support for `traffic_policy` tunnel definition, which `ngrok` renamed from `policy`.
- Support for `traffic_policy.on_http_request` and `traffic_policy.on_http_response`, which `ngrok` added unique keys for in HTTP tunnels.
- If a value for `apiKey` is not set in [`JavaNgrokConfig`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.3.5/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/conf/JavaNgrokConfig.html), it will attempt to use the environment variable `NGROK_API_KEY` if it is set.

### Remove

- Tunnel definition fields `hostname` and `client_cas`, as they are no longer supported by `ngrok`.

## [2.3.4](https://github.com/alexdlaird/java-ngrok/compare/2.3.3...2.3.4) - 2024-12-13

### Added

- Test improvements.

### Changed

- Permission for installed binary to limit execute to user.

## [2.3.3](https://github.com/alexdlaird/java-ngrok/compare/2.3.2...2.3.3) - 2024-11-10

### Added

- Documentation improvements.

## [2.3.2](https://github.com/alexdlaird/java-ngrok/compare/2.3.1...2.3.2) - 2024-11-08

### Added

- Documentation improvements.

### Fixed

- [NgrokProcess.ProcessMonitor.stop()](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.3.2/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/process/NgrokProcess.ProcessMonitor.html#stop()) is now public (it was already documented as such).

## [2.3.1](https://github.com/alexdlaird/java-ngrok/compare/2.3.0...2.3.1) - 2024-11-04

### Added

- Documentation improvements update links to `ngrok`'s documentation.
- Test improvements.

## [2.3.0](https://github.com/alexdlaird/java-ngrok/compare/2.2.16...2.3.0) - 2024-04-08

### Added

- Support for `domain` configuration when
  building [CreateTunnel](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.3.0/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/protocol/CreateTunnel.Builder.html#withDomain(java.lang.String)).
- Support for `user_agent_filter` configuration when
  building [CreateTunnel](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.3.0/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/protocol/CreateTunnel.Builder.html#withUserAgentFilter(com.github.alexdlaird.ngrok.protocol.TunnelUserAgentFilter)).
- Support for `policy` configuration when
  building [CreateTunnel](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.3.0/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/protocol/CreateTunnel.Builder.html#withPolicy(com.github.alexdlaird.ngrok.protocol.TunnelPolicy)).
- `us-cal-1`
  to [Region](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.3.0/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/protocol/Region.html).
- Test cases for TLS tunnels.
- Build improvements.

### Fixed

- `ngrok` config value `ip_restriction` was incorrectly plural in previous versions of `java-ngrok`. Value is now
  interpreted as singular to align with [the `ngrok` docs](https://ngrok.com/docs/agent/config/v2/#http-configuration), and
  classes and methods associated with it,
  like [TunnelIPRestriction](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.3.0/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/protocol/TunnelIPRestriction.html),
  have been renamed.

## [2.2.16](https://github.com/alexdlaird/java-ngrok/compare/2.2.15...2.2.16) - 2024-03-24

### Added

- Build and stability improvements.

## [2.2.15](https://github.com/alexdlaird/java-ngrok/compare/2.2.14...2.2.15) - 2024-03-08

### Added

- `obj` parsing
  in [`NgrokLog`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.2.15/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/process/NgrokLog.html).
- `throws` for unchecked exceptions to documentation.
- Documentation improvements.
- Build and stability improvements.

### Fixed

- Minor bugs, including a typo in the name
  of [JavaNgrokConfig.getStartupTimeout()](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.2.15/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/conf/JavaNgrokConfig.html#getStartupTimeout()).

## [2.2.14](https://github.com/alexdlaird/java-ngrok/compare/2.2.13...2.2.14) - 2024-03-06

### Added

- Build and stability improvements.
- Documentation and style improvements.

## [2.2.13](https://github.com/alexdlaird/java-ngrok/compare/2.2.12...2.2.13) - 2024-02-26

### Added

- Build improvements.
- Documentation improvements.

## [2.2.12](https://github.com/alexdlaird/java-ngrok/compare/2.2.10...2.2.12) - 2024-02-18

### Added

- Build and stability improvements.

## [2.2.10](https://github.com/alexdlaird/java-ngrok/compare/2.2.9...2.2.10) - 2024-02-15

### Changed

- Moved evaluation of HTTP `GET` retry logic from `DefaultHttpClient`'s [`getInputStream()`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.2.10/com.github.alexdlaird.ngrok/com/github/alexdlaird/http/DefaultHttpClient.html#getInputStream(java.net.HttpURLConnection,java.lang.String,java.lang.String,java.util.Map)) to [`get()`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.2.10/com.github.alexdlaird.ngrok/com/github/alexdlaird/http/DefaultHttpClient.html#get(java.lang.String,java.util.List,java.util.Map,java.nio.file.Path,int)).
- Build and stability improvements.

## [2.2.9](https://github.com/alexdlaird/java-ngrok/compare/2.2.8...2.2.9) - 2024-02-14

### Added

- Stability improvements.

## [2.2.8](https://github.com/alexdlaird/java-ngrok/compare/2.2.7...2.2.8) - 2024-01-08

### Added

- Support for Java 21.
- Build improvements.

## [2.2.7](https://github.com/alexdlaird/java-ngrok/compare/2.2.6...2.2.7) - 2023-12-30

### Fixed

- Test improvements, suite now respects `NGROK_AUTHTOKEN` for all necessary tests (skipped if not set, rather than tests
  failing).

## [2.2.6](https://github.com/alexdlaird/java-ngrok/compare/2.2.5...2.2.6) - 2023-12-27

### Added

- If a value for `authToken` is not set
  in [`JavaNgrokConfig`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.2.5/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/conf/JavaNgrokConfig.html),
  it will attempt to use the environment variable `NGROK_AUTHTOKEN` if it is set.
- Build improvements.

## [2.2.5](https://github.com/alexdlaird/java-ngrok/compare/2.2.4...2.2.5) - 2023-12-01

### Changed

- `java-ngrok` to no longer install the config file in a legacy location, now
  respects [`ngrok`'s default locations](https://ngrok.com/docs/agent/config/#default-locations).

### Fixed

- Build improvements.

## [2.2.4](https://github.com/alexdlaird/java-ngrok/compare/2.2.3...2.2.4) - 2023-11-14

### Added

- Documentation improvements.

## [2.2.3](https://github.com/alexdlaird/java-ngrok/compare/2.2.2...2.2.3) - 2023-09-17

### Added

- `retryCount` added to [`DefaultHttpClient`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.2.3/com.github.alexdlaird.ngrok/com/github/alexdlaird/http/DefaultHttpClient.html), so `GET` options can now support retries.
- Documentation improvements.
- Test improvements.

### Changed

- `NgrokInstaller` now uses the `DefaultHttpClient` to download `ngrok` binaries.

## [2.2.2](https://github.com/alexdlaird/java-ngrok/compare/2.2.1...2.2.2) - 2023-09-14

### Added

- Documentation improvements.

## [2.2.1](https://github.com/alexdlaird/java-ngrok/compare/2.2.0...2.2.1) - 2023-09-13

### Fixed

- Bug
  in [Tunnel](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.2.1/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/protocol/Tunnel.html),
  a misspelled method was committed instead of using `setters` to update `publicUrl` and `proto`.

## [2.2.0](https://github.com/alexdlaird/java-ngrok/compare/2.1.0...2.2.0) - 2023-09-12

### Added

- Support for `labels`,
  so [`ngrok`'s Labeled Tunnel Configuration](https://ngrok.com/docs/agent/config/v2/#labeled-tunnel-configuration)
  is now supported, which enables basic support for [`ngrok`'s Edge](https://ngrok.com/docs/universal-gateway/edges/).
- `apiKey`
  to [`JavaNgrokConfig`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.2.0/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/conf/JavaNgrokConfig.html),
  which can be set so `java-ngrok` can interface with Edges `labels`.
- `id`
  to [Tunnel](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.2.0/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/protocol/Tunnel.html).
- `timeout`
  to [DefaultHttpClient](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.2.0/com.github.alexdlaird.ngrok/com/github/alexdlaird/http/DefaultHttpClient.html).
- Documentation improvements.
- Test improvements.

## [2.1.0](https://github.com/alexdlaird/java-ngrok/compare/2.0.0...2.1.0) - 2023-04-22

### Added

- Support for `oauth` configuration when
  building [CreateTunnel](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.1.0/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/protocol/CreateTunnel.html).
- Support for other new `ngrok`
  v3 [CreateTunnel](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.1.0/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/protocol/CreateTunnel.html)
  parameters,
  including `circuit_breaker`, `compression`, `mutual_tls_cas`, `proxy_proto`, `websocket_tcp_converter`, `terminate_at`, `request_header`, `response_header`, `ip_restrictions`,
  and `verify_webhook`.
- Documentation improvements.
- Test improvements.

## [2.0.0](https://github.com/alexdlaird/java-ngrok/compare/1.7.2...2.0.0) - 2023-04-12

### Changed

- Default installer behavior downloads `ngrok` v3 by default.
- Documentation updates.
- Test updates.

## [1.7.2](https://github.com/alexdlaird/java-ngrok/compare/1.7.1...1.7.2) - 2023-04-12

### Added

- Support for `basic_auth` parameter in `ngrok` v3.
- Documentation improvements.
- Test improvements.

## [1.7.1](https://github.com/alexdlaird/java-ngrok/compare/1.7.0...1.7.1) - 2023-04-11

### Fixed

- Inconsistencies with default installation of `ngrok` v2.

## [1.7.0](https://github.com/alexdlaird/java-ngrok/compare/1.6.2...1.7.0) - 2023-04-11

### Added

- `ngrokVersion`
  to [CreateTunnel](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/1.7.0/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/protocol/CreateTunnel.html)
  so parameter details can be properly inferred when necessary.

### Fixed

- Inconsistencies with default installation of `ngrok` v2.
- Documentation improvements.
- Test improvements.

## [1.6.2](https://github.com/alexdlaird/java-ngrok/compare/1.6.1...1.6.2) - 2023-03-08

### Fixed

- Issue where zip file paths were not normalized before they were read.
- Documentation improvements.
- Test improvements.

## [1.6.1](https://github.com/alexdlaird/java-ngrok/compare/1.6.0...1.6.1) - 2022-11-29

### Fixed

- Stability improvements.

## [1.6.0](https://github.com/alexdlaird/java-ngrok/compare/1.5.6...1.6.0) - 2022-11-28

### Added

- Support for [`ngrok` v3](https://ngrok.com/docs/guides/other-guides/upgrade-v2-v3/) (v2 is still used by default).
- Documentation and examples for using `java-ngrok` with `ngrok` v3.

### Fixed

- Stability improvements.
- Documentation improvements.
- Test improvements.

## [1.5.6](https://github.com/alexdlaird/java-ngrok/compare/1.5.5...1.5.6) - 2022-02-07

### Added

- Darwin 64-bit ARM support, as this was added to `ngrok` itself.

### Removed

- Darwin 386 support, as this was removed from `ngrok` itself.

## [1.5.5](https://github.com/alexdlaird/java-ngrok/compare/1.5.4...1.5.5) - 2021-09-28

### Fixed

- Full `ngrok` log line now passed to Java logger (was previously just the `msg` field).

## [1.5.4](https://github.com/alexdlaird/java-ngrok/compare/1.5.3...1.5.4) - 2021-09-21

### Added

- Test improvements.

### Fixed

- Parse issue from `ngrok` config file with `inspect` and `bind_tls` in `tunnels` definitions.

## [1.5.3](https://github.com/alexdlaird/java-ngrok/compare/1.5.0...1.5.3) - 2021-08-26

### Added

- Build improvements.
- Documentation improvements.

### Fixed

- If no `configPath` is set in `JavaNgrokConfig`, now properly defaults to `~/.ngrok2/ngrok.yml`.

## [1.5.0](https://github.com/alexdlaird/java-ngrok/compare/1.1.0...1.5.0) - 2021-08-25

### Added

- Shutdown hook, so running `ngrok` processes will clean themselves when the JVM shuts down.
- Build improvements.
- Documentation improvements.
- Test improvements.

### Removed

- Java 8 support.
- `reconnectSessionRetries`
  from [`JavaNgrokConfig`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/1.5.0/com/github/alexdlaird/ngrok/conf/JavaNgrokConfig.html),
  instead relying on `ngrok`'s own built-in retry mechanism on startup fails.

## [1.4.x](https://github.com/alexdlaird/java-ngrok/compare/1.4.19...1.4.x)

The `1.4.x` branch is where support for Java 8 of `java-ngrok` was maintained. It is available through
the `java8-ngrok` artifact
on [Maven Central](https://central.sonatype.com/artifact/com.github.alexdlaird/java8-ngrok).

## [1.4.19](https://github.com/alexdlaird/java-ngrok/compare/1.4.17...1.4.19) - 2025-05-16

### Added

- Documentation improvements.

### Fixed

- Noisy log around `ProcessMonitor` thread shutting down.

## [1.4.17](https://github.com/alexdlaird/java-ngrok/compare/1.4.16...1.4.17) - 2025-05-16

### Added

- Support for [MSYS2's `mingw`](https://www.msys2.org/) when installing the agent. Like `cygwin`, it is mapped to its corresponding Windows binary.
- Windows ARM 64-bit support.
- FreeBSD ARM support.
- Build and stability improvements.
- Documentation improvements.

### Fixed

- `NgrokV2CDNUrl` and `NgrokV3CDNUrl` both improperly defined the constant for Apple Silicon as `DARWIN_i386_arm`. It has been renamed to `DARWIN_x86_64_arm`.

## [1.4.16](https://github.com/alexdlaird/java-ngrok/compare/1.4.15...1.4.16) - 2025-05-11

### Added

- [`NgrokClient.api()`](https://javadoc.io/static/com.github.alexdlaird/java8-ngrok/1.4.16/com/github/alexdlaird/ngrok/NgrokClient.html#api-java.util.List-), allowing `api` commands to be executed from the agent.
- [Agent interface](https://javadoc.io/static/com.github.alexdlaird/java8-ngrok/1.4.16/com/github/alexdlaird/ngrok/agent/NgrokAgent.html), which provides access to Captured Requests and agent status.
- [`slf4j`](https://www.slf4j.org/) dependency, making logging improvements.
- Stability improvements.
- Documentation improvements.

### Changed

- `NgrokLog.lvl` now consistently parses `ERR`, `EROR`, and `CRIT` to `ERROR` instead of `SEVERE`.

### Fixed

- Refactored concurrency around `ProcessMonitor` and startup logging, as the old design could lead to race conditions when startup errored.
- `NgrokLog` string may contain a malformed or empty `lvl`. Default is now `NOTSET` in such cases.

## [1.4.15](https://github.com/alexdlaird/java-ngrok/compare/1.4.14...1.4.15) - 2025-05-07

### Added

- Support for `pooling_enabled` in [`CreateTunnel`](https://javadoc.io/static/com.github.alexdlaird/java8-ngrok/1.4.15/com/github/alexdlaird/ngrok/protocol/CreateTunnel.Builder.html#withPoolingEnabled-java.lang.Boolean-), which allows for the use of `https` and `tls` [Internal Endpoints](https://ngrok.com/docs/universal-gateway/internal-endpoints/).
- Stability improvements around concurrency, process execution.
- Documentation improvements.
- Test improvements.

## [1.4.14](https://github.com/alexdlaird/java-ngrok/compare/1.4.13...1.4.14) - 2025-04-23

### Added

- Rebased the `1.4.x` branch with Java 8-compatible changes from `main` between `2.2.15` and `2.3.9`. See
  the [`main` changelog](https://github.com/alexdlaird/java-ngrok/blob/main/CHANGELOG.md#230---2024-04-08) for full
  details.
- Biggest changes include support for `ngrok`'s v3 config (including `traffic_policy`), support for latest fields in tunnel definitions, `ip_restriction`
- Deprecation warning when using Labeled Tunnels, since `ngrok` Edges will be sunset December 31st, 2025. See [this issue](https://github.com/alexdlaird/java-ngrok/issues/158) for more details.

### Fixed

- `ip_restriction` was incorrectly plural, config [and class](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/2.3.0/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/protocol/TunnelIPRestriction.html)
  are now singular to match `ngrok`.

## [1.4.13](https://github.com/alexdlaird/java-ngrok/compare/1.4.12...1.4.13) - 2024-03-08

### Added

- `obj` parsing
  in [`NgrokLog`](https://javadoc.io/static/com.github.alexdlaird/java8-ngrok/1.4.13/com/github/alexdlaird/ngrok/process/NgrokLog.html).
- `throws` for unchecked exceptions to documentation.
- Documentation improvements.
- Build and stability improvements.

### Fixed

- Minor bugs, including a typo in the name
  of [JavaNgrokConfig.getStartupTimeout()](https://javadoc.io/static/com.github.alexdlaird/java8-ngrok/1.4.13/com/github/alexdlaird/ngrok/conf/JavaNgrokConfig.html#getStartupTimeout--).

## [1.4.12](https://github.com/alexdlaird/java-ngrok/compare/1.4.11...1.4.12) - 2024-03-06

### Added

- Build and stability improvements.
- Documentation and style improvements.

## [1.4.11](https://github.com/alexdlaird/java-ngrok/compare/1.4.8...1.4.11) - 2024-02-26

### Added

- Build improvements.
- Documentation improvements.

## [1.4.8](https://github.com/alexdlaird/java-ngrok/compare/1.4.6...1.4.8) - 2024-02-19

### Added

- Rebased the `1.4.x` branch with Java 8-compatible changes from `main` between `1.6.0` and `2.2.12`. See
  the [`main changelog`](https://github.com/alexdlaird/java-ngrok/blob/main/CHANGELOG.md#160---2022-11-28) for full
  details.
- Biggest change is support for `ngrok` v3—including `labels`—and that `ngrok` v3 is installed by default.

### Removed

- `NgrokProcess.stopMonitorThread()`, use `NgrokProcess.ProcessMonitor.stop()` instead.

## [1.4.6](https://github.com/alexdlaird/java-ngrok/compare/1.4.5...1.4.6) - 2024-02-15

### Added

- If a value for `authToken` is not set
  in [`JavaNgrokConfig`](https://javadoc.io/static/com.github.alexdlaird/java8-ngrok/1.4.6/com/github/alexdlaird/ngrok/conf/JavaNgrokConfig.html),
  it will attempt to use the environment variable `NGROK_AUTHTOKEN` if it is set.
- Test improvements, suite now respects `NGROK_AUTHTOKEN` for all necessary tests (skipped if not set, rather than tests
  failing).
- Build and stability improvements.

### Fixed

- Issue where zip file paths were not normalized before they were read.

## [1.4.5](https://github.com/alexdlaird/java-ngrok/compare/1.4.4...1.4.5) - 2022-02-07

### Added

- Darwin 64-bit ARM support, as this was added to `ngrok` itself.

### Removed

- Darwin 386 support, as this was removed from `ngrok` itself.

## [1.4.4](https://github.com/alexdlaird/java-ngrok/compare/1.4.3...1.4.4) - 2021-09-28

### Added

- Test improvements.

### Fixed

- Parse issue from `ngrok` config file with `inspect` and `bind_tls` in `tunnels` definitions.
- Full `ngrok` log line now passed to Java logger (was previously just the `msg` field).

## [1.4.3](https://github.com/alexdlaird/java-ngrok/compare/1.4.0...1.4.3) - 2021-08-26

### Added

- Build improvements.
- Documentation improvements.

### Fixed

- If no `configPath` is set in `JavaNgrokConfig`, now properly defaults to `~/.ngrok2/ngrok.yml`.

## [1.4.0](https://github.com/alexdlaird/java-ngrok/compare/1.1.0...1.4.0) - 2021-08-25

### Added

- Java 8 support, which will not be actively maintained. It is available through the `java8-ngrok` artifact
  on [Maven Central](https://central.sonatype.com/artifact/com.github.alexdlaird/java8-ngrok).

## [1.1.0](https://github.com/alexdlaird/java-ngrok/compare/1.0.0...1.1.0) - 2021-08-20

### Added

- Support
  for [`ngrok`'s tunnel definitions](https://ngrok.com/docs/agent/config/v2/#tunnel-configurations)
  when
  calling [NgrokClient.connect()](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/1.1.0/com/github/alexdlaird/ngrok/NgrokClient.html#connect(com.github.alexdlaird.ngrok.protocol.CreateTunnel)).
  If a tunnel definition in `ngrok`'s config matches the given `name`, it will be used to start the tunnel.
- Support for
  a [`ngrok` tunnel definition](https://ngrok.com/docs/agent/config/v2/#tunnel-configurations)
  named "java-ngrok-default" when
  calling [NgrokClient.connect()](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/1.1.0/com/github/alexdlaird/ngrok/NgrokClient.html#connect(com.github.alexdlaird.ngrok.protocol.CreateTunnel)).
  When `name` is `None` and a "java-ngrok-default" tunnel definition exists it `ngrok`'s config, it will be used.
- `refreshMetrics()`
  to [NgrokClient](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/1.1.0/com/github/alexdlaird/ngrok/NgrokClient.html).
- Documentation improvements.
- Test improvements.

### Fixed

- `JavaNgrokConfig.keepMonitoring` is now `true` by default (was already documented this way, so fixing bug where it
  defaulted it `false`).
- Serialization of `ngrok`'s `ngrok.yml` (can now properly parsed nested YAML to a nested Map).
- `Tunnel.Metrics` `rate` and `p` fields are now `double`s rather than `int`s, so they serialize correctly when
  populated.

## [1.0.0](https://github.com/alexdlaird/java-ngrok/releases/tag/1.0.0) - 2021-08-18

- First stable release of `java-ngrok`.
