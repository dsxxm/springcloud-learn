package com.macro.cloud.sentinel.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.macro.cloud.sentinel.fallback.UserFallback;

/**
 * UserService
 */
@FeignClient(value = "nacos-user-service", fallback = UserFallback.class)
public interface UserService {

	@GetMapping("/user/{id}")
	public Object getUser(@PathVariable("id") int id);
}
