package com.app.civify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import java.util.Arrays;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

    /**
     * Configure OpenAPI documentation
     *
     * @return OpenAPI configuration
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Civify API")
                        .description("Comprehensive API for the Civify CV management and job application platform. Features include user authentication, subscription management, CV generation and tailoring, payment processing, and API usage analytics.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Civify Team")
                                .email("support@civify-app.com")
                                .url("https://cvupper.netlify.app"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(Arrays.asList(
                        new Server()
                                .url("http://localhost:8080/api/v1")
                                .description("Development server"),
                        new Server()
                                .url("https://cvupper.netlify.app/api/v1")
                                .description("Production server")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
