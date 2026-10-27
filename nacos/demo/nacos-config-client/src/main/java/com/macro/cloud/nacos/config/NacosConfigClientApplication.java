package com.macro.cloud.nacos.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * NacosConfigClientApplication
 */
@EnableDiscoveryClient
@SpringBootApplication
public class NacosConfigClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(NacosConfigClientApplication.class, args);
	}
}
