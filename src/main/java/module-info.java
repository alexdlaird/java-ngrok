module com.github.alexdlaird.ngrok {
    requires java.logging;
    requires com.google.gson;
    requires org.yaml.snakeyaml;

    exports com.github.alexdlaird.exception;
    exports com.github.alexdlaird.http;
    exports com.github.alexdlaird.ngrok;
    exports com.github.alexdlaird.ngrok.conf;
    exports com.github.alexdlaird.ngrok.installer;
    exports com.github.alexdlaird.ngrok.process;
    exports com.github.alexdlaird.ngrok.protocol;
    exports com.github.alexdlaird.util;
}
