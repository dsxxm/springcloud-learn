package com.macro.cloud.satoken.login;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import com.macro.cloud.satoken.login.config.LoginProperties;

@EnableDiscoveryClient
@SpringBootApplication
@EnableConfigurationProperties(LoginProperties.class)
public class SaTokenLoginApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaTokenLoginApplication.class, args);
    }
}
