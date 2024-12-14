<p align="center"><img alt="java-ngrok - a Java wrapper for ngrok" src="https://github.com/alexdlaird/java-ngrok/raw/main/logo.png" /></p>

[![Version](https://img.shields.io/maven-central/v/com.github.alexdlaird/java-ngrok)](https://maven-badges.herokuapp.com/maven-central/com.github.alexdlaird/java-ngrok/)
[![Java Versions](https://img.shields.io/badge/Java-11+-blue)](https://maven-badges.herokuapp.com/maven-central/com.github.alexdlaird/java-ngrok/)
[![Coverage](https://img.shields.io/codecov/c/github/alexdlaird/java-ngrok)](https://codecov.io/gh/alexdlaird/java-ngrok)
[![Build](https://img.shields.io/github/actions/workflow/status/alexdlaird/java-ngrok/build.yml)](https://github.com/alexdlaird/java-ngrok/actions/workflows/build.yml)
[![Code Quality](https://api.codacy.com/project/badge/Grade/940d16178f8f4e8abfcf9bf2873894b3)](https://app.codacy.com/gh/alexdlaird/java-ngrok?utm_source=github.com&utm_medium=referral&utm_content=alexdlaird/java-ngrok&utm_campaign=Badge_Grade)
[![Docs](https://img.shields.io/badge/docs-passing-brightgreen)](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok)
[![GitHub License](https://img.shields.io/github/license/alexdlaird/java-ngrok)](https://github.com/alexdlaird/java-ngrok/blob/main/LICENSE)

`java-ngrok` is a Java wrapper for `ngrok` that manages its own binary, making `ngrok` available via a convenient Java
API.

[`ngrok`](https://ngrok.com) is a reverse proxy tool that opens secure tunnels from public URLs to localhost, perfect
for
exposing local web servers, building webhook integrations, enabling SSH access, testing chatbots, demoing from your own
machine, and more, and its made even more powerful with native Java integration through `java-ngrok`.

## Installation

`java-ngrok` is available
on [Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.alexdlaird/java-ngrok/).

If we want `ngrok` to be available from the command
line, [pyngrok](https://pyngrok.readthedocs.io/en/latest/#installation)
can be installed using `pip` to manage that for us.

## Basic Usage

### Open a Tunnel

All `ngrok` functionality is available through
the [`NgrokClient`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/latest/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/NgrokClient.html).
To open a tunnel, use
the [`connect`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/latest/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/NgrokClient.html#connect(com.github.alexdlaird.ngrok.protocol.CreateTunnel))
method, which returns a `Tunnel`, and this returned object has a reference to the public URL generated by `ngrok`, which
can be retrieved
with [`getPublicUrl()`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/latest/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/protocol/Tunnel.html#getPublicUrl()).

```java
final NgrokClient ngrokClient = new NgrokClient.Builder().build();

// Open a HTTP tunnel on the default port 80
// <Tunnel: "https://<public_sub>.ngrok.io" -> "http://localhost:80">
final Tunnel httpTunnel = ngrokClient.connect();

// Open a SSH tunnel
// <Tunnel: "tcp://0.tcp.ngrok.io:12345" -> "localhost:22">
final CreateTunnel sshCreateTunnel = new CreateTunnel.Builder()
        .withProto(Proto.TCP)
        .withAddr(22)
        .build();
final Tunnel sshTunnel = ngrokClient.connect(sshCreateTunnel);

// Open a named tunnel from the config file
final CreateTunnel createNamedTunnel = new CreateTunnel.Builder()
        .withName("my_tunnel_name")
        .build();
final Tunnel namedTunnel = ngrokClient.connect(createNamedTunnel);
```

The [`connect`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/latest/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/NgrokClient.html#connect(com.github.alexdlaird.ngrok.protocol.CreateTunnel))
method can also take
a [`CreateTunnel`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/latest/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/protocol/CreateTunnel.html) (
which can be built
through [its Builder](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/latest/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/protocol/CreateTunnel.Builder.html))
that allows us to pass additional properties that
are supported by `ngrok`, [as documented here](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/latest/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/NgrokClient.html#tunnel-configurations).

### `ngrok`'s Edge
To use [`ngrok`'s Edge](https://ngrok.com/docs/network-edge/edges/) with `java-ngrok`, first
[configure an Edge](https://dashboard.ngrok.com/edges) [on ngrok's dashboard](https://dashboard.ngrok.com/edges) (with
at least one Endpoint mapped to the Edge), and define a labeled tunnel in
[the `ngrok` config file](https://ngrok.com/docs/agent/config/v2/#define-two-labeled-tunnels) that points to the Edge.

```yaml
tunnels:
  some-edge-tunnel:
    labels:
      - edge=my_edge_id
    addr: http://localhost:80
```

To start a labeled tunnel in `java-ngrok`, set [withName(String)](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/latest/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/protocol/CreateTunnel.Builder.html#withName(java.lang.String)).

```java
final NgrokClient ngrokClient = new NgrokClient.Builder().build();

// Open a named tunnel from the config file
final CreateTunnel createNamedTunnel = new CreateTunnel.Builder()
        .withName("some-edge-tunnel")
        .build();
final Tunnel namedTunnel = ngrokClient.connect(createNamedTunnel);
```

Once an Edge tunnel is started, it can be managed through [`ngrok`'s dashboard](https://dashboard.ngrok.com/edges).

### Command Line Usage

Assuming we have also installed [pyngrok](https://pyngrok.readthedocs.io/en/latest/#installation), all features of `ngrok` are available
on the command line.

```sh
ngrok http 80
```

For details on how to fully leverage `ngrok` from the command line,
see [`ngrok`'s official documentation](https://ngrok.com/docs/agent/cli/).

## Documentation

For more advanced usage, `java-ngrok`'s official documentation is available
at [https://javadoc.io/doc/com.github.alexdlaird/java-ngrok](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok).

### `ngrok` Version Compatibility

`java-ngrok` is compatible with `ngrok` v2 and v3, but by default it will install v3. To install v2 instead,
set the version
with [`JavaNgrokConfig.Builder.withNgrokVersion(NgrokVersion)`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/latest/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/conf/JavaNgrokConfig.Builder.html#withNgrokVersion(com.github.alexdlaird.ngrok.installer.NgrokVersion))
and [`CreateTunnel.Builder.withNgrokVersion(NgrokVersion)`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/latest/com.github.alexdlaird.ngrok/com/github/alexdlaird/ngrok/protocol/CreateTunnel.Builder.html#withNgrokVersion(com.github.alexdlaird.ngrok.installer.NgrokVersion)).

### Java 8

[![Version](https://img.shields.io/maven-central/v/com.github.alexdlaird/java8-ngrok)](https://maven-badges.herokuapp.com/maven-central/com.github.alexdlaird/java8-ngrok/)
[![Java Versions](https://img.shields.io/badge/Java-8+-blue)](https://maven-badges.herokuapp.com/maven-central/com.github.alexdlaird/java8-ngrok/)
[![Coverage](https://img.shields.io/codecov/c/github/alexdlaird/java-ngrok/1.4.x)](https://codecov.io/gh/alexdlaird/java-ngrok/tree/1.4.x)
[![Build](https://img.shields.io/github/actions/workflow/status/alexdlaird/java-ngrok/build.yml?branch=1.4.x)](https://github.com/alexdlaird/java-ngrok/actions/workflows/build.yml?query=branch%3A1.4.x)
[![Docs](https://img.shields.io/badge/docs-passing-brightgreen)](https://javadoc.io/doc/com.github.alexdlaird/java8-ngrok)
[![GitHub License](https://img.shields.io/github/license/alexdlaird/java-ngrok)](https://github.com/alexdlaird/java-ngrok/blob/main/LICENSE)

Java 8 support is not actively maintained, but on a periodic basis, `main` may be rebased in to the `1.4.x`
branch, where a compatible build of this project exists for Java 8. To use it, include the `java8-ngrok`
dependency from [Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.alexdlaird/java8-ngrok/).

For more details on what differs in the `java8-ngrok` dependency,
see [the "Java 8" section of the docs](https://javadoc.io/doc/com.github.alexdlaird/java8-ngrok/latest/index.html#java8).

## Contributing

If you would like to get involved, be sure to review
the [Contribution Guide](https://github.com/alexdlaird/java-ngrok/blob/main/CONTRIBUTING.md).

Want to contribute financially? If you've found `java-ngrok`
useful, [sponsorship](https://github.com/sponsors/alexdlaird)
would also be greatly appreciated!
