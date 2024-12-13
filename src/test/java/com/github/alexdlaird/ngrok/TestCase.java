package com.github.alexdlaird.ngrok;

import static com.github.alexdlaird.util.StringUtils.isNotBlank;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class TestCase {
    protected String testRequiresEnvVar(final String varName) {
        final String varValue = System.getenv(varName);
        assumeTrue(isNotBlank(varValue), String.format("%s environment variable not set", varName));
        return varValue;
    }
}
