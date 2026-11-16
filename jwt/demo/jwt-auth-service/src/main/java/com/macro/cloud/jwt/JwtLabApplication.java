package com.macro.cloud.jwt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.macro.cloud.jwt.config.TokenProperties;

/**
 * JwtLabApplication
 */
@SpringBootApplication
@EnableConfigurationProperties(TokenProperties.class)
public class JwtLabApplication {

	public static void main(String[] args) {
		SpringApplication.run(JwtLabApplication.class, args);
	}
}
