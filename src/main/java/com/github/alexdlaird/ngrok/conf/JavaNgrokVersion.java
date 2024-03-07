package com.github.alexdlaird.ngrok.conf;

import static java.util.Objects.nonNull;

import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.NgrokClient.Builder;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A singleton object that represents the <code>java-ngrok</code> library version.
 */
public class JavaNgrokVersion {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(NgrokClient.class));

    private static JavaNgrokVersion instance = null;

    private final String version;

    private JavaNgrokVersion(final String version) {
        this.version = version;
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

                    return properties.getProperty("version");
                }
            }
        } catch (final IOException ex) {
            LOGGER.log(Level.WARNING, "An error occurred trying to read \"version\" from resource properties", ex);
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
