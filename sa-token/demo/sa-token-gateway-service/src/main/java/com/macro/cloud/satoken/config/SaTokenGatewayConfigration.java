package com.macro.cloud.satoken.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;

/**
 * SaTokenGatewayConfigration
 */
@Configuration
public class SaTokenGatewayConfigration {

	@Bean
	SaReactorFilter saReactorFilter() {
		return new SaReactorFilter()
				.addInclude("/**")
				.addExclude("/mall-auth/**")
				.addExclude("/mall-portal/sso/login")
				.addExclude("/mall-portal/sso/register")
				.addExclude("/actuator/**")
				.setAuth(object -> {
					SaRouter.match("/mall-admin/**", rule -> StpUtil.checkLogin());
					SaRouter.match("/mall-admin/**", rule -> StpUtil.checkPermission("admin"));
					SaRouter.match("/mall-user/**", rule -> StpUtil.checkLogin());
				}).setError(throwable -> SaResult.error(throwable.getMessage()));

	}
}
