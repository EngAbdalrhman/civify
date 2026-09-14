package com.app.civify.security.interceptor;

import com.app.civify.annotation.RequiresScope;
import com.app.civify.entity.security.ApiKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Intercepts external API requests and enforces granular ApiKeyScope permissions.
 */
@Slf4j
@Component
public class ApiKeyScopeInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        RequiresScope annotation = method.getMethodAnnotation(RequiresScope.class);
        if (annotation == null) {
            annotation = method.getBeanType().getAnnotation(RequiresScope.class);
        }

        if (annotation == null) {
            return true; // No explicit scope required
        }

        ApiKey apiKey = (ApiKey) request.getAttribute("CURRENT_API_KEY");
        if (apiKey == null) {
            // Not an API-key authenticated request (e.g. web cookie session or unauthenticated public endpoint)
            return true;
        }

        String requiredScope = annotation.value();
        if (!apiKey.hasScope(requiredScope)) {
            log.warn("ApiKeyScopeInterceptor: Access Denied. Key preview: {}, Required: {}, Granted: {}",
                    apiKey.getKeyPreview(), requiredScope, apiKey.getScopes());

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            String scopesJson = apiKey.getScopes() != null ? apiKey.getScopes().toString() : "[]";
            response.getWriter().write(String.format(
                    "{\"error\":\"INSUFFICIENT_SCOPE\",\"message\":\"This API key lacks the required '%s' scope.\",\"requiredScope\":\"%s\",\"grantedScopes\":%s}",
                    requiredScope, requiredScope, scopesJson
            ));
            return false;
        }

        return true;
    }
}
