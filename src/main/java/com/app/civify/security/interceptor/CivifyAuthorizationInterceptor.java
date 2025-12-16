package com.app.civify.security.interceptor;

import com.app.civify.entity.User;
import com.app.civify.security.UserPrincipal;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.service.auth.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class CivifyAuthorizationInterceptor implements HandlerInterceptor {

    private final PermissionService permissionService;

    public CivifyAuthorizationInterceptor(@Lazy PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod method)) {
            return true; // Not a controller -> allow
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        User user = null;
        if (principal instanceof UserPrincipal userPrincipal) {
            user = userPrincipal.getUser();
        }
        // Check annotation on method
        CivifyAuthorization auth = method.getMethodAnnotation(CivifyAuthorization.class);

        // If not found on method, check on class
        if (auth == null) {
            auth = method.getBeanType().getAnnotation(CivifyAuthorization.class);
        }

        // If no annotation → public endpoint
        if (auth == null) {
            return true;
        }

        String requiredPermission = auth.permission();

        // Get logged-in user from SecurityContext

        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated");
            return false;
        }

        // Check dynamic permissions via PermissionService
        boolean hasPermission = permissionService.hasPermission(user, requiredPermission);

        if (!hasPermission) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "You do not have permission: " + requiredPermission);
            return false;
        }

        return true;
    }
}