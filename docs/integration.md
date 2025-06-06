# Integration Examples

## Spring
[![Clone on GitHub](https://img.shields.io/badge/Clone_on_GitHub-black?logo=github)](https://github.com/alexdlaird/java-ngrok-example-spring)

This example project is also setup to [show Docker usage](https://alexdlaird.github.io/java-ngrok/integration/#docker).

### Configuration

Create a [`NgrokConfiguration`](https://github.com/alexdlaird/java-ngrok-example-spring/blob/main/src/main/java/com/github/alexdlaird/ngrok/example/spring/conf/NgrokConfiguration.java)
class that lets you use the config to enable `ngrok` and pass it some useful parameters.

```java
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "ngrok")
public class NgrokConfiguration {
    private boolean enabled;

    private String publicUrl;
}
```

And pass parameters to your Spring application through
[your config file](https://github.com/alexdlaird/java-ngrok-example-spring/blob/main/src/main/resources/application.properties):

```yaml
spring.profiles.active=dev
ngrok.enabled=true
```

### Application Integration

If `ngrok.enabled` config flag is set, `java-ngrok` will be initialized when Spring is booting. An easy way to do
this is by creating [a `Component` with an `EventListener`](https://github.com/alexdlaird/java-ngrok-example-spring/blob/main/src/main/java/com/github/alexdlaird/ngrok/example/spring/NgrokWebServerEventListener.java)
that is executed when `WebServerInitializedEvent` is emitted.

```java
@Component
@Profile("dev")
@Slf4j
public class NgrokWebServerEventListener {

    @Autowired
    private NgrokConfiguration ngrokConfiguration;

    @EventListener
    public void onApplicationEvent(final WebServerInitializedEvent event) {
        // Only install and initialize ngrok if we're actually using it
        if (ngrokConfiguration.isEnabled()) {
            final NgrokClient ngrokClient = new NgrokClient.Builder()
                    .build();

            final int port = event.getWebServer().getPort();

            final CreateTunnel createTunnel = new CreateTunnel.Builder()
                    .withAddr(port)
                    .build();
            final Tunnel tunnel = ngrokClient.connect(createTunnel);
            final String publicUrl = tunnel.getPublicUrl();

            log.info(String.format("ngrok tunnel \"%s\" -> \"http://127.0.0.1:%d\"",publicUrl, port));

            // Update any base URLs or webhooks to use the public ngrok URL
            ngrokConfiguration.setPublicUrl(publicUrl);
            initWebhooks(publicUrl);
        }
    }

    private void initWebhooks(final String publicUrl) {
        // ... Implement updates necessary so inbound traffic uses the public-facing ngrok URL
    }
}
```

Now Spring can be started by the usual means, setting `ngrok.enabled` in the config to open a tunnel.

1. Run `./gradlew build` to build the application
1. Start application with `java -jar build/libs/java-ngrok-example-spring-1.0.0-SNAPSHOT.jar`
1. Check the logs for the `ngrok` tunnel's public URL, which should tunnel to  `http://localhost:8080`


## Dropwizard
[![Clone on GitHub](https://img.shields.io/badge/Clone_on_GitHub-black?logo=github)](https://github.com/alexdlaird/java-ngrok-example-dropwizard)

### Configuration

Create
a [`NgrokConfiguration`](https://github.com/alexdlaird/java-ngrok-example-dropwizard/blob/main/src/main/java/com/github/alexdlaird/conf/NgrokConfiguration.java)
class that lets you use the config to enable `ngrok` and pass it some useful parameters.

```java
public class NgrokConfiguration {
    @JsonProperty
    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }
}
```

Then wire this class as a `JsonProperty`
to [the Dropwizard Configuration for your Application](https://www.dropwizard.io/en/latest/getting-started.html#creating-a-configuration-class).

```java
public class JavaNgrokExampleDropwizardConfiguration extends Configuration {
    @JsonProperty
    private String environment = "dev";

    @JsonProperty("ngrok")
    private NgrokConfiguration ngrokConfiguration;

    public String getEnvironment() {
        return environment;
    }

    public NgrokConfiguration getNgrokConfiguration() {
        return ngrokConfiguration;
    }
}
```

And pass parameters to your Dropwizard application through
[your config file](https://github.com/alexdlaird/java-ngrok-example-dropwizard/blob/main/config.yml):

```yaml
ngrok:
  enabled: true
```

Now Dropwizard can be started by the usual means, setting `ngrok.enabled` in the config to open a tunnel.

1. Run `mvn install` to build the application
1. Start application with `java -jar target/java-ngrok-example-dropwizard-1.0.0-SNAPSHOT.jar server src/main/resources/config.yml`
1. Check the logs for the `ngrok` tunnel's public URL, which should tunnel to  `http://localhost:8080`

### Application Integration

If `ngrok.enabled` config flag is set, `java-ngrok` will be initialized when Dropwizard is booting. An easy place to do
this is in the `run()` method of [the Application](https://github.com/alexdlaird/java-ngrok-example-dropwizard/blob/main/src/main/java/com/github/alexdlaird/JavaNgrokExampleDropwizardApplication.java).

```java
public class JavaNgrokExampleDropwizardApplication extends Application<JavaNgrokExampleDropwizardConfiguration> {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(JavaNgrokExampleDropwizardApplication.class));

    @Override
    public void run(final JavaNgrokExampleDropwizardConfiguration configuration,
                    final Environment environment) {
        // Only install and initialize ngrok if we're actually using it
        if (configuration.getEnvironment().equals("dev") &&
                configuration.getNgrokConfiguration().isEnabled()) {
            final NgrokClient ngrokClient = new NgrokClient.Builder()
                    .build();

            final int port = getPort(configuration);

            final CreateTunnel createTunnel = new CreateTunnel.Builder()
                    .withAddr(port)
                    .build();
            final Tunnel tunnel = ngrokClient.connect(createTunnel);
            final String publicUrl = tunnel.getPublicUrl();

            LOGGER.info(String.format("ngrok tunnel \"%s\" -> \"http://127.0.0.1:%d\"", publicUrl, port));

            // Update any base URLs or webhooks to use the public ngrok URL
            configuration.setPublicUrl(publicUrl);
            initWebhooks(publicUrl);
        }
    }

    private int getPort(JavaNgrokExampleDropwizardConfiguration configuration) {
        final Stream<ConnectorFactory> connectors = configuration.getServerFactory() instanceof DefaultServerFactory
                ? ((DefaultServerFactory) configuration.getServerFactory()).getApplicationConnectors().stream()
                : Stream.of((SimpleServerFactory) configuration.getServerFactory()).map(SimpleServerFactory::getConnector);

        return connectors.filter(connector -> connector.getClass().isAssignableFrom(HttpConnectorFactory.class))
                .map(connector -> (HttpConnectorFactory) connector)
                .mapToInt(HttpConnectorFactory::getPort)
                .findFirst()
                .orElseThrow(IllegalStateException::new);
    }

    private void initWebhooks(final String publicUrl) {
        // ... Implement updates necessary so inbound traffic uses the public-facing ngrok URL
    }

    // ... Implement the rest of your Dropwizard application
}
```

## Play (Scala)
[![Clone on GitHub](https://img.shields.io/badge/Clone_on_GitHub-black?logo=github)](https://github.com/alexdlaird/java-ngrok-example-play)

### Application Integration

Register an eager `Singleton` in [the app's base `Module`](https://github.com/alexdlaird/java-ngrok-example-play/blob/main/app/Module.scala).

```scala
class Module extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[NgrokApplicationLifecycle]).asEagerSingleton()
  }
}
```

Then create a [`NgrokApplicatinLifecycle` class](https://github.com/alexdlaird/java-ngrok-example-play/blob/main/app/services/NgrokApplicationLifecycle.scala).
If `ngrok.enabled` config flag is set, `java-ngrok` will be initialized when Play is booting in a `dev` environment.

```scala
@Singleton
class NgrokApplicationLifecycle @Inject()(config: Configuration, lifecycle: ApplicationLifecycle) {
  private val environment: String = config.getOptional[String]("environment").getOrElse("production")
  private val ngrokEnabled: Boolean = config.getOptional[Boolean]("ngrok.enabled").getOrElse(false)

  // Only install and initialize ngrok if we're actually using it
  if (environment.equals("dev") && ngrokEnabled) {
    val ngrokClient: NgrokClient = new NgrokClient.Builder()
      .build

    val port: Int = config.getOptional[Int]("http.port").getOrElse(9000)

    val createTunnel: CreateTunnel = new CreateTunnel.Builder()
      .withAddr(port)
      .build
    val tunnel: Tunnel = ngrokClient.connect(createTunnel)

    println(s" * ngrok tunnel \"${tunnel.getPublicUrl}\" -> \"http://localhost:$port\"")
  }
}
```

Pass parameters to your Play application through
[your config file](https://github.com/alexdlaird/java-ngrok-example-play/blob/main/conf/application.conf) (including
making `.ngrok.io` an allowed host):

```
ngrok {
    enabled=true
}
play.filters.hosts {
  allowed = [".ngrok.io", "localhost:9000"]
}
```

Now Play can be started by the usual means, setting `ngrok.enabled` in the config to open a tunnel.

1. Run `sbt compile package` to build the application
1. Start application with `sbt run`
1. Check the logs for the `ngrok` tunnel's public URL, which should tunnel to  `http://localhost:9000`

## Docker

To use `java-ngrok` in a container image, you'll want to make sure you download and install the `ngrok` binary while
building the image. Here is an example `Dockerfile` that does this:

```Dockerfile
FROM ubuntu:24.04

ARG NGROK_INSTALLER_PATH=ngrok-v3-stable-linux-arm64.tgz

RUN apt-get update
RUN apt-get install -y curl openjdk-21-jre-headless
RUN curl -sSL https://ngrok-agent.s3.amazonaws.com/ngrok.asc \
      | tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null \
      && echo "deb https://ngrok-agent.s3.amazonaws.com buster main" \
      | tee /etc/apt/sources.list.d/ngrok.list \
      && apt update \
      && apt install ngrok

RUN mkdir -p /root/.config/ngrok
RUN echo "version: 2\nweb_addr: 0.0.0.0:4040" >> /root/.config/ngrok/ngrok.yml

# Provision your Java application
COPY my-java-ngrok-app.jar /root/my-java-ngrok-app.jar
CMD ["java", "-jar", "/root/my-java-ngrok-app.jar"]
```

In the above example, `ngrok` is being installed to `/usr/local/bin/ngrok`. You'll need to [specify this binary path](https://alexdlaird.github.io/java-ngrok/#binary-path)
for `ngrok` in your Java application to ensure `java-ngrok`'s installer is bypassed.

```java
final JavaNgrokConfig javaNgrokConfig = new JavaNgrokConfig.Builder()
        .withNgrokPath(Path.of("usr", "local", "bin", "ngrok"))
        .build();

final NgrokClient ngrokClient = new NgrokClient.Builder()
        .withJavaNgrokConfig(javaNgrokConfig)
        .build();
```

Now build your Java application, then build and launch the container image with:

```sh
# ... Command to build my-java-ngrok-app.jar
docker build -t my-java-ngrok .
docker run -e NGROK_AUTHTOKEN=$NGROK_AUTHTOKEN -it my-java-ngrok
```

If you want to start in a `bash` shell instead of your Java application, you can launch the container with.

```sh
docker run -e NGROK_AUTHTOKEN=$NGROK_AUTHTOKEN -it my-java-ngrok /bin/bash
```

The [`java-ngrok-example-spring` repository](https://github.com/alexdlaird/java-ngrok-example-spring/) also includes a
`Dockerfile` and `make` commands to run it, if you would like to see a complete example.

### Config File

`ngrok` will look for its config file in this container at `/root/.config/ngrok/ngrok.yml`. If you want to provide a
custom config file, specify a mount to this file when launching the container.

```sh
docker run -v ./ngrok.yml:/root/.config/ngrok/ngrok.yml -it my-java-ngrok
```

### Web Inspector

If you want to use `ngrok`'s web inspector, be sure to expose its port. Be sure whatever config file you use
[sets `web_addr: 0.0.0.0:4040`](https://ngrok.com/docs/agent/config/v2/#web_addr).

```sh
docker run --env-file .env -p 4040:4040 -it my-java-ngrok
```

### Docker Compose

You could also launch the container using `docker-compose.yml`:

```yaml
services:
  ngrok:
    image: my-java-ngrok
    env_file: ".env"
    volumes:
      - ./ngrok.yml:/root/.config/ngrok/ngrok.yml
    ports:
      - 4040:4040
```

Then launch it with:

```sh
docker compose up -d
```

## End-to-End Testing

Some testing use-cases might mean you want to temporarily expose a route via a `java-ngrok` tunnel to fully validate a
workflow. For example, an internal end-to-end tester, a step in a pre-deployment validation pipeline, or a service that
automatically updates a status page.

Whatever the case may be, using [JUnit's `BeforeAll` and `AfterAll`](https://junit.org/junit5/docs/current/api/org.junit.jupiter.api/org/junit/jupiter/api/BeforeAll.html)
fixtures are a good place to hook `java-ngrok` in to your integration tests. This snippet builds on the Dropwizard
example above, but it could be modified to work with other frameworks.

```java
@ExtendWith(DropwizardExtensionsSupport.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JavaNgrokTestCase extends TestCase {

    private final static DropwizardAppExtension<JavaNgrokExampleDropwizardConfiguration> dropwizardAppExtension = new DropwizardAppExtension<>(
        JavaNgrokExampleDropwizardApplication.class,
        ResourceHelpers.resourceFilePath("config.yml")
    );
    
    private NgrokClient ngrokClient;
    
    private String baseUrl;
    
    @BeforeAll
    public void setUpClass() {
        this.ngrokClient = new NgrokClient.Builder()
                .build();

        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withAddr(EXT.getLocalPort())
                .build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);
        this.baseUrl = tunnel.getPublicUrl();
        
        // ... Implement other initializes so you can assert against the inbound traffic through your tunnel
    }

    @AfterAll
    public void tearUpClass() {
        ngrokClient.kill();
    }
}
```

Now, any test that needs to assert against responses through a `java-ngrok` tunnel can simply extend
`JavaNgrokTestCase` to inherit these fixtures.

## Simple HTTP Server

Java's `HttpServer` class also makes for a useful development server. You can use `java-ngrok` to expose it to the web
via a tunnel, as shown here:

```java
public class NgrokHttpServer {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(NgrokHttpServer.class));

    public static void main(String[] args) throws Exception {
        final int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "80"));

        final HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        final NgrokClient ngrokClient = new NgrokClient.Builder().build();

        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withAddr(port)
                .build();
        final Tunnel tunnel = ngrokClient.connect(createTunnel);

        LOGGER.info(String.format("ngrok tunnel \"%s\" -> \"http://127.0.0.1:%d\"", tunnel.getPublicUrl(), port));

        server.start();
    }
}
```

## Simple TCP Server and Client
[![Clone on GitHub](https://img.shields.io/badge/Clone_on_GitHub-black?logo=github)](https://github.com/alexdlaird/java-ngrok-example-tcp-server-and-client)

This example project shows a simple TCP ping/pong server. It opens a local socket, uses ``ngrok`` to tunnel to that
socket, then the client/server communicate via the publicly exposed address.
