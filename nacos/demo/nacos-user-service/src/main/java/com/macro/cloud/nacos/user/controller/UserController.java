package com.macro.cloud.nacos.user.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.macro.cloud.nacos.user.dto.Result;
import com.macro.cloud.nacos.user.dto.User;

/**
 * UserController
 */
@RestController
@RequestMapping("/user")
public class UserController {

	private final static Logger LOGGER = LoggerFactory.getLogger(UserController.class);
	@Value("${server.port:8080}")
	private int port;

	@PostMapping("/create")
	public Result<Void> create(@RequestBody User user) {
		LOGGER.info("created user:{}", user);
		return new Result<>(200, "success");
	}

	@GetMapping("/{id}")
	public Result<User> getUser(@PathVariable int id) {
		User user = new User();
		user.setId(1);
		user.setName("bakaxxm");
		user.setPasswd("123");
		if (id == 1) {
			LOGGER.info("user info:{}", user);
			LOGGER.info("port:{}", port);
		} else {
			LOGGER.info("cannot find user");
		}
		return new Result<>(200, "success", user);
	}
}
