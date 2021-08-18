package com.github.alexdlaird.ngrok;

import com.github.alexdlaird.exception.JavaNgrokException;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.installer.NgrokInstaller;
import com.github.alexdlaird.ngrok.process.NgrokProcess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;

public class NgrokTestCase {

    protected JavaNgrokConfig javaNgrokConfig = new JavaNgrokConfig.Builder()
            .withConfigPath(Paths.get("build", ".ngrok2", "config.yml").toAbsolutePath())
            .withReconnectSessionRetries(10)
            .build();

    protected NgrokInstaller ngrokInstaller = new NgrokInstaller();

    protected NgrokProcess ngrokProcess;

    @BeforeEach
    public void setUp() {
        ngrokProcess = new NgrokProcess(javaNgrokConfig, ngrokInstaller);
    }

    @AfterEach
    public void tearDown() throws IOException {
        ngrokProcess.stop();

        Files.walk(javaNgrokConfig.getConfigPath().getParent())
                .sorted(Comparator.reverseOrder())
                .forEach((path) -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new JavaNgrokException(String.format("An error occurred cleaning up file %s when testing.", path));
                    }
                });
    }

    protected String createUniqueSubdomain() {
        return String.format("java-ngrok-%s-%s-%s-tcp", UUID.randomUUID(), System.getProperty("java.version").replaceAll("\\.", ""), NgrokInstaller.getSystem().toLowerCase());
    }
}
