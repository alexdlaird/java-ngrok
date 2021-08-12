package com.github.alexdlaird.ngrok.installer;

import com.github.alexdlaird.ngrok.NgrokException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class NgrokInstaller {

    // TODO: this entire class is a POC placeholder for simple testing while the API is built out, java-ngrok will soon manage its own binary

    public void installNgrok() {
        final String arch = getArch();
        final String system = getSystem();
        final NgrokCDNUrl ngrokCDNUrl = NgrokCDNUrl.valueOf(String.format("%s_%s", system, arch));
//        System.out.println(System.getenv("PROCESSOR_ARCHITECTURE"));
//        System.out.println(System.getenv("PROCESSOR_ARCHITEW6432"));

//        try {
//            final InputStream in = new URL(ngrokCDNUrl.getUrl()).openStream();
//            Files.copy(in, Paths.get("ngrok"), StandardCopyOption.REPLACE_EXISTING);
//        } catch (IOException e) {
//            // TODO: handle
//            e.printStackTrace();
//        }

        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("pip", "install", "pyngrok");
        final Process proc;
        try {
            proc = processBuilder.start();
            proc.waitFor();
        } catch (IOException | InterruptedException e) {
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

    private String getSystem() {
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

//    def validate_config(data):
//            if data.get("web_addr", None) is False:
//    raise PyngrokError("\"web_addr\" cannot be False, as the ngrok API is a dependency for pyngrok")
//    elif data.get("log_format") == "json":
//    raise PyngrokError("\"log_format\" must be \"term\" to be compatible with pyngrok")
//    elif data.get("log_level", "info") not in ["info", "debug"]:
//    raise PyngrokError("\"log_level\" must be \"info\" to be compatible with pyngrok")


//    def install_default_config(config_path, data=None):
//    if data is None:
//    data = {}
//
//    config_dir = os.path.dirname(config_path)
//            if not os.path.exists(config_dir):
//            os.makedirs(config_dir)
//            if not os.path.exists(config_path):
//    open(config_path, "w").close()
//
//    config = get_ngrok_config(config_path, use_cache=False)
//
//    config.update(data)
//
//    validate_config(config)
//
//    with open(config_path, "w") as config_file:
//            logger.debug("Installing default ngrok config to {} ...".format(config_path))
//
//            yaml.dump(config, config_file)


//    def get_ngrok_config(config_path, use_cache=True):
//    global _config_cache
//
//    if not _config_cache or not use_cache:
//    with open(config_path, "r") as config_file:
//    config = yaml.safe_load(config_file)
//            if config is None:
//    config = {}
//
//    _config_cache = config
//
//    return _config_cache
}
