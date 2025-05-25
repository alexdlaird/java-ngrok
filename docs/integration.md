# Integration Examples

## Spring
[![Clone on GitHub](https://img.shields.io/badge/Clone_on_GitHub-black?logo=github)](https://github.com/alexdlaird/java-ngrok-example-spring)

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
public class NgrokWebServerEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NgrokWebServerEventListener.class);

    private final Environment environment;

    private final NgrokConfiguration ngrokConfiguration;

    @Autowired
    public NgrokWebServerEventListener(final Environment environment,
                                       final NgrokConfiguration ngrokConfiguration) {
        this.environment = environment;
        this.ngrokConfiguration = ngrokConfiguration;
    }

    @EventListener
    public void onApplicationEvent(final WebServerInitializedEvent event) {
        // java-ngrok will only be installed, and should only ever be initialized, in a dev environment
        if (ngrokConfiguration.isEnabled()) {
            final NgrokClient ngrokClient = new NgrokClient.Builder()
                    .build();

            final int port = event.getWebServer().getPort();

            final CreateTunnel createTunnel = new CreateTunnel.Builder()
                    .withAddr(port)
                    .build();
            final Tunnel tunnel = ngrokClient.connect(createTunnel);
            final String publicUrl = tunnel.getPublicUrl();

            LOGGER.info(String.format("ngrok tunnel \"%s\" -> \"http://127.0.0.1:%d\"",publicUrl, port));

            // Update any base URLs or webhooks to use the public ngrok URL
            ngrokConfiguration.setPublicUrl(publicUrl);
            initWebhooks(publicUrl);
        }
    }

    private void initWebhooks(final String publicUrl) {
        // Update inbound traffic via APIs to use the public-facing ngrok URL
    }
}
```

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

### Application Integration

If `ngrok.enabled` config flag is set, `java-ngrok` will be initialized when Dropwizard is booting. An easy place to do
this is in the `run()` method of [the Application](https://github.com/alexdlaird/java-ngrok-example-dropwizard/blob/main/src/main/java/com/github/alexdlaird/JavaNgrokExampleDropwizardApplication.java).

```java
public class JavaNgrokExampleDropwizardApplication extends Application<JavaNgrokExampleDropwizardConfiguration> {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(JavaNgrokExampleDropwizardApplication.class));

    @Override
    public void run(final JavaNgrokExampleDropwizardConfiguration configuration,
                    final Environment environment) {
        // java-ngrok will only be installed, and should only ever be initialized, in a dev environment
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
        // Update inbound traffic via APIs to use the public-facing ngrok URL
    }

    // ... The rest of your Dropwizard application
}
```

## Play (Scala)
[![Clone on GitHub](https://img.shields.io/badge/Clone_on_GitHub-black?logo=github)](https://github.com/alexdlaird/java-ngrok-example-play)

## Application Integration

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

  // java-ngrok will only be installed, and should only ever be initialized, in a dev environment
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

[// TODO add]: # (## End-to-End Testing)

## Java Simple HTTP Server

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

## Java Simple TCP Server and Client
[![Clone on GitHub](https://img.shields.io/badge/Clone_on_GitHub-black?logo=github)](https://github.com/alexdlaird/java-ngrok-example-tcp-server-and-client)

This example project shows a simple TCP ping/pong server. It opens a local socket, uses ``ngrok`` to tunnel to that
socket, then the client/server communicate via the publicly exposed address.
