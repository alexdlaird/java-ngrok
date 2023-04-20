/*
 * Copyright (c) 2023 Alex Laird
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.alexdlaird.ngrok.protocol;

import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

public class OAuth {

    public static class Builder {
        private String provider;
        private List<String> scopes;
        private List<String> allowEmails;
        private List<String> allowDomains;

        public Builder() {
        }

        public Builder(Map<String, Object> oAuth) {
            if (oAuth.containsKey("provider")) {
                this.provider = (String) oAuth.get("provider");
            }
            if (oAuth.containsKey("scopes")) {
                this.scopes = (List<String>) oAuth.get("scopes");
            }
            if (oAuth.containsKey("allow_emails")) {
                this.allowEmails = (List<String>) oAuth.get("allow_emails");
            }
            if (oAuth.containsKey("allow_domains")) {
                this.allowDomains = (List<String>) oAuth.get("allow_domains");
            }
        }

        /**
         * The OAuth Provider. This setting is <b>required</b>. Valid examples for
         * provider are: amazon, facebook, github, gitlab, google, linkedin, microsoft, twitch
         */
        public Builder withProvider(final String provider) {
            this.provider = provider;
            return this;
        }

        /**
         * The OAuth Scopes
         */
        public Builder withScopes(final List<String> scopes) {
            this.scopes = scopes;
            return this;
        }

        /**
         * The OAuth Emails
         */
        public Builder withAllowEmails(final List<String> emails) {
            this.allowEmails = emails;
            return this;
        }

        /**
         * The OAuth Domains
         */
        public Builder withAllowDomains(final List<String> domains) {
            this.allowDomains = domains;
            return this;
        }

        public OAuth build() {
            if (isNull(provider)) {
                throw new IllegalArgumentException("OAuth needs a provider set");
            }
            return new OAuth(this);
        }
    }

    private final String provider;

    private final List<String> scopes;

    private final List<String> allowEmails;

    private final List<String> allowDomains;

    private OAuth(Builder builder) {
        this.provider = builder.provider;
        this.scopes = builder.scopes;
        this.allowDomains = builder.allowDomains;
        this.allowEmails = builder.allowEmails;
    }

    public String getProvider() {
        return provider;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public List<String> getAllowEmails() {
        return allowEmails;
    }

    public List<String> getAllowDomains() {
        return allowDomains;
    }

}
