package com.macro.cloud.jwt.dto;

/**
 * UnauthorizedException
 */
public class UnauthorizedException extends RuntimeException {

	public UnauthorizedException(String message) {
		super(message);
	}
}
