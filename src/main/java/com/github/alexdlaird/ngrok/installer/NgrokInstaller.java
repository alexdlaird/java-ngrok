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
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.github.alexdlaird.StringUtils.isBlank;

/**
 * A helper for downloading and installing the <code>ngrok</code> for the current system.
 */
public class NgrokInstaller {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(NgrokInstaller.class));

    private static final String MAC = "DARWIN";
    private static final String WINDOWS = "WINDOWS";
    private static final String LINUX = "LINUX";
    private static final String FREEBSD = "FREEBSD";
    private static final List<String> UNIX_BINARIES = List.of(MAC, LINUX, FREEBSD);
    private static final List<String> VALID_LOG_LEVELS = List.of("info", "debug");

    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

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
    public void installDefaultConfig(final Path configPath, Map<String, String> data) {
        try {
            Files.createDirectories(configPath.getParent());
            if (!Files.exists(configPath)) {
                Files.createFile(configPath);
            }

            final Map<String, String> config = getNgrokConfig(configPath);
            config.putAll(data);

            validateConfig(config);

            LOGGER.fine(String.format("Installing default config to %s ...", configPath));

            final FileOutputStream out = new FileOutputStream(configPath.toFile());
            out.write(gson.toJson(config).getBytes());
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
        final String arch = getArch();
        final String system = getSystem();
        final String plat = String.format("%s_%s", system, arch);

        LOGGER.fine(String.format("Platform to download: %s", plat));
        final NgrokCDNUrl ngrokCDNUrl = NgrokCDNUrl.valueOf(plat);

        LOGGER.fine(String.format("Installing ngrok to %s%s ...", ngrokPath, Files.exists(ngrokPath) ? ", overwriting" : ""));

        final Path ngrokZip = Paths.get(ngrokPath.getParent().toString(), "ngrok.zip");
        downloadFile(ngrokCDNUrl.getUrl(), ngrokZip);

        installNgrokZip(ngrokZip, ngrokPath);
    }

    /**
     * Validate that the config file at the given path is valid for <code>ngrok</code> and <code>java-ngrok</code>.
     *
     * @param configPath The config path to validate.
     */
    public void validateConfig(final Path configPath) {
        final Map<String, String> config = getNgrokConfig(configPath);

        validateConfig(config);
    }

    /**
     * Validate that the given map of config items are valid for <code>ngrok</code> and <code>java-ngrok</code>.
     *
     * @param data A map of things to be validated as config items.
     */
    public void validateConfig(final Map<String, String> data) {
        if (data.getOrDefault("web_addr", "127.0.0.1:4040").equals("false")) {
            throw new JavaNgrokException("\"web_addr\" cannot be false, as the ngrok API is a dependency for java-ngrok");
        }
        if (data.getOrDefault("log_format", "term").equals("json")) {
            throw new JavaNgrokException("\"log_format\" must be \"term\" to be compatible with java-ngrok");
        }
        if (!VALID_LOG_LEVELS.contains(data.getOrDefault("log_level", "info"))) {
            throw new JavaNgrokException("\"log_level\" must be \"info\" to be compatible with java-ngrok");
        }
    }

    private Map<String, String> getNgrokConfig(final Path configPath) {
        // TODO: implement a cache so we don't hit file IO every time we read the config
        try {
            final String config = Files.readString(configPath);

            if (isBlank(config)) {
                return Collections.emptyMap();
            }

            return gson.<Map<String, String>>fromJson(config, Map.class);
        } catch (IOException | JsonParseException e) {
            throw new JavaNgrokInstallerException(String.format("An error occurred while parsing the config file: %s", configPath), e);
        }
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

    private static String getSystem() {
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
}
