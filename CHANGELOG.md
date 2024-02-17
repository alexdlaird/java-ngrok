# Changelog
All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

**Note:** `1.4.x` is the branch of `java-ngrok` that passively maintains Java 8 support. It is available through the `java8-ngrok` artifact on [Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.alexdlaird/java8-ngrok/).

## [1.4.7](https://github.com/alexdlaird/java-ngrok/compare/1.4.6...1.4.7) - TBD
### Added
- Rebased the `1.4.x` branch with Java 8-compatible changes from `main` between `1.6.0` and `2.2.10`. See the [`main changelog`](https://github.com/alexdlaird/java-ngrok/blob/main/CHANGELOG.md) for full details.
- Biggest change is support for `ngrok` v3—including `labels`—and that `ngrok` v3 is installed by default.

## [1.4.6](https://github.com/alexdlaird/java-ngrok/compare/1.4.5...1.4.6) - 2024-02-15

### Added
- If a value for `authToken` is not set in [`JavaNgrokConfig`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/1.4.6/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/conf/JavaNgrokConfig.html), it will attempt to use the environment variable `NGROK_AUTHTOKEN` if it is set.
- Test improvements, suite now respects `NGROK_AUTHTOKEN` for all necessary tests (skipped if not set, rather than tests failing).
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
- Java 8 support, which will not be actively maintained. It is available through the `java8-ngrok` artifact on [Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.alexdlaird/java8-ngrok/).

## [1.1.0](https://github.com/alexdlaird/java-ngrok/compare/1.0.0...1.1.0) - 2021-08-20
### Added
- Support for [`ngrok`'s tunnel definitions](https://ngrok.com/docs/secure-tunnels/ngrok-agent/reference/config/#tunnel-definitions) when calling [NgrokClient.connect()](https://javadoc.io/static/com.github.alexdlaird/java-ngrok/1.1.0/com/github/alexdlaird/ngrok/NgrokClient.html#connect(com.github.alexdlaird.ngrok.protocol.CreateTunnel)). If a tunnel definition in `ngrok`'s config matches the given `name`, it will be used to start the tunnel.
- Support for a [`ngrok` tunnel definition](https://ngrok.com/docs/secure-tunnels/ngrok-agent/reference/config/#tunnel-definitions) named "java-ngrok-default" when calling [NgrokClient.connect()](https://javadoc.io/static/com.github.alexdlaird/java-ngrok/1.1.0/com/github/alexdlaird/ngrok/NgrokClient.html#connect(com.github.alexdlaird.ngrok.protocol.CreateTunnel)). When `name` is `None` and a "java-ngrok-default" tunnel definition exists it `ngrok`'s config, it will be used.
- `refreshMetrics()` to [NgrokClient](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/1.1.0/com/github/alexdlaird/ngrok/NgrokClient.html).
- Documentation improvements.
- Test improvements.
 
### Fixed
- `JavaNgrokConfig.keepMonitoring` is now `true` by default (was already documented this way, so fixing bug where it defaulted it `false`).
- Serialization of `ngrok`'s `ngrok.yml` (can now properly parsed nested YAML to a nested Map).
- `Tunnel.Metrics` `rate` and `p` fields are now `double`s rather than `int`s, so they serialize correctly when populated.

## [1.0.0](https://github.com/alexdlaird/java-ngrok/releases/tag/1.0.0) - 2021-08-18
- First stable release of `java-ngrok`.
