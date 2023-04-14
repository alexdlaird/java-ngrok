package com.github.alexdlaird.ngrok.protocol;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class OAuth {
	
	private String provider;
	
	@SerializedName("oauth_scopes")
    private List<String> scopes;
	
	@SerializedName("allow_emails")
    private List<String> allowEmails;
	
	@SerializedName("allow_domains")
    private List<String> allowDomains;

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public List<String> getScopes() {
		return scopes;
	}

	public void setScopes(List<String> scopes) {
		this.scopes = scopes;
	}

	public List<String> getAllowEmails() {
		return allowEmails;
	}

	public void setAllowEmails(List<String> allowEmails) {
		this.allowEmails = allowEmails;
	}

	public List<String> getAllowDomains() {
		return allowDomains;
	}

	public void setAllowDomains(List<String> allowDomains) {
		this.allowDomains = allowDomains;
	}
	
	
}
