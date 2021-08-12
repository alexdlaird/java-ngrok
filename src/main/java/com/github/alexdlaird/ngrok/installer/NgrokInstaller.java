package com.github.alexdlaird.ngrok.installer;

import com.github.alexdlaird.ngrok.NgrokException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
            // TODO: handle
            e.printStackTrace();
        }
    }

    public void installNgrok(final Path ngrokPath) {
        final Path dir = ngrokPath.getParent();

        final String arch = getArch();
        final String system = getSystem();
        final NgrokCDNUrl ngrokCDNUrl = NgrokCDNUrl.valueOf(String.format("%s_%s", system, arch));

        final Path ngrokZip = Paths.get(dir + File.separator + "ngrok.zip");
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
                final File file = new File(dest.toFile(), zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    if (!file.isDirectory() && !file.mkdirs()) {
                        throw new IOException("Failed to create directory " + file);
                    }
                } else {
                    // fix for Windows-created archives
                    final File parent = file.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    // write file content
                    final FileOutputStream out = new FileOutputStream(file);
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

            final Path ngrok = Paths.get(dest + File.separator + getNgrokBin());
            final Set<PosixFilePermission> perms = Files.readAttributes(ngrok, PosixFileAttributes.class).permissions();
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            perms.add(PosixFilePermission.GROUP_EXECUTE);
            perms.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(ngrok, perms);
        } catch (IOException e) {
            // TODO: handle
            e.printStackTrace();
        }
    }

    private void downloadFile(final String url, final Path dest) {
        try {
            Files.createDirectories(dest.getParent());

            final InputStream in = new URL(url).openStream();
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // TODO: handle
            e.printStackTrace();
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
            throw new NgrokException(String.format("Unknown os.name: %s", os));
        }
    }
}
