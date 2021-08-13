[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.alexdlaird/java-ngrok/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.alexdlaird/java-ngrok/)
[![CI/CD](https://github.com/alexdlaird/java-ngrok/workflows/CI/CD/badge.svg)](https://github.com/alexdlaird/java-ngrok/actions?query=workflow%3ACI%2FCD)
[![Codecov](https://codecov.io/gh/alexdlaird/java-ngrok/branch/main/graph/badge.svg)](https://codecov.io/gh/alexdlaird/java-ngrok)
![GitHub License](https://img.shields.io/github/license/alexdlaird/java-ngrok)
[![Tweet](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/intent/tweet?text=Check+out+java-ngrok%2C+a+Java+wrapper+for+%23ngrok+that+lets+you+programmatically+open+secure+%23tunnels+to+local+web+servers%2C+build+%23webhook+integrations%2C+enable+SSH+access%2C+test+chatbots%2C+demo+from+your+own+machine%2C+and+more.%0D%0A%0D%0A&url=https://github.com/alexdlaird/java-ngrok&via=alexdlaird)

# java-ngrok - a Java wrapper for ngrok

`java-ngrok` is a Java wrapper for `ngrok` that manages its own binary, making `ngrok` available via a convenient Java
API.

[ngrok](https://ngrok.com) is a reverse proxy tool that opens secure tunnels from public URLs to localhost, perfect for
exposing local web servers, building webhook integrations, enabling SSH access, testing chatbots, demoing from your own
machine, and more, and its made even more powerful with native Java integration through `java-ngrok`.

## Installation

`java-ngrok` is available
on [Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.alexdlaird/java-ngrok/).

#### Maven

```xml

<dependency>
    <groupId>com.github.alexdlaird</groupId>
    <artifactId>java-ngrok</artifactId>
    <version>0.1.0</version>
</dependency>
```

#### Gradle

```groovy
implementation 'com.github.alexdlaird:java-ngrok:0.1.0'
```

If we want `ngrok` to be available from the command line, [pyngrok](https://github.com/alexdlaird/pyngrok) can be
installed using `pip` to manage that for us.

## Basic Usage

To open a tunnel, use
the [`connect`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/latest/com/github/alexdlaird/ngrok/NgrokClient.html)
method, which returns a `Tunnel`, and this returned object has a reference to the public URL generated by `ngrok`, which
can be access with `getPublicUrl()`.

```java
import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnel;

import java.io.IOException;

public class MyClass {

    public void myMethod() throws IOException, InterruptedException {
        final NgrokClient ngrokClient = new NgrokClient.Builder().build();

        // Open a HTTP tunnel on the default port 80
        // <Tunnel: "http://<public_sub>.ngrok.io" -> "http://localhost:80">
        final Tunnel httpTunnel = ngrokClient.connect();

        // Open a SSH tunnel
        // <Tunnel: "tcp://0.tcp.ngrok.io:12345" -> "localhost:22">
        final CreateTunnel sshCreateTunnel = new CreateTunnel.Builder().withProto("tcp").withAddr(22).build();
        final Tunnel sshTunnel = ngrokClient.connect(sshCreateTunnel);
    }
}
```

The [`connect`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/latest/com/github/alexdlaird/ngrok/NgrokClient.html)
method
takes [`CreateTunnel`](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok/latest/com/github/alexdlaird/ngrok/protocol/CreateTunnel.html)
as well, which has a `Builder` that allows us to pass additional properties that
are [supported by ngrok](https://ngrok.com/docs#tunnel-definitions).

Assuming we have also installed [pyngrok](https://github.com/alexdlaird/pyngrok), all features of `ngrok` are available
on the command line.

```sh
ngrok http 80
```

For details on how to fully leverage `ngrok` from the command line,
see [ngrok's official documentation](https://ngrok.com/docs).

## Documentation

For more advanced usage, `java-ngrok`'s official documentation is available
at [https://javadoc.io/doc/com.github.alexdlaird/java-ngrok](https://javadoc.io/doc/com.github.alexdlaird/java-ngrok).

## Contributing

If you would like to get involved, be sure to review
the [Contribution Guide](https://github.com/alexdlaird/java-ngrok/blob/main/CONTRIBUTING.md).

Want to contribute financially? If you've found `java-ngrok` useful, [a donation](https://www.paypal.me/alexdlaird)
would also be greatly appreciated!
