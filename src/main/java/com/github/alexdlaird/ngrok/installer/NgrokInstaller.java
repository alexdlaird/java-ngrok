/*
 * Copyright (c) 2021-2025 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.installer;

import com.github.alexdlaird.exception.JavaNgrokException;
import com.github.alexdlaird.exception.JavaNgrokInstallerException;
import com.github.alexdlaird.exception.JavaNgrokSecurityException;
import com.github.alexdlaird.http.DefaultHttpClient;
import com.github.alexdlaird.http.HttpClient;
import com.github.alexdlaird.http.HttpClientException;
import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.google.gson.JsonParseException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import static com.github.alexdlaird.util.StringUtils.isBlank;
import static java.util.Objects.nonNull;

/**
 * A helper for downloading and installing <code>ngrok</code> for the current system.
 *
 * <p>For usage examples, see
 * <a href="https://alexdlaird.github.io/java-ngrok/" target="_blank"><code>java-ngrok</code>'s documentation</a>.
 */
public class NgrokInstaller {

    private static final Logger LOGGER = LoggerFactory.getLogger(NgrokInstaller.class);

    public static final String MAC = "DARWIN";
    public static final String WINDOWS = "WINDOWS";
    public static final String LINUX = "LINUX";
    public static final String FREEBSD = "FREEBSD";
    public static final List<String> UNIX_BINARIES = List.of(MAC, LINUX, FREEBSD);
    public static final Path DEFAULT_NGROK_PATH = Paths.get(getDefaultNgrokDir().toString(),
        NgrokInstaller.getNgrokBin());
    public static final Path DEFAULT_CONFIG_PATH = Paths.get(getDefaultNgrokDir().toString(), "ngrok.yml");
    private final List<String> validLogLevels = List.of("info", "debug");
    private final Yaml yaml = new Yaml();
    private final Map<String, Map<String, Object>> configCache = new HashMap<>();

    private final HttpClient httpClient;

    /**
     * Construct with the {@link DefaultHttpClient}.
     */
    public NgrokInstaller() {
        this(new DefaultHttpClient.Builder()
            .withTimeout(6000)
            .build());
    }

    /**
     * Construct with a custom {@link HttpClient}.
     *
     * @param httpClient The HTTP client.
     */
    public NgrokInstaller(final HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    /**
     * Get the <code>ngrok</code> executable for the current system.
     *
     * @return The name of the <code>ngrok</code> executable.
     */
    public static String getNgrokBin() {
        final String system = getSystem();

        if (UNIX_BINARIES.contains(system)) {
            return "ngrok";
        } else {
            return "ngrok.exe";
        }
    }

    /**
     * Parse the name of the OS from system properties and return a friendly name.
     *
     * @return The friendly name of the OS.
     * @throws JavaNgrokInstallerException The OS is not supported.
     */
    public static String getSystem() {
        final String os = System.getProperty("os.name").replaceAll(" ", "").toLowerCase();

        if (os.startsWith("mac")) {
            return MAC;
        } else if (os.startsWith("windows")
                   || os.contains("cygwin")
                   || os.contains("ming")) {
            return WINDOWS;
        } else if (os.startsWith("linux")) {
            return LINUX;
        } else if (os.startsWith("freebsd")) {
            return FREEBSD;
        } else {
            throw new JavaNgrokInstallerException(String.format("Unknown os.name: %s", os));
        }
    }

    static Path getDefaultNgrokDir() {
        final String system = getSystem();
        final String userHome = System.getProperty("user.home");
        if (system.equals(MAC)) {
            return Paths.get(userHome, "Library", "Application Support", "ngrok");
        } else if (system.equals(WINDOWS)) {
            return Paths.get(userHome, "AppData", "Local", "ngrok");
        } else {
            return Paths.get(userHome, ".config", "ngrok");
        }
    }

    /**
     * See {@link #installDefaultConfig(Path, Map, NgrokVersion, ConfigVersion)}.
     */
    public synchronized void installDefaultConfig(final Path configPath, final Map<String, Object> data) {
        installDefaultConfig(configPath, data, NgrokVersion.V3, ConfigVersion.V2);
    }

    /**
     * See {@link #installDefaultConfig(Path, Map, NgrokVersion, ConfigVersion)}.
     */
    public synchronized void installDefaultConfig(final Path configPath,
                                                  final Map<String, Object> data,
                                                  final NgrokVersion ngrokVersion) {
        installDefaultConfig(configPath, data, ngrokVersion, ConfigVersion.V2);
    }

    /**
     * Install the default <code>ngrok</code> config. If a config is not already present for the given path, create
     * one.
     *
     * @param configPath    The path to where the <code>ngrok</code> config should be installed.
     * @param data          A map of things to add to the default config.
     * @param ngrokVersion  The major version of <code>ngrok</code> installed.
     * @param configVersion The <code>ngrok</code> config version.
     * @throws JavaNgrokInstallerException An error occurred downloading <code>ngrok</code>.
     */
    public synchronized void installDefaultConfig(final Path configPath,
                                                  final Map<String, Object> data,
                                                  final NgrokVersion ngrokVersion,
                                                  final ConfigVersion configVersion) {
        try {
            Files.createDirectories(configPath.getParent());
            if (!Files.exists(configPath)) {
                Files.createFile(configPath);
            }

            final Map<String, Object> config = getNgrokConfig(configPath, false, ngrokVersion, configVersion);

            config.putAll(getDefaultConfig(ngrokVersion, configVersion));

            config.putAll(data);

            validateConfig(config);

            LOGGER.trace("Installing default config to {} ...", configPath);

            final FileOutputStream out = new FileOutputStream(configPath.toFile());
            final StringWriter writer = new StringWriter();
            yaml.dump(config, writer);
            out.write(writer.toString().getBytes(StandardCharsets.UTF_8));
            out.close();
        } catch (final IOException e) {
            throw new JavaNgrokInstallerException(String.format("An error while installing the default "
                                                                + "ngrok config to %s.", configPath), e);
        }
    }

    /**
     * See {@link #installNgrok(Path, NgrokVersion)}.
     */
    public void installNgrok(final Path ngrokPath) {
        installNgrok(ngrokPath, NgrokVersion.V3);
    }

    /**
     * Download and install the latest <code>ngrok</code> for the current system, overwriting any existing contents at
     * the given path.
     *
     * @param ngrokPath    The path to where the <code>ngrok</code> binary will be downloaded.
     * @param ngrokVersion The major <code>ngrok</code> version to install.
     * @throws JavaNgrokInstallerException An error occurred installing <code>ngrok</code>.
     * @throws JavaNgrokSecurityException  An error occurred unzipping the download.
     */
    public void installNgrok(final Path ngrokPath, final NgrokVersion ngrokVersion) {
        final NgrokCDNUrl ngrokCDNUrl = getNgrokCDNUrl(ngrokVersion);

        LOGGER.trace("Installing ngrok {} to {}{} ...", ngrokVersion, ngrokPath,
            Files.exists(ngrokPath) ? ", overwriting" : "");

        final Path ngrokZip = Paths.get(ngrokPath.getParent().toString(), "ngrok.zip");
        downloadFile(ngrokCDNUrl.getUrl(), ngrokZip);

        installNgrokZip(ngrokZip, ngrokPath);
    }

    /**
     * See {@link #getNgrokCDNUrl(NgrokVersion)}.
     */
    public NgrokCDNUrl getNgrokCDNUrl() {
        return getNgrokCDNUrl(NgrokVersion.V3);
    }

    /**
     * Determine the <code>ngrok</code> CDN URL for the current OS and architecture.
     *
     * @param ngrokVersion The major version of <code>ngrok</code> to install.
     * @return The <code>ngrok</code> CDN URL.
     */
    public NgrokCDNUrl getNgrokCDNUrl(final NgrokVersion ngrokVersion) {
        final String arch = getArch();
        final String system = getSystem();
        final String plat = String.format("%s_%s", system, arch);

        LOGGER.trace("Platform to download: {}", plat);
        if (ngrokVersion == NgrokVersion.V2) {
            return NgrokV2CDNUrl.valueOf(plat);
        } else {
            return NgrokV3CDNUrl.valueOf(plat);
        }
    }

    /**
     * Validate that the config file at the given path is valid for <code>ngrok</code> and <code>java-ngrok</code>.
     *
     * @param configPath The config path to validate.
     */
    public synchronized void validateConfig(final Path configPath) {
        final Map<String, Object> config = getNgrokConfig(configPath);

        validateConfig(config);
    }

    /**
     * Validate that the given map of config items are valid for <code>ngrok</code> and <code>java-ngrok</code>.
     *
     * @param data A map of things to be validated as config items.
     * @throws JavaNgrokException A key or value failed validation.
     */
    public void validateConfig(final Map<String, Object> data) {
        if (data.getOrDefault("web_addr", "127.0.0.1:4040").equals("false")) {
            throw new JavaNgrokException("\"web_addr\" cannot be false, as the ngrok API is a "
                                         + "dependency for java-ngrok");
        }
        if (data.getOrDefault("log_format", "term").equals("json")) {
            throw new JavaNgrokException("\"log_format\" must be \"term\" to be compatible with java-ngrok");
        }
        if (!validLogLevels.contains((String) data.getOrDefault("log_level", "info"))) {
            throw new JavaNgrokException("\"log_level\" must be \"info\" to be compatible with java-ngrok");
        }
    }

    /**
     * Get the <code>ngrok</code> config from the given path.
     *
     * @param configPath    The <code>ngrok</code> config path to read.
     * @param useCache      Use the cached version of the config (if populated).
     * @param ngrokVersion  The major version of <code>ngrok</code> installed.
     * @param configVersion The <code>ngrok</code> config version.
     * @return A map of the <code>ngrok</code> config.
     * @throws JavaNgrokInstallerException The config could not be parsed.
     */
    public synchronized Map<String, Object> getNgrokConfig(final Path configPath,
                                                           final boolean useCache,
                                                           final NgrokVersion ngrokVersion,
                                                           final ConfigVersion configVersion) {
        final String key = configPath.toString();
        if (!configCache.containsKey(key) || !useCache) {
            try {
                final String config = Files.readString(configPath);

                if (isBlank(config)) {
                    configCache.put(key, getDefaultConfig(ngrokVersion, configVersion));
                } else {
                    configCache.put(key, yaml.load(config));
                }
            } catch (final IOException | JsonParseException e) {
                throw new JavaNgrokInstallerException(String.format("An error occurred while parsing "
                                                                    + "the config file: %s", configPath), e);
            }
        }

        return configCache.get(key);
    }

    /**
     * See {@link #getNgrokConfig(Path, boolean, NgrokVersion, ConfigVersion)}.
     */
    public synchronized Map<String, Object> getNgrokConfig(final Path configPath, final boolean useCache) {
        return getNgrokConfig(configPath, useCache, NgrokVersion.V3, ConfigVersion.V2);
    }

    /**
     * See {@link #getNgrokConfig(Path, boolean, NgrokVersion, ConfigVersion)}.
     */
    public synchronized Map<String, Object> getNgrokConfig(final Path configPath) {
        return getNgrokConfig(configPath, true);
    }

    /**
     * Get the default config params for the given major version of <code>ngrok</code>.
     *
     * @param ngrokVersion  The major version of <code>ngrok</code> installed.
     * @param configVersion The <code>ngrok</code> config version.
     * @return The default config.
     */
    public Map<String, Object> getDefaultConfig(final NgrokVersion ngrokVersion,
                                                final ConfigVersion configVersion) {
        if (ngrokVersion == NgrokVersion.V2) {
            return new HashMap<>();
        } else {
            final HashMap<String, Object> config = new HashMap<>();
            config.put("version", configVersion.getVersion());
            if (configVersion == ConfigVersion.V2) {
                config.put("region", "us");
            }
            return config;
        }
    }

    private void installNgrokZip(final Path zipPath, final Path ngrokPath) {
        try {
            final Path dir = ngrokPath.getParent();

            LOGGER.trace("Extracting ngrok binary from {} to {} ...", zipPath, ngrokPath);

            Files.createDirectories(dir);

            final byte[] buffer = new byte[1024];
            final ZipInputStream in = new ZipInputStream(new FileInputStream(zipPath.toFile()));
            ZipEntry zipEntry;
            while (nonNull(zipEntry = in.getNextEntry())) {
                final Path file = Paths.get(dir.toString(), zipEntry.getName());
                if (!file.normalize().startsWith(dir)) {
                    throw new JavaNgrokSecurityException("Bad zip entry, paths don't match");
                }
                if (zipEntry.isDirectory()) {
                    if (!Files.isDirectory(file)) {
                        Files.createDirectories(file);
                    }
                } else {
                    final Path parent = file.getParent();
                    if (!Files.isDirectory(parent)) {
                        Files.createDirectories(parent);
                    }

                    final FileOutputStream out = new FileOutputStream(file.toFile());
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }
                    out.close();
                }
            }
            in.closeEntry();
            in.close();

            if (ngrokPath.getFileSystem().supportedFileAttributeViews().contains("posix")) {
                final Set<PosixFilePermission> perms = Files.readAttributes(ngrokPath, PosixFileAttributes.class)
                                                            .permissions();
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                Files.setPosixFilePermissions(ngrokPath, perms);
            }
        } catch (final IOException e) {
            throw new JavaNgrokInstallerException("An error occurred while unzipping ngrok.", e);
        }
    }

    private void downloadFile(final String url, final Path dest) {
        try {
            Files.createDirectories(dest.getParent());

            LOGGER.trace("Download ngrok from {} ...", url);

            httpClient.get(url, List.of(), Map.of(), dest);
        } catch (final IOException | HttpClientException | InterruptedException e) {
            throw new JavaNgrokInstallerException(String.format("An error occurred while downloading "
                                                                + "ngrok from %s.", url), e);
        }
    }

    private String getArch() {
        final String archProperty = System.getProperty("os.arch").toLowerCase();

        final StringBuilder arch = new StringBuilder();
        if (archProperty.contains("x86_64")
            || archProperty.contains("aarch64")) {
            arch.append("x86_64");
        } else {
            arch.append("i386");
        }
        if (archProperty.startsWith("arm") || archProperty.startsWith("aarch64")) {
            arch.append("_arm");
        }

        return arch.toString();
    }
}
