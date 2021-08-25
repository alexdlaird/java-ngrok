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
import java.util.UUID;

import static java.util.Objects.nonNull;

public class NgrokTestCase {

    protected JavaNgrokConfig javaNgrokConfig = new JavaNgrokConfig.Builder()
            .withConfigPath(Paths.get("build", ".ngrok2", "config.yml").toAbsolutePath())
            .build();

    protected NgrokInstaller ngrokInstaller = new NgrokInstaller();

    protected NgrokProcess ngrokProcess;

    protected NgrokProcess ngrokProcess2;

    @BeforeEach
    public void setUp() {
        ngrokProcess = new NgrokProcess(javaNgrokConfig, ngrokInstaller);
    }

    @AfterEach
    public void tearDown() throws IOException {
        ngrokProcess.stop();
        if (nonNull(ngrokProcess2)) {
            ngrokProcess2.stop();
        }

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
