package com.github.alexdlaird.ngrok.protocol;

import static java.util.Objects.*;

import java.util.Arrays;
import java.util.List;
import com.google.gson.annotations.SerializedName;

public class OAuth {

  public static class Builder {
    private String provider;
    private List<String> scopes;
    private List<String> allowEmails;
    private List<String> allowDomains;

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
    public Builder withScopes(final String... scopes) {
      this.scopes = Arrays.asList(scopes);
      return this;
    }

    /**
     * The OAuth Emails
     */
    public Builder withAllowEmails(final String... emails) {
      this.allowEmails = Arrays.asList(emails);
      return this;
    }

    /**
     * The OAuth Domains
     */
    public Builder withAllowDomains(final String... domains) {
      this.allowDomains = Arrays.asList(domains);
      return this;
    }

    public OAuth build() {
      if (isNull(provider)) {
        throw new IllegalArgumentException("OAuth needs a provider set");
      }
      return new OAuth(this);
    }
  }

  private String provider;

  @SerializedName("oauth_scopes")
  private List<String> scopes;

  @SerializedName("allow_emails")
  private List<String> allowEmails;

  @SerializedName("allow_domains")
  private List<String> allowDomains;

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
