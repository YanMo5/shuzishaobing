package com.campushealth.platform.config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.campushealth.platform.model.CampusPrincipal;
import com.campushealth.platform.service.AccessControlService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiAuthenticationFilter extends OncePerRequestFilter {

    public static final String PRINCIPAL_ATTRIBUTE = "campusPrincipal";

    private final AccessControlService accessControlService;

    public ApiAuthenticationFilter(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return !requestUri.startsWith("/api/v1/")
                || requestUri.equals("/api/v1/health/ping")
                || requestUri.equals("/api/v1/students/register")
                || requestUri.equals("/api/v1/students/login")
                || requestUri.equals("/api/v1/staff/login")
                || requestUri.equals("/api/v1/admin/login");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String apiToken = request.getHeader("X-Api-Token");
        CampusPrincipal principal = accessControlService.resolvePrincipal(apiToken).orElse(null);
        if (principal == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing or invalid X-Api-Token");
            return;
        }
        request.setAttribute(PRINCIPAL_ATTRIBUTE, principal);
        filterChain.doFilter(request, response);
    }
}