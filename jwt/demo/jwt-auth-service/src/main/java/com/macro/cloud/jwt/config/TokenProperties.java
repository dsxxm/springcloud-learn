package com.macro.cloud.jwt.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * TokenProperties
 */
@ConfigurationProperties(prefix = "sercurity.token")
public class TokenProperties {
	private String issuer;
	private String audience;
	private String hmacKeyBase64;
	private Duration accessTtl;
	private Duration refreshTtl;

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public String getAudience() {
		return audience;
	}

	public void setAudience(String audience) {
		this.audience = audience;
	}

	public String getHmacKeyBase64() {
		return hmacKeyBase64;
	}

	public void setHmacKeyBase64(String hmacKeyBase64) {
		this.hmacKeyBase64 = hmacKeyBase64;
	}

	public Duration getAccessTtl() {
		return accessTtl;
	}

	public void setAccessTtl(Duration accessTtl) {
		this.accessTtl = accessTtl;
	}

	public Duration getRefreshTtl() {
		return refreshTtl;
	}

	public void setRefreshTtl(Duration refreshTtl) {
		this.refreshTtl = refreshTtl;
	}
}
