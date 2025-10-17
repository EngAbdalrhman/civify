package com.app.civify.security;

import com.app.civify.service.auth.TokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private TokenService tokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String name = authentication.getName();
        String access = tokenService.generateAccessToken(name, "ROLE_USER");
        String refresh = tokenService.generateRefreshToken(name);
        String body = "{\"accessToken\":\"" + access + "\",\"refreshToken\":\"" + refresh + "\"}";
        response.setContentType("application/json");
        response.getWriter().write(body);
    }
}
