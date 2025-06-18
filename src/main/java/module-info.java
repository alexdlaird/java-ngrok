/*
 * Copyright (c) 2021-2025 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

/**
 * This module contains a Java wrapped for <code>ngrok</code> that manages its own binary.
 */
module com.github.alexdlaird.ngrok {
    requires com.google.gson;
    requires org.yaml.snakeyaml;
    requires org.slf4j;

    exports com.github.alexdlaird.exception;
    exports com.github.alexdlaird.http;
    exports com.github.alexdlaird.ngrok;
    exports com.github.alexdlaird.ngrok.agent;
    exports com.github.alexdlaird.ngrok.conf;
    exports com.github.alexdlaird.ngrok.installer;
    exports com.github.alexdlaird.ngrok.process;
    exports com.github.alexdlaird.ngrok.protocol;
    exports com.github.alexdlaird.util;
}
