package com.example.nexus.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Nexus Inventory API",
                version = "1.0.0",
                description = "API de gestiÃ³n de bodegas, inventarios y usuarios. Roles: ADMIN, WAREHOUSE_EMPLOYEE, WAREHOUSE_SUPERVISOR.",
                contact = @Contact(name = "Nexus Team", email = "nexus@local"),
                license = @License(name = "Apache 2.0")
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    OpenApiCustomizer openApiServerCustomizer(@Value("${openapi.server-url}") String openApiServerUrl) {
        return openApi -> openApi.setServers(List.of(
                new Server()
                        .url(openApiServerUrl)
                        .description("Configured server")
        ));
    }
}
