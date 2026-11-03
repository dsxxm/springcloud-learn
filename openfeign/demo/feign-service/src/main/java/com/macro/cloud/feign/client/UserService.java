package com.macro.cloud.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.macro.cloud.feign.dto.Result;
import com.macro.cloud.feign.dto.User;
import com.macro.cloud.feign.fallback.UserFallback;

/**
 * UserService
 */
@FeignClient(value = "nacos-user-service", fallback = UserFallback.class)
public interface UserService {

	@PostMapping("/user/create")
	public Result<Void> create(@RequestBody User user);

	@GetMapping("/user/{id}")
	public Result<User> getUser(@PathVariable("id") int id);
}
