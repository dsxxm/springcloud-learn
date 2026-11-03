package com.macro.cloud.feign.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.macro.cloud.feign.client.UserService;
import com.macro.cloud.feign.dto.Result;
import com.macro.cloud.feign.dto.User;

/**
 * UserController
 */
@RestController
@RequestMapping("/user")
public class UserController {

	private final UserService userService;

	UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/{id}")
	public Result<User> getUser(@PathVariable int id) {
		return userService.getUser(id);
	}

	@PostMapping("/create")
	public Result<Void> create(@RequestBody User user) {
		return userService.create(user);
	}
}
