# Changelog
All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased](https://github.com/alexdlaird/java-ngrok/compare/1.1.0...HEAD)
- Build improvements.
- Documentation improvements.
- Test improvements.

## [1.1.0](https://github.com/alexdlaird/pyngrok/compare/1.0.0...1.1.0) - 2021-08-20
### Added
- Support for [`ngrok`'s tunnel definitions](https://ngrok.com/docs#tunnel-definitions) when calling [NgrokClient.connect()](https://javadoc.io/static/com.github.alexdlaird/java-ngrok/1.1.0/com/github/alexdlaird/ngrok/NgrokClient.html#connect(com.github.alexdlaird.ngrok.protocol.CreateTunnel)). If a tunnel definition in `ngrok`'s config matches the given `name`, it will be used to start the tunnel.
- Support for a [`ngrok` tunnel definition](https://ngrok.com/docs#tunnel-definitions) named "java-ngrok-default" when calling [NgrokClient.connect()](https://javadoc.io/static/com.github.alexdlaird/java-ngrok/1.1.0/com/github/alexdlaird/ngrok/NgrokClient.html#connect(com.github.alexdlaird.ngrok.protocol.CreateTunnel)). When `name` is `None` and a "java-ngrok-default" tunnel definition exists it `ngrok`'s config, it will be used.
- `refreshMetrics()` to [NgrokClient](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/1.1.0/com/github/alexdlaird/ngrok/NgrokClient.html).
- Documentation improvements.
- Test improvements.
 
### Fixed
- `JavaNgrokConfig.keepMonitoring` is now `true` by default (was already documented this way, so fixing bug where it defaulted it `false`).
- Serialization of `ngrok`'s `config.yml` (can now properly parsed nested YAML to a nested Map).
- `Tunnel.Metrics` `rate` and `p` fields are now `double`s rather than `int`s, so they serialize correctly when populated.

## [1.0.0](https://github.com/alexdlaird/java-ngrok/releases/tag/1.0.0) - 2021-08-18
- First stable release of `java-ngrok`.
