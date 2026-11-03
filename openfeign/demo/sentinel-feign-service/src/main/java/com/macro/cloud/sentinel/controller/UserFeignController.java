package com.macro.cloud.sentinel.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.macro.cloud.sentinel.client.UserService;

/**
 * UserFeignController
 */
@RestController
@RequestMapping("/sentinel/user")
public class UserFeignController {

	private final UserService userService;

	public UserFeignController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/{id}")
	public Object getUser(@PathVariable int id) {
		return userService.getUser(id);
	}
}
