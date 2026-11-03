package com.macro.cloud.loadbalancer.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * UserLoadbalancedController
 */
@RestController
@RequestMapping("/loadlalancer/user")
public class UserLoadbalancedController {

	private final RestTemplate restTemplate;

	public UserLoadbalancedController(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@GetMapping("/{id}")
	public Object getUserById(@PathVariable int id) {
		return restTemplate.getForObject("http://nacos-user-service/user/{id}", Object.class, id);
	}
}
