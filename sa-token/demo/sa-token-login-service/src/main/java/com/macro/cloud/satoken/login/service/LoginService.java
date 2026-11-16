package com.macro.cloud.satoken.login.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.macro.cloud.satoken.login.config.LoginProperties;
import com.macro.cloud.satoken.login.dto.LoginRequest;
import com.macro.cloud.satoken.login.dto.LoginResponse;
import com.macro.cloud.satoken.login.dto.UnauthorizedException;

import cn.dev33.satoken.stp.StpUtil;

@Service
public class LoginService {

    private final LoginProperties properties;
    private final PasswordEncoder passwordEncoder;

    public LoginService(LoginProperties properties, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        boolean usernameMatches = properties.getUsername().equals(request.getUsername());
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), properties.getPasswordHash());
        if (!usernameMatches || !passwordMatches) {
            throw new UnauthorizedException("invalid username or password");
        }

        StpUtil.login(properties.getAdminId());
        return new LoginResponse(StpUtil.getTokenValue());
    }

    public void logout() {
        StpUtil.checkLogin();
        StpUtil.logout();
    }
}
