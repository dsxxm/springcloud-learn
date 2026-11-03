package com.macro.cloud.sentinel.fallback;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.macro.cloud.sentinel.client.UserService;

/**
 * UserFallback
 */
@Component
public class UserFallback implements UserService {

	@Override
	public Object getUser(int id) {
		Map<String, Object> data = new HashMap<>();
		data.put("id", -1);
		data.put("name", "defaultuser");

		Map<String, Object> response = new HashMap<>();
		response.put("code", 503);
		response.put("message", "服务被降级");
		response.put("data", data);
		return response;
	}

}
