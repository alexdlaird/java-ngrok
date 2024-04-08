/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import com.google.gson.annotations.SerializedName;

/**
 * An enum representing <code>ngrok</code>'s valid regions, as defined in
 * <a href="https://ngrok.com/docs/ngrok-agent/config#global-options" target="_blank"><code>ngrok</code>'s docs</a>.
 */
public enum Region {

    @SerializedName("us")
    US,
    @SerializedName("us-cal-1")
    US_CAL_1,
    @SerializedName("eu")
    EU,
    @SerializedName("ap")
    AP,
    @SerializedName("au")
    AU,
    @SerializedName("sa")
    SA,
    @SerializedName("jp")
    JP,
    @SerializedName("in")
    IN;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
