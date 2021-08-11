package com.github.alexdlaird.ngrok.installer;

import java.io.IOException;

public class NgrokInstaller {

    // TODO: this entire class is a POC placeholder for simple testing while the API is built out, java-ngrok will soon manage its own binary

    public void install() {
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
}
