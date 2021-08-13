package com.github.alexdlaird.ngrok.installer;

import com.github.alexdlaird.exception.JavaNgrokInstallerException;

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
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class NgrokInstaller {

    // TODO: this entire class is a POC placeholder for simple testing while the API is built out

    private static final List<String> unixBinaries = List.of("DARWIN", "LINUX", "FREEBSD");

    public static String getNgrokBin() {
        final String system = getSystem();

        if (unixBinaries.contains(system)) {
            return "ngrok";
        } else {
            return "ngrok.exe";
        }
    }

    public void installDefaultConfig(Path dest) {
        try {
            Files.createDirectories(dest.getParent());

            final FileOutputStream out = new FileOutputStream(dest.toFile());
            out.write("{}".getBytes());
            out.close();
        } catch (IOException e) {
            throw new JavaNgrokInstallerException(String.format("An error while installing the default ngrok config to %s.", dest), e);
        }
    }

    public void installNgrok(final Path ngrokPath) {
        final Path dir = ngrokPath.getParent();

        final String arch = getArch();
        final String system = getSystem();
        final NgrokCDNUrl ngrokCDNUrl = NgrokCDNUrl.valueOf(String.format("%s_%s", system, arch));

        final Path ngrokZip = Paths.get(dir.toString(), "ngrok.zip");
        downloadFile(ngrokCDNUrl.getUrl(), ngrokZip);

        installNgrokZip(ngrokZip, dir);
    }

    private void installNgrokZip(Path zip, Path dest) {
        try {
            Files.createDirectories(dest);

            final byte[] buffer = new byte[1024];
            final ZipInputStream in = new ZipInputStream(new FileInputStream(zip.toFile()));
            ZipEntry zipEntry = in.getNextEntry();
            while (zipEntry != null) {
                final Path file = Paths.get(dest.toString(), zipEntry.getName());
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
                zipEntry = in.getNextEntry();
            }
            in.closeEntry();
            in.close();

            if (dest.getFileSystem().supportedFileAttributeViews().contains("posix")) {
                final Path ngrok = Paths.get(dest.toString(), getNgrokBin());
                final Set<PosixFilePermission> perms = Files.readAttributes(ngrok, PosixFileAttributes.class).permissions();
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                perms.add(PosixFilePermission.GROUP_EXECUTE);
                perms.add(PosixFilePermission.OTHERS_EXECUTE);
                Files.setPosixFilePermissions(ngrok, perms);
            }
        } catch (IOException e) {
            throw new JavaNgrokInstallerException("An error occurred while unzipping ngrok.", e);
        }
    }

    private void downloadFile(final String url, final Path dest) {
        try {
            Files.createDirectories(dest.getParent());

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
            return "DARWIN";
        } else if (os.startsWith("windows") || os.contains("cygwin")) {
            return "WINDOWS";
        } else if (os.startsWith("linux")) {
            return "LINUX";
        } else if (os.startsWith("freebsd")) {
            return "FREEBSD";
        } else {
            throw new JavaNgrokInstallerException(String.format("Unknown os.name: %s", os));
        }
    }
}
