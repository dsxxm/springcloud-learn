package com.macro.cloud.satoken;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * SaTokenGatewayApplication
 */
@EnableDiscoveryClient
@SpringBootApplication
public class SaTokenGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(SaTokenGatewayApplication.class, args);
	}
}
