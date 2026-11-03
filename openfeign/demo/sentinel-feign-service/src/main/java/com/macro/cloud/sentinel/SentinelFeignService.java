package com.macro.cloud.sentinel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * SentinelFeignService
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class SentinelFeignService {

	public static void main(String[] args) {
		SpringApplication.run(SentinelFeignService.class, args);
	}
}
