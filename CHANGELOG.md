# Changelog
All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased](https://github.com/alexdlaird/java-ngrok/compare/2.0.0...HEAD)
### Added
- Support for `oauth` configuration when building [CreateTunnel](https://javadoc.io/static/com.github.alexdlaird/java-ngrok/2.1.0/com/github/alexdlaird/ngrok/protocol/CreateTunnel.html).
- Support for other new `ngrok` v3 [CreateTunnel](https://javadoc.io/static/com.github.alexdlaird/java-ngrok/2.1.0/com/github/alexdlaird/ngrok/protocol/CreateTunnel.html) parameters, including `circuit_breaker`, `compression`, `mutual_tls_cas`, `proxy_proto`, `websocket_tcp_converter`, `terminate_at`, `request_header`, `response_header`, `ip_restrictions`, and `verify_webhook`.
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
- `ngrokVersion` to [CreateTunnel](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/1.7.0/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/protocol/CreateTunnel.Builder.html) so parameter details can be properly inferred when necessary.

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
- Support for [`ngrok` v3](https://ngrok.com/docs/guides/upgrade-v2-v3) (v2 is still used by default).
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
- `reconnectSessionRetries` from [`JavaNgrokConfig`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/1.5.0/com/github/alexdlaird/ngrok/conf/JavaNgrokConfig.html), instead relying on `ngrok`'s own built-in retry mechanism on startup fails.

## [1.1.0](https://github.com/alexdlaird/java-ngrok/compare/1.0.0...1.1.0) - 2021-08-20
### Added
- Support for [`ngrok`'s tunnel definitions](https://ngrok.com/docs#tunnel-definitions) when calling [NgrokClient.connect()](https://javadoc.io/static/com.github.alexdlaird/java-ngrok/1.1.0/com/github/alexdlaird/ngrok/NgrokClient.html#connect(com.github.alexdlaird.ngrok.protocol.CreateTunnel)). If a tunnel definition in `ngrok`'s config matches the given `name`, it will be used to start the tunnel.
- Support for a [`ngrok` tunnel definition](https://ngrok.com/docs#tunnel-definitions) named "java-ngrok-default" when calling [NgrokClient.connect()](https://javadoc.io/static/com.github.alexdlaird/java-ngrok/1.1.0/com/github/alexdlaird/ngrok/NgrokClient.html#connect(com.github.alexdlaird.ngrok.protocol.CreateTunnel)). When `name` is `None` and a "java-ngrok-default" tunnel definition exists it `ngrok`'s config, it will be used.
- `refreshMetrics()` to [NgrokClient](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/1.1.0/com/github/alexdlaird/ngrok/NgrokClient.html).
- Documentation improvements.
- Test improvements.
 
### Fixed
- `JavaNgrokConfig.keepMonitoring` is now `true` by default (was already documented this way, so fixing bug where it defaulted it `false`).
- Serialization of `ngrok`'s `ngrok.yml` (can now properly parsed nested YAML to a nested Map).
- `Tunnel.Metrics` `rate` and `p` fields are now `double`s rather than `int`s, so they serialize correctly when populated.

## [1.0.0](https://github.com/alexdlaird/java-ngrok/releases/tag/1.0.0) - 2021-08-18
- First stable release of `java-ngrok`.
