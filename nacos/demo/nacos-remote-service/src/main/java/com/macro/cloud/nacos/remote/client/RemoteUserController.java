package com.macro.cloud.nacos.remote.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.macro.cloud.nacos.remote.dto.Result;
import com.macro.cloud.nacos.remote.dto.User;

/**
 * RemoteUserController
 */
@RestController
@RequestMapping("/user")
public class RemoteUserController {

	private final RestTemplate restTemplate;

	@Value("${service-url.nacos-user-service}")
	private String userServiceUrl;

	public RemoteUserController(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@GetMapping("/{id}")
	public Result<?> getUser(@PathVariable int id) {
		return restTemplate.getForObject(userServiceUrl + "/user/{id}", Result.class, id);
	}

	@PostMapping("/create")
	public Result<?> create(@RequestBody User user) {
		return restTemplate.postForObject(userServiceUrl + "/user/create", user, Result.class);
	}
}
