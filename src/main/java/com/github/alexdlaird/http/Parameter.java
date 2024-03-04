/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.http;

/**
 * A parameter for the {@link HttpClient}.
 */
public class Parameter {

    private final String name;
    private final String value;

    /**
     * Construct a parameter.
     *
     * @param name  Name of the field.
     * @param value Value of the field.
     */
    public Parameter(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Get the name of the field.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the value of the field.
     */
    public String getValue() {
        return value;
    }
}
