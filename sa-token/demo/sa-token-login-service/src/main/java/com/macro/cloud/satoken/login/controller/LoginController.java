package com.macro.cloud.satoken.login.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.macro.cloud.satoken.login.dto.LoginRequest;
import com.macro.cloud.satoken.login.dto.LoginResponse;
import com.macro.cloud.satoken.login.service.LoginService;

@RestController
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return loginService.login(request);
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout() {
        loginService.logout();
        return ResponseEntity.noContent().build();
    }
}
