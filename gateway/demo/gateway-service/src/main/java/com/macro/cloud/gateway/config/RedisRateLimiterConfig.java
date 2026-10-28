package com.macro.cloud.gateway.config;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * RedisRateLimiterConfig
 */
@Configuration
public class RedisRateLimiterConfig {

	@Bean
	public KeyResolver ipKeyResolver(){
		return new KeyResolver() {

			@Override
			public Mono<String> resolve(ServerWebExchange exchange) {
				String ip = "unknown";
				InetSocketAddress socketAddress = exchange.getRequest().getRemoteAddress();

				if (socketAddress != null){
					InetAddress address  = socketAddress.getAddress();
					if (address != null){
						ip = address.getHostAddress();
					}
				}

				return Mono.just(ip);
			}
			
		};
	}
}
