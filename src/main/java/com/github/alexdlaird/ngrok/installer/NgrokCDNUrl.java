/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.installer;

/**
 * An interface for getting <code>ngrok</code> download URLs.
 */
public interface NgrokCDNUrl {
    String getUrl();
}
