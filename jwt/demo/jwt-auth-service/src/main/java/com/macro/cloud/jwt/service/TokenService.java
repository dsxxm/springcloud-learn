package com.macro.cloud.jwt.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.macro.cloud.jwt.config.TokenProperties;
import com.macro.cloud.jwt.dto.TokenPair;
import com.macro.cloud.jwt.dto.UnauthorizedException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * TokenService
 */
@Service
public class TokenService {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final String ROTATE_SCRIPT = "local saved = redis.call('GET', KEYS[1])\n" +
			"if not saved then return 0 end\n" +
			"if saved ~= ARGV[1] then redis.call('DEL', KEYS[1]); return -1 end\n" +
			"redis.call('SET',KEYS[1],ARGV[2],'EX',ARGV[3])\n" +
			"return 1\n";

	private final StringRedisTemplate redisTemplate;
	private final TokenProperties properties;
	private final DefaultRedisScript<Long> rotateScript = new DefaultRedisScript<>(ROTATE_SCRIPT, Long.class);

	public TokenService(StringRedisTemplate redisTemplate, TokenProperties properties) {
		this.redisTemplate = redisTemplate;
		this.properties = properties;
	}

	// redis
	private String refreshKey(String familyId) {
		return "auth:refresh:" + familyId;
	}

	// generate random token
	private String randomToken() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	// security
	private String sha256(String value) {
		try {
			return Base64.getEncoder()
					.encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
	}

	// build signingKey
	private SecretKey signingKey() {
		return Keys.hmacShaKeyFor(Base64.getDecoder().decode(properties.getHmacKeyBase64()));
	}

	// build access token
	private String mintAccessToken(String userId, String familyId) {
		Instant now = Instant.now();

		return Jwts.builder()
				.header().type("at+jwt").and()
				.issuer(properties.getIssuer())
				.subject(userId)
				.audience().add(properties.getAudience()).and()
				.id(UUID.randomUUID().toString())
				.claim("scope", "profile.read")
				.claim("sid", familyId)
				.issuedAt(Date.from(now))
				.notBefore(Date.from(now))
				.expiration(Date.from(now.plus(properties.getAccessTtl())))
				.signWith(signingKey())
				.compact();
	}

	private TokenPair issue(String userId, String familyId) {
		String refreshSecret = randomToken();
		redisTemplate.opsForValue().set(refreshKey(familyId), sha256(refreshSecret), properties.getRefreshTtl());

		return new TokenPair(mintAccessToken(userId, familyId), familyId + "." + refreshSecret);
	}

	private Claims parseAccessToken(String accessToken) {
		Jws<Claims> parsed = Jwts.parser().verifyWith(signingKey()).requireIssuer(properties.getIssuer())
				.requireAudience(properties.getAudience()).build().parseSignedClaims(accessToken);
		if (!"at+jwt".equals(parsed.getHeader().getType())) {
			throw new UnauthorizedException("wrong token type");
		}
		Claims claims = parsed.getPayload();

		if (Boolean.TRUE.equals(redisTemplate.hasKey("auth:deny" + claims.getId()))) {
			throw new UnauthorizedException("access token revoked");
		}

		return claims;
	}

	public TokenPair login(String userId) {
		return issue(userId, UUID.randomUUID().toString());
	}

	public TokenPair refresh(String refreshToken) {
		String[] parts = refreshToken.split("\\.", 2);
		if (parts.length != 2) {
			throw new UnauthorizedException("invalid refresh token format");
		}
		String familyId = parts[0];
		String currentHash = sha256(parts[1]);
		String nextSecret = randomToken();
		List<String> key = new ArrayList<>();
		key.add(refreshKey(familyId));
		Long result = redisTemplate.execute(rotateScript, key, currentHash, sha256(nextSecret),
				Long.toString(properties.getRefreshTtl().getSeconds()));

		if (result == null || result != 1L) {
			throw new UnauthorizedException("refresh token expired, reused, or revoken");
		}

		return new TokenPair(mintAccessToken("demo-user", familyId), familyId + "." + nextSecret);
	}

	public void logout(String accessToken) {
		Claims claims = parseAccessToken(accessToken);
		String jti = claims.getId();
		String familyId = claims.get("sid", String.class);
		Instant expiresAt = claims.getExpiration().toInstant();
		Duration remaining = Duration.between(Instant.now(), expiresAt);
		if (!remaining.isNegative() && !remaining.isZero()) {
			redisTemplate.opsForValue().set("auth:deny:" + jti, "1", remaining);
		}
		redisTemplate.delete(refreshKey(familyId));
	}

}
