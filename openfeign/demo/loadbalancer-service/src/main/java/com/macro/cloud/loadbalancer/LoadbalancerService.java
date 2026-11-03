package com.macro.cloud.loadbalancer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * LoadbalancerService
 */
@SpringBootApplication
@EnableDiscoveryClient
public class LoadbalancerService {

	public static void main(String[] args) {
		SpringApplication.run(LoadbalancerService.class, args);
	}
}
