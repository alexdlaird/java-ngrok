package com.github.alexdlaird.ngrok.conf;

import com.github.alexdlaird.ngrok.NgrokClient.Builder;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.nonNull;

/**
 * A singleton object that represents the <code>java-ngrok</code> library version.
 */
public class JavaNgrokVersion {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaNgrokVersion.class);

    private static JavaNgrokVersion instance = null;

    private final String version;

    private JavaNgrokVersion(final String version) {
        this.version = Objects.requireNonNull(version);
    }

    /**
     * Get or initialize the singleton instance.
     *
     * @return The singleton instance.
     */
    public static synchronized JavaNgrokVersion getInstance() {
        if (instance == null) {
            final String version = getVersionFromProperties();
            instance = new JavaNgrokVersion(version);
        }

        return instance;
    }

    private static String getVersionFromProperties() {
        try {
            try (final InputStream resourceStream = Builder.class.getResourceAsStream("/version.properties")) {
                if (nonNull(resourceStream)) {
                    final Properties properties = new Properties();
                    properties.load(resourceStream);

                    final String version = properties.getProperty("version");
                    LOGGER.trace("Version number {} fetched from version.properties resource", version);

                    return version;
                }
            }
        } catch (final IOException e) {
            LOGGER.warn("An error occurred trying to read \"version\" from resource properties", e);
        }

        return null;
    }

    /**
     * Get the version.
     */
    public String getVersion() {
        return version;
    }
}
