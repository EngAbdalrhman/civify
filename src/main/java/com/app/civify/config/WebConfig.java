package com.app.civify.config;

import com.app.civify.controller.v1.ApiV1Controller;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.security.interceptor.CivifyAuthorizationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final CivifyAuthorizationInterceptor civifyAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(civifyAuthInterceptor)
                .addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Automatically add /v1 prefix to all controllers annotated with @ApiV1Controller
        configurer.addPathPrefix("/v1",
                c -> c.isAnnotationPresent(ApiV1Controller.class)
        );
    }
}
