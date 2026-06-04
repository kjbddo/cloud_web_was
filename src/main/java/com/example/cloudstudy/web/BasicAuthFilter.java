package com.example.cloudstudy.web;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BasicAuthFilter implements Filter {
    private static final String USER_ENV = "ADMIN_USERNAME";
    private static final String PASSWORD_ENV = "ADMIN_PASSWORD";

    private String username;
    private String password;

    @Override
    public void init(FilterConfig filterConfig) {
        username = trimToNull(System.getenv(USER_ENV));
        password = trimToNull(System.getenv(PASSWORD_ENV));
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // 학습 편의를 위해 ADMIN_USERNAME/ADMIN_PASSWORD가 없으면 인증을 비활성화한다.
        // 운영 환경에서는 반드시 인증 또는 네트워크 접근 제한을 적용해야 한다.
        if (username == null || password == null) {
            chain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization");
        if (isValid(authorization)) {
            chain.doFilter(request, response);
            return;
        }

        response.setHeader("WWW-Authenticate", "Basic realm=\"Cloud Study Admin\"");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "관리자 인증이 필요합니다.");
    }

    private boolean isValid(String authorization) {
        if (authorization == null || !authorization.startsWith("Basic ")) {
            return false;
        }

        try {
            String encoded = authorization.substring("Basic ".length());
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            return (username + ":" + password).equals(decoded);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
