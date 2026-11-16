package com.macro.cloud.jwt.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.macro.cloud.jwt.dto.RefreshRequest;
import com.macro.cloud.jwt.dto.TokenPair;
import com.macro.cloud.jwt.dto.UnauthorizedException;
import com.macro.cloud.jwt.service.TokenService;

/**
 * AuthController
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

	private final TokenService tokens;

	public AuthController(TokenService tokens) {
		this.tokens = tokens;
	}

	private String bearer(String authorization) {
		if (!authorization.startsWith("Bearer ")) {
			throw new UnauthorizedException("Bearer access token required");
		}

		return authorization.substring(7);
	}

	@PostMapping("/demo-login")
	TokenPair login() {
		return tokens.login("demo-user");
	}

	@PostMapping("/refresh")
	TokenPair refresh(@RequestBody RefreshRequest request) {
		return tokens.refresh(request.getRefreshToken());
	}

	@PostMapping("/logout")
	ResponseEntity<Void> logout(@RequestHeader String authorization) {
		tokens.logout(bearer(authorization));

		return ResponseEntity.noContent().build();
	}
}
