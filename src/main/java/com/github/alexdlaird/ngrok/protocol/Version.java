package com.github.alexdlaird.ngrok.protocol;

public class Version {
    private final String ngrokVersion;
    private final String javaNgrokVersion;

    public Version(final String ngrokVersion, final String javaNgrokVersion) {
        this.ngrokVersion = ngrokVersion;
        this.javaNgrokVersion = javaNgrokVersion;
    }

    public String getNgrokVersion() {
        return ngrokVersion;
    }

    public String getJavaNgrokVersion() {
        return javaNgrokVersion;
    }
}
