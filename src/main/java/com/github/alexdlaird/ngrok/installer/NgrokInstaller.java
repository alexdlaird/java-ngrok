/*
 * Copyright (c) 2021 Alex Laird
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.alexdlaird.ngrok.installer;

import com.github.alexdlaird.exception.JavaNgrokException;
import com.github.alexdlaird.exception.JavaNgrokInstallerException;
import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.google.gson.JsonParseException;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.github.alexdlaird.util.StringUtils.isBlank;
import static java.util.Objects.isNull;

/**
 * A helper for downloading and installing the <code>ngrok</code> for the current system.
 *
 * <h2>Config File</h2>
 * By default, <a href="https://ngrok.com/docs#config" target="_blank"><code>ngrok</code> will look for its config file</a> in the home
 * directory’s <code>.ngrok2</code> folder. We can override this behavior with
 * {@link JavaNgrokConfig.Builder#withConfigPath(Path)}.
 *
 * <h2>Binary Path</h2>
 * The <code>java-ngrok</code> package manages its own <code>ngrok</code> binary. We can use our <code>ngrok</code>
 * binary if we want by setting it with {@link JavaNgrokConfig.Builder#withNgrokPath(Path)} and passing that config to
 * {@link NgrokClient}.
 */
public class NgrokInstaller {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(NgrokInstaller.class));

    public static final String MAC = "DARWIN";
    public static final String WINDOWS = "WINDOWS";
    public static final String LINUX = "LINUX";
    public static final String FREEBSD = "FREEBSD";
    public static final List<String> UNIX_BINARIES = List.of(MAC, LINUX, FREEBSD);
    public static final Path DEFAULT_NGROK_PATH = Paths.get(System.getProperty("user.home"), ".ngrok2", NgrokInstaller.getNgrokBin());
    public static final Path DEFAULT_CONFIG_PATH = Paths.get(System.getProperty("user.home"), ".ngrok2", "ngrok.yml");

    private static final List<String> VALID_LOG_LEVELS = List.of("info", "debug");

    private final Yaml yaml = new Yaml();

    private Map<String, Object> configCache;

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
     * Install the default <code>ngrok</code> config. If a config is not already present for the given path,
     * create one.
     *
     * @param configPath The path to where the <code>ngrok</code> config should be installed.
     * @param data       A map of things to add to the default config.
     */
    public void installDefaultConfig(final Path configPath, Map<String, Object> data) {
        try {
            Files.createDirectories(configPath.getParent());
            if (!Files.exists(configPath)) {
                Files.createFile(configPath);
            }

            final Map<String, Object> config = getNgrokConfig(configPath, false);
            config.putAll(data);

            validateConfig(config);

            LOGGER.fine(String.format("Installing default config to %s ...", configPath));

            final FileOutputStream out = new FileOutputStream(configPath.toFile());
            final StringWriter writer = new StringWriter();
            yaml.dump(config, writer);
            out.write(writer.toString().getBytes());
            out.close();
        } catch (IOException e) {
            throw new JavaNgrokInstallerException(String.format("An error while installing the default ngrok config to %s.", configPath), e);
        }
    }

    /**
     * Download and install the latest <code>ngrok</code> for the current system, overwriting any existing contents
     * at the given path.
     *
     * @param ngrokPath The path to where the <code>ngrok</code> binary will be downloaded.
     */
    public void installNgrok(final Path ngrokPath) {
        final NgrokCDNUrl ngrokCDNUrl = getNgrokCDNUrl();

        LOGGER.fine(String.format("Installing ngrok to %s%s ...", ngrokPath, Files.exists(ngrokPath) ? ", overwriting" : ""));

        final Path ngrokZip = Paths.get(ngrokPath.getParent().toString(), "ngrok.zip");
        downloadFile(ngrokCDNUrl.getUrl(), ngrokZip);

        installNgrokZip(ngrokZip, ngrokPath);
    }

    /**
     * Determine the <code>ngrok</code> CDN URL for the current OS and architecture.
     *
     * @return The <code>ngrok</code> CDN URL.
     */
    public NgrokCDNUrl getNgrokCDNUrl() {
        final String arch = getArch();
        final String system = getSystem();
        final String plat = String.format("%s_%s", system, arch);

        LOGGER.fine(String.format("Platform to download: %s", plat));
        return NgrokCDNUrl.valueOf(plat);
    }

    /**
     * Validate that the config file at the given path is valid for <code>ngrok</code> and <code>java-ngrok</code>.
     *
     * @param configPath The config path to validate.
     */
    public void validateConfig(final Path configPath) {
        final Map<String, Object> config = getNgrokConfig(configPath);

        validateConfig(config);
    }

    /**
     * Validate that the given map of config items are valid for <code>ngrok</code> and <code>java-ngrok</code>.
     *
     * @param data A map of things to be validated as config items.
     */
    public void validateConfig(final Map<String, Object> data) {
        if (data.getOrDefault("web_addr", "127.0.0.1:4040").equals("false")) {
            throw new JavaNgrokException("\"web_addr\" cannot be false, as the ngrok API is a dependency for java-ngrok");
        }
        if (data.getOrDefault("log_format", "term").equals("json")) {
            throw new JavaNgrokException("\"log_format\" must be \"term\" to be compatible with java-ngrok");
        }
        if (!VALID_LOG_LEVELS.contains((String) data.getOrDefault("log_level", "info"))) {
            throw new JavaNgrokException("\"log_level\" must be \"info\" to be compatible with java-ngrok");
        }
    }

    /**
     * Parse the name fo the OS from system properties and return a friendly name.
     *
     * @return The friendly name of the OS.
     */
    public static String getSystem() {
        final String os = System.getProperty("os.name").replaceAll(" ", "").toLowerCase();

        if (os.startsWith("mac")) {
            return MAC;
        } else if (os.startsWith("windows") || os.contains("cygwin")) {
            return WINDOWS;
        } else if (os.startsWith("linux")) {
            return LINUX;
        } else if (os.startsWith("freebsd")) {
            return FREEBSD;
        } else {
            throw new JavaNgrokInstallerException(String.format("Unknown os.name: %s", os));
        }
    }

    /**
     * Get the <code>ngrok</code> config from the given path.
     *
     * @param configPath The <code>ngrok</code> config path to read.
     * @param useCache   Use the cached version of the config (if populated).
     * @return A map of the <code>ngrok</code> config.
     */
    public Map<String, Object> getNgrokConfig(final Path configPath, final boolean useCache) {
        if (isNull(configCache) || !useCache) {
            try {
                final String config = Files.readString(configPath);

                if (isBlank(config)) {
                    configCache = new HashMap<>();
                } else {
                    configCache = yaml.load(config);
                }
            } catch (IOException | JsonParseException e) {
                throw new JavaNgrokInstallerException(String.format("An error occurred while parsing the config file: %s", configPath), e);
            }
        }

        return configCache;
    }

    /**
     * See {@link #getNgrokConfig(Path, boolean)}.
     */
    public Map<String, Object> getNgrokConfig(final Path configPath) {
        return getNgrokConfig(configPath, true);
    }

    private void installNgrokZip(final Path zipPath, final Path ngrokPath) {
        try {
            final Path dir = ngrokPath.getParent();

            LOGGER.fine(String.format("Extracting ngrok binary from %s to %s ...", zipPath, ngrokPath));

            Files.createDirectories(dir);

            final byte[] buffer = new byte[1024];
            final ZipInputStream in = new ZipInputStream(new FileInputStream(zipPath.toFile()));
            ZipEntry zipEntry;
            while ((zipEntry = in.getNextEntry()) != null) {
                final Path file = Paths.get(dir.toString(), zipEntry.getName());
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
                final Set<PosixFilePermission> perms = Files.readAttributes(ngrokPath, PosixFileAttributes.class).permissions();
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                perms.add(PosixFilePermission.GROUP_EXECUTE);
                perms.add(PosixFilePermission.OTHERS_EXECUTE);
                Files.setPosixFilePermissions(ngrokPath, perms);
            }
        } catch (IOException e) {
            throw new JavaNgrokInstallerException("An error occurred while unzipping ngrok.", e);
        }
    }

    private void downloadFile(final String url, final Path dest) {
        try {
            Files.createDirectories(dest.getParent());

            LOGGER.fine(String.format("Download ngrok from %s ...", url));

            final InputStream in = new URL(url).openStream();
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new JavaNgrokInstallerException(String.format("An error occurred while downloading the file from %s.", url), e);
        }
    }

    private String getArch() {
        final String archProperty = System.getProperty("os.arch");

        final StringBuilder arch = new StringBuilder();
        if (archProperty.contains("x86_64")) {
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
