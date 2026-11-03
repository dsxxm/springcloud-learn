package com.macro.cloud.feign.fallback;

import com.macro.cloud.feign.client.UserService;
import com.macro.cloud.feign.dto.Result;
import com.macro.cloud.feign.dto.User;
import org.springframework.stereotype.Component;

/**
 * UserFallback
 */
@Component
public class UserFallback implements UserService {

	@Override
	public Result<Void> create(User user) {
		return new Result<>(500, "调用失败 服务被降级");
	}

	@Override
	public Result<User> getUser(int id) {
		return new Result<>(500, "调用失败服务被降级", new User(-1, "defaultUser", "123456"));
	}

}
