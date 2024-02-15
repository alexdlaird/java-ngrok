# Changelog
All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased](https://github.com/alexdlaird/java-ngrok/compare/1.4.6...java8-ngrok)

## [1.4.6](https://github.com/alexdlaird/java-ngrok/compare/1.4.5...1.4.6) - 2024-02-15
### Added
- If a value for `authToken` is not set in [`JavaNgrokConfig`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/1.4.6/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/conf/JavaNgrokConfig.html), it will attempt to use the environment variable `NGROK_AUTHTOKEN` if it is set.
- Test improvements, suite now respects `NGROK_AUTHTOKEN` for all necessary tests (skipped if not set, rather than tests failing).

### Fixed
- Issue where zip file paths were not normalized before they were read.

## [1.4.5](https://github.com/alexdlaird/java-ngrok/compare/1.4.4...1.4.5) - 2022-02-07
**Note:** 1.4.x is a special release that added Java 8 support, which will not be actively maintained. It is available through the `java8-ngrok` artifact on [Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.alexdlaird/java8-ngrok/).

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

## [1.4.0](https://github.com/alexdlaird/java-ngrok/releases/tag/1.4.0) - 2021-08-25
### Added
- Java 8 support, which will not be actively maintained. It is available through the `java8-ngrok` artifact on [Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.alexdlaird/java8-ngrok/).
